package com.project.platform.dto.learning;

import lombok.Data;

@Data
public class LearningAssistantContextRequest {
    private Integer courseId;
    private Integer sectionId;
    private String activeModule;
    private String activeKnowledgePoint;
}
