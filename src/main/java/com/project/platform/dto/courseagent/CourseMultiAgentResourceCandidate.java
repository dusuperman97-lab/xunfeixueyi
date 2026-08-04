package com.project.platform.dto.courseagent;

import lombok.Data;

@Data
public class CourseMultiAgentResourceCandidate {
    private String title;
    private String url;
    private String snippet;
    private Double score;
    private String sourceType;
    private String retrievedBy;
}

