## MODIFIED Requirements

### Requirement: Message processing pipeline
The system SHALL process accepted platform messages through the configured stage pipeline and produce a response when the Agent returns one.

#### Scenario: Pipeline records sub-agent selection metadata
- **GIVEN** sub-agent routing is configured
- **WHEN** the pipeline selects an agent for a message
- **THEN** the pipeline context records selected agent name, route name when present, and selection reason
- **AND** response decoration and sending continue to use the existing pipeline stages
