## MODIFIED Requirements

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
