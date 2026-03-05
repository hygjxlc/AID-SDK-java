package com.aid.sdk.cli;

import com.aid.sdk.AidClient;
import com.aid.sdk.common.AidResponse;
import com.aid.sdk.task.model.TaskCreateResponse;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * 创建任务命令
 * 对应API: newTaskCreate
 * 用法: aid newTaskCreate --simulateType LaWan --taskName my_task [--config ./config/config.properties]
 */
public class NewTaskCreateCommand extends BaseCommand {

    @Override
    public void execute(String[] args) {
        // 定义命令行选项
        Options options = new Options();
        options.addOption(null, "simulateType", true, "仿真类型（LaWan/CHOnYA/ZhuZao/ZhaZhi/ZHEWan/JIYA）[必填]");
        options.addOption(null, "taskName", true, "任务名称，1-64位字母/数字/下划线 [必填]");
        options.addOption(null, "config", true, "配置文件路径（默认: " + DEFAULT_CONFIG_PATH + "）");
        options.addOption("h", "help", false, "显示帮助信息");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                new HelpFormatter().printHelp("aid newTaskCreate", options, true);
                return;
            }

            String simulateType = cmd.getOptionValue("simulateType");
            String taskName = cmd.getOptionValue("taskName");
            String configPath = cmd.getOptionValue("config", DEFAULT_CONFIG_PATH);

            if (simulateType == null || simulateType.isEmpty()) {
                printError(400, "参数 --simulateType 不能为空");
                return;
            }
            if (taskName == null || taskName.isEmpty()) {
                printError(400, "参数 --taskName 不能为空");
                return;
            }

            try (AidClient client = new AidClient(configPath)) {
                AidResponse<TaskCreateResponse> response = client.newTaskCreate(simulateType, taskName);
                printResponse(response);
            }

        } catch (ParseException e) {
            printError(400, "命令行参数解析失败: " + e.getMessage());
        } catch (Exception e) {
            printError(500, "执行失败: " + e.getMessage());
        }
    }
}
