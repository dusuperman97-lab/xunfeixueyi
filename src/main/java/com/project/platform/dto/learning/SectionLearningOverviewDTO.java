package com.project.platform.dto.learning;

import com.project.platform.entity.StudentSectionProgress;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SectionLearningOverviewDTO {
    private Integer studentId;
    private Integer courseId;
    private String courseName;
    private Integer chapterId;
    private String chapterName;
    private Integer sectionId;
    private String sectionName;
    private String activeModule;
    private String activeKnowledgePoint;
    private String sectionState;
    private StudentSectionProgress progress;
    private List<KnowledgePointSummaryDTO> knowledgePoints = new ArrayList<>();
    private List<String> weakKnowledgePoints = new ArrayList<>();
    private List<ModuleSnapshotDTO> moduleSnapshots = new ArrayList<>();
    private ResourceSummaryDTO resourceSummary = new ResourceSummaryDTO();
    private RecommendedActionDTO nextRecommendedAction;
}
