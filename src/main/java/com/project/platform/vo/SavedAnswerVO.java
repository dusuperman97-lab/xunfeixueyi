package com.project.platform.vo;

import lombok.Data;

/**
 * 已保存答案VO（用于考试状态查询中的答案列表）
 */
@Data
public class SavedAnswerVO {

    /** 题目ID */
    private Integer questionId;

    /** 用户答案 */
    private String userAnswer;
}
