# Temporal Health Check Demo

基于 **Temporal + Spring Boot** 的患者健康检查工作流演示项目，用于展示 Temporal 工作流引擎的核心能力。

## 技术栈

| 组件 | 版本 |
|---|---|
| Java | 17 |
| Spring Boot | 4.0.3 |
| Temporal SDK | 1.24.1 |
| Temporal Server | 1.29.2 |
| PostgreSQL | 12 |

## 业务场景

模拟一个完整的医疗健康检查流程：

```
患者就诊 → 记录就诊 → 获取诊断 → 分析严重程度 → 随访循环 → 生成报告
                                                    ↑
                                        外部事件（Signal）可动态改变流程
```

### 工作流步骤

1. **记录就诊** — 记录患者基本信息（ID、姓名、医生、就诊原因）
2. **获取诊断结果** — 根据场景生成诊断评分（0.0 ~ 1.0）
3. **分析严重程度** — 按评分划分等级：
   - `NORMAL`（< 0.4）— 正常
   - `ABNORMAL`（0.4 ~ 0.75）— 异常
   - `SEVERE`（≥ 0.75）— 严重
4. **随访循环** — 根据严重程度动态决定随访次数和间隔：

   | 严重程度 | 最大随访次数 | 随访间隔（Demo 模式） | 随访间隔（真实模式） |
   |---|---|---|---|
   | NORMAL | 3 | 30 秒 | 7 天 |
   | ABNORMAL | 5 | 20 秒 | 7 天 |
   | SEVERE | 7 | 15 秒 | 3 天 |

5. **生成总结报告** — 汇总诊断结果、随访记录、最终严重程度

### 动态行为

- 随访过程中健康评分会**动态变化**，可能触发严重程度升级/降级
- 严重程度变化时自动调整随访计划（次数和间隔）
- `SEVERE` 患者会立即通知医生
- 恢复至 `NORMAL` 后提前结束随访

## 演示的 Temporal 特性

| 特性 | 体现 |
|---|---|
| **Workflow** | `HealthCheckWorkflow` 定义主流程，编排多个 Activity |
| **Activity** | `HealthCheckActivities` 封装 5 个可重试的业务操作 |
| **Signal（信号）** | `cancelFollowUp` — 外部取消随访；`newLabResult` — 注入新化验结果 |
| **Query（查询）** | `getStatus` — 实时查询工作流状态（阶段、严重程度、随访进度） |
| **持久化等待** | `Workflow.await()` 实现 Signal 感知的持久化睡眠 |
| **自动重试** | Activity 失败自动重试（最多 3 次，初始间隔 1 秒） |
| **确定性重放** | Worker 崩溃重启后通过 Event Sourcing 恢复状态 |

## 项目结构

```
src/main/java/com/example/temporaldemo/
├── TemporalDemoApplication.java          # Spring Boot 启动类
└── healthcheck/
    ├── HealthCheckConstants.java          # 共享常量（Task Queue 名称）
    ├── config/
    │   └── TemporalConfig.java           # Temporal Bean 配置（WorkflowClient + Worker）
    ├── controller/
    │   └── HealthCheckController.java    # REST API（启动工作流、发送 Signal、查询状态）
    ├── model/
    │   ├── CancelRequest.java            # 取消请求体
    │   ├── DiagnosisResult.java          # 诊断结果
    │   ├── FollowUpRecord.java           # 随访记录
    │   ├── HealthCheckStatus.java        # 工作流状态（Query 返回值）
    │   ├── LabResultRequest.java         # 化验结果请求体
    │   ├── PatientVisit.java             # 患者就诊信息
    │   ├── Severity.java                 # 严重程度枚举（含 fromScore 静态方法）
    │   └── StartHealthCheckRequest.java  # 启动工作流请求体
    ├── activity/
    │   ├── HealthCheckActivities.java    # Activity 接口
    │   └── HealthCheckActivitiesImpl.java# Activity 实现
    ├── workflow/
    │   ├── HealthCheckWorkflow.java      # Workflow 接口（含 Signal + Query 定义）
    │   └── HealthCheckWorkflowImpl.java  # Workflow 实现
    ├── starter/
    │   ├── HealthCheckStarter.java       # CLI 方式启动工作流（旧）
    │   └── SignalSender.java             # CLI 方式发送 Signal（旧）
    └── worker/
        └── HealthCheckWorker.java        # 独立 Worker 进程（旧）
```

## 快速开始

### 前置条件

- Java 17+
- Docker & Docker Compose

### 1. 启动 Temporal Server

```bash
docker compose up -d
```

等待约 30 秒，验证服务：

```bash
docker compose ps
# 确认 temporal-db、temporal-server、temporal-ui 三个容器都是 running
```

### 2. 启动应用

```bash
./gradlew bootRun
```

应用启动后同时包含：
- **Worker** — 监听 `health-check-task-queue`，执行 Workflow 和 Activity
- **HTTP API** — 端口 `8081`，提供 REST 接口

### 3. 打开 Temporal UI

浏览器访问 http://localhost:8080 ，可查看工作流列表和 Event History。

## HTTP API

### 启动工作流

```bash
curl -X POST http://localhost:8081/api/healthcheck/start \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "P001",
    "patientName": "John Doe",
    "doctorName": "Dr. Smith",
    "visitReason": "Annual physical exam",
    "scenario": "severe"
  }'
```

`scenario` 可选值：`normal`、`abnormal`、`severe`，不传则随机。

### 查询状态

