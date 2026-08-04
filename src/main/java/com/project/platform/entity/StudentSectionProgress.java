package com.project.platform.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StudentSectionProgress {
    private Integer studentId;
    private Integer courseId;
    private Integer chapterId;
    private Integer sectionId;
    private boolean contentViewed;
    private boolean practiceCompleted;
    private boolean resourceReviewed;
    private boolean completed;
    private String masteryState = "NOT_STARTED";
    private String lastPosition;
    private String recommendedNextAction = "CONTENT_LEARNING";
    private Long lastEventTime;
    private List<String> weakKnowledgePoints = new ArrayList<>();

    public static StudentSectionProgress create(Integer studentId, Integer courseId,
                                                Integer chapterId, Integer sectionId) {
        StudentSectionProgress progress = new StudentSectionProgress();
        progress.setStudentId(studentId);
        progress.setCourseId(courseId);
        progress.setChapterId(chapterId);
        progress.setSectionId(sectionId);
        progress.setLastEventTime(System.currentTimeMillis());
        return progress;
    }
}
