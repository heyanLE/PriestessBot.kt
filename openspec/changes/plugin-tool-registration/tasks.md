## 1. OpenSpec

- [x] 1.1 Create plugin tool registration proposal, design, specs, and tasks.

## 2. Runtime API

- [x] 2.1 Extend `PluginContext` with tool registration APIs.
- [x] 2.2 Wire `DefaultPluginContext` to `ToolController`.
- [x] 2.3 Track plugin-owned tool names for cleanup.

## 3. Lifecycle Cleanup

- [x] 3.1 Unregister plugin tools on disable.
- [x] 3.2 Unregister plugin tools on unload and reload.
- [x] 3.3 Unregister plugin tools after lifecycle failures.
- [x] 3.4 Make repeated plugin enable/register idempotent for tool names.

## 4. API Visibility

- [x] 4.1 Ensure Dashboard tool listing includes plugin tools.

## 5. Tests

- [x] 5.1 Add plugin runtime test for registering and executing a plugin-provided tool.
- [x] 5.2 Add tests for disable/unload/failure cleanup.
- [x] 5.3 Add Dashboard route test showing plugin tools in `/api/tools`.

## 6. Verification

- [x] 6.1 Run targeted plugin/server tests.
- [x] 6.2 Run the full test suite.
- [x] 6.3 Run OpenSpec strict validation/status checks.
