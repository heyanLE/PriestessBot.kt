# Pipeline Sub-Agent Routing

## Why

Sub-agent routing is available through Dashboard test APIs, but real platform messages still execute the primary Agent in the pipeline. v2 needs configured sub-agent routing to affect actual NapCat/local runtime message processing.

## What Changes

- Apply sub-agent selection during pipeline pre-processing before `AgentContext` creation.
- Preserve existing ReAct execution, tool use, conversation persistence, decoration, and response stages.
- Store selected sub-agent metadata in `PipelineContext.shared` for debugging and tests.

## Impact

- Runtime pipeline behavior changes when `subAgents.enabled=true`.
- Existing default behavior remains unchanged when sub-agent routing is disabled or no route/default matches.
