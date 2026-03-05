#!/bin/bash
# AID Client SDK Java CLI 启动脚本
# 使用方法: ./cli_start.sh <命令> [参数]

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 设置 JAR 路径（开发环境、安装包环境、或嵌套解压环境）
SDK_DIR="${SCRIPT_DIR}/.."
if [ -f "${SDK_DIR}/target/aid-client-sdk-1.0.0.jar" ]; then
    JAR_PATH="${SDK_DIR}/target/aid-client-sdk-1.0.0.jar"
elif [ -f "${SDK_DIR}/aid-client-sdk-1.0.0.jar" ]; then
    JAR_PATH="${SDK_DIR}/aid-client-sdk-1.0.0.jar"
elif [ -f "${SDK_DIR}/../aid-client-sdk-1.0.0.jar" ]; then
    # 处理嵌套解压的情况: aid-client-sdk-1.0.0/aid-client-sdk-1.0.0/examples/
    JAR_PATH="${SDK_DIR}/../aid-client-sdk-1.0.0.jar"
else
    echo "[错误] 找不到 SDK JAR 文件"
    echo "请确保以下位置之一存在 JAR 文件:"
    echo "  - ${SDK_DIR}/target/aid-client-sdk-1.0.0.jar      (开发环境)"
    echo "  - ${SDK_DIR}/aid-client-sdk-1.0.0.jar              (安装包环境)"
    echo "  - ${SDK_DIR}/../aid-client-sdk-1.0.0.jar          (嵌套解压环境)"
    exit 1
fi

# 显示帮助信息
show_help() {
    echo "============================================================"
    echo "AID Client SDK Java CLI 启动脚本"
    echo "============================================================"
    echo ""
    echo "用法: ./cli_start.sh <命令> [参数]"
    echo ""
    echo "可用命令:"
    echo "  help              显示帮助信息"
    echo "  newTaskCreate     创建新任务"
    echo "  uploadParamfiles  上传参数文件"
    echo "  newTaskverify     校验任务文件"
    echo "  startTask         启动任务"
    echo "  queryTaskStatus   查询任务状态"
    echo "  stopTask          停止任务"
    echo "  deleteTask        删除任务"
    echo "  fetchTaskResult   获取任务结果"
    echo ""
    echo "============================================================"
    echo "命令详细示例"
    echo "============================================================"
    echo ""
    echo "[1] help - 显示帮助信息"
    echo "  ./cli_start.sh help"
    echo ""
    echo "[2] newTaskCreate - 创建新任务"
    echo "  ./cli_start.sh newTaskCreate --simulateType LaWan --taskName myTask001"
    echo "  参数说明:"
    echo "    --simulateType  仿真类型: LaWan / CHOnYA / ZhuZao / ZhaZhi / ZHEWan / JIYA"
    echo "    --taskName      任务名称: 自定义字符串"
    echo ""
    echo "[3] uploadParamfiles - 上传参数文件"
    echo "  ./cli_start.sh uploadParamfiles --TaskID LaWan00000001 --files ./data/model.stp,./data/params.csv"
    echo "  参数说明:"
    echo "    --TaskID  任务ID: 如 LaWan00000001"
    echo "    --files   文件路径: 多个文件用逗号分隔"
    echo ""
    echo "[4] newTaskverify - 校验任务文件"
    echo "  ./cli_start.sh newTaskverify --TaskID LaWan00000001"
    echo "  参数说明:"
    echo "    --TaskID  任务ID: 如 LaWan00000001"
    echo ""
    echo "[5] startTask - 启动任务"
    echo "  ./cli_start.sh startTask --TaskID LaWan00000001"
    echo "  参数说明:"
    echo "    --TaskID  任务ID: 如 LaWan00000001"
    echo ""
    echo "[6] queryTaskStatus - 查询任务状态"
    echo "  ./cli_start.sh queryTaskStatus --TaskID LaWan00000001"
    echo "  参数说明:"
    echo "    --TaskID  任务ID: 如 LaWan00000001"
    echo ""
    echo "[7] stopTask - 停止任务"
    echo "  ./cli_start.sh stopTask --TaskID LaWan00000001"
    echo "  参数说明:"
    echo "    --TaskID  任务ID: 如 LaWan00000001"
    echo ""
    echo "[8] deleteTask - 删除任务"
    echo "  ./cli_start.sh deleteTask --TaskID LaWan00000001"
    echo "  参数说明:"
    echo "    --TaskID  任务ID: 如 LaWan00000001"
    echo ""
    echo "[9] fetchTaskResult - 获取任务结果"
    echo "  ./cli_start.sh fetchTaskResult --TaskID LaWan00000001 --output ./result.zip"
    echo "  参数说明:"
    echo "    --TaskID  任务ID: 如 LaWan00000001"
    echo "    --output  输出文件路径: 如 ./result.zip"
    echo ""
    echo "============================================================"
}

