package com.project.platform.dto.courseagent;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CourseMultiAgentSectionResponse {
    private String sectionName;
    private Integer durationSeconds;
    private String contentMarkdown;
    private String selectedVideoUrl;
    private String selectedImageUrl;
    private String videoSearchKeyword;
    private String imageSearchKeyword;
    private String verificationSummary;
    private List<CourseMultiAgentVerifiedResource> resources = new ArrayList<>();
}

