package com.aid.sdk.task.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 上传文件响应模型
 * 封装后端服务返回的文件上传结果，包含已成功上传的文件列表
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadFilesResponse {

    /** 响应状态码 */
    @JsonProperty("code")
    private int code;

    /** 任务唯一标识ID */
    @JsonProperty("taskID")
    private String taskID;

    /** 响应描述信息 */
    @JsonProperty("message")
    private String message;

    /** 已成功上传的文件列表（服务端字段名 uploadFiles） */
    @JsonProperty("uploadFiles")
    private List<Map<String, Object>> uploadFiles;

    /** 上传失败的文件列表（服务端字段名 failFiles） */
    @JsonProperty("failFiles")
    private List<Map<String, Object>> failFiles;

    /**
     * 默认构造函数
     */
    public UploadFilesResponse() {
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getTaskID() { return taskID; }
    public void setTaskID(String taskID) { this.taskID = taskID; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<Map<String, Object>> getUploadFiles() { return uploadFiles; }
    public void setUploadFiles(List<Map<String, Object>> uploadFiles) { this.uploadFiles = uploadFiles; }

    public List<Map<String, Object>> getFailFiles() { return failFiles; }
    public void setFailFiles(List<Map<String, Object>> failFiles) { this.failFiles = failFiles; }

    /**
     * 兼容方法：返回已上传文件名列表
     */
    public List<String> getFileList() {
        List<String> names = new ArrayList<>();
        if (uploadFiles != null) {
            for (Map<String, Object> f : uploadFiles) {
                Object fn = f.get("filename");
                if (fn != null) names.add(fn.toString());
            }
        }
        return names;
    }

    @Override
    public String toString() {
        return "UploadFilesResponse{code=" + code + ", taskID='" + taskID + "', message='" + message
                + "', uploadFiles=" + uploadFiles + ", failFiles=" + failFiles + '}';
    }
}
