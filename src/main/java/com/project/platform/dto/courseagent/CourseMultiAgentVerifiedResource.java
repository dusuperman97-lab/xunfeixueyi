package com.project.platform.dto.courseagent;

import lombok.Data;

@Data
public class CourseMultiAgentVerifiedResource {
    private String title;
    private String url;
    private String snippet;
    private Double authorityScore;
    private String credibilityLabel;
    private String reason;
    private Boolean selected;
}

