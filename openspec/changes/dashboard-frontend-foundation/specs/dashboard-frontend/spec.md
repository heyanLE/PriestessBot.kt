## ADDED Requirements

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
The Dashboard frontend SHALL use backend REST APIs for health, platforms, providers, tools, conversations, plugins, and configuration.

#### Scenario: Overview loads current runtime status
- **GIVEN** the backend Dashboard API is available
- **WHEN** the Dashboard overview opens
- **THEN** it requests current health and runtime lists
- **AND** displays the returned counts and statuses

### Requirement: Realtime log stream
The Dashboard frontend SHALL connect to the backend log WebSocket.

#### Scenario: Log socket receives messages
- **GIVEN** the backend log socket is available
- **WHEN** the Log view opens
- **THEN** it connects to `/ws/logs`
- **AND** renders received log events
