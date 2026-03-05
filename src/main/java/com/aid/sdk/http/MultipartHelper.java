package com.aid.sdk.http;

import com.aid.sdk.common.AidException;
import com.aid.sdk.common.ErrorCode;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Multipart请求构建工具类
 * 提供Multipart/form-data请求体的构建，包含文件扩展名和大小校验
 */
public class MultipartHelper {

    /** 允许上传的文件扩展名（小写） */
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(
            Arrays.asList("stp", "txt", "csv", "yml", "jnl")
    );

    /** 单个文件最大大小：100MB */
    private static final long MAX_SINGLE_FILE_SIZE = 100L * 1024 * 1024;

    /** 所有文件总大小上限：500MB */
    private static final long MAX_TOTAL_FILE_SIZE = 500L * 1024 * 1024;

    /** 表单数据媒体类型 */
    private static final MediaType FORM_DATA_TYPE = MediaType.parse("application/octet-stream");

    /** 文本表单字段媒体类型 */
    private static final MediaType TEXT_PLAIN_TYPE = MediaType.parse("text/plain; charset=utf-8");

    /**
     * 私有构造函数，禁止实例化工具类
     */
    private MultipartHelper() {
        throw new UnsupportedOperationException("工具类不支持实例化");
    }

    /**
     * 构建Multipart请求体（支持多文件同字段名上传）
     *
     * @param params 表单文本参数
     * @param files  文件列表（服务端统一使用 "files" 字段名接收多文件）
     * @return OkHttp MultipartBody实例
     * @throws AidException 文件校验失败时抛出异常
     */
    public static RequestBody buildMultipartBody(Map<String, String> params, java.util.List<File> files) {
        // 文件校验
        if (files != null && !files.isEmpty()) {
            validateFileList(files);
        }

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        // 添加文本表单字段
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    builder.addFormDataPart(entry.getKey(), entry.getValue());
                }
            }
        }

        // 添加文件字段（统一使用 "files" 字段名，OkHttp 支持同名多字段）
        if (files != null) {
            for (File file : files) {
                if (file != null) {
                    RequestBody fileBody = RequestBody.create(file, FORM_DATA_TYPE);
                    builder.addFormDataPart("files", file.getName(), fileBody);
                }
            }
        }

        return builder.build();
    }

    /**
     * 构建Multipart请求体（Map模式，保持向后兼容）
     *
     * @param params 表单文本参数
     * @param files  文件参数，key为表单字段名，value为File对象
     * @return OkHttp MultipartBody实例
     * @throws AidException 文件校验失败时抛出异常
     */
    public static RequestBody buildMultipartBody(Map<String, String> params, Map<String, File> files) {
        // 转换为 List 调用
        java.util.List<File> fileList = files != null ? new java.util.ArrayList<>(files.values()) : null;
        return buildMultipartBody(params, fileList);
    }

    /**
     * 校验上传文件的扩展名和大小（List版本）
     *
     * @param files 待校验的文件列表
     * @throws AidException 扩展名不合法或文件超过大小限制时抛出
     */
    private static void validateFileList(java.util.List<File> files) {
        long totalSize = 0L;

        for (File file : files) {
            if (file == null) {
                continue;
            }

            // 校验文件是否存在
            if (!file.exists()) {
                throw new AidException(ErrorCode.FILE_VERIFY_FAILED,
                        "文件不存在: " + file.getAbsolutePath());
            }

            // 校验文件扩展名
            String fileName = file.getName();
            String extension = getFileExtension(fileName);
            if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                throw new AidException(ErrorCode.FILE_VERIFY_FAILED,
                        "不支持的文件类型: " + extension + "，仅允许上传: " +
                                String.join(", ", ALLOWED_EXTENSIONS) + " 格式的文件");
            }

            // 校验单个文件大小
            long fileSize = file.length();
            if (fileSize > MAX_SINGLE_FILE_SIZE) {
                throw new AidException(ErrorCode.FILE_VERIFY_FAILED,
                        "文件 [" + fileName + "] 超过单文件大小限制(100MB)，当前大小: " +
                                formatFileSize(fileSize));
            }

            totalSize += fileSize;
        }

        // 校验文件总大小
        if (totalSize > MAX_TOTAL_FILE_SIZE) {
            throw new AidException(ErrorCode.FILE_VERIFY_FAILED,
                    "上传文件总大小超过限制(500MB)，当前总大小: " + formatFileSize(totalSize));
        }
    }

    /**
     * 校验上传文件的扩展名和大小
     *
     * @param files 待校验的文件Map
     * @throws AidException 扩展名不合法或文件超过大小限制时抛出
     */
    private static void validateFiles(Map<String, File> files) {
        validateFileList(new java.util.ArrayList<>(files.values()));
    }

    /**
     * 获取文件扩展名（不含点号，小写）
     *
     * @param fileName 文件名
     * @return 扩展名字符串，无扩展名则返回空字符串
     */
    private static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    /**
     * 格式化文件大小为可读字符串
     *
     * @param size 文件大小（字节）
     * @return 格式化后的文件大小字符串
     */
    private static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }
}
