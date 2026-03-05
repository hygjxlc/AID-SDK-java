package com.aid.sdk.auth;

import com.aid.sdk.config.ConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AuthManager 单元测试
 * 使用 Mockito mock ConfigManager，隔离文件系统依赖，
 * 验证认证 Token 获取、认证校验和认证类型等核心逻辑
 */
@ExtendWith(MockitoExtension.class)
class AuthManagerTest {

    /** 模拟的配置管理器（不需要真实的 properties 文件） */
    @Mock
    private ConfigManager mockConfigManager;

    /** 被测对象：AuthManager */
    private AuthManager authManager;

    /** 测试用的 API Token 值 */
    private static final String VALID_TOKEN = "11111111";

    @BeforeEach
    void setUp() {
        // 每个测试前创建 AuthManager，注入 mock 的 ConfigManager
        authManager = new AuthManager(mockConfigManager);
    }

    // ----------------------------------------------------------------
    // 测试：getAuthToken 应从 ConfigManager 读取 api_token
    // ----------------------------------------------------------------

    /**
     * test_getAuthToken
     * 验证 AuthManager.getAuthToken() 调用 configManager.getApiToken() 并返回其值
     */
    @Test
    void test_getAuthToken() {
        // 安排：mock getApiToken() 返回固定值
        when(mockConfigManager.getApiToken()).thenReturn(VALID_TOKEN);

        // 执行
        String token = authManager.getAuthToken();

        // 断言：返回值应与 mock 设定的值一致
        assertEquals(VALID_TOKEN, token,
                "getAuthToken() 应返回 ConfigManager.getApiToken() 的值");

        // 验证：getApiToken() 被调用了一次
        verify(mockConfigManager, times(1)).getApiToken();
    }

    // ----------------------------------------------------------------
    // 测试：authenticate 传入匹配 Token 应返回 true
    // ----------------------------------------------------------------

    /**
     * test_authenticateValid
     * 当传入的 Token 与配置中的 api_token 完全匹配时，authenticate() 应返回 true
     */
    @Test
    void test_authenticateValid() {
        // 安排：mock getApiToken() 返回 "11111111"
        when(mockConfigManager.getApiToken()).thenReturn(VALID_TOKEN);

        // 执行：传入与配置一致的 Token
        boolean result = authManager.authenticate(VALID_TOKEN);

        // 断言：匹配时应返回 true
        assertTrue(result, "传入与配置一致的 Token 时 authenticate() 应返回 true");
    }

    // ----------------------------------------------------------------
    // 测试：authenticate 传入错误 Token 应返回 false
    // ----------------------------------------------------------------

    /**
     * test_authenticateInvalid
     * 当传入的 Token 与配置中的 api_token 不匹配时，authenticate() 应返回 false
     */
    @Test
    void test_authenticateInvalid() {
        // 安排：mock getApiToken() 返回 "11111111"
        when(mockConfigManager.getApiToken()).thenReturn(VALID_TOKEN);

        // 执行：传入错误的 Token
        boolean result = authManager.authenticate("wrong-token");

        // 断言：不匹配时应返回 false
        assertFalse(result, "传入错误 Token 时 authenticate() 应返回 false");
    }

    // ----------------------------------------------------------------
    // 测试：getAuthType 应返回 "api_key"
    // ----------------------------------------------------------------

    /**
     * test_getAuthType
     * 验证 AuthManager 固定返回认证类型 "api_key"
     */
    @Test
    void test_getAuthType() {
        // 执行
        String authType = authManager.getAuthType();

        // 断言：认证类型固定为 "api_key"
        assertEquals("api_key", authType,
                "getAuthType() 应固定返回 \"api_key\"");
    }
}
