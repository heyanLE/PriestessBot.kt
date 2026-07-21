## ADDED Requirements

### Requirement: Agent tool timeout SHALL be honored
The Agent loop SHALL pass the configured tool timeout to tool execution so tool calls cannot block an Agent run indefinitely.

#### Scenario: Agent timeout config reaches executor
- **GIVEN** an Agent has `toolTimeoutMs` configured
- **WHEN** the Agent loop executes a tool call
- **THEN** the configured timeout SHALL be passed to `ToolExecutor`

#### Scenario: Tool timeout produces observable event
- **GIVEN** a tool call exceeds `toolTimeoutMs`
- **WHEN** the Agent loop receives the timeout result
- **THEN** the loop SHALL append the timeout observation for the model
- **AND** tool start/end events SHALL expose the timeout status

#### Scenario: Missing timeout uses runtime default
- **GIVEN** an Agent does not configure `toolTimeoutMs`
- **WHEN** the Agent loop executes a tool call
- **THEN** the runtime default tool timeout SHALL be used
