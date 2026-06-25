# Dashboard API Token Auth

## Why

The v2 Dashboard exposes operational APIs that can return or mutate sensitive runtime configuration. The foundation change intentionally shipped without auth for trusted local use, but NAS/LAN deployments now need a minimal protection layer before the Dashboard is casually exposed beyond localhost.

## What Changes

- Add optional Dashboard API token configuration to `ServerConfig`.
- Support `PRIESTESS_SERVER_API_TOKEN` as an environment override.
- Require `Authorization: Bearer <token>` for `/api/*` and `/ws/*` when a token is configured.
- Keep `/health`, `/metrics`, and static Dashboard assets public for health checks and page loading.
- Let the Dashboard frontend attach a configured token from local browser storage or URL query.

## Impact

- Existing local setups keep working when the token is blank.
- Operators can protect config, plugin, knowledge, chat, and log endpoints in NAS/LAN deployments.
- Health checks remain simple and unauthenticated.
