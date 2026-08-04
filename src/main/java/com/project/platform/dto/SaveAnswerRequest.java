package com.project.platform.dto;

import lombok.Data;

import java.util.List;

/**
 * 暂存答案请求DTO（考试中途保存，不等同交卷）
 */
@Data
public class SaveAnswerRequest {

    /** 测试批次号 */
    private String testBatchNo;

    /** 暂存的答案列表 */
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        /** 题目ID */
        private Integer questionId;
        /** 用户答案 */
        private String userAnswer;
    }
}
