package com.project.platform.vo;

import lombok.Data;

/**
 * 题目详情VO
 * 用于返回每道题的详细信息,包括用户答案和正确答案
 */
@Data
public class QuestionDetailVO {
    
    private Integer questionId;         // 题目ID
    private String questionType;        // 题目类型:CHOICE(单选), MULTIPLE(多选), JUDGE(判断), FILL(填空)
    private String questionTypeLabel;   // 题目类型标签(中文)
    private String content;             // 题目内容
    private String options;             // 选项(JSON数组格式)
    private String userAnswer;          // 用户答案
    private String correctAnswer;       // 正确答案
    private String analysis;            // 答案解析
    private String knowledgePoint;      // 考察知识点
    private Boolean isCorrect;          // 是否正确:false-错误, true-正确, null-未作答
}
