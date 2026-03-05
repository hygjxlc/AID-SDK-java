package com.aid.sdk.http;

import com.aid.sdk.auth.AuthInterceptor;
import com.aid.sdk.auth.AuthManager;
import com.aid.sdk.common.AidException;
import com.aid.sdk.common.AidResponse;
import com.aid.sdk.common.ErrorCode;
import com.aid.sdk.logging.AidLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于OkHttp的HTTP客户端实现
 * 封装所有与后端服务的HTTP通信，支持JSON POST、Multipart文件上传和文件下载
 */
public class OkHttpClientImpl implements HttpClient {

    /** JSON媒体类型 */
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    /** API名称（用于日志MDC） */
    private static final String API_PREFIX = "OkHttpClient";

    /** 连接超时时间（秒） */
    private static final int CONNECT_TIMEOUT_SECONDS = 30;

    /** 读取超时时间（秒） */
    private static final int READ_TIMEOUT_SECONDS = 300;

    /** 写入超时时间（秒） */
    private static final int WRITE_TIMEOUT_SECONDS = 300;

    /** OkHttp客户端实例 */
    private final OkHttpClient okHttpClient;

    /** Jackson JSON处理器 */
    private final ObjectMapper objectMapper;

    /** 后端服务基础URL */
    private String baseUrl;

    /** 认证管理器（用于在请求体中注入apiKey） */
    private AuthManager authManager;

    /**
     * 构造函数（无认证拦截器）
     *
     * @param baseUrl 后端服务基础URL
     */
    public OkHttpClientImpl(String baseUrl) {
        this(baseUrl, null, null);
    }

    /**
     * 构造函数（带认证拦截器）
     *
     * @param baseUrl         后端服务基础URL
     * @param authInterceptor 认证拦截器（可为null）
     */
    public OkHttpClientImpl(String baseUrl, AuthInterceptor authInterceptor) {
        this(baseUrl, authInterceptor, null);
    }

    /**
     * 构造函数（带认证拦截器和认证管理器）
     *
     * @param baseUrl         后端服务基础URL
     * @param authInterceptor 认证拦截器（可为null）
     * @param authManager     认证管理器（用于在请求体中注入apiKey）
     */
    public OkHttpClientImpl(String baseUrl, AuthInterceptor authInterceptor, AuthManager authManager) {
        this.baseUrl = baseUrl;
        this.objectMapper = new ObjectMapper();
        this.authManager = authManager;

        // 构建OkHttp客户端
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // 注册认证拦截器
        if (authInterceptor != null) {
            builder.addInterceptor(authInterceptor);
        }

        this.okHttpClient = builder.build();
    }