```bash
curl http://localhost:8081/api/healthcheck/health-check-P001/status
```

返回示例：

```json
{
  "patientId": "P001",
  "currentSeverity": "SEVERE",
  "followUpCompleted": 2,
  "maxFollowUps": 7,
  "cancelled": false,
  "currentPhase": "FOLLOW_UP"
}
```

### 发送化验结果（Signal）

```bash
curl -X POST http://localhost:8081/api/healthcheck/health-check-P001/labresult \
  -H "Content-Type: application/json" \
  -d '{"score": 0.9}'
```

`score` 范围 0.0 ~ 1.0，会动态改变严重程度和随访计划。

### 取消随访（Signal）

```bash
curl -X POST http://localhost:8081/api/healthcheck/health-check-P001/cancel \
  -H "Content-Type: application/json" \
  -d '{"reason": "Patient transferred to another hospital"}'
```

## 演示脚本

### 第一幕：正常场景 — 快速完成

```bash
curl -X POST http://localhost:8081/api/healthcheck/start \
  -H "Content-Type: application/json" \
  -d '{"patientId":"P001","scenario":"normal"}'
```

观察：严重程度为 `NORMAL`，少量随访后自动完成。

### 第二幕：严重场景 — 长时间随访

```bash
curl -X POST http://localhost:8081/api/healthcheck/start \
  -H "Content-Type: application/json" \
  -d '{"patientId":"P002","scenario":"severe"}'
```

观察：多次随访，通过 `/status` 接口轮询查看进度变化。

### 第三幕：Signal 改变病情

```bash
# 启动 abnormal 场景
curl -X POST http://localhost:8081/api/healthcheck/start \
  -H "Content-Type: application/json" \
  -d '{"patientId":"P003","scenario":"abnormal"}'

# 等几秒后注入恶化的化验结果
curl -X POST http://localhost:8081/api/healthcheck/health-check-P003/labresult \
  -H "Content-Type: application/json" \
  -d '{"score": 0.9}'

# 再注入好转的结果
curl -X POST http://localhost:8081/api/healthcheck/health-check-P003/labresult \
  -H "Content-Type: application/json" \
  -d '{"score": 0.1}'
```

观察：严重程度从 `ABNORMAL` → `SEVERE` → `NORMAL`，随访计划动态调整。

### 第四幕：外部取消

```bash
# 启动 severe 场景
curl -X POST http://localhost:8081/api/healthcheck/start \
  -H "Content-Type: application/json" \
  -d '{"patientId":"P004","scenario":"severe"}'

# 发送取消信号
curl -X POST http://localhost:8081/api/healthcheck/health-check-P004/cancel \
  -H "Content-Type: application/json" \
  -d '{"reason": "Patient transferred to another hospital"}'
```

观察：工作流优雅终止，生成包含取消原因的报告。

### 第五幕：崩溃恢复

```bash
# 1. 启动一个 severe 场景
curl -X POST http://localhost:8081/api/healthcheck/start \
  -H "Content-Type: application/json" \
  -d '{"patientId":"P005","scenario":"severe"}'

# 2. 确认工作流在运行中
curl http://localhost:8081/api/healthcheck/health-check-P005/status

# 3. 强杀 Worker 进程（模拟崩溃）
kill -9 $(jps | grep TemporalDemoApplication | awk '{print $1}')

# 4. 在 Temporal UI (http://localhost:8080) 查看，工作流卡在 Running 状态

# 5. 重新启动应用
./gradlew bootRun

# 6. 工作流自动从中断点恢复继续执行
curl http://localhost:8081/api/healthcheck/health-check-P005/status
```

## 核心原理

### Event Sourcing（事件溯源）

Temporal 不持久化 Workflow 的内存变量。它将每个关键事件（Activity 调用/返回、Timer、Signal）持久化到数据库。恢复状态时通过**重放 Workflow 代码 + 跳过已完成的步骤**实现：

```
重放：从 Workflow 代码第一行开始执行
  recordVisit()      → 历史有结果 → 跳过，返回旧值
  getDiagnosisResult → 历史有结果 → 跳过，返回旧值
  followUpCompleted++ → 纯 Java 代码，正常执行
  performFollowUp()  → 历史有结果 → 跳过，返回旧值
  Workflow.await()   → 历史有 TimerFired → 跳过
  performFollowUp()  → 历史无记录 → 真正执行
```

### 确定性约束

由于每次事件触发都会重放 Workflow 代码，因此 Workflow 中**不能**包含非确定性操作：

| 禁止 | 替代方案 |
|---|---|
| `new Random()` | 放在 Activity 里 |
| `System.currentTimeMillis()` | 使用 `Workflow.currentTimeMillis()` |
| 直接 HTTP/数据库调用 | 放在 Activity 里 |
| `Thread.sleep()` | 使用 `Workflow.sleep()` 或 `Workflow.await()` |

### Workflow.await() 机制

`Workflow.await(timeout, condition)` 不是真正的线程阻塞：

1. SDK 向 Server 注册 Timer
2. **释放 Worker 线程**（等待中的 Workflow 不消耗任何 Worker 资源）
3. Timer 到期或 Signal 到达时，Worker 领取新的 WorkflowTask
4. 重放 Workflow 代码到 `await()` 位置，评估条件决定是继续等还是往下走

## 配置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `server.port` | 8081 | HTTP 端口（避免与 Temporal UI 8080 冲突） |
| `temporal.server.target` | 127.0.0.1:7233 | Temporal Server gRPC 地址 |

## 清理

```bash
docker compose down -v
```
