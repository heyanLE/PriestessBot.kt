## ADDED Requirements

### Requirement: Reload failure rollback SHALL have test coverage
Config and workspace reload behavior SHALL have tests proving invalid reload attempts do not replace the active runtime snapshot.

#### Scenario: Invalid config reload preserves active config
- **GIVEN** a valid active config is already published
- **WHEN** a reload reads invalid config data
- **THEN** a test SHALL verify the active config remains unchanged and the reload result reports failure

#### Scenario: Invalid workspace reload preserves active snapshot
- **GIVEN** a valid workspace snapshot is active
- **WHEN** workspace reload validation fails or snapshot construction fails
- **THEN** a test SHALL verify the old snapshot remains active for new message resolution

#### Scenario: In-flight messages keep prior snapshot
- **GIVEN** a message has entered the pipeline with a resolved config or workspace snapshot
- **WHEN** a later reload publishes a new snapshot
- **THEN** a test SHALL verify the in-flight message continues using the snapshot captured at pipeline entry
