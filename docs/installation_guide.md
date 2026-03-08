# AID Client SDK（Java 版）安装与配置手册

## 目录

1. [环境要求](#1-环境要求)
2. [目录结构](#2-目录结构)
3. [快速开始（普通用户）](#3-快速开始普通用户)
4. [配置文件说明](#4-配置文件说明)
5. [日志配置](#5-日志配置)
6. [CLI 命令使用](#6-cli-命令使用)
7. [完整调用示例](#7-完整调用示例)


---

## 1. 环境要求

| 组件 | 版本要求 | 说明 |
|-----|---------|------|
| JDK | 1.8（Java 8） | **必须使用 JDK 1.8**，高版本会导致日志库兼容性错误 |
| Maven | 3.6+ | 构建和依赖管理工具 |
| 操作系统 | Windows 10+ / Linux / macOS | 均支持 |

> **重要**：Java 11+ 会导致 Logback 版本冲突（`UnsupportedClassVersionError`），请严格使用 JDK 1.8。

### 验证环境

```bash
java -version
# 期望输出：java version "1.8.x_xxx"

mvn -version
# 期望输出：Apache Maven 3.x.x
```

---

## 2. 目录结构

```
aid-client-sdk-java/
├── config/
│   └── config.properties         # SDK 配置文件（重要）
├── examples/
│   └── BasicUsage.java           # 使用示例（参考）
├── src/
│   └── main/
│       ├── java/com/aid/sdk/     # SDK 源码
│       └── resources/
│           └── logback.xml       # 日志配置文件（重要）
├── target/
│   └── aid-client-sdk-1.0.0.jar # 构建产物（Fat JAR）
├── logs/                         # 日志目录（运行时自动创建）
│   └── aid-sdk-YYYY-MM-DD.log
├── cli_start.bat                 # Windows CLI 启动脚本
├── cli_start.sh                  # Linux/macOS CLI 启动脚本
└── pom.xml                       # Maven 依赖配置
```

---

## 3. 快速开始（普通用户）

> 如果您只需要**使用 CLI 工具**调用仿真服务，按以下步骤操作即可，**无需安装 Maven，无需编译源码**。

### 3.1 解压安装包

将 `aid-client-sdk-1.0.0.zip` 解压到任意目录：

```
aid-client-sdk-java/
├── aid-client-sdk-1.0.0.jar   ← 已包含所有依赖，直接运行
├── config/config.properties   ← 配置服务器地址和密钥
├── cli_start.bat              ← Windows 启动脚本
├── cli_start.sh               ← Linux/macOS 启动脚本
└── examples/                  ← 示例数据和代码（参考）
```

### 3.2 确认 Java 环境

只需安装 **JDK 1.8（Java 8）** 或对应 JRE：

```bash
java -version
# 期望输出：java version "1.8.x_xxx"
```

> **重要**：必须使用 Java 8，Java 11+ 会导致日志库兼容性错误（`UnsupportedClassVersionError`）。

### 3.3 修改配置文件

编辑 `config/config.properties`，填写服务器地址和认证密钥（详见 [第 4 章](#4-配置文件说明)）。

### 3.4 运行 CLI

```bash
# Windows（双击或命令行运行）
cli_start.bat

# Linux / macOS
./cli_start.sh
```

不带参数运行可查看所有可用命令和示例。

---

## 4. 配置文件说明

配置文件路径：`config/config.properties`

```properties
# AID Client SDK Configuration

# 后端服务基础URL（必填）
baseURL=http://111.228.12.67:28090/api/v1

# API认证Token（必填，须与服务端 app_config.yaml 中的 api_key 一致）
api_token=11111111
```

### 4.1 字段说明

| 字段 | 说明 | 示例 |
|-----|------|------|
| `baseURL` | AID-Service 服务地址，包含协议、IP、端口和路径前缀 | `http://111.228.12.67:28090/api/v1` |
| `api_token` | 认证密钥，须与服务端配置一致 | `11111111` |

### 4.2 常见配置场景

**生产服务器（默认）：**
```properties
baseURL=http://111.228.12.67:28090/api/v1
api_token=11111111
```

**连接其他服务器：**
```properties
baseURL=http://192.168.1.100:8080/api/v1
api_token=your_production_api_key
```

> **重要**：URL 路径必须以 `/api/v1` 结尾，不能使用旧版路径 `/aid-service`。

---

## 5. 日志配置

日志配置文件：`src/main/resources/logback.xml`

### 5.1 日志级别修改

找到以下两处 `level` 属性进行修改：

```xml
<!-- 修改全局日志级别 -->
<root level="DEBUG">   <!-- 可选：DEBUG / INFO / WARN / ERROR -->

<!-- 修改 SDK 内部日志级别 -->
<logger name="com.aid.sdk" level="DEBUG" additivity="false">
```

| 级别 | 适用场景 |
|-----|---------|
| `DEBUG` | 开发、测试阶段，输出详细调试信息 |
| `INFO` | 生产环境，记录关键流程（推荐） |
| `WARN` | 只记录警告及以上 |
| `ERROR` | 只记录错误 |

### 5.2 日志文件位置

- 日志目录：`logs/`（相对于 JAR 运行目录，自动创建）
- 当前日志：`logs/aid-sdk.log`
- 历史日志：`logs/aid-sdk-YYYY-MM-DD.log`（按天滚动，保留 30 天）

### 5.3 日志格式

日志输出为 JSON 格式，包含 taskID、api、code 结构化字段：

```json
{"timestamp":"2026-03-05T14:00:16.503+0800","level":"INFO","logger_name":"com.aid.sdk","message":"创建任务成功","taskID":"AID-20260305-001","api":"newTaskCreate"}
```

### 5.4 注意事项

修改 `logback.xml` 后需重新打包才能生效：

```bash
mvn clean package -DskipTests
```

---

## 6. CLI 命令使用

SDK 根目录的 `cli_start.bat`（Windows）/ `cli_start.sh`（Linux/macOS）封装了所有命令，无需手动拼写 JAR 路径。

**查看帮助（不带参数运行）：**
```bash
cli_start.bat
```

**各命令示例（Windows）：**
```bash
# 1. 创建任务
cli_start.bat newTaskCreate --simulateType LaWan --taskName myTask001

# 2. 上传参数文件（多文件用逗号分隔）
cli_start.bat uploadParamfiles --TaskID LaWan00000001 --files ./data/model.stp,./data/params.csv

# 3. 校验文件
cli_start.bat newTaskverify --TaskID LaWan00000001

# 4. 启动任务
cli_start.bat startTask --TaskID LaWan00000001

# 5. 查询状态
cli_start.bat queryTaskStatus --TaskID LaWan00000001

# 6. 停止任务
cli_start.bat stopTask --TaskID LaWan00000001

# 7. 删除任务
cli_start.bat deleteTask --TaskID LaWan00000001

# 8. 获取任务结果
cli_start.bat fetchTaskResult --TaskID LaWan00000001 --output ./result.zip
```

> **提示**：若未设置 `JAVA_HOME`，脚本会自动使用 `java.exe`（需在系统 PATH 中）。

---

## 7. 完整调用示例

参考 `examples/BasicUsage.java`，完整流程如下：

```
初始化 AidClient（读取 config.properties）
    ↓
步骤 1：newTaskCreate（创建仿真任务）→ 获取 TaskID
    ↓
步骤 2：uploadParamfiles（上传参数文件）
    ↓
步骤 3：newTaskVerify（校验文件完整性）
    ↓
步骤 4：startTask（启动任务）
    ↓
步骤 5：queryTaskStatus（轮询状态，每 10 秒一次）
    ↓ 状态 = COMPLETED
步骤 6：fetchTaskResult（下载仿真结果）
```

### 关键配置常量（BasicUsage.java）

| 常量 | 说明 | 默认值 |
|-----|------|-------|
| `CONFIG_PATH` | 配置文件路径 | `./config/config.properties` |
| `SIMULATE_TYPE` | 仿真类型 | `LaWan` |
| `TASK_NAME` | 任务名称 | `java_demo_task_001` |
| `PARAM_FILE_PATHS` | 参数文件列表 | `./data/model.stp` 等 |
| `OUTPUT_DIR` | 结果下载目录 | `./results/` |
| `POLL_INTERVAL_MS` | 状态轮询间隔（毫秒） | `10000`（10秒） |

### 支持的仿真类型

| 值 | 说明 |
|----|------|
| `LaWan` | 拉弯成型 |
| `CHOnYA` | 冲压 |
| `ZhuZao` | 铸造 |
| `ZhaZhi` | 轧制 |
| `ZHEWan` | 折弯 |
| `JIYA` | 挤压 |

---


