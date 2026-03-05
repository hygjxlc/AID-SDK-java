package com.aid.sdk.cli;

/**
 * 帮助命令
 * 打印所有可用命令及其用法说明
 * 用法: aid help
 */
public class HelpCommand extends BaseCommand {

    @Override
    public void execute(String[] args) {
        System.out.println("AID Client SDK - 金属加工仿真系统客户端命令行工具");
        System.out.println("版本: 1.0.0");
        System.out.println();
        System.out.println("用法: aid <命令> [选项]");
        System.out.println();
        System.out.println("可用命令:");
        System.out.println();

        System.out.println("  newTaskCreate         创建新仿真任务");
        System.out.println("    --simulateType      仿真类型 [必填]，可选值: LaWan, CHOnYA, ZhuZao, ZhaZhi, ZHEWan, JIYA");
        System.out.println("    --taskName          任务名称 [必填]，1-64位字母/数字/下划线");
        System.out.println("    --config            配置文件路径（默认: ./config/config.properties）");
        System.out.println();

        System.out.println("  uploadParamfiles      上传任务参数文件");
        System.out.println("    --TaskID            任务ID [必填]");
        System.out.println("    --files             文件路径列表 [必填]，多个文件以英文逗号分隔");
        System.out.println("                        支持格式: stp, txt, csv, yml, jnl");
        System.out.println("                        单文件限制: ≤100MB，总大小限制: ≤500MB");
        System.out.println("    --config            配置文件路径（默认: ./config/config.properties）");
        System.out.println();

        System.out.println("  newTaskverify         校验任务文件完整性");
        System.out.println("    --TaskID            任务ID [必填]");
        System.out.println("    --config            配置文件路径（默认: ./config/config.properties）");
        System.out.println();

        System.out.println("  startTask             启动仿真任务");
        System.out.println("    --TaskID            任务ID [必填]");
        System.out.println("    --config            配置文件路径（默认: ./config/config.properties）");
        System.out.println();

        System.out.println("  queryTaskStatus       查询任务运行状态");
        System.out.println("    --TaskID            任务ID [必填]");
        System.out.println("    --config            配置文件路径（默认: ./config/config.properties）");
        System.out.println();

        System.out.println("  stopTask              停止仿真任务");
        System.out.println("    --TaskID            任务ID [必填]");
        System.out.println("    --config            配置文件路径（默认: ./config/config.properties）");
        System.out.println();

        System.out.println("  deleteTask            删除仿真任务");
        System.out.println("    --TaskID            任务ID [必填]");
        System.out.println("    --config            配置文件路径（默认: ./config/config.properties）");
        System.out.println();

        System.out.println("  fetchTaskResult       获取仿真任务结果并下载到本地");
        System.out.println("    --TaskID            任务ID [必填]");
        System.out.println("    --output            本地结果输出路径 [必填]");
        System.out.println("    --config            配置文件路径（默认: ./config/config.properties）");
        System.out.println();

        System.out.println("  help                  显示此帮助信息");
        System.out.println();

        System.out.println("示例:");
        System.out.println("  aid newTaskCreate --simulateType LaWan --taskName my_task_001");
        System.out.println("  aid uploadParamfiles --TaskID abc123 --files model.stp,params.csv");
        System.out.println("  aid newTaskverify --TaskID abc123");
        System.out.println("  aid startTask --TaskID abc123");
        System.out.println("  aid queryTaskStatus --TaskID abc123");
        System.out.println("  aid stopTask --TaskID abc123");
        System.out.println("  aid deleteTask --TaskID abc123");
        System.out.println("  aid fetchTaskResult --TaskID abc123 --output ./results");
        System.out.println();

        System.out.println("配置文件说明 (config/config.properties):");
        System.out.println("  baseURL     后端服务地址，例如: http://127.0.0.1:8080/aid-service");
        System.out.println("  api_token   API认证Token");
    }
}
