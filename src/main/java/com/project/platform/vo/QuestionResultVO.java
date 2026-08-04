package com.project.platform.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 单题结果VO — 用于提交考试返回的逐题详情
 */
@Data
public class QuestionResultVO {

    /** 题目ID */
    private Integer questionId;

    /** 题目类型：CHOICE / MULTIPLE / JUDGE / FILL */
    private String questionType;

    /** 题目内容 */
    private String content;

    /** 考察知识点 */
    private String knowledgePoint;

    /** 正确答案 */
    private String correctAnswer;

    /** 用户答案 */
    private String userAnswer;

    /** 是否正确 */
    private Boolean isCorrect;

    /** 得分 */
    private BigDecimal score;

    /** 该题耗时（秒），基于首次暂存时间到交卷时间的差值 */
    private Integer timeSpent;
}
