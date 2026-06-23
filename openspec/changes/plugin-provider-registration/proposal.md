## Why

Plugins can now register executable tools, but v2 also requires third-party model providers. The core provider system must let plugins contribute `ChatProvider` implementations and remove them cleanly when plugin lifecycle changes.

## What Changes

- Extend `PluginContext` with `registerProvider(ChatProvider)`.
- Wire plugin contexts to the shared `ProviderController`.
- Track provider names registered by each plugin.
- Add unregister support to `ProviderController` and `ProviderRegistry`.
- Automatically remove plugin providers when the plugin is disabled, unloaded, reloaded, or fails.
- Ensure Dashboard provider listing includes plugin-provided providers.
- Add tests for plugin provider registration, lookup, connectivity test, text chat, cleanup, and Dashboard visibility.

## Capabilities

### New Capabilities
- `plugin-provider-registration`: Plugins can register executable `ChatProvider` instances into the runtime provider registry.

### Modified Capabilities
- `plugin-runtime-isolation`: Plugin contexts gain safe provider registration APIs.
- `provider`: Runtime provider management includes plugin-provided providers and cleanup.

## Impact

- `PluginManager` gains a dependency on `ProviderController`.
- `ProviderController` and `ProviderRegistry` gain unregister behavior.
- Existing built-in provider behavior remains compatible.
