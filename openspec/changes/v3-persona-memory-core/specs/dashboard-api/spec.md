## ADDED Requirements

### Requirement: Persona Dashboard API
The Dashboard API SHALL expose CRUD routes for operator-managed personas.

#### Scenario: List personas
- **GIVEN** personas exist for a workspace
- **WHEN** an operator requests `GET /api/personas`
- **THEN** the API returns personas with id, workspace id, name, description, tone, boundaries, prompt template, enabled state, agent names, and timestamps

#### Scenario: Create persona
- **GIVEN** the Dashboard API is running
- **WHEN** an operator posts a valid persona create request to `POST /api/personas`
- **THEN** the API persists and returns the created persona

#### Scenario: Update persona
- **GIVEN** a persona exists
- **WHEN** an operator sends `PUT /api/personas/{id}` with valid changes
- **THEN** the API persists and returns the updated persona

#### Scenario: Delete persona
- **GIVEN** a persona exists
- **WHEN** an operator sends `DELETE /api/personas/{id}`
- **THEN** the persona is removed from list and resolve results

### Requirement: Memory Dashboard API
The Dashboard API SHALL expose list, create, search, delete, and expiry routes for memory records.

#### Scenario: List memory
- **GIVEN** memory records exist
- **WHEN** an operator requests `GET /api/memory` with optional filters
- **THEN** the API returns matching non-deleted and non-expired memory records

#### Scenario: Create memory
- **GIVEN** the Dashboard API is running
- **WHEN** an operator posts a valid memory create request to `POST /api/memory`
- **THEN** the API persists and returns the created memory record

#### Scenario: Search memory
- **GIVEN** memory records exist for the requested scope context
- **WHEN** an operator posts a query to `POST /api/memory/search`
- **THEN** the API returns matching memory records with scores and match reasons

#### Scenario: Delete memory
- **GIVEN** a memory record exists
- **WHEN** an operator sends `DELETE /api/memory/{id}`
- **THEN** the memory record is excluded from future list, search, recall, and injection results

#### Scenario: Expire memory
- **GIVEN** expired memory records exist
- **WHEN** an operator sends `POST /api/memory/expire`
- **THEN** the API runs expiry cleanup and returns the number of affected records

### Requirement: Agent chat injection trace API
The Dashboard API SHALL include persona and memory injection trace in Agent chat test responses.

#### Scenario: Chat response includes injection trace
- **GIVEN** persona or memory injection runs during `/api/agent/chat`
- **WHEN** the chat response is returned
- **THEN** the response includes injected persona id and name when present
- **AND** includes injected memory ids, scores, and match reasons

#### Scenario: Chat response omits empty trace details
- **GIVEN** no persona or memory is injected
- **WHEN** the chat response is returned
- **THEN** the response remains valid
- **AND** the trace is empty or explicitly reports no injection
