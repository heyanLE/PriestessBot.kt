## ADDED Requirements

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
