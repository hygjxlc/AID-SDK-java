package com.aid.sdk.examples;

import com.aid.sdk.AidClient;
import com.aid.sdk.common.AidException;
import com.aid.sdk.common.AidResponse;
import com.aid.sdk.task.model.TaskCreateResponse;
import com.aid.sdk.task.model.TaskResultResponse;
import com.aid.sdk.task.model.TaskStatusResponse;
import com.aid.sdk.task.model.UploadFilesResponse;
import com.aid.sdk.task.model.VerifyResponse;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AID Client SDK Java 使用示例
 * <p>
 * 本示例展示了使用 AID Client SDK 完成金属加工仿真任务的完整工作流程：
 * <ol>
 *   <li>创建仿真任务</li>
 *   <li>上传参数文件</li>
 *   <li>校验文件完整性</li>
 *   <li>启动任务</li>
 *   <li>轮询查询任务状态</li>
 *   <li>下载仿真结果</li>
 * </ol>
 * <p>
 * 运行前请确保：
 * <ul>
 *   <li>已构建 SDK JAR：mvn clean package</li>
 *   <li>已在 CONFIG_PATH 指向的路径准备好配置文件</li>
 *   <li>配置文件中已填写正确的 baseURL 和 api_token</li>
 * </ul>
 */
public class BasicUsage {

    // ============================================================
    // 配置项：根据实际情况修改以下常量
    // ============================================================

    /** 配置文件路径（包含 baseURL 和 api_token） */
    private static final String CONFIG_PATH = "./config/config.properties";

    /**
     * 仿真类型（可选值：LaWan / CHOnYA / ZhuZao / ZhaZhi / ZHEWan / JIYA）
     */
    private static final String SIMULATE_TYPE = "LaWan";

    /** 任务名称（自定义，便于识别） */
    private static final String TASK_NAME = "java_demo_task_001";

    /**
     * 需要上传的参数文件路径列表。
     * 支持格式：stp / txt / csv / yml / jnl
     * 单文件 ≤ 100MB，总大小 ≤ 500MB
     */
    private static final String[] PARAM_FILE_PATHS = {
        "./data/model.stp",        // 三维模型文件
        "./data/params.csv",       // 仿真参数文件
        "./data/boundary.yml",     // 边界条件文件
    };

    /** 仿真结果下载目录 */
    private static final String OUTPUT_DIR = "./results/";

    /** 状态轮询间隔（毫秒） */
    private static final long POLL_INTERVAL_MS = 10_000L;

    // ============================================================
    // 错误码建议映射表
    // ============================================================
    private static final Map<Integer, String> ERROR_HINTS = new HashMap<>();

    static {
        ERROR_HINTS.put(301, "TaskID 不存在，请确认任务 ID 是否正确");
        ERROR_HINTS.put(302, "当前任务状态不允许此操作，请检查任务流程");
        ERROR_HINTS.put(303, "文件格式不支持或超出大小限制（stp/txt/csv/yml/jnl，单文件≤100MB，总量≤500MB）");
        ERROR_HINTS.put(401, "API Key 无效，请检查 config.properties 中的 api_token 配置");
        ERROR_HINTS.put(402, "请求参数有误，请检查传入参数是否正确");
        ERROR_HINTS.put(403, "权限不足，请联系管理员确认账号权限");
        ERROR_HINTS.put(500, "服务器内部错误，请联系 AID 技术支持");
    }

    /**
     * 程序入口
     *
     * @param args 命令行参数（本示例不使用）
     */
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("AID Client SDK Java 使用示例");
        System.out.println("仿真类型: " + SIMULATE_TYPE);
        System.out.println("任务名称: " + TASK_NAME);
        System.out.println("==================================================");

