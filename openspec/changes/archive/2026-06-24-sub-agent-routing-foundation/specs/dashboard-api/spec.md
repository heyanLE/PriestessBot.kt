## ADDED Requirements

### Requirement: Sub-agent Dashboard API
The Dashboard API SHALL expose sub-agent orchestration config and test routes.

#### Scenario: Read orchestration config
- **GIVEN** the Dashboard API is running
- **WHEN** an operator requests `/api/sub-agents/config`
- **THEN** the API returns the current orchestration config

#### Scenario: Replace orchestration config
- **GIVEN** the Dashboard API is running
- **WHEN** an operator puts a new orchestration config
- **THEN** the API persists and returns the updated config

#### Scenario: Test route execution
- **GIVEN** orchestration config is present
- **WHEN** an operator posts a test message
- **THEN** the API returns selected agent, selected route, status, content, and events
