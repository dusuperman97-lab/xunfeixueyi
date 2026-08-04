package com.project.platform.dto.courseagent;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CourseMultiAgentSectionRequest {
    private String courseName;
    private String courseDescription;
    private String chapterName;
    private String sectionName;
    private String language;
    private List<CourseMultiAgentResourceCandidate> resourceCandidates = new ArrayList<>();
    private List<CourseMultiAgentMediaCandidate> videoCandidates = new ArrayList<>();
    private List<CourseMultiAgentMediaCandidate> imageCandidates = new ArrayList<>();
}

