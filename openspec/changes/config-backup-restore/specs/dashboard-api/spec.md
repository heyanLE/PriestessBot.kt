## MODIFIED Requirements

### Requirement: Dashboard API SHALL expose config backup recovery

The Dashboard API SHALL allow operators to list config backups and restore a selected backup without exposing backup file contents.

#### Scenario: List config backup metadata

- **GIVEN** config backups exist
- **WHEN** an operator requests `GET /api/config/backups`
- **THEN** the response SHALL include backup IDs, creation timestamps, sizes, and paths
- **AND** the response SHALL NOT include config JSON contents or secrets

#### Scenario: Restore config backup

- **GIVEN** a valid backup ID exists
- **WHEN** an operator requests `POST /api/config/backups/{id}/restore`
- **THEN** the API SHALL restore the backup
- **AND** the response SHALL include the restored effective config
