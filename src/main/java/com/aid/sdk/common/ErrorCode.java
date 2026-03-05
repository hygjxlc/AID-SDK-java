package com.aid.sdk.common;

/**
 * AID客户端SDK错误码枚举
 * 定义所有业务操作的错误码和对应的中文描述信息
 */
public enum ErrorCode {

    /** 操作成功 */
    SUCCESS(200, "操作成功"),

    /** 任务创建失败 */
    TASK_CREATE_FAILED(301, "任务创建失败"),

    /** 工作文件缺失/校验失败 */
    FILE_VERIFY_FAILED(302, "工作文件缺失/校验失败"),

    /** 上传文件失败 */
    FILE_UPLOAD_FAILED(303, "上传文件失败"),

    /** 任务开始失败 */
    TASK_START_FAILED(401, "任务开始失败"),

    /** 任务停止失败 */
    TASK_STOP_FAILED(402, "任务停止失败"),

    /** 任务删除失败 */
    TASK_DELETE_FAILED(403, "任务删除失败"),

    /** 获取结果/状态失败 */
    RESULT_FETCH_FAILED(404, "获取结果/状态失败"),

    /** 算法服务内部运行错误 */
    INTERNAL_ERROR(500, "算法服务内部运行错误");

    /** 错误码 */
    private final int code;

    /** 错误描述信息 */
    private final String message;

    /**
     * 构造函数
     *
     * @param code    错误码整数值
     * @param message 错误中文描述信息
     */
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取错误码整数值
     *
     * @return 错误码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取错误描述信息
     *
     * @return 中文错误描述
     */
    public String getMessage() {
        return message;
    }

    /**
     * 根据错误码整数值查找对应的枚举
     *
     * @param code 错误码整数值
     * @return 对应的ErrorCode枚举，未找到则返回INTERNAL_ERROR
     */
    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        return INTERNAL_ERROR;
    }

    @Override
    public String toString() {
        return "ErrorCode{code=" + code + ", message='" + message + "'}";
    }
}
