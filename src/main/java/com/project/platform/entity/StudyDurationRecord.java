package com.project.platform.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学生学习时长记录实体
 */
@Data
public class StudyDurationRecord {
    private Integer id;
    private Integer studentId;        // 学生ID
    private Integer courseId;         // 课程ID
    private LocalDate studyDate;      // 学习日期
    private Integer durationMinutes;  // 学习时长（分钟）
    private String studyType;         // 学习类型
    private Integer sectionId;        // 关联小节ID
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
