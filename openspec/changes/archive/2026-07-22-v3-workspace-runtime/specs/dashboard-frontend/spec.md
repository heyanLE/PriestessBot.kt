## MODIFIED Requirements

### Requirement: Operational navigation
The Dashboard frontend SHALL provide navigation for v2 and v3 operational areas.

#### Scenario: Navigate to workspace page
- **WHEN** an operator opens the Dashboard sidebar
- **THEN** a Workspaces navigation entry is visible
- **AND** selecting it opens `/workspaces`

### Requirement: API-backed status views
The Dashboard frontend SHALL use backend REST APIs for health, platforms, providers, tools, conversations, plugins, configuration, config backups, and workspaces.

#### Scenario: Workspace view lists statuses
- **GIVEN** the Dashboard frontend is open
- **WHEN** the Workspaces view loads
- **THEN** it requests `GET /api/workspaces`
- **AND** displays each workspace id, name, enabled state, active snapshot version, loaded timestamp, and last reload status

#### Scenario: Workspace detail shows scoped summaries
- **GIVEN** an operator selects a workspace
- **WHEN** the workspace detail panel loads
- **THEN** it displays scoped summaries for skills, MCP servers/tools, built-in/plugin tools, personas, and memory policy
- **AND** displays the active snapshot version and recent reload errors when present

#### Scenario: Operator reloads one workspace
- **GIVEN** a workspace is visible in the Workspaces view
- **WHEN** the operator triggers reload for that workspace
- **THEN** the frontend calls `POST /api/workspaces/{id}/reload`
- **AND** updates the displayed status from the returned reload result

#### Scenario: Operator reloads all workspaces
- **WHEN** the operator triggers reload all
- **THEN** the frontend calls `POST /api/workspaces/reload`
- **AND** updates each displayed workspace status from the returned results

#### Scenario: Failed reload keeps active snapshot visible
- **GIVEN** a workspace reload fails
- **WHEN** the Workspaces view refreshes
- **THEN** it shows the failure summary
- **AND** still shows the active snapshot version that remains in use

