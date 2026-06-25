# Design

## Config

`SubAgentOrchestrationConfig` is added to `PriestessConfig`:

- `enabled`: opt-in runtime flag.
- `defaultAgentName`: optional fallback sub-agent name.
- `agents`: list of `SubAgentConfig`, each wrapping an `AgentConfig`.
- `routes`: list of `SubAgentRouteConfig` with name, target agent, keywords, and priority.

All fields have defaults so existing config files remain valid.

## Routing

`SubAgentOrchestrator` implements route mode only:

1. If orchestration is disabled, use the primary `AgentConfig`.
2. Sort enabled routes by priority descending.
3. Select the first route whose keyword appears in the input text.
4. If no route matches, use `defaultAgentName` when available.
5. If still unresolved, use the primary `AgentConfig`.

The selected Agent is executed with `ReActRunner.stepUntilDone()` using the same provider/tool/context dependencies as Dashboard Agent chat.

## Dashboard API

Routes:

- `GET /api/sub-agents/config`
- `PUT /api/sub-agents/config`
- `POST /api/sub-agents/test`

The test response includes selected agent, selected route, status, content, and execution events.
