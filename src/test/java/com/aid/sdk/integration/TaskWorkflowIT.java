package com.aid.sdk.integration;

import com.aid.sdk.common.AidResponse;
import com.aid.sdk.http.HttpClient;
import com.aid.sdk.http.OkHttpClientImpl;
import com.aid.sdk.task.TaskService;
import com.aid.sdk.task.model.TaskCreateResponse;
import com.aid.sdk.task.model.TaskResultResponse;
import com.aid.sdk.task.model.TaskStatusResponse;
import com.aid.sdk.task.model.UploadFilesResponse;
import com.aid.sdk.task.model.VerifyResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskService 集成测试
 * 使用 WireMock 搭建 HTTP 桩服务，通过真实 OkHttpClient 发起请求，
 * 端到端验证金属加工仿真任务的完整工作流
 */
class TaskWorkflowIT {

    /** WireMock 桩服务器实例 */
    private WireMockServer wireMockServer;

    /** 真实 HTTP 客户端（对接 WireMock 端口） */
    private HttpClient httpClient;

    /** 被测 TaskService */
    private TaskService taskService;

    /** 临时输出目录（用于结果文件下载） */
    @TempDir
    Path tempDir;

    /** 测试用任务 ID */
    private static final String TASK_ID = "IT_TASK_001";

    @BeforeEach
    void setUp() {
        // 启动 WireMock，动态随机端口，避免端口冲突
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();

        // 基础 URL 指向 WireMock 本地端口
        String baseUrl = "http://localhost:" + wireMockServer.port();
        httpClient = new OkHttpClientImpl(baseUrl);
        taskService = new TaskService(httpClient);
    }

