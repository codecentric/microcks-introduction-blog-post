package de.codecentric.microcks_demo.tests;

import de.codecentric.microcks_demo.tests.infrastructure.InfrastructureSetup;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static de.codecentric.microcks_demo.tests.infrastructure.InfrastructureSetup.HTTP_HEADER_CUSTOMER_TOKEN;
import static de.codecentric.microcks_demo.tests.infrastructure.InfrastructureSetup.getLoginApiExamples;
import static de.codecentric.microcks_demo.tests.infrastructure.InfrastructureSetup.getMockedApiBaseUri;
import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

/**
 * Tests for the "GET /customer" operation based on <a href="https://rest-assured.io/">REST-assured</a>. The tests run
 * against the Microcks testcontainer that mocks the example API and is fired up by invoking
 * {@link InfrastructureSetup#setup()}.
 */
public class GetCustomerTest {
    @BeforeAll
    static void setup() {
        InfrastructureSetup.setup();
    }

    /**
     * Test valid requests.
     */
    @Test
    void testValidRequests() {
        // The customer information returned by the operation must correspond to the customer's login token to be
        // obtained from the "POST /login" operation
        getLoginApiExamples().values().forEach(example ->
            given().headers(
                    "Accept", "application/json",
                    HTTP_HEADER_CUSTOMER_TOKEN, example.customerToken()
                )
                .when().get(getMockedApiBaseUri() + "/customer")
                    .then().statusCode(Response.Status.OK.getStatusCode()).body("email", is(example.customerEmail()))
        );
    }

    /**
     * Test invalid requests.
     */
    @Test
    void testInvalidRequests() {
        // Unknown tokens must result in an HTTP Unauthorized (401) responses
        var endpointUri = getMockedApiBaseUri() + "/customer";
        var validExample = getLoginApiExamples().values().stream().findFirst().orElse(null);
        var token = validExample != null ? validExample.customerToken() : "none";
        given().headers(
                "Accept", "application/json",
                HTTP_HEADER_CUSTOMER_TOKEN, token + "_appendix_to_make_token_invalid"
            )
            .when().get(endpointUri)
                .then().statusCode(Response.Status.UNAUTHORIZED.getStatusCode());
    }
}
