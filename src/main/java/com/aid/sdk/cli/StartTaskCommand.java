package com.aid.sdk.cli;

import com.aid.sdk.AidClient;
import com.aid.sdk.common.AidResponse;
import com.aid.sdk.task.model.TaskStatusResponse;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * 启动任务命令
 * 对应API: startTask
 * 用法: aid startTask --TaskID xxx [--config ./config/config.properties]
 */
public class StartTaskCommand extends BaseCommand {

    @Override
    public void execute(String[] args) {
        Options options = new Options();
        options.addOption(null, "TaskID", true, "任务ID [必填]");
        options.addOption(null, "config", true, "配置文件路径（默认: " + DEFAULT_CONFIG_PATH + "）");
        options.addOption("h", "help", false, "显示帮助信息");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                new HelpFormatter().printHelp("aid startTask", options, true);
                return;
            }

            String taskId = cmd.getOptionValue("TaskID");
            String configPath = cmd.getOptionValue("config", DEFAULT_CONFIG_PATH);

            if (taskId == null || taskId.isEmpty()) {
                printError(400, "参数 --TaskID 不能为空");
                return;
            }

            try (AidClient client = new AidClient(configPath)) {
                AidResponse<TaskStatusResponse> response = client.startTask(taskId);
                printResponse(response);
            }

        } catch (ParseException e) {
            printError(400, "命令行参数解析失败: " + e.getMessage());
        } catch (Exception e) {
            printError(500, "执行失败: " + e.getMessage());
        }
    }
}
