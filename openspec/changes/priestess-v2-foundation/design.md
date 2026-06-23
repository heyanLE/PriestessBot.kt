## Overview

This change establishes the v2 runtime foundation while keeping the v1 bot core intact. The server is an optional Ktor layer created in the same process and wired to existing controllers through Koin. Management endpoints expose typed snapshots and simple commands, while deeper v2 systems can extend the same boundaries later.

## Runtime Shape

The process owns one Koin runtime and can start:

- bot controllers, including platform listeners and pipeline processing
- a Dashboard API server when enabled by config or environment
- a plugin manager foundation that discovers local plugin descriptors and tracks lifecycle state

Shutdown flows through the existing controller `stop()` methods and the Ktor engine stop hook.

## Dashboard API

The API uses Ktor 3.x with JSON serialization. Initial endpoints are intentionally small but stable:

- `GET /health`: health summary with database/config/server status
- `GET /api/config`: current `PriestessConfig`
- `PUT /api/config`: replace config, persist it, and publish updated flows
- `POST /api/config/reload`: reload config from disk
- `GET /api/platforms`: configured/running platform status
- `POST /api/platforms/{name}/start` and `/stop`: enable or disable platform config
- `GET /api/providers`: provider metadata
- `POST /api/providers/test`: connectivity test map
- `GET /api/tools`: built-in and registered tool metadata
- `GET /api/conversations`: conversation summaries
- `GET /api/conversations/{id}/messages`: message history
- `GET /api/plugins`: plugin descriptors and lifecycle state
- `POST /api/plugins/{id}/enable` and `/disable`: lifecycle transitions
- `GET /ws/logs`: WebSocket stream foundation

Route handlers stay thin and delegate to service classes so future auth, audit, and validation can be added without rewriting routing.

## Config Hot Reload

`ConfigController` already publishes focused `StateFlow` values. v2 extends it with:

- `reload()`: parse the configured file, publish all config slices, and return the active config
- API writes using existing `update()` + `save()` semantics
- an optional polling file watcher for production config-file edits

Consumers that already collect config flows, such as `PlatformController`, can react without extra coupling.

## Plugin Lifecycle Foundation

This slice does not attempt full isolated ClassLoader execution. It introduces the stable management model first:

- `PluginManifest`: id, name, version, description, entrypoint, capabilities, dependencies
- `PluginState`: discovered, loaded, enabled, disabled, failed
- `PluginManager`: discover, list, enable, disable, reload metadata
- `PluginExtensionRegistry`: typed registration surface for future platform/provider/tool/skill/stage/runner extensions

Local plugin discovery scans a configured plugin directory for `plugin.json`. This keeps the dashboard and API contract useful while full ClassLoader isolation is implemented later.

## Ops Runtime

Container assets package the Gradle application distribution. Runtime paths are expected to be mounted:

- `/app/config`
- `/app/data`
- `/app/logs`
- `/app/plugins`

The container health check calls `/health`. Compose defaults should work for local/NAS deployments and allow overriding API port and config path.

## Non-Goals

- No full Vue dashboard implementation in this foundation slice.
- No plugin marketplace downloads or dependency resolution yet.
- No isolated plugin ClassLoader execution yet.
- No authentication/authorization yet; routes are intended for trusted local/LAN deployment until auth is added.
- No full RAG, sub-agent orchestration, or external runner implementation yet.

## Risks

- Management routes can expose sensitive config values. This slice keeps behavior simple for local use; auth and secret redaction should follow before public exposure.
- Platform start/stop depends on config-flow behavior. Tests should cover state publication, while real adapter lifecycle remains integration-tested.
- Log WebSocket starts as an event stream foundation; deep Logback appender integration can be layered later.
