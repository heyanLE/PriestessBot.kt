## ADDED Requirements

### Requirement: Sub-agent dashboard page
The Dashboard frontend SHALL provide a sub-agent routing page for managing orchestration configuration.

#### Scenario: Navigate to sub-agent page
- **WHEN** an operator opens the Dashboard sidebar
- **THEN** a Sub-Agents navigation entry is visible
- **AND** selecting it opens `/sub-agents`

#### Scenario: Edit and save sub-agent config
- **GIVEN** the sub-agent page has loaded the current config
- **WHEN** an operator edits valid JSON and saves it
- **THEN** the frontend sends the config to the sub-agent config API
- **AND** the page updates the editor and summary from the saved response

#### Scenario: Reject invalid config JSON
- **GIVEN** the sub-agent page is open
- **WHEN** an operator enters invalid JSON
- **THEN** save and test actions show an inline error instead of calling the API

### Requirement: Sub-agent test runner
The Dashboard frontend SHALL allow operators to test routing and selected-agent execution.

#### Scenario: Test draft config
- **GIVEN** the sub-agent page has a valid draft config
- **WHEN** an operator submits a test message
- **THEN** the frontend calls the sub-agent test API with the draft config
- **AND** displays status, selected agent, selected route, selection reason, response content, and execution events
