package com.aid.sdk.auth;

import com.aid.sdk.config.ConfigManager;

/**
 * 认证管理器
 * 基于API Key方式实现AuthService接口，从配置文件获取Token并进行验证
 */
public class AuthManager implements AuthService {

    /** 认证类型：API Key */
    private static final String AUTH_TYPE = "api_key";

    /** 配置管理器，用于获取api_token配置 */
    private final ConfigManager configManager;

    /**
     * 构造函数
     *
     * @param configManager 配置管理器实例
     */
    public AuthManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * 从配置文件获取API认证Token
     *
     * @return API Token字符串
     */
    @Override
    public String getAuthToken() {
        return configManager.getApiToken();
    }

    /**
     * 验证给定Token与配置中的api_token是否匹配
     *
     * @param token 待验证的Token
     * @return true表示Token有效，false表示无效或token为null
     */
    @Override
    public boolean authenticate(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        String configuredToken = configManager.getApiToken();
        return token.trim().equals(configuredToken);
    }

    /**
     * 获取认证类型
     *
     * @return 固定返回 "api_key"
     */
    @Override
    public String getAuthType() {
        return AUTH_TYPE;
    }
}
