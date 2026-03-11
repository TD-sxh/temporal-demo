# Workflow Definition API Documentation

**Base URL:** `/api/definitions`

## Data Model

### WorkflowDefinitionEntity

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Database primary key (auto-increment) |
| `type` | String | Workflow type identifier, e.g. `health-check-flow` (max 128 chars) |
| `name` | String | Human-readable name (max 256 chars) |
| `version` | Integer | Version number (auto-incremented per type; type+version is unique) |
| `status` | Enum | Status: `DRAFT` / `PUBLISHED` / `ARCHIVED` |
| `definitionJson` | String(TEXT) | Complete workflow JSON definition |
| `description` | String(TEXT) | Optional description |
| `createdAt` | DateTime | Creation time |
| `updatedAt` | DateTime | Last update time |

### Status Lifecycle

```
DRAFT → PUBLISHED → ARCHIVED
```

- **DRAFT** — Draft, can be edited/deleted, cannot be used to start workflows
- **PUBLISHED** — Published, can be used to start workflows (latest PUBLISHED version per type is preferred)
- **ARCHIVED** — Archived, cannot be used to start new workflows

---

## API Endpoints

### 1. Create Workflow Definition

Create a new version for the specified type (initial status: DRAFT). Version number is auto-incremented.

```
POST /api/definitions
```

**Request Body:**

```json
{
  "type": "health-check-flow",
  "name": "Health Check Workflow",
  "definitionJson": "{ ... full JSON definition ... }",
  "description": "Initial version"
}
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `type` | String | Yes | Workflow type identifier |
| `name` | String | No | Human-readable name |
| `definitionJson` | String / Object | Yes | Workflow JSON definition (supports string or nested JSON object) |
| `description` | String | No | Description |

**Success Response:** `201 Created`

```json
{
  "id": 1,
  "type": "health-check-flow",
  "name": "Health Check Workflow",
  "version": 1,
  "status": "DRAFT",
  "description": "Initial version",
  "createdAt": "2026-03-10T09:00:00",
  "updatedAt": "2026-03-10T09:00:00"
}
```

**Error Responses:** `400 Bad Request`

```json
{ "error": "type is required" }
```
```json
{ "error": "definitionJson is required" }
```
```json
{ "error": "Invalid definitionJson: ..." }
```

---

### 2. List All Workflow Types (latest version each)

```
GET /api/definitions
```

**Success Response:** `200 OK`

```json
[
  {
    "id": 3,
    "type": "health-check-flow",
    "name": "Health Check Workflow",
    "version": 3,
    "status": "PUBLISHED",
    "description": "Latest version",
    "createdAt": "2026-03-10T09:00:00",
    "updatedAt": "2026-03-10T09:00:00"
  },
  {
    "id": 5,
    "type": "order-flow",
    "name": "Order Processing",
    "version": 2,
    "status": "DRAFT",
    "description": null,
    "createdAt": "2026-03-10T10:00:00",
    "updatedAt": "2026-03-10T10:00:00"
  }
]
```

---

### 3. List All Type Names

```
GET /api/definitions/types
```

**Success Response:** `200 OK`

```json
["health-check-flow", "order-flow", "approval-flow"]
```

---

### 4. List All Versions of a Type

```
GET /api/definitions/{type}
```

| Path Parameter | Type | Description |
|----------------|------|-------------|
| `type` | String | Workflow type identifier |

**Example:** `GET /api/definitions/health-check-flow`

**Success Response:** `200 OK` (sorted by version descending)

```json
[
  {
    "id": 3,
    "type": "health-check-flow",
    "name": "Health Check V3",
    "version": 3,
    "status": "DRAFT",
    "description": "WIP",
    "createdAt": "2026-03-10T11:00:00",
    "updatedAt": "2026-03-10T11:00:00"
  },
  {
    "id": 2,
    "type": "health-check-flow",
    "name": "Health Check V2",
    "version": 2,
    "status": "PUBLISHED",
    "description": "Production version",
    "createdAt": "2026-03-09T10:00:00",
    "updatedAt": "2026-03-09T15:00:00"
  },
  {
    "id": 1,
    "type": "health-check-flow",
    "name": "Health Check V1",
    "version": 1,
    "status": "ARCHIVED",
    "description": "Initial version",
    "createdAt": "2026-03-08T09:00:00",
    "updatedAt": "2026-03-09T10:00:00"
  }
]
```

---

### 5. Get Specific Version of a Type

```
GET /api/definitions/{type}/{version}
```

| Path Parameter | Type | Description |
|----------------|------|-------------|
| `type` | String | Workflow type identifier |
| `version` | Integer | Version number |

**Example:** `GET /api/definitions/health-check-flow/2`

**Success Response:** `200 OK` (includes full definitionJson)

```json
{
  "id": 2,
  "type": "health-check-flow",
  "name": "Health Check V2",
  "version": 2,
  "status": "PUBLISHED",
  "description": "Production version",
  "createdAt": "2026-03-09T10:00:00",
  "updatedAt": "2026-03-09T15:00:00",
  "definitionJson": "{ ... full JSON ... }"
}
```

**Error Response:** `404 Not Found` (when version does not exist)

---

### 6. Get by Database ID

```
GET /api/definitions/id/{id}
```

| Path Parameter | Type | Description |
|----------------|------|-------------|
| `id` | Long | Database primary key |

**Example:** `GET /api/definitions/id/2`

**Success Response:** `200 OK` (includes full definitionJson, same format as endpoint 5)

**Error Response:** `404 Not Found`

---

### 7. Update Draft Definition

Only `DRAFT` definitions can be updated. All fields are optional.

```
PUT /api/definitions/{id}
```

| Path Parameter | Type | Description |
|----------------|------|-------------|
| `id` | Long | Database primary key |

**Request Body:**

```json
{
  "name": "Updated Name",
  "definitionJson": "{ ... updated JSON ... }",
  "description": "Updated description"
}
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `name` | String | No | Updated name |
| `definitionJson` | String / Object | No | Updated JSON definition |
| `description` | String | No | Updated description |

