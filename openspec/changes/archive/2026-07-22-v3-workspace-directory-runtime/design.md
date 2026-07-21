## Context

The current runtime treats a workspace as a serialized config scope loaded from `PriestessConfig.workspaces`, then resolved into immutable snapshots held by `WorkspaceController`. That model already gives good runtime isolation, but its source of truth is wrong for the next phase of the product: skills are expected to live as `SKILL.md`-backed directories, MCP servers are better represented as external declarations, and operators need to move or version a workspace as a self-contained directory.

This change keeps the workspace module's role as the runtime boundary for pinned message execution, but changes how snapshots are built. Instead of reloading a global list of workspace configs, the pipeline resolves one effective workspace directory for each message, reads that directory at preparation time, and builds a metadata-first snapshot. The design must keep code readable, avoid partial runtime state, and leave room for future MCP tool wrapping without forcing it into this change.

## Goals / Non-Goals

**Goals:**
- Redefine workspace semantics as a directory rooted at a real filesystem path.
- Support a directory layout centered on `config.yaml`, `skills/`, and `mcpserver.json`.
- Add a config-layered default workspace directory and allow higher-precedence overrides from `PlatformConfig.config` and message metadata.
- Introduce `PrepareWorkspaceStage` ahead of `PreProcessStage` so workspace resolution happens once per message before agent context construction.
- Build `WorkspaceSnapshot` from the resolved directory at preparation time, storing skill metadata and `SKILL.md` paths instead of eagerly loading prompt bodies.
- Predeclare MCP server metadata in the snapshot without requiring immediate tool wrapping or live client initialization.

**Non-Goals:**
- Deliver a full dashboard or authoring UI for directory-backed workspaces.
- Implement full MCP runtime tool exposure in this change.
- Redesign provider, persona, or memory systems beyond the adjustments needed to consume the new snapshot format.
- Add distributed or remote workspace storage.

## Decisions

### Workspace becomes a directory-backed runtime source

`WorkspaceSnapshot` remains the runtime contract, but its source changes from `PriestessConfig.workspaces` to a resolved workspace directory. The directory is treated as the source of truth and is read at snapshot preparation time.

Alternatives considered:
- Keep `PriestessConfig.workspaces` as the primary source and merely attach directory references. Rejected because it duplicates the source of truth and weakens portability.
- Replace snapshots with direct on-demand reads everywhere. Rejected because it would leak mid-message changes into in-flight execution.

### Workspace directory path uses layered precedence

The runtime resolves the effective workspace directory using three layers, from lowest to highest precedence:
1. Config module default workspace directory.
2. `PlatformConfig.config` extension value such as `workspace_dir`.
3. Message/session metadata such as `workspace_dir`.

The resolved directory path is recorded in pipeline metadata and used by `PrepareWorkspaceStage` to construct the snapshot.

Alternatives considered:
- Resolve only from global config. Rejected because the user explicitly needs platform and message-scoped overrides.
- Let `PreProcessStage` re-resolve the path on demand. Rejected because stage-local resolution risks drift and duplicates logic.

### PrepareWorkspaceStage owns resolution and snapshot pinning

`PrepareWorkspaceStage` is added before `PreProcessStage`. It resolves the effective directory path, reads the workspace directory, validates its structure, builds the snapshot, and pins it onto `PipelineContext`. `PreProcessStage` becomes a consumer of an already-prepared snapshot rather than a resolver.

Alternatives considered:
- Keep workspace preparation inside `PreProcessStage`. Rejected because it mixes infrastructure resolution with agent-context assembly and obscures the pipeline flow.
- Build snapshots only inside `WorkspaceController` startup or explicit reload flows. Rejected because the user requires real-time directory reads during snapshot generation.

### Skills use descriptor-first snapshots and lazy prompt loading

Workspace snapshots store skill descriptors rather than full prompt content. Each descriptor carries stable metadata such as name, description, enabled state, skill directory path, and `SKILL.md` path. `use_skill` or equivalent skill-state loading resolves the markdown lazily and marks that skill as active for later system prompt renders in the same agent run.

Alternatives considered:
- Eagerly read all `SKILL.md` files into the snapshot. Rejected because it increases per-message cost and defeats explicit skill enablement.
- Store only skill names and rescan the directory at every `use_skill`. Rejected because it duplicates discovery work and weakens snapshot consistency.

### MCP is predeclared in snapshots, not eagerly executable

`mcpserver.json` is parsed into snapshot-level MCP declaration objects that preserve server identity, transport, source path, and diagnostics. The change deliberately does not require converting those declarations into executable workspace tools yet.

Alternatives considered:
- Keep the existing eager MCP connection flow during snapshot build. Rejected because the user explicitly wants MCP to follow the same metadata-first direction as skills.
- Remove MCP from the snapshot until tool wrapping exists. Rejected because the user wants the snapshot to pre-embed related objects now.

### Config adds a dedicated default workspace directory field

The Config module gains an explicit default workspace directory field with the same style of layered resolution already used elsewhere in runtime config. Environment override support is retained so deployments can change the effective directory without rewriting the config file.

Alternatives considered:
- Hide the default workspace directory inside `PlatformConfig.config` only. Rejected because the user explicitly wants a Config-level default.
- Reuse `plugins.directory` or another existing path field. Rejected because it conflates unrelated concerns.

## Risks / Trade-offs

- [Risk] Message metadata could point to arbitrary local directories. -> Mitigation: normalize paths, validate existence/type, and consider restricting runtime use to approved roots during implementation.
- [Risk] Reading the workspace directory for each prepared snapshot can add per-message IO overhead. -> Mitigation: keep the snapshot metadata-only for skills and MCP, and use file reads lazily where possible.
- [Risk] YAML parsing introduces a new dependency or a fragile hand-rolled parser. -> Mitigation: add a dedicated YAML dependency or define a deliberately narrow accepted subset and test it thoroughly.
- [Risk] Existing workspace IDs used by personas, memory, or reminders may drift if they derive directly from paths. -> Mitigation: define a stable workspace identity strategy in the runtime model rather than using raw absolute paths as long-term IDs.
- [Risk] The runtime temporarily loses eager MCP availability for workspaces. -> Mitigation: preserve MCP declarations in snapshots so future MCP tool wrapping can attach without redesigning the snapshot contract.

## Migration Plan

1. Add the new config field for default workspace directory and publish it through config layering.
2. Introduce new directory-backed workspace models and parsing helpers for `config.yaml`, `skills/`, and `mcpserver.json`.
3. Add `PrepareWorkspaceStage` and move workspace resolution/pinning out of `PreProcessStage`.
4. Update `WorkspaceSnapshot`, `SkillCase`, and pipeline skill state to support descriptor-first skills with lazy `SKILL.md` loading.
5. Replace eager workspace MCP resolution with snapshot-level declaration metadata and adapt downstream consumers accordingly.
6. Update tests for config layering, workspace preparation precedence, pipeline pinning, lazy skill loading, and MCP declaration parsing.

Rollback strategy: preserve the old config-backed workspace builder behind a compatibility seam until the new directory-backed path is verified. If the new path proves unstable, revert the stage wiring and source selection without changing unrelated runtime modules.

## Open Questions

- Should missing `skills/` or `mcpserver.json` be treated as valid empty inputs or as workspace diagnostics that block snapshot creation?
- What exact field names should be standardized for platform config and message metadata overrides: `workspace_dir`, `workspaceDir`, or both?
- Should the stable workspace identifier come from `config.yaml`, the normalized directory path, or a hybrid rule?
