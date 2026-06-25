## Overview

Plugin platform registration mirrors plugin tools/providers: plugins register through `PluginContext`, the context records ownership, and cleanup unregisters the contributed platform type.

## API

`PluginContext` adds:

- `registerPlatform(metadata: PlatformMetadata, factory: (PlatformConfig?) -> Platform)`
- `registeredPlatforms(): List<String>`

The factory receives the same `PlatformConfig?` shape used by built-in `PlatformRegistry` registrations.

## Cleanup

Plugin lifecycle cleanup unregisters platform names owned by the plugin. Cleanup occurs on disable, unload, reload, and lifecycle failure.

## Runtime Behavior

This slice registers platform adapter factories. If config already references the platform type after registration, `PlatformController` can create it through the existing config flow behavior. If a running platform instance exists and its plugin is disabled, full runtime stop/migration coordination will be handled by a later controller lifecycle change.

## Non-Goals

- No automatic start/stop of running platform instances during plugin unload.
- No marketplace installation.
- No new real third-party platform adapter implementation in this slice.
