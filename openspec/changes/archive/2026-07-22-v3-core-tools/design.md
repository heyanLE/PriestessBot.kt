## Context

The runtime already has `FunctionTool`, `ToolSchema`, `ToolSet`, `ToolController`, `ToolExecutor`, `AgentToolContext`, several built-in tools, plugin tool registration, and MCP wrappers. v3 now needs the default tool surface to behave more like a complete chat Agent without requiring plugins: inspect available tools, report health, fetch public URLs, search conversation history, recall/save memory, and create reminders.

The change is cross-cutting because permission metadata lives on tool schemas, policy decisions run during execution, built-in tools depend on runtime services, and the Dashboard must show the effective tool state operators care about.

## Goals / Non-Goals

**Goals:**
- Define a stable permission model for built-in, plugin, and MCP tools.
- Add v3 core built-in tools with conservative default enablement.
- Ensure tool calls are bounded by `Agent.toolTimeoutMs`.
- Keep URL fetching, conversation search, memory, reminders, and health output scoped and non-sensitive.
- Surface effective tool status in Dashboard API and ToolView.

**Non-Goals:**
- No shell/code execution, browser automation, arbitrary HTTP request builder, file writing, or group administration tools.
- No full permission editor UI in this change; Dashboard only needs to show policy/status state.
- No replacement of existing plugin or MCP registration architecture.
- No broad redesign of persona memory internals beyond the tool-facing contract.

## Decisions

### Permission metadata belongs on `ToolSchema`

Add `riskLevel`, `requiredCapabilities`, `defaultEnabled`, and `auditLog` to `ToolSchema`. The schema is already the shared contract used by the Agent, Dashboard, plugin tools, and MCP wrappers, so placing metadata there avoids a parallel registry.

Alternatives considered:
- Separate policy registry keyed by tool name. This would make plugin/MCP tools harder to reason about and easier to desynchronize from schemas.
- Hard-code built-in tool risk in `ToolPolicy`. This would not scale to plugins or future MCP wrappers.

### Policy runs in `ToolExecutor`

`ToolExecutor` is the last common path before tool implementation code runs, so it should enforce workspace/agent enablement, risk allowance, required capability availability, optional confirmation state, and audit decisions. Denied calls return structured `ToolResult` errors and still trigger Agent tool end hooks.

Alternatives considered:
- Enforce policy in the Agent loop. This would miss direct executor callers and duplicate checks in tests and management tools.
- Enforce policy inside each tool. This would make permission behavior inconsistent and easy to forget.

### Conservative defaults for unknown tool metadata

Built-in v3 tools declare explicit metadata. Existing plugin and MCP tools without metadata get conservative defaults: not high-risk by label unless explicitly declared, but not silently granted state-write or high-risk privileges. Workspace/agent policy can keep them disabled or require explicit enablement depending on the configured default policy.

Alternatives considered:
- Treat all unknown tools as `SAFE_READ`. That is convenient but unsafe for plugin/MCP tools that can perform side effects.
- Treat all unknown tools as `HIGH_RISK`. That is safer but would break existing benign plugin tools too aggressively.

### Core tool risk levels

Use the roadmap levels:
- `SAFE_READ`: `system_info`, `list_tools`, `health_check`, `knowledge_search`, `conversation_search`, `memory_recall`
- `SESSION_ACTION`: `send_message`, `early_reply`, future reply/image/file session tools
- `EXTERNAL_READ`: `web_search`, `fetch_url`, future `summarize_url`
- `STATE_WRITE`: `memory_save`, `memory_delete`, `create_reminder`, `delete_reminder`
- `HIGH_RISK`: shell/code execution, arbitrary file writes, browser automation, group management, arbitrary HTTP request tools

`list_reminders` is a read tool but depends on reminder storage and scope; it can remain `SAFE_READ` unless implementation chooses a distinct read capability requirement.

### URL fetch is public-web only

`fetch_url` accepts only HTTP(S), resolves and rejects loopback, localhost, link-local, private LAN, multicast, and other non-public addresses before fetching, and enforces redirect, byte, character, and timeout limits. Output is simplified text with title/final URL/status metadata.

Alternatives considered:
- Arbitrary HTTP client tool. This is too broad for v3 defaults and belongs in high-risk plugin space.
- Let the model fetch any URL and rely on network errors. That creates SSRF risk and leaks internal service reachability.

### Memory and reminder tools use service interfaces

The tools should call persona memory and reminder services through interfaces rather than writing storage directly. This keeps scoping, TTL expiry, scheduler delivery, and future storage changes centralized.

Memory and reminder writes are `STATE_WRITE` and audited. Reads are scoped by workspace plus session/user/persona visibility.

### Dashboard shows effective state, not just schema

`GET /api/tools` should include both static schema fields and runtime status: source, owner, risk, capabilities, default enabled, effective enabled, audit flag, and unavailable/status reason. The ToolView can filter and highlight using this response without needing to reproduce policy logic in the frontend.

## Risks / Trade-offs

- [Risk] Existing plugin/MCP tools may lack permission metadata. → Mitigation: assign conservative defaults, include source/owner in listings, and add tests for default policy behavior.
- [Risk] URL fetch SSRF protections can be bypassed through redirects or DNS changes. → Mitigation: validate scheme and resolved address before initial fetch and after each redirect, enforce redirect limit, and block private final targets.
- [Risk] Tool timeouts may not cancel non-cooperative blocking work immediately. → Mitigation: return timeout promptly from `ToolExecutor`, prefer coroutine cancellation for Kotlin tools, and document that implementations must be cancellation-friendly.
- [Risk] Health and search tools can accidentally expose sensitive text. → Mitigation: health output uses counts/status only, conversation search is workspace scoped and bounded, and audit logs avoid full transcripts by default.
- [Risk] Reminder delivery depends on platform/session availability. → Mitigation: store delivery status and failure reason, retry only according to scheduler policy, and avoid duplicate sends after success.

## Migration Plan

1. Extend tool schema/model types with permission fields and serialization defaults.
2. Add `ToolPolicy` and wire it into `ToolExecutor`.
3. Thread `Agent.toolTimeoutMs` from Agent config through the Agent loop into `ToolExecutor`.
4. Implement core tools behind existing service interfaces where available, adding reminder/memory service adapters if needed.
5. Extend Dashboard API models and ToolView rendering.
6. Add targeted tests before broad regression verification.

Rollback is straightforward for runtime code because new tools can be disabled by config/policy. Schema fields should remain backward compatible by providing defaults during deserialization.

## Open Questions

- Should unknown plugin/MCP tools default to disabled until explicitly allowed, or enabled with a conservative risk level for compatibility?
- What exact memory scopes are supported by the persona-memory module at implementation time?
- Should reminder relative-time parsing support only simple durations initially, or natural language phrases as well?
