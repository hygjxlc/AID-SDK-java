package com.aid.sdk.config;

import com.aid.sdk.common.AidException;
import com.aid.sdk.common.ErrorCode;

/**
 * SDK配置校验器
 * 验证配置管理器中所有必填配置项是否合法
 */
public class ConfigValidator {

    /**
     * 校验配置管理器中的配置项
     * 检查所有必填字段是否存在且不为空
     *
     * @param config 配置管理器实例
     * @throws AidException 如果必填配置项缺失或值不合法
     */
    public void validate(ConfigManager config) {
        // 校验必填配置项：后端服务基础URL
        String baseUrl = config.get("baseURL");
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new AidException(ErrorCode.INTERNAL_ERROR,
                    "配置校验失败：必填配置项 [baseURL] 未设置，请在配置文件中配置后端服务地址");
        }

        // 校验URL格式（必须以http://或https://开头）
        String trimmedUrl = baseUrl.trim();
        if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
            throw new AidException(ErrorCode.INTERNAL_ERROR,
                    "配置校验失败：[baseURL] 格式不正确，必须以 http:// 或 https:// 开头，当前值: " + trimmedUrl);
        }

        // 校验必填配置项：API认证Token
        String apiToken = config.get("api_token");
        if (apiToken == null || apiToken.trim().isEmpty()) {
            throw new AidException(ErrorCode.INTERNAL_ERROR,
                    "配置校验失败：必填配置项 [api_token] 未设置，请在配置文件中配置认证Token");
        }
    }
}