**Success Response:** `200 OK`

```json
{
  "id": 3,
  "type": "health-check-flow",
  "name": "Updated Name",
  "version": 3,
  "status": "DRAFT",
  "description": "Updated description",
  "createdAt": "2026-03-10T11:00:00",
  "updatedAt": "2026-03-10T12:00:00"
}
```

**Error Responses:**

| Status Code | Scenario |
|-------------|----------|
| `400` | `Invalid definitionJson: ...` |
| `404` | Definition not found |
| `500` | `Only DRAFT definitions can be updated. Current status: PUBLISHED` |

---

### 8. Publish Definition

Changes status from `DRAFT` to `PUBLISHED`. Once published, the definition can be used by the workflow engine.

```
POST /api/definitions/{id}/publish
```

| Path Parameter | Type | Description |
|----------------|------|-------------|
| `id` | Long | Database primary key |

**Success Response:** `200 OK`

```json
{
  "id": 3,
  "type": "health-check-flow",
  "name": "Health Check V3",
  "version": 3,
  "status": "PUBLISHED",
  "description": "...",
  "createdAt": "2026-03-10T11:00:00",
  "updatedAt": "2026-03-10T13:00:00"
}
```

**Error Response:** `500` — `Only DRAFT definitions can be published. Current status: ...`

---

### 9. Archive Definition

Archive a definition (soft delete). Archived definitions will not be used for new workflow starts.

```
POST /api/definitions/{id}/archive
```

| Path Parameter | Type | Description |
|----------------|------|-------------|
| `id` | Long | Database primary key |

**Success Response:** `200 OK`

```json
{
  "id": 2,
  "type": "health-check-flow",
  "name": "Health Check V2",
  "version": 2,
  "status": "ARCHIVED",
  "description": "...",
  "createdAt": "2026-03-09T10:00:00",
  "updatedAt": "2026-03-10T14:00:00"
}
```

---

### 10. Delete Draft Definition

Permanently delete a `DRAFT` definition. Non-DRAFT definitions cannot be deleted (use archive instead).

```
DELETE /api/definitions/{id}
```

| Path Parameter | Type | Description |
|----------------|------|-------------|
| `id` | Long | Database primary key |

**Success Response:** `200 OK`

```json
{
  "message": "Deleted successfully",
  "id": "3"
}
```

**Error Responses:**

| Status Code | Scenario |
|-------------|----------|
| `404` | Definition not found |
| `500` | `Only DRAFT definitions can be deleted. Current status: PUBLISHED. Use archive instead.` |

---

