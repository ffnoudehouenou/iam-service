// controller/AuthController.java
package com.company.iam.controller;

import com.company.iam.model.dto.LoginRequest;
import com.company.iam.model.dto.TokenResponse;
import com.company.iam.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "AAA - Authentication endpoints")
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    @Operation(summary = "Login - Obtenir un JWT Token")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);
        TokenResponse response = authenticationService.login(request, ipAddress);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rafraîchir le token")
    public ResponseEntity<TokenResponse> refreshToken(
            @RequestParam("refresh_token") String refreshToken) {

        return ResponseEntity.ok(authenticationService.refreshToken(refreshToken));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout - Invalider le token")
    public ResponseEntity<Void> logout(
            @RequestParam("refresh_token") String refreshToken,
            @RequestHeader("X-Username") String username) {

        authenticationService.logout(refreshToken, username);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/introspect")
    @Operation(summary = "Vérifier la validité d'un token")
    public ResponseEntity<Map<String, Object>> introspect(
            @RequestParam("token") String token) {

        return ResponseEntity.ok(authenticationService.introspectToken(token));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip != null && !ip.isEmpty()) ? ip.split(",")[0] : request.getRemoteAddr();
    }
}