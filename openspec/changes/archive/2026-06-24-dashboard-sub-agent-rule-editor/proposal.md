# Dashboard Sub-Agent Rule Editor

## Why

The Dashboard sub-agent page can edit raw JSON, but routine routing changes are still too easy to mistype. Operators need a structured editor for common tasks while keeping JSON available for advanced tuning.

## What Changes

- Add structured controls to the sub-agent page for orchestration enabled state and default agent.
- Add forms for creating/removing sub-agents and routes.
- Add inline controls for route enabled state, priority, target agent, and keywords.

## Impact

- Frontend-only enhancement over existing sub-agent APIs.
- No backend behavior changes.
- JSON editor remains the source of truth for save/test payloads.
