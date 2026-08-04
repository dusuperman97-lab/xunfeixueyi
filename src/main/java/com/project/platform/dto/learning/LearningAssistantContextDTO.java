package com.project.platform.dto.learning;

import com.project.platform.entity.ResourcePush;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
public class LearningAssistantContextDTO {
    private Integer studentId;
    private Integer courseId;
    private String courseName;
    private Integer chapterId;
    private String chapterName;
    private Integer sectionId;
    private String sectionName;
    private String activeModule;
    private String activeKnowledgePoint;
    private boolean learningContextAvailable;
    private String contextStatus;
    private String contextError;
    private String greeting;
    private String agentEndpoint = "/course/digital-human/chat";
    private String eventEndpoint = "/learning-assistant/events";
    private LearningContextDTO learningContext;
    private StudentProfileSummaryDTO profileSummary = new StudentProfileSummaryDTO();
    private List<String> weakKnowledgePoints = new ArrayList<>();
    private RecommendedActionDTO nextRecommendedAction;
    private List<ResourcePush> contextualResources = new ArrayList<>();
    private List<AssistantTipDTO> tips = new ArrayList<>();
    private List<AssistantActionDTO> actions = new ArrayList<>();

    @Data
    public static class StudentProfileSummaryDTO {
        private boolean profileAvailable;
        private String learningGoal;
        private String learningStyle;
        private String preferredStyle;
        private List<String> weaknesses = new ArrayList<>();
        private Integer overallScore;
        private Integer studyMinutes;
        private Integer concentration;
        private Integer knowledgeMastery;
        private String riskWarning;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssistantTipDTO {
        private String id;
        private String text;
        private String source;
    }

    @Data
    public static class AssistantActionDTO {
        private String key;
        private String label;
        private String title;
        private String actionType;
        private String targetModule;
        private String knowledgePoint;
        private String eventType;
        private String message;
        private String endpoint;
        private String route;
    }
}
