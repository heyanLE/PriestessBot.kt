# core-tools Specification

## Purpose
TBD - created while syncing archived changes. Update Purpose after archive.

## Requirements

### Requirement: Core tool permission model
The system SHALL classify every registered tool with permission metadata used for default enablement, execution policy, audit decisions, and Dashboard visibility.

#### Scenario: Built-in tool schemas include permission metadata
- **WHEN** built-in tools are registered
- **THEN** each tool schema SHALL include a risk level, required capabilities, default enabled flag, and audit log flag

#### Scenario: Permission risk levels are standardized
- **WHEN** a tool schema declares its risk level
- **THEN** the risk level SHALL be one of `SAFE_READ`, `SESSION_ACTION`, `EXTERNAL_READ`, `STATE_WRITE`, or `HIGH_RISK`

#### Scenario: Default safe tools are enabled
- **WHEN** a workspace starts without explicit tool overrides
- **THEN** `system_info`, `list_tools`, `health_check`, `knowledge_search`, `conversation_search`, and `memory_recall` SHALL be enabled by default

#### Scenario: High risk tools are not default enabled
- **WHEN** a high-risk tool is registered
- **THEN** it SHALL NOT be enabled by default

### Requirement: Tool policy enforcement
The system SHALL evaluate a tool policy before executing a tool call.

#### Scenario: Disabled tool is blocked
- **GIVEN** a registered tool is disabled for the workspace or agent
- **WHEN** the Agent requests that tool
- **THEN** execution SHALL be skipped
- **AND** the tool result SHALL contain a structured permission error

#### Scenario: Risk level not allowed is blocked
- **GIVEN** an Agent is not allowed to use a tool's risk level
- **WHEN** the Agent requests that tool
- **THEN** execution SHALL be skipped with a structured permission error

#### Scenario: Missing session capability is blocked
- **GIVEN** a tool requires a platform, session, network, memory, reminder, or conversation capability that is unavailable
- **WHEN** the Agent requests that tool
- **THEN** execution SHALL be skipped with a structured capability error

#### Scenario: Audited tool call emits events
- **GIVEN** a tool schema has audit logging enabled
- **WHEN** the Agent requests that tool
- **THEN** the system SHALL record audit information containing tool name, risk level, caller context, decision, and result status
- **AND** the audit information SHALL NOT include secrets or full message transcripts unless explicitly required by the tool result

### Requirement: List tools built-in tool
The system SHALL provide `list_tools` to return the tools visible to the current Agent context.

#### Scenario: List visible tools
- **WHEN** the Agent calls `list_tools`
- **THEN** the result SHALL include built-in and plugin-registered tools visible to the current workspace and agent
- **AND** each item SHALL include name, description, source, risk level, required capabilities, enabled state, default enabled state, and audit flag

#### Scenario: Filter tool listing
- **WHEN** the Agent calls `list_tools` with filters for enabled state, source, or risk level
- **THEN** the result SHALL include only tools matching those filters

#### Scenario: High risk tools can be hidden
- **WHEN** the Agent calls `list_tools` with high-risk tools excluded
- **THEN** tools with risk level `HIGH_RISK` SHALL be omitted from the result

### Requirement: Health check built-in tool
The system SHALL provide `health_check` to return a non-sensitive runtime health summary aligned with Dashboard health data.

#### Scenario: Health check returns runtime summary
- **WHEN** the Agent calls `health_check`
- **THEN** the result SHALL include overall status, component statuses, timestamp, uptime, and diagnostics for database, providers, platforms, plugins, tools, and workspace reload state

#### Scenario: Health check excludes sensitive data
- **GIVEN** runtime config contains API keys, platform tokens, prompts, or message content
- **WHEN** the Agent calls `health_check`
- **THEN** the result SHALL NOT include those sensitive values

#### Scenario: Health check degrades gracefully
- **GIVEN** one runtime component cannot be inspected
- **WHEN** the Agent calls `health_check`
- **THEN** the result SHALL include a degraded component status instead of failing the whole tool

### Requirement: URL fetch built-in tool
The system SHALL provide `fetch_url` for retrieving public HTTP(S) page content as simplified text.