## API Summary

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/definitions` | Create new version (DRAFT) |
| `GET` | `/api/definitions` | List all types (latest version each) |
| `GET` | `/api/definitions/types` | List all type names |
| `GET` | `/api/definitions/{type}` | List all versions of a type |
| `GET` | `/api/definitions/{type}/{version}` | Get specific version (includes definitionJson) |
| `GET` | `/api/definitions/id/{id}` | Get by ID (includes definitionJson) |
| `PUT` | `/api/definitions/{id}` | Update draft |
| `POST` | `/api/definitions/{id}/publish` | Publish draft |
| `POST` | `/api/definitions/{id}/archive` | Archive |
| `DELETE` | `/api/definitions/{id}` | Delete draft |

## Notes

- When starting a workflow (`POST /api/engine/start`), the engine first looks for the latest `PUBLISHED` version in DB; if not found, falls back to classpath file `workflows/{type}.json`
- `definitionJson` accepts either a JSON string or a nested JSON object (the latter is auto-serialized)
- List endpoints (2, 3, 4) do not include `definitionJson` in the response; detail endpoints (5, 6) do
- Version numbers are auto-incremented per type; no manual specification needed

---

---

# Workflow Admin API Documentation

**Base URL:** `http://localhost:8083/api/workflows`

**Service:** `workflow-admin` (port 8083)

管理运行中的 workflow 实例：查询列表、查看详情、暂停/恢复/终止。

---

## Data Model

### WorkflowInfo（列表条目）

| 字段 | 类型 | 说明 |
|------|------|------|
| `workflowId` | String | Workflow ID，启动时指定 |
| `runId` | String | 本次运行唯一 ID（同一 workflowId 可多次运行） |
| `workflowType` | String | Workflow 类型名，如 `OrchestratorWorkflow` |
| `status` | String | 执行状态（见下表） |
| `taskQueue` | String | Worker 监听的 task queue |
| `startTime` | String (ISO 8601) | 启动时间 |
| `closeTime` | String (ISO 8601) | 结束时间（仅已完成/终止时有值） |

### WorkflowDetail（详情条目，继承 WorkflowInfo）

| 额外字段 | 类型 | 说明 |
|---------|------|------|
| `historyLength` | Long | 历史事件数量 |
| `pendingActivities` | Integer | 当前待完成的 Activity 数量 |
| `runtimeStatus` | Object | 仅 RUNNING 状态时存在，包含运行时变量（见下） |

### runtimeStatus 结构

| 字段 | 类型 | 说明 |
|------|------|------|
| `currentNodeId` | String | 当前正在执行的节点 ID |
| `statusMessage` | String | 状态描述，如 `EXECUTING: task_001`、`PAUSED` |
| `paused` | Boolean | 是否处于暂停状态 |
| `variables` | Object | 当前 workflow 上下文中的所有变量 |

### Workflow 状态值

| status | 说明 |
|--------|------|
| `RUNNING` | 执行中 |
| `COMPLETED` | 正常完成 |
| `FAILED` | 失败 |
| `CANCELED` | 已取消 |
| `TERMINATED` | 被强制终止 |
| `TIMED_OUT` | 超时 |
| `CONTINUED_AS_NEW` | 以新 Run 继续 |

---

## API Endpoints

### 1. 获取 Workflow 列表

```
GET /api/workflows
```

**Query 参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `query` | String | 否 | 无（返回全部） | Temporal Query Language (TQL) 过滤表达式 |
| `pageSize` | Integer | 否 | `20` | 每页条数 |
| `nextPageToken` | String | 否 | 无 | 翻页 token（Base64），从上一次响应的 `nextPageToken` 获取 |

**常用 TQL 过滤示例：**

```
# 只看运行中
query=ExecutionStatus='Running'

# 按 workflowId 精确查找
query=WorkflowId='hc-patient-001'

# 按类型过滤
query=WorkflowType='OrchestratorWorkflow'

# 组合过滤：运行中且特定 task queue
query=ExecutionStatus='Running' AND TaskQueue='orchestrator-task-queue'
```

**成功响应：** `200 OK`

```json
{
  "total": 2,
  "workflows": [
    {
      "workflowId": "hc-patient-001",
      "runId": "a1b2c3d4-...",
      "workflowType": "OrchestratorWorkflow",
      "status": "RUNNING",
      "taskQueue": "orchestrator-task-queue",
      "startTime": "2026-03-11T10:00:00Z"
    },
    {
      "workflowId": "hc-patient-002",
      "runId": "e5f6g7h8-...",
      "workflowType": "OrchestratorWorkflow",
      "status": "COMPLETED",
      "taskQueue": "orchestrator-task-queue",
      "startTime": "2026-03-11T09:00:00Z",
      "closeTime": "2026-03-11T09:05:00Z"
    }
  ],
  "nextPageToken": "abc123=="
}
```

