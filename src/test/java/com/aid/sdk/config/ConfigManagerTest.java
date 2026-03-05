package com.aid.sdk.config;

import com.aid.sdk.common.AidException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConfigManager 单元测试
 * 覆盖配置文件加载、字段读取、默认值、校验等核心逻辑
 */
class ConfigManagerTest {

    @TempDir
    Path tempDir;

    // ----------------------------------------------------------------
    // 辅助方法：在临时目录创建 .properties 文件
    // ----------------------------------------------------------------

    /**
     * 在临时目录下创建内容为 content 的 properties 文件，返回绝对路径
     */
    private String createTempPropertiesFile(String content) throws IOException {
        File file = tempDir.resolve("aid-test.properties").toFile();
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(content);
        }
        return file.getAbsolutePath();
    }

    // ----------------------------------------------------------------
    // 测试：成功加载配置文件
    // ----------------------------------------------------------------

    /**
     * test_loadConfigSuccess
     * 创建包含 baseURL 和 api_token 的临时配置文件，
     * 验证 ConfigManager 能正常读取两个核心配置项
     */
    @Test
    void test_loadConfigSuccess() throws IOException {
        // 准备：写入合法配置
        String content = "baseURL=http://localhost:8080\napi_token=11111111\n";
        String path = createTempPropertiesFile(content);

        // 执行：创建 ConfigManager 不应抛出异常
        ConfigManager config = new ConfigManager(path);

        // 断言：基础 URL 和 Token 均能正确读取
        assertEquals("http://localhost:8080", config.getBaseUrl(),
                "getBaseUrl() 应返回配置文件中的 baseURL 值");
        assertEquals("11111111", config.getApiToken(),
                "getApiToken() 应返回配置文件中的 api_token 值");
    }

    // ----------------------------------------------------------------
    // 测试：配置文件不存在时抛出 AidException
    // ----------------------------------------------------------------

    /**
     * test_loadConfigFileNotFound
     * 传入一个不存在的路径，期望 ConfigManager 构造时抛出 AidException
     */
    @Test
    void test_loadConfigFileNotFound() {
        String nonExistentPath = tempDir.resolve("not_exist.properties").toString();

        // 期望构造时抛出 AidException（配置文件未找到）
        AidException ex = assertThrows(AidException.class,
                () -> new ConfigManager(nonExistentPath),
                "配置文件不存在时应抛出 AidException");

        // 断言：异常信息应包含路径相关提示
        assertNotNull(ex.getErrorMessage(), "异常信息不应为 null");
    }

    // ----------------------------------------------------------------
    // 测试：get(key, defaultValue) 当 key 缺失时返回默认值
    // ----------------------------------------------------------------

    /**
     * test_getWithDefault
     * 配置文件中不包含 "unknown_key"，调用 get(key, default) 应返回默认值
     */
    @Test
    void test_getWithDefault() throws IOException {
        String content = "baseURL=http://localhost:8080\napi_token=tokenABC\n";
        String path = createTempPropertiesFile(content);
        ConfigManager config = new ConfigManager(path);

        // key 不存在时应返回默认值
        String result = config.get("unknown_key", "DEFAULT_VALUE");
        assertEquals("DEFAULT_VALUE", result,
                "key 不存在时 get(key, defaultValue) 应返回 defaultValue");
    }

    // ----------------------------------------------------------------
    // 测试：getBaseUrl 正确读取 baseURL
    // ----------------------------------------------------------------

    /**
     * test_getBaseUrl
     * 验证 getBaseUrl() 能准确返回配置文件中的 baseURL
     */
    @Test
    void test_getBaseUrl() throws IOException {
        String expected = "https://api.example.com:9090";
        String content = "baseURL=" + expected + "\napi_token=tokenXYZ\n";
        String path = createTempPropertiesFile(content);
        ConfigManager config = new ConfigManager(path);

        assertEquals(expected, config.getBaseUrl(),
                "getBaseUrl() 返回值应与配置文件中 baseURL 完全一致");
    }

    // ----------------------------------------------------------------
    // 测试：getApiToken 正确读取 api_token
    // ----------------------------------------------------------------

    /**
     * test_getApiToken
     * 验证 getApiToken() 能准确返回配置文件中的 api_token
     */
    @Test
    void test_getApiToken() throws IOException {
        String expected = "my-secret-token-999";
        String content = "baseURL=http://localhost:8080\napi_token=" + expected + "\n";
        String path = createTempPropertiesFile(content);
        ConfigManager config = new ConfigManager(path);

        assertEquals(expected, config.getApiToken(),
                "getApiToken() 返回值应与配置文件中 api_token 完全一致");
    }

    // ----------------------------------------------------------------
    // 测试：缺少 baseURL 时 ConfigValidator 应抛出异常
    // ----------------------------------------------------------------

    /**
     * test_missingRequiredField
     * 配置文件中仅包含 api_token 而无 baseURL，
     * 使用 ConfigValidator 校验时应抛出 AidException
     */
    @Test
    void test_missingRequiredField() throws IOException {
        // 仅写入 api_token，不写 baseURL
        String content = "api_token=tokenABC\n";
        String path = createTempPropertiesFile(content);
        ConfigManager config = new ConfigManager(path);

        ConfigValidator validator = new ConfigValidator();

        // 期望校验抛出 AidException（缺少 baseURL）
        AidException ex = assertThrows(AidException.class,
                () -> validator.validate(config),
                "缺少 baseURL 时 ConfigValidator.validate() 应抛出 AidException");

        // 断言：错误信息应提及 baseURL
        assertTrue(ex.getErrorMessage().contains("baseURL"),
                "异常信息应包含 'baseURL' 字段名提示，实际信息: " + ex.getErrorMessage());
    }
}
