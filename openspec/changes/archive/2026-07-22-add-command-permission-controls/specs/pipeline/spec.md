## ADDED Requirements

### Requirement: Permission and command stages
The system SHALL resolve a sender permission group after workspace preparation and before command dispatch, and SHALL run command dispatch before `PreProcessStage`.

#### Scenario: Command completion retains response stages
- **WHEN** a command is handled successfully or denied
- **THEN** the pipeline bypasses `PreProcessStage` and Agent processing
- **AND** it continues through result decoration and response delivery with the command response

#### Scenario: Permission metadata reaches Agent execution
- **WHEN** a non-command message continues past the permission stage
- **THEN** its resolved sender ID and permission group are available to Agent Tool execution
