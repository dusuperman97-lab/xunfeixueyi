package com.project.platform.entity;

import lombok.Data;

@Data
public class CourseWorkflowResource {
    private Long id;
    private String taskId;
    private String stageId;
    private String kind;
    private String title;
    private String url;
    private String source;
    private String snippet;
    private String credibilityLabel;
    private Double score;
    private String note;
    private Long createdAt;
}
