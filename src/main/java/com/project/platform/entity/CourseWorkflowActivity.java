package com.project.platform.entity;

import lombok.Data;

@Data
public class CourseWorkflowActivity {
    private Long id;
    private String taskId;
    private String activityId;
    private String stageId;
    private String agentName;
    private String agentRole;
    private String type;
    private String label;
    private String message;
    private Long timestamp;
}
