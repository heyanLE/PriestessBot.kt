# Environment Config Overrides

## Why

NAS, Docker, and CI deployments often need to change runtime paths or server ports without editing the deployed `config.json`. The runtime already supports `PRIESTESS_CONFIG_PATH` for locating the config file and provider API-key environment fallbacks, but it does not yet support general operational overrides from the v2 ops roadmap.

## What Changes

- Apply selected environment variables after loading `PriestessConfig` from disk.
- Support server, database, and plugin runtime settings that are common in deployment scripts.
- Keep overrides in memory during load/reload so deployment environment values do not rewrite the config file automatically.
- Ignore malformed override values and keep the file/default value.

## Impact

- Operators can reuse the same config file across local, Docker, and NAS deployments.
- Hot reload still reloads file changes, then reapplies the active process environment.
- Secrets remain out of this feature; provider API keys continue to use the existing provider-specific resolver path.
