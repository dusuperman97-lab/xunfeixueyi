package com.project.platform.dto.learning;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedActionDTO {
    private String actionType;
    private String label;
    private String targetModule;
    private Integer sectionId;
    private String knowledgePoint;
}
