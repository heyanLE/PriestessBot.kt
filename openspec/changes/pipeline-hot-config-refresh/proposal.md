# Pipeline Hot Config Refresh

## Why

Dashboard config updates publish new values, but `PipelineController` currently builds stages from a startup snapshot. That means sub-agent routing, pipeline limits, and primary Agent changes may require a restart before affecting new platform messages.

## What Changes

- Build runtime pipeline stages from the latest `ConfigCase.current()` for each incoming message.
- Keep existing test-only static stage construction for focused stage tests.
- Add coverage proving a later config update changes the selected sub-agent for subsequent messages without reconstructing the controller.

## Impact

- New messages observe current pipeline, primary Agent, and sub-agent config.
- In-flight messages keep the stage list built at their start.
- Provider/platform lifecycle remains unchanged.
