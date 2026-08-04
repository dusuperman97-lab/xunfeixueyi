package com.project.platform.dto;

import lombok.Data;

/**
 * 题目判题结果DTO
 */
@Data
public class QuestionResultDTO {
    private Integer questionId;     // 题目ID
    private String userAnswer;      // 用户答案
    private String correctAnswer;   // 正确答案
    private Boolean isCorrect;      // 是否正确
    private String analysis;        // 答案解析
}
