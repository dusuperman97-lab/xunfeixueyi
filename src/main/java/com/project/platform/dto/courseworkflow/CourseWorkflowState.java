package com.project.platform.dto.courseworkflow;

import com.project.platform.dto.ChapterOutlineDTO;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LangGraph 工作流状态 — 跨阶段传递上下文
 */
@Data
public class CourseWorkflowState {
    private String taskId;
    private String message;
    private Integer userId;
    private String courseMode;
    private String difficulty;

    // 阶段1产物
    private String topic;
    private String level;
    private String duration;
    private String style;
    private String courseName;
    private String description;
    private Integer courseId;
    private List<ChapterOutlineDTO> outline;

    // 阶段2产物
    private List<String> searchQueries;

    // 阶段3产物
    private List<CourseWorkflowSnapshotDTO.Evidence> resources = new ArrayList<>();

    // 阶段4产物
    private boolean reviewRequired;
    private String reviewDecision;
    private String reviewFeedback;
    private List<CourseMultiAgentReviewRequest.ResourceAction> resourceActions;
    private boolean reviewApproved;

    // 阶段状态
    private Map<String, String> stageStatus = new LinkedHashMap<>();
}
