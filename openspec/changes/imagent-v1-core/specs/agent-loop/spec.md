## ADDED Requirements

### Requirement: AgentRunner interface with per-message isolation
The system SHALL define `AgentRunner` interface with `reset()`, `step()`, `stepUntilDone()`, `isDone()`. Each Runner instance SHALL serve exactly one message chain and be discarded after completion.

#### Scenario: New Runner per message
- **WHEN** ProcessStage receives a new message event
- **THEN** a fresh `ReActRunner` instance is created via Koin `factory` scope

#### Scenario: Runner isolation
- **WHEN** two messages arrive concurrently
- **THEN** their respective Runner instances operate independently with no shared mutable state

### Requirement: Agent state machine
The system SHALL implement `AgentState` with states: IDLE, RUNNING, DONE, ERROR.

#### Scenario: State transitions
- **WHEN** `reset()` is called → state becomes IDLE
- **WHEN** first `step()` is called → state becomes RUNNING
- **WHEN** final response is obtained → state becomes DONE
- **WHEN** an unrecoverable error occurs → state becomes ERROR

### Requirement: Agent hooks
The system SHALL provide `AgentHooks` with callbacks: `onAgentBegin`, `onToolStart`, `onToolEnd`, `onAgentDone`.

#### Scenario: Tool invocation hook
- **WHEN** the Agent invokes a tool
- **THEN** `onToolStart` is called with tool name and arguments, and `onToolEnd` is called with the result

#### Scenario: Agent lifecycle hook
- **WHEN** the Agent loop begins
- **THEN** `onAgentBegin` is called. When the loop ends, `onAgentDone` is called.

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

### Requirement: Context compression strategy interface
The system SHALL define `ContextCompressStrategy` interface with configurable implementations.

#### Scenario: Round truncation strategy
- **WHEN** `compressStrategy` is set to `RoundTruncationStrategy` with `maxRounds = 5`
- **THEN** messages older than the last 5 conversation rounds are discarded

#### Scenario: Token window strategy
- **WHEN** `compressStrategy` is set to `TokenWindowStrategy` with `maxTokens = 4096`
- **THEN** messages exceeding the token limit are trimmed from the earliest

#### Scenario: Strategy switchable via config
- **WHEN** `AgentConfig.compressStrategy` is changed
- **THEN** the new strategy takes effect on the next message without restart

### Requirement: Agent context carries platform reference
The system SHALL include `Platform` and `MessageSession` references in `AgentContext`, enabling built-in tools like `EarlyReplyTool` to send messages.

#### Scenario: Tool sends early reply
- **WHEN** a tool needs to send a proactive message during the loop
- **THEN** it retrieves the Platform and MessageSession from AgentContext and calls `platform.sendMessage()`
