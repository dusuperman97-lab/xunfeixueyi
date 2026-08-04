package com.project.platform.exception;

import com.project.platform.neo4j.exception.CypherValidationException;
import com.project.platform.neo4j.exception.GraphQueryException;
import com.project.platform.vo.ResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常拦截器
 * 统一处理所有Controller层抛出的异常
 *
 * 优先级：
 * 1. 自定义业务异常 (CustomException)
 * 2. 参数校验异常 (MethodArgumentNotValidException, BindException)
 * 3. 参数类型不匹配异常 (MethodArgumentTypeMismatchException)
 * 4. 缺少必要参数异常 (MissingServletRequestParameterException)
 * 5. HTTP方法不支持异常 (HttpRequestMethodNotSupportedException)
 * 6. 文件大小超限异常 (MaxUploadSizeExceededException)
 * 7. 参数校验异常 (IllegalArgumentException)
 * 8. 通用异常 (Exception)
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理自定义异常
     */
    @ExceptionHandler(value = CustomException.class)
    @ResponseBody
    public ResponseEntity<ResponseVO<Void>> handleCustomException(CustomException e) {
        log.error("业务异常：{}", e.getMessage());
        return new ResponseEntity<>(ResponseVO.fail(e.getHttpStatus().value(), e.getMessage()), e.getHttpStatus());
    }

    /**
     * 处理知识图谱查询异常
     */
    @ExceptionHandler(value = GraphQueryException.class)
    @ResponseBody
    public ResponseEntity<ResponseVO<Void>> handleGraphQueryException(GraphQueryException e) {
        log.error("图谱查询异常 [phase={}]: {}", e.getPhase(), e.getMessage());
        String message = String.format("图谱查询失败（阶段: %s）：%s", e.getPhase(), e.getMessage());
        return new ResponseEntity<>(ResponseVO.error(e.getHttpStatus().value(), message), e.getHttpStatus());
    }

    /**
     * 处理 Cypher 校验失败异常（重试耗尽后抛出）
     */
    @ExceptionHandler(value = CypherValidationException.class)
    @ResponseBody
    public ResponseEntity<ResponseVO<Map<String, Object>>> handleCypherValidationException(
            CypherValidationException e) {
        log.error("Cypher 校验失败: {} 个错误", e.getErrors().size());
        Map<String, Object> errorData = new java.util.HashMap<>();
        errorData.put("message", e.getMessage());
        errorData.put("errors", e.getErrors());
        errorData.put("originalCypher", e.getOriginalCypher());
        return new ResponseEntity<>(new ResponseVO<>(422, "Cypher 校验失败", errorData),
                HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseVO<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数校验异常：{}", e.getMessage());
        return ResponseVO.fail(400, e.getMessage());
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseVO<Void> handleException(Exception e) {
        log.error("服务异常", e);
        return ResponseVO.fail(500, "服务内部错误，请稍后重试");
    }

    /**
     * 处理参数校验异常 (@Validated / @Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseVO<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验异常：{}", message);
        return ResponseVO.fail(400, message);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseBody
    public ResponseVO<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定异常：{}", message);
        return ResponseVO.fail(400, message);
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseBody
    public ResponseVO<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String message = String.format("参数类型错误：%s 应为 %s", e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知");
        log.warn("参数类型不匹配：{}", message);
        return ResponseVO.fail(400, message);
    }

    /**
     * 处理缺少必要参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseBody
    public ResponseVO<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        String message = String.format("缺少必要参数：%s", e.getParameterName());
        log.warn("缺少必要参数：{}", message);
        return ResponseVO.fail(400, message);
    }

    /**
     * 处理HTTP方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseBody
    public ResponseVO<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        String message = String.format("不支持的HTTP方法：%s", e.getMethod());
        log.warn("HTTP方法不支持：{}", message);
        return ResponseVO.fail(405, message);
    }

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseBody
    public ResponseVO<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("文件上传大小超限");
        return ResponseVO.fail(413, "文件过大，请上传小于 " + (e.getMaxUploadSize() / 1024 / 1024) + "MB 的文件");
    }
}
