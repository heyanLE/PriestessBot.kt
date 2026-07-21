## ADDED Requirements

### Requirement: Dashboard ToolView SHALL show tool policy and status
The Dashboard frontend SHALL provide a ToolView that lets operators inspect registered tool permission, enablement, source, and health/status state.

#### Scenario: ToolView renders policy metadata
- **GIVEN** the ToolView loads tool metadata from the Dashboard API
- **WHEN** tools are displayed
- **THEN** each tool row SHALL show name, description, source, risk level, effective enabled state, default enabled state, audit flag, and unavailable/status reason when present

#### Scenario: ToolView supports operational filtering
- **GIVEN** the ToolView has loaded tools
- **WHEN** an operator filters by source, risk level, enabled state, or text query
- **THEN** the visible tools SHALL match the selected filters

#### Scenario: ToolView highlights risky and unavailable tools
- **GIVEN** a tool is high risk, disabled, or unavailable
- **WHEN** the ToolView renders that tool
- **THEN** the tool state SHALL be visually distinguishable from enabled safe-read tools

#### Scenario: ToolView uses typed API model
- **WHEN** frontend code consumes the tool API
- **THEN** the API client types SHALL include tool source, risk level, required capabilities, default enabled state, effective enabled state, audit flag, and status reason
