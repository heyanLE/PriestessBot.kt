## Context

The Agent appends every successful tool result directly to `AgentContext.messages`. `fetch_url` and `web_extract` apply source-level byte and character limits, but their observations can still exceed the active Agent token budget; the same risk applies to terminal, plugin, workspace, and MCP tools. The subsequent context compressor can only discard context and cannot give the Agent a safe way to recover omitted content.

The runtime already has a per-conversation `AgentContext`, `ReActRunner` as the shared tool-result integration point, builtin tool registration, workspace tool policy, and shutdown coordination. It does not currently expose conversation ownership through `AgentToolContext`, and its line-paginated `read_file` tool is not suitable for simplified HTML or arbitrary single-line tool output.

## Goals / Non-Goals

**Goals:**

- Keep provider-bound tool observations within a configured inline token budget while preserving a recoverable full result.
- Apply one policy to successful results from every tool source without changing each tool implementation.
- Let the originating conversation retrieve a bounded Unicode-safe window through a purpose-built safe-read tool.
- Keep overflow data private to its conversation and bounded by expiry, per-result limits, total capacity, and runtime shutdown cleanup.
- Distinguish source truncation reported by a tool from runtime inline-result materialization.

**Non-Goals:**

- Persist results across process restarts, expose them as workspace files, or offer Dashboard download/browsing APIs.
- Summarize, index, search, redact, or transform the full result content.
- Spill failed tool errors, tool-call arguments, prompts, or assistant responses.
- Retrofit pagination into every source tool; source-specific `max_bytes` and `max_chars` controls remain in place.

## Decisions

### 1. Materialize results centrally in the Agent runner

`ReActRunner` SHALL materialize a successful `ToolResult.output` after execution and before it is appended as a `tool` conversation message. It has the tool-call ID, the owning conversation, and every tool source converges there.

This avoids duplicating overflow code in builtins, plugins, workspace tools, and MCP wrappers. Materializing in `ToolExecutor` was rejected because it has no conversation identity and can be used outside an Agent run. Materializing in `ContextManager` was rejected because the large output would already have entered history and because it cannot present a retrieval reference at the point the tool result is generated.

### 2. Use a disk-backed, runtime-scoped overflow store

The store SHALL write complete UTF-8 output into a runtime-managed temporary directory and keep metadata in memory: opaque result ID, owner conversation ID, creation/expiry timestamps, original character and estimated-token counts, and byte size. IDs SHALL be cryptographically unguessable and no result path SHALL be included in an Agent-visible observation.

Disk backing protects heap usage for multi-megabyte output and allows deterministic capacity accounting. An in-memory store was rejected because generic plugin/MCP output can exhaust heap. A workspace-backed store was rejected because external data would become user files and workspace access could cross the intended conversation boundary.

### 3. Replace oversized results with a bounded observation

When estimated output tokens exceed `inlineTokenBudget`, the runner SHALL retain the complete output in the store and append only a preview no larger than `previewTokenBudget`, plus `resultId`, counts, an overflow flag, and retrieval instructions. The preview is a prefix because it gives the Agent immediate context; the store remains the source of truth.

The store configuration SHALL define defaults for inline budget, preview budget, TTL, maximum single result bytes, and maximum total stored bytes. The preview budget SHALL be lower than the inline budget. If the store cannot accept output because of I/O or capacity, the runner SHALL return only the bounded preview and a non-sensitive unavailable notice; it SHALL not append the original oversized output.

### 4. Add an always-visible, safe `read_tool_result` built-in

`read_tool_result` SHALL accept an opaque `result_id`, a zero-based Unicode code-point `offset`, and a bounded code-point `limit`. Its response SHALL include the returned text, the next offset when more content remains, total size metadata, and a truncation flag. The result is deliberately paginated by text position rather than lines because simplified web pages and tool output can be a single line.

The tool SHALL be registered as a `SAFE_READ` core tool and default-enabled, subject to existing workspace policy overrides. Keeping it visible avoids a dead end where an Agent receives a result reference but has no schema that can retrieve it. Dynamic tool injection was rejected for the first version because it complicates workspace-scoped tool snapshots and provider tool schemas for modest token savings.

### 5. Authorize retrieval by conversation identity

`AgentToolContext` SHALL receive the current `conversationId` as an explicit field. `read_tool_result` SHALL resolve only results owned by that ID. Missing, expired, or foreign IDs SHALL use the same non-sensitive not-found response to prevent identifier probing.

Binding only to platform/session was rejected because dashboard and sub-agent runs can have synthetic or missing sessions, while `conversationId` is the Agent's stable access boundary.

### 6. Cleanup is both periodic and deterministic at shutdown

The store SHALL evict expired entries before write/read operations and when enforcing capacity. The runtime shutdown path SHALL close the store and remove its temporary directory. Capacity eviction SHALL remove the oldest eligible entries first; active reads use a snapshot or synchronization so removal cannot return mixed content.

## Risks / Trade-offs

- [A very large result exceeds store capacity] → Return only a bounded preview and an explicit unavailable marker; log non-sensitive diagnostics.
- [Temporary storage contains untrusted or sensitive external content] → Use a runtime-owned directory, opaque IDs, conversation authorization, no path disclosure, TTL, and shutdown deletion.
- [A workspace policy disables the retrieval tool] → Preserve the preview and unavailable guidance; document that a workspace must not disable the default safe reader if full retrieval is required.
- [Estimated tokens differ from the provider tokenizer] → Use the existing estimate consistently for thresholds and keep a configurable safety margin; provider context compression remains the final guard.
- [History persistence stores only the preview] → This is intentional: references are runtime-scoped and expired data must not appear retrievable after restart.

## Migration Plan

1. Add the store, configuration, owner propagation, materializer, and reader while retaining existing source-level limits.
2. Register the reader with default-safe tool visibility and add focused tests for all tool sources through the shared runner path.
3. Deploy with conservative default budgets and monitor non-sensitive overflow/unavailable counts.
4. Roll back by disabling overflow materialization through configuration; tool results then use the existing inline behavior. Removing the store does not require database migration because results are runtime-temporary.

## Open Questions

- Whether initial defaults should reserve a fixed share of `AgentConfig.maxTokens` or use global absolute values.
- Whether non-sensitive overflow count and byte diagnostics should be added to `/health` in this change or a later observability change.
