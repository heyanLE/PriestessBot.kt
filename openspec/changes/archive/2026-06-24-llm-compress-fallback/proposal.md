## Why

The v2 config model exposes `compressStrategy = "llm_compress"`, and `AgentCase` maps it into `CompressStrategy.LLM_COMPRESS`. The concrete `LLMCompressStrategy` still throws `NotImplementedError`, so a valid v2 configuration can crash a normal message flow when context compression is needed.

Until a real summarizing LLM compressor is wired, the strategy should be safe and deterministic.

## What Changes

- Replace the `NotImplementedError` in `LLMCompressStrategy` with a conservative fallback implementation.
- The fallback keeps the `llm_compress` strategy name and delegates to token-window compression.
- Add coverage that selecting `llm_compress` compresses without throwing and preserves recent context/system messages.
- Add coverage through `ContextManager` to ensure configured agents can use the strategy.

## Impact

- Operators can safely configure `llm_compress` before the full summarizer exists.
- Future work can replace the fallback internals without changing config names or strategy registration.
