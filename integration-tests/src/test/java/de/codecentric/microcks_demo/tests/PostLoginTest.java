package de.codecentric.microcks_demo.tests;

import de.codecentric.microcks_demo.tests.infrastructure.InfrastructureSetup;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static de.codecentric.microcks_demo.tests.infrastructure.InfrastructureSetup.HTTP_HEADER_API_KEY;
import static de.codecentric.microcks_demo.tests.infrastructure.InfrastructureSetup.getApiKeyToken;
import static de.codecentric.microcks_demo.tests.infrastructure.InfrastructureSetup.getLoginApiExamples;
import static de.codecentric.microcks_demo.tests.infrastructure.InfrastructureSetup.MockedApiExample;
import static de.codecentric.microcks_demo.tests.infrastructure.InfrastructureSetup.getMockedApiBaseUri;
import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

/**
 * Tests for the "POST /login" operation based on <a href="https://rest-assured.io/">REST-assured</a>. The tests run
 * against the Microcks testcontainer that mocks the example API and is fired up by invoking
 * {@link InfrastructureSetup#setup()}.
 */
public class PostLoginTest {
    @BeforeAll
    static void setup() {
        InfrastructureSetup.setup();
    }

    /**
     * Test valid requests.
     */
    @Test
    void testValidRequests() {
        // Correct customer credentials must result in corresponding customer login tokens
        for (var example : getLoginApiExamples().values()) {
            var loginRequestBody = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(example.customerEmail(), example.customerPassword());

            given().headers(
                    "Accept", "application/json",
                    HTTP_HEADER_API_KEY, getApiKeyToken()
                )
                .when().body(loginRequestBody).post(getMockedApiBaseUri() + "/login")
                    .then().statusCode(Response.Status.OK.getStatusCode())
                        .body("customer_token", is(example.customerToken()));
        }
    }

    /**
     * Test invalid requests.
     */
    @Test
    void testInvalidRequests() {
        var invalidExample = getLoginApiExamples().values().stream().findFirst()
            .orElse(new MockedApiExample("invalid_email", "invalid_password", "invalid_token"));

        var requestBodyWithInvalidPassword = """
                {
                    "email": "%s",
                    "password": "%s_appendix_to_make_password_invalid"
                }
                """.formatted(invalidExample.customerEmail(), invalidExample.customerPassword());

        var endpointUri = getMockedApiBaseUri() + "/login";

        // Without the correct API key token generated and installed to the mocked operation during the call to
        // InfrastructureSetup.setup(), requests against the operation are invalid
        given().header("Accept", "application/json")
            .when().body(requestBodyWithInvalidPassword).post(endpointUri)
                .then().statusCode(Response.Status.BAD_REQUEST.getStatusCode());

        // Requests with wrong credentials must result in HTTP Unauthorized (401) responses
        given().headers(
                "Accept", "application/json",
                HTTP_HEADER_API_KEY, getApiKeyToken()
            )
            .when().body(requestBodyWithInvalidPassword).post(endpointUri)
                .then().statusCode(Response.Status.UNAUTHORIZED.getStatusCode());
    }
}
