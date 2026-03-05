package com.aid.sdk.task.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 创建任务请求模型
 * 封装新建仿真任务所需的请求参数
 */
public class TaskCreateRequest {

    /** 仿真类型（如 LaWan、CHOnYA、ZhuZao 等） */
    @JsonProperty("simulateType")
    private String simulateType;

    /** 任务名称（1-64位，字母/数字/下划线） */
    @JsonProperty("taskName")
    private String taskName;

    /** API认证Key */
    @JsonProperty("apiKey")
    private String apiKey;

    /**
     * 默认构造函数
     */
    public TaskCreateRequest() {
    }

    /**
     * 全参构造函数
     *
     * @param simulateType 仿真类型
     * @param taskName     任务名称
     * @param apiKey       API认证Key
     */
    public TaskCreateRequest(String simulateType, String taskName, String apiKey) {
        this.simulateType = simulateType;
        this.taskName = taskName;
        this.apiKey = apiKey;
    }

    public String getSimulateType() { return simulateType; }
    public void setSimulateType(String simulateType) { this.simulateType = simulateType; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    @Override
    public String toString() {
        return "TaskCreateRequest{simulateType='" + simulateType + "', taskName='" + taskName + "'}";
    }
}
