// service/AuthenticationService.java
package com.company.iam.service;

import com.company.iam.exception.IamException;
import com.company.iam.model.dto.LoginRequest;
import com.company.iam.model.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client.id}")
    private String clientId;

    @Value("${keycloak.client.secret}")
    private String clientSecret;

    private final RestTemplate restTemplate;
    private final AuditService auditService;

    /**
     * AUTHENTICATION - Obtenir un token via username/password
     */
    public TokenResponse login(LoginRequest request, String ipAddress) {
        String tokenUrl = buildTokenUrl();
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "password");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("username", request.getUsername());
        params.add("password", request.getPassword());
        params.add("scope", "openid profile email");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                tokenUrl,
                new HttpEntity<>(params, headers),
                Map.class
            );

            Map<String, Object> body = response.getBody();
            
            // Audit log - SUCCESS
            auditService.logAction("LOGIN", request.getUsername(), 
                ipAddress, "SUCCESS", "Login successful");

            log.info("User {} logged in successfully", request.getUsername());

            return TokenResponse.builder()
                .accessToken((String) body.get("access_token"))
                .refreshToken((String) body.get("refresh_token"))
                .tokenType((String) body.get("token_type"))
                .expiresIn((Integer) body.get("expires_in"))
                .refreshExpiresIn((Integer) body.get("refresh_expires_in"))
                .build();

        } catch (HttpClientErrorException e) {
            // Audit log - FAILURE
            auditService.logAction("LOGIN", request.getUsername(), 
                ipAddress, "FAILURE", "Invalid credentials");
            
            log.warn("Failed login attempt for user: {}", request.getUsername());
            throw new IamException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Renouveler le token avec le refresh token
     */
    public TokenResponse refreshToken(String refreshToken) {
        String tokenUrl = buildTokenUrl();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "refresh_token");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("refresh_token", refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                tokenUrl,
                new HttpEntity<>(params, headers),
                Map.class
            );

            Map<String, Object> body = response.getBody();
            return TokenResponse.builder()
                .accessToken((String) body.get("access_token"))
                .refreshToken((String) body.get("refresh_token"))
                .tokenType((String) body.get("token_type"))
                .expiresIn((Integer) body.get("expires_in"))
                .refreshExpiresIn((Integer) body.get("refresh_expires_in"))
                .build();

        } catch (HttpClientErrorException e) {
            throw new IamException("Invalid or expired refresh token", HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Logout - Invalider le token
     */
    public void logout(String refreshToken, String username) {
        String logoutUrl = String.format("%s/realms/%s/protocol/openid-connect/logout",
            keycloakUrl, realm);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("refresh_token", refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        restTemplate.postForEntity(logoutUrl, new HttpEntity<>(params, headers), Void.class);
        
        auditService.logAction("LOGOUT", username, null, "SUCCESS", "User logged out");
        log.info("User {} logged out successfully", username);
    }

    /**
     * Introspection du token
     */
    public Map<String, Object> introspectToken(String token) {
        String introspectUrl = String.format(
            "%s/realms/%s/protocol/openid-connect/token/introspect",
            keycloakUrl, realm
        );

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("token", token);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map> response = restTemplate.postForEntity(
            introspectUrl,
            new HttpEntity<>(params, headers),
            Map.class
        );

        return response.getBody();
    }

    private String buildTokenUrl() {
        return String.format("%s/realms/%s/protocol/openid-connect/token",
            keycloakUrl, realm);
    }
}