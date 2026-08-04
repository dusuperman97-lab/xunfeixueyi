package com.project.platform.exception;

/**
 * 项目级统一错误码枚举
 * <p>
 * 格式：模块前缀(2位) + 错误类别(1位) + 序号(3位)
 * <p>
 * 错误类别：1-参数错误, 2-权限错误, 3-业务逻辑错误, 4-数据错误, 5-外部服务错误, 9-系统错误
 * <p>
 * 模块前缀：
 * - 10: 通用模块
 * - 20: 用户模块
 * - 30: 课程模块
 * - 40: 知识库/RAG 模块
 * - 50: 考试模块
 * - 60: 知识图谱模块
 * - 70: 文件/资源模块
 * - 80: AI 服务模块
 */
public enum ErrorCode {

    // ==================== 通用模块 (10) ====================
    SUCCESS(0, "操作成功"),
    UNKNOWN_ERROR(109999, "系统内部错误，请稍后重试"),
    PARAM_INVALID(101001, "请求参数无效"),
    PARAM_MISSING(101002, "缺少必要参数"),
    PARAM_TYPE_ERROR(101003, "参数类型错误"),
    METHOD_NOT_SUPPORTED(101004, "不支持的HTTP方法"),
    REQUEST_BODY_MISSING(101005, "请求体不能为空"),

    // ==================== 用户模块 (20) ====================
    USER_NOT_LOGIN(202001, "用户未登录"),
    USER_TOKEN_INVALID(202002, "登录凭证无效或已过期"),
    USER_NOT_FOUND(204001, "用户不存在"),
    USER_ACCOUNT_DISABLED(203002, "用户账号已被禁用"),

    // ==================== 知识库/RAG 模块 (40) ====================
    KNOWLEDGE_BASE_NOT_FOUND(404001, "知识库不存在"),
    KNOWLEDGE_BASE_NO_PERMISSION(402001, "无权限访问该知识库"),
    KNOWLEDGE_BASE_CREATE_FAILED(403001, "知识库创建失败"),
    KNOWLEDGE_BASE_UPDATE_FAILED(403002, "知识库更新失败"),
    KNOWLEDGE_BASE_DELETE_FAILED(403003, "知识库删除失败"),
    KNOWLEDGE_BASE_NAME_EMPTY(401001, "知识库名称不能为空"),
    KNOWLEDGE_BASE_NAME_TOO_LONG(401002, "知识库名称过长（最多100个字符）"),

    DOCUMENT_NOT_FOUND(404101, "文档不存在"),
    DOCUMENT_NO_PERMISSION(402101, "无权限操作该文档"),
    DOCUMENT_UPLOAD_FAILED(403101, "文档上传失败"),
    DOCUMENT_PARSE_FAILED(403102, "文档解析失败"),
    DOCUMENT_DELETE_FAILED(403103, "文档删除失败"),
    DOCUMENT_FILE_TYPE_INVALID(401101, "不支持的文件类型，仅支持 PDF、DOCX、DOC、TXT、MD"),
    DOCUMENT_FILE_EMPTY(401102, "上传文件不能为空"),
    DOCUMENT_REINDEX_FAILED(403104, "文档重新向量化失败"),
    DOCUMENT_SEGMENT_NOT_FOUND(404102, "文档分片不存在"),

    RAG_QUERY_EMPTY(401201, "查询问题不能为空"),
    RAG_RETRIEVAL_FAILED(405201, "知识库检索失败"),
    RAG_LLM_GENERATION_FAILED(405202, "AI 答案生成失败"),
    RAG_RERANK_FAILED(405203, "搜索结果重排序失败"),
    RAG_NO_RELEVANT_CONTENT(404201, "未找到相关知识库内容"),

    QA_HISTORY_NOT_FOUND(404301, "问答记录不存在"),
    QA_HISTORY_NO_PERMISSION(402301, "无权限操作该问答记录"),

    // ==================== 向量存储模块 (45) ====================
    VECTOR_DELETE_FAILED(455001, "向量数据清理失败"),
    VECTOR_STORE_UNAVAILABLE(455002, "向量存储服务不可用"),

    // ==================== 文件/资源模块 (70) ====================
    FILE_UPLOAD_TOO_LARGE(701001, "文件过大，超出上传限制"),
    FILE_PATH_INVALID(701002, "非法的文件路径"),
    FILE_NOT_FOUND(704001, "文件不存在"),

    // ==================== AI 服务模块 (80) ====================
    AI_SERVICE_TIMEOUT(805001, "AI 服务响应超时"),
    AI_SERVICE_UNAVAILABLE(805002, "AI 服务暂不可用"),
    AI_RESPONSE_PARSE_FAILED(805003, "AI 响应解析失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 根据 code 查找 ErrorCode，找不到返回 UNKNOWN_ERROR
     */
    public static ErrorCode fromCode(int code) {
        for (ErrorCode ec : values()) {
            if (ec.code == code) {
                return ec;
            }
        }
        return UNKNOWN_ERROR;
    }
}
