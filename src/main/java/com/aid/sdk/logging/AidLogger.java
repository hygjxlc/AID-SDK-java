package com.aid.sdk.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * AID SDK日志工具类
 * 封装SLF4J Logger，提供带MDC上下文的结构化日志记录
 * MDC字段：taskID（任务ID）、api（接口名称）、code（响应状态码）
 */
public class AidLogger {

    /** 默认日志记录器 */
    private static final Logger LOGGER = LoggerFactory.getLogger("com.aid.sdk");

    /** MDC字段：任务ID */
    private static final String MDC_TASK_ID = "taskID";

    /** MDC字段：接口名称 */
    private static final String MDC_API = "api";

    /** MDC字段：响应状态码 */
    private static final String MDC_CODE = "code";

    /**
     * 私有构造函数，禁止实例化工具类
     */
    private AidLogger() {
        throw new UnsupportedOperationException("工具类不支持实例化");
    }

    /**
     * 设置MDC上下文字段
     *
     * @param api    接口名称
     * @param taskId 任务ID（可为null）
     */
    private static void setMdc(String api, String taskId) {
        if (api != null) {
            MDC.put(MDC_API, api);
        }
        if (taskId != null) {
            MDC.put(MDC_TASK_ID, taskId);
        }
    }

    /**
     * 设置MDC上下文字段（含状态码）
     *
     * @param api    接口名称
     * @param taskId 任务ID（可为null）
     * @param code   响应状态码（可为null）
     */
    private static void setMdc(String api, String taskId, String code) {
        setMdc(api, taskId);
        if (code != null) {
            MDC.put(MDC_CODE, code);
        }
    }

    /**
     * 清除MDC上下文字段
     */
    private static void clearMdc() {
        MDC.remove(MDC_API);
        MDC.remove(MDC_TASK_ID);
        MDC.remove(MDC_CODE);
    }

    /**
     * 记录INFO级别日志
     *
     * @param api     接口名称
     * @param taskId  任务ID
     * @param message 日志消息
     */
    public static void info(String api, String taskId, String message) {
        try {
            setMdc(api, taskId);
            LOGGER.info(message);
        } finally {
            clearMdc();
        }
    }

    /**
     * 记录INFO级别日志（含状态码）
     *
     * @param api     接口名称
     * @param taskId  任务ID
     * @param code    响应状态码
     * @param message 日志消息
     */
    public static void info(String api, String taskId, String code, String message) {
        try {
            setMdc(api, taskId, code);
            LOGGER.info(message);
        } finally {
            clearMdc();
        }
    }

    /**
     * 记录WARN级别日志
     *
     * @param api     接口名称
     * @param taskId  任务ID
     * @param message 日志消息
     */
    public static void warn(String api, String taskId, String message) {
        try {
            setMdc(api, taskId);
            LOGGER.warn(message);
        } finally {
            clearMdc();
        }
    }

    /**
     * 记录WARN级别日志（含状态码）
     *
     * @param api     接口名称
     * @param taskId  任务ID
     * @param code    响应状态码
     * @param message 日志消息
     */
    public static void warn(String api, String taskId, String code, String message) {
        try {
            setMdc(api, taskId, code);
            LOGGER.warn(message);
        } finally {
            clearMdc();
        }
    }

    /**
     * 记录ERROR级别日志
     *
     * @param api     接口名称
     * @param taskId  任务ID
     * @param message 日志消息
     */
    public static void error(String api, String taskId, String message) {
        try {
            setMdc(api, taskId);
            LOGGER.error(message);
        } finally {
            clearMdc();
        }
    }

    /**
     * 记录ERROR级别日志（含状态码和异常）
     *
     * @param api     接口名称
     * @param taskId  任务ID
     * @param code    响应状态码
     * @param message 日志消息
     * @param t       异常对象
     */
    public static void error(String api, String taskId, String code, String message, Throwable t) {
        try {
            setMdc(api, taskId, code);
            LOGGER.error(message, t);
        } finally {
            clearMdc();
        }
    }

    /**
     * 记录ERROR级别日志（含状态码）
     *
     * @param api     接口名称
     * @param taskId  任务ID
     * @param code    响应状态码
     * @param message 日志消息
     */
    public static void error(String api, String taskId, String code, String message) {
        try {
            setMdc(api, taskId, code);
            LOGGER.error(message);
        } finally {
            clearMdc();
        }
    }

    /**
     * 记录DEBUG级别日志
     *
     * @param api     接口名称
     * @param taskId  任务ID
     * @param message 日志消息
     */
    public static void debug(String api, String taskId, String message) {
        try {
            setMdc(api, taskId);
            LOGGER.debug(message);
        } finally {
            clearMdc();
        }
    }

    /**
     * 记录DEBUG级别日志（含状态码）
     *
     * @param api     接口名称
     * @param taskId  任务ID
     * @param code    响应状态码
     * @param message 日志消息
     */
    public static void debug(String api, String taskId, String code, String message) {
        try {
            setMdc(api, taskId, code);
            LOGGER.debug(message);
        } finally {
            clearMdc();
        }
    }
}
