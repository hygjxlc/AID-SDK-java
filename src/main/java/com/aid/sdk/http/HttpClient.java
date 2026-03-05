package com.aid.sdk.http;

import com.aid.sdk.common.AidResponse;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * HTTP客户端接口
 * 定义SDK与后端服务通信的基本操作，支持JSON POST、Multipart上传和文件下载
 */
public interface HttpClient {

    /**
     * 发送JSON格式的POST请求
     *
     * @param path   请求路径（相对于baseUrl）
     * @param params 请求参数，将被序列化为JSON Body
     * @return 包含响应字符串的AidResponse
     */
    AidResponse<String> post(String path, Map<String, String> params);

    /**
     * 发送Multipart格式的POST请求（支持多文件上传，文件字段名统一为 "files"）
     *
     * @param path   请求路径（相对于baseUrl）
     * @param params 表单参数
     * @param files  文件列表
     * @return 包含响应字符串的AidResponse
     */
    AidResponse<String> postMultipart(String path, Map<String, String> params, List<File> files);

    /**
     * 发送Multipart格式的POST请求（Map模式，保持向后兼容）
     *
     * @param path   请求路径（相对于baseUrl）
     * @param params 表单参数
     * @param files  文件参数，key为表单字段名，value为文件对象
     * @return 包含响应字符串的AidResponse
     */
    AidResponse<String> postMultipart(String path, Map<String, String> params, Map<String, File> files);

    /**
     * 下载文件，返回原始字节数组
     *
     * @param path   请求路径（相对于baseUrl）
     * @param params 请求参数，将被序列化为JSON Body
     * @return 文件原始字节数组
     */
    byte[] download(String path, Map<String, String> params);

    /**
     * 设置后端服务基础URL
     *
     * @param baseUrl 基础URL字符串（例如：http://127.0.0.1:8080/aid-service）
     */
    void setBaseUrl(String baseUrl);
}
