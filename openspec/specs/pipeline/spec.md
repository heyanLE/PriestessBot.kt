# pipeline Specification

## Purpose
TBD - created by archiving change priestess-v1-core. Update Purpose after archive.
## Requirements

### Requirement: Pipeline scheduler with onion model
The system SHALL process each `MessageEvent` through 10 stages in order, supporting an onion model where stages can execute pre-processing, yield for subsequent stages, then execute post-processing.

#### Scenario: Linear stage execution
- **WHEN** a stage returns null from `process()`
- **THEN** the scheduler proceeds immediately to the next stage

#### Scenario: Onion stage execution
- **WHEN** a stage returns a Flow from `process()`
- **THEN** the pre-processing logic runs
- **AND** subsequent stages execute
- **AND** the post-processing logic in the Flow resumes afterward

#### Scenario: Stage stops the pipeline
- **WHEN** a stage sets `event.isStopped = true`
- **THEN** all subsequent stages are skipped
- **AND** the pipeline terminates

#### Scenario: PrepareWorkspace runs before preprocessing
- **WHEN** an accepted message enters the pipeline
- **THEN** `PrepareWorkspaceStage` runs before `PreProcessStage`
- **AND** workspace preparation completes before agent context assembly begins

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
The system SHALL load conversation history, resolve a workspace snapshot, and attach workspace-scoped runtime context before the Agent loop.

#### Scenario: PreProcess attaches system prompt
- **WHEN** PreProcessStage processes an event
- **THEN** the Agent's System Prompt is prepended to the message context

#### Scenario: PreProcess loads conversation history
- **WHEN** a conversation exists for the current session
- **THEN** the recent message history is loaded and attached to the context

#### Scenario: PreProcess consumes pinned workspace snapshot
- **WHEN** `PreProcessStage` processes an accepted event
- **THEN** it reads the already-pinned workspace snapshot from the pipeline context
- **AND** it does not re-resolve the workspace directory on its own

#### Scenario: PreProcess attaches workspace-scoped skill availability
- **GIVEN** the resolved workspace snapshot contains enabled skills
- **WHEN** PreProcessStage builds the message context
- **THEN** only the skills visible in that workspace snapshot are available for the current Pipeline run

#### Scenario: PreProcess resolves workspace snapshot
- **WHEN** PreProcessStage processes an accepted event
- **THEN** it resolves a workspace snapshot from the event platform, session, user, group, and metadata context
- **AND** stores the snapshot on the pipeline context

#### Scenario: Agent prompt renders context blocks
- **WHEN** the Agent loop sends a request to the LLM provider
- **THEN** the system prompt contains separate Platform, Role Document, Tools, and Loaded Skills blocks

#### Scenario: Skill tool loads prompt content
- **GIVEN** a workspace-visible skill exists
- **WHEN** the LLM calls `use_skill` with that skill name
- **THEN** the current Pipeline skill state loads that skill's prompt document
- **AND** later LLM requests in the same Agent run include the loaded `SKILL.md` content in the Loaded Skills block

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

#### Scenario: In-flight messages keep pinned workspace snapshot
- **GIVEN** a message has entered the pipeline and pinned workspace snapshot version `N`
- **WHEN** the workspace reloads and publishes version `N+1`
- **THEN** all remaining stages for the in-flight message continue using version `N`
- **AND** later messages use version `N+1`

#### Scenario: Pipeline records workspace source metadata
- **WHEN** a platform message is processed
- **THEN** the pipeline context records workspace id, workspace name, snapshot version, effective directory path, and resolution source
- **AND** downstream agent execution can read that metadata

#### Scenario: Pipeline records workspace metadata
- **WHEN** a platform message is processed
- **THEN** the pipeline context records workspace id, workspace name, snapshot version, and workspace resolution reason
- **AND** downstream Agent execution and response diagnostics can read that metadata

### Requirement: Permission and command stages
The system SHALL resolve a sender permission group after workspace preparation and before command dispatch, and SHALL run command dispatch before `PreProcessStage`.

#### Scenario: Command completion retains response stages
- **WHEN** a command is handled successfully or denied
- **THEN** the pipeline bypasses `PreProcessStage` and Agent processing
- **AND** it continues through result decoration and response delivery with the command response

#### Scenario: Permission metadata reaches Agent execution
- **WHEN** a non-command message continues past the permission stage
- **THEN** its resolved sender ID and permission group are available to Agent Tool execution

### Requirement: PrepareWorkspace stage
The system SHALL provide a `PrepareWorkspaceStage` before `PreProcessStage` that resolves the effective workspace directory and builds the pinned workspace snapshot.

#### Scenario: PrepareWorkspace applies source precedence
- **GIVEN** candidate workspace directory values from config, platform config, and message metadata
- **WHEN** `PrepareWorkspaceStage` resolves the effective workspace directory
- **THEN** it applies the configured precedence from config default to platform override to message override

#### Scenario: PrepareWorkspace reads workspace directory in real time
- **WHEN** `PrepareWorkspaceStage` prepares a workspace snapshot for a message
- **THEN** it reads the workspace directory from disk at that time
- **AND** stores the resulting snapshot on the pipeline context before later stages execute

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
