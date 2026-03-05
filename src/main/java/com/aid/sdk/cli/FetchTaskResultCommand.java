package com.aid.sdk.cli;

import com.aid.sdk.AidClient;
import com.aid.sdk.common.AidResponse;
import com.aid.sdk.task.model.TaskResultResponse;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * 获取任务结果命令
 * 对应API: fetchTaskResult
 * 用法: aid fetchTaskResult --TaskID xxx --output ./output [--config ./config/config.properties]
 */
public class FetchTaskResultCommand extends BaseCommand {

    @Override
    public void execute(String[] args) {
        Options options = new Options();
        options.addOption(null, "TaskID", true, "任务ID [必填]");
        options.addOption(null, "output", true, "结果文件本地输出路径 [必填]");
        options.addOption(null, "config", true, "配置文件路径（默认: " + DEFAULT_CONFIG_PATH + "）");
        options.addOption("h", "help", false, "显示帮助信息");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                new HelpFormatter().printHelp("aid fetchTaskResult", options, true);
                return;
            }

            String taskId = cmd.getOptionValue("TaskID");
            String outputPath = cmd.getOptionValue("output");
            String configPath = cmd.getOptionValue("config", DEFAULT_CONFIG_PATH);

            if (taskId == null || taskId.isEmpty()) {
                printError(400, "参数 --TaskID 不能为空");
                return;
            }
            if (outputPath == null || outputPath.isEmpty()) {
                printError(400, "参数 --output 不能为空");
                return;
            }

            try (AidClient client = new AidClient(configPath)) {
                AidResponse<TaskResultResponse> response = client.fetchTaskResult(taskId, outputPath);
                printResponse(response);
            }

        } catch (ParseException e) {
            printError(400, "命令行参数解析失败: " + e.getMessage());
        } catch (Exception e) {
            printError(500, "执行失败: " + e.getMessage());
        }
    }
}
