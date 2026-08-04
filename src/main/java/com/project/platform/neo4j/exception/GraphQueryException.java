package com.project.platform.neo4j.exception;

import com.project.platform.exception.CustomException;
import org.springframework.http.HttpStatus;

/**
 * 知识图谱查询通用异常。
 * 用于封装 Neo4j 执行失败、检索服务异常、LLM 调用失败等场景。
 */
public class GraphQueryException extends CustomException {

    /** 查询执行阶段：IDENTIFY / RETRIEVE / GENERATE / VALIDATE / CORRECT / EXECUTE */
    private final String phase;

    public GraphQueryException(String phase, String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
        this.phase = phase;
    }

    public GraphQueryException(String phase, String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
        this.phase = phase;
        initCause(cause);
    }

    public String getPhase() {
        return phase;
    }
}
