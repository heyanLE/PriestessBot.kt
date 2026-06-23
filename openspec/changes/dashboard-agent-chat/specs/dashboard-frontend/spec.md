## ADDED Requirements

### Requirement: Agent Dashboard view
The Dashboard frontend SHALL include an Agent view for inspecting and testing the active Agent.

#### Scenario: Operator opens Agent view
- **GIVEN** the Dashboard has loaded runtime config
- **WHEN** the operator opens `/agent`
- **THEN** the view displays the active Agent name, provider, model, limits, available providers, and available tools

### Requirement: Agent test chat
The Agent view SHALL let operators send a test message to the Agent chat API.

#### Scenario: Operator tests a message
- **GIVEN** the Agent view is open
- **WHEN** the operator sends a message
- **THEN** the frontend posts it to `/api/agent/chat`
- **AND** displays both the user message and Agent response
- **AND** displays returned execution events
