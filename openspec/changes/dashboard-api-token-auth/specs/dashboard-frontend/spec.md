## MODIFIED Requirements

### Requirement: Dashboard frontend SHALL support API token access

The Dashboard frontend SHALL attach an operator-provided API token to protected backend calls.

#### Scenario: REST API token is stored from URL query

- **GIVEN** the Dashboard opens with `?token=secret`
- **WHEN** the frontend sends REST API requests
- **THEN** it SHALL include `Authorization: Bearer secret`

#### Scenario: Log socket token is sent through query parameter

- **GIVEN** a Dashboard API token is available in browser storage
- **WHEN** the Log view connects to `/ws/logs`
- **THEN** the WebSocket URL SHALL include the token query parameter
