package com.aid.sdk;

import com.aid.sdk.auth.AuthInterceptor;
import com.aid.sdk.auth.AuthManager;
import com.aid.sdk.common.AidResponse;
import com.aid.sdk.config.ConfigManager;
import com.aid.sdk.config.ConfigValidator;
import com.aid.sdk.http.OkHttpClientImpl;
import com.aid.sdk.logging.AidLogger;
import com.aid.sdk.task.TaskService;
import com.aid.sdk.task.model.TaskCreateResponse;
import com.aid.sdk.task.model.TaskResultResponse;
import com.aid.sdk.task.model.TaskStatusResponse;
import com.aid.sdk.task.model.UploadFilesResponse;
import com.aid.sdk.task.model.VerifyResponse;

import java.io.File;
import java.util.List;

/**
 * AID客户端SDK门面类（Facade）
 * 统一封装所有SDK能力，用户通过此类即可完成所有仿真任务管理操作
 * 实现AutoCloseable接口以支持try-with-resources用法
 */
public class AidClient implements AutoCloseable {

    /** 配置管理器 */
    private final ConfigManager configManager;

    /** 认证管理器 */
    private final AuthManager authManager;

    /** 任务服务 */
    private final TaskService taskService;

    /**
     * 构造函数
     * 按顺序初始化所有内部组件：配置 → 校验 → 认证 → HTTP客户端 → 任务服务
     *
     * @param configPath 配置文件路径（如 ./config/config.properties）
     */
    public AidClient(String configPath) {
        AidLogger.info("AidClient", null, "初始化AID客户端SDK，配置文件路径=" + configPath);

        // 1. 加载配置
        this.configManager = new ConfigManager(configPath);

        // 2. 校验配置
        ConfigValidator validator = new ConfigValidator();
        validator.validate(configManager);

        // 3. 初始化认证管理器
        this.authManager = new AuthManager(configManager);

        // 4. 初始化认证拦截器和HTTP客户端
        AuthInterceptor authInterceptor = new AuthInterceptor(authManager);
        // 传递authManager以便在请求体中注入apiKey（符合OpenSpec规范）
        OkHttpClientImpl httpClient = new OkHttpClientImpl(configManager.getBaseUrl(), authInterceptor, authManager);

        // 5. 初始化任务服务
        this.taskService = new TaskService(httpClient);

        AidLogger.info("AidClient", null,
                "AID客户端SDK初始化完成，baseURL=" + configManager.getBaseUrl());
    }

    // ==================== API代理方法 ====================

    /**
     * 创建新仿真任务
     *
     * @param simulateType 仿真类型（LaWan/CHOnYA/ZhuZao/ZhaZhi/ZHEWan/JIYA）
     * @param taskName     任务名称（1-64位，字母/数字/下划线）
     * @return 包含TaskCreateResponse的AidResponse
     */
    public AidResponse<TaskCreateResponse> newTaskCreate(String simulateType, String taskName) {
        return taskService.newTaskCreate(simulateType, taskName);
    }

    /**
     * 上传任务参数文件
     *
     * @param taskId 任务ID
     * @param files  待上传的文件列表
     * @return 包含UploadFilesResponse的AidResponse
     */
    public AidResponse<UploadFilesResponse> uploadParamfiles(String taskId, List<File> files) {
        return taskService.uploadParamfiles(taskId, files);
    }

    /**
     * 校验任务文件完整性
     *
     * @param taskId 任务ID
     * @return 包含VerifyResponse的AidResponse
     */
    public AidResponse<VerifyResponse> newTaskverify(String taskId) {
        return taskService.newTaskverify(taskId);
    }

    /**
     * 启动仿真任务
     *
     * @param taskId 任务ID
     * @return 包含TaskStatusResponse的AidResponse
     */
    public AidResponse<TaskStatusResponse> startTask(String taskId) {
        return taskService.startTask(taskId);
    }

    /**
     * 查询任务运行状态
     *
     * @param taskId 任务ID
     * @return 包含TaskStatusResponse的AidResponse
     */
    public AidResponse<TaskStatusResponse> queryTaskStatus(String taskId) {
        return taskService.queryTaskStatus(taskId);
    }

    /**
     * 停止仿真任务
     *
     * @param taskId 任务ID
     * @return 包含TaskStatusResponse的AidResponse
     */
    public AidResponse<TaskStatusResponse> stopTask(String taskId) {
        return taskService.stopTask(taskId);
    }

    /**
     * 删除仿真任务
     *
     * @param taskId 任务ID
     * @return 包含TaskStatusResponse的AidResponse
     */
    public AidResponse<TaskStatusResponse> deleteTask(String taskId) {
        return taskService.deleteTask(taskId);
    }

    /**
     * 获取仿真任务结果并下载到本地
     *
     * @param taskId     任务ID
     * @param outputPath 本地输出路径
     * @return 包含TaskResultResponse的AidResponse
     */
    public AidResponse<TaskResultResponse> fetchTaskResult(String taskId, String outputPath) {
        return taskService.fetchTaskResult(taskId, outputPath);
    }

    // ==================== 访问器方法 ====================

    /**
     * 获取配置管理器
     *
     * @return ConfigManager实例
     */
    public ConfigManager getConfig() {
        return configManager;
    }

    /**
     * 获取认证管理器
     *
     * @return AuthManager实例
     */
    public AuthManager getAuth() {
        return authManager;
    }

    /**
     * 释放资源（AutoCloseable实现）
     * 当前版本无需主动释放资源，预留给后续版本扩展
     */
    @Override
    public void close() {
        AidLogger.info("AidClient", null, "AID客户端SDK已关闭");
    }
}
