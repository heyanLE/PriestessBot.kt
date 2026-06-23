## Design

`HealthResponse` gains:

- `uptimeMillis`
- `diagnostics: Map<String, String>`

Diagnostics values remain strings to keep the response display-friendly and avoid a growing typed schema. Initial keys:

- `configPath`
- `databasePath`
- `configuredPlatforms`
- `runningPlatforms`
- `configuredProviders`
- `availableProviders`
- `registeredTools`
- `configuredPlugins`
- `loadedPluginExtensions`

`DashboardService` computes the values from existing controllers and config snapshots. No health endpoint should expose secrets.

The Dashboard overview renders diagnostics as a second grid below components.

## Non-Goals

- No Prometheus metrics endpoint in this change.
- No active provider/platform probing beyond existing in-memory state.
- No database query health check beyond the already constructed controller path.
