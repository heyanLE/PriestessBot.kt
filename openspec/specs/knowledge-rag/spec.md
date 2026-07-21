# knowledge-rag Specification

## Purpose
TBD - created by archiving change knowledge-rag-foundation. Update Purpose after archive.
## Requirements

### Requirement: Knowledge base persistence
The system SHALL persist knowledge bases and text document chunks.

#### Scenario: Create and list knowledge bases
- **GIVEN** an operator creates a knowledge base
- **WHEN** knowledge bases are listed
- **THEN** the created base is returned with id, name, description, and timestamps

#### Scenario: Add text document
- **GIVEN** a knowledge base exists
- **WHEN** the operator adds a text document
- **THEN** the document is split into one or more stored chunks

### Requirement: Keyword retrieval
The system SHALL provide deterministic keyword retrieval over stored knowledge chunks subject to the resolved workspace memory policy.

#### Scenario: Query matches relevant chunk
- **GIVEN** multiple chunks are stored
- **WHEN** a query shares terms with one chunk
- **THEN** that chunk is returned with a positive score

#### Scenario: Limit search results
- **GIVEN** several chunks match a query
- **WHEN** a search limit is provided
- **THEN** no more than that number of results is returned

#### Scenario: Workspace memory policy filters retrieval
- **GIVEN** a workspace snapshot has a memory policy limiting accessible knowledge bases or memory scopes
- **WHEN** an Agent in that workspace executes knowledge retrieval
- **THEN** only records allowed by the workspace memory policy are searched

#### Scenario: Memory policy reload affects later retrieval
- **GIVEN** a message pinned workspace snapshot version `N`
- **WHEN** a reload publishes version `N+1` with changed memory policy
- **THEN** retrieval for the in-flight message continues using version `N` memory policy
- **AND** later messages use version `N+1` memory policy
