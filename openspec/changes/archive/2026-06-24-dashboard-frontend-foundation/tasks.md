## 1. OpenSpec

- [x] 1.1 Create dashboard frontend foundation proposal, design, specs, and tasks.

## 2. Frontend Project

- [x] 2.1 Add Vue 3 + Vite + TypeScript project files under `dashboard/`.
- [x] 2.2 Add typed Dashboard API client and runtime store.
- [x] 2.3 Add router, layout, sidebar, and top status UI.
- [x] 2.4 Add overview, platform, provider, tool, conversation, plugin, log, and config views.

## 3. Backend Hosting

- [x] 3.1 Add Ktor static resource serving dependency.
- [x] 3.2 Serve Dashboard static assets and SPA fallback from the Ktor server.
- [x] 3.3 Add tests for root, nested SPA route, static asset, and API route behavior.

## 4. Build And Ops

- [x] 4.1 Add optional Gradle task wiring for frontend build/package.
- [x] 4.2 Update NAS deployment to package Dashboard assets.
- [x] 4.3 Document local frontend and packaged Dashboard commands.

## 5. Verification

- [x] 5.1 Run frontend typecheck/build.
- [x] 5.2 Run targeted server tests.
- [x] 5.3 Run full Gradle test suite.
- [x] 5.4 Run OpenSpec strict validation/status checks.
