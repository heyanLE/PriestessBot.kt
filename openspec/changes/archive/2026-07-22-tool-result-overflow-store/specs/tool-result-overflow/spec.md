## ADDED Requirements

### Requirement: Oversized successful tool results are materialized outside provider context
The system SHALL evaluate every successful tool result in the shared Agent execution path against a configured inline token budget before appending it to provider-bound conversation messages.

#### Scenario: Result remains within inline budget
- **WHEN** a successful tool result is at or below the configured inline token budget
- **THEN** the system SHALL append its original output as the tool observation

#### Scenario: Result exceeds inline budget
- **WHEN** a successful tool result exceeds the configured inline token budget
- **THEN** the system SHALL store the complete output in the runtime overflow store and append only a bounded preview, opaque result ID, original size metadata, and retrieval guidance

#### Scenario: Generic tool sources use the same materialization path
- **WHEN** a built-in, workspace, plugin, or MCP tool returns an oversized successful output
- **THEN** the system SHALL apply the same overflow materialization policy without requiring source-tool-specific changes

#### Scenario: Failed tool results are not spilled
- **WHEN** a tool result is unsuccessful
- **THEN** the system SHALL preserve its error result behavior and SHALL NOT store it as an overflow result

### Requirement: Overflow results are scoped runtime resources
The system SHALL store overflow content in a runtime-managed temporary location with opaque identifiers and metadata bound to the originating conversation.

#### Scenario: Agent-visible result reference hides storage path
- **WHEN** the system materializes an oversized result
- **THEN** the tool observation SHALL expose an opaque result ID and SHALL NOT expose the underlying file-system path

#### Scenario: Store capacity prevents unbounded growth
- **WHEN** accepting a result would exceed configured per-result or total storage capacity
- **THEN** the system SHALL not append the full result inline and SHALL return a bounded preview with a non-sensitive unavailable indication

#### Scenario: Expired result is evicted
- **WHEN** an overflow result has passed its configured TTL
- **THEN** the system SHALL remove it before a subsequent read or capacity check

#### Scenario: Runtime shutdown clears temporary results
- **WHEN** the runtime shuts down
- **THEN** the system SHALL remove overflow result content and metadata from its managed temporary storage

### Requirement: Conversation-scoped result retrieval is paginated and safe
The system SHALL provide `read_tool_result` as a default-enabled `SAFE_READ` tool for retrieving a bounded Unicode-safe window from an overflow result owned by the current conversation.

#### Scenario: Owner reads a bounded window
- **GIVEN** an overflow result belongs to the current conversation
- **WHEN** the Agent calls `read_tool_result` with a valid result ID, offset, and limit
- **THEN** the system SHALL return no more than the configured maximum window and include returned content, total size metadata, next offset when content remains, and a truncation indicator

#### Scenario: Foreign result cannot be read
- **GIVEN** an overflow result belongs to another conversation
- **WHEN** the Agent calls `read_tool_result` with that result ID
- **THEN** the system SHALL return the same non-sensitive not-found response used for an unknown result ID

#### Scenario: Expired result cannot be read
- **WHEN** the Agent calls `read_tool_result` for an expired result ID
- **THEN** the system SHALL return a non-sensitive not-found response

#### Scenario: Reader obeys workspace tool policy
- **WHEN** the current workspace disables `read_tool_result`
- **THEN** the system SHALL apply the existing tool-policy denial behavior

### Requirement: Source truncation and runtime materialization remain distinguishable
The system SHALL preserve source-tool truncation metadata independently of runtime overflow materialization metadata.

#### Scenario: Fetch source output was truncated before materialization
- **WHEN** `fetch_url` or `web_extract` reaches its source byte or character limit
- **THEN** the observation SHALL indicate source truncation even when the resulting observation does not overflow the inline budget

#### Scenario: Source output becomes an overflow result
- **WHEN** a successful source-tool result exceeds the inline token budget
- **THEN** the observation SHALL identify both the source truncation state and the runtime result reference state
