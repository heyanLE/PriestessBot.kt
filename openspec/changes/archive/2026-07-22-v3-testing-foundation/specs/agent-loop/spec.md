## ADDED Requirements

### Requirement: ReActRunner SHALL have unit test coverage for core state transitions
`ReActRunner` behavior SHALL be covered by deterministic unit tests for final responses, tool-call loops, provider failures, tool failures, max-step limits, and terminal states.

#### Scenario: Final response without tool call is tested
- **WHEN** a fake provider returns assistant text without tool calls
- **THEN** a unit test SHALL verify `ReActRunner` emits `AgentResponse.Final` and transitions to `DONE`

#### Scenario: Tool-call loop is tested
- **WHEN** a fake provider first returns a tool call and later returns a final response after receiving the observation
- **THEN** a unit test SHALL verify assistant and tool messages are appended before the loop continues

#### Scenario: Tool failure observation is tested
- **WHEN** the tool executor returns or throws a tool failure during a tool call
- **THEN** a unit test SHALL verify the failure is converted into an observation that the Agent loop can pass back to the provider

#### Scenario: Provider exception is tested
- **WHEN** the provider throws during an Agent step
- **THEN** a unit test SHALL verify `ReActRunner` emits `AgentResponse.Error` and transitions to `ERROR`

#### Scenario: Max steps exceeded is tested
- **WHEN** the fake provider keeps returning tool calls until `maxSteps` is exceeded
- **THEN** a unit test SHALL verify `ReActRunner` emits `AgentResponse.Error`

#### Scenario: Terminal state step behavior is tested
- **WHEN** `step()` is called after the runner is already `DONE` or `ERROR`
- **THEN** a unit test SHALL verify the runner does not restart the message chain or duplicate side effects

### Requirement: Context compression SHALL have unit test coverage
Context compression behavior SHALL be covered by deterministic unit tests for round truncation, token window, and LLM compression fallback behavior.

#### Scenario: Round truncation is tested
- **WHEN** conversation history exceeds the configured round count
- **THEN** a unit test SHALL verify only the allowed recent rounds remain

#### Scenario: Token window is tested
- **WHEN** conversation history exceeds the configured token budget
- **THEN** a unit test SHALL verify older messages are trimmed while required system context is preserved

#### Scenario: LLM compression fallback is tested
- **WHEN** LLM compression cannot call a provider or returns an unusable result
- **THEN** a unit test SHALL verify fallback compression completes without crashing the Agent loop
