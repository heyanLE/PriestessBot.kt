## MODIFIED Requirements

### Requirement: API-backed status views

The Dashboard frontend SHALL use backend REST APIs for health, platforms, providers, tools, conversations, plugins, configuration, and config backups.

#### Scenario: Runtime config view lists backups

- **GIVEN** the Dashboard frontend is open
- **WHEN** the Runtime Config view loads
- **THEN** it SHALL request `GET /api/config/backups`
- **AND** display backup IDs, creation timestamps, sizes, and paths without config contents

#### Scenario: Runtime config view restores backup

- **GIVEN** a config backup is listed
- **WHEN** an operator selects restore for that backup
- **THEN** the Dashboard SHALL call `POST /api/config/backups/{id}/restore`
- **AND** update the active config editor with the restored config
