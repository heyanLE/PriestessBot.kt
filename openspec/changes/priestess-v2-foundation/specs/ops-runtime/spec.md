## ADDED Requirements

### Requirement: Container deployment assets
The system SHALL provide container deployment assets for running PriestessBot in production-like environments.

#### Scenario: Docker image can run application distribution
- **WHEN** the Docker image is built
- **THEN** it packages the Gradle application distribution
- **AND** runs the configured application entrypoint

#### Scenario: Compose defines persistent paths
- **WHEN** docker compose is used
- **THEN** config, data, logs, and plugins paths are mounted as persistent volumes

### Requirement: Health check support
The system SHALL provide a health endpoint suitable for container health checks.

#### Scenario: Container health check
- **WHEN** the container health check runs
- **THEN** it calls `/health`
- **AND** treats a healthy response as success
