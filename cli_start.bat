@echo off
chcp 65001 >nul
REM AID Client SDK Java CLI 启动脚本
REM 使用方法: cli_start.bat <命令> [参数]

setlocal EnableDelayedExpansion

REM 获取脚本所在目录（SDK 根目录）
set "SCRIPT_DIR=%~dp0"
set "SDK_DIR=%SCRIPT_DIR%"

REM 检查 JAR 文件位置（当前目录）
if exist "%SDK_DIR%\aid-client-sdk-1.0.0.jar" (
    set "JAR_PATH=%SDK_DIR%\aid-client-sdk-1.0.0.jar"
) else if exist "%SDK_DIR%\target\aid-client-sdk-1.0.0.jar" (
    REM 开发环境
    set "JAR_PATH=%SDK_DIR%\target\aid-client-sdk-1.0.0.jar"
) else (
    echo [错误] 找不到 SDK JAR 文件: aid-client-sdk-1.0.0.jar
    echo 请确保 JAR 文件与脚本在同一目录下。
    exit /b 1
)

REM 设置 Java 路径（优先使用 JRE，避免 JDK 的 server JVM 问题）
if exist "%JAVA_HOME%\jre\bin\java.exe" (
    set "JAVA_CMD=%JAVA_HOME%\jre\bin\java.exe"
) else if exist "%JAVA_HOME%\bin\java.exe" (
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_CMD=java.exe"
)

REM 检查 JAR 文件是否存在
if not exist "%JAR_PATH%" (
    echo [错误] 找不到 SDK JAR 文件: %JAR_PATH%
    echo 请先构建 SDK: mvn clean package
    exit /b 1
)

REM 显示帮助信息
if "%~1"=="" (
    echo ============================================================
    echo AID Client SDK Java CLI 启动脚本
    echo ============================================================
    echo.
    echo 用法: cli_start.bat ^<命令^> [参数]
    echo.
    echo 可用命令:
    echo   help              显示帮助信息
    echo   newTaskCreate     创建新任务
    echo   uploadParamfiles  上传参数文件
    echo   newTaskverify     校验任务文件
    echo   startTask         启动任务
    echo   queryTaskStatus   查询任务状态
    echo   stopTask          停止任务
    echo   deleteTask        删除任务
    echo   fetchTaskResult   获取任务结果
    echo.
    echo ============================================================
    echo 命令详细示例
    echo ============================================================
    echo.
    echo [1] help - 显示帮助信息
    echo   cli_start.bat help
    echo.
    echo [2] newTaskCreate - 创建新任务
    echo   cli_start.bat newTaskCreate --simulateType LaWan --taskName myTask001
    echo   参数说明:
    echo     --simulateType  仿真类型: LaWan / CHOnYA / ZhuZao / ZhaZhi / ZHEWan / JIYA
    echo     --taskName      任务名称: 自定义字符串
    echo.
    echo [3] uploadParamfiles - 上传参数文件
    echo   cli_start.bat uploadParamfiles --TaskID LaWan00000001 --files ./examples/data/model.stp,./examples/data/params.csv
    echo   参数说明:
    echo     --TaskID  任务ID: 如 LaWan00000001
    echo     --files   文件路径: 多个文件用逗号分隔
    echo.
    echo [4] newTaskverify - 校验任务文件
    echo   cli_start.bat newTaskverify --TaskID LaWan00000001
    echo   参数说明:
    echo     --TaskID  任务ID: 如 LaWan00000001
    echo.
    echo [5] startTask - 启动任务
    echo   cli_start.bat startTask --TaskID LaWan00000001
    echo   参数说明:
    echo     --TaskID  任务ID: 如 LaWan00000001
    echo.
    echo [6] queryTaskStatus - 查询任务状态
    echo   cli_start.bat queryTaskStatus --TaskID LaWan00000001
    echo   参数说明:
    echo     --TaskID  任务ID: 如 LaWan00000001
    echo.
    echo [7] stopTask - 停止任务
    echo   cli_start.bat stopTask --TaskID LaWan00000001
    echo   参数说明:
    echo     --TaskID  任务ID: 如 LaWan00000001
    echo.
    echo [8] deleteTask - 删除任务
    echo   cli_start.bat deleteTask --TaskID LaWan00000001
    echo   参数说明:
    echo     --TaskID  任务ID: 如 LaWan00000001
    echo.
    echo [9] fetchTaskResult - 获取任务结果
    echo   cli_start.bat fetchTaskResult --TaskID LaWan00000001 --output ./result.zip
    echo   参数说明:
    echo     --TaskID  任务ID: 如 LaWan00000001
    echo     --output  输出文件路径: 如 ./result.zip
    echo.
    echo ============================================================
    goto :eof
)

