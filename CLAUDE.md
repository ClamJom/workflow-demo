# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

**Backend (Spring Boot 3.5.11 / Java 21 / Maven):**
```bash
./mvnw install          # install dependencies
./mvnw package          # build JAR
./mvnw spring-boot:run  # run dev server
./mvnw test             # run tests
```

**Frontend (Vue 3 / Vite, in `web/`):**
```bash
cd web
npm install
npm run dev      # dev server with hot reload
npm run build    # production build
```

Requires MySQL and Redis. Configure both in `src/main/resources/application.yaml`.

## Architecture

This is a visual workflow engine: users compose DAG-based flowcharts in a Vue Flow frontend, submit them to the Spring Boot backend, which executes nodes concurrently via virtual threads and streams results back via SSE.

### Core Execution Model

1. **WorkflowHandler** — entry point. Receives `WorkflowVO`, converts to `Workflow` DTO, starts ResultHandler and NodeHandler threads, then exits (non-blocking).
2. **NodeHandler** — walks the graph from the start node. For each node: runs `before()` → `run()` → `after()` hooks, then spawns a new NodeHandler per downstream node for parallel branch execution.
3. **ResultHandler** — polls the result queue and streams results to the client via SSE (`SseHandler`).

### State Management

All state lives in **GlobalPool**, backed by Redis. States are bitwise hex flags — check with `&` operator, never equality:
```java
(state & WorkflowStates.RUNNING) != 0  // correct
state == WorkflowStates.RUNNING         // wrong
```
State classes: `WorkflowStates`, `NodeStates`, `ResultHandlerStates`. `-1` is reserved NULL, `0x1` is reserved ERROR. New states must be hex, ordered by execution sequence, and use left-shift where possible.

### Node System

All nodes extend `NodeImpl` (in `utils.workflow.nodes`). Three hooks to override:
- `before()` — load configs, validate, call `onNodeError()` or `onNodeDisabled()` to abort
- `run()` — core logic, write results via `globalPool.pushWorkflowResult()`, write variables to `nodePool.put()` (NOT directly to globalPool)
- `after()` — cleanup, filter results

**To add a new node:**
1. Create class extending `NodeImpl` in appropriate package (`base/`, `collections/list/`, `collections/map/`)
2. Add enum entry in `NodeType` with hex code (use `| NESTABLE_FLAG` if it can contain sub-nodes)
3. Register class in `NodeType.nodeClazzMap` static block
4. Override `getNodeConfigs()` and `getNodeOutputs()` for frontend discovery

**Config types** (see `ConfigTypes`): `NUMBER`, `SLIDER`, `STRING`, `BOOLEAN`, `LIST`, `MAP`, `CONDITION`. Variable injection uses `{{nodeId:variableName}}` syntax in string configs — GlobalPool resolves these at parse time.

### Variable Pool

- **GlobalPool** (Redis): shared across all nodes in a workflow, keyed by workflow token
- **NodePool**: per-node, merged into GlobalPool during `after()` with `nodeId:` prefix on keys
- Never modify state directly — always go through GlobalPool methods

### API

REST base path: `/api/v3`. Key endpoints:
- `POST /workflow/run` — execute workflow (returns SSE emitter)
- `POST /workflow` — save workflow
- `GET /node/types` — list all node types
- `GET /node/{code}/config` — get node config schema
- `GET /node/{code}/output` — get node output variable descriptors

### Frontend

Vue 3 + Vite + Vue Flow (DAG editor) + Ant Design Vue. Node types rendered as custom Vue Flow nodes. SSE client uses `@microsoft/fetch-event-source`. Node.js >= 20.19 required.
