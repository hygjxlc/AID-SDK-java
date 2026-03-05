package com.aid.sdk.auth;

/**
 * 认证服务接口
 * 预留扩展接口，支持未来多种认证机制（api_key、token、oauth2等）
 */
public interface AuthService {

    /**
     * 获取当前认证Token
     *
     * @return 认证Token字符串
     */
    String getAuthToken();

    /**
     * 验证给定Token是否有效
     *
     * @param token 待验证的Token
     * @return true表示Token有效，false表示无效
     */
    boolean authenticate(String token);

    /**
     * 获取当前认证类型
     * 可选值：api_key、token、oauth2
     *
     * @return 认证类型字符串
     */
    String getAuthType();
}
