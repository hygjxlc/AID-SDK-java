package com.aid.sdk.common;

/**
 * AID客户端SDK自定义运行时异常
 * 封装SDK内部所有业务异常，统一错误处理
 */
public class AidException extends RuntimeException {

    /** 错误码 */
    private final int errorCode;

    /** 错误描述信息 */
    private final String errorMessage;

    /**
     * 通过ErrorCode枚举构造异常
     *
     * @param errorCode 错误码枚举值
     */
    public AidException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode.getCode();
        this.errorMessage = errorCode.getMessage();
    }

    /**
     * 通过ErrorCode枚举和自定义详情构造异常
     *
     * @param errorCode 错误码枚举值
     * @param detail    详细错误信息（附加到标准消息后）
     */
    public AidException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.errorCode = errorCode.getCode();
        this.errorMessage = errorCode.getMessage() + ": " + detail;
    }

    /**
     * 通过错误码整数值和消息构造异常
     *
     * @param code    错误码整数值
     * @param message 错误描述信息
     */
    public AidException(int code, String message) {
        super(message);
        this.errorCode = code;
        this.errorMessage = message;
    }

    /**
     * 通过ErrorCode枚举和原始异常构造异常
     *
     * @param errorCode 错误码枚举值
     * @param cause     原始异常
     */
    public AidException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode.getCode();
        this.errorMessage = errorCode.getMessage();
    }

    /**
     * 获取错误码
     *
     * @return 错误码整数值
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * 获取错误描述信息
     *
     * @return 中文错误描述
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return "AidException{errorCode=" + errorCode + ", errorMessage='" + errorMessage + "'}";
    }
}
