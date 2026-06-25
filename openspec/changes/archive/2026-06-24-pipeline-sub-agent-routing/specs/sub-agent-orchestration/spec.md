## MODIFIED Requirements

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
