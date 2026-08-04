package com.project.platform.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class LearningActionEvent {
    private String eventId;
    private Integer studentId;
    private Integer courseId;
    private Integer chapterId;
    private Integer sectionId;
    private String activeModule;
    private String knowledgePoint;
    private String eventType;
    private String triggerSource;
    private Integer resourceId;
    private String resourceUrl;
    private String lastPosition;
    private Double accuracyRate;
    private List<String> weakKnowledgePoints = new ArrayList<>();
    private Map<String, Object> payload = new LinkedHashMap<>();
    private Long occurredAt;
}
