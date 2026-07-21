## ADDED Requirements

### Requirement: Dashboard API routes SHALL have contract test coverage
Dashboard API routes SHALL be covered by route contract tests that verify HTTP status, authentication behavior where applicable, request/response JSON shape, and service/controller delegation.

#### Scenario: Existing management routes are covered
- **WHEN** Dashboard API tests run
- **THEN** they SHALL cover health, metrics, config read/write/reload, platforms, providers, tools, conversations, plugins, logs WebSocket, sub-agent, knowledge, and Agent chat routes that exist in the runtime

#### Scenario: Authentication contracts are covered
- **WHEN** API token authentication is enabled or disabled
- **THEN** route tests SHALL verify protected routes reject missing or invalid credentials and public routes remain reachable

#### Scenario: Error response contracts are covered
- **WHEN** a route receives invalid input, references a missing resource, or the service layer returns a domain error
- **THEN** route tests SHALL verify the HTTP status and JSON error body are stable

#### Scenario: Future workspace routes require tests
- **WHEN** workspace Dashboard routes are introduced
- **THEN** route contract tests SHALL cover workspace listing, detail, reload, tools, MCP, skills, personas, and memory views

#### Scenario: Future persona and memory routes require tests
- **WHEN** persona or memory Dashboard routes are introduced
- **THEN** route contract tests SHALL cover create, update, list, search, delete, expire, and error paths for those APIs
