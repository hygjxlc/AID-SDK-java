package com.aid.sdk.cli;

import com.aid.sdk.common.AidResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * CLI命令基类
 * 提供统一的JSON输出格式化和错误打印能力，所有命令类均继承此类
 */
public abstract class BaseCommand {

    /** 默认配置文件路径 */
    protected static final String DEFAULT_CONFIG_PATH = "./config/config.properties";

    /** JSON格式化输出 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * 以格式化JSON输出对象
     *
     * @param obj 待输出的对象
     */
    protected void printJson(Object obj) {
        try {
            System.out.println(OBJECT_MAPPER.writeValueAsString(obj));
        } catch (Exception e) {
            System.out.println("{\"error\": \"JSON序列化失败: " + e.getMessage() + "\"}");
        }
    }

    /**
     * 输出AidResponse为JSON格式（构造统一结构）
     *
     * @param response AidResponse对象
     */
    protected void printResponse(AidResponse<?> response) {
        try {
            System.out.println(OBJECT_MAPPER.writeValueAsString(response));
        } catch (Exception e) {
            System.out.println("{\"code\": 500, \"message\": \"输出序列化失败\"}");
        }
    }

    /**
     * 输出错误信息为JSON格式
     *
     * @param code    错误码
     * @param message 错误描述信息
     */
    protected void printError(int code, String message) {
        System.out.println("{\"code\": " + code + ", \"message\": \"" + escapeJson(message) + "\", \"success\": false}");
    }

    /**
     * 输出错误信息为JSON格式（使用默认500错误码）
     *
     * @param message 错误描述信息
     */
    protected void printError(String message) {
        printError(500, message);
    }

    /**
     * 转义JSON字符串中的特殊字符
     *
     * @param str 原始字符串
     * @return 转义后的字符串
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    /**
     * 执行命令（由子类实现）
     *
     * @param args 命令行参数数组
     */
    public abstract void execute(String[] args);
}