#### Scenario: Fetch public URL
- **WHEN** the Agent calls `fetch_url` with a valid public HTTP or HTTPS URL
- **THEN** the tool SHALL return status code, final URL, content type, title when available, simplified text, and truncation metadata

#### Scenario: Respect timeout and size limits
- **WHEN** `fetch_url` exceeds its timeout, redirect limit, response byte limit, or output character limit
- **THEN** the tool SHALL stop processing and return a structured error or truncated successful result

#### Scenario: Reject private network targets
- **WHEN** the Agent calls `fetch_url` with localhost, loopback, link-local, private LAN, multicast, file, or non-HTTP(S) targets
- **THEN** the tool SHALL reject the request before fetching content

#### Scenario: Fetch failures are structured
- **WHEN** URL fetching fails because of DNS, TLS, timeout, blocked address, unsupported content, or HTTP error handling
- **THEN** the tool SHALL return a structured error code and human-readable message

### Requirement: Conversation search built-in tool
The system SHALL provide `conversation_search` for scoped retrieval of stored message history.

#### Scenario: Search current session by default
- **GIVEN** the Agent context has a current platform and session
- **WHEN** the Agent calls `conversation_search` without an explicit conversation id
- **THEN** the tool SHALL search only the current workspace and current session conversation

#### Scenario: Search with filters
- **WHEN** the Agent calls `conversation_search` with query text, time range, role, conversation id, or limit
- **THEN** the tool SHALL return matching messages constrained by those filters

#### Scenario: Search does not cross workspace
- **GIVEN** messages exist in multiple workspaces
- **WHEN** the Agent calls `conversation_search`
- **THEN** the tool SHALL NOT return messages from another workspace

#### Scenario: Search result is bounded
- **WHEN** many messages match a conversation search
- **THEN** the tool SHALL return no more than the requested limit or configured maximum limit

### Requirement: Memory built-in tools
The system SHALL provide `memory_save`, `memory_recall`, and `memory_delete` for persona memory integration.

#### Scenario: Save memory with scope and ttl
- **WHEN** the Agent calls `memory_save` with content, memory type, scope, and optional TTL
- **THEN** the tool SHALL persist a memory record bound to the workspace and declared scope
- **AND** the result SHALL include the memory id

#### Scenario: Recall memory by query
- **WHEN** the Agent calls `memory_recall` with a query and limit
- **THEN** the tool SHALL return matching non-expired memories visible to the current workspace and scope

#### Scenario: Delete memory by id
- **WHEN** the Agent calls `memory_delete` with an explicit memory id
- **THEN** the tool SHALL delete only a memory visible to the current workspace and scope

#### Scenario: Memory write tools require state permission
- **WHEN** the Agent requests `memory_save` or `memory_delete`
- **THEN** the tool policy SHALL treat the call as `STATE_WRITE`

### Requirement: Reminder built-in tools
The system SHALL provide `create_reminder`, `list_reminders`, and `delete_reminder` for workspace/session/user-bound reminders and todos.

#### Scenario: Create reminder with absolute time
- **WHEN** the Agent calls `create_reminder` with reminder text and an absolute due time
- **THEN** the tool SHALL create a reminder bound to the current workspace, platform, session, and user when available

#### Scenario: Create reminder with relative time
- **WHEN** the Agent calls `create_reminder` with a relative due time
- **THEN** the tool SHALL resolve the due time using the workspace timezone or configured default timezone

#### Scenario: List reminders by scope
- **WHEN** the Agent calls `list_reminders`
- **THEN** the tool SHALL return reminders visible to the current workspace/session/user scope with optional status and time filters

#### Scenario: Delete reminder by id
- **WHEN** the Agent calls `delete_reminder` with an explicit reminder id
- **THEN** the tool SHALL delete only a reminder visible to the current workspace/session/user scope

#### Scenario: Due reminder is delivered
- **GIVEN** a reminder reaches its due time
- **WHEN** the reminder scheduler processes due reminders
- **THEN** the system SHALL send the reminder through the bound platform/session when available
- **AND** the reminder status SHALL be updated so it is not delivered repeatedly

#### Scenario: Reminder write tools require state permission
- **WHEN** the Agent requests `create_reminder` or `delete_reminder`
- **THEN** the tool policy SHALL treat the call as `STATE_WRITE`
