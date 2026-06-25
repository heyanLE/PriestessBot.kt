# Design

## Metrics Registry

Add a small `MetricsRegistry` with thread-safe counters and duration totals. It exposes:

- `incrementCounter(name, labels, amount)`
- `recordDuration(name, labels, durationMillis)`
- `renderPrometheus()`

The registry avoids external dependencies and only supports the metric types needed for this slice. It emits `# HELP` and `# TYPE` lines for known metrics, then sorted samples for deterministic tests.

## Metrics

- `priestess_pipeline_messages_total{platform,status}`: message pipeline attempts, labelled by platform and `completed`/`failed`.
- `priestess_pipeline_duration_milliseconds_count{platform,status}` and `_sum{platform,status}`: pipeline processing duration.
- `priestess_llm_requests_total{provider,status}`: LLM requests issued by `ProcessStage`, labelled by provider and `success`/`error`.
- `priestess_llm_request_duration_milliseconds_count{provider,status}` and `_sum{provider,status}`: LLM request elapsed time.
- `priestess_tool_calls_total{tool,status}`: tool calls executed by `ToolExecutor`, labelled by tool and `success`/`error`.

`status` is intentionally coarse. Error messages, user IDs, session IDs, prompts, and tool arguments are not emitted.

## Integration

`MetricsRegistry` is registered in DI and injected into:

- `PipelineController`, to measure each accepted platform message.
- `ProcessStage`, to measure the ReAct/provider execution path.
- `ToolExecutor`, to count tool executions.
- `DashboardService` or routes, to expose `GET /metrics`.

Tests can use a fresh registry per fixture to avoid cross-test contamination.
