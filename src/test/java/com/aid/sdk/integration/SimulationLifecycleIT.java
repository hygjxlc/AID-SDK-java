package com.aid.sdk.integration;

import com.aid.sdk.common.AidResponse;
import com.aid.sdk.http.HttpClient;
import com.aid.sdk.http.OkHttpClientImpl;
import com.aid.sdk.task.TaskService;
import com.aid.sdk.task.model.TaskCreateResponse;
import com.aid.sdk.task.model.TaskResultResponse;
import com.aid.sdk.task.model.TaskStatusResponse;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SimulationLifecycleIT - 仿真任务生命周期集成测试
 *
 * 验证本次优化的核心功能：
 *   1. 任务启动后状态应变为 running
 *   2. 仿真完成后状态变为 finished，结果文件以 {taskId}result.stp 命名
 *   3. 在完成之前收到 stop 指令 → 不产生结果文件，任务停止
 *   4. 在完成之前收到 delete 指令 → 不产生结果文件，任务被删除
 *   5. stop 后再查询状态应为 stop
 *   6. 对运行中任务执行 delete 应成功
 *   7. 结果文件大小应为 500 KB
 *
 * 测试策略：使用 WireMock 作为 HTTP 桩服务，通过真实 OkHttpClient 调用，
 * 端到端验证 Java SDK 与 AID-Service API 的交互链路。
 */
class SimulationLifecycleIT {

    private WireMockServer wireMockServer;
    private HttpClient httpClient;
    private TaskService taskService;

    @TempDir
    Path tempDir;

    /** 测试使用的固定任务 ID（模拟 AID-Service 分配的 LaWan 序列号） */
    private static final String TASK_ID = "LaWan00000001";

    /** 期望的结果文件名：{taskId}result.stp */
    private static final String EXPECTED_RESULT_FILENAME = TASK_ID + "result.stp";

    /** 500 KB 的模拟结果文件内容 */
    private static final byte[] FAKE_RESULT_BYTES = new byte[500 * 1024];

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        String baseUrl = "http://localhost:" + wireMockServer.port();
        httpClient = new OkHttpClientImpl(baseUrl);
        taskService = new TaskService(httpClient);
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    // ====================================================================
    // TC-1: 任务启动后状态应立即变为 running
    // ====================================================================

    /**
     * TC-1: test_startTask_statusBecomesRunning
     *
     * 场景：调用 startTask，服务端接受请求并将状态更新为 running。
     * 验证：SDK 正确解析服务端返回的 running 状态。
     */
    @Test
    void test_startTask_statusBecomesRunning() {
        // 桩：startTask 返回 running
        stubStartTask("running");

        AidResponse<TaskStatusResponse> resp = taskService.startTask(TASK_ID);

        assertTrue(resp.isSuccess(), "TC-1: startTask 应返回成功响应");
        assertNotNull(resp.getData(), "TC-1: 响应 data 不应为 null");
        assertEquals("running", resp.getData().getStatus(),
                "TC-1: 启动后任务状态应为 running");
        assertEquals(TASK_ID, resp.getData().getTaskID(),
                "TC-1: 响应中 taskID 应与请求一致");

        // 验证 SDK 确实调用了 /startTask 端点
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/startTask")));
    }

    // ====================================================================
    // TC-2: 仿真完成后状态变为 finished
    // ====================================================================

    /**
     * TC-2: test_simulationCompletes_statusFinished
     *
     * 场景：任务启动后，轮询 queryTaskStatus，服务端返回 finished（代表 5 分钟后计时器触发）。
     * 验证：SDK 能正确解析 finished 状态，客户端可据此进入结果下载阶段。
     */
    @Test
    void test_simulationCompletes_statusFinished() {
        // 桩：queryTaskStatus 返回 finished
        wireMockServer.stubFor(post(urlEqualTo("/queryTaskStatus"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":200,\"taskID\":\"" + TASK_ID + "\","
                                + "\"status\":\"finished\",\"message\":\"仿真完成\"}")));

        AidResponse<TaskStatusResponse> resp = taskService.queryTaskStatus(TASK_ID);

        assertTrue(resp.isSuccess(), "TC-2: queryTaskStatus 应返回成功响应");
        assertEquals("finished", resp.getData().getStatus(),
                "TC-2: 仿真完成后状态应为 finished");

        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/queryTaskStatus")));
    }

