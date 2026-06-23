# Runtime Metrics

## Why

The v2 ops roadmap calls for Prometheus metrics so operators can observe message throughput, LLM latency, tool calls, and runtime errors. The runtime currently exposes `/health`, but health only answers whether the process is alive and which paths/extensions are active. It does not provide counters or latency signals for dashboards and alerts.

## What Changes

- Add a lightweight in-process metrics registry that can render Prometheus text exposition.
- Expose `GET /metrics` from the Dashboard/API server.
- Record message pipeline attempts and outcomes.
- Record LLM provider call count and elapsed time from the pipeline execution path.
- Record tool call count and outcomes from the tool executor.

## Impact

- Operators can scrape the running NAS/Docker service without additional dependencies.
- Metrics are process-local and reset on restart.
- Labels are limited to low-cardinality runtime dimensions to keep the endpoint safe for long-running bots.
