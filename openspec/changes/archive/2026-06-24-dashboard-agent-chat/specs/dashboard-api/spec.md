## ADDED Requirements

### Requirement: Agent chat test API
The Dashboard API SHALL expose a synchronous Agent chat test endpoint.

#### Scenario: Agent returns final response
- **GIVEN** the configured provider is available
- **WHEN** the operator posts a user message to `/api/agent/chat`
- **THEN** the server runs the configured Agent runner
- **AND** returns the final response content

#### Scenario: Provider missing
- **GIVEN** the configured Agent references a missing provider
- **WHEN** the operator posts a user message to `/api/agent/chat`
- **THEN** the server returns an Agent chat response with status `ERROR`
- **AND** includes an error message naming the missing provider

### Requirement: Agent chat events
The Dashboard API SHALL include ordered Agent execution events in chat responses.

#### Scenario: Tool execution is captured
- **GIVEN** the Agent runner executes a tool during chat
- **WHEN** the chat response is returned
- **THEN** it includes tool start and tool end events with the tool name
