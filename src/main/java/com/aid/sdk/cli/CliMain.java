package com.aid.sdk.cli;

/**
 * CLI 入口类 - 委托给 AidCli
 * 用法: java -cp aid-client-sdk.jar com.aid.sdk.cli.CliMain <命令> [选项...]
 */
public class CliMain {

    /**
     * CLI 主函数入口
     * 直接委托给 AidCli.main 处理
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        AidCli.main(args);
    }
}
