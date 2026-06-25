# Design

## Runtime Pipeline

`PipelineController` should not store runtime stages built from a single config snapshot. Instead, it stores the dependencies needed to build stages and calls `buildStages()` at the start of `process()`.

The execution function receives the per-message stage list. This keeps in-flight execution stable while allowing the next message to use updated config.

## Test Pipeline

The internal test-only constructor still accepts explicit stages. It stores those stages in a static provider so existing focused tests can control the stage list.

## Scope

This change refreshes config consumed directly by pipeline stages:

- `agent`
- `subAgents`
- `pipeline`

It does not recreate provider/platform instances on provider/platform config changes.
