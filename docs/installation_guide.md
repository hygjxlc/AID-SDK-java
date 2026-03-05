# AID Client SDK（Java 版）安装与配置手册

## 目录

1. [环境要求](#1-环境要求)
2. [目录结构](#2-目录结构)
3. [安装步骤](#3-安装步骤)
4. [配置文件说明](#4-配置文件说明)
5. [日志配置](#5-日志配置)
6. [构建与运行](#6-构建与运行)
7. [完整调用示例](#7-完整调用示例)
8. [常见问题](#8-常见问题)

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
└── pom.xml                       # Maven 依赖配置
```

---

## 3. 安装步骤

### 3.1 克隆代码

```bash
git clone <仓库地址>
cd aid-client-sdk-java
```

### 3.2 安装 Maven（Windows）

若未安装 Maven，从官网下载后配置环境变量，或使用以下命令下载到用户目录（推荐，避免 C:\ 权限问题）：

```powershell
# 下载并解压到用户目录
$url = "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip"
Invoke-WebRequest -Uri $url -OutFile "$env:USERPROFILE\maven.zip"
Expand-Archive -Path "$env:USERPROFILE\maven.zip" -DestinationPath "$env:USERPROFILE\"

# 配置环境变量（当前会话有效）
$env:MAVEN_HOME = "$env:USERPROFILE\apache-maven-3.9.6"
$env:PATH = "$env:MAVEN_HOME\bin;$env:PATH"
```

### 3.3 主要依赖说明

依赖版本已在 `pom.xml` 中锁定，适配 JDK 1.8：

| 依赖 | 版本 | 说明 |
|-----|-----|------|
| OkHttp | 4.9.3 | HTTP 客户端 |
| Jackson | 2.15.2 | JSON 序列化/反序列化 |
| Commons-CLI | 1.5.0 | 命令行参数解析 |
| SLF4J | **1.7.36** | 日志门面（JDK 1.8 兼容版本） |
| Logback | **1.2.13** | 日志实现（JDK 1.8 兼容版本） |
| logstash-logback-encoder | **6.6** | JSON 格式日志（JDK 1.8 兼容版本） |

> **注意**：SLF4J 2.x、Logback 1.4.x、logstash 7.x 均要求 Java 11+，在 JDK 1.8 上会报错，已降级至以上版本。

---

## 4. 配置文件说明

配置文件路径：`config/config.properties`

```properties
# AID Client SDK Configuration

# 后端服务基础URL（必填）
baseURL=http://127.0.0.1:8080/api/v1

# API认证Token（必填，须与服务端 app_config.yaml 中的 api_key 一致）
api_token=11111111
```

### 4.1 字段说明

| 字段 | 说明 | 示例 |
|-----|------|------|
| `baseURL` | AID-Service 服务地址，包含协议、IP、端口和路径前缀 | `http://127.0.0.1:8080/api/v1` |
| `api_token` | 认证密钥，须与服务端配置一致 | `11111111` |

### 4.2 常见配置场景

**本机调试（默认）：**
```properties
baseURL=http://127.0.0.1:8080/api/v1
api_token=11111111
```

**连接远程服务器：**
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

## 6. 构建与运行

### 6.1 构建 Fat JAR

```bash
cd aid-client-sdk-java

# 编译并打包（包含所有依赖）
mvn clean package -DskipTests
```

构建成功后，在 `target/` 目录生成：
- `aid-client-sdk-1.0.0.jar`（Fat JAR，可直接运行）

### 6.2 运行示例程序

```bash
# Windows（使用完整 JDK 路径，避免注册表路径问题）
"C:\Program Files\Java\jdk1.8.0_291\bin\java.exe" -jar target\aid-client-sdk-1.0.0.jar com.aid.sdk.examples.BasicUsage

# Linux / macOS
java -jar target/aid-client-sdk-1.0.0.jar com.aid.sdk.examples.BasicUsage
```

### 6.3 使用 CLI 命令行工具

```bash
# 查看帮助
java -jar target/aid-client-sdk-1.0.0.jar help

# 创建任务
java -jar target/aid-client-sdk-1.0.0.jar newTaskCreate --type LaWan --name my_task

# 上传参数文件
java -jar target/aid-client-sdk-1.0.0.jar uploadParamfiles --taskId <taskId> --files ./data/model.stp,./data/params.csv

# 校验文件
java -jar target/aid-client-sdk-1.0.0.jar newTaskVerify --taskId <taskId>

# 启动任务
java -jar target/aid-client-sdk-1.0.0.jar startTask --taskId <taskId>

# 查询状态
java -jar target/aid-client-sdk-1.0.0.jar queryTaskStatus --taskId <taskId>

# 下载结果
java -jar target/aid-client-sdk-1.0.0.jar fetchTaskResult --taskId <taskId> --output ./results/
```

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

## 8. 常见问题

### Q1：`UnsupportedClassVersionError` 启动报错

**错误信息**：
```
ch/qos/logback/classic/spi/LogbackServiceProvider has been compiled by a more recent version of the Java Runtime (class file version 55.0)
```

**原因**：`pom.xml` 中 Logback 版本过高（1.4.x 要求 Java 11+）。

**解决**：确认 `pom.xml` 中版本为：
```xml
<logback.version>1.2.13</logback.version>
<slf4j.version>1.7.36</slf4j.version>
<logstash.version>6.6</logstash.version>
```

---

### Q2：`NoSuchMethodError: ILoggingEvent.getInstant()`

**原因**：logstash-logback-encoder 7.x 不兼容 JDK 1.8。

**解决**：确认 `pom.xml` 中：
```xml
<logstash.version>6.6</logstash.version>
```

---

### Q3：API 返回 401 认证失败

**错误**：`API Key认证失败，无效的密钥`

**原因**：`config.properties` 中的 `api_token` 与服务端不一致。

**解决**：检查并对齐：
- SDK：`config/config.properties` → `api_token`
- 服务端：`AID-service/app_config.yaml` → `auth.api_key`

---

### Q4：API 返回 404 Not Found

**原因**：`baseURL` 路径配置错误，使用了旧版 `/aid-service` 路径。

**解决**：
```properties
# 正确
baseURL=http://127.0.0.1:8080/api/v1

# 错误（旧版）
baseURL=http://127.0.0.1:8080/aid-service
```

---

### Q5：`ClassNotFoundException: com.aid.sdk.examples.BasicUsage`

**原因**：`BasicUsage.java` 未放入正确的源码目录。

**解决**：确认文件存在于：
```
src/main/java/com/aid/sdk/examples/BasicUsage.java
```

---

### Q6：Maven 下载依赖失败（C:\ 权限问题）

**原因**：Maven 默认将本地仓库存储在 `C:\Users\<用户名>\.m2`，某些环境存在权限限制。

**解决**：指定本地仓库到用户目录：
```powershell
mvn clean package -Dmaven.repo.local="$env:USERPROFILE\.m2\repository" -DskipTests
```

---

### Q7：修改 logback.xml 后日志级别不变

**原因**：`logback.xml` 打包进了 JAR，修改文件后未重新构建。

**解决**：
```bash
mvn clean package -DskipTests
```
