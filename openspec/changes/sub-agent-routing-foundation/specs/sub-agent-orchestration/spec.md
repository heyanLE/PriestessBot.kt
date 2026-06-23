## ADDED Requirements

### Requirement: Sub-agent routing config
The system SHALL support serializable sub-agent orchestration configuration.

#### Scenario: Default config is backward-compatible
- **GIVEN** an existing config file without orchestration fields
- **WHEN** it is decoded
- **THEN** orchestration defaults are available without failing config load

### Requirement: Keyword route selection
The system SHALL route messages to sub-agents by deterministic keyword rules.

#### Scenario: Keyword route matches
- **GIVEN** orchestration is enabled with a route targeting a sub-agent
- **WHEN** an input message contains a route keyword
- **THEN** the orchestrator selects the targeted sub-agent

#### Scenario: No route falls back
- **GIVEN** orchestration is enabled without a matching route
- **WHEN** an input message is tested
- **THEN** the orchestrator selects the configured default sub-agent or the primary Agent

### Requirement: Routed Agent execution
The system SHALL execute the selected sub-agent using the existing Agent runtime.

#### Scenario: Selected Agent returns final response
- **GIVEN** a selected sub-agent provider is available
- **WHEN** the orchestrator runs a message
- **THEN** the response includes the selected agent name and final content
