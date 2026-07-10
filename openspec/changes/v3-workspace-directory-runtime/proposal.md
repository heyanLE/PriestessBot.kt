## Why

The current workspace runtime models a workspace as an in-memory configuration scope loaded from `PriestessConfig.workspaces`, which makes it hard to organize workspace assets as a portable directory and to align runtime behavior with file-based skill and MCP conventions. We need a directory-backed workspace model now so that skills, MCP server declarations, and workspace-local configuration can be managed together and loaded with clearer boundaries.

## What Changes

- Change workspace semantics from a config list entry to a directory-backed runtime scope containing `config.yaml`, `skills/`, and `mcpserver.json`.
- Add a default workspace directory setting to the Config module with layered resolution behavior, and allow later overrides from platform config extensions and message metadata.
- Introduce a dedicated pipeline stage before `PreProcessStage` to resolve the effective workspace directory and build the pinned `WorkspaceSnapshot`.
- Change workspace snapshot construction to read the target directory at preparation time, load `config.yaml`, and store skill metadata plus `SKILL.md` file paths instead of eagerly loading prompt bodies.
- Predeclare MCP server metadata from `mcpserver.json` in the workspace snapshot without requiring immediate runtime tool wrapping.
- Preserve existing workspace module responsibilities for resolution, snapshot pinning, and downstream consumption while changing the source of truth and loading flow.

## Capabilities

### New Capabilities
- `workspace-runtime`: Directory-backed workspace discovery, snapshot construction, and runtime metadata contracts for skills and MCP declarations.

### Modified Capabilities
- `config-hot-reload`: Runtime config publishes a default workspace directory and related source-layer values used by workspace preparation.
- `pipeline`: The pipeline resolves workspace directories and prepares pinned workspace snapshots in a dedicated stage before preprocessing.
- `skill-management`: Workspace-visible skills are represented as metadata plus `SKILL.md` paths and are loaded lazily when the agent explicitly enables them.
- `tool-mcp`: Workspace MCP behavior shifts from eager tool resolution during snapshot construction to predeclared server metadata that can back future MCP tool/runtime integration.

## Impact

- Affected code spans `config`, `workspace`, `pipeline`, `skill`, and MCP integration modules.
- Runtime models such as `WorkspaceConfig`, `WorkspaceSnapshot`, and pipeline context metadata will change shape.
- A YAML parser will likely be required for `config.yaml` unless the runtime adopts a deliberately constrained parser.
- Existing tests for workspace reload, pipeline pinning, skill loading, and MCP workspace data will need to be updated to reflect directory-backed snapshots and lazy loading behavior.
