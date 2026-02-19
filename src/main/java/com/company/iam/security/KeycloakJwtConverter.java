// security/KeycloakJwtConverter.java
package com.company.iam.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter defaultConverter = 
        new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // Extraction des rôles depuis le realm_access et resource_access
        Collection<GrantedAuthority> authorities = Stream.concat(
            defaultConverter.convert(jwt).stream(),
            extractKeycloakRoles(jwt).stream()
        ).collect(Collectors.toSet());

        return new JwtAuthenticationToken(jwt, authorities, getPrincipalName(jwt));
    }

    /**
     * Extrait les rôles Keycloak du token JWT
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractKeycloakRoles(Jwt jwt) {
        List<GrantedAuthority> roles = new ArrayList<>();

        // Rôles du Realm
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            List<String> realmRoles = (List<String>) realmAccess.get("roles");
            if (realmRoles != null) {
                realmRoles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .forEach(roles::add);
            }
        }

        // Rôles des Resources (clients)
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            resourceAccess.forEach((clientId, clientRoles) -> {
                Map<String, Object> clientRolesMap = (Map<String, Object>) clientRoles;
                List<String> clientRoleList = (List<String>) clientRolesMap.get("roles");
                if (clientRoleList != null) {
                    clientRoleList.stream()
                        .map(role -> new SimpleGrantedAuthority(
                            "ROLE_" + clientId.toUpperCase() + "_" + role.toUpperCase()))
                        .forEach(roles::add);
                }
            });
        }

        return roles;
    }

    private String getPrincipalName(Jwt jwt) {
        return jwt.getClaimAsString("preferred_username") != null 
            ? jwt.getClaimAsString("preferred_username") 
            : jwt.getSubject();
    }
}