    // ====================================================================
    // TC-3: 结果文件名应为 {taskId}result.stp，大小为 500 KB
    // ====================================================================

    /**
     * TC-3: test_resultFile_namedByTaskId_and_500KB
     *
     * 场景：任务完成后调用 fetchTaskResult，服务端返回文件名为 {taskId}result.stp 的结果文件。
     * 验证：
     *   (a) 文件名格式为 {taskId}result.stp
     *   (b) 下载文件大小为 500 KB
     */
    @Test
    void test_resultFile_namedByTaskId_and_500KB() throws IOException {
        // 桩：fetchTaskResult 直接返回 500 KB 二进制内容
        stubFetchTaskResultMeta(EXPECTED_RESULT_FILENAME);

        String outputDir = tempDir.toAbsolutePath().toString();
        AidResponse<TaskResultResponse> resp = taskService.fetchTaskResult(TASK_ID, outputDir);

        assertTrue(resp.isSuccess(), "TC-3: fetchTaskResult 应返回成功响应");
        assertNotNull(resp.getData().getResultFilePath(),
                "TC-3: resultFilePath 不应为 null");

        // 验证文件名规则：{taskId}result.stp（SDK 写本地文件名）
        String localFilePath = resp.getData().getResultFilePath();
        assertTrue(localFilePath.endsWith(EXPECTED_RESULT_FILENAME),
                "TC-3: 本地结果文件路径应以 " + EXPECTED_RESULT_FILENAME + " 结尾，实际为: " + localFilePath);

        // 验证本地下载文件存在且大小为 500 KB
        File downloadedFile = new File(outputDir, EXPECTED_RESULT_FILENAME);
        assertTrue(downloadedFile.exists(),
                "TC-3: 本地应存在下载的结果文件 " + EXPECTED_RESULT_FILENAME);
        assertEquals(500 * 1024, downloadedFile.length(),
                "TC-3: 结果文件大小应为 500 KB (512000 bytes)");
    }

    // ====================================================================
    // TC-4: stop 在完成之前 → 不应产生结果文件，fetchTaskResult 应失败
    // ====================================================================

    /**
     * TC-4: test_stopBeforeFinish_fetchResultFails
     *
     * 场景：任务 running 期间调用 stopTask，此后再调用 fetchTaskResult。
     * 期望：服务端返回 HTTP 500（任务非 finished 状态不可下载），SDK 应返回失败响应。
     * 验证：fetchTaskResult 失败，本地无结果文件。
     */
    @Test
    void test_stopBeforeFinish_fetchResultFails() {
        // 桩：stopTask 成功，状态变为 stop
        stubStopTask("stop");

        // 桩：fetchTaskResult/download 返回 HTTP 500（任务未完成，服务端拒绝下载）
        wireMockServer.stubFor(post(urlEqualTo("/fetchTaskResult"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":500,\"message\":\"任务未完成: stop\"}")));

        // 执行停止
        AidResponse<TaskStatusResponse> stopResp = taskService.stopTask(TASK_ID);
        assertTrue(stopResp.isSuccess(), "TC-4: stopTask 应成功");
        assertEquals("stop", stopResp.getData().getStatus(),
                "TC-4: 停止后状态应为 stop");

        // 执行结果获取（应失败，因为 download 收到 HTTP 500 抛出 AidException）
        AidResponse<TaskResultResponse> resultResp =
                taskService.fetchTaskResult(TASK_ID, tempDir.toAbsolutePath().toString());

        assertFalse(resultResp.isSuccess(),
                "TC-4: stop 后调用 fetchTaskResult 应返回失败（任务未完成）");

        // 验证本地无结果文件
        File resultFile = new File(tempDir.toAbsolutePath().toString(), EXPECTED_RESULT_FILENAME);
        assertFalse(resultFile.exists(),
                "TC-4: stop 后本地不应存在结果文件 " + EXPECTED_RESULT_FILENAME);

        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/stopTask")));
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/fetchTaskResult")));
    }

