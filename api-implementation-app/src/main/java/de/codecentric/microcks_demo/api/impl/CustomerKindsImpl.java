package de.codecentric.microcks_demo.api.impl;

import de.codecentric.microcks_demo.api.CustomerKindsApiDelegate;
import de.codecentric.microcks_demo.model.CustomerKindDetailsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementation of the example API's "GET /customer_kinds" and "GET /customer_kinds/{id}" operations for the
 * illustration of Microcks's contract testing capabilities.
 */
@Service
public class CustomerKindsImpl implements CustomerKindsApiDelegate {
    @Value("${api_key_token}")
    private String apiKeyToken;

    private final NativeWebRequest request;

    public CustomerKindsImpl(NativeWebRequest request) {
        this.request = request;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.ofNullable(request);
    }

    /**
     * Implementation of the example API's "GET /customer_kinds" operation.
     */
    @Override
    public ResponseEntity<Map<String, String>> customerKindsGet() {
        var request = getRequest().orElse(null);
        var providedApiKeyToken = request != null ? request.getHeader("api_key") : null;
        if (!Objects.equals(providedApiKeyToken, apiKeyToken))
            return ResponseEntity.badRequest().build();

        return ResponseEntity
            .status(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of(
                "1", "Private person",
                "2", "Commercial enterprise",
                "3", "Public institution"
            ));
    }

    /**
     * Implementation of the example API's "GET /customer_kinds/{id}" operation.
     */
    @Override
    public ResponseEntity<CustomerKindDetailsResponse> customerKindDetailsGet(String id) {
        var request = getRequest().orElse(null);
        var providedApiKeyToken = request != null ? request.getHeader("api_key") : null;
        if (!Objects.equals(providedApiKeyToken, apiKeyToken))
            return ResponseEntity.badRequest().build();

        return switch (id) {
            case "1" ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CustomerKindDetailsResponse("1", "Private person", "11.07.2024 07:50:51", ""));
            case "2" ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        new CustomerKindDetailsResponse("2", "Commercial enterprise", "11.07.2024 07:50:51",
                            "11.07.2024 08:01:50")
                    );
            case "3" ->
                ResponseEntity
                    .status(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CustomerKindDetailsResponse("3", "Public institution", "19.07.2024 12:30:33", ""));
            default ->
                ResponseEntity.badRequest().build();
        };
    }
}
