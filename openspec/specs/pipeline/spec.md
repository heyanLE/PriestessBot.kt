# pipeline Specification

## Purpose
TBD - created by archiving change priestess-v1-core. Update Purpose after archive.
## Requirements
### Requirement: Pipeline scheduler with onion model
The system SHALL process each `MessageEvent` through 9 stages in order, supporting an onion model where stages can execute pre-processing, yield for subsequent stages, then execute post-processing.

#### Scenario: Linear stage execution
- **WHEN** a stage returns null from `process()`
- **THEN** the scheduler proceeds immediately to the next stage

#### Scenario: Onion stage execution
- **WHEN** a stage returns a Flow from `process()`
- **THEN** the pre-processing logic runs, subsequent stages execute, then the post-processing logic in the Flow resumes

#### Scenario: Stage stops the pipeline
- **WHEN** a stage sets `event.isStopped = true`
- **THEN** all subsequent stages are skipped and the pipeline terminates

### Requirement: WakingCheck stage
The system SHALL skip messages that do not trigger the bot (e.g., group messages without @mention, messages without command prefix).

#### Scenario: Group message without mention
- **WHEN** a group message arrives without @mentioning the bot and no prefix is configured
- **THEN** the pipeline stops with `isStopped = true`

#### Scenario: Direct message always passes
- **WHEN** a direct/private message arrives
- **THEN** the waking check passes and pipeline continues

### Requirement: WhitelistCheck stage
The system SHALL only process messages from users/groups in the configured whitelist if whitelist is enabled.

#### Scenario: User in whitelist
- **WHEN** whitelist is enabled and the sender is in the whitelist
- **THEN** the pipeline continues

#### Scenario: User not in whitelist
- **WHEN** whitelist is enabled and the sender is not in the whitelist
- **THEN** the pipeline stops

### Requirement: RateLimit stage
The system SHALL limit message processing frequency per user or per session.

#### Scenario: Under rate limit
- **WHEN** user sends a message and has not exceeded the configured rate limit
- **THEN** the pipeline continues

#### Scenario: Over rate limit
- **WHEN** user sends a message and has exceeded the configured rate limit
- **THEN** the pipeline stops and optionally sends a rate-limit notification

### Requirement: PreProcess stage
The system SHALL inject System Prompt, load conversation history, and attach Skill instructions before the Agent loop.

#### Scenario: PreProcess attaches system prompt
- **WHEN** PreProcessStage processes an event
- **THEN** the Agent's System Prompt is prepended to the message context

#### Scenario: PreProcess loads conversation history
- **WHEN** a conversation exists for the current session
- **THEN** the recent message history is loaded and attached to the context

### Requirement: Process stage invokes AgentRunner
The system SHALL create a new AgentRunner instance and execute the ReAct loop.

#### Scenario: AgentRunner invoked for a message
- **WHEN** ProcessStage processes an event
- **THEN** a new `ReActRunner` instance is created, `stepUntilDone()` is called, and the final response is stored in the pipeline context

#### Scenario: AgentRunner times out at max steps
- **WHEN** the AgentRunner reaches maxSteps without a final response
- **THEN** an error response is generated

### Requirement: ResultDecorate stage
The system SHALL format the Agent's response before sending (Markdown rendering, prefix/suffix decoration).

#### Scenario: Plain text response
- **WHEN** the Agent response is plain text
- **THEN** the response passes through without modification

### Requirement: Respond stage
The system SHALL send the final response back to the user via the originating Platform.

#### Scenario: Successful response delivery
- **WHEN** RespondStage processes the final response
- **THEN** `platform.sendMessage()` is called with the response content and original session

### Requirement: Message processing pipeline
The system SHALL process accepted platform messages through the configured stage pipeline and produce a response when the Agent returns one.

#### Scenario: Pipeline records sub-agent selection metadata
- **GIVEN** sub-agent routing is configured
- **WHEN** the pipeline selects an agent for a message
- **THEN** the pipeline context records selected agent name, route name when present, and selection reason
- **AND** response decoration and sending continue to use the existing pipeline stages

#### Scenario: New messages use latest pipeline config
- **GIVEN** a pipeline controller is already running
- **WHEN** the application config is updated
- **AND** a later platform message is processed
- **THEN** the pipeline stages for that later message use the updated pipeline, primary Agent, and sub-agent orchestration config
- **AND** in-flight messages keep their already-created stage sequence