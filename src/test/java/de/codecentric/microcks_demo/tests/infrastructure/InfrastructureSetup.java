package de.codecentric.microcks_demo.tests.infrastructure;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.microcks.domain.ParameterConstraint;
import io.github.microcks.domain.ParameterLocation;
import io.github.microcks.domain.Service;
import io.github.microcks.testcontainers.MicrocksContainer;
import io.github.microcks.util.DispatchStyles;
import io.github.microcks.web.dto.OperationOverrideDTO;
import io.jsonwebtoken.Jwts;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.parser.OpenAPIV3Parser;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Set up the testcontainers infrastructure to mock the example API, and illustrate the capabilities and usage of
 * Microcks's own API.
 */
public class InfrastructureSetup {
    public static final String HTTP_HEADER_API_KEY = "api_key";
    public static final String HTTP_HEADER_CUSTOMER_TOKEN = "customer_token";

    private static final long DEFAULT_TOKEN_VALIDITY_MS = TimeUnit.MINUTES.toMillis(5);
    private static final Logger logger = LogManager.getLogger(InfrastructureSetup.class);

    private static Info apiInfo;
    private static String apiKeyToken;
    private static MicrocksContainer apiMock;
    private static Map<String, MockedApiExample> loginApiExamples;

    /**
     * Allow for manual examination of the testcontainers infrastructure outside regular test runs, after whose
     * execution the infrastructure is shut down. The trigger manual infrastructure examination, run the program with
     * Maven using {@code mvn clean test -Dtest="InfrastructureSetup"}. This call will ultimately invoke the
     * {@link #haltForInfrastructureExamination()} test that informs about the host HTTP endpoint of the Microcks
     * testcontainer and prevents its shut down.
     */
    @BeforeAll
    static void enableInfrastructureExamination() {
        setup();
    }

    @Test
    void haltForInfrastructureExamination() {
        logger.info("Mock HTTP endpoint: {}", apiMock.getHttpEndpoint());
        logger.info("Ctrl+C to exit");
        new Scanner(System.in).nextLine();
    }

    /**
     * Trigger infrastructure setup. This method is expected to be called prior to the execution of tests that rely on
     * the availability of the mocked API. Note that for the sake of efficiency the testcontainer for the mocked API is
     * only started if it wasn't already by another test in the same test run.
     */
    public static void setup() {
        if (apiMock != null && apiMock.isRunning())
            return;

        /* Start the Microcks testcontainer for the API mock */
        apiMock = new MicrocksContainer("quay.io/microcks/microcks-uber:1.9.0");
        apiMock
            .withCreateContainerCmdModifier(cmd -> cmd.withName("microcks_demo_api_mock"))
            // Main artifacts are the API specifications from which Microcks derives mocks
            .withMainArtifacts("customers.yaml")
            // Secondary artifacts may accompany the main artifacts and provide, e.g., examples for API requests and
            // responses in the form of Postman collections. For illustrative purposes, we specify examples in both such
            // a collection and the OpenAPI specification for the mock (i.e., the main artifact). See
            // https://microcks.io/documentation/explanations/multi-artifacts for details.
            .withSecondaryArtifacts("customers_examples.postman_collection.json")
            .start();

        /*
         * Generate a Java Web Token (JWT) that the API expects (as an additional, yet weak, security measure) for some
         * of its operations
         */
        apiKeyToken = buildMockedApiKeyToken();

        /* Prepare the API mock for tests */
        // We parse the OpenAPI spec that is the mock's foundation because we (i) require some of its information to be
        // able to access the mock (primarily API title and version); and (ii) prepare the mock itself.
        var parsedSpec = new OpenAPIV3Parser().read("customers.yaml");
        apiInfo = parsedSpec.getInfo();
        var mockedService = getMockedApiMicrocksService(apiInfo, apiMock.getHttpEndpoint());

        // Prepare mock for "GET /customer_kinds" operation
        prepareCustomerKindsOperationMock(apiMock.getHttpEndpoint(), mockedService, apiKeyToken);

        // Prepare mock for "GET /customer_kinds/{id}" operation
        prepareCustomerKindsIdOperationMock(apiMock.getHttpEndpoint(), mockedService, apiKeyToken);

        // Prepare mock for "POST /login" operation
        loginApiExamples = getMockedApiLoginExamples(parsedSpec);
        var loginErrorExampleName = parsedSpec
            .getPaths()
            .get("/login")
            .getPost()
            .getResponses()
            .get(String.valueOf(Response.Status.UNAUTHORIZED.getStatusCode()))
            .getContent()
            .get("application/json")
            .getExamples()
            .keySet().stream().findFirst().orElse("null");
        prepareLoginOperationMock(apiMock.getHttpEndpoint(), mockedService, apiKeyToken, loginApiExamples,
            loginErrorExampleName);

        // Prepare mock for "GET /customer" operation
        var customerErrorExampleName = parsedSpec
            .getPaths()
            .get("/customer")
            .getGet()
            .getResponses()
            .get(String.valueOf(Response.Status.UNAUTHORIZED.getStatusCode()))
            .getContent()
            .get("application/json")
            .getExamples()
            .keySet().stream().findFirst().orElse("null");
        prepareCustomerOperationMock(apiMock.getHttpEndpoint(), mockedService, loginApiExamples,
            customerErrorExampleName);
    }

