package com.aid.sdk.common;

/**
 * AID客户端SDK通用响应包装类
 * 统一封装所有API调用的返回结果
 *
 * @param <T> 响应数据的类型
 */
public class AidResponse<T> {

    /** 响应状态码 */
    private int code;

    /** 响应描述信息 */
    private String message;

    /** 响应业务数据 */
    private T data;

    /**
     * 默认构造函数（Jackson反序列化需要）
     */
    public AidResponse() {
    }

    /**
     * 全参构造函数
     *
     * @param code    响应状态码
     * @param message 响应描述信息
     * @param data    响应业务数据
     */
    public AidResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 判断操作是否成功（状态码为200表示成功）
     *
     * @return true表示操作成功，false表示操作失败
     */
    public boolean isSuccess() {
        return this.code == ErrorCode.SUCCESS.getCode();
    }

    /**
     * 创建成功响应（静态工厂方法）
     *
     * @param data    响应业务数据
     * @param message 成功描述信息
     * @param <T>     数据类型
     * @return 成功响应对象
     */
    public static <T> AidResponse<T> success(T data, String message) {
        return new AidResponse<>(ErrorCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 创建失败响应（静态工厂方法）
     *
     * @param code    错误码
     * @param message 错误描述信息
     * @param <T>     数据类型
     * @return 失败响应对象
     */
    public static <T> AidResponse<T> failure(int code, String message) {
        return new AidResponse<>(code, message, null);
    }

    /**
     * 创建失败响应（使用ErrorCode枚举）
     *
     * @param errorCode 错误码枚举
     * @param <T>       数据类型
     * @return 失败响应对象
     */
    public static <T> AidResponse<T> failure(ErrorCode errorCode) {
        return new AidResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    // ========== Getter/Setter ==========

    /**
     * 获取响应状态码
     *
     * @return 状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 设置响应状态码
     *
     * @param code 状态码
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * 获取响应描述信息
     *
     * @return 描述信息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置响应描述信息
     *
     * @param message 描述信息
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 获取响应业务数据
     *
     * @return 业务数据
     */
    public T getData() {
        return data;
    }

    /**
     * 设置响应业务数据
     *
     * @param data 业务数据
     */
    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "AidResponse{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", success=" + isSuccess() +
                '}';
    }
}
