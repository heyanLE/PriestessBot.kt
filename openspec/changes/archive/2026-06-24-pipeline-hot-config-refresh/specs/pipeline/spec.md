## MODIFIED Requirements

### Requirement: Message processing pipeline
The system SHALL process accepted platform messages through the configured stage pipeline and produce a response when the Agent returns one.

#### Scenario: New messages use latest pipeline config
- **GIVEN** a pipeline controller is already running
- **WHEN** the application config is updated
- **AND** a later platform message is processed
- **THEN** the pipeline stages for that later message use the updated pipeline, primary Agent, and sub-agent orchestration config
- **AND** in-flight messages keep their already-created stage sequence
