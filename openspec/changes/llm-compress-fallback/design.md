## Design

`LLMCompressStrategy` remains the strategy object registered for `CompressStrategy.LLM_COMPRESS`, but its implementation becomes a safe fallback:

- It accepts a `TokenCounter` dependency.
- It delegates to `TokenWindowStrategy` using the same `messages`, `systemMessage`, `maxTokens`, and `maxRounds`.
- It keeps `name = "llm_compress"` so telemetry/config surfaces still reflect the selected strategy.

This creates a stable extension point. A later summarizer can inject a provider-backed summarization step and then fall back to the token window if summarization fails.

## Non-Goals

- No provider-backed summarization call in this change.
- No new prompt or summary message schema.
- No streaming compression behavior.