        new BasicUsage().run();
    }

    /**
     * 执行完整仿真流程
     */
    public void run() {
        // 记录任务 ID，供后续步骤使用
        String taskId = null;

        try {
            // --------------------------------------------------
            // 步骤 0：初始化 AidClient
            // --------------------------------------------------
            // AidClient 会自动读取配置文件中的 baseURL 和 api_token
            // 后续所有 API 调用均通过此客户端实例发起
            System.out.println("\n[初始化] 加载配置文件并创建 AidClient...");
            AidClient client = new AidClient(CONFIG_PATH);
            System.out.println("AidClient 初始化成功");

            // --------------------------------------------------
            // 步骤 1：创建仿真任务
            // --------------------------------------------------
            System.out.println("\n[步骤 1/6] 创建仿真任务...");
            AidResponse<TaskCreateResponse> createResult = client.newTaskCreate(SIMULATE_TYPE, TASK_NAME);

            // 检查响应码，200 表示成功
            if (!createResult.isSuccess() || createResult.getData() == null) {
                throw new RuntimeException(
                    String.format("创建任务失败 [code=%d]: %s", createResult.getCode(), createResult.getMessage())
                );
            }

            TaskCreateResponse createResp = createResult.getData();
            // 保存任务 ID，后续所有操作均需要它
            taskId = createResp.getTaskID();
            System.out.println("✓ 任务创建成功，TaskID: " + taskId);

            // --------------------------------------------------
            // 步骤 2：上传参数文件
            // --------------------------------------------------
            System.out.println("\n[步骤 2/6] 上传参数文件...");

            // 将文件路径转换为 File 对象列表
            List<File> paramFiles = new ArrayList<>();
            for (String path : PARAM_FILE_PATHS) {
                File file = new File(path);
                paramFiles.add(file);
                System.out.println("  待上传: " + file.getPath());
            }

            AidResponse<UploadFilesResponse> uploadResult = client.uploadParamfiles(taskId, paramFiles);

            if (!uploadResult.isSuccess() || uploadResult.getData() == null) {
                throw new RuntimeException(
                    String.format("文件上传失败 [code=%d]: %s", uploadResult.getCode(), uploadResult.getMessage())
                );
            }

            UploadFilesResponse uploadResp = uploadResult.getData();
            // 打印服务端已接收到的文件列表
            System.out.println("✓ 文件上传成功，已上传文件: " + uploadResp.getFileList());

            // --------------------------------------------------
            // 步骤 3：校验文件完整性
            // --------------------------------------------------
            // 服务端会检查任务所需的必要文件是否已全部上传
            System.out.println("\n[步骤 3/6] 校验文件完整性...");
            AidResponse<VerifyResponse> verifyResult = client.newTaskverify(taskId);

            if (!verifyResult.isSuccess() || verifyResult.getData() == null) {
                throw new RuntimeException(
                    String.format("文件校验请求失败 [code=%d]: %s", verifyResult.getCode(), verifyResult.getMessage())
                );
            }

            VerifyResponse verifyResp = verifyResult.getData();
            // ready=false 说明仍有文件未上传
            if (!verifyResp.isReady()) {
                List<String> missingFiles = verifyResp.getLeftFileList();
                System.err.println("✗ 文件校验未通过，以下文件仍需上传:");
                for (String f : missingFiles) {
                    System.err.println("   - " + f);
                }
                throw new RuntimeException("文件不完整，请补充上传后重试");
            }

            System.out.println("✓ 文件校验通过，任务已就绪，可以启动");

            // --------------------------------------------------
            // 步骤 4：启动仿真任务
            // --------------------------------------------------
            System.out.println("\n[步骤 4/6] 启动仿真任务...");
            AidResponse<TaskStatusResponse> startResult = client.startTask(taskId);

            if (!startResult.isSuccess() || startResult.getData() == null) {
                throw new RuntimeException(
                    String.format("启动任务失败 [code=%d]: %s", startResult.getCode(), startResult.getMessage())
                );
            }

            TaskStatusResponse startResp = startResult.getData();
            System.out.println("✓ 任务启动成功，当前状态: " + startResp.getStatus());

            // --------------------------------------------------
            // 步骤 5：轮询查询任务状态
            // --------------------------------------------------
            System.out.println(
                String.format("\n[步骤 5/6] 等待任务完成（每 %d 秒查询一次）...", POLL_INTERVAL_MS / 1000)
            );

            while (true) {
                // 查询当前任务状态
                AidResponse<TaskStatusResponse> statusResult = client.queryTaskStatus(taskId);

                if (!statusResult.isSuccess() || statusResult.getData() == null) {
                    System.out.println("  警告：查询状态失败 [code=" + statusResult.getCode() + "]，继续等待...");
                    Thread.sleep(POLL_INTERVAL_MS);
                    continue;
                }

                TaskStatusResponse statusResp = statusResult.getData();
                String status = statusResp.getStatus();

                // 提取进度信息（如果有的话）
                Object extraInfo = statusResp.getExtraInfo();
                String progressStr = "";
                if (extraInfo instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> infoMap = (Map<String, Object>) extraInfo;
                    Object progress = infoMap.get("progress");
                    Object currentStep = infoMap.get("currentStep");
                    if (progress != null) {
                        progressStr += " | 进度: " + progress + "%";
                    }
                    if (currentStep != null) {
                        progressStr += " | 当前步骤: " + currentStep;
                    }
                }

                System.out.println("  状态: " + status + progressStr);

                // 任务完成，退出轮询
                if ("COMPLETED".equals(status)) {
                    System.out.println("✓ 任务计算完成！");
                    break;
                }

                // 任务异常结束（失败）
                if ("FAILED".equals(status)) {
                    throw new RuntimeException("仿真任务执行失败，请查看任务日志排查原因");
                }

                // 任务被停止
                if ("STOPPED".equals(status)) {
                    throw new RuntimeException("仿真任务已被停止");
                }

                // 任务仍在运行，等待后继续轮询
                Thread.sleep(POLL_INTERVAL_MS);
            }

            // --------------------------------------------------
            // 步骤 6：下载仿真结果
            // --------------------------------------------------
            System.out.println("\n[步骤 6/6] 下载仿真结果...");

            // 确保输出目录存在
            File outputDir = new File(OUTPUT_DIR);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            AidResponse<TaskResultResponse> resultResult = client.fetchTaskResult(taskId, OUTPUT_DIR);
            if (!resultResult.isSuccess() || resultResult.getData() == null) {
                throw new RuntimeException(
                    String.format("下载结果失败 [code=%d]: %s", resultResult.getCode(), resultResult.getMessage())
                );
            }

            TaskResultResponse resultResp = resultResult.getData();
            System.out.println("✓ 仿真结果已下载至: " + resultResp.getResultFilePath());

        } catch (AidException e) {
            // 处理 SDK 抛出的异常
            System.err.println("\n✗ SDK 异常 [code=" + e.getErrorCode() + "]: " + e.getErrorMessage());
            String hint = ERROR_HINTS.getOrDefault(e.getErrorCode(), "请参阅 API 文档或联系技术支持");
            System.err.println("  提示：" + hint);

        } catch (RuntimeException e) {
            // 处理流程中的业务错误
            System.err.println("\n✗ 流程错误: " + e.getMessage());

        } catch (InterruptedException e) {
            // 处理线程中断
            Thread.currentThread().interrupt();
            System.err.println("\n✗ 程序被中断");

        } catch (Exception e) {
            // 处理其他未预期的异常
            System.err.println("\n✗ 未知异常 [" + e.getClass().getSimpleName() + "]: " + e.getMessage());

        } finally {
            // 结束时打印摘要
            System.out.println("\n==================================================");
            System.out.println("示例执行结束");
            if (taskId != null) {
                System.out.println("任务 ID: " + taskId);
            }
            System.out.println("==================================================");
        }
    }
}
