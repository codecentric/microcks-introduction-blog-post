package de.codecentric.microcks_demo.tests;

import de.codecentric.microcks_demo.tests.infrastructure.InfrastructureSetup;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static de.codecentric.microcks_demo.tests.infrastructure.InfrastructureSetup.HTTP_HEADER_API_KEY;
import static de.codecentric.microcks_demo.tests.infrastructure.InfrastructureSetup.getApiKeyToken;
import static de.codecentric.microcks_demo.tests.infrastructure.InfrastructureSetup.getMockedApiBaseUri;
import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

/**
 * Tests for the "GET /customer_kinds" operation based on <a href="https://rest-assured.io/">REST-assured</a>. The tests
 * run against the Microcks testcontainer that mocks the example API and is fired up by invoking
 * {@link InfrastructureSetup#setup()}.
 */
public class GetCustomerKindsTest {
    public static Map<String, String> expectedCustomerKinds = new HashMap<>() {{
            put("1", "Private person");
            put("2", "Commercial enterprise");
            put("3", "Public institution");
        }};

    @BeforeAll
    static void setup() {
        InfrastructureSetup.setup();
    }

    /**
     * Test valid requests.
     */
    @Test
    void testValidRequests() {
        // Each customer kind must have the expected description
        var statusCodeResponse = given().headers(
                "Accept", "application/json",
                HTTP_HEADER_API_KEY, getApiKeyToken()
            )
            .when().get(getMockedApiBaseUri() + "/customer_kinds")
                .then().statusCode(Response.Status.OK.getStatusCode());
        expectedCustomerKinds.forEach((key, description) -> statusCodeResponse.body(key, is(description)));
    }

    /**
     * Test invalid requests.
     */
    @Test
    void testInvalidRequests() {
        // Without the correct API key token generated and installed to the mocked operation during the call to
        // InfrastructureSetup.setup(), requests against the operation are invalid
        given().header("Accept", "application/json")
            .when().get(getMockedApiBaseUri() + "/customer_kinds")
                .then().statusCode(Response.Status.BAD_REQUEST.getStatusCode());
    }
}