set "CMD=%~1"

REM 构建参数列表（跳过第一个参数）
set "ARGS="
shift
:loop
if "%~1"=="" goto :done
set "ARGS=!ARGS! %~1"
shift
goto :loop
:done

set "EXIT_CODE=0"

if "%CMD%"=="help" (
    echo ============================================================
    echo AID Client SDK Java CLI 命令帮助
    echo ============================================================
    echo.
    echo 命令格式: cli_start.bat ^<命令^> [参数]
    echo.
    echo [newTaskCreate] - 创建新任务
    echo   参数: --simulateType ^<类型^> --taskName ^<名称^>
    echo   示例: cli_start.bat newTaskCreate --simulateType LaWan --taskName test001
    echo.
    echo [uploadParamfiles] - 上传参数文件
    echo   参数: --TaskID ^<任务ID^> --files ^<文件路径1,文件路径2,...^>
    echo   示例: cli_start.bat uploadParamfiles --TaskID LaWan00000001 --files ./data/a.stp,./data/b.csv
    echo.
    echo [newTaskverify] - 校验任务文件
    echo   参数: --TaskID ^<任务ID^>
    echo   示例: cli_start.bat newTaskverify --TaskID LaWan00000001
    echo.
    echo [startTask] - 启动任务
    echo   参数: --TaskID ^<任务ID^>
    echo   示例: cli_start.bat startTask --TaskID LaWan00000001
    echo.
    echo [queryTaskStatus] - 查询任务状态
    echo   参数: --TaskID ^<任务ID^>
    echo   示例: cli_start.bat queryTaskStatus --TaskID LaWan00000001
    echo.
    echo [stopTask] - 停止任务
    echo   参数: --TaskID ^<任务ID^>
    echo   示例: cli_start.bat stopTask --TaskID LaWan00000001
    echo.
    echo [deleteTask] - 删除任务
    echo   参数: --TaskID ^<任务ID^>
    echo   示例: cli_start.bat deleteTask --TaskID LaWan00000001
    echo.
    echo [fetchTaskResult] - 获取任务结果
    echo   参数: --TaskID ^<任务ID^> --output ^<输出路径^>
    echo   示例: cli_start.bat fetchTaskResult --TaskID LaWan00000001 --output ./result.zip
    echo.
    echo ============================================================
) else if "%CMD%"=="newTaskCreate" (
    "%JAVA_CMD%" -cp "%JAR_PATH%" com.aid.sdk.cli.CliMain newTaskCreate !ARGS!
    set "EXIT_CODE=!ERRORLEVEL!"
    if !EXIT_CODE! neq 0 (
        echo.
        echo [错误] 命令执行失败，请检查参数是否正确
        echo.
        echo 正确示例:
        echo   cli_start.bat newTaskCreate --simulateType LaWan --taskName myTask001
        echo.
        echo 参数说明:
        echo   --simulateType  仿真类型: LaWan / CHOnYA / ZhuZao / ZhaZhi / ZHEWan / JIYA
        echo   --taskName      任务名称: 自定义字符串
    )
) else if "%CMD%"=="uploadParamfiles" (
    "%JAVA_CMD%" -cp "%JAR_PATH%" com.aid.sdk.cli.CliMain uploadParamfiles !ARGS!
    set "EXIT_CODE=!ERRORLEVEL!"
    if !EXIT_CODE! neq 0 (
        echo.
        echo [错误] 命令执行失败，请检查参数是否正确
        echo.
        echo 正确示例:
        echo   cli_start.bat uploadParamfiles --TaskID LaWan00000001 --files ./data/model.stp,./data/params.csv
        echo.
        echo 参数说明:
        echo   --TaskID  任务ID: 如 LaWan00000001
        echo   --files   文件路径: 多个文件用逗号分隔
    )
) else if "%CMD%"=="newTaskverify" (
    "%JAVA_CMD%" -cp "%JAR_PATH%" com.aid.sdk.cli.CliMain newTaskverify !ARGS!
    set "EXIT_CODE=!ERRORLEVEL!"
    if !EXIT_CODE! neq 0 (
        echo.
        echo [错误] 命令执行失败，请检查参数是否正确
        echo.
        echo 正确示例:
        echo   cli_start.bat newTaskverify --TaskID LaWan00000001
        echo.
        echo 参数说明:
        echo   --TaskID  任务ID: 如 LaWan00000001
    )
) else if "%CMD%"=="startTask" (
    "%JAVA_CMD%" -cp "%JAR_PATH%" com.aid.sdk.cli.CliMain startTask !ARGS!
    set "EXIT_CODE=!ERRORLEVEL!"
    if !EXIT_CODE! neq 0 (
        echo.
        echo [错误] 命令执行失败，请检查参数是否正确
        echo.
        echo 正确示例:
        echo   cli_start.bat startTask --TaskID LaWan00000001
        echo.
        echo 参数说明:
        echo   --TaskID  任务ID: 如 LaWan00000001
    )
) else if "%CMD%"=="queryTaskStatus" (
    "%JAVA_CMD%" -cp "%JAR_PATH%" com.aid.sdk.cli.CliMain queryTaskStatus !ARGS!
    set "EXIT_CODE=!ERRORLEVEL!"
    if !EXIT_CODE! neq 0 (
        echo.
        echo [错误] 命令执行失败，请检查参数是否正确
        echo.
        echo 正确示例:
        echo   cli_start.bat queryTaskStatus --TaskID LaWan00000001
        echo.
        echo 参数说明:
        echo   --TaskID  任务ID: 如 LaWan00000001
    )
) else if "%CMD%"=="stopTask" (
    "%JAVA_CMD%" -cp "%JAR_PATH%" com.aid.sdk.cli.CliMain stopTask !ARGS!
    set "EXIT_CODE=!ERRORLEVEL!"
    if !EXIT_CODE! neq 0 (
        echo.
        echo [错误] 命令执行失败，请检查参数是否正确
        echo.
        echo 正确示例:
        echo   cli_start.bat stopTask --TaskID LaWan00000001
        echo.
        echo 参数说明:
        echo   --TaskID  任务ID: 如 LaWan00000001
    )
) else if "%CMD%"=="deleteTask" (
    "%JAVA_CMD%" -cp "%JAR_PATH%" com.aid.sdk.cli.CliMain deleteTask !ARGS!
    set "EXIT_CODE=!ERRORLEVEL!"
    if !EXIT_CODE! neq 0 (
        echo.
        echo [错误] 命令执行失败，请检查参数是否正确
        echo.
        echo 正确示例:
        echo   cli_start.bat deleteTask --TaskID LaWan00000001
        echo.
        echo 参数说明:
        echo   --TaskID  任务ID: 如 LaWan00000001
    )
) else if "%CMD%"=="fetchTaskResult" (
    "%JAVA_CMD%" -cp "%JAR_PATH%" com.aid.sdk.cli.CliMain fetchTaskResult !ARGS!
    set "EXIT_CODE=!ERRORLEVEL!"
    if !EXIT_CODE! neq 0 (
        echo.
        echo [错误] 命令执行失败，请检查参数是否正确
        echo.
        echo 正确示例:
        echo   cli_start.bat fetchTaskResult --TaskID LaWan00000001 --output ./result.zip
        echo.
        echo 参数说明:
        echo   --TaskID  任务ID: 如 LaWan00000001
        echo   --output  输出文件路径: 如 ./result.zip
    )
) else (
    echo 错误: 未知命令 '%CMD%'
    echo 运行 'cli_start.bat' 查看可用命令
    set "EXIT_CODE=1"
)

endlocal
exit /b %EXIT_CODE%
