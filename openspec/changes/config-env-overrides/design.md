# Design

## Scope

This change targets operational values that are frequently different per host:

- `PRIESTESS_SERVER_ENABLED`
- `PRIESTESS_SERVER_HOST`
- `PRIESTESS_SERVER_PORT`
- `PRIESTESS_SERVER_CORS_ENABLED`
- `PRIESTESS_CONFIG_WATCH_ENABLED`
- `PRIESTESS_CONFIG_WATCH_INTERVAL_MILLIS`
- `PRIESTESS_DATABASE_PATH`
- `PRIESTESS_PLUGINS_ENABLED`
- `PRIESTESS_PLUGINS_DIRECTORY`
- `PRIESTESS_PLUGINS_AUTO_DISCOVER`

Provider secrets and provider endpoint overrides are intentionally excluded because providers already have API-key environment lookup and provider-specific runtime injection should be handled separately.

## Behavior

`ConfigController.load()` decodes the file-backed config, then applies an environment overlay before publishing it. File initialization and malformed-file recovery still write plain defaults to disk; the returned effective config receives overrides afterward.

Overrides are process-local. Calling `reload()` repeats file load plus overlay. Calling `save()` or `replace(..., persist = true)` keeps existing semantics and persists the config object the caller passes; this change only guarantees that passive load/reload does not write environment overrides back to disk.

Invalid values are ignored:

- Boolean values accept `true/false`, `1/0`, `yes/no`, and `y/n`.
- Integer and long values must parse successfully and satisfy the field's basic range.
- Port must be between 1 and 65535.
- Watch interval must be at least 250 milliseconds, matching watcher behavior.

## Testing

Config tests use an injectable environment provider so they do not mutate the process environment.
