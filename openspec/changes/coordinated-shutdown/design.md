# Design

## Runtime Lifecycle

Introduce `PriestessRuntime`, a small coordinator built from existing singleton controllers:

1. Start the server.
2. On shutdown, stop platforms to terminate adapters and prevent new platform messages.
3. Drain pipeline jobs with a configurable timeout.
4. Stop the server.
5. Stop plugin, provider, tool, conversation, knowledge, database, and config controllers.

`main()` uses the coordinator in the shutdown hook instead of manually stopping only platform and server.

## Pipeline Drain

`PipelineController` tracks jobs started by `process(event)` separately from controller background tasks. `drain(timeoutMillis)` waits for a snapshot of active message jobs to finish. `stop()` calls drain first and then falls back to cancelling the controller scope through `BaseController.stop()`.

New calls to `process(event)` after shutdown begins return a cancelled job and log the rejection. This keeps platform shutdown and direct callers from enqueueing new work during drain.

## Timeout

The default pipeline drain timeout is 10 seconds. If jobs do not finish, shutdown proceeds to cancellation so the process can exit. Tests use shorter timeouts.

## Testing

- Pipeline stop waits for an accepted message job to finish.
- Pipeline rejects new work after shutdown begins.
- Runtime coordinator invokes components in the intended order.
