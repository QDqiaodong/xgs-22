package com.example.lightmanager.common;

/**
 * 业务异常: 携带 HTTP 状态语义, 由全局异常处理器转换为统一响应体。
 * code=409 用于并发冲突(乐观锁更新失败/状态已被他人改变)。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(500, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
