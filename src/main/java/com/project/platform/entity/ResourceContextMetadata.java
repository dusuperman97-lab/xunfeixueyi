package com.project.platform.entity;

import lombok.Data;

@Data
public class ResourceContextMetadata {
    private Integer resourceId;
    private Integer courseId;
    private Integer chapterId;
    private Integer sectionId;
    private String knowledgePoint;
    private String purpose = "CONTEXT_REVIEW";
    private String difficulty = "UNKNOWN";
    private String recommendationReason = "Related to current learning context.";
    private String credibilityStatus = "UNKNOWN";
    private String learningStatus = "NOT_STARTED";
    private boolean remedial;
    private double relevanceScore;
}
