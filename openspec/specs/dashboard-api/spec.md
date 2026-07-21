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
The system SHALL expose management endpoints for platforms, providers, tools, conversations, plugins, and workspaces.

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

#### Scenario: Workspace list
- **WHEN** a dashboard client calls `GET /api/workspaces`
- **THEN** the server returns workspace status entries with id, name, enabled state, active snapshot version, loaded timestamp, last reload result, and diagnostic summary

#### Scenario: Workspace detail
- **GIVEN** a workspace has an active snapshot
- **WHEN** a dashboard client calls `GET /api/workspaces/{id}`
- **THEN** the server returns workspace detail including config summary, active snapshot version, reload status, resolution metadata, and scoped resource counts
- **AND** secret values are redacted

#### Scenario: Reload workspace
- **WHEN** a dashboard client calls `POST /api/workspaces/{id}/reload`
- **THEN** the server reloads that workspace
- **AND** returns a `WorkspaceReloadResult`

#### Scenario: Reload all workspaces
- **WHEN** a dashboard client calls `POST /api/workspaces/reload`
- **THEN** the server reloads all configured workspaces
- **AND** returns per-workspace `WorkspaceReloadResult` entries

#### Scenario: Workspace scoped resource endpoints
- **GIVEN** a workspace has an active snapshot
- **WHEN** a dashboard client calls `/api/workspaces/{id}/tools`, `/api/workspaces/{id}/mcp`, `/api/workspaces/{id}/skills`, `/api/workspaces/{id}/personas`, or `/api/workspaces/{id}/memory`
- **THEN** the server returns the scoped resource summary for that workspace snapshot
- **AND** omits or redacts secrets

#### Scenario: Failed reload status is visible
- **GIVEN** a workspace reload failed
- **WHEN** a dashboard client requests workspace status or detail
- **THEN** the response includes the failed reload status, timestamp, and error summary
- **AND** reports the still-active snapshot version

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
The Dashboard frontend API client SHALL expose typed functions for backend Dashboard operations used by frontend views, including workspace operations.

#### Scenario: Sub-agent endpoints are callable
- **WHEN** a frontend view needs sub-agent orchestration data
- **THEN** the API client provides typed functions for reading config, replacing config, and running a test execution
- **AND** the request and response types include selected agent, selected route, selection reason, events, and content

#### Scenario: Workspace endpoints are callable
- **WHEN** a frontend view needs workspace data
- **THEN** the API client provides typed functions for listing workspaces, reading workspace detail, reloading one workspace, reloading all workspaces, and reading scoped tools, MCP servers, skills, personas, and memory summaries

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

### Requirement: Persona Dashboard API
The Dashboard API SHALL expose CRUD routes for operator-managed personas.

#### Scenario: List personas
- **GIVEN** personas exist for a workspace
- **WHEN** an operator requests `GET /api/personas`
- **THEN** the API returns personas with id, workspace id, name, description, tone, boundaries, prompt template, enabled state, agent names, and timestamps

#### Scenario: Create persona
- **GIVEN** the Dashboard API is running
- **WHEN** an operator posts a valid persona create request to `POST /api/personas`
- **THEN** the API persists and returns the created persona

#### Scenario: Update persona
- **GIVEN** a persona exists
- **WHEN** an operator sends `PUT /api/personas/{id}` with valid changes
- **THEN** the API persists and returns the updated persona

#### Scenario: Delete persona
- **GIVEN** a persona exists
- **WHEN** an operator sends `DELETE /api/personas/{id}`
- **THEN** the persona is removed from list and resolve results

### Requirement: Memory Dashboard API
The Dashboard API SHALL expose list, create, search, delete, and expiry routes for memory records.

#### Scenario: List memory
- **GIVEN** memory records exist
- **WHEN** an operator requests `GET /api/memory` with optional filters
- **THEN** the API returns matching non-deleted and non-expired memory records

#### Scenario: Create memory
- **GIVEN** the Dashboard API is running
- **WHEN** an operator posts a valid memory create request to `POST /api/memory`
- **THEN** the API persists and returns the created memory record

#### Scenario: Search memory
- **GIVEN** memory records exist for the requested scope context
- **WHEN** an operator posts a query to `POST /api/memory/search`
- **THEN** the API returns matching memory records with scores and match reasons

#### Scenario: Delete memory
- **GIVEN** a memory record exists
- **WHEN** an operator sends `DELETE /api/memory/{id}`
- **THEN** the memory record is excluded from future list, search, recall, and injection results

#### Scenario: Expire memory
- **GIVEN** expired memory records exist
- **WHEN** an operator sends `POST /api/memory/expire`
- **THEN** the API runs expiry cleanup and returns the number of affected records

### Requirement: Agent chat injection trace API
The Dashboard API SHALL include persona and memory injection trace in Agent chat test responses.

#### Scenario: Chat response includes injection trace
- **GIVEN** persona or memory injection runs during `/api/agent/chat`
- **WHEN** the chat response is returned
- **THEN** the response includes injected persona id and name when present
- **AND** includes injected memory ids, scores, and match reasons

#### Scenario: Chat response omits empty trace details
- **GIVEN** no persona or memory is injected
- **WHEN** the chat response is returned
- **THEN** the response remains valid
- **AND** the trace is empty or explicitly reports no injection

### Requirement: Dashboard Tool API SHALL expose tool policy state
The Dashboard API SHALL expose tool permission, enablement, source, and runtime status metadata.

#### Scenario: Tool list includes policy state
- **WHEN** a dashboard client calls `GET /api/tools`
- **THEN** each tool item SHALL include name, description, source, owner when available, risk level, required capabilities, default enabled state, effective enabled state, audit flag, and status reason when unavailable

#### Scenario: Tool list includes built-in and plugin tools
- **GIVEN** built-in and plugin tools are registered
- **WHEN** a dashboard client calls `GET /api/tools`
- **THEN** the response SHALL include both built-in and plugin tools with their source classification

#### Scenario: Tool health does not expose secrets
- **GIVEN** a tool is backed by provider, platform, plugin, memory, reminder, or network configuration
- **WHEN** a dashboard client requests tool metadata
- **THEN** the response SHALL NOT include API keys, tokens, prompts, message bodies, or memory contents

### Requirement: Dashboard health data SHALL align with health_check tool
The Dashboard API health data SHALL provide the same non-sensitive operational components used by the `health_check` built-in tool.

#### Scenario: Health components align
- **WHEN** a client requests `GET /health`
- **THEN** the response SHALL include component and diagnostic categories for database, providers, platforms, plugins, tools, and workspace reload state

#### Scenario: Health alignment preserves public endpoint safety
- **WHEN** a client requests `GET /health`
- **THEN** the response SHALL remain non-sensitive and suitable for unauthenticated health checks when Dashboard token auth leaves health public
