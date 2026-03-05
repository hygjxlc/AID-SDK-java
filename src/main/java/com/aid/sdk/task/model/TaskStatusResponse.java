package com.aid.sdk.task.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * 任务状态响应模型
 * 封装后端服务返回的任务状态信息，用于查询/启动/停止/删除等操作的结果
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskStatusResponse {

    /** 响应状态码 */
    @JsonProperty("code")
    private int code;

    /** 任务唯一标识ID */
    @JsonProperty("taskID")
    private String taskID;

    /** 任务当前状态（如 running、completed、failed、stopped 等） */
    @JsonProperty("status")
    private String status;

    /** 任务额外信息（服务端字段名 extra） */
    @JsonProperty("extra")
    private Map<String, Object> extra;

    /** 响应描述信息 */
    @JsonProperty("message")
    private String message;

    /**
     * 默认构造函数
     */
    public TaskStatusResponse() {
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getTaskID() { return taskID; }
    public void setTaskID(String taskID) { this.taskID = taskID; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Object> getExtra() { return extra; }
    public void setExtra(Map<String, Object> extra) { this.extra = extra; }

    /** 兼容旧方法名 */
    public String getExtraInfo() { return extra != null ? extra.toString() : null; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    @Override
    public String toString() {
        return "TaskStatusResponse{code=" + code + ", taskID='" + taskID + "', status='" + status
                + "', extra=" + extra + ", message='" + message + "'}";
    }
}
