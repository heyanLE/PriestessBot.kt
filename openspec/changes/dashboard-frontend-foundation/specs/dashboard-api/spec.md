## ADDED Requirements

### Requirement: Dashboard static frontend hosting
The Dashboard API server SHALL host the built Dashboard frontend from classpath resources.

#### Scenario: Root serves frontend shell
- **GIVEN** the Dashboard frontend assets are packaged
- **WHEN** an operator requests `/`
- **THEN** the server responds with the frontend `index.html`

#### Scenario: Nested route refresh works
- **GIVEN** the Dashboard frontend assets are packaged
- **WHEN** an operator requests a non-API nested route
- **THEN** the server responds with the frontend `index.html`

#### Scenario: API routes remain available
- **GIVEN** the Dashboard frontend assets are packaged
- **WHEN** an operator requests `/api/config`
- **THEN** the server responds with API JSON rather than the frontend shell