    /**
     * 向请求参数中注入apiKey（符合OpenSpec规范）
     *
     * @param params 原始请求参数
     * @return 注入apiKey后的参数Map
     */
    private Map<String, String> injectApiKey(Map<String, String> params) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (params != null) {
            merged.putAll(params);
        }
        if (authManager != null) {
            String apiKey = authManager.getAuthToken();
            if (apiKey != null && !apiKey.isEmpty()) {
                merged.put("api_key", apiKey);
            }
        }
        return merged;
    }

    /**
     * 发送JSON格式的POST请求
     *
     * @param path   请求路径
     * @param params 请求参数，将被序列化为JSON Body
     * @return 包含响应字符串的AidResponse
     */
    @Override
    public AidResponse<String> post(String path, Map<String, String> params) {
        String url = buildUrl(path);
        AidLogger.info(API_PREFIX + ".post", null, "发送POST请求，URL=" + url);

        try {
            // 注入apiKey到请求参数
            Map<String, String> bodyParams = injectApiKey(params);
            // 将参数序列化为JSON字符串
            String jsonBody = objectMapper.writeValueAsString(bodyParams);
            RequestBody requestBody = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            return executeRequest(request, API_PREFIX + ".post");

        } catch (IOException e) {
            AidLogger.error(API_PREFIX + ".post", null,
                    String.valueOf(ErrorCode.INTERNAL_ERROR.getCode()),
                    "POST请求参数序列化失败: " + e.getMessage());
            return AidResponse.failure(ErrorCode.INTERNAL_ERROR.getCode(),
                    "请求参数序列化失败: " + e.getMessage());
        }
    }

    /**
     * 发送Multipart格式的POST请求（文件上传）
     *
     * @param path   请求路径
     * @param params 表单参数
     * @param files  文件参数
     * @return 包含响应字符串的AidResponse
     */
    @Override
    public AidResponse<String> postMultipart(String path, Map<String, String> params, java.util.List<File> files) {
        String url = buildUrl(path);
        AidLogger.info(API_PREFIX + ".postMultipart", null,
                "发送Multipart POST请求，URL=" + url + "，文件数量=" + (files != null ? files.size() : 0));

        try {
            Map<String, String> bodyParams = injectApiKey(params);
            RequestBody requestBody = MultipartHelper.buildMultipartBody(bodyParams, files);

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            return executeRequest(request, API_PREFIX + ".postMultipart");

        } catch (AidException e) {
            AidLogger.error(API_PREFIX + ".postMultipart", null,
                    String.valueOf(e.getErrorCode()),
                    "Multipart请求构建失败: " + e.getErrorMessage());
            return AidResponse.failure(e.getErrorCode(), e.getErrorMessage());
        }
    }

    @Override
    public AidResponse<String> postMultipart(String path, Map<String, String> params, Map<String, File> files) {
        return postMultipart(path, params, files != null ? new java.util.ArrayList<>(files.values()) : null);
    }

    /**
     * 下载文件，返回原始字节数组
     *
     * @param path   请求路径
     * @param params 请求参数
     * @return 文件原始字节数组
     * @throws AidException 下载失败时抛出异常
     */
    @Override
    public byte[] download(String path, Map<String, String> params) {
        String url = buildUrl(path);
        AidLogger.info(API_PREFIX + ".download", null, "发起文件下载请求，URL=" + url);

        try {
            // 注入apiKey到请求参数
            Map<String, String> bodyParams = injectApiKey(params);
            // 将参数序列化为JSON字符串
            String jsonBody = objectMapper.writeValueAsString(bodyParams);
            RequestBody requestBody = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new AidException(ErrorCode.RESULT_FETCH_FAILED,
                            "HTTP状态码异常: " + response.code());
                }

                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    throw new AidException(ErrorCode.RESULT_FETCH_FAILED, "响应体为空，文件下载失败");
                }

                byte[] bytes = responseBody.bytes();
                AidLogger.info(API_PREFIX + ".download", null,
                        "文件下载成功，字节数=" + bytes.length);
                return bytes;
            }

        } catch (AidException e) {
            throw e;
        } catch (IOException e) {
            AidLogger.error(API_PREFIX + ".download", null,
                    String.valueOf(ErrorCode.RESULT_FETCH_FAILED.getCode()),
                    "文件下载IO异常: " + e.getMessage(), e);
            throw new AidException(ErrorCode.RESULT_FETCH_FAILED, "文件下载失败: " + e.getMessage());
        }
    }

    /**
     * 设置后端服务基础URL
     *
     * @param baseUrl 基础URL字符串
     */
    @Override
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * 拼接完整URL
     *
     * @param path 相对路径
     * @return 完整URL字符串
     */
    private String buildUrl(String path) {
        if (path == null || path.isEmpty()) {
            return baseUrl;
        }
        // 避免双斜杠
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return baseUrl + path.substring(1);
        } else if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return baseUrl + "/" + path;
        }
        return baseUrl + path;
    }

    /**
     * 执行HTTP请求并解析响应
     *
     * @param request HTTP请求对象
     * @param apiName API名称（用于日志）
     * @return 包含响应字符串的AidResponse
     */
    private AidResponse<String> executeRequest(Request request, String apiName) {
        try (Response response = okHttpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String responseStr = responseBody != null ? responseBody.string() : "";

            if (!response.isSuccessful()) {
                AidLogger.warn(apiName, null,
                        String.valueOf(response.code()),
                        "HTTP请求失败，状态码=" + response.code() + "，响应体=" + responseStr);
                return AidResponse.failure(ErrorCode.INTERNAL_ERROR.getCode(),
                        "HTTP请求失败，状态码: " + response.code());
            }

            // 解析响应JSON中的code字段
            try {
                JsonNode jsonNode = objectMapper.readTree(responseStr);
                int code = jsonNode.has("code") ? jsonNode.get("code").asInt() : 200;
                String message = jsonNode.has("message") ? jsonNode.get("message").asText() : "";

                AidLogger.info(apiName, null, String.valueOf(code),
                        "HTTP请求完成，业务状态码=" + code + "，消息=" + message);

                if (code == ErrorCode.SUCCESS.getCode()) {
                    return AidResponse.success(responseStr, message);
                } else {
                    return AidResponse.failure(code, message);
                }
            } catch (IOException e) {
                // JSON解析失败，表明服务端响应格式异常，应返回错误
                AidLogger.error(apiName, null,
                        String.valueOf(ErrorCode.INTERNAL_ERROR.getCode()),
                        "响应JSON解析失败: " + e.getMessage() + "，原始响应: " + responseStr);
                return AidResponse.failure(ErrorCode.INTERNAL_ERROR.getCode(),
                        "服务端响应格式异常，JSON解析失败: " + e.getMessage());
            }

        } catch (IOException e) {
            AidLogger.error(apiName, null,
                    String.valueOf(ErrorCode.INTERNAL_ERROR.getCode()),
                    "HTTP请求IO异常: " + e.getMessage(), e);
            return AidResponse.failure(ErrorCode.INTERNAL_ERROR.getCode(),
                    "网络连接异常，请检查后端服务是否正常: " + e.getMessage());
        }
    }
}
