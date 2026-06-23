## Why

The plugin runtime can load executable plugins, but plugins still only register passive extension metadata. v2 needs plugins to contribute real tools to the agent runtime so third-party capabilities can be called during ReAct processing.

## What Changes

- Extend `PluginContext` with `registerTool(FunctionTool)`.
- Wire plugin contexts to the shared `ToolController`.
- Track tool names registered by each plugin.
- Automatically unregister plugin tools when a plugin is disabled, unloaded, reloads, or fails during lifecycle handling.
- Prevent duplicate tool registrations from accumulating when plugins are enabled repeatedly.
- Add tests proving plugin-provided tools are visible in `ToolController` and removed on disable/unload.

## Capabilities

### New Capabilities
- `plugin-tool-registration`: Plugins can register executable `FunctionTool` instances into the runtime tool registry.

### Modified Capabilities
- `plugin-runtime-isolation`: Plugin contexts gain access to safe tool registration APIs.
- `tool-mcp`: Tool runtime listing includes plugin-provided tools registered through the shared `ToolController`.

## Impact

- `PluginManager` now depends on `ToolController`.
- `PluginContext` becomes the public plugin API for tool registration.
- Existing plugin lifecycle behavior remains compatible; metadata-only plugins continue to work.
