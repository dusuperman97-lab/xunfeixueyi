package com.project.platform.neo4j.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Cypher 语句校验结果 DTO。
 * 由 LLM 对生成的 Cypher 进行语法正确性和逻辑合理性验证后返回。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CypherValidationResult {

    /** 是否通过校验 */
    private boolean valid;

    /** 校验错误列表 */
    @Builder.Default
    private List<ValidationError> errors = Collections.emptyList();

    /** 是否有需要修正后才可执行的关键错误 */
    public boolean hasBlockingErrors() {
        return errors != null && errors.stream().anyMatch(e -> "BLOCKING".equals(e.getSeverity()));
    }

    /**
     * 单条校验错误详情。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {

        /** 错误类型：SYNTAX（语法）/ SEMANTIC（语义）/ LOGIC（逻辑）/ SECURITY（安全） */
        private String errorType;

        /** 严重程度：BLOCKING（阻止执行）/ WARNING（警告，可尝试执行） */
        @Builder.Default
        private String severity = "BLOCKING";

        /** 错误在 Cypher 中的位置描述（如 "第3行的 MATCH 子句"） */
        private String errorLocation;

        /** 错误详细信息 */
        private String message;

        /** 修正建议 */
        private String suggestion;
    }
}