    // ====================================================================
    // TC-5: delete 在完成之前 → 不应产生结果文件，fetchTaskResult 应失败
    // ====================================================================

    /**
     * TC-5: test_deleteBeforeFinish_fetchResultFails
     *
     * 场景：任务 running 期间调用 deleteTask，此后再调用 fetchTaskResult。
     * 期望：任务已删除，服务端返回 HTTP 500，fetchTaskResult 应失败。
     */
    @Test
    void test_deleteBeforeFinish_fetchResultFails() {
        // 桩：deleteTask 成功
        stubDeleteTask("deleted");

        // 桩：fetchTaskResult/download 返回 HTTP 500（任务已被删除）
        wireMockServer.stubFor(post(urlEqualTo("/fetchTaskResult"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":404,\"message\":\"任务不存在\"}")));

        // 执行删除
        AidResponse<TaskStatusResponse> deleteResp = taskService.deleteTask(TASK_ID);
        assertTrue(deleteResp.isSuccess(), "TC-5: deleteTask 应返回成功");

        // 执行结果获取（应失败）
        AidResponse<TaskResultResponse> resultResp =
                taskService.fetchTaskResult(TASK_ID, tempDir.toAbsolutePath().toString());

        assertFalse(resultResp.isSuccess(),
                "TC-5: delete 后调用 fetchTaskResult 应返回失败（任务不存在）");

        // 验证本地无结果文件
        File resultFile = new File(tempDir.toAbsolutePath().toString(), EXPECTED_RESULT_FILENAME);
        assertFalse(resultFile.exists(),
                "TC-5: delete 后本地不应存在结果文件 " + EXPECTED_RESULT_FILENAME);

        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/deleteTask")));
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/fetchTaskResult")));
    }

    // ====================================================================
    // TC-6: stop 后再查询状态应为 stop（不是 finished）
    // ====================================================================

    /**
     * TC-6: test_stopTask_statusBecomesStop_notFinished
     *
     * 场景：stop 后轮询 queryTaskStatus。
     * 验证：状态为 stop，而非 finished，说明计时器已被取消。
     */
    @Test
    void test_stopTask_statusBecomesStop_notFinished() {
        // 桩：stopTask → stop
        stubStopTask("stop");

        // 桩：queryTaskStatus → stop
        wireMockServer.stubFor(post(urlEqualTo("/queryTaskStatus"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":200,\"taskID\":\"" + TASK_ID + "\","
                                + "\"status\":\"stop\",\"message\":\"任务已停止\"}")));

        AidResponse<TaskStatusResponse> stopResp = taskService.stopTask(TASK_ID);
        assertEquals("stop", stopResp.getData().getStatus(),
                "TC-6: stop 后状态应为 stop");

        AidResponse<TaskStatusResponse> queryResp = taskService.queryTaskStatus(TASK_ID);
        assertEquals("stop", queryResp.getData().getStatus(),
                "TC-6: 查询状态仍应为 stop，不应自动变为 finished");
        assertNotEquals("finished", queryResp.getData().getStatus(),
                "TC-6: stop 后状态不应变为 finished（计时器已被取消）");
    }

    // ====================================================================
    // TC-7: 对运行中任务执行 delete 应成功
    // ====================================================================

    /**
     * TC-7: test_deleteRunningTask_succeeds
     *
     * 场景：任务处于 running 状态时直接调用 deleteTask。
     * 验证：删除成功（服务端应先取消计时器再删除任务记录）。
     */
    @Test
    void test_deleteRunningTask_succeeds() {
        // 桩：deleteTask 成功（运行中强制删除）
        stubDeleteTask("deleted");

        AidResponse<TaskStatusResponse> resp = taskService.deleteTask(TASK_ID);

        assertTrue(resp.isSuccess(), "TC-7: 对运行中任务执行 deleteTask 应返回成功");
        assertEquals("deleted", resp.getData().getStatus(),
                "TC-7: 删除后状态应为 deleted");

        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/deleteTask")));
    }

