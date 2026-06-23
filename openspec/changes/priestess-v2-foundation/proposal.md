## Why

v2 needs a stable operational and management foundation before larger features such as the dashboard, plugins, RAG, and sub-agent orchestration can be built safely. The current v1 runtime is usable from code and config files, but it has no first-class HTTP API, health surface, runtime observability, or plugin lifecycle boundary.

## What Changes

- Add a Ktor-based Dashboard API server that can run with the existing bot runtime.
- Expose REST endpoints for health, config, platforms, providers, tools, conversations, and plugins.
- Add a WebSocket log/event stream foundation for dashboard clients.
- Add configuration hot reload support so persisted config changes can be applied without restarting the process.
- Introduce a plugin lifecycle foundation with manifests, plugin states, registry/manager APIs, and extension registration boundaries.
- Add basic production deployment assets: Dockerfile, compose file, healthcheck, and persistent data/config/log directories.
- Keep v1 interfaces compatible while preparing built-in implementations to become default internal extensions.

## Capabilities

### New Capabilities
- `dashboard-api`: Dashboard-facing HTTP and WebSocket API for runtime management and inspection.
- `config-hot-reload`: Runtime config reload when the config file changes or API writes occur.
- `plugin-lifecycle`: Plugin manifest, discovery, lifecycle state, enable/disable, and extension registry foundation.
- `ops-runtime`: Production-oriented health checks, container packaging, and persistent runtime directories.

### Modified Capabilities
- `core-infrastructure`: The application runtime gains an optional server mode and coordinated shutdown lifecycle.
- `platform-abstraction`: Runtime platform status and start/stop management are exposed to the Dashboard API.
- `provider`: Provider listing and connectivity testing are exposed to the Dashboard API.
- `tool-mcp`: Tool listing is exposed to the Dashboard API as a foundation for future tool toggles and MCP management.
- `conversation-management`: Conversation and message history are exposed to the Dashboard API.

## Impact

- Adds Ktor server dependencies and a server package under the main Kotlin source tree.
- Extends configuration handling with reload/update semantics that existing controllers can observe.
- Adds plugin domain classes without requiring third-party plugin loading to be complete in this slice.
- Adds Docker and compose deployment files for local/NAS/server operation.
- Adds focused tests for config reload, API routes, plugin lifecycle, and health behavior.