# 显示命令帮助
show_cmd_help() {
    echo "============================================================"
    echo "AID Client SDK Java CLI 命令帮助"
    echo "============================================================"
    echo ""
    echo "命令格式: ./cli_start.sh <命令> [参数]"
    echo ""
    echo "[newTaskCreate] - 创建新任务"
    echo "  参数: --simulateType <类型> --taskName <名称>"
    echo "  示例: ./cli_start.sh newTaskCreate --simulateType LaWan --taskName test001"
    echo ""
    echo "[uploadParamfiles] - 上传参数文件"
    echo "  参数: --TaskID <任务ID> --files <文件路径1,文件路径2,...>"
    echo "  示例: ./cli_start.sh uploadParamfiles --TaskID LaWan00000001 --files ./data/a.stp,./data/b.csv"
    echo ""
    echo "[newTaskverify] - 校验任务文件"
    echo "  参数: --TaskID <任务ID>"
    echo "  示例: ./cli_start.sh newTaskverify --TaskID LaWan00000001"
    echo ""
    echo "[startTask] - 启动任务"
    echo "  参数: --TaskID <任务ID>"
    echo "  示例: ./cli_start.sh startTask --TaskID LaWan00000001"
    echo ""
    echo "[queryTaskStatus] - 查询任务状态"
    echo "  参数: --TaskID <任务ID>"
    echo "  示例: ./cli_start.sh queryTaskStatus --TaskID LaWan00000001"
    echo ""
    echo "[stopTask] - 停止任务"
    echo "  参数: --TaskID <任务ID>"
    echo "  示例: ./cli_start.sh stopTask --TaskID LaWan00000001"
    echo ""
    echo "[deleteTask] - 删除任务"
    echo "  参数: --TaskID <任务ID>"
    echo "  示例: ./cli_start.sh deleteTask --TaskID LaWan00000001"
    echo ""
    echo "[fetchTaskResult] - 获取任务结果"
    echo "  参数: --TaskID <任务ID> --output <输出路径>"
    echo "  示例: ./cli_start.sh fetchTaskResult --TaskID LaWan00000001 --output ./result.zip"
    echo ""
    echo "============================================================"
}

# 显示错误示例
show_error_example() {
    local cmd="$1"
    echo ""
    echo "[错误] 命令执行失败，请检查参数是否正确"
    echo ""
    echo "正确示例:"
    case "$cmd" in
        newTaskCreate)
            echo "  ./cli_start.sh newTaskCreate --simulateType LaWan --taskName myTask001"
            echo ""
            echo "参数说明:"
            echo "  --simulateType  仿真类型: LaWan / CHOnYA / ZhuZao / ZhaZhi / ZHEWan / JIYA"
            echo "  --taskName      任务名称: 自定义字符串"
            ;;
        uploadParamfiles)
            echo "  ./cli_start.sh uploadParamfiles --TaskID LaWan00000001 --files ./data/model.stp,./data/params.csv"
            echo ""
            echo "参数说明:"
            echo "  --TaskID  任务ID: 如 LaWan00000001"
            echo "  --files   文件路径: 多个文件用逗号分隔"
            ;;
        newTaskverify)
            echo "  ./cli_start.sh newTaskverify --TaskID LaWan00000001"
            echo ""
            echo "参数说明:"
            echo "  --TaskID  任务ID: 如 LaWan00000001"
            ;;
        startTask)
            echo "  ./cli_start.sh startTask --TaskID LaWan00000001"
            echo ""
            echo "参数说明:"
            echo "  --TaskID  任务ID: 如 LaWan00000001"
            ;;
        queryTaskStatus)
            echo "  ./cli_start.sh queryTaskStatus --TaskID LaWan00000001"
            echo ""
            echo "参数说明:"
            echo "  --TaskID  任务ID: 如 LaWan00000001"
            ;;
        stopTask)
            echo "  ./cli_start.sh stopTask --TaskID LaWan00000001"
            echo ""
            echo "参数说明:"
            echo "  --TaskID  任务ID: 如 LaWan00000001"
            ;;
        deleteTask)
            echo "  ./cli_start.sh deleteTask --TaskID LaWan00000001"
            echo ""
            echo "参数说明:"
            echo "  --TaskID  任务ID: 如 LaWan00000001"
            ;;
        fetchTaskResult)
            echo "  ./cli_start.sh fetchTaskResult --TaskID LaWan00000001 --output ./result.zip"
            echo ""
            echo "参数说明:"
            echo "  --TaskID  任务ID: 如 LaWan00000001"
            echo "  --output  输出文件路径: 如 ./result.zip"
            ;;
    esac
}

