package com.project.platform.dto.courseworkflow;

import lombok.Data;

@Data
public class CourseMultiAgentStartRequest {
    private String message;
    private String courseMode;
    private String difficulty;
}
