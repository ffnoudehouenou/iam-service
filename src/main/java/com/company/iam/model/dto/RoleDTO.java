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