# dashboard-frontend Specification

## Purpose
TBD - created by archiving change dashboard-frontend-foundation. Update Purpose after archive.
## Requirements
### Requirement: Dashboard frontend application
The system SHALL include a Vue-based Dashboard frontend that can be built independently from the Kotlin backend.

#### Scenario: Frontend build
- **GIVEN** the frontend dependencies are installed
- **WHEN** the operator runs the frontend build command
- **THEN** the build produces static assets under the frontend distribution directory

### Requirement: Operational navigation
The Dashboard frontend SHALL provide navigation for v2 operational areas.

#### Scenario: Operator moves between sections
- **GIVEN** the Dashboard frontend is open
- **WHEN** the operator selects a navigation item
- **THEN** the matching view is shown without a full page reload

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

### Requirement: Realtime log stream
The Dashboard frontend SHALL connect to the backend log WebSocket.

#### Scenario: Log socket receives messages
- **GIVEN** the backend log socket is available
- **WHEN** the Log view opens
- **THEN** it connects to `/ws/logs`
- **AND** renders received log events

#### Scenario: Runtime log socket receives messages

- **GIVEN** the backend log socket is available
- **WHEN** the Log view opens
- **THEN** it connects to `/ws/logs`
- **AND** renders connected, buffered, and live log events
### Requirement: Knowledge management view
The Dashboard frontend SHALL provide a Knowledge view for operator-managed RAG content.

#### Scenario: Operator opens Knowledge view
- **GIVEN** the Dashboard is running
- **WHEN** the operator opens `/knowledge`
- **THEN** the frontend loads and displays knowledge bases

#### Scenario: Operator creates knowledge base
- **GIVEN** the Knowledge view is open
- **WHEN** the operator submits a new base name
- **THEN** the frontend calls the knowledge base create API
- **AND** refreshes the displayed base list

#### Scenario: Operator adds text document
- **GIVEN** a knowledge base exists
- **WHEN** the operator submits document text
- **THEN** the frontend calls the add document API
- **AND** displays the stored chunk count

#### Scenario: Operator tests retrieval
- **GIVEN** indexed document chunks exist
- **WHEN** the operator submits a search query
- **THEN** the frontend calls the knowledge search API
- **AND** displays scored result snippets

### Requirement: Conversation detail view
The Dashboard frontend SHALL provide a detail view for a selected conversation.

#### Scenario: Operator opens a conversation
- **GIVEN** the conversation list is visible
- **WHEN** the operator selects a conversation
- **THEN** the Dashboard navigates to `/conversations/{id}`
- **AND** loads the message history for that conversation

### Requirement: Message transcript rendering
The conversation detail view SHALL render message roles, content, timestamps, and tool metadata.

#### Scenario: Tool messages are visible
- **GIVEN** a conversation contains assistant tool calls and tool responses
- **WHEN** the operator opens the conversation detail view
- **THEN** tool call payloads and tool call ids are displayed alongside the transcript

### Requirement: Agent Dashboard view
The Dashboard frontend SHALL include an Agent view for inspecting and testing the active Agent.

#### Scenario: Operator opens Agent view
- **GIVEN** the Dashboard has loaded runtime config
- **WHEN** the operator opens `/agent`
- **THEN** the view displays the active Agent name, provider, model, limits, available providers, and available tools

### Requirement: Agent test chat
The Agent view SHALL let operators send a test message to the Agent chat API.

#### Scenario: Operator tests a message
- **GIVEN** the Agent view is open
- **WHEN** the operator sends a message
- **THEN** the frontend posts it to `/api/agent/chat`
- **AND** displays both the user message and Agent response
- **AND** displays returned execution events

### Requirement: Dashboard overview SHALL show runtime diagnostics

The Dashboard overview SHALL show health diagnostics returned by the backend.

#### Scenario: Operator inspects diagnostics

- **GIVEN** the health response contains diagnostics
- **WHEN** the operator opens the Dashboard overview
- **THEN** the overview SHALL render the diagnostic key/value pairs

### Requirement: Sub-agent dashboard page
The Dashboard frontend SHALL provide a sub-agent routing page for managing orchestration configuration.

#### Scenario: Structured runtime controls update draft config
- **GIVEN** the sub-agent page has a valid draft config
- **WHEN** an operator toggles orchestration or changes the default agent
- **THEN** the JSON editor draft is updated with the new config value

#### Scenario: Structured agent editing updates draft config
- **GIVEN** the sub-agent page has a valid draft config
- **WHEN** an operator adds or removes a sub-agent
- **THEN** the JSON editor draft is updated
- **AND** route/default references to removed agents are cleaned up

#### Scenario: Structured route editing updates draft config
- **GIVEN** the sub-agent page has a valid draft config with at least one agent
- **WHEN** an operator adds, edits, enables/disables, or removes a route
- **THEN** the JSON editor draft reflects the route changes
- **AND** save/test actions use the updated draft

#### Scenario: Navigate to sub-agent page
- **WHEN** an operator opens the Dashboard sidebar
- **THEN** a Sub-Agents navigation entry is visible
- **AND** selecting it opens `/sub-agents`

#### Scenario: Edit and save sub-agent config
- **GIVEN** the sub-agent page has loaded the current config
- **WHEN** an operator edits valid JSON and saves it
- **THEN** the frontend sends the config to the sub-agent config API
- **AND** the page updates the editor and summary from the saved response

#### Scenario: Reject invalid config JSON
- **GIVEN** the sub-agent page is open
- **WHEN** an operator enters invalid JSON
- **THEN** save and test actions show an inline error instead of calling the API

### Requirement: Sub-agent test runner
The Dashboard frontend SHALL allow operators to test routing and selected-agent execution.

#### Scenario: Test draft config
- **GIVEN** the sub-agent page has a valid draft config
- **WHEN** an operator submits a test message
- **THEN** the frontend calls the sub-agent test API with the draft config
- **AND** displays status, selected agent, selected route, selection reason, response content, and execution events

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
