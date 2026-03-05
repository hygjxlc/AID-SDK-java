package com.aid.sdk.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 获取任务结果响应模型
 * 封装后端服务返回的仿真计算结果信息，含结果文件下载路径
 */
public class TaskResultResponse {

    /** 响应状态码 */
    @JsonProperty("code")
    private int code;

    /** 任务唯一标识ID */
    @JsonProperty("taskID")
    private String taskID;

    /** 响应描述信息 */
    @JsonProperty("message")
    private String message;

    /** 仿真结果文件在服务端的路径 */
    @JsonProperty("resultFilePath")
    private String resultFilePath;

    /**
     * 默认构造函数
     */
    public TaskResultResponse() {
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getTaskID() { return taskID; }
    public void setTaskID(String taskID) { this.taskID = taskID; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getResultFilePath() { return resultFilePath; }
    public void setResultFilePath(String resultFilePath) { this.resultFilePath = resultFilePath; }

    @Override
    public String toString() {
        return "TaskResultResponse{code=" + code + ", taskID='" + taskID + "', message='" + message
                + "', resultFilePath='" + resultFilePath + "'}";
    }
}
