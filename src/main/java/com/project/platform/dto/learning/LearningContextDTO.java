package com.project.platform.dto.learning;

import com.project.platform.entity.StudentSectionProgress;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LearningContextDTO {
    private Integer studentId;
    private Integer courseId;
    private String courseName;
    private Integer chapterId;
    private String chapterName;
    private Integer sectionId;
    private String sectionName;
    private String activeModule;
    private String activeKnowledgePoint;
    private StudentSectionProgress progressSummary;
    private List<String> weakKnowledgePoints = new ArrayList<>();
    private RecommendedActionDTO nextRecommendedAction;
    private SectionLearningOverviewDTO sectionOverview;
}
