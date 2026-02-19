// controller/RoleController.java
package com.company.iam.controller;

import com.company.iam.model.dto.RoleDTO;
import com.company.iam.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "AAA - Authorization - Role Management")
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un rôle")
    public ResponseEntity<RoleDTO> createRole(
            @RequestBody RoleDTO roleDTO,
            @AuthenticationPrincipal Jwt jwt) {

        String createdBy = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(roleService.createRole(roleDTO, createdBy));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER_MANAGER')")
    @Operation(summary = "Lister tous les rôles")
    public ResponseEntity<List<RoleDTO>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir un rôle par nom")
    public ResponseEntity<RoleDTO> getRoleByName(@PathVariable String roleName) {
        return ResponseEntity.ok(roleService.getRoleByName(roleName));
    }

    @DeleteMapping("/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un rôle")
    public ResponseEntity<Void> deleteRole(
            @PathVariable String roleName,
            @AuthenticationPrincipal Jwt jwt) {

        String deletedBy = jwt.getClaimAsString("preferred_username");
        roleService.deleteRole(roleName, deletedBy);
        return ResponseEntity.noContent().build();
    }
}
