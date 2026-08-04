package com.project.platform.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuestionTestRecordDTO {
    private Integer rowNumber;      // 序号
    private Integer totalTimeSpent; // 练习时长（秒）
    private String sectionName;     // 练习名称（小节名称）
    private LocalDateTime createTime; // 练习创建时间
    private String accuracyRate;    // 练习正确率
}
