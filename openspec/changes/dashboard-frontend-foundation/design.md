# Design

## Frontend Shape

The Dashboard lives in a separate `dashboard/` folder to keep frontend tooling isolated from the Kotlin runtime. It uses Vue 3 Composition API, Vue Router, Pinia, TypeScript, and native `fetch`/`WebSocket` wrappers. The first foundation focuses on operational readability rather than a decorative landing page.

The UI is organized around:

- `AppLayout`: persistent sidebar and top status bar.
- Pinia stores for server state and logs.
- Route-level views that fetch the matching API resource on mount and after actions.
- Small local components for status dots, summary metrics, and empty states.

## Backend Serving

Ktor serves `dashboard/index.html` and static resources from classpath resources after the frontend is built into `src/main/resources/dashboard`. API routes keep their `/api` prefix and `/ws/logs` remains the log socket.

The SPA fallback returns `dashboard/index.html` for non-API, non-WebSocket paths so browser refresh works on nested routes.

## Build Integration

Regular Kotlin builds do not automatically run npm. A Gradle `npmBuildDashboard` task runs `npm install` and `npm run build` when `-PbuildDashboard=true` is provided, then `processResources` includes `dashboard/dist` into `dashboard/`.

This keeps local backend test runs fast while allowing NAS/distribution builds to include the UI explicitly.
