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
The system SHALL provide `ToolSet` that collects tools and converts them to OpenAI tool-calling format.

#### Scenario: Convert tools to OpenAI format
- **WHEN** `toOpenAIFormat()` is called on a ToolSet with 3 tools
- **THEN** a list of 3 JSON objects in OpenAI function-calling format is returned

#### Scenario: Add and remove tools
- **WHEN** a tool is added to or removed from a ToolSet
- **THEN** subsequent format conversions reflect the updated tool list

### Requirement: ToolExecutor resolves and runs tools
The system SHALL provide `ToolExecutor` that matches LLM tool call names to registered tools, validates arguments, and returns results.

#### Scenario: Known tool called
- **WHEN** the LLM requests `web_search` with valid arguments
- **THEN** the executor finds the `WebSearchTool`, invokes it, and returns the result

#### Scenario: Unknown tool called
- **WHEN** the LLM requests a tool not in the ToolSet
- **THEN** an error `ToolResult` is returned

### Requirement: McpClient with three transport types
The system SHALL implement `McpClient` supporting stdio (subprocess stdin/stdout), SSE (HTTP long-lived connection), and streamable HTTP (HTTP POST request-response).

#### Scenario: Stdio transport connects
- **WHEN** an MCP server with stdio configuration is started
- **THEN** a child process is spawned and communication occurs via stdin/stdout

#### Scenario: SSE transport reconnects on disconnect
- **WHEN** an SSE connection drops
- **THEN** the client retries with exponential backoff

#### Scenario: McpTool wraps MCP tools as FunctionTool
- **WHEN** `McpClient.listTools()` returns tool definitions from an MCP server
- **THEN** each is wrapped as an `McpTool` that delegates execution to `McpClient.callTool()`

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
The system SHALL expose registered tool metadata through management APIs.

#### Scenario: Tool metadata exposed
- **WHEN** a dashboard client requests tools
- **THEN** registered tool names, descriptions, and schema metadata are returned

#### Scenario: Plugin tool metadata exposed
- **GIVEN** a plugin has registered a tool
- **WHEN** a dashboard client requests tools
- **THEN** the plugin-provided tool appears in the returned tool metadata