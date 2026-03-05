package com.aid.sdk.auth;

import com.aid.sdk.logging.AidLogger;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/**
 * OkHttp认证拦截器
 * 拦截所有HTTP请求，自动注入API Key认证参数，并记录认证事件日志
 */
public class AuthInterceptor implements Interceptor {

    /** 认证管理器 */
    private final AuthManager authManager;

    /** API名称（用于日志MDC） */
    private static final String API_NAME = "AuthInterceptor";

    /** API Key请求头名称 */
    private static final String HEADER_API_KEY = "apiKey";

    /**
     * 构造函数
     *
     * @param authManager 认证管理器实例
     */
    public AuthInterceptor(AuthManager authManager) {
        this.authManager = authManager;
    }

    /**
     * 拦截HTTP请求，注入API Key认证信息
     * 对所有请求在请求头中添加apiKey字段
     *
     * @param chain OkHttp拦截器链
     * @return HTTP响应
     * @throws IOException 网络IO异常
     */
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String apiToken = authManager.getAuthToken();

        // 在请求头中注入API Key
        Request authenticatedRequest = originalRequest.newBuilder()
                .header(HEADER_API_KEY, apiToken)
                .build();

        AidLogger.debug(API_NAME, null,
                "认证拦截器：向请求注入API Key，URL=" + originalRequest.url());

        // 继续执行请求链
        Response response = chain.proceed(authenticatedRequest);

        AidLogger.debug(API_NAME, null,
                "认证拦截器：请求完成，状态码=" + response.code() + "，URL=" + originalRequest.url());

        return response;
    }
}
