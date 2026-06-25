## ADDED Requirements

### Requirement: Tool schema permission metadata
The system SHALL include permission metadata in `ToolSchema` for all built-in, plugin, and MCP-wrapped tools.

#### Scenario: Tool schema exposes policy fields
- **WHEN** a tool schema is inspected
- **THEN** it SHALL expose `riskLevel`, `requiredCapabilities`, `defaultEnabled`, and `auditLog`

#### Scenario: Legacy tool metadata receives safe defaults
- **GIVEN** an existing plugin or MCP tool does not declare permission metadata
- **WHEN** the tool is wrapped or registered
- **THEN** the system SHALL assign conservative defaults that do not silently grant high-risk execution

### Requirement: Tool executor applies policy and timeout
The system SHALL apply tool policy checks and execution timeout inside `ToolExecutor`.

#### Scenario: Policy runs before execution
- **WHEN** a tool call is submitted to `ToolExecutor`
- **THEN** `ToolExecutor` SHALL evaluate the configured tool policy before invoking the tool implementation

#### Scenario: Timeout returns structured result
- **GIVEN** a tool call runs longer than its configured timeout
- **WHEN** the timeout expires
- **THEN** `ToolExecutor` SHALL stop waiting for the tool and return a structured timeout `ToolResult`

#### Scenario: Tool hooks include skipped executions
- **WHEN** a tool call is denied by policy or times out
- **THEN** Agent tool hooks SHALL still receive tool end information with the skipped or timeout status

### Requirement: Built-in tool registry includes v3 core tools
The system SHALL register v3 core tools alongside existing built-in tools.

#### Scenario: Core tools are registered
- **WHEN** the runtime constructs the default `ToolSet`
- **THEN** it SHALL include `list_tools`, `health_check`, `fetch_url`, `conversation_search`, `memory_save`, `memory_recall`, `memory_delete`, `create_reminder`, `list_reminders`, and `delete_reminder` when their dependencies are available

#### Scenario: Missing dependency disables dependent tool
- **GIVEN** a core tool dependency such as memory or reminder storage is unavailable
- **WHEN** the runtime constructs the default `ToolSet`
- **THEN** the dependent tool SHALL be marked unavailable or disabled with a status reason instead of crashing runtime startup
