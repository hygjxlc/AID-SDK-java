package com.aid.sdk.task;

import com.aid.sdk.common.AidResponse;
import com.aid.sdk.http.HttpClient;
import com.aid.sdk.task.model.TaskCreateResponse;
import com.aid.sdk.task.model.TaskResultResponse;
import com.aid.sdk.task.model.TaskStatusResponse;
import com.aid.sdk.task.model.UploadFilesResponse;
import com.aid.sdk.task.model.VerifyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TaskService 单元测试
 * 使用 Mockito mock HttpClient，验证 TaskService 的参数校验逻辑
 * 以及对 HTTP 响应的解析与封装是否正确
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    /** 模拟 HTTP 客户端，不发起真实网络请求 */
    @Mock
    private HttpClient mockHttpClient;

    /** 被测对象 */
    private TaskService taskService;

    /** 用于写临时测试文件的目录 */
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // 每次测试前重新构建 TaskService，注入 mock HttpClient
        taskService = new TaskService(mockHttpClient);
    }

    // ================================================================
    // API 1: newTaskCreate
    // ================================================================

    /**
     * test_newTaskCreateSuccess
     * mock HttpClient.post() 返回合法 JSON，
     * 验证 TaskService 能正确解析 TaskCreateResponse
     */
    @Test
    void test_newTaskCreateSuccess() {
        // 安排：mock 返回成功的 JSON 响应体
        String responseJson = "{\"code\":200,\"taskID\":\"T001\",\"message\":\"任务创建成功\"}";
        when(mockHttpClient.post(eq("/newTaskCreate"), anyMap()))
                .thenReturn(AidResponse.success(responseJson, "成功"));

        // 执行：创建合法仿真任务
        AidResponse<TaskCreateResponse> result = taskService.newTaskCreate("LaWan", "my_task_001");

        // 断言：响应应为成功
        assertTrue(result.isSuccess(), "合法参数时 newTaskCreate 应返回成功响应");
        assertNotNull(result.getData(), "响应 data 不应为 null");
        assertEquals("T001", result.getData().getTaskID(),
                "taskID 应与 JSON 中的值一致");
        assertEquals(200, result.getData().getCode(),
                "响应内 code 应为 200");
    }

    /**
     * test_newTaskCreateInvalidType
     * 传入不受支持的 simulateType，
     * 期望返回失败响应（code == 301 / TASK_CREATE_FAILED）
     */
    @Test
    void test_newTaskCreateInvalidType() {
        // 执行：传入不合法的仿真类型（不在白名单中）
        AidResponse<TaskCreateResponse> result =
                taskService.newTaskCreate("InvalidType", "my_task");

        // 断言：应返回失败，且不调用 HttpClient
        assertFalse(result.isSuccess(), "不支持的 simulateType 应返回失败响应");
        assertEquals(301, result.getCode(),
                "失败响应 code 应为 301（TASK_CREATE_FAILED）");
        verify(mockHttpClient, never()).post(anyString(), anyMap());
    }

    /**
     * test_newTaskCreateInvalidName
     * 传入空的 taskName，期望返回参数校验失败响应
     */
    @Test
    void test_newTaskCreateInvalidName() {
        // 执行：taskName 为空字符串
        AidResponse<TaskCreateResponse> result =
                taskService.newTaskCreate("LaWan", "");

        // 断言：应返回失败响应
        assertFalse(result.isSuccess(), "空 taskName 应返回失败响应");
        assertEquals(301, result.getCode(),
                "失败响应 code 应为 301（TASK_CREATE_FAILED）");
        verify(mockHttpClient, never()).post(anyString(), anyMap());
    }

    /**
     * test_newTaskCreateInvalidNameSpecialChars
     * taskName 包含特殊字符（空格和感叹号），期望校验失败
     */
    @Test
    void test_newTaskCreateInvalidNameSpecialChars() {
        // 执行：taskName 包含特殊字符
        AidResponse<TaskCreateResponse> result =
                taskService.newTaskCreate("LaWan", "bad name!");

        // 断言：应返回失败响应（格式不合法）
        assertFalse(result.isSuccess(), "含特殊字符的 taskName 应返回失败响应");
        assertEquals(301, result.getCode(),
                "失败响应 code 应为 301（TASK_CREATE_FAILED）");
        verify(mockHttpClient, never()).post(anyString(), anyMap());
    }

    // ================================================================
    // API 2: uploadParamfiles
    // ================================================================

    /**
     * test_uploadParamfilesSuccess
     * mock postMultipart() 返回成功响应，验证文件上传正常路径
     */
    @Test
    void test_uploadParamfilesSuccess() throws IOException {
        // 准备：创建一个临时文件用于上传
        File tempFile = tempDir.resolve("param.dat").toFile();
        tempFile.createNewFile();

        // 安排：mock 返回文件上传成功的 JSON 响应
        String responseJson = "{\"code\":200,\"taskID\":\"T001\",\"message\":\"上传成功\",\"fileList\":[\"param.dat\"]}";
        when(mockHttpClient.postMultipart(eq("/uploadParamfiles"), anyMap(), anyMap()))
                .thenReturn(AidResponse.success(responseJson, "上传成功"));

        // 执行
        AidResponse<UploadFilesResponse> result =
                taskService.uploadParamfiles("T001", Arrays.asList(tempFile));

        // 断言：应返回成功
        assertTrue(result.isSuccess(), "合法参数时 uploadParamfiles 应返回成功响应");
        assertNotNull(result.getData(), "响应 data 不应为 null");
        assertNotNull(result.getData().getFileList(), "fileList 不应为 null");
        assertFalse(result.getData().getFileList().isEmpty(), "fileList 不应为空");
    }

    /**
     * test_uploadParamfilesEmptyTaskId
     * taskId 为空时，期望直接返回失败（不调用 HttpClient）
     */
    @Test
    void test_uploadParamfilesEmptyTaskId() throws IOException {
        // 准备临时文件
        File tempFile = tempDir.resolve("param.dat").toFile();
        tempFile.createNewFile();

        // 执行：taskId 为空
        AidResponse<UploadFilesResponse> result =
                taskService.uploadParamfiles("", Arrays.asList(tempFile));

        // 断言：应返回失败
        assertFalse(result.isSuccess(), "空 taskId 时 uploadParamfiles 应返回失败");
        assertEquals(303, result.getCode(),
                "失败响应 code 应为 303（FILE_UPLOAD_FAILED）");
        verify(mockHttpClient, never()).postMultipart(anyString(), anyMap(), anyMap());
    }

    // ================================================================
    // API 3: newTaskverify
    // ================================================================

    /**
     * test_newTaskverifySuccess
     * mock post() 返回 ready=true 的 JSON，验证文件校验成功路径
     */
    @Test
    void test_newTaskverifySuccess() {
        // 安排：校验通过，ready=true
        String responseJson = "{\"code\":200,\"taskID\":\"T001\",\"ready\":true,\"leftFileList\":[],\"message\":\"校验通过\"}";
        when(mockHttpClient.post(eq("/newTaskverify"), anyMap()))
                .thenReturn(AidResponse.success(responseJson, "成功"));

        // 执行
        AidResponse<VerifyResponse> result = taskService.newTaskverify("T001");

        // 断言
        assertTrue(result.isSuccess(), "文件校验成功时 newTaskverify 应返回成功响应");
        assertNotNull(result.getData(), "响应 data 不应为 null");
        assertTrue(result.getData().isReady(), "ready 字段应为 true");
    }

    // ================================================================
    // API 4: startTask
    // ================================================================

    /**
     * test_startTaskSuccess
     * mock post() 返回 status=running，验证任务启动成功路径
     */
    @Test
    void test_startTaskSuccess() {
        // 安排：任务已启动，状态 running
        String responseJson = "{\"code\":200,\"taskID\":\"T001\",\"status\":\"running\",\"message\":\"任务已启动\"}";
        when(mockHttpClient.post(eq("/startTask"), anyMap()))
                .thenReturn(AidResponse.success(responseJson, "成功"));

        // 执行
        AidResponse<TaskStatusResponse> result = taskService.startTask("T001");

        // 断言
        assertTrue(result.isSuccess(), "startTask 应返回成功响应");
        assertNotNull(result.getData(), "响应 data 不应为 null");
        assertEquals("running", result.getData().getStatus(),
                "任务状态应为 running");
    }

    // ================================================================
    // API 5: queryTaskStatus
    // ================================================================

    /**
     * test_queryTaskStatusSuccess
     * mock post() 返回 status=finished，验证任务状态查询成功路径
     */
    @Test
    void test_queryTaskStatusSuccess() {
        // 安排：任务已完成
        String responseJson = "{\"code\":200,\"taskID\":\"T001\",\"status\":\"finished\",\"message\":\"任务完成\"}";
        when(mockHttpClient.post(eq("/queryTaskStatus"), anyMap()))
                .thenReturn(AidResponse.success(responseJson, "成功"));

        // 执行
        AidResponse<TaskStatusResponse> result = taskService.queryTaskStatus("T001");

        // 断言
        assertTrue(result.isSuccess(), "queryTaskStatus 应返回成功响应");
        assertEquals("finished", result.getData().getStatus(),
                "任务状态应为 finished");
    }

    // ================================================================
    // API 6: stopTask
    // ================================================================

    /**
     * test_stopTaskSuccess
     * mock post() 返回 status=stopped，验证任务停止成功路径
     */
    @Test
    void test_stopTaskSuccess() {
        // 安排：任务已停止
        String responseJson = "{\"code\":200,\"taskID\":\"T001\",\"status\":\"stopped\",\"message\":\"任务已停止\"}";
        when(mockHttpClient.post(eq("/stopTask"), anyMap()))
                .thenReturn(AidResponse.success(responseJson, "成功"));

        // 执行
        AidResponse<TaskStatusResponse> result = taskService.stopTask("T001");

        // 断言
        assertTrue(result.isSuccess(), "stopTask 应返回成功响应");
        assertEquals("stopped", result.getData().getStatus(),
                "任务状态应为 stopped");
    }

    // ================================================================
    // API 7: deleteTask
    // ================================================================

    /**
     * test_deleteTaskSuccess
     * mock post() 返回 status=deleted，验证任务删除成功路径
     */
    @Test
    void test_deleteTaskSuccess() {
        // 安排：任务已删除
        String responseJson = "{\"code\":200,\"taskID\":\"T001\",\"status\":\"deleted\",\"message\":\"任务已删除\"}";
        when(mockHttpClient.post(eq("/deleteTask"), anyMap()))
                .thenReturn(AidResponse.success(responseJson, "成功"));

        // 执行
        AidResponse<TaskStatusResponse> result = taskService.deleteTask("T001");

        // 断言
        assertTrue(result.isSuccess(), "deleteTask 应返回成功响应");
        assertEquals("deleted", result.getData().getStatus(),
                "任务状态应为 deleted");
    }

    // ================================================================
    // API 8: fetchTaskResult
    // ================================================================

    /**
     * test_fetchTaskResultSuccess
     * mock post() 返回包含结果文件路径的 JSON，
     * mock download() 返回模拟二进制数据，
     * 验证结果文件能正确写入本地输出目录
     */
    @Test
    void test_fetchTaskResultSuccess() {
        // 安排：结果接口返回 resultFilePath
        String responseJson = "{\"code\":200,\"taskID\":\"T001\",\"message\":\"获取成功\",\"resultFilePath\":\"/data/result/T001.zip\"}";
        when(mockHttpClient.post(eq("/fetchTaskResult"), anyMap()))
                .thenReturn(AidResponse.success(responseJson, "成功"));

        // 安排：下载接口返回模拟字节数据
        byte[] fakeBytes = "FAKE_ZIP_CONTENT".getBytes();
        when(mockHttpClient.download(eq("/fetchTaskResult/download"), anyMap()))
                .thenReturn(fakeBytes);

        // 执行：指定输出目录为临时目录
        String outputPath = tempDir.toAbsolutePath().toString();
        AidResponse<TaskResultResponse> result = taskService.fetchTaskResult("T001", outputPath);

        // 断言：响应应为成功
        assertTrue(result.isSuccess(), "fetchTaskResult 应返回成功响应");
        assertNotNull(result.getData(), "响应 data 不应为 null");
        assertEquals("/data/result/T001.zip", result.getData().getResultFilePath(),
                "resultFilePath 应与 JSON 中的值一致");

        // 断言：结果文件应已写入本地
        File downloadedFile = new File(outputPath, "T001.zip");
        assertTrue(downloadedFile.exists(), "下载的结果文件应存在于输出目录中");
        assertEquals(fakeBytes.length, downloadedFile.length(),
                "下载文件大小应与模拟数据一致");
    }
}
