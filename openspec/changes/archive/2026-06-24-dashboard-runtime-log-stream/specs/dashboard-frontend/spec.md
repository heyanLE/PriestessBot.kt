## MODIFIED Requirements

### Requirement: Realtime log stream

The Dashboard frontend SHALL connect to the backend log WebSocket and render runtime log events.

#### Scenario: Runtime log socket receives messages

- **GIVEN** the backend log socket is available
- **WHEN** the Log view opens
- **THEN** it connects to `/ws/logs`
- **AND** renders connected, buffered, and live log events
