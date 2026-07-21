## MODIFIED Requirements

### Requirement: ReActRunner Thought-Action-Observation loop
The system SHALL implement `ReActRunner` that iterates: check context → call LLM → execute tool calls if present → repeat until final answer or max steps.

#### Scenario: Single turn without tools
- **WHEN** the LLM responds with text and no tool calls
- **THEN** `AgentResponse.Final` is emitted with the response text

#### Scenario: Multi-turn with tools
- **WHEN** the LLM responds with a tool call
- **THEN** the tool is executed, result returned to LLM, and the loop continues

#### Scenario: Max steps exceeded
- **WHEN** the loop reaches maxSteps without a final answer
- **THEN** `AgentResponse.Error` is emitted

#### Scenario: Persona and memory are available before first LLM call
- **GIVEN** persona or memory injection is enabled for the current workspace
- **WHEN** the Agent runner builds the initial LLM messages
- **THEN** the system message includes the injected persona and memory prompt section
- **AND** the first LLM call receives the enhanced instructions

### Requirement: Agent context carries platform reference
The system SHALL include `Platform` and `MessageSession` references in `AgentContext`, enabling built-in tools like `EarlyReplyTool` to send messages.

#### Scenario: Tool sends early reply
- **WHEN** a tool needs to send a proactive message during the loop
- **THEN** it retrieves the Platform and MessageSession from AgentContext and calls `platform.sendMessage()`

#### Scenario: Context carries persona and memory injection trace
- **GIVEN** persona and memory injection ran while preparing the Agent context
- **WHEN** Agent hooks, tools, or Dashboard chat response builders inspect the context metadata
- **THEN** they can read the injected persona id and injected memory result metadata
