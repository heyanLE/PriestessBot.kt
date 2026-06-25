## ADDED Requirements

### Requirement: Dashboard HTTP server
The system SHALL provide an optional Ktor Dashboard API server that runs in the same process as the bot runtime.

#### Scenario: Server exposes health endpoint
- **WHEN** the Dashboard API server is running
- **THEN** `GET /health` returns a JSON health response
- **AND** the response includes overall status and component statuses

#### Scenario: Server exposes current configuration
- **WHEN** a dashboard client calls `GET /api/config`
- **THEN** the server returns the active `PriestessConfig` as JSON

#### Scenario: Server updates configuration
- **WHEN** a dashboard client calls `PUT /api/config` with a valid `PriestessConfig`
- **THEN** the server persists the new config
- **AND** publishes updated config flows to runtime controllers

### Requirement: Runtime management endpoints
The system SHALL expose management endpoints for platforms, providers, tools, conversations, and plugins.

#### Scenario: Platform list
- **WHEN** a dashboard client calls `GET /api/platforms`
- **THEN** the server returns configured platform entries with running status

#### Scenario: Provider list
- **WHEN** a dashboard client calls `GET /api/providers`
- **THEN** the server returns provider metadata for registered providers

#### Scenario: Tool list
- **WHEN** a dashboard client calls `GET /api/tools`
- **THEN** the server returns registered tool metadata

#### Scenario: Conversation history
- **WHEN** a dashboard client calls `GET /api/conversations/{id}/messages`
- **THEN** the server returns stored messages for that conversation

### Requirement: Log WebSocket foundation
The system SHALL provide a WebSocket endpoint for dashboard log/event streaming.

#### Scenario: WebSocket connection accepted
- **WHEN** a dashboard client connects to `GET /ws/logs`
- **THEN** the server accepts the WebSocket connection
- **AND** sends structured log or heartbeat events as JSON text frames
