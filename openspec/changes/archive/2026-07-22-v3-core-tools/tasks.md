## 1. Tool Model And Policy

- [x] 1.1 Extend `ToolSchema` with `riskLevel`, `requiredCapabilities`, `defaultEnabled`, and `auditLog` fields with backward-compatible serialization defaults.
- [x] 1.2 Define `ToolRiskLevel` values `SAFE_READ`, `SESSION_ACTION`, `EXTERNAL_READ`, `STATE_WRITE`, and `HIGH_RISK`.
- [x] 1.3 Define tool capability identifiers for platform/session, network, conversation history, memory, reminders, provider/search, and plugin/MCP availability.
- [x] 1.4 Implement `ToolPolicy` to evaluate workspace enablement, agent risk allowance, required capabilities, optional confirmation state, and audit decision.
- [x] 1.5 Add structured policy denial/error result types for disabled tools, disallowed risk levels, missing capabilities, and unavailable dependencies.
- [x] 1.6 Assign explicit permission metadata to existing built-in tools: `system_info`, `early_reply`, `send_message`, `web_search`, and `knowledge_search`.
- [x] 1.7 Assign conservative default metadata for plugin and MCP tools that do not declare permission fields.

## 2. Tool Executor And Agent Timeout

- [x] 2.1 Wire `ToolPolicy` into `ToolExecutor` so policy is evaluated before invoking a tool implementation.
- [x] 2.2 Ensure policy-denied executions return structured `ToolResult` errors without calling the target tool.
- [x] 2.3 Add timeout support to `ToolExecutor` and return structured timeout results when a tool exceeds the configured duration.
- [x] 2.4 Thread `Agent.toolTimeoutMs` from Agent config through the Agent loop into `ToolExecutor`.
- [x] 2.5 Ensure tool start/end hooks and Dashboard chat events include denied and timed-out tool calls.
- [x] 2.6 Add runtime default timeout behavior when `Agent.toolTimeoutMs` is missing.

## 3. Core Read Tools

- [x] 3.1 Implement `ListToolsTool` with filters for enabled state, source, risk level, and high-risk inclusion.
- [x] 3.2 Implement `HealthCheckTool` using the same non-sensitive component and diagnostic sources as Dashboard health.
- [x] 3.3 Implement `FetchUrlTool` for public HTTP(S) pages with redirect, byte, character, timeout, and content type limits.
- [x] 3.4 Add `FetchUrlTool` protections for localhost, loopback, link-local, private LAN, multicast, non-HTTP(S), and private redirect targets.
- [x] 3.5 Implement structured `fetch_url` errors for blocked target, DNS, TLS, timeout, unsupported content, and HTTP failure cases.
- [x] 3.6 Implement `ConversationSearchTool` with default current-session scope, query/time/role/conversation filters, and bounded results.
- [x] 3.7 Extend conversation history persistence/query services to support workspace-scoped search with snippets and limits.

## 4. Memory Tools

- [x] 4.1 Identify or add the persona-memory service interface used by Agent tools for save, recall, delete, scope checks, and TTL handling.
- [x] 4.2 Implement `MemorySaveTool` with content, memory type, scope, TTL, workspace binding, and returned memory id.
- [x] 4.3 Implement `MemoryRecallTool` with query, scope, limit, expiry filtering, and workspace isolation.
- [x] 4.4 Implement `MemoryDeleteTool` requiring explicit memory id and workspace/scope visibility checks.
- [x] 4.5 Mark memory write/delete tools as `STATE_WRITE` and audited; mark recall as default-enabled `SAFE_READ`.

## 5. Reminder Tools

- [x] 5.1 Add or wire reminder storage/service models for reminder id, text, due time, status, workspace, platform, session, user, and delivery metadata.
- [x] 5.2 Implement absolute and relative due-time parsing using workspace timezone or configured default timezone.
- [x] 5.3 Implement `CreateReminderTool` with workspace/session/user binding and structured validation errors.
- [x] 5.4 Implement `ListRemindersTool` with scope, status, time filters, and bounded results.
- [x] 5.5 Implement `DeleteReminderTool` requiring explicit reminder id and scope visibility checks.
- [x] 5.6 Implement due reminder scheduling/delivery through the bound platform/session with duplicate-send protection and failure status.
- [x] 5.7 Mark reminder create/delete tools as `STATE_WRITE` and audited; mark list as read-only with reminder capability requirement.

## 6. Registration And Runtime Wiring

- [x] 6.1 Register v3 core tools in the default `ToolSet` when their dependencies are available.
- [x] 6.2 Mark dependency-backed tools unavailable or disabled with status reasons when memory, reminder, conversation, network, or health dependencies are missing.
- [x] 6.3 Ensure `list_tools` returns built-in and plugin tools with source, owner, risk, capabilities, default enabled, effective enabled, audit flag, and status reason.
- [x] 6.4 Ensure high-risk tools are not enabled by default in a fresh workspace.

## 7. Dashboard API And Frontend

- [x] 7.1 Extend Dashboard API tool response models with source, owner, risk level, required capabilities, default enabled, effective enabled, audit flag, and status reason.
- [x] 7.2 Align Dashboard `/health` component/diagnostic categories with `health_check` without exposing secrets.
- [x] 7.3 Extend frontend API client types for the new tool metadata fields.
- [x] 7.4 Update ToolView to render tool policy metadata and unavailable/status reasons.
- [x] 7.5 Add ToolView filtering by text query, source, risk level, and enabled state.
- [x] 7.6 Add visual states for high-risk, disabled, and unavailable tools.

## 8. Tests And Verification

- [x] 8.1 Add unit tests for `ToolSchema` serialization defaults and explicit built-in tool metadata.
- [x] 8.2 Add unit tests for `ToolPolicy` allowed, disabled, disallowed risk, missing capability, and audit-decision cases.
- [x] 8.3 Add `ToolExecutor` tests for policy denial, timeout result, unknown tool behavior, and hook emission.
- [x] 8.4 Add tests for `list_tools` including plugin tools, filtering, enabled state, and high-risk exclusion.
- [x] 8.5 Add tests for `health_check` non-sensitive output and degraded component handling.
- [x] 8.6 Add tests for `fetch_url` success, truncation, timeout, structured failures, and private-address blocking.
- [x] 8.7 Add tests for `conversation_search` current-session default, filters, limit, and workspace isolation.
- [x] 8.8 Add tests for memory save/recall/delete scope, TTL, explicit id deletion, and permission metadata.
- [x] 8.9 Add tests for reminder create/list/delete parsing, scope binding, due delivery, duplicate-send protection, and permission metadata.
- [x] 8.10 Add Dashboard API tests for tool metadata fields and health alignment.
- [x] 8.11 Add frontend ToolView tests or build/type-check coverage for the new metadata and filters.
- [x] 8.12 Run targeted Gradle tests, frontend build/type-check, and `openspec validate v3-core-tools --strict`.