    @AfterEach
    void tearDown() {
        // 每个测试完成后停止桩服务器，释放端口
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    // ================================================================
    // 集成测试 1：完整任务生命周期（创建→上传→校验→启动→查询→停止→删除）
    // ================================================================

    /**
     * test_fullTaskLifecycle
     * 桩所有 7 个端点，按正确顺序依次调用，
     * 验证 TaskService 在真实 HTTP 通信下各步骤均能正常工作
     */
    @Test
    void test_fullTaskLifecycle() throws IOException {
        // ---- 1. 桩：POST /newTaskCreate ----
        wireMockServer.stubFor(post(urlEqualTo("/newTaskCreate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":200,\"taskID\":\"" + TASK_ID + "\",\"message\":\"任务创建成功\"}")));

        // ---- 2. 桩：POST /uploadParamfiles（multipart 上传）----
        wireMockServer.stubFor(post(urlEqualTo("/uploadParamfiles"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":200,\"taskID\":\"" + TASK_ID + "\",\"message\":\"文件上传成功\",\"fileList\":[\"param.dat\"]}")));

        // ---- 3. 桩：POST /newTaskverify ----
        wireMockServer.stubFor(post(urlEqualTo("/newTaskverify"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":200,\"taskID\":\"" + TASK_ID + "\",\"ready\":true,\"leftFileList\":[],\"message\":\"文件校验通过\"}")));

        // ---- 4. 桩：POST /startTask ----
        wireMockServer.stubFor(post(urlEqualTo("/startTask"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":200,\"taskID\":\"" + TASK_ID + "\",\"status\":\"running\",\"message\":\"任务已启动\"}")));

        // ---- 5. 桩：POST /queryTaskStatus ----
        wireMockServer.stubFor(post(urlEqualTo("/queryTaskStatus"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":200,\"taskID\":\"" + TASK_ID + "\",\"status\":\"running\",\"message\":\"任务运行中\"}")));

        // ---- 6. 桩：POST /stopTask ----
        wireMockServer.stubFor(post(urlEqualTo("/stopTask"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":200,\"taskID\":\"" + TASK_ID + "\",\"status\":\"stopped\",\"message\":\"任务已停止\"}")));

        // ---- 7. 桩：POST /deleteTask ----
        wireMockServer.stubFor(post(urlEqualTo("/deleteTask"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":200,\"taskID\":\"" + TASK_ID + "\",\"status\":\"deleted\",\"message\":\"任务已删除\"}")));

        // ================================================================
        // 执行：按生命周期顺序依次调用 API
        // ================================================================

        // 步骤 1：创建任务（仿真类型 LaWan）
        AidResponse<TaskCreateResponse> createResp = taskService.newTaskCreate("LaWan", "it_test_task");
        assertTrue(createResp.isSuccess(), "集成测试 - 创建任务应成功");
        assertEquals(TASK_ID, createResp.getData().getTaskID(),
                "集成测试 - taskID 应与桩返回值一致");

        // 步骤 2：上传参数文件
        File tempFile = tempDir.resolve("param.dat").toFile();
        tempFile.createNewFile();
        AidResponse<UploadFilesResponse> uploadResp =
                taskService.uploadParamfiles(TASK_ID, Arrays.asList(tempFile));
        assertTrue(uploadResp.isSuccess(), "集成测试 - 上传文件应成功");
        assertFalse(uploadResp.getData().getFileList().isEmpty(),
                "集成测试 - 上传文件列表不应为空");

        // 步骤 3：校验文件完整性
        AidResponse<VerifyResponse> verifyResp = taskService.newTaskverify(TASK_ID);
        assertTrue(verifyResp.isSuccess(), "集成测试 - 文件校验应成功");
        assertTrue(verifyResp.getData().isReady(), "集成测试 - ready 应为 true");

        // 步骤 4：启动仿真任务
        AidResponse<TaskStatusResponse> startResp = taskService.startTask(TASK_ID);
        assertTrue(startResp.isSuccess(), "集成测试 - 启动任务应成功");
        assertEquals("running", startResp.getData().getStatus(),
                "集成测试 - 启动后状态应为 running");

        // 步骤 5：查询任务状态
        AidResponse<TaskStatusResponse> statusResp = taskService.queryTaskStatus(TASK_ID);
        assertTrue(statusResp.isSuccess(), "集成测试 - 查询状态应成功");
        assertEquals("running", statusResp.getData().getStatus(),
                "集成测试 - 查询状态应为 running");

        // 步骤 6：停止任务
        AidResponse<TaskStatusResponse> stopResp = taskService.stopTask(TASK_ID);
        assertTrue(stopResp.isSuccess(), "集成测试 - 停止任务应成功");
        assertEquals("stopped", stopResp.getData().getStatus(),
                "集成测试 - 停止后状态应为 stopped");

        // 步骤 7：删除任务
        AidResponse<TaskStatusResponse> deleteResp = taskService.deleteTask(TASK_ID);
        assertTrue(deleteResp.isSuccess(), "集成测试 - 删除任务应成功");
        assertEquals("deleted", deleteResp.getData().getStatus(),
                "集成测试 - 删除后状态应为 deleted");

        // 验证：所有关键端点均被调用过至少 1 次
        wireMockServer.verify(postRequestedFor(urlEqualTo("/newTaskCreate")));
        wireMockServer.verify(postRequestedFor(urlEqualTo("/uploadParamfiles")));
        wireMockServer.verify(postRequestedFor(urlEqualTo("/newTaskverify")));
        wireMockServer.verify(postRequestedFor(urlEqualTo("/startTask")));
        wireMockServer.verify(postRequestedFor(urlEqualTo("/queryTaskStatus")));
        wireMockServer.verify(postRequestedFor(urlEqualTo("/stopTask")));
        wireMockServer.verify(postRequestedFor(urlEqualTo("/deleteTask")));
    }

    // ================================================================
    // 集成测试 2：结果获取流程（创建→上传→校验→启动→查询完成→下载结果）
    // ================================================================

    /**
     * test_taskResultFlow
     * 桩 6 个端点（含二进制下载），验证从任务创建到结果文件下载的完整链路
     */
    @Test
    void test_taskResultFlow() throws IOException {
        // ---- 1. 桩：POST /newTaskCreate ----
        wireMockServer.stubFor(post(urlEqualTo("/newTaskCreate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":200,\"taskID\":\"" + TASK_ID + "\",\"message\":\"任务创建成功\"}")));

        // ---- 2. 桩：POST /uploadParamfiles ----
        wireMockServer.stubFor(post(urlEqualTo("/uploadParamfiles"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":200,\"taskID\":\"" + TASK_ID + "\",\"message\":\"上传成功\",\"fileList\":[\"model.inp\"]}")));

        // ---- 3. 桩：POST /newTaskverify ----
        wireMockServer.stubFor(post(urlEqualTo("/newTaskverify"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":200,\"taskID\":\"" + TASK_ID + "\",\"ready\":true,\"leftFileList\":[],\"message\":\"校验通过\"}")));

        // ---- 4. 桩：POST /startTask ----
        wireMockServer.stubFor(post(urlEqualTo("/startTask"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":200,\"taskID\":\"" + TASK_ID + "\",\"status\":\"running\",\"message\":\"已启动\"}")));

        // ---- 5. 桩：POST /queryTaskStatus（返回 finished）----
        wireMockServer.stubFor(post(urlEqualTo("/queryTaskStatus"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":200,\"taskID\":\"" + TASK_ID + "\",\"status\":\"finished\",\"message\":\"仿真完成\"}")));

        // ---- 6. 桩：POST /fetchTaskResult（返回结果文件路径）----
        wireMockServer.stubFor(post(urlEqualTo("/fetchTaskResult"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":200,\"taskID\":\"" + TASK_ID + "\",\"message\":\"获取成功\",\"resultFilePath\":\"/data/results/" + TASK_ID + ".zip\"}")));

        // ---- 7. 桩：POST /fetchTaskResult/download（返回模拟二进制文件）----
        byte[] fakeZipContent = "PK_FAKE_ZIP_BINARY_CONTENT".getBytes();
        wireMockServer.stubFor(post(urlEqualTo("/fetchTaskResult/download"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/octet-stream")
                        .withBody(fakeZipContent)));

        // ================================================================
        // 执行：结果获取完整流程
        // ================================================================

        // 步骤 1：创建任务
        AidResponse<TaskCreateResponse> createResp = taskService.newTaskCreate("CHOnYA", "result_flow_task");
        assertTrue(createResp.isSuccess(), "结果流集成测试 - 创建任务应成功");
        assertEquals(TASK_ID, createResp.getData().getTaskID(),
                "结果流集成测试 - taskID 应正确");

        // 步骤 2：上传仿真模型文件
        File modelFile = tempDir.resolve("model.inp").toFile();
        modelFile.createNewFile();
        AidResponse<UploadFilesResponse> uploadResp =
                taskService.uploadParamfiles(TASK_ID, Arrays.asList(modelFile));
        assertTrue(uploadResp.isSuccess(), "结果流集成测试 - 上传文件应成功");

        // 步骤 3：校验文件
        AidResponse<VerifyResponse> verifyResp = taskService.newTaskverify(TASK_ID);
        assertTrue(verifyResp.isSuccess(), "结果流集成测试 - 校验应成功");
        assertTrue(verifyResp.getData().isReady(), "结果流集成测试 - ready 应为 true");

        // 步骤 4：启动仿真
        AidResponse<TaskStatusResponse> startResp = taskService.startTask(TASK_ID);
        assertTrue(startResp.isSuccess(), "结果流集成测试 - 启动应成功");

        // 步骤 5：查询完成状态
        AidResponse<TaskStatusResponse> statusResp = taskService.queryTaskStatus(TASK_ID);
        assertTrue(statusResp.isSuccess(), "结果流集成测试 - 查询应成功");
        assertEquals("finished", statusResp.getData().getStatus(),
                "结果流集成测试 - 状态应为 finished");

        // 步骤 6：获取并下载结果文件
        String outputPath = tempDir.toAbsolutePath().toString();
        AidResponse<TaskResultResponse> resultResp = taskService.fetchTaskResult(TASK_ID, outputPath);
        assertTrue(resultResp.isSuccess(), "结果流集成测试 - 获取结果应成功");
        assertNotNull(resultResp.getData().getResultFilePath(),
                "结果流集成测试 - resultFilePath 不应为 null");

        // 断言：结果 ZIP 文件已下载到本地
        File downloadedZip = new File(outputPath, TASK_ID + ".zip");
        assertTrue(downloadedZip.exists(), "结果流集成测试 - 下载的 ZIP 文件应存在于输出目录");
        assertEquals(fakeZipContent.length, downloadedZip.length(),
                "结果流集成测试 - 下载文件大小应与桩数据一致");

        // 验证：结果相关端点均被正确调用
        wireMockServer.verify(postRequestedFor(urlEqualTo("/fetchTaskResult")));
        wireMockServer.verify(postRequestedFor(urlEqualTo("/fetchTaskResult/download")));
    }
}
