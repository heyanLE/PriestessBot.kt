## ADDED Requirements

### Requirement: Knowledge management view
The Dashboard frontend SHALL provide a Knowledge view for operator-managed RAG content.

#### Scenario: Operator opens Knowledge view
- **GIVEN** the Dashboard is running
- **WHEN** the operator opens `/knowledge`
- **THEN** the frontend loads and displays knowledge bases

#### Scenario: Operator creates knowledge base
- **GIVEN** the Knowledge view is open
- **WHEN** the operator submits a new base name
- **THEN** the frontend calls the knowledge base create API
- **AND** refreshes the displayed base list

#### Scenario: Operator adds text document
- **GIVEN** a knowledge base exists
- **WHEN** the operator submits document text
- **THEN** the frontend calls the add document API
- **AND** displays the stored chunk count

#### Scenario: Operator tests retrieval
- **GIVEN** indexed document chunks exist
- **WHEN** the operator submits a search query
- **THEN** the frontend calls the knowledge search API
- **AND** displays scored result snippets
