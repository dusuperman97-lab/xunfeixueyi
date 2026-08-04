package com.project.platform.entity;

import lombok.Data;

@Data
public class AgentRole {
    private Integer id;
    private String roleCode;
    private String roleName;
    private String stageId;
    private String stageName;
    private String description;
    private String accentColor;
    private String avatar;
    private Integer sortOrder;
    private Integer isActive;
    private Long createdAt;
    private Long updatedAt;
}
