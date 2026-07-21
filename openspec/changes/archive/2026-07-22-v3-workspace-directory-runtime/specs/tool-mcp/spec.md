## MODIFIED Requirements

### Requirement: McpClient with three transport types
The system SHALL implement `McpClient` supporting stdio (subprocess stdin/stdout), SSE (HTTP long-lived connection), and streamable HTTP (HTTP POST request-response), and SHALL allow workspace snapshots to retain MCP server declarations before runtime tool wrapping is enabled.

#### Scenario: Stdio transport connects
- **WHEN** an MCP server with stdio configuration is started
- **THEN** a child process is spawned
- **AND** communication occurs via stdin/stdout

#### Scenario: SSE transport reconnects on disconnect
- **WHEN** an SSE connection drops
- **THEN** the client retries with exponential backoff

#### Scenario: Workspace snapshot stores MCP declarations without eager wrapping
- **WHEN** a workspace snapshot is built from a directory containing `mcpserver.json`
- **THEN** the runtime parses and stores workspace MCP server declarations in the snapshot
- **AND** it does not require wrapping those declarations as executable `FunctionTool` instances during snapshot construction
