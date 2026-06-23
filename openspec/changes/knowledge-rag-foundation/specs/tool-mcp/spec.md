## ADDED Requirements

### Requirement: Knowledge search tool
The system SHALL expose a built-in Agent tool for knowledge retrieval.

#### Scenario: Agent calls knowledge search
- **GIVEN** knowledge chunks are stored
- **WHEN** the `knowledge_search` tool is executed with a query
- **THEN** the tool returns formatted matching snippets

#### Scenario: No knowledge result
- **GIVEN** no stored chunk matches the query
- **WHEN** the `knowledge_search` tool is executed
- **THEN** the tool returns a successful empty-result message
