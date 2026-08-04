package com.project.platform.entity;

import lombok.Data;

@Data
public class CourseWorkflowTask {
    private Long id;
    private String taskId;
    private Integer courseId;
    private Integer userId;
    private String status;
    private String currentStage;
    private String requestMessage;
    private String courseMode;
    private String difficulty;
    private String title;
    private String subtitle;
    private String summary;
    private String resultJson;
    private Long createdAt;
    private Long updatedAt;
    private Long finishedAt;
}
