# skill-management Specification

## Purpose
TBD - created by archiving change priestess-v1-core. Update Purpose after archive.
## Requirements

### Requirement: Skill interface
The system SHALL define `Skill` interface with `name`, `description`, `priority`, `canHandle()`, and `execute()` methods, and SHALL allow workspace snapshots to expose scoped skill sets and prompt documents.

#### Scenario: Skill handles a matching message
- **WHEN** `canHandle(message)` returns true
- **THEN** the Skill's `execute()` is called
- **AND** its response is returned

#### Scenario: Skill priority ordering
- **WHEN** multiple Skills return true for `canHandle()`
- **THEN** `SkillManager` invokes the highest priority skill first

#### Scenario: Workspace snapshot exposes skill descriptors
- **GIVEN** a workspace directory contains enabled skill folders
- **WHEN** the workspace snapshot is built
- **THEN** the snapshot exposes only the enabled workspace-scoped skill descriptors
- **AND** each descriptor carries `SKILL.md` path metadata instead of eager markdown content

#### Scenario: Agent explicitly loads skill markdown
- **GIVEN** a workspace-visible skill descriptor exists in the current pipeline skill state
- **WHEN** the agent calls `use_skill` for that skill
- **THEN** the runtime reads the referenced `SKILL.md` file at load time
- **AND** marks the skill as loaded for later system prompt renders in the same agent run

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

### Requirement: DefaultSkill
The system SHALL provide a `DefaultSkill` that acts as a fallback handler.

#### Scenario: Fallback handling
- **WHEN** no other Skill can handle a message
- **THEN** `DefaultSkill` provides a generic response

### Requirement: Skill permission declaration and loading view
The system SHALL allow Skills and workspace skill descriptors to declare a required permission group, defaulting to `OPERATOR`, and SHALL derive available skill references from the current sender's group.

#### Scenario: Super-administrator Skill is unavailable to lower roles
- **GIVEN** a Skill requires `SUPER_ADMIN`
- **AND** the sender is not a `SUPER_ADMIN`
- **WHEN** available workspace skills are assembled
- **THEN** that Skill is absent from the available names and cannot be loaded

#### Scenario: Administrator Skill is marked unavailable to an operator
- **GIVEN** a Skill requires `ADMIN`
- **AND** the sender is an `OPERATOR`
- **WHEN** available workspace skills are assembled
- **THEN** the Skill remains available to the model
- **AND** its rendered reference or prompt includes a current-permission-insufficient notice

### Requirement: Permission-enforced skill loading
The system SHALL enforce a Skill's required permission when the `use_skill` Tool attempts to load that Skill into `PipelineSkillState`.

#### Scenario: Operator cannot load an administrator Skill
- **GIVEN** a visible Skill requires `ADMIN`
- **AND** the sender is an `OPERATOR`
- **WHEN** the model calls `use_skill` for that Skill
- **THEN** the Skill document is not loaded into `PipelineSkillState`
- **AND** `use_skill` returns a `PERMISSION_DENIED` Tool result identifying the current and required groups and containing the configured persona denial wording

#### Scenario: Authorized sender loads a Skill
- **GIVEN** a Skill requires `ADMIN`
- **AND** the sender is an `ADMIN` or `SUPER_ADMIN`
- **WHEN** the model calls `use_skill` for that Skill
- **THEN** the Skill document is loaded into `PipelineSkillState`
