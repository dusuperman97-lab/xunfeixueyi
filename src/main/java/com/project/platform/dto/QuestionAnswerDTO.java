package com.project.platform.dto;

import lombok.Data;

/**
 * 题目答案提交DTO
 */
@Data
public class QuestionAnswerDTO {

    private Integer questionId;     // 题目ID
    private Integer courseId;       // 课程ID
    private Integer sectionId;      // 小节ID
    private String userAnswer;      // 用户答案
}
