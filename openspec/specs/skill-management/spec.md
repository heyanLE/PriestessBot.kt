# skill-management Specification

## Purpose
TBD - created by archiving change priestess-v1-core. Update Purpose after archive.
## Requirements
### Requirement: Skill interface
The system SHALL define `Skill` interface with `name`, `description`, `priority`, `canHandle()`, and `execute()` methods.

#### Scenario: Skill handles a matching message
- **WHEN** `canHandle(message)` returns true
- **THEN** the Skill's `execute()` is called and its response is returned

#### Scenario: Skill priority ordering
- **WHEN** multiple Skills return true for `canHandle()`
- **THEN** `SkillManager` invokes the highest priority skill first

### Requirement: DefaultSkill
The system SHALL provide a `DefaultSkill` that acts as a fallback handler.

#### Scenario: Fallback handling
- **WHEN** no other Skill can handle a message
- **THEN** `DefaultSkill` provides a generic response

