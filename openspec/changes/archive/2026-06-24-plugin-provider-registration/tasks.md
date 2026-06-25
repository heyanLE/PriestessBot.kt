## 1. OpenSpec

- [x] 1.1 Create plugin provider registration proposal, design, specs, and tasks.

## 2. Provider Runtime API

- [x] 2.1 Add unregister support to `ProviderController`.
- [x] 2.2 Add unregister support to `ProviderRegistry`.
- [x] 2.3 Extend `PluginContext` with provider registration APIs.
- [x] 2.4 Wire `DefaultPluginContext` to `ProviderController`.
- [x] 2.5 Track plugin-owned provider names for cleanup.

## 3. Lifecycle Cleanup

- [x] 3.1 Unregister plugin providers on disable.
- [x] 3.2 Unregister plugin providers on unload and reload.
- [x] 3.3 Unregister plugin providers after lifecycle failures.
- [x] 3.4 Make repeated plugin enable/register idempotent for provider names.

## 4. API Visibility

- [x] 4.1 Ensure Dashboard provider listing includes plugin providers.
- [x] 4.2 Ensure provider test endpoint includes plugin providers.

## 5. Tests

- [x] 5.1 Add plugin runtime test for registering and using a plugin-provided provider.
- [x] 5.2 Add tests for provider cleanup on disable/unload/failure.
- [x] 5.3 Add Dashboard route test showing plugin providers in `/api/providers` and `/api/providers/test`.

## 6. Verification

- [x] 6.1 Run targeted plugin/server tests.
- [x] 6.2 Run the full test suite.
- [x] 6.3 Run OpenSpec strict validation/status checks.
