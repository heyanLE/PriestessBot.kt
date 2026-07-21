# persona-memory Specification

## Purpose
TBD - created while syncing archived changes. Update Purpose after archive.

## Requirements

### Requirement: Persona error-message persistence
The system SHALL persist optional structured error messages with a persona, including `permissionDenied`, and expose them through persona create, update, list, get, and resolve operations.

#### Scenario: Permission-denied message round trip
- **GIVEN** an operator saves a persona with a `permissionDenied` message
- **WHEN** that persona is retrieved or resolved
- **THEN** its returned error-message configuration contains the saved `permissionDenied` message

### Requirement: Persona persistence and resolution
The system SHALL persist operator-managed personas and resolve an enabled persona for an Agent within a workspace.

#### Scenario: List personas by workspace
- **GIVEN** personas exist in multiple workspaces
- **WHEN** personas are listed for one workspace
- **THEN** only personas from that workspace are returned
- **AND** each persona includes id, name, description, tone, boundaries, prompt template, enabled state, agent names, and timestamps

#### Scenario: Resolve agent-specific persona
- **GIVEN** an enabled workspace-wide persona exists
- **AND** an enabled persona assigned to agent `assistant` exists in the same workspace
- **WHEN** persona is resolved for agent `assistant`
- **THEN** the agent-specific persona is returned

#### Scenario: Disabled persona is not resolved
- **GIVEN** a persona exists with `enabled = false`
- **WHEN** persona is resolved for its workspace and agent
- **THEN** that persona is not returned

#### Scenario: Delete persona
- **GIVEN** a persona exists
- **WHEN** the persona is deleted by id
- **THEN** it is no longer returned by list or resolve operations

### Requirement: Memory persistence and lifecycle
The system SHALL persist memory records with type, scope, confidence, tags, timestamps, TTL, and soft deletion state.

#### Scenario: Save memory record
- **GIVEN** a valid memory record for a workspace
- **WHEN** the record is saved
- **THEN** the stored record is returned with id, created timestamp, updated timestamp, scope, type, content, tags, confidence, and optional expiry

#### Scenario: List memory by filter
- **GIVEN** memory records exist with different types and tags
- **WHEN** memory is listed with a type or tag filter
- **THEN** only matching non-deleted and non-expired records are returned

#### Scenario: Expired memory excluded
- **GIVEN** a memory record has `expiresAt` earlier than the current time
- **WHEN** memory is listed or searched
- **THEN** the expired record is excluded

#### Scenario: Expire memory records
- **GIVEN** memory records include entries with `expiresAt` earlier than the current time
- **WHEN** expiration cleanup runs
- **THEN** expired records are marked expired or deleted according to storage policy
- **AND** the cleanup result reports how many records were affected

#### Scenario: Delete memory by exact id
- **GIVEN** a memory record exists
- **WHEN** memory is deleted using that exact id
- **THEN** the record is no longer returned by list, search, or prompt injection

### Requirement: Memory scope enforcement
The system SHALL enforce memory visibility by workspace and scope context.

#### Scenario: Workspace memory is visible in workspace
- **GIVEN** a `GLOBAL` memory exists for a workspace
- **WHEN** memory is searched from that workspace
- **THEN** the memory is eligible for retrieval

#### Scenario: Platform memory requires matching platform
- **GIVEN** a `PLATFORM` memory exists for platform `p1`
- **WHEN** memory is searched from platform `p2`
- **THEN** the memory is not eligible for retrieval

#### Scenario: Session memory requires matching session
- **GIVEN** a `SESSION` memory exists for session `s1`
- **WHEN** memory is searched from session `s1`
- **THEN** the memory is eligible for retrieval

#### Scenario: User memory requires matching user
- **GIVEN** a `USER` memory exists for user `u1`
- **WHEN** memory is searched for user `u2`
- **THEN** the memory is not eligible for retrieval

#### Scenario: Agent memory requires matching agent
- **GIVEN** an `AGENT` memory exists for agent `assistant`
- **WHEN** memory is searched for agent `planner`
- **THEN** the memory is not eligible for retrieval

### Requirement: Keyword memory retrieval
The system SHALL provide deterministic keyword retrieval over eligible memory records.

#### Scenario: Relevant memory is ranked first
- **GIVEN** multiple eligible memory records exist
- **WHEN** a query shares more terms with one record than another
- **THEN** the stronger match is returned first with a higher score

#### Scenario: Retrieval includes match reason
- **GIVEN** a memory record matches a query by content or tag
- **WHEN** memory search returns the record
- **THEN** the result includes a score and human-readable match reason

#### Scenario: Retrieval respects limit
- **GIVEN** more eligible memory records match than the requested limit
- **WHEN** search is executed with a limit
- **THEN** no more than that number of results is returned

### Requirement: Persona and memory prompt injection
The system SHALL render resolved persona and relevant memories into the Agent system prompt before execution.

#### Scenario: Persona is injected into system prompt
- **GIVEN** an enabled persona resolves for an Agent
- **WHEN** the Agent context is prepared
- **THEN** the rendered system prompt includes the persona prompt template, tone, and boundaries

#### Scenario: Relevant memories are injected into system prompt
- **GIVEN** eligible memory records match the current user message
- **WHEN** the Agent context is prepared
- **THEN** the rendered system prompt includes bounded memory snippets
- **AND** each snippet is associated with its memory id in trace metadata

#### Scenario: Injection trace is recorded
- **GIVEN** persona and memory injection runs
- **WHEN** the Agent context is inspected
- **THEN** metadata includes persona id, persona name, injected memory ids, scores, match reasons, and skipped-memory reasons where available

#### Scenario: Expired or deleted memory is not injected
- **GIVEN** matching memory records include expired or deleted records
- **WHEN** prompt injection runs
- **THEN** expired and deleted records are omitted from the prompt
- **AND** the injection trace records that they were skipped when those records were considered
