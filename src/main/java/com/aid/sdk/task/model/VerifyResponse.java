package com.aid.sdk.task.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 任务文件校验响应模型
 * 封装后端服务返回的文件完整性校验结果，含缺失文件清单
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerifyResponse {

    /** 响应状态码 */
    @JsonProperty("code")
    private int code;

    /** 任务唯一标识ID */
    @JsonProperty("taskID")
    private String taskID;

    /** 文件是否齐全，true表示所有必需文件均已就绪 */
    @JsonProperty("ready")
    private boolean ready;

    /** 尚未上传的缺失文件名列表（服务端字段名 missingFiles） */
    @JsonProperty("missingFiles")
    private List<String> missingFiles;

    /** 响应描述信息 */
    @JsonProperty("message")
    private String message;

    /**
     * 默认构造函数
     */
    public VerifyResponse() {
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getTaskID() { return taskID; }
    public void setTaskID(String taskID) { this.taskID = taskID; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public List<String> getMissingFiles() { return missingFiles; }
    public void setMissingFiles(List<String> missingFiles) { this.missingFiles = missingFiles; }

    /** 兼容旧方法名 */
    public List<String> getLeftFileList() { return missingFiles; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    @Override
    public String toString() {
        return "VerifyResponse{code=" + code + ", taskID='" + taskID + "', ready=" + ready
                + ", missingFiles=" + missingFiles + ", message='" + message + "'}";
    }
}
