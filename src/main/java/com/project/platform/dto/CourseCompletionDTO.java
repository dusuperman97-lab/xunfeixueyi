package com.project.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 课程完成情况统计DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseCompletionDTO {
    private Integer courseId;              // 课程ID
    private String courseName;             // 课程名称
    private String description;            // 课程描述
    private Integer totalChapters;         // 总章节数
    private Integer totalSections;         // 总小节数
    private Integer completedSections;     // 已完成小节数
    private Double completionRate;         // 完成率（百分比）
    private String completionStatus;       // 完成状态：NOT_STARTED(未开始), IN_PROGRESS(进行中), COMPLETED(已完成)
}
