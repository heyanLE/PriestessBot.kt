# tool-mcp Specification

## Purpose
TBD - created by archiving change priestess-v1-core. Update Purpose after archive.
## Requirements

### Requirement: FunctionTool abstract base
The system SHALL define `FunctionTool` with a `ToolSchema` definition and an `execute()` method that receives `AgentContext` and named arguments.

#### Scenario: Tool execution
- **WHEN** `execute(context, args)` is called on a registered FunctionTool
- **THEN** a `ToolResult` with success/error status and output is returned

### Requirement: ToolSet with format conversion
The system SHALL provide `ToolSet` that collects tools, supports workspace-scoped views, and converts them to OpenAI tool-calling format.

#### Scenario: Convert tools to OpenAI format
- **WHEN** `toOpenAIFormat()` is called on a ToolSet with 3 tools
- **THEN** a list of 3 JSON objects in OpenAI function-calling format is returned

#### Scenario: Add and remove tools
- **WHEN** a tool is added to or removed from a ToolSet
- **THEN** subsequent format conversions reflect the updated tool list

#### Scenario: Workspace tool view filters tools
- **GIVEN** global built-in and plugin tools are registered
- **AND** a workspace tool policy enables only a subset
- **WHEN** the workspace snapshot builds its `ToolSet`
- **THEN** only tools allowed by that workspace policy are included

#### Scenario: Tool permission changes affect new messages
- **GIVEN** a workspace reload publishes a snapshot with changed tool policy
- **WHEN** a later message creates its tool executor from that workspace snapshot
- **THEN** the changed tool permissions are applied immediately for that message

### Requirement: ToolExecutor resolves and runs tools
The system SHALL provide `ToolExecutor` that matches LLM tool call names to registered tools, validates arguments, and returns results.

#### Scenario: Known tool called
- **WHEN** the LLM requests `web_search` with valid arguments
- **THEN** the executor finds the `WebSearchTool`, invokes it, and returns the result

#### Scenario: Unknown tool called
- **WHEN** the LLM requests a tool not in the ToolSet
- **THEN** an error `ToolResult` is returned

### Requirement: McpClient with three transport types
The system SHALL implement `McpClient` supporting stdio, SSE, and streamable HTTP transports, and SHALL support workspace-scoped MCP client lifecycle during reload.

#### Scenario: Stdio transport connects
- **WHEN** an MCP server with stdio configuration is started
- **THEN** a child process is spawned
- **AND** communication occurs via stdin/stdout

#### Scenario: SSE transport reconnects on disconnect
- **WHEN** an SSE connection drops
- **THEN** the client retries with exponential backoff

#### Scenario: McpTool wraps MCP tools as FunctionTool
- **WHEN** `McpClient.listTools()` returns tool definitions from an MCP server
- **THEN** each is wrapped as an `McpTool` that delegates execution to `McpClient.callTool()`

#### Scenario: Workspace snapshot stores MCP declarations without eager wrapping
- **WHEN** a workspace snapshot is built from a directory containing `mcpserver.json`
- **THEN** the runtime parses and stores workspace MCP server declarations in the snapshot
- **AND** it does not require wrapping those declarations as executable `FunctionTool` instances during snapshot construction

#### Scenario: Workspace MCP tools are exposed through snapshot
- **GIVEN** a workspace config declares enabled MCP servers
- **WHEN** the workspace snapshot is built
- **THEN** the runtime connects to those servers, lists their tools, and exposes them as workspace-scoped `FunctionTool` instances

#### Scenario: MCP reload failure preserves old clients
- **GIVEN** a workspace has active MCP clients in snapshot version `N`
- **WHEN** reload candidate version `N+1` fails to connect to a required MCP server
- **THEN** version `N` remains active
- **AND** the old MCP clients remain usable for in-flight and later messages

#### Scenario: Removed MCP server is closed after safe replacement
- **GIVEN** reload publishes a candidate snapshot that removes an MCP server
- **WHEN** no in-flight message is using the old snapshot
- **THEN** the removed server client is closed

### Requirement: WebSearch built-in tool
The system SHALL provide `WebSearchTool` that performs internet searches via a configured search API.

#### Scenario: Web search invoked
- **WHEN** the Agent calls `web_search` with a query
- **THEN** search results are returned as a formatted string

### Requirement: EarlyReply built-in tool
The system SHALL provide `EarlyReplyTool` that sends a proactive message to the user during the Agent loop.

#### Scenario: Early reply sent during long processing
- **WHEN** the Agent calls `early_reply` with a message like "Please wait..."
- **THEN** the message is immediately sent to the user via the current Platform, and the Agent loop continues

### Requirement: SendMessage built-in tool
The system SHALL provide `SendMessageTool` for proactive message sending when supported by the Platform.

#### Scenario: Proactive message sent
- **WHEN** the Agent calls `send_message` with a target and content
- **THEN** the message is sent if the Platform supports proactive messaging, otherwise returns an error

### Requirement: SystemInfo built-in tool
The system SHALL provide `SystemInfoTool` that returns current system status.

#### Scenario: System info queried
- **WHEN** the Agent calls `system_info`
- **THEN** current Agent state, available tools, and runtime metrics are returned

### Requirement: Knowledge search tool
The system SHALL expose a built-in Agent tool for knowledge retrieval.

#### Scenario: Agent calls knowledge search
- **GIVEN** knowledge chunks are stored
- **WHEN** the `knowledge_search` tool is executed with a query
- **THEN** the tool returns formatted matching snippets

