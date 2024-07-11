package de.codecentric.microcks_demo.tests;

import de.codecentric.microcks_demo.tests.infrastructure.InfrastructureSetup;
import jakarta.ws.rs.core.Response;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.format.DateTimeFormatter;

import static de.codecentric.microcks_demo.tests.GetCustomerKindsTest.expectedCustomerKinds;
import static de.codecentric.microcks_demo.tests.infrastructure.InfrastructureSetup.HTTP_HEADER_API_KEY;
import static de.codecentric.microcks_demo.tests.infrastructure.InfrastructureSetup.getApiKeyToken;
import static de.codecentric.microcks_demo.tests.infrastructure.InfrastructureSetup.getMockedApiBaseUri;
import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

/**
 * Tests for the "GET /customer_kinds/{id}" operation based on <a href="https://rest-assured.io/">REST-assured</a>. The
 * tests run against the Microcks testcontainer that mocks the example API and is fired up by invoking
 * {@link InfrastructureSetup#setup()}.
 */
public class GetCustomerKindsIdTest {
    private static final String expectedDateFormat = "dd.MM.yyyy HH:mm:ss";

    @BeforeAll
    static void setup() {
        InfrastructureSetup.setup();
    }

    /**
     * Test valid requests.
     */
    @Test
    void testValidRequests() {
        // The description of each customer kind must be consistent with the output of the "GET /customer_kinds"
        // operation. Furthermore, each customer kind must have a creation date in a certain date format.
        expectedCustomerKinds.forEach((kindId, kindDescription) ->
            given().headers(
                "Accept", "application/json",
                HTTP_HEADER_API_KEY, getApiKeyToken()
            )
            .when().get(getMockedApiBaseUri() + "/customer_kinds/" + kindId)
                .then().statusCode(Response.Status.OK.getStatusCode()).body(
                    "kind", is(kindDescription),
                    "createdOn", hasDateFormat(expectedDateFormat)
                )
        );
    }

    /**
     * Hamcrest {@link Matcher} for date formats to be used in REST-assured API tests.
     */
    private static Matcher<String> hasDateFormat(String format) {
        return new TypeSafeMatcher<>() {
            private String currentlyCheckedString;

            @Override
            protected boolean matchesSafely(String s) {
                currentlyCheckedString = s;

                if (currentlyCheckedString == null)
                    return false;

                try {
                    DateTimeFormatter.ofPattern(format).parse(currentlyCheckedString);
                    return true;
                } catch (Exception ex) {
                    return false;
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("date %s not in expected format %s", currentlyCheckedString,
                    format));
            }
        };
    }
}
