## MODIFIED Requirements

### Requirement: Runtime config publication
The system SHALL publish updated config slices to runtime components after explicit updates or reloads.

#### Scenario: Pipeline observes published config for subsequent messages
- **GIVEN** Dashboard or config reload publishes updated Agent, pipeline, or sub-agent config
- **WHEN** the next message enters the pipeline
- **THEN** the pipeline uses the latest published config values without requiring process restart