    /**
     * Generate a JWT token to serve as the API key for some operations of the mocked API.
     */
    private static String buildMockedApiKeyToken() {
        var now = new Date();
        return Jwts.builder()
            .issuer("APIMock")
            .issuedAt(now)
            .expiration(new Date(now.getTime() + DEFAULT_TOKEN_VALIDITY_MS))
            .signWith(Jwts.SIG.HS256.key().build())
            .compact();
    }

    /**
     * From the given OpenAPI {@link Info} object retrieve the corresponding Microcks {@link Service} representation by
     * means of Microcks's own API.
     *
     * @see MicrocksApi
     */
    private static Service getMockedApiMicrocksService(Info apiInfo, String mockedApiBaseUri) {
        try(var restClient = (ResteasyClient) ResteasyClientBuilder.newClient()) {
            var serviceId = apiInfo.getTitle() + ":" + apiInfo.getVersion();
            var response = getMicrocksApiProxy(restClient, mockedApiBaseUri).getService(serviceId, false);
            assertEquals(response.getStatus(), Response.Status.OK.getStatusCode(),
                String.format("Couldn't retrieve Microcks information for service %s: Expected response code " +
                    "(%d) differs from actual response code (%d)", serviceId, Response.Status.OK.getStatusCode(),
                    response.getStatus()));
            return response.readEntity(Service.class);
        }
    }

    /**
     * Get Resteasy proxy for {@link MicrocksApi}.
     */
    private static MicrocksApi getMicrocksApiProxy(ResteasyClient client, String targetUri) {
        return client.target(targetUri).proxy(MicrocksApi.class);
    }

    /**
     * Prepare the Microcks mock of the "GET /customer_kinds" operation for subsequent tests. More specifically, we add
     * a Microcks <a href="https://microcks.io/documentation/guides/usage/mocks-constraints/">header constraint</a> to
     * check for an API key to be provided in the form of a JWT in order to invoke the operation. To this end, we rely
     * on Microcks's own API because there is currently no support to add such information to specifications based on
     * which Microcks provides mocks.
     */
    private static void prepareCustomerKindsOperationMock(String mockedApiBaseUri, Service mockedService,
        String apiKeyToken) {
        var operationMethod = HttpMethod.GET;
        var operationName = "customer_kinds";

        var dto = buildOperationOverrideDto(mockedService, operationMethod, operationName);
        var apiKeyConstraint = buildMockedApiKeyConstraint(apiKeyToken);
        // As Microcks's Override Service Operation endpoint overrides existing operation configuration, we preserve all
        // configuration properties, including possibly existing parameter constraints, and just add the given parameter
        // constraints to the configuration
        var combinedConstraints = new ArrayList<>(List.of(apiKeyConstraint));
        if (dto.getParameterConstraints() != null)
            combinedConstraints.addAll(dto.getParameterConstraints());
        dto.setParameterConstraints(combinedConstraints);

        overrideMockedApiOperation(mockedApiBaseUri, mockedService.getId(), operationMethod, operationName, dto);
    }

    /**
     * Create a Microcks {@link OperationOverrideDTO} instance from an operation of the given {@link Service} identified
     * by its HTTP method and name. The created instance can be passed to the
     * {@link #overrideMockedApiOperation(String, String, String, String, OperationOverrideDTO)} method as a wrapper for
     * Microcks's Override Service Operation endpoint.
     */
    private static OperationOverrideDTO buildOperationOverrideDto(Service mockedService, String operationMethod,
        String operationName) {
        var operationId = buildMicrocksOperationId(operationMethod, operationName);
        var operation = mockedService.getOperations().stream()
            .filter(o -> o.getName().equals(operationId))
            .findFirst()
            .orElse(null);
        assertNotNull(operation, "Couldn't retrieve Microcks information about operation " + operationId);

        var dto = new OperationOverrideDTO();
        dto.setDefaultDelay(operation.getDefaultDelay());
        dto.setDispatcher(operation.getDispatcher());
        dto.setDispatcherRules(operation.getDispatcherRules());
        dto.setParameterConstraints(operation.getParameterConstraints());
        return dto;
    }

