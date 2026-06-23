# Design

## Provider Maps

`ProviderController` keeps two maps:

- config providers: created from `configCase.current().providers`
- runtime providers: registered directly by plugins or tests through `register()`

Lookup combines runtime providers over config providers so plugin overrides keep their existing behavior.

## Refresh

The controller collects `providerConfigsFlow` in its task scope and rebuilds only config providers for each published provider config list.

## Compatibility

`register()` and `unregister()` continue to manage runtime providers. Config refresh does not remove runtime providers. Disabling a config provider removes only that config-backed instance.

## Non-Goals

- No provider instance graceful draining.
- No provider config validation beyond existing registry creation behavior.
- No platform lifecycle changes.
