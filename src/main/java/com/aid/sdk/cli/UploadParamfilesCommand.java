package com.aid.sdk.cli;

import com.aid.sdk.AidClient;
import com.aid.sdk.common.AidResponse;
import com.aid.sdk.task.model.UploadFilesResponse;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 上传文件命令
 * 对应API: uploadParamfiles
 * 用法: aid uploadParamfiles --TaskID xxx --files file1.stp file2.txt [--config ./config/config.properties]
 *      或: aid uploadParamfiles --TaskID xxx --files file1.stp,file2.txt
 */
public class UploadParamfilesCommand extends BaseCommand {

    @Override
    public void execute(String[] args) {
        Options options = new Options();
        options.addOption(null, "TaskID", true, "任务ID [必填]");
        // hasArgs() allows multiple space-separated values after --files
        options.addOption(Option.builder().longOpt("files")
                .hasArgs()
                .desc("待上传文件路径，多个文件以空格或英文逗号分隔 [必填]")
                .build());
        options.addOption(null, "config", true, "配置文件路径（默认: " + DEFAULT_CONFIG_PATH + "）");
        options.addOption("h", "help", false, "显示帮助信息");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                new HelpFormatter().printHelp("aid uploadParamfiles", options, true);
                return;
            }

            String taskId = cmd.getOptionValue("TaskID");
            String[] filesArgs = cmd.getOptionValues("files");
            String configPath = cmd.getOptionValue("config", DEFAULT_CONFIG_PATH);

            if (taskId == null || taskId.isEmpty()) {
                printError(400, "参数 --TaskID 不能为空");
                return;
            }
            if (filesArgs == null || filesArgs.length == 0) {
                printError(400, "参数 --files 不能为空");
                return;
            }

            // Support both space-separated (multiple values) and comma-separated (single value)
            List<File> files = new ArrayList<>();
            for (String arg : filesArgs) {
                // Each arg may itself be comma-separated
                for (String path : arg.split(",")) {
                    String trimmed = path.trim();
                    if (!trimmed.isEmpty()) {
                        File f = new File(trimmed);
                        if (!f.exists()) {
                            printError(302, "文件不存在: " + trimmed);
                            return;
                        }
                        files.add(f);
                    }
                }
            }

            if (files.isEmpty()) {
                printError(400, "未解析到有效文件路径");
                return;
            }

            try (AidClient client = new AidClient(configPath)) {
                AidResponse<UploadFilesResponse> response = client.uploadParamfiles(taskId, files);
                printResponse(response);
            }

        } catch (ParseException e) {
            printError(400, "命令行参数解析失败: " + e.getMessage());
        } catch (Exception e) {
            printError(500, "执行失败: " + e.getMessage());
        }
    }
}
