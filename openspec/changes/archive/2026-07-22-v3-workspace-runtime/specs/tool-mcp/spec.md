## MODIFIED Requirements

### Requirement: ToolSet with format conversion
The system SHALL provide `ToolSet` that collects tools, supports workspace-scoped views, and converts them to OpenAI tool-calling format.

#### Scenario: Workspace tool view filters tools
- **GIVEN** global built-in and plugin tools are registered
- **AND** a workspace tool policy enables only a subset
- **WHEN** the workspace snapshot builds its `ToolSet`
- **THEN** only tools allowed by that workspace policy are included

#### Scenario: Tool permission changes affect new messages
- **GIVEN** a workspace reload publishes a snapshot with changed tool policy
- **WHEN** a later message creates its tool executor from that workspace snapshot
- **THEN** the changed tool permissions are applied immediately for that message

### Requirement: McpClient with three transport types
The system SHALL implement `McpClient` supporting stdio, SSE, and streamable HTTP transports, and SHALL support workspace-scoped MCP client lifecycle during reload.

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

### Requirement: Tool runtime listing
The system SHALL expose registered tool metadata through management APIs, including workspace-scoped tool metadata.

#### Scenario: Workspace tool metadata exposed
- **GIVEN** a workspace has an active snapshot
- **WHEN** a dashboard client requests tools for that workspace
- **THEN** the response includes the scoped built-in, plugin, and MCP tool metadata visible in that workspace

