## 1. OpenSpec

- [x] 1.1 Create plugin runtime isolation proposal, design, specs, and tasks.

## 2. Plugin Runtime API

- [x] 2.1 Add `Plugin` lifecycle interface.
- [x] 2.2 Add `PluginContext` and default context implementation.
- [x] 2.3 Extend plugin descriptors or runtime records to expose load/runtime state safely.

## 3. ClassLoader Runtime

- [x] 3.1 Add plugin artifact URL resolution for directory and jar layouts.
- [x] 3.2 Add closeable per-plugin ClassLoader creation.
- [x] 3.3 Implement load, enable, disable, unload, and reload orchestration in `PluginManager`.
- [x] 3.4 Ensure lifecycle failures set `FAILED` state with error details without crashing the runtime.

## 4. API Surface

- [x] 4.1 Expose load and unload operations through `PluginCase`.
- [x] 4.2 Add Dashboard API plugin load/unload routes.

## 5. Tests

- [x] 5.1 Add tests for successful plugin load/enable/disable/unload using a dynamically compiled plugin jar.
- [x] 5.2 Add tests for lifecycle failure isolation.
- [x] 5.3 Add tests for Dashboard API plugin load/unload routes.

## 6. Verification

- [x] 6.1 Run targeted plugin/server tests.
- [x] 6.2 Run the full test suite.
- [x] 6.3 Run OpenSpec strict validation/status checks.
