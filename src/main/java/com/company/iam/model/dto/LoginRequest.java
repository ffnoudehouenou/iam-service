package com.company.iam.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    private String clientId;
}

    @NotBlank(message = "Username is required")
    private String username;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String password;
    private boolean enabled;
    private List<String> roles;
    private List<String> groups;
}

// ─────────────────────────────────────────────────────────────

// model/dto/RoleDTO.java
package com.company.iam.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleDTO {
    private String id;
    private String name;
    private String description;
    private boolean composite;
}