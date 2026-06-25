## Why

The v2 runtime exposes `/health`, but the response is too shallow for real deployment troubleshooting. During NAS deployment, operators need to quickly confirm which config/database paths are in use, whether configured extension counts match expectations, and how long the process has been running.

## What Changes

- Extend `/health` with a `diagnostics` object while preserving existing `status`, `components`, and `timestamp` fields.
- Include lightweight runtime/config counts and paths useful for operations.
- Surface diagnostics on the Dashboard overview.
- Add API/frontend tests for the new health shape.

## Impact

- Backward compatible for existing health consumers.
- Improves NAS/Docker troubleshooting without adding a metrics backend.
- Keeps sensitive values such as provider API keys and platform tokens out of health output.
