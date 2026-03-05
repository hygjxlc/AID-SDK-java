package com.aid.sdk.examples;

import com.aid.sdk.AidClient;
import com.aid.sdk.common.AidResponse;
import com.aid.sdk.task.model.TaskResultResponse;

import java.io.File;

/**
 * 测试获取仿真结果
 */
public class TestFetchResult {
    
    private static final String CONFIG_PATH = "./config/config.properties";
    private static final String OUTPUT_DIR = "./test_results/";
    
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("测试：获取仿真结果 (fetchTaskResult)");
        System.out.println("==================================================");
        
        // 测试任务列表
        String[] taskIds = {"LaWan00000016", "LaWan00000013", "LaWan00000099"};
        
        try {
            AidClient client = new AidClient(CONFIG_PATH);
            
            // 确保输出目录存在
            File dir = new File(OUTPUT_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            for (String taskId : taskIds) {
                System.out.println("\n----------------------------------------");
                System.out.println("测试任务: " + taskId);
                System.out.println("----------------------------------------");
                
                try {
                    AidResponse<TaskResultResponse> result = client.fetchTaskResult(taskId, OUTPUT_DIR);
                    
                    if (result.isSuccess() && result.getData() != null) {
                        TaskResultResponse resp = result.getData();
                        System.out.println("✓ 获取结果成功");
                        System.out.println("  响应码: " + result.getCode());
                        System.out.println("  消息: " + result.getMessage());
                        System.out.println("  结果文件路径: " + resp.getResultFilePath());
                        
                        // 验证文件是否存在
                        File resultFile = new File(resp.getResultFilePath());
                        if (resultFile.exists()) {
                            System.out.println("  文件大小: " + resultFile.length() + " bytes");
                        } else {
                            System.out.println("  ⚠ 警告：结果文件不存在于本地");
                        }
                    } else {
                        System.out.println("✗ 获取结果失败");
                        System.out.println("  响应码: " + result.getCode());
                        System.out.println("  消息: " + result.getMessage());
                        
                        if (result.getCode() == 404) {
                            System.out.println("  ✓ 符合预期：任务不存在返回404");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("✗ 异常: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.err.println("✗ 初始化失败: " + e.getMessage());
        }
        
        System.out.println("\n==================================================");
        System.out.println("测试结束");
        System.out.println("==================================================");
    }
}
