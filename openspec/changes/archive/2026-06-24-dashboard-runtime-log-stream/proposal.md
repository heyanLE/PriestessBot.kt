# Dashboard Runtime Log Stream

## Why

The Dashboard has a Live Logs view and `/ws/logs` endpoint, but the endpoint only emits a synthetic `connected` event. In NAS deployments operators still need SSH and `docker logs` for actual runtime troubleshooting. The v2 ops surface should stream real runtime log events into the Dashboard.

## What Changes

- Add an in-process Dashboard log hub with a bounded recent-event buffer.
- Add a Logback appender that publishes application log events to the hub.
- Update `/ws/logs` to send recent events and then stream new events.
- Preserve the existing `connected` handshake event.

## Impact

- Operators can see runtime logs from the Dashboard without opening an SSH shell.
- Log streaming stays in-memory and bounded.
- Existing stdout/file logging behavior is unchanged.
