# Temporal Health Check Demo

A patient health check workflow demo project built on **Temporal + Spring Boot**, showcasing the core capabilities of the Temporal workflow engine.

## Tech Stack

| Component | Version |
|---|---|
| Java | 17 |
| Spring Boot | 4.0.3 |
| Temporal SDK | 1.24.1 |
| Temporal Server | 1.29.2 |
| PostgreSQL | 12 |

## Business Scenario

Simulates a complete medical health check workflow:

```
Patient Visit → Record Visit → Get Diagnosis → Analyze Severity → Follow-up Loop → Generate Report
                                                    ↑
                                        External events (Signal) can dynamically alter the flow
```

### Workflow Steps

1. **Record Visit** — Record patient basic info (ID, name, doctor, visit reason)
2. **Get Diagnosis Result** — Generate a diagnosis score based on scenario (0.0 ~ 1.0)
3. **Analyze Severity** — Classify severity by score:
   - `NORMAL` (< 0.4) — Normal
   - `ABNORMAL` (0.4 ~ 0.75) — Abnormal
   - `SEVERE` (≥ 0.75) — Severe
4. **Follow-up Loop** — Dynamically determine follow-up count and interval based on severity:

   | Severity | Max Follow-ups | Follow-up Interval (Demo) | Follow-up Interval (Real) |
   |---|---|---|---|
   | NORMAL | 3 | 30 sec | 7 days |
   | ABNORMAL | 5 | 20 sec | 7 days |
   | SEVERE | 7 | 15 sec | 3 days |

5. **Generate Summary Report** — Summarize diagnosis results, follow-up records, and final severity

### Dynamic Behavior

- Health score **changes dynamically** during follow-ups, potentially triggering severity upgrades/downgrades
- When severity changes, follow-up plan is automatically adjusted (count and interval)
- `SEVERE` patients trigger immediate doctor notifications
- Recovery to `NORMAL` ends follow-ups early

## Demonstrated Temporal Features

| Feature | How It's Used |
|---|---|
| **Workflow** | `HealthCheckWorkflow` defines the main flow, orchestrating multiple Activities |
| **Activity** | `HealthCheckActivities` wraps 5 retryable business operations |
| **Signal** | `cancelFollowUp` — external follow-up cancellation; `newLabResult` — inject new lab results |
| **Query** | `getStatus` — real-time workflow status (phase, severity, follow-up progress) |
| **Durable Wait** | `Workflow.await()` for Signal-aware durable sleep |
| **Auto Retry** | Activity auto-retries on failure (max 3 attempts, initial interval 1 sec) |
| **Deterministic Replay** | Worker crash recovery via Event Sourcing |

## Project Structure

```
engine-model/     — Shared model classes (InputParam, etc.)
engine-core/      — Workflow engine core (orchestrator workflow, generic activity)
engine-api/       — REST API + definition management (Spring Boot application)
health-check-worker/ — Health check Activity worker (Spring Boot application)
```

## Quick Start

### Prerequisites

- Java 17+
- Docker & Docker Compose

### 1. Start Temporal Server

```bash
docker compose up -d
```

Wait ~30 seconds, verify services:

```bash
docker compose ps
# Confirm temporal-db, temporal-server, temporal-ui are all running
```

### 2. Build and Run

```bash
./gradlew build
docker compose up -d engine-api engine-core health-check-worker
```

Services:
- **engine-api** — REST API on port `8081`
- **engine-core** — Workflow orchestrator worker
- **health-check-worker** — Activity worker for health check tasks

### 3. Open Temporal UI

Visit http://localhost:8080 in your browser to view workflow list and Event History.

## HTTP API

### Start a Workflow

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

`scenario` options: `normal`, `abnormal`, `severe` (random if omitted).

### Query Status

```bash
curl http://localhost:8081/api/healthcheck/health-check-P001/status
```

Response example:

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

### Send Lab Result (Signal)

```bash
curl -X POST http://localhost:8081/api/healthcheck/health-check-P001/labresult \
  -H "Content-Type: application/json" \
  -d '{"score": 0.9}'
```

`score` range 0.0 ~ 1.0, dynamically changes severity and follow-up plan.

### Cancel Follow-up (Signal)

```bash
curl -X POST http://localhost:8081/api/healthcheck/health-check-P001/cancel \
  -H "Content-Type: application/json" \
  -d '{"reason": "Patient transferred to another hospital"}'
```

## Demo Scripts