    /**
     * Build the ID of an operation mocked by Microcks.
     */
    private static String buildMicrocksOperationId(String operationMethod, String operationName) {
        return String.format("%s /%s", operationMethod, operationName);
    }

    /**
     * Create a Microcks {@link ParameterConstraint} for an API key to be provided in the form of a JWT. More
     * specifically, the created constraint is a Microcks header constraint whose value must match a regular expression
     * that is equivalent to the given API key token.
     */
    private static ParameterConstraint buildMockedApiKeyConstraint(String apiKeyToken) {
        var constraint = new ParameterConstraint();
        constraint.setName(HTTP_HEADER_API_KEY);
        constraint.setRequired(true);
        constraint.setMustMatchRegexp(Pattern.quote(apiKeyToken));
        constraint.setIn(ParameterLocation.header);
        return constraint;
    }

    /**
     * Wrapper for Microcks's <a href="https://microcks.io/documentation/references/apis/open-api">Override Service
     * Operation endpoint</a>.
     */
    private static void overrideMockedApiOperation(
        String mockedApiBaseUri,
        String mockedServiceId,
        String operationMethod,
        String operationName,
        OperationOverrideDTO dto
    ) {
        var operationId = buildMicrocksOperationId(operationMethod, operationName);
        try(
            var restClient = (ResteasyClient) ResteasyClientBuilder.newClient();
            var response = getMicrocksApiProxy(restClient, mockedApiBaseUri)
                .overrideServiceOperation(mockedServiceId, operationId, dto)
        ) {
            assertEquals(response.getStatus(), Response.Status.OK.getStatusCode(),
                String.format("Couldn't override Microcks information for operation %s: Expected response code " +
                    "(%d) differs from actual response code (%d)", operationId, Response.Status.OK.getStatusCode(),
                    response.getStatus()));
        }
    }

    /**
     * Prepare the Microcks mock of the "GET /customer_kinds/{id}" operation for subsequent tests. As in
     * {@link #prepareCustomerKindsOperationMock(String, Service, String)}, we add a Microcks
     * <a href="https://microcks.io/documentation/guides/usage/mocks-constraints/">header constraint</a> to check for an
     * API key to be provided in the form of a JWT in order to invoke the operation. To this end, we rely on Microcks's
     * own API because there is currently no support to add such information to specifications based on which Microcks
     * provides mocks.
     */
    private static void prepareCustomerKindsIdOperationMock(String mockedApiBaseUri, Service mockedService,
        String apiKeyToken) {
        var operationMethod = HttpMethod.GET;
        var operationName = "customer_kinds/{id}";

        var dto = buildOperationOverrideDto(mockedService, operationMethod, operationName);
        var apiKeyConstraint = buildMockedApiKeyConstraint(apiKeyToken);
        var combinedConstraints = new ArrayList<>(List.of(apiKeyConstraint));
        if (dto.getParameterConstraints() != null)
            combinedConstraints.addAll(dto.getParameterConstraints());
        dto.setParameterConstraints(combinedConstraints);

        overrideMockedApiOperation(mockedApiBaseUri, mockedService.getId(), operationMethod, operationName, dto);
    }

    /**
     * From the given parsed {@link OpenAPI} specification retrieve a {@link Map} that links the name of each example
     * for the mocked API's "POST /login" operation to an instance of {@link MockedApiExample}. Consequently, the
     * constructed map facilitates the access to certain fields of the specified examples for the "POST /login"
     * operation.
     */
    private static Map<String, MockedApiExample> getMockedApiLoginExamples(OpenAPI parsedSpec) {
        var login = parsedSpec.getPaths().get("/login").getPost();
        var requestExamples = login.getRequestBody().getContent().get("application/json").getExamples();
        var responseExamples = login.getResponses().get(String.valueOf(Response.Status.OK.getStatusCode()))
            .getContent().get("application/json").getExamples();

        if (requestExamples == null || responseExamples == null)
            return Collections.emptyMap();

        return requestExamples.entrySet().stream()
            .filter(requestExample -> responseExamples.containsKey(requestExample.getKey()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> {
                    var exampleName = e.getKey();
                    var requestExample = (ObjectNode) e.getValue().getValue();
                    var customerEmail = requestExample.get("email").asText();
                    var customerPassword = requestExample.get("password").asText();
                    var correspondingResponseExample = (ObjectNode) responseExamples.get(exampleName).getValue();
                    var customerToken = correspondingResponseExample.get("customer_token").asText();
                    return new MockedApiExample(customerEmail, customerPassword, customerToken);
                }
            ));
    }

    /**
     * POJO that consolidates information from request and response examples of the "POST /login" operation.
     */
    public record MockedApiExample(String customerEmail, String customerPassword, String customerToken) {}

