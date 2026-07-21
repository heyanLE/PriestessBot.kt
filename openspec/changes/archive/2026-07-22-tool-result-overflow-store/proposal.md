## Why

Large successful tool outputs currently enter the Agent message history verbatim. A single `fetch_url`, `web_extract`, MCP, or terminal result can consume the context budget and force loss of useful conversation history; `fetch_url` and `web_extract` also only offer destructive character truncation. The runtime needs a bounded way for an Agent to retain access to complete output without sending it wholesale to the provider.

## What Changes

- Add a runtime-scoped overflow store for successful tool outputs that exceed a configurable inline token budget.
- Replace oversized inline output with a bounded preview, an opaque result reference, size/truncation metadata, and instructions for retrieving more content.
- Add a safe, paginated `read_tool_result` built-in tool that returns bounded character windows from a referenced result.
- Bind each stored result to its originating conversation, reject cross-conversation access, and clean up expired or shutdown-time data.
- Materialize overflow output in the shared Agent tool-result path so built-in, workspace, plugin, and MCP tools receive the same protection.
- Update `fetch_url` and `web_extract` behavior to report source truncation separately from runtime inline-result materialization.

## Capabilities

### New Capabilities

- `tool-result-overflow`: Store oversized successful tool results outside the model context and expose scoped, paginated retrieval.

### Modified Capabilities

None.

## Impact

- Affects Agent/ReAct result handling, tool registration and policy, runtime shutdown, and tool-context ownership metadata.
- Adds a temporary runtime storage component plus configuration for inline budget, preview budget, TTL, and storage capacity.
- Adds a new safe-read tool schema and tests for retrieval, authorization, expiration, cleanup, and context-budget behavior.
- Changes the shape of oversized successful `fetch_url` and `web_extract` tool observations; no platform, provider, or external service dependency is introduced.
