## 1. OpenSpec and Architecture

- [x] 1.1 Create v2 foundation OpenSpec artifacts for API, config reload, plugin lifecycle, ops, and modified v1 capabilities.
- [x] 1.2 Update project dependencies for Ktor server, WebSocket, and test support.

## 2. Config and Runtime Lifecycle

- [x] 2.1 Add server/plugin runtime config fields with backward-compatible defaults.
- [x] 2.2 Add explicit config reload and publish support to `ConfigController`.
- [x] 2.3 Add optional config file watcher support.
- [x] 2.4 Update application startup to coordinate bot runtime and optional server lifecycle.

## 3. Dashboard API Server

- [x] 3.1 Implement Ktor server bootstrap and JSON/WebSocket plugins.
- [x] 3.2 Implement health, config, platform, provider, tool, conversation, and plugin routes.
- [x] 3.3 Implement route service/DTO layer to keep route handlers thin.
- [x] 3.4 Add API route tests covering health, config read/write/reload, listings, and WebSocket heartbeat.

## 4. Plugin Lifecycle Foundation

- [x] 4.1 Implement plugin manifest, state, descriptor, and extension metadata models.
- [x] 4.2 Implement local plugin discovery from `plugin.json`.
- [x] 4.3 Implement plugin manager enable/disable/reload operations.
- [x] 4.4 Add unit tests for manifest parsing, discovery, and lifecycle transitions.

## 5. Ops Runtime

- [x] 5.1 Add Dockerfile and docker-compose assets with persistent config/data/log/plugin paths.
- [x] 5.2 Add container healthcheck using `/health`.
- [x] 5.3 Document local/NAS deployment commands for the v2 runtime.

## 6. Verification

- [x] 6.1 Run the full test suite.
- [x] 6.2 Run OpenSpec validation/status checks for the v2 foundation change.
