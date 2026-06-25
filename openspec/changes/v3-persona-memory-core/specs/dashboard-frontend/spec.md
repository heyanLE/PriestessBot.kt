## ADDED Requirements

### Requirement: Persona management view
The Dashboard frontend SHALL provide a Persona view for managing Agent personas.

#### Scenario: Operator opens Persona view
- **GIVEN** the Dashboard is running
- **WHEN** the operator opens `/personas`
- **THEN** the frontend loads personas from the Dashboard API
- **AND** displays persona name, enabled state, assigned agents, and update time

#### Scenario: Operator creates or edits persona
- **GIVEN** the Persona view is open
- **WHEN** the operator submits valid persona fields
- **THEN** the frontend calls the persona create or update API
- **AND** refreshes the displayed persona list

#### Scenario: Operator deletes persona
- **GIVEN** a persona is listed
- **WHEN** the operator confirms deletion
- **THEN** the frontend calls `DELETE /api/personas/{id}`
- **AND** removes the persona from the list after success

### Requirement: Memory management view
The Dashboard frontend SHALL provide a Memory view for managing long-term memory records.

#### Scenario: Operator opens Memory view
- **GIVEN** the Dashboard is running
- **WHEN** the operator opens `/memory`
- **THEN** the frontend loads memory records from the Dashboard API
- **AND** displays type, scope, tags, confidence, expiry, and update time

#### Scenario: Operator filters and searches memory
- **GIVEN** memory records exist
- **WHEN** the operator applies filters or submits a search query
- **THEN** the frontend calls the matching memory list or search API
- **AND** displays result scores and match reasons for search results

#### Scenario: Operator creates memory
- **GIVEN** the Memory view is open
- **WHEN** the operator submits valid memory content, type, scope, and required scope keys
- **THEN** the frontend calls `POST /api/memory`
- **AND** refreshes the displayed memory list

#### Scenario: Operator deletes memory by exact id
- **GIVEN** a memory record is listed
- **WHEN** the operator confirms deletion for that record
- **THEN** the frontend calls `DELETE /api/memory/{id}`
- **AND** removes the record from the list after success

#### Scenario: Operator runs memory expiry cleanup
- **GIVEN** the Memory view is open
- **WHEN** the operator triggers expiry cleanup
- **THEN** the frontend calls `POST /api/memory/expire`
- **AND** displays the affected record count

### Requirement: Agent chat injection trace display
The Dashboard frontend SHALL show persona and memory injection trace returned by the Agent chat test API.

#### Scenario: Operator inspects chat injection trace
- **GIVEN** an Agent chat test response includes injection trace
- **WHEN** the Agent view renders the response
- **THEN** the view displays the injected persona id or name
- **AND** displays injected memory ids, scores, and match reasons

#### Scenario: Empty injection trace is handled
- **GIVEN** an Agent chat test response has no injected persona or memory
- **WHEN** the Agent view renders the response
- **THEN** the view remains usable without showing stale trace data from an earlier run
