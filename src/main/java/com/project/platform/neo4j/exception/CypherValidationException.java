package com.project.platform.neo4j.exception;

import com.project.platform.neo4j.dto.CypherValidationResult;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Cypher 校验失败异常。
 * 当校验未通过且超过最大重试次数时抛出，由 GlobalExceptionHandler 统一处理。
 */
@Getter
public class CypherValidationException extends RuntimeException {

    private final List<CypherValidationResult.ValidationError> errors;
    private final String originalCypher;

    public CypherValidationException(String message, List<CypherValidationResult.ValidationError> errors,
                                      String originalCypher) {
        super(message);
        this.errors = errors;
        this.originalCypher = originalCypher;
    }

    public CypherValidationException(String message, List<CypherValidationResult.ValidationError> errors) {
        this(message, errors, null);
    }

    /**
     * 获取 HTTP 状态码（供全局异常处理器使用）。
     */
    public int getHttpStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY.value();
    }
}
