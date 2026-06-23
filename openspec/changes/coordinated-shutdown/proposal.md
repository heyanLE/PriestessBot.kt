# Coordinated Shutdown

## Why

The v2 ops roadmap calls for graceful shutdown on SIGTERM so in-flight message processing can finish before the process exits. The current shutdown hook stops platforms and the server, but it does not coordinate the pipeline, providers, tools, plugins, or database. `PipelineController.stop()` inherits immediate coroutine cancellation, which can interrupt an accepted message before persistence and response cleanup complete.

## What Changes

- Add a runtime lifecycle coordinator for shutdown.
- Stop platform adapters first so no new platform messages are accepted.
- Drain in-flight pipeline jobs before cancelling the pipeline scope.
- Stop server and long-lived runtime controllers in a deterministic order.
- Preserve a bounded timeout so shutdown can still finish if a task hangs.

## Impact

- NAS/Docker/SIGTERM shutdowns become safer for active conversations.
- Existing controller APIs stay small; only pipeline needs explicit drain behavior.
- Tests can verify shutdown order and that already accepted pipeline jobs are awaited.
