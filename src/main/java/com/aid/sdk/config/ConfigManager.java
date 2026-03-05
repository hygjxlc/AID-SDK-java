package com.aid.sdk.config;

import com.aid.sdk.common.AidException;
import com.aid.sdk.common.ErrorCode;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * SDK配置管理器
 * 负责从配置文件读取并缓存所有配置项，提供统一的配置访问接口
 */
public class ConfigManager {

    /** 配置文件路径 */
    private final String configFilePath;

    /** 缓存的配置属性集合 */
    private final Properties properties;

    /** 配置项：后端服务基础URL */
    private static final String KEY_BASE_URL = "baseURL";

    /** 配置项：API认证Token */
    private static final String KEY_API_TOKEN = "api_token";

    /**
     * 构造函数，加载指定路径的配置文件
     *
     * @param configFilePath 配置文件路径（相对路径或绝对路径）
     * @throws AidException 如果配置文件不存在或加载失败
     */
    public ConfigManager(String configFilePath) {
        this.configFilePath = configFilePath;
        this.properties = new Properties();
        loadConfig();
    }

    /**
     * 从文件加载配置到内存（带缓存）
     * 优先从文件系统加载，若失败则从classpath加载
     *
     * @throws AidException 配置文件不存在或IO异常
     */
    private void loadConfig() {
        // 优先尝试从文件系统加载
        try (InputStream is = new FileInputStream(configFilePath)) {
            properties.load(is);
            return;
        } catch (IOException e) {
            // 文件系统加载失败，尝试从classpath加载
        }

        // 从classpath加载
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(configFilePath)) {
            if (is == null) {
                throw new AidException(ErrorCode.INTERNAL_ERROR,
                        "配置文件未找到，请检查路径是否正确: " + configFilePath);
            }
            properties.load(is);
        } catch (IOException e) {
            throw new AidException(ErrorCode.INTERNAL_ERROR,
                    "配置文件读取失败: " + configFilePath + "，原因: " + e.getMessage());
        }
    }

    /**
     * 根据键名获取配置值
     *
     * @param key 配置键名
     * @return 配置值，如不存在则返回null
     */
    public String get(String key) {
        return properties.getProperty(key);
    }

    /**
     * 根据键名获取配置值，不存在时返回默认值
     *
     * @param key          配置键名
     * @param defaultValue 默认值
     * @return 配置值，如不存在则返回默认值
     */
    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * 获取后端服务基础URL
     *
     * @return 基础URL字符串
     * @throws AidException 如果baseURL配置项未设置
     */
    public String getBaseUrl() {
        String baseUrl = get(KEY_BASE_URL);
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new AidException(ErrorCode.INTERNAL_ERROR,
                    "配置项 [baseURL] 未设置，请在配置文件中配置后端服务地址");
        }
        return baseUrl.trim();
    }

    /**
     * 获取API认证Token
     *
     * @return API Token字符串
     * @throws AidException 如果api_token配置项未设置
     */
    public String getApiToken() {
        String apiToken = get(KEY_API_TOKEN);
        if (apiToken == null || apiToken.trim().isEmpty()) {
            throw new AidException(ErrorCode.INTERNAL_ERROR,
                    "配置项 [api_token] 未设置，请在配置文件中配置认证Token");
        }
        return apiToken.trim();
    }

    /**
     * 获取配置文件路径
     *
     * @return 配置文件路径
     */
    public String getConfigFilePath() {
        return configFilePath;
    }
}
