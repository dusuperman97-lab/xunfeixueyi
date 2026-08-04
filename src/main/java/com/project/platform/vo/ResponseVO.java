package com.project.platform.vo;

/**
 * 统一响应结果封装类
 * @param <T> 数据类型
 */
public class ResponseVO<T> {
    private int code;           // 状态码：200-成功, 其他-失败
    private String msg;         // 响应消息
    private T data;             // 响应数据

    public ResponseVO(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 成功响应（无数据）
     */
    public static ResponseVO ok() {
        return new ResponseVO(200, "操作成功", null);
    }

    /**
     * 成功响应（带数据）
     */
    public static ResponseVO ok(Object data) {
        return new ResponseVO(200, "操作成功", data);
    }


    /**
     * 失败响应（自定义状态码和数据）
     */
    public static ResponseVO fail(int code, Object data) {
        return new ResponseVO(code, "操作失败", data);
    }

    /**
     * 错误响应（自定义消息）
     */
    public static ResponseVO error(String msg) {
        return new ResponseVO(500, msg, null);
    }

    /**
     * 错误响应（自定义状态码和消息）
     */
    public static ResponseVO error(int code, String msg) {
        return new ResponseVO(code, msg, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
