## ADDED Requirements

### Requirement: Pipeline stages SHALL have stage-level unit test coverage
Each pipeline stage SHALL have focused tests for allow, block, stop, context mutation, and post-processing behavior relevant to that stage.

#### Scenario: Waking and whitelist decisions are tested
- **WHEN** direct, group, mentioned, prefixed, whitelisted, and non-whitelisted messages are evaluated
- **THEN** stage-level tests SHALL verify whether the pipeline continues or stops

#### Scenario: Rate limit decisions are tested
- **WHEN** a sender is under or over the configured rate limit
- **THEN** stage-level tests SHALL verify pass, stop, and optional notification behavior

#### Scenario: PreProcess creates AgentContext
- **WHEN** `PreProcessStage` accepts a message
- **THEN** a stage-level test SHALL verify system prompt, conversation history, platform, session, and metadata are attached to the Agent context

#### Scenario: Onion flow post-processing is tested
- **WHEN** a stage returns a post-processing flow
- **THEN** a stage-level test SHALL verify later stages run before post-processing resumes

#### Scenario: Respond final and error delivery is tested
- **WHEN** the pipeline context contains a final response or error response
- **THEN** `RespondStage` tests SHALL verify the originating platform receives the expected outbound message

### Requirement: Pipeline integration SHALL cover Agent tool-call handoff
The pipeline integration suite SHALL cover the handoff from accepted platform message to Agent runner, ToolExecutor, response decoration, and platform delivery.

#### Scenario: Pipeline invokes ReAct tool flow
- **WHEN** an accepted message enters the pipeline and the fake provider returns a tool call before a final response
- **THEN** an integration test SHALL verify the tool executes, the final response is stored, and `RespondStage` sends it

#### Scenario: Pipeline metrics are recorded
- **WHEN** a pipeline integration test completes successfully or with an error
- **THEN** the test SHALL verify pipeline, LLM, and tool metrics are recorded with non-sensitive labels
