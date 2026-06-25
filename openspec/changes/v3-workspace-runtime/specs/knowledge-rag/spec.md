## MODIFIED Requirements

### Requirement: Keyword retrieval
The system SHALL provide deterministic keyword retrieval over stored knowledge chunks subject to the resolved workspace memory policy.

#### Scenario: Workspace memory policy filters retrieval
- **GIVEN** a workspace snapshot has a memory policy limiting accessible knowledge bases or memory scopes
- **WHEN** an Agent in that workspace executes knowledge retrieval
- **THEN** only records allowed by the workspace memory policy are searched

#### Scenario: Memory policy reload affects later retrieval
- **GIVEN** a message pinned workspace snapshot version `N`
- **WHEN** a reload publishes version `N+1` with changed memory policy
- **THEN** retrieval for the in-flight message continues using version `N` memory policy
- **AND** later messages use version `N+1` memory policy

