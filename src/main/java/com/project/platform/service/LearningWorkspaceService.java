package com.project.platform.service;

import com.project.platform.dto.learning.LearningContextDTO;
import com.project.platform.dto.learning.SectionLearningOverviewDTO;
import com.project.platform.entity.LearningActionEvent;
import com.project.platform.entity.QuestionTestRecord;
import com.project.platform.entity.ResourcePush;
import com.project.platform.entity.StudentSectionProgress;

import java.util.List;

public interface LearningWorkspaceService {

    LearningContextDTO getContext(Integer studentId, Integer courseId, Integer sectionId,
                                  String activeModule, String activeKnowledgePoint);

    SectionLearningOverviewDTO getSectionOverview(Integer studentId, Integer courseId, Integer sectionId,
                                                  String activeModule, String activeKnowledgePoint);

    StudentSectionProgress recordEvent(LearningActionEvent event);

    void recordPracticeSubmission(Integer studentId, Integer courseId, Integer sectionId,
                                  List<QuestionTestRecord> records);

    StudentSectionProgress markSectionCompleted(Integer studentId, Integer sectionId, boolean completed);

    List<String> listWeakKnowledgePoints(Integer studentId, Integer sectionId);

    String resolveResourceLearningStatus(Integer studentId, ResourcePush resource);
}
