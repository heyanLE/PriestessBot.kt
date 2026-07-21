## MODIFIED Requirements

### Requirement: PreProcess stage
The system SHALL load conversation history, resolve a workspace snapshot, and attach workspace-scoped runtime context before the Agent loop.

#### Scenario: PreProcess resolves workspace snapshot
- **WHEN** PreProcessStage processes an accepted event
- **THEN** it resolves a workspace snapshot from the event platform, session, user, group, and metadata context
- **AND** stores the snapshot on the pipeline context

#### Scenario: PreProcess attaches workspace-scoped skill availability
- **GIVEN** the resolved workspace snapshot contains enabled skills
- **WHEN** PreProcessStage builds the message context
- **THEN** only the skills visible in that workspace snapshot are available for the current Pipeline run

#### Scenario: Agent prompt renders context blocks
- **WHEN** the Agent loop sends a request to the LLM provider
- **THEN** the system prompt contains separate Platform, Role Document, Tools, and Loaded Skills blocks

#### Scenario: Skill tool loads prompt content
- **GIVEN** a workspace-visible skill exists
- **WHEN** the LLM calls `use_skill` with that skill name
- **THEN** the current Pipeline skill state loads that skill's prompt document
- **AND** later LLM requests in the same Agent run include the loaded `SKILL.md` content in the Loaded Skills block

### Requirement: Message processing pipeline
The system SHALL process accepted platform messages through the configured stage pipeline and produce a response when the Agent returns one.

#### Scenario: In-flight messages keep pinned workspace snapshot
- **GIVEN** a message has entered the pipeline and pinned workspace snapshot version `N`
- **WHEN** the workspace reloads and publishes version `N+1`
- **THEN** all remaining stages for the in-flight message continue using version `N`
- **AND** later messages use version `N+1`

#### Scenario: Pipeline records workspace metadata
- **WHEN** a platform message is processed
- **THEN** the pipeline context records workspace id, workspace name, snapshot version, and workspace resolution reason
- **AND** downstream Agent execution and response diagnostics can read that metadata
