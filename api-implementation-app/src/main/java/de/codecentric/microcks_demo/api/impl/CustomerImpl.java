package de.codecentric.microcks_demo.api.impl;

import de.codecentric.microcks_demo.api.CustomerApiDelegate;
import de.codecentric.microcks_demo.model.CustomerResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Objects;
import java.util.Optional;

/**
 * Implementation of the example API's "GET /customer" operation for the illustration of Microcks's contract testing
 * capabilities.
 */
@Service
public class CustomerImpl implements CustomerApiDelegate {
    @Value("${some_customer_email}")
    private String someCustomerEmail;

    @Value("${some_customer_token}")
    private String someCustomerToken;

    private final NativeWebRequest request;

    public CustomerImpl(NativeWebRequest request) {
        this.request = request;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.ofNullable(request);
    }

    /**
     * Implementation of the example API's "GET /customer" operation.
     */
    @Override
    public ResponseEntity<CustomerResponse> customerGet() {
        var request = getRequest().orElse(null);
        var providedCustomerToken = request != null ? request.getHeader("customer_token") : null;
        if (!Objects.equals(providedCustomerToken, someCustomerToken))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CustomerResponse(someCustomerEmail, "John", "Doe", "", "Random Street 1"));
    }
}