#### Scenario: No knowledge result
- **GIVEN** no stored chunk matches the query
- **WHEN** the `knowledge_search` tool is executed
- **THEN** the tool returns a successful empty-result message

### Requirement: Tool runtime listing
The system SHALL expose registered tool metadata through management APIs, including workspace-scoped tool metadata.

#### Scenario: Tool metadata exposed
- **WHEN** a dashboard client requests tools
- **THEN** registered tool names, descriptions, and schema metadata are returned

#### Scenario: Plugin tool metadata exposed
- **GIVEN** a plugin has registered a tool
- **WHEN** a dashboard client requests tools
- **THEN** the plugin-provided tool appears in the returned tool metadata

#### Scenario: Workspace tool metadata exposed
- **GIVEN** a workspace has an active snapshot
- **WHEN** a dashboard client requests tools for that workspace
- **THEN** the response includes the scoped built-in, plugin, and MCP tool metadata visible in that workspace

### Requirement: Tool permission declaration and visibility
The system SHALL allow each Tool schema to declare a required permission group, defaulting to `OPERATOR`, and SHALL derive an OpenAI tool view from the current sender's group.

#### Scenario: Super-administrator Tool is hidden
- **GIVEN** a Tool requires `SUPER_ADMIN`
- **AND** the sender is not a `SUPER_ADMIN`
- **WHEN** the OpenAI tool view is built
- **THEN** the Tool is absent from that view

#### Scenario: Administrator Tool is described as unavailable to an operator
- **GIVEN** a Tool requires `ADMIN`
- **AND** the sender is an `OPERATOR`
- **WHEN** the OpenAI tool view is built
- **THEN** the Tool remains in the view
- **AND** its description states that the current sender lacks the required permission

### Requirement: Tool permission execution enforcement
The system SHALL enforce a Tool's required permission group in `ToolExecutor` even when that Tool is visible to the model.

#### Scenario: Unauthorized Tool call returns an OpenAI-compatible denial
- **GIVEN** an `OPERATOR` invokes a Tool requiring `ADMIN`
- **WHEN** `ToolExecutor` executes the Tool call
- **THEN** the Tool implementation is not invoked
- **AND** the result has `success = false` and `errorCode = PERMISSION_DENIED`
- **AND** the result identifies the current and required permission groups and contains the configured persona denial wording
- **AND** the Agent loop appends it as the `content` of a `role=tool` message using the original `tool_call_id`

### Requirement: ToolExecutor SHALL have unit test coverage for validation and execution outcomes
`ToolExecutor` SHALL be covered by unit tests for unknown tools, JSON argument parsing, schema validation, tool exceptions, permissions, timeouts, batch execution, partial failures, and metrics.

#### Scenario: Unknown tool is tested
- **WHEN** the LLM requests a tool name that is not registered
- **THEN** a unit test SHALL verify the executor returns an error `ToolResult` and records a failed tool metric

#### Scenario: Invalid arguments are tested
- **WHEN** the LLM sends malformed JSON or arguments that do not match the tool schema
- **THEN** a unit test SHALL verify the executor returns a validation error without invoking the tool

#### Scenario: Tool exception is tested
- **WHEN** a registered tool throws during execution
- **THEN** a unit test SHALL verify the executor converts the exception into an error `ToolResult`

#### Scenario: Permission denial is tested
- **WHEN** a tool is disabled or denied by tool policy
- **THEN** a unit test SHALL verify execution is skipped and a permission-denied result is returned

#### Scenario: Tool timeout is tested
- **WHEN** a tool exceeds its configured timeout
- **THEN** a unit test SHALL verify execution ends with a timeout result and records failure metrics

#### Scenario: Batch partial failure is tested
- **WHEN** a batch contains multiple tool calls and one call fails
- **THEN** a unit test SHALL verify calls are processed in order and successful results are preserved beside failed results

#### Scenario: Tool metrics are tested
- **WHEN** tool execution succeeds, fails, times out, or references an unknown tool
- **THEN** a unit test SHALL verify the metrics registry records the tool name and status without sensitive arguments

### Requirement: Memory built-in tools
The system SHALL expose built-in Agent tools for saving, recalling, and deleting memory records.

#### Scenario: Agent saves memory
- **GIVEN** state-writing tools are enabled for the Agent
- **WHEN** the Agent calls `memory_save` with valid content, type, scope, and required scope keys
- **THEN** a memory record is persisted
- **AND** the tool returns the created memory id

#### Scenario: Memory save validates scope keys
- **GIVEN** the Agent calls `memory_save` for `SESSION` scope without a session id
- **WHEN** the tool executes
- **THEN** the tool returns a validation error
- **AND** no memory record is persisted

#### Scenario: Agent recalls memory
- **GIVEN** eligible memory records exist for the current Agent context
- **WHEN** the Agent calls `memory_recall` with a query and limit
- **THEN** the tool returns matching memory snippets with ids, scores, and match reasons

#### Scenario: Agent deletes memory by id
- **GIVEN** a memory record exists
- **WHEN** the Agent calls `memory_delete` with the exact memory id
- **THEN** the record is deleted or soft-deleted
- **AND** it is excluded from future recall and injection

#### Scenario: Fuzzy memory delete is rejected
- **GIVEN** the Agent calls `memory_delete` with free-form criteria instead of an exact id
- **WHEN** the tool executes
- **THEN** the tool returns a validation error
- **AND** no memory record is deleted

#### Scenario: Memory write tool requires state-write permission
- **GIVEN** state-writing tools are disabled for the Agent
- **WHEN** the Agent calls `memory_save`
- **THEN** tool execution is rejected before persistence changes occur

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
