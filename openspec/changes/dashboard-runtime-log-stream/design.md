# Design

## Log Hub

`DashboardLogHub` owns:

- a bounded `ArrayDeque<LogEventDto>` for recent events
- a shared flow for subscribers

The hub exposes:

- `publish(event)`
- `recent()`
- `events()`
- `clearForTest()`

The buffer defaults to 200 events, matching the frontend display cap.

## Logback Appender

`DashboardLogbackAppender` extends Logback `AppenderBase<ILoggingEvent>` and publishes formatted log events to `DashboardLogHub`.

The appender:

- maps Logback level to string
- includes logger name and formatted message in the event message
- uses the logging event timestamp
- ignores events emitted by Dashboard log streaming internals to avoid feedback loops

Both `logback.xml` and `logback-test.xml` attach the appender to root.

## WebSocket Route

`/ws/logs` sends:

1. the existing `connected` event
2. recent buffered events
3. new events from the shared flow until the socket closes

The route remains protected by Dashboard API token auth when configured.
