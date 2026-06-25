## 1. OpenSpec

- [x] 1.1 Create plugin platform registration proposal, design, specs, and tasks.

## 2. Platform Runtime API

- [x] 2.1 Add unregister support to `PlatformRegistry`.
- [x] 2.2 Extend `PluginContext` with platform registration APIs.
- [x] 2.3 Wire `DefaultPluginContext` to `PlatformRegistry`.
- [x] 2.4 Track plugin-owned platform registration names for cleanup.

## 3. Lifecycle Cleanup

- [x] 3.1 Unregister plugin platforms on disable.
- [x] 3.2 Unregister plugin platforms on unload and reload.
- [x] 3.3 Unregister plugin platforms after lifecycle failures.
- [x] 3.4 Make repeated plugin enable/register idempotent for platform names.

## 4. API Visibility

- [x] 4.1 Ensure plugin platform metadata can be listed from `PlatformRegistry`.
- [x] 4.2 Ensure configured plugin platform status can appear in Dashboard platform listing.

## 5. Tests

- [x] 5.1 Add plugin runtime test for registering and creating a plugin-provided platform.
- [x] 5.2 Add tests for platform cleanup on disable/unload/failure.
- [x] 5.3 Add Dashboard route test showing configured plugin platform status.

## 6. Verification

- [x] 6.1 Run targeted plugin/server tests.
- [x] 6.2 Run the full test suite.
- [x] 6.3 Run OpenSpec strict validation/status checks.
