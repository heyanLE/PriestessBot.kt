## Design

`ConfigController.load()` will continue to own all disk loading behavior. The logic becomes:

1. If the file does not exist, create and return the default config.
2. Read file text.
3. Remove a leading `\uFEFF` if present.
4. If the remaining text is blank, write defaults and return them.
5. Decode JSON normally.
6. On `SerializationException`, keep the existing backup and default rewrite behavior.

This keeps invalid JSON handling explicit while making common encoding and first-run placeholder cases safe.

## Alternatives Considered

- Fix only deployment to emit UTF-8 without BOM. This was already done, but existing user-created configs can still contain a BOM.
- Make tests stop pre-creating temp files. That reduces warning noise in tests but leaves production behavior fragile.
