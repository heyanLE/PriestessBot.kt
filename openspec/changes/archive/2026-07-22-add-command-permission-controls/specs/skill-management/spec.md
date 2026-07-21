## ADDED Requirements

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
