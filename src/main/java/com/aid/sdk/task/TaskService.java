package com.aid.sdk.task;

import com.aid.sdk.common.AidException;
import com.aid.sdk.common.AidResponse;
import com.aid.sdk.common.ErrorCode;
import com.aid.sdk.http.HttpClient;
import com.aid.sdk.logging.AidLogger;
import com.aid.sdk.task.model.TaskCreateResponse;
import com.aid.sdk.task.model.TaskResultResponse;
import com.aid.sdk.task.model.TaskStatusResponse;
import com.aid.sdk.task.model.UploadFilesResponse;
import com.aid.sdk.task.model.VerifyResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 任务服务类
 * 封装所有8个与AID后端服务交互的API，提供统一的业务方法入口
 */
public class TaskService {

    /** 支持的仿真类型集合 */
    private static final Set<String> ALLOWED_SIMULATE_TYPES = new HashSet<>(
            Arrays.asList("LaWan", "CHOnYA", "ZhuZao", "ZhaZhi", "ZHEWan", "JIYA")
    );

    /** 任务名称合法格式：1-64位，仅允许字母/数字/下划线 */
    private static final Pattern TASK_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{1,64}$");

    /** HTTP客户端 */
    private final HttpClient httpClient;

    /** Jackson JSON处理器 */
    private final ObjectMapper objectMapper;

    // ===== API路径定义 =====
    /** 创建任务接口路径 */
    private static final String PATH_NEW_TASK_CREATE = "/newTaskCreate";

    /** 上传文件接口路径 */
    private static final String PATH_UPLOAD_PARAMFILES = "/uploadParamfiles";

    /** 文件校验接口路径 */
    private static final String PATH_NEW_TASK_VERIFY = "/newTaskverify";

    /** 启动任务接口路径 */
    private static final String PATH_START_TASK = "/startTask";

    /** 查询任务状态接口路径 */
    private static final String PATH_QUERY_TASK_STATUS = "/queryTaskStatus";

    /** 停止任务接口路径 */
    private static final String PATH_STOP_TASK = "/stopTask";

    /** 删除任务接口路径 */
    private static final String PATH_DELETE_TASK = "/deleteTask";

    /** 获取任务结果接口路径 */
    private static final String PATH_FETCH_TASK_RESULT = "/fetchTaskResult";

