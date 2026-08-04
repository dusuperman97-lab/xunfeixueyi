package com.project.platform.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 课程实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {
    private Integer id;
    private String courseName;
    private String description;
    private Integer chapterCount;
    private Integer creatorId;        // 创建人ID（学生ID）
    private Integer examDuration;     // 考试时长（秒），null 则使用系统默认值
    private LocalDateTime createTime;

    public Course(String courseName, String description, Integer chapterCount, Integer creatorId, LocalDateTime createTime) {
        this.courseName = courseName;
        this.description = description;
        this.chapterCount = chapterCount;
        this.creatorId = creatorId;
        this.createTime = createTime;
    }
}
