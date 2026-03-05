package com.aid.sdk.cli;

import java.util.HashMap;
import java.util.Map;

/**
 * AID CLI主入口类
 * 解析命令名称并分发到对应的命令处理类执行
 * 用法: java -jar aid-client-sdk.jar <命令> [选项...]
 */
public class AidCli {

    /** 命令注册表：命令名称 → 命令处理类实例 */
    private static final Map<String, BaseCommand> COMMAND_REGISTRY = new HashMap<>();

    // 静态初始化块：注册所有可用命令
    static {
        COMMAND_REGISTRY.put("newTaskCreate",     new NewTaskCreateCommand());
        COMMAND_REGISTRY.put("uploadParamfiles",  new UploadParamfilesCommand());
        COMMAND_REGISTRY.put("newTaskverify",     new NewTaskverifyCommand());
        COMMAND_REGISTRY.put("startTask",         new StartTaskCommand());
        COMMAND_REGISTRY.put("queryTaskStatus",   new QueryTaskStatusCommand());
        COMMAND_REGISTRY.put("stopTask",          new StopTaskCommand());
        COMMAND_REGISTRY.put("deleteTask",        new DeleteTaskCommand());
        COMMAND_REGISTRY.put("fetchTaskResult",   new FetchTaskResultCommand());
        COMMAND_REGISTRY.put("help",              new HelpCommand());
    }

    /**
     * CLI主函数入口
     * 解析第一个参数作为命令名，其余参数传递给命令处理类
     *
     * @param args 命令行参数，args[0]为命令名称
     */
    public static void main(String[] args) {
        // 无参数时显示帮助
        if (args == null || args.length == 0) {
            new HelpCommand().execute(new String[0]);
            System.exit(0);
            return;
        }

        String commandName = args[0];

        // 处理 --help / -h 全局帮助标志
        if ("--help".equals(commandName) || "-h".equals(commandName)) {
            new HelpCommand().execute(new String[0]);
            System.exit(0);
            return;
        }

        // 查找命令处理类
        BaseCommand command = COMMAND_REGISTRY.get(commandName);
        if (command == null) {
            System.err.println("{\"code\": 400, \"message\": \"未知命令: " + commandName
                    + "，请使用 'aid help' 查看所有可用命令\", \"success\": false}");
            System.exit(1);
            return;
        }

        // 将剩余参数（去掉命令名）传给命令处理类
        String[] commandArgs = new String[args.length - 1];
        System.arraycopy(args, 1, commandArgs, 0, commandArgs.length);

        // 执行命令
        command.execute(commandArgs);
    }
}
