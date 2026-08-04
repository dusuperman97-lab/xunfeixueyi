package com.project.platform.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class CourseDigitalHumanChatRequest {
    private String memoryId;
    private Integer courseId;
    private Integer chapterId;
    private Integer sectionId;
    private String digitalHumanId;
    private CourseDigitalHumanChatType type;
    private String message;
    private JsonNode selection;
    private JsonNode practice;
}
