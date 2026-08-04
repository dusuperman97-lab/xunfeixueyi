package com.project.platform.dto.courseworkflow;

import lombok.Data;

import java.util.List;

@Data
public class CourseMultiAgentReviewRequest {
    private String decision;
    private String feedback;
    private List<ResourceAction> resourceActions;

    @Data
    public static class ResourceAction {
        private String url;
        private String title;
        private String action;
        private String newLabel;
    }
}
