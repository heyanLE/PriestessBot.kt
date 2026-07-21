## MODIFIED Requirements

### Requirement: Skill interface
The system SHALL define `Skill` interface with `name`, `description`, `priority`, `canHandle()`, and `execute()` methods, and SHALL allow workspace snapshots to expose scoped skill descriptors that lazily load `SKILL.md` content.

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
