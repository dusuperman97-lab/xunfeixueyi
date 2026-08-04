package com.project.platform.dto.courseagent;

import com.project.platform.dto.ChapterOutlineDTO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CourseMultiAgentOutlineResponse {
    private String courseName;
    private String description;
    private String level;
    private String duration;
    private String style;
    private List<ChapterOutlineDTO> outline = new ArrayList<>();
}

