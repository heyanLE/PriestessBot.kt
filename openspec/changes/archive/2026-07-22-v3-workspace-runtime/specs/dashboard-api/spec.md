## MODIFIED Requirements

### Requirement: Runtime management endpoints
The system SHALL expose management endpoints for platforms, providers, tools, conversations, plugins, and workspaces.

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

### Requirement: Dashboard API client
The Dashboard frontend API client SHALL expose typed functions for backend Dashboard operations used by frontend views, including workspace operations.

#### Scenario: Workspace endpoints are callable
- **WHEN** a frontend view needs workspace data
- **THEN** the API client provides typed functions for listing workspaces, reading workspace detail, reloading one workspace, reloading all workspaces, and reading scoped tools, MCP servers, skills, personas, and memory summaries

