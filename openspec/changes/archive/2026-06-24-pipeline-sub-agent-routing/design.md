# Design

## Placement

Selection belongs in `PreProcessStage` because that stage already chooses the Agent config, creates `AgentContext`, and loads conversation history. `ProcessStage` should continue to execute whatever `AgentContext` it receives.

## Selection

`PreProcessStage` uses `SubAgentOrchestrator.select()` with:

- current message text
- primary `agent` config
- current `subAgents` config

It then creates the selected Agent and records selection metadata in `PipelineContext.shared`.

## Compatibility

When orchestration is disabled, selection returns the primary Agent. This preserves existing behavior and avoids changing pipeline stage ordering.

## Observability

The stage stores:

- `subAgentSelectionAgent`
- `subAgentSelectionRoute`
- `subAgentSelectionReason`

These keys support tests and future Dashboard/log inspection without introducing a new persistence schema yet.
