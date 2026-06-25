## Overview

Plugin providers are registered through `PluginContext` rather than directly mutating global provider state. The context records ownership so lifecycle cleanup can remove provider instances and extension metadata together.

## API

`PluginContext` adds:

- `registerProvider(provider: ChatProvider)`
- `registeredProviders(): List<String>`

`DefaultPluginContext` registers the provider with `ProviderController`, stores the provider name under the plugin id, and registers extension metadata of type `provider`.

## Provider Cleanup

Plugin lifecycle cleanup unregisters provider names owned by the plugin. Cleanup occurs on:

- disable
- unload
- reload
- lifecycle failure

Before registering a provider, the context unregisters the same name to keep repeated enable calls idempotent.

## Registry Unregister

`ProviderRegistry` gains `unregister(name)` so future plugin provider factories can be cleaned up. This change primarily registers live provider instances through `ProviderController`, but removing the global registry hook now prevents leaks as plugin provider factories are added later.

## Non-Goals

- No plugin marketplace provider package resolution.
- No non-chat provider types yet.
- No automatic config file mutation for plugin providers.
