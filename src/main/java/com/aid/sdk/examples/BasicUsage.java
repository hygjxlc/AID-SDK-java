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
        "./data/product.stp",              // 三维模型文件（optional）
        "./data/materials.csv",            // 材料参数文件（required）
        "./data/config.yml",               // 配置文件（required）
        "./data/feature_line_ref_0.stp",   // 特征线参考文件0（required）
        "./data/feature_line_ref_1.stp",   // 特征线参考文件1（required）
        "./data/left_boundary.txt",        // 左边界条件（required）
        "./data/mould_section.stp",        // 模具截面（required）
        "./data/strip_section.stp",        // 料带截面（required）
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
        ERROR_HINTS.put(301, "任务创建失败，可能原因：仿真类型不支持、任务名称不合法");
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

        // --------------------------------------------------
        // 前置测试：使用不存在的仿真类型，验证错误码返回
        // --------------------------------------------------
        new BasicUsage().testInvalidSimulateType();

        // --------------------------------------------------
        // 前置测试：上传超过 8 个文件（含 2 个不在白名单中的文件）
        // --------------------------------------------------
        new BasicUsage().testUploadExtraFiles();

        // --------------------------------------------------
        // 前置测试：删除指定任务（LaWan00000001），验证服务端删除行为
        // --------------------------------------------------
        new BasicUsage().testDeleteTask("LaWan00000001");

        // --------------------------------------------------
        // 前置测试：分别对三个 TaskID 执行文件校验，观察返回结果
        // --------------------------------------------------
        new BasicUsage().testVerifyTasks(new String[]{
            "LaWan00000001",   // 已删除的任务
            "LaWan00000001",   // 同一个已删除的任务，重复校验
            "LaWan00000010",   // 存在但未必完整的任务
        });

        // --------------------------------------------------
        // 前置测试：对三个 TaskID 执行 stop，覆盖不存在/未启动/运行中场景
        // --------------------------------------------------
        new BasicUsage().testStopTasks(new String[]{
            "LaWan00000001",   // 已删除的任务（不存在）
            "LaWan00000002",   // 存在但状态为 created（未启动）
            "LaWan00000010",   // 存在但状态为 created（未启动）
        });

        // --------------------------------------------------
        // 前置测试：对三个 TaskID 执行 start，覆盖不存在/文件缺失/文件缺失场景
        // --------------------------------------------------
        new BasicUsage().testStartTasks(new String[]{
            "LaWan00000001",   // 已删除的任务（不存在）
            "LaWan00000002",   // 存在但文件未上传（created 状态）
            "LaWan00000010",   // 存在但文件不完整（created 状态，缺 5 个文件）
        });

        // --------------------------------------------------
        // 前置测试：校验 running任务/文件完整任务/无文件任务，观察服务端处理
        // --------------------------------------------------
        new BasicUsage().testVerifyTasks(new String[]{
            "LaWan00000016",   // running 状态，8 个文件齐全
            "LaWan00000015",   // created 状态，8 个文件齐全
            "LaWan00000026",   // created 状态，无文件（空目录）
        });

        // --------------------------------------------------
        // 前置测试：stop running/created文件完整/created无文件，观察服务端处理
        // --------------------------------------------------
        new BasicUsage().testStopTasks(new String[]{
            "LaWan00000016",   // running 状态，8 个文件齐全 → 预期停止成功
            "LaWan00000015",   // created 状态，8 个文件齐全 → 预期操作无效
            "LaWan00000026",   // created 状态，无文件      → 预期操作无效
        });

        // 执行主流程
        new BasicUsage().run();
    }

    /**
     * 测试用例：使用不支持的仿真类型创建任务，验证 SDK 是否正确返回错误码 301。
     * <p>
     * 预期行为：SDK 在本地参数校验阶段拦截非法类型，
     * 返回 {@code code=301, message=任务创建失败: 不支持的仿真类型}，不发起网络请求。
     */
    public void testInvalidSimulateType() {
        System.out.println("\n[测试] 使用不存在的仿真类型创建任务...");
        String invalidType = "INVALID_TYPE_XYZ";
        System.out.println("  传入仿真类型: " + invalidType);

        try {
            AidClient client = new AidClient(CONFIG_PATH);
            AidResponse<TaskCreateResponse> result = client.newTaskCreate(invalidType, "test_invalid_type");

            if (!result.isSuccess()) {
                System.out.println("✓ 测试通过：服务端正确拒绝无效仿真类型");
                System.out.println("  错误码: " + result.getCode());
                System.out.println("  错误信息: " + result.getMessage());
                String hint = ERROR_HINTS.getOrDefault(result.getCode(), "请参阅 API 文档");
                System.out.println("  处理建议: " + hint);
            } else {
                System.err.println("✗ 测试未通过：期望失败但返回了成功，TaskID=" +
                        (result.getData() != null ? result.getData().getTaskID() : "null"));
            }
        } catch (AidException e) {
            System.out.println("✓ 测试通过：SDK 本地校验拦截无效仿真类型（抛出 AidException）");
            System.out.println("  错误码: " + e.getErrorCode());
            System.out.println("  错误信息: " + e.getErrorMessage());
            String hint = ERROR_HINTS.getOrDefault(e.getErrorCode(), "请参阅 API 文档");
            System.out.println("  处理建议: " + hint);
        } catch (Exception e) {
            System.err.println("✗ 测试异常: " + e.getMessage());
        }

        System.out.println("[测试] 无效仿真类型测试完成\n");
    }

    /**
     * 测试用例：上传 10 个文件（8 个合法 + 2 个不在白名单中的文件），验证服务端对超额/非法文件的处理行为。
     * <p>
     * 预期行为：
     * <ul>
     *   <li>合法文件（在白名单内）应上传成功，出现在 uploadFiles 列表中</li>
     *   <li>非法文件（文件名不在白名单内）应上传失败，出现在 failFiles 列表中</li>
     *   <li>整体响应码视成功/失败比例而定</li>
     * </ul>
     */
    public void testUploadExtraFiles() {
        System.out.println("\n[测试] 上传 10 个文件（含 2 个不在白名单中的文件）...");

        // 10 个文件：8 个标准文件 + 2 个额外文件（文件名不在服务端白名单中）
        String[] testFilePaths = {
            "./data/product.stp",              // 白名单：optional
            "./data/materials.csv",            // 白名单：required
            "./data/config.yml",               // 白名单：required
            "./data/feature_line_ref_0.stp",   // 白名单：required
            "./data/feature_line_ref_1.stp",   // 白名单：required
            "./data/left_boundary.txt",        // 白名单：required
            "./data/mould_section.stp",        // 白名单：required
            "./data/strip_section.stp",        // 白名单：required
            "./data/extra_data_01.csv",        // ★ 不在白名单：预期被拒绝
            "./data/extra_data_02.txt",        // ★ 不在白名单：预期被拒绝
        };

        try {
            // 先创建一个临时任务用于测试
            AidClient client = new AidClient(CONFIG_PATH);
            AidResponse<TaskCreateResponse> createResult = client.newTaskCreate(SIMULATE_TYPE, "test_extra_files_task");
            if (!createResult.isSuccess() || createResult.getData() == null) {
                System.err.println("✗ 测试任务创建失败: " + createResult.getMessage());
                return;
            }
            String testTaskId = createResult.getData().getTaskID();
            System.out.println("  测试任务创建成功，TaskID: " + testTaskId);
            System.out.println("  共上传 " + testFilePaths.length + " 个文件（含 2 个白名单外文件）:");
            for (String p : testFilePaths) {
                System.out.println("    - " + p);
            }

            // 构建文件列表
            List<File> files = new ArrayList<>();
            for (String path : testFilePaths) {
                files.add(new File(path));
            }

            // 上传
            AidResponse<UploadFilesResponse> uploadResult = client.uploadParamfiles(testTaskId, files);
            UploadFilesResponse resp = uploadResult.getData();

            System.out.println("\n  ── 服务端响应 ──");
            System.out.println("  响应码: " + uploadResult.getCode());
            System.out.println("  响应消息: " + uploadResult.getMessage());

            if (resp != null) {
                // 上传成功的文件
                List<Map<String, Object>> uploadFiles = resp.getUploadFiles();
                System.out.println("  ✓ 上传成功文件数: " + (uploadFiles != null ? uploadFiles.size() : 0));
                if (uploadFiles != null) {
                    for (Map<String, Object> f : uploadFiles) {
                        System.out.println("    → " + f.get("filename") + "  (" + f.get("size") + " bytes)");
                    }
                }

                // 上传失败的文件
                List<Map<String, Object>> failFiles = resp.getFailFiles();
                System.out.println("  ✗ 上传失败文件数: " + (failFiles != null ? failFiles.size() : 0));
                if (failFiles != null) {
                    for (Map<String, Object> f : failFiles) {
                        System.out.println("    → " + f.get("filename") + "  原因: " + f.get("reason"));
                    }
                }
            }

            System.out.println("\n  ── 测试结论 ──");
            if (resp != null && resp.getFailFiles() != null && resp.getFailFiles().size() == 2) {
                System.out.println("  ✓ 测试通过：服务端正确拒绝了 2 个白名单外文件，合法文件正常接收");
            } else if (resp != null && resp.getFailFiles() != null && resp.getFailFiles().size() > 0) {
                System.out.println("  △ 部分通过：服务端拒绝了 " + resp.getFailFiles().size() + " 个文件");
            } else {
                System.out.println("  ? 请检查服务端行为（失败文件数: 0，需人工确认）");
            }

        } catch (AidException e) {
            System.err.println("  ✗ SDK 异常 [code=" + e.getErrorCode() + "]: " + e.getErrorMessage());
        } catch (Exception e) {
            System.err.println("  ✗ 测试异常: " + e.getMessage());
        }

        System.out.println("[测试] 超额文件上传测试完成\n");
    }

    /**
     * 测试用例：删除指定任务，覆盖以下场景：
     * <ol>
     *   <li>删除一个真实存在的任务（LaWan00000001）→ 预期成功</li>
     *   <li>再次删除同一个任务（已不存在）→ 预期返回「任务不存在」错误</li>
     *   <li>删除空字符串 TaskID → 预期 SDK 本地校验拦截</li>
     * </ol>
     *
     * @param taskId 要删除的任务 ID
     */
    public void testDeleteTask(String taskId) {
        System.out.println("\n[测试] 删除任务测试，目标 TaskID: " + taskId);
        System.out.println("================================================================");

        try {
            AidClient client = new AidClient(CONFIG_PATH);

            // ── 场景 1：删除存在的任务 ──────────────────────────────────
            System.out.println("\n  【场景 1】删除真实任务: " + taskId);
            AidResponse<TaskStatusResponse> result1 = client.deleteTask(taskId);
            System.out.println("  响应码: " + result1.getCode());
            System.out.println("  响应消息: " + result1.getMessage());
            if (result1.isSuccess()) {
                System.out.println("  ✓ 删除成功：任务 " + taskId + " 已从服务端移除");
            } else {
                System.out.println("  ✗ 删除失败：" + result1.getMessage());
                String hint = ERROR_HINTS.getOrDefault(result1.getCode(), "请参阅 API 文档");
                System.out.println("  提示：" + hint);
            }

            // ── 场景 2：重复删除同一任务（任务已不存在）───────────────────
            System.out.println("\n  【场景 2】再次删除同一任务（预期：任务不存在错误）");
            AidResponse<TaskStatusResponse> result2 = client.deleteTask(taskId);
            System.out.println("  响应码: " + result2.getCode());
            System.out.println("  响应消息: " + result2.getMessage());
            if (!result2.isSuccess()) {
                System.out.println("  ✓ 预期行为：服务端正确返回任务不存在");
            } else {
                System.out.println("  ? 意外：重复删除返回成功，需确认服务端幂等策略");
            }

            // ── 场景 3：删除空 TaskID ────────────────────────────────────
            System.out.println("\n  【场景 3】删除空 TaskID（预期：SDK 本地校验拦截）");
            AidResponse<TaskStatusResponse> result3 = client.deleteTask("");
            System.out.println("  响应码: " + result3.getCode());
            System.out.println("  响应消息: " + result3.getMessage());
            if (!result3.isSuccess()) {
                System.out.println("  ✓ 预期行为：SDK 拒绝空 TaskID");
            } else {
                System.out.println("  ✗ 意外：空 TaskID 被接受");
            }

        } catch (AidException e) {
            System.err.println("  ✗ SDK 异常 [code=" + e.getErrorCode() + "]: " + e.getErrorMessage());
        } catch (Exception e) {
            System.err.println("  ✗ 测试异常: " + e.getMessage());
        }

        System.out.println("\n[测试] 删除任务测试完成\n");
    }

    /**
     * 测试用例：对多个 TaskID 依次执行文件完整性校验（newTaskverify），观察返回结果。
     * <p>
     * 覆盖场景：
     * <ul>
     *   <li>已删除的任务 → 预期返回任务不存在或校验失败</li>
     *   <li>同一删除任务重复校验 → 验证结果一致性</li>
     *   <li>真实存在但文件可能不完整的任务 → 观察 missingFiles 列表</li>
     * </ul>
     *
     * @param taskIds 要依次校验的 TaskID 数组
     */
    public void testVerifyTasks(String[] taskIds) {
        System.out.println("\n[测试] 多任务文件校验测试，共 " + taskIds.length + " 个任务");
        System.out.println("================================================================");

        try {
            AidClient client = new AidClient(CONFIG_PATH);

            for (int i = 0; i < taskIds.length; i++) {
                String tid = taskIds[i];
                System.out.println("\n  【任务 " + (i + 1) + "/" + taskIds.length + "】校验 TaskID: " + tid);
                System.out.println("  ──────────────────────────────────────────────────────");

                try {
                    AidResponse<VerifyResponse> result = client.newTaskverify(tid);
                    System.out.println("  响应码: " + result.getCode());
                    System.out.println("  响应消息: " + result.getMessage());

                    if (result.isSuccess() && result.getData() != null) {
                        VerifyResponse vr = result.getData();
                        boolean ready = vr.isReady();
                        List<String> missing = vr.getLeftFileList();

                        System.out.println("  ready: " + ready);
                        if (ready) {
                            System.out.println("  ✓ 文件完整，任务可启动");
                        } else {
                            System.out.println("  ✗ 文件不完整，缺少以下文件（共 "
                                    + (missing != null ? missing.size() : 0) + " 个）:");
                            if (missing != null) {
                                for (String f : missing) {
                                    System.out.println("      - " + f);
                                }
                            }
                        }
                    } else {
                        // 校验接口本身返回失败（任务不存在、状态不对等）
                        System.out.println("  ✗ 校验请求失败：" + result.getMessage());
                        String hint = ERROR_HINTS.getOrDefault(result.getCode(), "请参阅 API 文档");
                        System.out.println("  提示：" + hint);
                    }

                } catch (AidException e) {
                    System.err.println("  ✗ SDK 异常 [code=" + e.getErrorCode() + "]: " + e.getErrorMessage());
                } catch (Exception e) {
                    System.err.println("  ✗ 校验异常: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("  ✗ 初始化异常: " + e.getMessage());
        }

        System.out.println("\n[测试] 多任务文件校验测试完成\n");
    }

    /**
     * 测试用例：对多个 TaskID 依次执行停止操作（stopTask），覆盖以下场景：
     * <ol>
     *   <li>任务不存在（已删除）→ 预期 404 任务不存在</li>
     *   <li>任务存在但未启动（status=created）→ 预期返回"操作无效"</li>
     *   <li>任务存在但未启动（status=created）→ 预期返回"操作无效"</li>
     * </ol>
     *
     * @param taskIds 要依次 stop 的 TaskID 数组
     */
    public void testStopTasks(String[] taskIds) {
        System.out.println("\n[测试] 多任务 Stop 测试，共 " + taskIds.length + " 个任务");
        System.out.println("================================================================");

        // 场景说明表
        String[] scenarios = {
            "任务不存在（已删除）",
            "任务存在但未启动（status=created）",
            "任务存在但未启动（status=created）",
        };

        try {
            AidClient client = new AidClient(CONFIG_PATH);

            for (int i = 0; i < taskIds.length; i++) {
                String tid = taskIds[i];
                String scene = i < scenarios.length ? scenarios[i] : "";
                System.out.println("\n  【任务 " + (i + 1) + "/" + taskIds.length + "】"
                        + "stop TaskID: " + tid + "  ← " + scene);
                System.out.println("  ──────────────────────────────────────────────────────");

                try {
                    AidResponse<TaskStatusResponse> result = client.stopTask(tid);
                    System.out.println("  响应码: " + result.getCode());
                    System.out.println("  响应消息: " + result.getMessage());

                    if (result.getData() != null) {
                        String status = result.getData().getStatus();
                        System.out.println("  当前状态: " + (status != null ? status : "（无）"));
                    }

                    if (result.isSuccess()) {
                        if ("操作无效".equals(result.getMessage())) {
                            System.out.println("  △ 服务端接受请求但标记为操作无效（任务未在运行中）");
                        } else {
                            System.out.println("  ✓ 停止成功");
                        }
                    } else {
                        System.out.println("  ✗ 停止失败：" + result.getMessage());
                        String hint = ERROR_HINTS.getOrDefault(result.getCode(), "请参阅 API 文档");
                        System.out.println("  提示：" + hint);
                    }

                } catch (AidException e) {
                    System.err.println("  ✗ SDK 异常 [code=" + e.getErrorCode() + "]: " + e.getErrorMessage());
                } catch (Exception e) {
                    System.err.println("  ✗ 测试异常: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("  ✗ 初始化异常: " + e.getMessage());
        }

        System.out.println("\n[测试] 多任务 Stop 测试完成\n");
    }

    /**
     * 测试用例：对多个 TaskID 依次执行启动操作（startTask），覆盖以下场景：
     * <ol>
     *   <li>任务不存在（已删除）→ 预期 404 任务不存在</li>
     *   <li>任务存在但文件未上传（status=created，0个文件）→ 预期缺少必需文件错误</li>
     *   <li>任务存在但文件不完整（status=created，缺5个文件）→ 预期缺少必需文件错误</li>
     * </ol>
     *
     * @param taskIds 要依次 start 的 TaskID 数组
     */
    public void testStartTasks(String[] taskIds) {
        System.out.println("\n[测试] 多任务 Start 测试，共 " + taskIds.length + " 个任务");
        System.out.println("================================================================");

        // 场景说明表
        String[] scenarios = {
            "任务不存在（已删除）",
            "任务存在但文件未上传（created，0 个文件）",
            "任务存在但文件不完整（created，缺 5 个必需文件）",
        };

        try {
            AidClient client = new AidClient(CONFIG_PATH);

            for (int i = 0; i < taskIds.length; i++) {
                String tid = taskIds[i];
                String scene = i < scenarios.length ? scenarios[i] : "";
                System.out.println("\n  【任务 " + (i + 1) + "/" + taskIds.length + "】"
                        + "start TaskID: " + tid + "  ← " + scene);
                System.out.println("  ──────────────────────────────────────────────────────");

                try {
                    AidResponse<TaskStatusResponse> result = client.startTask(tid);
                    System.out.println("  响应码: " + result.getCode());
                    System.out.println("  响应消息: " + result.getMessage());

                    if (result.getData() != null) {
                        String status = result.getData().getStatus();
                        System.out.println("  当前状态: " + (status != null ? status : "（无）"));
                    }

                    if (result.isSuccess()) {
                        System.out.println("  ✓ 启动成功，任务开始运行");
                    } else {
                        System.out.println("  ✗ 启动失败：" + result.getMessage());
                        String hint = ERROR_HINTS.getOrDefault(result.getCode(), "请参阅 API 文档");
                        System.out.println("  提示：" + hint);
                    }

                } catch (AidException e) {
                    System.err.println("  ✗ SDK 异常 [code=" + e.getErrorCode() + "]: " + e.getErrorMessage());
                } catch (Exception e) {
                    System.err.println("  ✗ 测试异常: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("  ✗ 初始化异常: " + e.getMessage());
        }

        System.out.println("\n[测试] 多任务 Start 测试完成\n");
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
