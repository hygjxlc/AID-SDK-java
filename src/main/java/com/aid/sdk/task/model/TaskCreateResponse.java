package com.aid.sdk.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 创建任务响应模型
 * 封装后端服务返回的任务创建结果
 */
public class TaskCreateResponse {

    /** 响应状态码 */
    @JsonProperty("code")
    private int code;

    /** 任务唯一标识ID */
    @JsonProperty("taskID")
    private String taskID;

    /** 响应描述信息 */
    @JsonProperty("message")
    private String message;

    /**
     * 默认构造函数
     */
    public TaskCreateResponse() {
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getTaskID() { return taskID; }
    public void setTaskID(String taskID) { this.taskID = taskID; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    @Override
    public String toString() {
        return "TaskCreateResponse{code=" + code + ", taskID='" + taskID + "', message='" + message + "'}";
    }
}
