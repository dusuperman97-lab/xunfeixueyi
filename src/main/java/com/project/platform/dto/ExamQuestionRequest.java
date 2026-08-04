package com.project.platform.dto;

import lombok.Data;

/**
 * 考试题生成请求DTO
 */
@Data
public class ExamQuestionRequest {
    private Integer courseId;         // 课程ID
    private String knowledgePoints;   // 知识点列表(逗号分隔或JSON数组字符串)
    private String questionTypes;     // 题型要求(可选),格式: "CHOICE:5,MULTIPLE:2,JUDGE:2,FILL:1"
}