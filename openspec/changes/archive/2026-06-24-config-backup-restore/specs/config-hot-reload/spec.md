## MODIFIED Requirements

### Requirement: Runtime config persistence SHALL support backup and restore

The config controller SHALL maintain timestamped backups for persisted config replacements and allow restoring a previous valid config.

#### Scenario: Persisted replacement creates a timestamped backup

- **GIVEN** an active config file already exists with non-empty content
- **WHEN** the runtime persists a replacement config
- **THEN** a timestamped backup of the previous file content SHALL be created
- **AND** the replacement config SHALL become the active config

#### Scenario: Backup restore publishes config slices

- **GIVEN** a timestamped backup contains a valid config
- **WHEN** the backup is restored
- **THEN** the active config file SHALL contain the restored config
- **AND** config state flows SHALL publish the restored values

#### Scenario: Restore rejects unknown backup IDs

- **GIVEN** a caller provides an ID that is not a known backup file
- **WHEN** restore is requested
- **THEN** restore SHALL fail without modifying the active config
