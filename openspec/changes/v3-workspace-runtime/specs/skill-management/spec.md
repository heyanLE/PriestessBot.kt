## MODIFIED Requirements

### Requirement: Skill interface
The system SHALL define `Skill` interface with `name`, `description`, `priority`, `canHandle()`, and `execute()` methods, and SHALL allow workspace snapshots to expose scoped skill sets and prompt documents.

#### Scenario: Workspace snapshot selects skills
- **GIVEN** global skill definitions are available
- **AND** a workspace config enables a subset with workspace-specific settings
- **WHEN** the workspace snapshot is built
- **THEN** only the enabled workspace-scoped skills are included in that snapshot

#### Scenario: Workspace skills expose prompt documents
- **GIVEN** a workspace snapshot contains enabled skills
- **WHEN** the AgentContext is built for a Pipeline run
- **THEN** the scoped skills are exposed as available skill prompt documents
- **AND** they are not executed by automatic pre-match before the LLM decides to use them

#### Scenario: Skill reload affects later messages only
- **GIVEN** a message pinned workspace snapshot version `N`
- **WHEN** a reload publishes version `N+1` with changed skill settings
- **THEN** the in-flight message continues using version `N` skills
- **AND** later messages use version `N+1` skills
