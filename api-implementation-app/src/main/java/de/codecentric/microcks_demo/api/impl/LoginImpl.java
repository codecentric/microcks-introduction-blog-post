package de.codecentric.microcks_demo.api.impl;

import de.codecentric.microcks_demo.api.LoginApiDelegate;
import de.codecentric.microcks_demo.model.LoginRequest;
import de.codecentric.microcks_demo.model.LoginResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Objects;
import java.util.Optional;

/**
 * Implementation of the example API's "POST /login" operation for the illustration of Microcks's contract testing
 * capabilities.
 */
@Service
public class LoginImpl implements LoginApiDelegate {
    @Value("${api_key_token}")
    private String apiKeyToken;

    @Value("${some_customer_email}")
    private String someCustomerEmail;

    @Value("${some_customer_password}")
    private String someCustomerPassword;

    @Value("${some_customer_token}")
    private String someCustomerToken;

    private final NativeWebRequest request;

    public LoginImpl(NativeWebRequest request) {
        this.request = request;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.ofNullable(request);
    }

    /**
     * Implementation of the example API's "POST /login" operation.
     */
    @Override
    public ResponseEntity<LoginResponse> loginPost(LoginRequest loginRequest) {
        var request = getRequest().orElse(null);
        if (loginRequest == null || request == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        var providedApiKeyToken = request.getHeader("api_key");
        if (!Objects.equals(providedApiKeyToken, apiKeyToken))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (!Objects.equals(loginRequest.getEmail(), someCustomerEmail) ||
            !Objects.equals(loginRequest.getPassword(), someCustomerPassword))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity
            .status(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(new LoginResponse(someCustomerToken));
    }
}
