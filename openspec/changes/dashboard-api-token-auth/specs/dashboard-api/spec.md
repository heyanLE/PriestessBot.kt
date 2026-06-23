## MODIFIED Requirements

### Requirement: Dashboard API optional token authentication

The Dashboard API SHALL support optional bearer token authentication for operational endpoints.

#### Scenario: Blank token keeps local development open

- **GIVEN** server config has a blank API token
- **WHEN** a client requests a Dashboard API endpoint
- **THEN** the request SHALL be handled without authentication

#### Scenario: Configured token protects management APIs

- **GIVEN** server config has a non-blank API token
- **WHEN** a client requests `/api/config` without `Authorization: Bearer <token>`
- **THEN** the server SHALL return `401 Unauthorized`

#### Scenario: Correct bearer token allows management APIs

- **GIVEN** server config has a non-blank API token
- **WHEN** a client requests `/api/config` with `Authorization: Bearer <token>`
- **THEN** the server SHALL handle the request normally

#### Scenario: Health and metrics stay public

- **GIVEN** server config has a non-blank API token
- **WHEN** a client requests `/health` or `/metrics`
- **THEN** the server SHALL handle the request without authentication

#### Scenario: Log WebSocket is protected

- **GIVEN** server config has a non-blank API token
- **WHEN** a client connects to `/ws/logs` without a valid token
- **THEN** the server SHALL reject the request
