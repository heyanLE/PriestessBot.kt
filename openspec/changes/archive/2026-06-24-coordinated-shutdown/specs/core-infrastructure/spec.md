## MODIFIED Requirements

### Requirement: Runtime shutdown SHALL be coordinated

The application runtime SHALL coordinate shutdown across platform adapters, in-flight message processing, server resources, and long-lived controllers.

#### Scenario: Shutdown drains accepted pipeline work

- **GIVEN** a platform message has already been accepted by the pipeline
- **WHEN** runtime shutdown begins
- **THEN** platform adapters SHALL stop accepting new messages
- **AND** the pipeline SHALL wait for the accepted message job to finish before cancelling its scope

#### Scenario: Pipeline rejects new work after shutdown starts

- **GIVEN** pipeline shutdown has started
- **WHEN** a caller attempts to process a new message
- **THEN** the pipeline SHALL reject the new message job
- **AND** the rejected job SHALL complete without running stages

#### Scenario: Shutdown remains bounded

- **GIVEN** an in-flight pipeline job does not complete
- **WHEN** shutdown waits longer than the configured drain timeout
- **THEN** shutdown SHALL proceed by cancelling the pipeline scope

#### Scenario: Runtime resources stop in deterministic order

- **GIVEN** the application runtime is running
- **WHEN** shutdown is requested
- **THEN** platform adapters SHALL be stopped before pipeline drain
- **AND** the server SHALL be stopped before provider, tool, plugin, and database resources are released
