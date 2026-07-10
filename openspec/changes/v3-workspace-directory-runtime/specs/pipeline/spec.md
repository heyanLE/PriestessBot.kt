## MODIFIED Requirements

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

#### Scenario: PrepareWorkspace runs before preprocessing
- **WHEN** an accepted message enters the pipeline
- **THEN** `PrepareWorkspaceStage` runs before `PreProcessStage`
- **AND** workspace preparation completes before agent context assembly begins

#### Scenario: Stage stops the pipeline
- **WHEN** a stage sets `event.isStopped = true`
- **THEN** all subsequent stages are skipped
- **AND** the pipeline terminates

### Requirement: PreProcess stage
The system SHALL load conversation history and attach workspace-scoped runtime context from an already-prepared workspace snapshot before the Agent loop.

#### Scenario: PreProcess consumes pinned workspace snapshot
- **WHEN** `PreProcessStage` processes an accepted event
- **THEN** it reads the already-pinned workspace snapshot from the pipeline context
- **AND** it does not re-resolve the workspace directory on its own

#### Scenario: PreProcess attaches workspace-scoped skill availability
- **GIVEN** the pinned workspace snapshot contains enabled skill descriptors
- **WHEN** `PreProcessStage` builds the message context
- **THEN** only the skills visible in that workspace snapshot are available for the current pipeline run

#### Scenario: PreProcess loads conversation history
- **WHEN** a conversation exists for the current session
- **THEN** the recent message history is loaded and attached to the context

### Requirement: Message processing pipeline
The system SHALL process accepted platform messages through the configured stage pipeline and produce a response when the Agent returns one.

#### Scenario: In-flight messages keep pinned workspace snapshot
- **GIVEN** a message has entered the pipeline and pinned a workspace snapshot
- **WHEN** the underlying workspace directory changes for a later message
- **THEN** all remaining stages for the in-flight message continue using the pinned snapshot
- **AND** later messages use a newly prepared snapshot

#### Scenario: Pipeline records workspace source metadata
- **WHEN** a platform message is processed
- **THEN** the pipeline context records workspace id, workspace name, snapshot version, effective directory path, and resolution source
- **AND** downstream agent execution can read that metadata

## ADDED Requirements

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
