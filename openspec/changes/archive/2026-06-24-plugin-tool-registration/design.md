## Overview

Plugins should register tools through `PluginContext`, not by reaching into `ToolController` directly. The context records ownership so lifecycle cleanup can be deterministic.

## API

`PluginContext` adds:

- `registerTool(tool: FunctionTool)`
- `registeredTools(): List<String>`

`DefaultPluginContext` registers the tool with `ToolController`, stores the tool name under the plugin id, and registers extension metadata of type `tool`.

## Ownership Tracking

`DefaultPluginContext` owns a mutable set of tool names registered by that plugin. `PluginManager` calls `clearRuntimeContributions(pluginId)` when disabling, unloading, reloading, or handling lifecycle failures. Cleanup unregisters:

- extension metadata for the plugin
- every tool name registered by the plugin context

This keeps plugin tools from leaking into future agent runs.

## Duplicate Handling

Before registering a plugin tool, the context unregisters any existing tool with the same name. This makes repeated enable calls idempotent and avoids duplicate schema entries.

## Non-Goals

- No plugin Provider or Platform registration in this slice.
- No Dashboard UI for plugin tools yet.
- No tool enable/disable policy beyond plugin lifecycle ownership.