    // ====================================================================
    // TC-8: 完整流程（启动→完成→下载结果文件命名正确）
    // ====================================================================

    /**
     * TC-8: test_fullSimulationFlow_startToFinishedResult
     *
     * 场景：完整仿真链路 start → query(finished) → fetchResult。
     * 验证：
     *   (a) 启动状态为 running
     *   (b) 轮询后状态为 finished
     *   (c) 结果文件名为 {taskId}result.stp，大小 500 KB
     */
    @Test
    void test_fullSimulationFlow_startToFinishedResult() throws IOException {
        // 桩：startTask
        stubStartTask("running");

        // 桩：queryTaskStatus → finished
        wireMockServer.stubFor(post(urlEqualTo("/queryTaskStatus"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":200,\"taskID\":\"" + TASK_ID + "\","
                                + "\"status\":\"finished\",\"message\":\"仿真完成\"}")));

        // 桩：fetchTaskResult → 500 KB 二进制
        stubFetchTaskResultMeta(EXPECTED_RESULT_FILENAME);

        // 步骤 1：启动
        AidResponse<TaskStatusResponse> startResp = taskService.startTask(TASK_ID);
        assertTrue(startResp.isSuccess(), "TC-8: startTask 应成功");
        assertEquals("running", startResp.getData().getStatus(),
                "TC-8: 启动后状态应为 running");

        // 步骤 2：轮询（已完成）
        AidResponse<TaskStatusResponse> statusResp = taskService.queryTaskStatus(TASK_ID);
        assertEquals("finished", statusResp.getData().getStatus(),
                "TC-8: 轮询到完成后状态应为 finished");

        // 步骤 3：下载结果
        String outputDir = tempDir.toAbsolutePath().toString();
        AidResponse<TaskResultResponse> resultResp = taskService.fetchTaskResult(TASK_ID, outputDir);

        assertTrue(resultResp.isSuccess(), "TC-8: fetchTaskResult 应成功");
        assertTrue(resultResp.getData().getResultFilePath().endsWith(EXPECTED_RESULT_FILENAME),
                "TC-8: 结果文件路径应以 " + EXPECTED_RESULT_FILENAME + " 结尾");

        File localFile = new File(outputDir, EXPECTED_RESULT_FILENAME);
        assertTrue(localFile.exists(), "TC-8: 本地应存在结果文件");
        assertEquals(500 * 1024, localFile.length(),
                "TC-8: 结果文件大小应为 500 KB");

        // 验证所有关键端点均已调用
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/startTask")));
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/queryTaskStatus")));
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/fetchTaskResult")));
    }

    // ====================================================================
    // 私有桩辅助方法
    // ====================================================================

    private void stubStartTask(String status) {
        wireMockServer.stubFor(post(urlEqualTo("/startTask"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":200,\"taskID\":\"" + TASK_ID + "\","
                                + "\"status\":\"" + status + "\",\"message\":\"任务启动成功\"}")));
    }

    private void stubStopTask(String status) {
        wireMockServer.stubFor(post(urlEqualTo("/stopTask"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":200,\"taskID\":\"" + TASK_ID + "\","
                                + "\"status\":\"" + status + "\",\"message\":\"任务已停止\"}")));
    }

    private void stubDeleteTask(String status) {
        wireMockServer.stubFor(post(urlEqualTo("/deleteTask"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\":200,\"taskID\":\"" + TASK_ID + "\","
                                + "\"status\":\"" + status + "\",\"message\":\"任务已删除\"}")));
    }

    private void stubFetchTaskResultMeta(String resultFilename) {
        // The SDK calls /fetchTaskResult directly for binary download (not a separate /download endpoint).
        // Return the 500 KB binary content with Content-Disposition header carrying the filename.
        wireMockServer.stubFor(post(urlEqualTo("/fetchTaskResult"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/octet-stream")
                        .withHeader("Content-Disposition",
                                "attachment; filename=\"" + resultFilename + "\"")
                        .withBody(FAKE_RESULT_BYTES)));
    }
}
