## MODIFIED Requirements

### Requirement: Platform runtime management
The system SHALL expose configured platform status and allow runtime enable/disable through management APIs.

#### Scenario: Platform status exposed
- **WHEN** a dashboard client requests platform status
- **THEN** configured platforms include whether their adapter is currently running

#### Scenario: Platform disabled through API
- **WHEN** a dashboard client disables a platform
- **THEN** the platform config is updated
- **AND** the platform controller stops the adapter
