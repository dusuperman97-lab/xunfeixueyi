package com.project.platform.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 错题详情VO
 * 在测试记录基础上富化了题目的选项和解析，用于错题本页面
 */
@Data
public class WrongQuestionVO {

    private Long id;
    private Integer userId;
    private Integer courseId;
    private Integer sectionId;
    private Integer questionId;
    private String questionCategory;
    private String questionType;
    private String content;
    private String knowledgePoint;
    private String correctAnswer;
    private String userAnswer;
    private Boolean isCorrect;
    private BigDecimal score;
    private Integer timeSpent;
    private String testBatchNo;
    private LocalDateTime createTime;

    /** 题目选项（JSON数组字符串，从 Question 表富化） */
    private String options;

    /** 题目解析（从 Question 表富化） */
    private String analysis;
}
