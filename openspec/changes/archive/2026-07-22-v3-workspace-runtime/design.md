## Context

The runtime currently has a global `PriestessConfig`, config reload publication, provider/pipeline partial refresh, plugin-registered tools/providers/platforms, MCP client transports, and skill management primitives. v3 introduces workspace as the operational scope for Agent runtime behavior. A workspace is not a directory; it is a reloadable configuration scope that produces an immutable runtime snapshot containing selected agents/personas, skill instances, MCP tools, tool policy, provider choices, and memory policy.

The change crosses runtime controllers, pipeline context, MCP lifecycle, skill/tool views, memory selection, and Dashboard surfaces. The key constraint is that reloads must never partially mutate active behavior: new messages should see a complete new snapshot after success, failed reloads should keep the old snapshot, and messages already being processed should keep the snapshot they resolved at entry.

## Goals / Non-Goals

**Goals:**
- Define workspace configuration and runtime snapshot models.
- Provide a `WorkspaceController` that loads, validates, builds, publishes, resolves, lists, and reloads workspaces.
- Make snapshot replacement atomic and rollback-safe for skills, MCP servers/tools, tool policies, agent/persona/provider selection, and memory policy.
- Pin each pipeline message to one workspace snapshot for the lifetime of that message.
- Expose Dashboard APIs and frontend views for workspace status, reload actions, scoped resource details, errors, and active snapshot version.
- Preserve existing global registration mechanisms as inputs to workspace-scoped runtime views.

**Non-Goals:**
- Redesign provider, plugin, MCP, skill, or memory implementations beyond the workspace-scoped contracts needed here.
- Add a multi-process distributed configuration system.
- Guarantee that old snapshots are retained forever; retention only needs to cover in-flight messages and diagnostic history.
- Build a full visual workspace editor in this change. Status, inspection, and reload controls are in scope; rich editing can follow separately.

## Decisions

### Workspace snapshots are immutable runtime products

`WorkspaceConfig` is the serializable intent, while `WorkspaceSnapshot` is the immutable runtime product used by messages. A snapshot includes the config, scoped `ToolSet`, resolved skills, MCP-backed tools and clients, personas, memory policy, provider/agent selection metadata, `version`, `loadedAt`, and diagnostic summaries.

Alternatives considered:
- Read live config from each controller on demand. Rejected because it makes partial reload states observable and makes in-flight isolation hard.
- Store only IDs in the snapshot and resolve everything lazily. Rejected for MCP/tools/skills because failures would move from reload time into message processing.

### Reload uses a two-phase build and publish flow

`reload(id)` reads config, validates it, builds all candidate runtime resources, computes a `WorkspaceReloadPlan`, then atomically swaps the active snapshot reference only after the candidate is complete. If validation or resource initialization fails, the active snapshot is not replaced and the reload result records the failure. Old MCP connections and other closeable resources are closed only after the new snapshot is published, with a grace mechanism so in-flight messages can finish.

Alternatives considered:
- Mutate existing snapshot components in place. Rejected because partial mutation can affect running messages.
- Stop all message processing during reload. Rejected because hot reload should be operationally smooth and failures should not interrupt in-flight work.

### Pipeline resolves workspace once at entry

The earliest pipeline stage that has enough session/platform/metadata context resolves a workspace snapshot and stores it on `PipelineContext`. Downstream stages create `AgentContext`, tool executors, skill prompts, personas, and memory access from that pinned snapshot. Later reloads affect only later messages.

Alternatives considered:
- Resolve workspace separately in each stage. Rejected because a reload between stages could mix different snapshots in one message.
- Resolve only inside `ProcessStage`. Rejected because pre-processing needs workspace-scoped skills, persona, and memory.

### Global registries remain sources, workspace views filter them

Plugin and built-in tools/providers/platforms continue registering globally. The workspace runtime builds scoped views by applying workspace configuration and policy to global registries and MCP clients. This avoids duplicating plugin lifecycle while allowing each workspace to expose a different tool set.

Alternatives considered:
- Instantiate separate plugin registries per workspace. Rejected for this change due to lifecycle complexity and higher memory/process cost.

### Workspace config source is explicit and extensible

`WorkspaceConfigSource` abstracts workspace config loading from the main config, workspace directory files, or future remote sources. v3 requires explicit reload and may support optional file watching; source implementations must return complete workspace config sets and validation diagnostics without publishing partial state.

Alternatives considered:
- Put all workspace config directly under `PriestessConfig` only. Rejected because the roadmap allows directory or remote sources, and hot reload should not be coupled to one persistence shape.

### Dashboard exposes operational status first

Dashboard APIs return workspace status, active snapshot version, reload result, scoped resource summaries, and error details. Reload endpoints are action-oriented and return `WorkspaceReloadResult`. Frontend scope is a status and inspection view with reload controls, not a full config authoring flow.

Alternatives considered:
- Expose only raw config through existing `/api/config`. Rejected because operators need runtime status, active snapshot versions, and reload diagnostics that are not visible in config alone.

## Risks / Trade-offs

- [Risk] MCP reload can leak clients if old snapshots are retained for in-flight messages. -> Mitigation: make snapshots closeable, track active snapshot leases or use bounded grace cleanup, and test release after message completion.
- [Risk] Workspace resolution rules can become ambiguous across platform/session/user metadata. -> Mitigation: centralize resolution in `WorkspaceController.resolve(context)` and return diagnostics for the matched rule and fallback.
- [Risk] Reloading all workspaces may be expensive when MCP servers or skill initialization is slow. -> Mitigation: reload per workspace independently, report per-workspace results, and avoid publishing partial candidates.
- [Risk] Dashboard might expose secrets through scoped config details. -> Mitigation: return summaries and redacted config fields; never include provider API keys, platform tokens, MCP env secrets, or memory credentials.
- [Risk] Existing code may assume a global tool set or skill list. -> Mitigation: introduce workspace-scoped accessors while keeping backward-compatible global defaults for callers not yet migrated.

## Migration Plan

1. Add workspace data models, config source abstraction, snapshot builder, reload result/status types, and controller tests.
2. Wire `WorkspaceController` into application startup with a default workspace derived from existing config so current deployments continue to run.
3. Resolve and pin snapshots in pipeline context, then migrate downstream tool, skill, agent/persona, provider, and memory access to use the pinned snapshot.
4. Add MCP candidate initialization and old-client cleanup around snapshot publication.
5. Add Dashboard API routes and frontend status view.
6. Add end-to-end tests for successful reload, failed reload rollback, in-flight isolation, MCP failure rollback, and Dashboard status/reload behavior.

Rollback strategy: keep the old global runtime path available behind the default workspace compatibility layer until workspace resolution is fully stable. If a workspace reload fails in production, the controller keeps the last active snapshot and reports the failure through status APIs.

## Open Questions

- What exact workspace resolution priority should be used when platform, session, user, group, and explicit metadata all provide workspace hints?
- Should file watching be enabled by default or remain opt-in for v3?
- How long should old snapshots be retained for diagnostics after in-flight messages finish?
- Should Dashboard workspace editing be a separate change after status/reload inspection lands?
