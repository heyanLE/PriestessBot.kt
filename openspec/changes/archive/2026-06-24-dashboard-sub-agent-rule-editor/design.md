# Design

## Goals

- Make common sub-agent changes possible without raw JSON edits.
- Keep the existing JSON editor as an advanced escape hatch.
- Avoid introducing a separate draft store that can drift from the editor.

## Approach

The structured controls parse the current editor draft, mutate a cloned config object, then write the updated JSON back into the editor. Save and test continue to use the same draft JSON path.

## Structured Editing

- Runtime controls toggle `enabled` and select `defaultAgentName`.
- Agent creation uses lightweight defaults derived from the primary agent shape where possible.
- Route creation starts disabled or enabled with empty keywords, a target agent, and priority `0`.
- Route table rows expose target, keywords, priority, enabled, and remove controls.

## Validation

The existing JSON parse guard remains the gate for save and test actions. Structured controls are disabled if the draft cannot be parsed.
