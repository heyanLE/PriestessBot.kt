## MODIFIED Requirements

### Requirement: Sub-agent dashboard page
The Dashboard frontend SHALL provide a sub-agent routing page for managing orchestration configuration.

#### Scenario: Structured runtime controls update draft config
- **GIVEN** the sub-agent page has a valid draft config
- **WHEN** an operator toggles orchestration or changes the default agent
- **THEN** the JSON editor draft is updated with the new config value

#### Scenario: Structured agent editing updates draft config
- **GIVEN** the sub-agent page has a valid draft config
- **WHEN** an operator adds or removes a sub-agent
- **THEN** the JSON editor draft is updated
- **AND** route/default references to removed agents are cleaned up

#### Scenario: Structured route editing updates draft config
- **GIVEN** the sub-agent page has a valid draft config with at least one agent
- **WHEN** an operator adds, edits, enables/disables, or removes a route
- **THEN** the JSON editor draft reflects the route changes
- **AND** save/test actions use the updated draft
