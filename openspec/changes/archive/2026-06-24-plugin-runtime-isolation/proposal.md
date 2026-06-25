## Why

The v2 foundation can discover plugin manifests, but plugins cannot yet execute code or contribute extensions. v2 needs real plugin runtime isolation and lifecycle hooks so third-party capabilities can be loaded, enabled, disabled, and unloaded without turning the core runtime into a monolith.

## What Changes

- Add a `Plugin` runtime interface with `onLoad`, `onEnable`, `onDisable`, and `onUnload` lifecycle hooks.
- Add a `PluginContext` passed to plugin entrypoints for extension metadata registration and runtime access.
- Load plugin classes through one closeable ClassLoader per plugin directory or jar.
- Support plugin artifacts from either a plugin directory containing `plugin.json` plus jar files, or a standalone plugin jar with adjacent manifest.
- Track loaded plugin instances and ClassLoaders separately from discovered metadata.
- Disable and unload plugins by invoking lifecycle hooks, unregistering extensions, and closing the ClassLoader.
- Surface lifecycle failures in `PluginDescriptor.error` without crashing the bot runtime.

## Capabilities

### New Capabilities
- `plugin-runtime-isolation`: Executable plugin runtime with isolated ClassLoader, entrypoint lifecycle, context, and safe unload behavior.

### Modified Capabilities
- `plugin-lifecycle`: Discovery-only plugin lifecycle is extended to load, enable, disable, unload, and reload executable plugin instances.

## Impact

- Adds plugin runtime APIs under `com.heyanle.priestess.bot.plugin`.
- Extends `PluginManager` from metadata-only state transitions to executable lifecycle orchestration.
- Adds tests that dynamically compile a sample plugin jar and validate lifecycle counters, extension registration, and ClassLoader close/unload behavior.
