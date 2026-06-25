# Provider Hot Config Refresh

## Why

Dashboard and config reload can publish provider config changes, but `ProviderController` currently materializes providers only when constructed. Operators must restart the process before new provider definitions, model names, API URLs, or enabled flags affect Agent execution.

## What Changes

- Rebuild config-backed provider instances when provider config is updated or reloaded.
- Preserve plugin/runtime-registered providers separately so plugin lifecycle remains explicit.
- Add tests for adding, disabling, and preserving plugin providers across config refresh.

## Impact

- Provider config changes affect later Agent and pipeline executions without restart.
- In-flight provider calls keep using the provider instance already selected.
- Plugin providers remain managed by plugin enable/disable/unload.
