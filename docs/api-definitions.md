# Workflow Definition API 接口文档

**Base URL:** `/api/definitions`

## 数据模型

### WorkflowDefinitionEntity

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Long | 数据库主键（自增） |
| `type` | String | 工作流类型标识，如 `health-check-flow`（最大128字符） |
| `name` | String | 可读名称（最大256字符） |
| `version` | Integer | 版本号（同一 type 下自增，type+version 唯一） |
| `status` | Enum | 状态：`DRAFT` / `PUBLISHED` / `ARCHIVED` |
| `definitionJson` | String(TEXT) | 完整的工作流 JSON 定义 |
| `description` | String(TEXT) | 可选描述 |
| `createdAt` | DateTime | 创建时间 |
| `updatedAt` | DateTime | 最后更新时间 |

### 状态生命周期

```
DRAFT → PUBLISHED → ARCHIVED
```

- **DRAFT** — 草稿，可编辑/删除，不可被工作流引擎启动
- **PUBLISHED** — 已发布，可被引擎启动（同 type 取最新 PUBLISHED 版本）
- **ARCHIVED** — 已归档，不可再被启动

---

## 接口列表

### 1. 创建工作流定义

创建指定 type 的新版本（初始状态为 DRAFT），版本号自动递增。

```
POST /api/definitions
```

**Request Body:**

```json
{
  "type": "health-check-flow",
  "name": "Health Check Workflow",
  "definitionJson": "{ ... 完整 JSON 定义 ... }",
  "description": "Initial version"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `type` | String | 是 | 工作流类型标识 |
| `name` | String | 否 | 可读名称 |
| `definitionJson` | String / Object | 是 | 工作流 JSON 定义（支持字符串或嵌套 JSON 对象） |
| `description` | String | 否 | 描述信息 |

**成功响应:** `201 Created`

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

**错误响应:** `400 Bad Request`

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

### 2. 查询所有工作流类型（每种取最新版本）

```
GET /api/definitions
```

**成功响应:** `200 OK`

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

### 3. 查询所有类型名称

```
GET /api/definitions/types
```

**成功响应:** `200 OK`

```json
["health-check-flow", "order-flow", "approval-flow"]
```

---

### 4. 查询指定类型的所有版本

```
GET /api/definitions/{type}
```

| 路径参数 | 类型 | 说明 |
|----------|------|------|
| `type` | String | 工作流类型标识 |

**示例:** `GET /api/definitions/health-check-flow`

**成功响应:** `200 OK`（按版本倒序排列）

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

### 5. 查询指定类型的指定版本

```
GET /api/definitions/{type}/{version}
```

| 路径参数 | 类型 | 说明 |
|----------|------|------|
| `type` | String | 工作流类型标识 |
| `version` | Integer | 版本号 |

**示例:** `GET /api/definitions/health-check-flow/2`

**成功响应:** `200 OK`（包含完整 definitionJson）

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
  "definitionJson": "{ ... 完整 JSON ... }"
}
```

**错误响应:** `404 Not Found`（版本不存在时）

---

### 6. 按数据库 ID 查询

```
GET /api/definitions/id/{id}
```

| 路径参数 | 类型 | 说明 |
|----------|------|------|
| `id` | Long | 数据库主键 |

**示例:** `GET /api/definitions/id/2`

**成功响应:** `200 OK`（包含完整 definitionJson，格式同接口 5）

**错误响应:** `404 Not Found`

---

### 7. 更新草稿定义

只允许更新状态为 `DRAFT` 的定义，所有字段均为可选。

```
PUT /api/definitions/{id}
```

| 路径参数 | 类型 | 说明 |
|----------|------|------|
| `id` | Long | 数据库主键 |

**Request Body:**

```json
{
  "name": "Updated Name",
  "definitionJson": "{ ... 更新后的 JSON ... }",
  "description": "Updated description"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | String | 否 | 更新名称 |
| `definitionJson` | String / Object | 否 | 更新 JSON 定义 |
| `description` | String | 否 | 更新描述 |

**成功响应:** `200 OK`

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

**错误响应:**

| 状态码 | 场景 |
|--------|------|
| `400` | `Invalid definitionJson: ...` |
| `404` | 定义不存在 |
| `500` | `Only DRAFT definitions can be updated. Current status: PUBLISHED` |

---

### 8. 发布定义

将 `DRAFT` 状态变更为 `PUBLISHED`，发布后可被工作流引擎使用。

```
POST /api/definitions/{id}/publish
```

| 路径参数 | 类型 | 说明 |
|----------|------|------|
| `id` | Long | 数据库主键 |

**成功响应:** `200 OK`

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

**错误响应:** `500` — `Only DRAFT definitions can be published. Current status: ...`

---

### 9. 归档定义

将定义归档（软删除），归档后不再被引擎用于新的工作流启动。

```
POST /api/definitions/{id}/archive
```

| 路径参数 | 类型 | 说明 |
|----------|------|------|
| `id` | Long | 数据库主键 |

**成功响应:** `200 OK`

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

### 10. 删除草稿定义

永久删除状态为 `DRAFT` 的定义，非 DRAFT 状态无法删除（请使用归档）。

```
DELETE /api/definitions/{id}
```

| 路径参数 | 类型 | 说明 |
|----------|------|------|
| `id` | Long | 数据库主键 |

**成功响应:** `200 OK`

```json
{
  "message": "Deleted successfully",
  "id": "3"
}
```

**错误响应:**

| 状态码 | 场景 |
|--------|------|
| `404` | 定义不存在 |
| `500` | `Only DRAFT definitions can be deleted. Current status: PUBLISHED. Use archive instead.` |

---

## 接口总览

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/definitions` | 创建新版本（DRAFT） |
| `GET` | `/api/definitions` | 列出所有类型（每种最新版本） |
| `GET` | `/api/definitions/types` | 列出所有类型名称 |
| `GET` | `/api/definitions/{type}` | 列出指定类型所有版本 |
| `GET` | `/api/definitions/{type}/{version}` | 查询指定版本（含 definitionJson） |
| `GET` | `/api/definitions/id/{id}` | 按 ID 查询（含 definitionJson） |
| `PUT` | `/api/definitions/{id}` | 更新草稿 |
| `POST` | `/api/definitions/{id}/publish` | 发布草稿 |
| `POST` | `/api/definitions/{id}/archive` | 归档 |
| `DELETE` | `/api/definitions/{id}` | 删除草稿 |

## 备注

- 启动工作流时（`POST /api/engine/start`），引擎优先从 DB 查找最新 `PUBLISHED` 版本，若无则回退到 classpath 下的 `workflows/{type}.json` 文件
- `definitionJson` 支持传入 JSON 字符串或嵌套 JSON 对象，后者会自动序列化
- 列表接口（接口 2、3、4）响应中不包含 `definitionJson`，详情接口（5、6）才包含
- 版本号在同一 type 下自动递增，无需手动指定
