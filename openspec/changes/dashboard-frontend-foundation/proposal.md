# Dashboard Frontend Foundation

## Why

The v2 backend exposes Dashboard APIs for health, config, platforms, providers, tools, conversations, plugins, and logs, but there is no user-facing management surface yet. Operators still need to inspect raw HTTP responses or local files to manage a running bot.

## What Changes

- Add a Vue 3 + Vite + TypeScript Dashboard frontend under `dashboard/`.
- Provide an app shell with navigation and first-pass pages for overview, platforms, providers, tools, conversations, plugins, logs, and config.
- Integrate the frontend with existing Dashboard REST and WebSocket APIs.
- Serve the built frontend from the Ktor server with SPA fallback.
- Add an optional Gradle packaging hook that builds the frontend when requested.

## Impact

- Adds Node/Vite frontend sources without changing the core bot runtime.
- Adds optional Gradle task wiring controlled by `-PbuildDashboard=true`.
- Adds Ktor static resource serving dependency and route tests.