    /**
     * Prepare the Microcks mock of the "POST /login" operation for subsequent tests. As in
     * {@link #prepareCustomerKindsOperationMock(String, Service, String)}, we add a Microcks
     * <a href="https://microcks.io/documentation/guides/usage/mocks-constraints/">header constraint</a> to check for an
     * API key to be provided in the form of a JWT in order to invoke the operation. Furthermore, we add a
     * <a href="https://microcks.io/documentation/explanations/dispatching/#json-body-dispatcher">JSON BODY dispatcher
     * </a> to the operation in order to return the corresponding login token for users who provided the correct
     * credentials upon login. For adding the constraint and dispatcher, we rely on Microcks's own API because there is
     * currently no support to add such information to specifications based on which Microcks provides mocks.
     */
    private static void prepareLoginOperationMock(String mockedApiBaseUri, Service mockedService, String apiKeyToken,
        Map<String, MockedApiExample> examples, String errorExampleName) {
        var operationMethod = HttpMethod.POST;
        var operationName = "login";

        /* Add dispatcher */
        // Map the given login examples to case statements in the form of '"$jsonFieldValue": "$exampleToBeReturned" as
        // expected by Microcks for JSON_BODY dispatchers
        var credentialCases = examples.entrySet().stream()
            .map(e -> String.format("\"%s\": \"%s\",", e.getValue().customerPassword, e.getKey()))
            .collect(Collectors.joining("\n"));

        var dto = buildOperationOverrideDto(mockedService, operationMethod, operationName);
        dto.setDispatcher(DispatchStyles.JSON_BODY);
        // Note that the dispatcher relies on values in the "password" field of requests' JSON bodies to identify which
        // response example to send back to clients. As a result, passwords in response examples must be distinct. For
        // the API mock, we resorted to this approach of associating credentials with login tokens because there
        // currently seems no possibility in Microcks to compose JSON_BODY dispatchers so that combinations of JSON
        // field values like username and password can be checked.
        dto.setDispatcherRules("""
                {
                  "exp": "/password",
                  "operator": "equals",
                  "cases": {
                    %s
                    "default": "%s"
                  }
                }
            """.formatted(
                credentialCases,
                errorExampleName
            ));

        /* Add constraint */
        var combinedConstraints = new ArrayList<>(List.of(buildMockedApiKeyConstraint(apiKeyToken)));
        if (dto.getParameterConstraints() != null)
            combinedConstraints.addAll(dto.getParameterConstraints());
        dto.setParameterConstraints(combinedConstraints);

        overrideMockedApiOperation(mockedApiBaseUri, mockedService.getId(), operationMethod, operationName, dto);
    }

    /**
     * Prepare the Microcks mock of the "GET /customer" operation for subsequent tests. More precisely, we add a
     * <a href="https://microcks.io/documentation/explanations/dispatching/#script-dispatcher">SCRIPT dispatcher</a>
     * to the operation which distills a user's login token retrieved from the "POST /login" operation from the
     * expected request header field and informs Microcks about the example response to sent back for the token, i.e.,
     * the details of the logged-in user identified by the token. For adding the dispatcher, we rely on Microcks's own
     * API because there is currently no support to add such information to specifications based on which Microcks
     * provides mocks.
     */
    private static void prepareCustomerOperationMock(String mockedApiBaseUri, Service mockedService,
        Map<String, MockedApiExample> examples, String errorExampleName) {
        var operationMethod = HttpMethod.GET;
        var operationName = "customer";

        var loginCases = examples.entrySet().stream()
            .map(e -> String.format("case \"%s\": return \"%s\"", e.getValue().customerToken, e.getKey()))
            .collect(Collectors.joining("\n"));

        var dto = buildOperationOverrideDto(mockedService, operationMethod, operationName);
        dto.setDispatcher(DispatchStyles.SCRIPT);
        dto.setDispatcherRules("""
                def headers = mockRequest.getRequestHeaders()
                def loginToken = headers.get("%s", "null")
                switch(loginToken) {
                    %s
                    default: return "%s"
                }
            """.formatted(
                HTTP_HEADER_CUSTOMER_TOKEN,
                loginCases,
                errorExampleName
            ));

        overrideMockedApiOperation(mockedApiBaseUri, mockedService.getId(), operationMethod, operationName, dto);
    }

    public static String getMockedApiBaseUri() {
        return String.format("%s/rest/%s/%s", apiMock.getHttpEndpoint(), apiInfo.getTitle(),
            apiInfo.getVersion());
    }

    public static String getApiKeyToken() {
        return apiKeyToken;
    }

    public static Map<String, MockedApiExample> getLoginApiExamples() {
        return Map.copyOf(loginApiExamples);
    }
}
