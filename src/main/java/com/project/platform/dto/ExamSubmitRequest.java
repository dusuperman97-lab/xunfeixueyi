package com.project.platform.dto;

import lombok.Data;

import java.util.List;

/**
 * 提交考试请求DTO
 */
@Data
public class ExamSubmitRequest {

    /** 测试批次号 */
    private String testBatchNo;

    /** 所有题目答案 */
    private List<QuestionAnswerDTO> answers;
}