# 检查参数
if [ $# -eq 0 ]; then
    show_help
    exit 0
fi

# 获取命令并移除第一个参数
CMD="$1"
shift

# 构建剩余参数数组
ARGS=()
while [ $# -gt 0 ]; do
    ARGS+=("$1")
    shift
done

EXIT_CODE=0

case "$CMD" in
    help)
        show_cmd_help
        ;;
    newTaskCreate)
        java -cp "$JAR_PATH" com.aid.sdk.cli.CliMain newTaskCreate "${ARGS[@]}"
        EXIT_CODE=$?
        if [ $EXIT_CODE -ne 0 ]; then
            show_error_example "newTaskCreate"
        fi
        ;;
    uploadParamfiles)
        java -cp "$JAR_PATH" com.aid.sdk.cli.CliMain uploadParamfiles "${ARGS[@]}"
        EXIT_CODE=$?
        if [ $EXIT_CODE -ne 0 ]; then
            show_error_example "uploadParamfiles"
        fi
        ;;
    newTaskverify)
        java -cp "$JAR_PATH" com.aid.sdk.cli.CliMain newTaskverify "${ARGS[@]}"
        EXIT_CODE=$?
        if [ $EXIT_CODE -ne 0 ]; then
            show_error_example "newTaskverify"
        fi
        ;;
    startTask)
        java -cp "$JAR_PATH" com.aid.sdk.cli.CliMain startTask "${ARGS[@]}"
        EXIT_CODE=$?
        if [ $EXIT_CODE -ne 0 ]; then
            show_error_example "startTask"
        fi
        ;;
    queryTaskStatus)
        java -cp "$JAR_PATH" com.aid.sdk.cli.CliMain queryTaskStatus "${ARGS[@]}"
        EXIT_CODE=$?
        if [ $EXIT_CODE -ne 0 ]; then
            show_error_example "queryTaskStatus"
        fi
        ;;
    stopTask)
        java -cp "$JAR_PATH" com.aid.sdk.cli.CliMain stopTask "${ARGS[@]}"
        EXIT_CODE=$?
        if [ $EXIT_CODE -ne 0 ]; then
            show_error_example "stopTask"
        fi
        ;;
    deleteTask)
        java -cp "$JAR_PATH" com.aid.sdk.cli.CliMain deleteTask "${ARGS[@]}"
        EXIT_CODE=$?
        if [ $EXIT_CODE -ne 0 ]; then
            show_error_example "deleteTask"
        fi
        ;;
    fetchTaskResult)
        java -cp "$JAR_PATH" com.aid.sdk.cli.CliMain fetchTaskResult "${ARGS[@]}"
        EXIT_CODE=$?
        if [ $EXIT_CODE -ne 0 ]; then
            show_error_example "fetchTaskResult"
        fi
        ;;
    *)
        echo "错误: 未知命令 '$CMD'"
        echo "运行 './cli_start.sh' 查看可用命令"
        EXIT_CODE=1
        ;;
esac

exit $EXIT_CODE
