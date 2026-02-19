// service/UserService.java
package com.company.iam.service;

import com.company.iam.exception.IamException;
import com.company.iam.model.dto.UserDTO;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final Keycloak keycloakAdminClient;
    private final AuditService auditService;

    @Value("${keycloak.realm}")
    private String realm;

    /**
     * Créer un nouvel utilisateur dans Keycloak
     */
    public UserDTO createUser(UserDTO userDTO, String createdBy) {
        RealmResource realmResource = keycloakAdminClient.realm(realm);

        // Construire la représentation utilisateur
        UserRepresentation user = buildUserRepresentation(userDTO);

        // Créer l'utilisateur
        Response response = realmResource.users().create(user);

        if (response.getStatus() == 409) {
            throw new IamException("User already exists: " + userDTO.getUsername(), 
                HttpStatus.CONFLICT);
        }

        if (response.getStatus() != 201) {
            throw new IamException("Failed to create user", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Récupérer l'ID de l'utilisateur créé
        String userId = extractUserId(response);

        // Assigner les rôles
        if (userDTO.getRoles() != null && !userDTO.getRoles().isEmpty()) {
            assignRoles(userId, userDTO.getRoles());
        }

        // Définir le mot de passe
        if (userDTO.getPassword() != null) {
            setPassword(userId, userDTO.getPassword(), false);
        }

        auditService.logAction("CREATE_USER", createdBy, null, "SUCCESS",
            "Created user: " + userDTO.getUsername());

        log.info("User created successfully: {}", userDTO.getUsername());
        return getUserById(userId);
    }

    /**
     * Obtenir un utilisateur par ID
     */
    public UserDTO getUserById(String userId) {
        try {
            UserResource userResource = keycloakAdminClient.realm(realm).users().get(userId);
            UserRepresentation user = userResource.toRepresentation();
            return mapToDTO(user, userResource);
        } catch (Exception e) {
            throw new IamException("User not found: " + userId, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Lister tous les utilisateurs avec pagination
     */
    public List<UserDTO> getAllUsers(int page, int size) {
        return keycloakAdminClient.realm(realm)
            .users()
            .list(page * size, size)
            .stream()
            .map(user -> {
                UserResource userResource = keycloakAdminClient.realm(realm)
                    .users().get(user.getId());
                return mapToDTO(user, userResource);
            })
            .collect(Collectors.toList());
    }

    /**
     * Mettre à jour un utilisateur
     */
    public UserDTO updateUser(String userId, UserDTO userDTO, String updatedBy) {
        UserResource userResource = keycloakAdminClient.realm(realm).users().get(userId);
        UserRepresentation user = userResource.toRepresentation();

        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEmail(userDTO.getEmail());
        user.setEnabled(userDTO.isEnabled());

        userResource.update(user);

        if (userDTO.getRoles() != null) {
            // Reset et reassigner les rôles
            resetAndAssignRoles(userId, userDTO.getRoles());
        }

        auditService.logAction("UPDATE_USER", updatedBy, null, "SUCCESS",
            "Updated user: " + userId);

        return getUserById(userId);
    }

    /**
     * Supprimer un utilisateur
     */
    public void deleteUser(String userId, String deletedBy) {
        try {
            keycloakAdminClient.realm(realm).users().get(userId).remove();
            auditService.logAction("DELETE_USER", deletedBy, null, "SUCCESS",
                "Deleted user: " + userId);
        } catch (Exception e) {
            throw new IamException("Failed to delete user: " + userId, 
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Activer/Désactiver un utilisateur
     */
    public void toggleUserStatus(String userId, boolean enabled, String modifiedBy) {
        UserResource userResource = keycloakAdminClient.realm(realm).users().get(userId);
        UserRepresentation user = userResource.toRepresentation();
        user.setEnabled(enabled);
        userResource.update(user);

        String action = enabled ? "ENABLE_USER" : "DISABLE_USER";
        auditService.logAction(action, modifiedBy, null, "SUCCESS",
            (enabled ? "Enabled" : "Disabled") + " user: " + userId);
    }

    /**
     * Réinitialiser le mot de passe
     */
    public void resetPassword(String userId, String newPassword, boolean temporary) {
        setPassword(userId, newPassword, temporary);
        auditService.logAction("RESET_PASSWORD", userId, null, "SUCCESS",
            "Password reset for user: " + userId);
    }

    /**
     * Rechercher des utilisateurs
     */
    public List<UserDTO> searchUsers(String query) {
        return keycloakAdminClient.realm(realm)
            .users()
            .search(query)
            .stream()
            .map(user -> mapToDTO(user, null))
            .collect(Collectors.toList());
    }

    // ─── Méthodes privées ────────────────────────────────────────

    private UserRepresentation buildUserRepresentation(UserDTO userDTO) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEnabled(userDTO.isEnabled());
        user.setEmailVerified(true);
        return user;
    }

    private void setPassword(String userId, String password, boolean temporary) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(temporary);
        keycloakAdminClient.realm(realm).users().get(userId).resetPassword(credential);
    }

    private void assignRoles(String userId, List<String> roleNames) {
        RolesResource rolesResource = keycloakAdminClient.realm(realm).roles();
        List<RoleRepresentation> roles = roleNames.stream()
            .map(roleName -> {
                try {
                    return rolesResource.get(roleName).toRepresentation();
                } catch (Exception e) {
                    log.warn("Role not found: {}", roleName);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (!roles.isEmpty()) {
            keycloakAdminClient.realm(realm).users()
                .get(userId).roles().realmLevel().add(roles);
        }
    }

    private void resetAndAssignRoles(String userId, List<String> newRoles) {
        // Supprimer tous les rôles existants
        UserResource userResource = keycloakAdminClient.realm(realm).users().get(userId);
        List<RoleRepresentation> currentRoles = userResource.roles().realmLevel().listEffective();
        if (!currentRoles.isEmpty()) {
            userResource.roles().realmLevel().remove(currentRoles);
        }
        // Assigner les nouveaux rôles
        assignRoles(userId, newRoles);
    }

    private String extractUserId(Response response) {
        String location = response.getHeaderString("Location");
        return location.substring(location.lastIndexOf("/") + 1);
    }

    private UserDTO mapToDTO(UserRepresentation user, UserResource userResource) {
        List<String> roles = new ArrayList<>();
        if (userResource != null) {
            try {
                roles = userResource.roles().realmLevel().listEffective()
                    .stream()
                    .map(RoleRepresentation::getName)
                    .collect(Collectors.toList());
            } catch (Exception e) {
                log.warn("Could not fetch roles for user: {}", user.getId());
            }
        }

        return UserDTO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .enabled(user.isEnabled())
            .roles(roles)
            .build();
    }
}