### Act 1: Normal Scenario — Quick Completion

```bash
curl -X POST http://localhost:8081/api/healthcheck/start \
  -H "Content-Type: application/json" \
  -d '{"patientId":"P001","scenario":"normal"}'
```

Observe: Severity is `NORMAL`, a few follow-ups then auto-completes.

### Act 2: Severe Scenario — Extended Follow-ups

```bash
curl -X POST http://localhost:8081/api/healthcheck/start \
  -H "Content-Type: application/json" \
  -d '{"patientId":"P002","scenario":"severe"}'
```

Observe: Multiple follow-ups; poll `/status` to watch progress changes.

### Act 3: Signal Changes Condition

```bash
# Start abnormal scenario
curl -X POST http://localhost:8081/api/healthcheck/start \
  -H "Content-Type: application/json" \
  -d '{"patientId":"P003","scenario":"abnormal"}'

# Wait a few seconds, inject worsening lab result
curl -X POST http://localhost:8081/api/healthcheck/health-check-P003/labresult \
  -H "Content-Type: application/json" \
  -d '{"score": 0.9}'

# Then inject improving result
curl -X POST http://localhost:8081/api/healthcheck/health-check-P003/labresult \
  -H "Content-Type: application/json" \
  -d '{"score": 0.1}'
```

Observe: Severity changes `ABNORMAL` → `SEVERE` → `NORMAL`, follow-up plan adjusts dynamically.

### Act 4: External Cancellation

```bash
# Start severe scenario
curl -X POST http://localhost:8081/api/healthcheck/start \
  -H "Content-Type: application/json" \
  -d '{"patientId":"P004","scenario":"severe"}'

# Send cancel signal
curl -X POST http://localhost:8081/api/healthcheck/health-check-P004/cancel \
  -H "Content-Type: application/json" \
  -d '{"reason": "Patient transferred to another hospital"}'
```

Observe: Workflow terminates gracefully, generating a report with the cancellation reason.

### Act 5: Crash Recovery

```bash
# 1. Start a severe scenario
curl -X POST http://localhost:8081/api/healthcheck/start \
  -H "Content-Type: application/json" \
  -d '{"patientId":"P005","scenario":"severe"}'

# 2. Confirm workflow is running
curl http://localhost:8081/api/healthcheck/health-check-P005/status

# 3. Kill the Worker process (simulate crash)
kill -9 $(jps | grep TemporalDemoApplication | awk '{print $1}')

# 4. Check Temporal UI (http://localhost:8080) — workflow stuck in Running state

# 5. Restart the application
./gradlew bootRun

# 6. Workflow automatically resumes from the interruption point
curl http://localhost:8081/api/healthcheck/health-check-P005/status
```

## Core Concepts

### Event Sourcing

Temporal does not persist Workflow in-memory variables. It persists each key event (Activity call/return, Timer, Signal) to the database. State recovery is achieved by **replaying the Workflow code and skipping already completed steps**:

```
Replay: Execute Workflow code from the first line
  recordVisit()      → History has result → Skip, return cached value
  getDiagnosisResult → History has result → Skip, return cached value
  followUpCompleted++ → Pure Java code, execute normally
  performFollowUp()  → History has result → Skip, return cached value
  Workflow.await()   → History has TimerFired → Skip
  performFollowUp()  → No history record → Actually execute
```

### Determinism Constraints

Since Workflow code is replayed on every event trigger, Workflows **must not** contain non-deterministic operations:

| Forbidden | Alternative |
|---|---|
| `new Random()` | Move to Activity |
| `System.currentTimeMillis()` | Use `Workflow.currentTimeMillis()` |
| Direct HTTP/database calls | Move to Activity |
| `Thread.sleep()` | Use `Workflow.sleep()` or `Workflow.await()` |

### Workflow.await() Mechanism

`Workflow.await(timeout, condition)` is not a real thread block:

1. SDK registers a Timer with the Server
2. **Releases the Worker thread** (waiting Workflows consume zero Worker resources)
3. When Timer expires or Signal arrives, Worker picks up a new WorkflowTask
4. Replays Workflow code to the `await()` position, evaluates condition to decide whether to continue waiting or proceed

## Configuration

| Setting | Default | Description |
|---|---|---|
| `server.port` | 8081 | HTTP port (avoids conflict with Temporal UI on 8080) |
| `temporal.server.target` | 127.0.0.1:7233 | Temporal Server gRPC address |

## Cleanup

```bash
docker compose down -v
```
