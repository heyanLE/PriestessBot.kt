## Why

Tools and providers can now be contributed by plugins, but v2 also needs third-party IM platform adapters. Platform registration is the last core extension point needed before built-in adapters can gradually become internal plugins and external adapters can be installed independently.

## What Changes

- Extend `PluginContext` with `registerPlatform(metadata, factory)`.
- Add unregister support to `PlatformRegistry`.
- Track platform registration names owned by each plugin.
- Automatically remove plugin platform registrations when the plugin is disabled, unloaded, reloaded, or fails.
- Ensure Dashboard platform metadata/status can reflect plugin-provided platform types once configured.
- Add tests for plugin platform factory registration, instance creation, cleanup, and Dashboard visibility through platform config.

## Capabilities

### New Capabilities
- `plugin-platform-registration`: Plugins can register executable `Platform` adapter factories into the runtime platform registry.

### Modified Capabilities
- `plugin-runtime-isolation`: Plugin contexts gain safe platform registration APIs.
- `platform-abstraction`: Runtime platform registry supports plugin-provided platform adapter types and unregister cleanup.

## Impact

- `PluginContext` imports platform API types.
- `PlatformRegistry` gains unregister behavior.
- Existing built-in platform behavior remains compatible.
- Running platform hot migration is out of scope; this slice covers registration and future config-driven startup.
