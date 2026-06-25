## MODIFIED Requirements

### Requirement: Dashboard API client
The Dashboard frontend API client SHALL expose typed functions for backend Dashboard operations used by frontend views.

#### Scenario: Sub-agent endpoints are callable
- **WHEN** a frontend view needs sub-agent orchestration data
- **THEN** the API client provides typed functions for reading config, replacing config, and running a test execution
- **AND** the request and response types include selected agent, selected route, selection reason, events, and content
