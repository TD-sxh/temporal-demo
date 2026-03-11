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