    /**
     * 构造函数
     *
     * @param httpClient HTTP客户端实例
     */
    public TaskService(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 创建新仿真任务（API 1: newTaskCreate）
     *
     * @param simulateType 仿真类型，允许值：LaWan/CHOnYA/ZhuZao/ZhaZhi/ZHEWan/JIYA
     * @param taskName     任务名称，1-64位字母/数字/下划线
     * @return 包含TaskCreateResponse的AidResponse
     */
    public AidResponse<TaskCreateResponse> newTaskCreate(String simulateType, String taskName) {
        AidLogger.info("newTaskCreate", null,
                "开始创建任务，simulateType=" + simulateType + "，taskName=" + taskName);

        // 参数校验
        try {
            validateSimulateType(simulateType);
            validateTaskName(taskName);
        } catch (AidException e) {
            AidLogger.warn("newTaskCreate", null, String.valueOf(e.getErrorCode()), e.getErrorMessage());
            return AidResponse.failure(e.getErrorCode(), e.getErrorMessage());
        }

        // 构建请求参数
        Map<String, String> params = new LinkedHashMap<>();
        params.put("simulateType", simulateType);
        params.put("taskName", taskName);

        // 发送请求
        AidResponse<String> rawResponse = httpClient.post(PATH_NEW_TASK_CREATE, params);
        if (!rawResponse.isSuccess()) {
            AidLogger.error("newTaskCreate", null,
                    String.valueOf(rawResponse.getCode()), "创建任务失败: " + rawResponse.getMessage());
            return AidResponse.failure(
                    rawResponse.getCode() != 0 ? rawResponse.getCode() : ErrorCode.TASK_CREATE_FAILED.getCode(),
                    rawResponse.getMessage());
        }

        // 解析响应
        try {
            TaskCreateResponse response = objectMapper.readValue(rawResponse.getData(), TaskCreateResponse.class);
            AidLogger.info("newTaskCreate", response.getTaskID(),
                    String.valueOf(response.getCode()),
                    "创建任务成功，taskID=" + response.getTaskID());
            return AidResponse.success(response, response.getMessage());
        } catch (IOException e) {
            AidLogger.error("newTaskCreate", null,
                    String.valueOf(ErrorCode.TASK_CREATE_FAILED.getCode()),
                    "创建任务响应解析失败: " + e.getMessage());
            return AidResponse.failure(ErrorCode.TASK_CREATE_FAILED.getCode(), "响应解析失败: " + e.getMessage());
        }
    }

    /**
     * 上传任务参数文件（API 2: uploadParamfiles）
     *
     * @param taskId 任务ID
     * @param files  待上传的文件列表
     * @return 包含UploadFilesResponse的AidResponse
     */
    public AidResponse<UploadFilesResponse> uploadParamfiles(String taskId, List<File> files) {
        AidLogger.info("uploadParamfiles", taskId,
                "开始上传文件，taskId=" + taskId + "，文件数量=" + (files != null ? files.size() : 0));

        // 参数校验
        if (taskId == null || taskId.trim().isEmpty()) {
            return AidResponse.failure(ErrorCode.FILE_UPLOAD_FAILED.getCode(), "任务ID不能为空");
        }
        if (files == null || files.isEmpty()) {
            return AidResponse.failure(ErrorCode.FILE_UPLOAD_FAILED.getCode(), "上传文件列表不能为空");
        }

        // 构建表单参数（TaskID 字段名需与服务端大小写一致）
        Map<String, String> params = new LinkedHashMap<>();
        params.put("TaskID", taskId);

        // 发送Multipart请求（直接传 List<File>，由 MultipartHelper 统一处理字段名为 "files"）
        AidResponse<String> rawResponse = httpClient.postMultipart(PATH_UPLOAD_PARAMFILES, params, files);
        if (!rawResponse.isSuccess()) {
            AidLogger.error("uploadParamfiles", taskId,
                    String.valueOf(rawResponse.getCode()), "上传文件失败: " + rawResponse.getMessage());
            return AidResponse.failure(
                    rawResponse.getCode() != 0 ? rawResponse.getCode() : ErrorCode.FILE_UPLOAD_FAILED.getCode(),
                    rawResponse.getMessage());
        }

        // 解析响应
        try {
            UploadFilesResponse response = objectMapper.readValue(rawResponse.getData(), UploadFilesResponse.class);
            AidLogger.info("uploadParamfiles", taskId,
                    String.valueOf(response.getCode()),
                    "上传文件完成，fileList=" + response.getFileList());
            return AidResponse.success(response, response.getMessage());
        } catch (IOException e) {
            AidLogger.error("uploadParamfiles", taskId,
                    String.valueOf(ErrorCode.FILE_UPLOAD_FAILED.getCode()),
                    "上传文件响应解析失败: " + e.getMessage());
            return AidResponse.failure(ErrorCode.FILE_UPLOAD_FAILED.getCode(), "响应解析失败: " + e.getMessage());
        }
    }

    /**
     * 校验任务文件完整性（API 3: newTaskverify）
     *
     * @param taskId 任务ID
     * @return 包含VerifyResponse的AidResponse
     */
    public AidResponse<VerifyResponse> newTaskverify(String taskId) {
        AidLogger.info("newTaskverify", taskId, "开始校验任务文件，taskId=" + taskId);

        if (taskId == null || taskId.trim().isEmpty()) {
            return AidResponse.failure(ErrorCode.FILE_VERIFY_FAILED.getCode(), "任务ID不能为空");
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("TaskID", taskId);

        AidResponse<String> rawResponse = httpClient.post(PATH_NEW_TASK_VERIFY, params);
        if (!rawResponse.isSuccess()) {
            AidLogger.error("newTaskverify", taskId,
                    String.valueOf(rawResponse.getCode()), "文件校验失败: " + rawResponse.getMessage());
            return AidResponse.failure(
                    rawResponse.getCode() != 0 ? rawResponse.getCode() : ErrorCode.FILE_VERIFY_FAILED.getCode(),
                    rawResponse.getMessage());
        }

        try {
            VerifyResponse response = objectMapper.readValue(rawResponse.getData(), VerifyResponse.class);
            AidLogger.info("newTaskverify", taskId,
                    String.valueOf(response.getCode()),
                    "文件校验完成，ready=" + response.isReady() + "，leftFileList=" + response.getLeftFileList());
            return AidResponse.success(response, response.getMessage());
        } catch (IOException e) {
            AidLogger.error("newTaskverify", taskId,
                    String.valueOf(ErrorCode.FILE_VERIFY_FAILED.getCode()),
                    "文件校验响应解析失败: " + e.getMessage());
            return AidResponse.failure(ErrorCode.FILE_VERIFY_FAILED.getCode(), "响应解析失败: " + e.getMessage());
        }
    }

    /**
     * 启动仿真任务（API 4: startTask）
     *
     * @param taskId 任务ID
     * @return 包含TaskStatusResponse的AidResponse
     */
    public AidResponse<TaskStatusResponse> startTask(String taskId) {
        AidLogger.info("startTask", taskId, "开始启动仿真任务，taskId=" + taskId);

        if (taskId == null || taskId.trim().isEmpty()) {
            return AidResponse.failure(ErrorCode.TASK_START_FAILED.getCode(), "任务ID不能为空");
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("TaskID", taskId);

        AidResponse<String> rawResponse = httpClient.post(PATH_START_TASK, params);
        return parseTaskStatusResponse("startTask", taskId, rawResponse, ErrorCode.TASK_START_FAILED);
    }

    /**
     * 查询任务运行状态（API 5: queryTaskStatus）
     *
     * @param taskId 任务ID
     * @return 包含TaskStatusResponse的AidResponse
     */
    public AidResponse<TaskStatusResponse> queryTaskStatus(String taskId) {
        AidLogger.info("queryTaskStatus", taskId, "查询任务状态，taskId=" + taskId);

        if (taskId == null || taskId.trim().isEmpty()) {
            return AidResponse.failure(ErrorCode.RESULT_FETCH_FAILED.getCode(), "任务ID不能为空");
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("TaskID", taskId);

        AidResponse<String> rawResponse = httpClient.post(PATH_QUERY_TASK_STATUS, params);
        return parseTaskStatusResponse("queryTaskStatus", taskId, rawResponse, ErrorCode.RESULT_FETCH_FAILED);
    }

    /**
     * 停止仿真任务（API 6: stopTask）
     *
     * @param taskId 任务ID
     * @return 包含TaskStatusResponse的AidResponse
     */
    public AidResponse<TaskStatusResponse> stopTask(String taskId) {
        AidLogger.info("stopTask", taskId, "停止仿真任务，taskId=" + taskId);

        if (taskId == null || taskId.trim().isEmpty()) {
            return AidResponse.failure(ErrorCode.TASK_STOP_FAILED.getCode(), "任务ID不能为空");
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("TaskID", taskId);

        AidResponse<String> rawResponse = httpClient.post(PATH_STOP_TASK, params);
        return parseTaskStatusResponse("stopTask", taskId, rawResponse, ErrorCode.TASK_STOP_FAILED);
    }

    /**
     * 删除仿真任务（API 7: deleteTask）
     *
     * @param taskId 任务ID
     * @return 包含TaskStatusResponse的AidResponse
     */
    public AidResponse<TaskStatusResponse> deleteTask(String taskId) {
        AidLogger.info("deleteTask", taskId, "删除仿真任务，taskId=" + taskId);

        if (taskId == null || taskId.trim().isEmpty()) {
            return AidResponse.failure(ErrorCode.TASK_DELETE_FAILED.getCode(), "任务ID不能为空");
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("TaskID", taskId);

        AidResponse<String> rawResponse = httpClient.post(PATH_DELETE_TASK, params);
        return parseTaskStatusResponse("deleteTask", taskId, rawResponse, ErrorCode.TASK_DELETE_FAILED);
    }

    /**
     * 获取仿真任务结果并下载到本地（API 8: fetchTaskResult）
     * 符合OpenSpec规范：直接返回二进制文件流
     *
     * @param taskId     任务ID
     * @param outputPath 本地输出路径（文件夹路径）
     * @return 包含TaskResultResponse的AidResponse
     */
    public AidResponse<TaskResultResponse> fetchTaskResult(String taskId, String outputPath) {
        AidLogger.info("fetchTaskResult", taskId,
                "获取任务结果，taskId=" + taskId + "，输出路径=" + outputPath);

        if (taskId == null || taskId.trim().isEmpty()) {
            return AidResponse.failure(ErrorCode.RESULT_FETCH_FAILED.getCode(), "任务ID不能为空");
        }
        if (outputPath == null || outputPath.trim().isEmpty()) {
            return AidResponse.failure(ErrorCode.RESULT_FETCH_FAILED.getCode(), "输出路径不能为空");
        }

        try {
            // 直接下载二进制内容（符合OpenSpec规范）
            Map<String, String> params = new LinkedHashMap<>();
            params.put("TaskID", taskId);

            byte[] fileBytes = httpClient.download(PATH_FETCH_TASK_RESULT, params);

            // 确保输出目录存在
            File outputDir = new File(outputPath);
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                throw new IOException("创建输出目录失败: " + outputDir.getAbsolutePath());
            }

            // 结果文件命名规则：{taskId}result.stp
            File outputFile = new File(outputDir, taskId + "result.stp");
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(fileBytes);
            }

            TaskResultResponse response = new TaskResultResponse();
            response.setCode(ErrorCode.SUCCESS.getCode());
            response.setTaskID(taskId);
            response.setMessage("任务结果获取成功");
            response.setResultFilePath(outputFile.getAbsolutePath());

            AidLogger.info("fetchTaskResult", taskId,
                    String.valueOf(response.getCode()),
                    "任务结果下载成功，本地路径=" + response.getResultFilePath());

            return AidResponse.success(response, response.getMessage());

        } catch (AidException e) {
            AidLogger.error("fetchTaskResult", taskId,
                    String.valueOf(e.getErrorCode()),
                    "获取任务结果失败: " + e.getErrorMessage());
            return AidResponse.failure(e.getErrorCode(), e.getErrorMessage());
        } catch (IOException e) {
            AidLogger.error("fetchTaskResult", taskId,
                    String.valueOf(ErrorCode.RESULT_FETCH_FAILED.getCode()),
                    "获取任务结果IO异常: " + e.getMessage());
            return AidResponse.failure(ErrorCode.RESULT_FETCH_FAILED.getCode(),
                    "获取任务结果失败: " + e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 解析TaskStatusResponse的通用方法
     *
     * @param apiName     API名称（日志用）
     * @param taskId      任务ID
     * @param rawResponse HTTP原始响应
     * @param failCode    失败时使用的错误码
     * @return 包含TaskStatusResponse的AidResponse
     */
    private AidResponse<TaskStatusResponse> parseTaskStatusResponse(
            String apiName, String taskId, AidResponse<String> rawResponse, ErrorCode failCode) {

        if (!rawResponse.isSuccess()) {
            AidLogger.error(apiName, taskId,
                    String.valueOf(rawResponse.getCode()), apiName + "失败: " + rawResponse.getMessage());
            return AidResponse.failure(
                    rawResponse.getCode() != 0 ? rawResponse.getCode() : failCode.getCode(),
                    rawResponse.getMessage());
        }

        try {
            TaskStatusResponse response = objectMapper.readValue(rawResponse.getData(), TaskStatusResponse.class);
            AidLogger.info(apiName, taskId,
                    String.valueOf(response.getCode()),
                    apiName + "完成，status=" + response.getStatus());
            return AidResponse.success(response, response.getMessage());
        } catch (IOException e) {
            AidLogger.error(apiName, taskId,
                    String.valueOf(failCode.getCode()),
                    apiName + "响应解析失败: " + e.getMessage());
            return AidResponse.failure(failCode.getCode(), "响应解析失败: " + e.getMessage());
        }
    }

    /**
     * 校验仿真类型是否合法
     *
     * @param simulateType 仿真类型字符串
     * @throws AidException 仿真类型不合法时抛出
     */
    private void validateSimulateType(String simulateType) {
        if (simulateType == null || simulateType.trim().isEmpty()) {
            throw new AidException(ErrorCode.TASK_CREATE_FAILED, "仿真类型（simulateType）不能为空");
        }
        if (!ALLOWED_SIMULATE_TYPES.contains(simulateType)) {
            throw new AidException(ErrorCode.TASK_CREATE_FAILED,
                    "不支持的仿真类型: [" + simulateType + "]，允许的类型为: " +
                            String.join(", ", ALLOWED_SIMULATE_TYPES));
        }
    }

    /**
     * 校验任务名称是否合法
     *
     * @param taskName 任务名称字符串
     * @throws AidException 任务名称不合法时抛出
     */
    private void validateTaskName(String taskName) {
        if (taskName == null || taskName.trim().isEmpty()) {
            throw new AidException(ErrorCode.TASK_CREATE_FAILED, "任务名称（taskName）不能为空");
        }
        if (!TASK_NAME_PATTERN.matcher(taskName).matches()) {
            throw new AidException(ErrorCode.TASK_CREATE_FAILED,
                    "任务名称格式不正确: [" + taskName + "]，仅允许1-64位字母、数字或下划线");
        }
    }

    /**
     * 从文件路径中提取文件名
     *
     * @param filePath 文件路径字符串
     * @return 文件名
     */
    private String extractFileName(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "result.zip";
        }
        int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return lastSlash >= 0 ? filePath.substring(lastSlash + 1) : filePath;
    }
}
