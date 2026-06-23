# Dashboard Sub-Agent View

## Why

The sub-agent routing runtime exposes Dashboard APIs, but operators still need to edit JSON elsewhere or call endpoints manually. v2 needs a practical Dashboard workflow for inspecting routing rules, saving orchestration config, and testing a prompt against the selected sub-agent.

## What Changes

- Add Dashboard frontend API types and functions for sub-agent config and test execution.
- Add a `/sub-agents` Dashboard route and sidebar entry.
- Implement an operational sub-agent view with config editing, save/reset actions, route summary, and test execution results.

## Impact

- Frontend-only feature over the existing sub-agent Dashboard API.
- No backend API behavior changes.
- No runtime pipeline behavior changes.
