package com.project.platform.dto.courseworkflow;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CourseWorkflowSnapshotDTO {
    private String taskId;
    private String taskType;
    private String title;
    private String subtitle;
    private String status;
    private String summary;
    private Long createdAt;
    private Long updatedAt;
    private Long finishedAt;
    private List<Step> steps = new ArrayList<>();
    private List<Activity> activities = new ArrayList<>();
    private List<Evidence> evidences = new ArrayList<>();
    private List<Expert> experts = new ArrayList<>();
    private String phase; // RUNNING / REVIEW_PENDING / DELIVERY_COMPLETE
    private boolean reviewRequired;
    private List<Evidence> reviewEvidences = new ArrayList<>();
    private Integer courseId;
    private String courseName;

    @Data
    public static class Step {
        private String id;
        private String title;
        private String agent;
        private String status;
        private String detail;
    }

    @Data
    public static class Activity {
        private String id;
        private String stageId;
        private String agentName;
        private String agentRole;
        private String type;
        private String label;
        private String message;
        private Long timestamp;
    }

    @Data
    public static class Evidence {
        private String id;
        private String kind;
        private String title;
        private String url;
        private String source;
        private String snippet;
        private String credibilityLabel;
        private String note;
        private Double score;
    }

    @Data
    public static class Expert {
        private String id;
        private String name;
        private String role;
        private String accent;
    }
}

