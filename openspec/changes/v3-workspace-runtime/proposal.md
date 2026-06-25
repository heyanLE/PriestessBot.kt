## Why

v3 needs a runtime scope that can change Agent behavior without restarting the process or mixing unrelated operational contexts. The current global configuration and partial reload flows do not provide an atomic workspace snapshot across skills, MCP servers, tools, agents/personas, memory policy, and provider selection.

## What Changes

- Introduce `workspace-runtime` as a new capability for workspace configuration, immutable runtime snapshots, resolution, reload planning, atomic snapshot replacement, rollback on failure, and reload status tracking.
- Add workspace-scoped behavior for skills, MCP tools, built-in/plugin tools, agents/personas, memory policy, and provider selection.
- Extend pipeline behavior so each accepted message resolves exactly one workspace snapshot at pipeline entry and keeps using that snapshot until processing finishes.
- Extend config hot reload so workspace config sources can be explicitly reloaded and optionally watched, while preserving existing config backup/restore behavior.
- Add Dashboard API endpoints and frontend status views for listing workspaces, inspecting active snapshots, triggering reloads, and viewing scoped skill/MCP/tool/persona/memory details.
- Require reload operations to build candidate snapshots before publication, keep old snapshots on failure, and replace active snapshots atomically only after successful validation and resource initialization.

## Capabilities

### New Capabilities
- `workspace-runtime`: Defines workspace config, runtime snapshots, workspace resolution, reload plans, atomic replacement, rollback, and operational status.

### Modified Capabilities
- `config-hot-reload`: Adds workspace config source reload and publication semantics.
- `pipeline`: Resolves and pins a workspace snapshot per in-flight message.
- `tool-mcp`: Exposes workspace-scoped tool views and reload-safe MCP server lifecycle handling.
- `skill-management`: Provides workspace-scoped skill selection and reload visibility.
- `sub-agent-orchestration`: Runs agent/persona/provider selection within the resolved workspace.
- `knowledge-rag`: Applies workspace memory policy to knowledge and memory access.
- `dashboard-api`: Adds workspace management and inspection endpoints.
- `dashboard-frontend`: Adds workspace status, reload, and scoped resource views.

## Impact

- Runtime model additions: `WorkspaceConfig`, `WorkspaceSnapshot`, `WorkspaceStatus`, `WorkspaceReloadPlan`, `WorkspaceReloadResult`, and `WorkspaceController`.
- Pipeline context changes: each message carries the resolved snapshot and downstream runtime components consume scoped resources from it.
- Tool/MCP/Skill controllers gain workspace-aware read views without removing existing global registration mechanisms.
- Dashboard backend and frontend gain workspace routes, typed client methods, navigation, status pages, and reload action handling.
- Tests must cover reload success, reload failure rollback, atomic replacement, concurrent in-flight message isolation, MCP lifecycle rollback, and Dashboard API/status rendering.