> 响应中无 `nextPageToken` 字段表示已是最后一页。

---

### 2. 获取 Workflow 详情

```
GET /api/workflows/{workflowId}
```

**路径参数：**

| 参数 | 说明 |
|------|------|
| `workflowId` | 启动 workflow 时指定的 ID |

**示例：** `GET /api/workflows/hc-patient-001`

**成功响应（RUNNING 状态）：** `200 OK`

```json
{
  "workflowId": "hc-patient-001",
  "runId": "a1b2c3d4-...",
  "workflowType": "OrchestratorWorkflow",
  "status": "RUNNING",
  "taskQueue": "orchestrator-task-queue",
  "startTime": "2026-03-11T10:00:00Z",
  "historyLength": 42,
  "pendingActivities": 1,
  "runtimeStatus": {
    "currentNodeId": "task_getDiagnosis",
    "statusMessage": "EXECUTING: task_getDiagnosis",
    "paused": false,
    "variables": {
      "patientId": "hc-patient-001",
      "visitId": "VISIT-1741694400000",
      "severity": "ABNORMAL"
    }
  }
}
```

**成功响应（COMPLETED 状态，无 runtimeStatus）：**

```json
{
  "workflowId": "hc-patient-002",
  "runId": "e5f6g7h8-...",
  "workflowType": "OrchestratorWorkflow",
  "status": "COMPLETED",
  "taskQueue": "orchestrator-task-queue",
  "startTime": "2026-03-11T09:00:00Z",
  "closeTime": "2026-03-11T09:05:00Z",
  "historyLength": 277,
  "pendingActivities": 0
}
```

---

### 3. 暂停 Workflow

向指定 workflow 发送 `pause` signal，workflow 在当前节点执行完毕后暂停，不再进入下一个节点，直到收到 resume。

```
POST /api/workflows/{workflowId}/pause
```

**路径参数：**

| 参数 | 说明 |
|------|------|
| `workflowId` | Workflow ID |

**请求体：** 无

**成功响应：** `200 OK`

```json
{
  "workflowId": "hc-patient-001",
  "action": "PAUSED"
}
```

> 暂停后，`GET /api/workflows/{workflowId}` 的 `runtimeStatus.paused` 将变为 `true`，`statusMessage` 变为 `PAUSED at {nodeId}`。

---

### 4. 恢复 Workflow

```
POST /api/workflows/{workflowId}/resume
```

**路径参数：**

| 参数 | 说明 |
|------|------|
| `workflowId` | Workflow ID |

**请求体：** 无

**成功响应：** `200 OK`

```json
{
  "workflowId": "hc-patient-001",
  "action": "RESUMED"
}
```

---

### 5. 终止 Workflow

强制中止 workflow，不可撤销。状态变为 `TERMINATED`。

```
POST /api/workflows/{workflowId}/terminate
```

**路径参数：**

| 参数 | 说明 |
|------|------|
| `workflowId` | Workflow ID |

**Query 参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `reason` | String | 否 | 终止原因，记录在 Temporal 历史中，默认 `Terminated via workflow-admin` |

**示例：** `POST /api/workflows/hc-patient-001/terminate?reason=Manual+cancel+by+admin`

**成功响应：** `200 OK`

```json
{
  "workflowId": "hc-patient-001",
  "action": "TERMINATED"
}
```

---

## 接口汇总

| Method | Path | 说明 |
|--------|------|------|
| `GET` | `/api/workflows` | 列表（支持 TQL 过滤、分页） |
| `GET` | `/api/workflows/{workflowId}` | 详情（RUNNING 时附带运行时变量） |
| `POST` | `/api/workflows/{workflowId}/pause` | 暂停 |
| `POST` | `/api/workflows/{workflowId}/resume` | 恢复 |
| `POST` | `/api/workflows/{workflowId}/terminate` | 终止 |

## Notes

- pause/resume 通过 Temporal Signal 实现，workflow 在**节点间**暂停（当前节点会正常完成），不会中断正在执行的 Activity
- terminate 是立即强制终止，正在执行的 Activity 会被取消
- `workflowId` 不唯一对应一次运行（同一 ID 可重跑），`runId` 才是每次运行的唯一标识；本 API 默认操作最新一次运行
- TQL 中状态值大小写敏感：`'Running'` 而非 `'RUNNING'`
