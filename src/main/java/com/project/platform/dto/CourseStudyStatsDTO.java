package com.project.platform.dto;

import lombok.Data;
import java.util.List;

/**
 * 课程维度的学习时长统计
 */
@Data
public class CourseStudyStatsDTO {
    private Integer courseId;
    private String courseName;
    private Integer totalMinutes;
    private Integer avgMinutes;
    private List<String> dates;
    private List<Integer> durations;
}
