## Overview

This change turns the plugin foundation into an executable runtime while keeping the first implementation intentionally conservative. Each plugin gets its own `URLClassLoader`. The loaded entrypoint must implement the core `Plugin` interface, which remains in the parent classloader so plugins can compile against the public API.

## Plugin Layout

Supported layouts:

- Directory plugin:
  - `plugin.json`
  - `lib/*.jar` or `*.jar`
- Jar plugin:
  - `<plugin>.jar`
  - `<plugin>.json` next to it

The manifest `entrypoint` field names the plugin class.

## Lifecycle

The manager owns runtime records:

- `DISCOVERED`: manifest was parsed
- `LOADED`: ClassLoader and plugin instance created, `onLoad` called
- `ENABLED`: `onEnable` called successfully
- `DISABLED`: `onDisable` called successfully; instance may remain loaded
- `FAILED`: lifecycle step failed; error is recorded

`unload(id)` calls `onUnload`, unregisters plugin extensions, closes the ClassLoader, and returns to `DISCOVERED` when metadata is still available.

`reload()` disables/unloads all loaded plugins, clears discovery state, then scans again.

## Plugin Context

`PluginContext` exposes:

- `manifest`
- `pluginPath`
- `registerExtension(type, name, description)`
- `extensions(type)`

This avoids giving plugins direct mutable access to core registries until capability-specific APIs are added.

## ClassLoader Strategy

Use `URLClassLoader(urls, parent)` with the application classloader as parent. This gives dependency separation for plugin jars while sharing public core interfaces. The classloader is closed on unload to release jar file handles.

## Error Handling

Lifecycle exceptions are caught by the manager. The descriptor moves to `FAILED`, the error message is retained, and the runtime keeps operating. Disable/unload should be best effort so a partially loaded plugin can be cleaned up.

## Non-Goals

- No plugin marketplace dependency resolution in this slice.
- No child-first classloading yet.
- No security sandboxing beyond ClassLoader isolation.
- No direct registration of executable Platform/Provider/Tool instances yet; this slice registers extension metadata only.
