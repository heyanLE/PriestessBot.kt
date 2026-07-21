## 1. Workspace Runtime Core

- [x] 1.1 Add serializable `WorkspaceConfig`, scoped agent/persona/skill/MCP/tool/memory config models, and backward-compatible default workspace mapping from existing config.
- [x] 1.2 Add immutable `WorkspaceSnapshot`, `WorkspaceStatus`, `WorkspaceReloadPlan`, and `WorkspaceReloadResult` models with version, loaded timestamp, diagnostics, and redacted summary fields.
- [x] 1.3 Implement `WorkspaceConfigSource` for current config-backed workspace loading, with extension points for directory/remote sources and optional file watcher integration.
- [x] 1.4 Implement workspace config validation for duplicate ids, disabled/default handling, unknown skill/tool/MCP/persona/agent references, and secret-safe diagnostics.
- [x] 1.5 Implement snapshot building that resolves scoped tools, skills, MCP tools, agents/personas, provider selection, and memory policy before publication.
- [x] 1.6 Implement `WorkspaceController.list`, `get`, `resolve`, `reload`, and `reloadAll`.
- [x] 1.7 Implement atomic snapshot publication, failed reload rollback, reload plan diffing, and last reload status persistence in memory.
- [x] 1.8 Implement old snapshot resource retention and cleanup so closeable MCP clients remain usable for pinned in-flight messages and are closed after release.

## 2. Runtime Integration

- [x] 2.1 Wire `WorkspaceController` into application startup and config reload publication without changing existing non-workspace startup behavior.
- [x] 2.2 Add workspace snapshot fields to `PipelineContext`, including workspace id, snapshot version, and resolution reason.
- [x] 2.3 Update `PreProcessStage` to resolve and pin the workspace snapshot before building system prompt, skills, persona, conversation, and memory context.
- [x] 2.4 Update Agent/sub-agent orchestration to read selected agents, personas, provider choices, and execution limits from the pinned workspace snapshot.
- [x] 2.5 Update tool execution setup to use the pinned workspace `ToolSet` and workspace tool policy.
- [x] 2.6 Update MCP integration so workspace reload initializes candidate MCP clients and preserves old clients on candidate failure.
  - [x] 2.6a Add a workspace MCP candidate resolver seam so reload can resolve MCP tool names before publication, fail without publishing, and close failed candidate handles.
  - [x] 2.6b Expose executable workspace-scoped runtime tools from the pinned snapshot to agent runs without global registration.
  - [x] 2.6c Connect the resolver to real MCP clients/tools.
- [x] 2.7a Add `SkillCase` workspace-scoped skill access so callers can expose only skills from the pinned snapshot and read scoped skill settings.
- [x] 2.7b Update agent execution flow to expose workspace-scoped skill prompt documents and let the LLM load them explicitly through `use_skill`.
- [x] 2.8 Update knowledge/memory access so retrieval and memory writes honor the pinned workspace memory policy.

## 3. Dashboard API and Frontend

- [x] 3.1 Add Dashboard API response models for workspace status, detail, reload result, reload plan summary, and scoped resource summaries with secret redaction.
- [x] 3.2 Add `GET /api/workspaces`, `GET /api/workspaces/{id}`, `POST /api/workspaces/{id}/reload`, and `POST /api/workspaces/reload`.
- [x] 3.3 Add `GET /api/workspaces/{id}/tools`, `/mcp`, `/skills`, `/personas`, and `/memory` endpoints.
- [x] 3.4 Add typed frontend API client methods and TypeScript types for workspace endpoints.
- [x] 3.5 Add Workspaces navigation and `/workspaces` route.
- [x] 3.6 Implement `WorkspaceView` with workspace list, enabled state, active snapshot version, loaded timestamp, reload buttons, last reload status, and error details.
- [x] 3.7 Implement workspace detail panels for scoped skills, MCP servers/tools, built-in/plugin tools, personas, and memory policy summaries.

## 4. Verification

- [x] 4.1 Add unit tests for workspace config validation, default workspace derivation, workspace resolution fallback, and reload plan diffing.
- [x] 4.2 Add tests proving successful reload atomically publishes a new snapshot and later messages use the new version.
- [x] 4.3 Add tests proving validation or MCP initialization failure keeps the old snapshot active and reports failure diagnostics.
- [x] 4.4 Add concurrency tests proving in-flight messages continue using their pinned snapshot while later messages use the reloaded snapshot.
- [x] 4.5a Add tests proving skill scope changes affect new messages after workspace reload.
- [x] 4.5b Add tests proving tool policy, agent/persona/provider selection, and memory policy changes affect new messages only.
- [x] 4.6 Add Dashboard API tests for workspace list/detail/reload/scoped resource endpoints and secret redaction.
- [x] 4.7 Add frontend tests for Workspaces view loading, detail rendering, reload success, reload failure display, and active snapshot visibility.
- [x] 4.8 Run the relevant Kotlin backend tests, frontend tests/build, and OpenSpec validation for `v3-workspace-runtime`.
