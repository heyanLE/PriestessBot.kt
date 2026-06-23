## ADDED Requirements

### Requirement: Knowledge Dashboard API
The Dashboard API SHALL expose knowledge base management and search routes.

#### Scenario: Create base through API
- **GIVEN** the Dashboard API is running
- **WHEN** an operator posts a knowledge base create request
- **THEN** the API returns the created knowledge base

#### Scenario: Add document through API
- **GIVEN** a knowledge base exists
- **WHEN** an operator posts a text document to that base
- **THEN** the API returns stored chunk metadata

#### Scenario: Test search through API
- **GIVEN** stored knowledge chunks exist
- **WHEN** an operator posts a search request
- **THEN** the API returns matching chunks and scores
