# dashboard-api Specification

## Purpose
TBD - created by archiving change sub-agent-routing-foundation. Update Purpose after archive.
## Requirements
### Requirement: Sub-agent Dashboard API
The Dashboard API SHALL expose sub-agent orchestration config and test routes.

#### Scenario: Read orchestration config
- **GIVEN** the Dashboard API is running
- **WHEN** an operator requests `/api/sub-agents/config`
- **THEN** the API returns the current orchestration config

#### Scenario: Replace orchestration config
- **GIVEN** the Dashboard API is running
- **WHEN** an operator puts a new orchestration config
- **THEN** the API persists and returns the updated config

#### Scenario: Test route execution
- **GIVEN** orchestration config is present
- **WHEN** an operator posts a test message
- **THEN** the API returns selected agent, selected route, status, content, and events

### Requirement: Knowledge Dashboard API
The Dashboard API SHALL expose knowledge base management and search routes.

#### Scenario: Create base through API
- **GIVEN** the Dashboard API is running
- **WHEN** an operator posts a knowledge base create request
- **THEN** the API returns the created knowledge base

#### Scenario: Add document through API
- **GIVEN** a knowledge base exists
- **WHEN** an operator posts a text document to that base
- **THEN** the API returns stored chunk metadata

#### Scenario: Test search through API
- **GIVEN** stored knowledge chunks exist
- **WHEN** an operator posts a search request
- **THEN** the API returns matching chunks and scores

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

### Requirement: Conversation message detail API coverage
The Dashboard API SHALL expose stored message history for a conversation.

#### Scenario: Messages are returned in chronological order
- **GIVEN** a conversation has multiple stored messages
- **WHEN** the operator requests `/api/conversations/{id}/messages`
- **THEN** messages are returned oldest to newest

#### Scenario: Message limit returns recent history
- **GIVEN** a conversation has more messages than the requested count
- **WHEN** the operator requests `/api/conversations/{id}/messages?count=2`
- **THEN** the response contains the two most recent messages in chronological order

#### Scenario: Tool metadata is preserved
- **GIVEN** a stored assistant message has tool calls and a stored tool message has a tool call id
- **WHEN** the operator requests the conversation messages
- **THEN** the response includes the tool call payload and tool call id

### Requirement: Agent chat test API
The Dashboard API SHALL expose a synchronous Agent chat test endpoint.

#### Scenario: Agent returns final response
- **GIVEN** the configured provider is available
- **WHEN** the operator posts a user message to `/api/agent/chat`
- **THEN** the server runs the configured Agent runner
- **AND** returns the final response content

#### Scenario: Provider missing
- **GIVEN** the configured Agent references a missing provider
- **WHEN** the operator posts a user message to `/api/agent/chat`
- **THEN** the server returns an Agent chat response with status `ERROR`
- **AND** includes an error message naming the missing provider

### Requirement: Agent chat events
The Dashboard API SHALL include ordered Agent execution events in chat responses.

#### Scenario: Tool execution is captured
- **GIVEN** the Agent runner executes a tool during chat
- **WHEN** the chat response is returned
- **THEN** it includes tool start and tool end events with the tool name

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

### Requirement: Dashboard health endpoint SHALL expose operational diagnostics

The Dashboard health endpoint SHALL include lightweight diagnostics that help operators verify runtime wiring without exposing secrets.

#### Scenario: Health response includes diagnostics

- **WHEN** a client requests `GET /health`
- **THEN** the response SHALL include `status`, `components`, `timestamp`, and `uptimeMillis`
- **AND** the response SHALL include a `diagnostics` object
- **AND** diagnostics SHALL include config path, database path, configured platform count, running platform count, configured provider count, available provider count, registered tool count, configured plugin count, and loaded plugin extension count

#### Scenario: Health diagnostics exclude secrets

- **GIVEN** config contains provider API keys or platform tokens
- **WHEN** a client requests `GET /health`
- **THEN** the diagnostics SHALL NOT include provider API keys or platform tokens

### Requirement: Dashboard API client
The Dashboard frontend API client SHALL expose typed functions for backend Dashboard operations used by frontend views.

#### Scenario: Sub-agent endpoints are callable
- **WHEN** a frontend view needs sub-agent orchestration data
- **THEN** the API client provides typed functions for reading config, replacing config, and running a test execution
- **AND** the request and response types include selected agent, selected route, selection reason, events, and content

### Requirement: Dashboard log WebSocket SHALL stream runtime logs

The Dashboard log WebSocket SHALL stream recent and live runtime log events.

#### Scenario: Log socket sends recent buffered events

- **GIVEN** runtime log events were emitted before a Dashboard log socket connects
- **WHEN** a client connects to `/ws/logs`
- **THEN** the socket SHALL send the connected event
- **AND** send buffered recent log events

#### Scenario: Log socket sends live events

- **GIVEN** a client is connected to `/ws/logs`
- **WHEN** the runtime emits a new log event
- **THEN** the socket SHALL send that log event to the client

#### Scenario: Log buffer remains bounded

- **GIVEN** more runtime log events are emitted than the Dashboard buffer size
- **WHEN** recent events are requested
- **THEN** only the most recent bounded set SHALL be retained

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
