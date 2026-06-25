# sub-agent-orchestration Specification

## Purpose
TBD - created by archiving change sub-agent-routing-foundation. Update Purpose after archive.
## Requirements
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

### Requirement: Sub-agent route selection
The system SHALL select a configured sub-agent deterministically from an incoming message.

#### Scenario: Pipeline uses selected sub-agent
- **GIVEN** sub-agent orchestration is enabled
- **AND** a route keyword matches an incoming platform message
- **WHEN** the message reaches pipeline pre-processing
- **THEN** the selected route target agent is used to create the pipeline `AgentContext`
- **AND** downstream ReAct execution uses that selected agent

#### Scenario: Pipeline preserves primary agent fallback
- **GIVEN** sub-agent orchestration is disabled or has no matching/default agent
- **WHEN** an incoming platform message reaches pipeline pre-processing
- **THEN** the primary configured Agent is used as before
