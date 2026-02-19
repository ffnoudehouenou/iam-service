// service/RoleService.java
package com.company.iam.service;

import com.company.iam.exception.IamException;
import com.company.iam.model.dto.RoleDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final Keycloak keycloakAdminClient;
    private final AuditService auditService;

    @Value("${keycloak.realm}")
    private String realm;

    /**
     * Créer un nouveau rôle
     */
    public RoleDTO createRole(RoleDTO roleDTO, String createdBy) {
        RoleRepresentation role = new RoleRepresentation();
        role.setName(roleDTO.getName());
        role.setDescription(roleDTO.getDescription());

        try {
            keycloakAdminClient.realm(realm).roles().create(role);
            auditService.logAction("CREATE_ROLE", createdBy, null, "SUCCESS",
                "Created role: " + roleDTO.getName());
            return getRoleByName(roleDTO.getName());
        } catch (Exception e) {
            throw new IamException("Failed to create role: " + roleDTO.getName(),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Lister tous les rôles
     */
    public List<RoleDTO> getAllRoles() {
        return keycloakAdminClient.realm(realm).roles().list()
            .stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Obtenir un rôle par nom
     */
    public RoleDTO getRoleByName(String roleName) {
        try {
            RoleRepresentation role = keycloakAdminClient.realm(realm)
                .roles().get(roleName).toRepresentation();
            return mapToDTO(role);
        } catch (Exception e) {
            throw new IamException("Role not found: " + roleName, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Supprimer un rôle
     */
    public void deleteRole(String roleName, String deletedBy) {
        try {
            keycloakAdminClient.realm(realm).roles().get(roleName).remove();
            auditService.logAction("DELETE_ROLE", deletedBy, null, "SUCCESS",
                "Deleted role: " + roleName);
        } catch (Exception e) {
            throw new IamException("Failed to delete role: " + roleName,
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private RoleDTO mapToDTO(RoleRepresentation role) {
        return RoleDTO.builder()
            .id(role.getId())
            .name(role.getName())
            .description(role.getDescription())
            .composite(role.isComposite())
            .build();
    }
}