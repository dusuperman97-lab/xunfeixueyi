package com.project.platform.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 学生实体
 */
@Data
public class Student {
    private Integer id;
    private String studentNo;
    private String username;
    private String password;
    private String avatarUrl;
    private String tel;
    private String email;
    private String status;
    private LocalDateTime createTime;
    private Boolean isRegistered;

    // 学习时长统计相关字段（用于展示）
    private List<String> studyDurationDates; // 近七天日期列表，标准格式如 ["2026-04-22", "2026-04-23", ..., "2026-04-28"]
    private List<Integer> studyDurationMinutes; // 近七天学习时长列表（分钟），如 [100, 200, 150, 80, 120, 90, 300]
    private Integer totalStudyMinutes; // 近七天总学习时长（分钟）
    private Integer avgStudyMinutes; // 近七天平均每天学习时长（分钟）
}
