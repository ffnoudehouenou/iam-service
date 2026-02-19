// controller/UserController.java
package com.company.iam.controller;

import com.company.iam.model.dto.UserDTO;
import com.company.iam.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "AAA - User Management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER')")
    @Operation(summary = "Créer un utilisateur")
    public ResponseEntity<UserDTO> createUser(
            @Valid @RequestBody UserDTO userDTO,
            @AuthenticationPrincipal Jwt jwt) {

        String createdBy = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(userService.createUser(userDTO, createdBy));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER')")
    @Operation(summary = "Lister tous les utilisateurs")
    public ResponseEntity<List<UserDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(userService.getAllUsers(page, size));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER') or #userId == authentication.name")
    @Operation(summary = "Obtenir un utilisateur par ID")
    public ResponseEntity<UserDTO> getUserById(@PathVariable String userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER')")
    @Operation(summary = "Mettre à jour un utilisateur")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UserDTO userDTO,
            @AuthenticationPrincipal Jwt jwt) {

        String updatedBy = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(userService.updateUser(userId, userDTO, updatedBy));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un utilisateur")
    public ResponseEntity<Void> deleteUser(
            @PathVariable String userId,
            @AuthenticationPrincipal Jwt jwt) {

        String deletedBy = jwt.getClaimAsString("preferred_username");
        userService.deleteUser(userId, deletedBy);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activer/Désactiver un utilisateur")
    public ResponseEntity<Void> toggleUserStatus(
            @PathVariable String userId,
            @RequestParam boolean enabled,
            @AuthenticationPrincipal Jwt jwt) {

        String modifiedBy = jwt.getClaimAsString("preferred_username");
        userService.toggleUserStatus(userId, enabled, modifiedBy);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{userId}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Réinitialiser le mot de passe")
    public ResponseEntity<Void> resetPassword(
            @PathVariable String userId,
            @RequestParam String newPassword,
            @RequestParam(defaultValue = "true") boolean temporary) {

        userService.resetPassword(userId, newPassword, temporary);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER')")
    @Operation(summary = "Rechercher des utilisateurs")
    public ResponseEntity<List<UserDTO>> searchUsers(@RequestParam String query) {
        return ResponseEntity.ok(userService.searchUsers(query));
    }
}