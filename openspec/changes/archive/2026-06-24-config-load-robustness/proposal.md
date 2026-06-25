## Why

Operational deployment exposed that valid UTF-8 JSON written with a byte order mark can be rejected during startup. Some tests also create empty temporary config files, which currently triggers noisy parse warnings and backup writes before falling back to defaults.

Both cases make v2 hot configuration feel brittle: a harmless editor or PowerShell encoding choice can cause the runtime to overwrite intended config with defaults, and empty seed files look like corrupted config.

## What Changes

- Load config files after stripping a leading UTF-8 BOM.
- Treat empty or whitespace-only existing config files as an uninitialized config and persist defaults without warning/backing up.
- Preserve the existing backup-and-default behavior for genuinely malformed JSON.
- Add tests covering BOM input and empty seed files.

## Impact

- Reduces deployment footguns for NAS/Windows-generated config files.
- Keeps existing config schema and public API unchanged.
- Improves test signal by avoiding expected empty-file warning noise.
