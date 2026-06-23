# Builtin Anthropic And Gemini Providers

## Why

The v2 provider model already reserves `ANTHROPIC` and `GEMINI` kinds, but the built-in registry only materializes OpenAI-compatible and Ollama providers. Operators who use Claude or Gemini must currently rely on external compatibility gateways instead of first-class provider configs.

## What Changes

- Add built-in Anthropic and Gemini `ChatProvider` adapters.
- Register `type = "anthropic"` and `type = "gemini"` in the built-in provider registry.
- Support non-streaming text chat and model listing/connectivity tests for both adapters.
- Keep tool calling, vision, and streaming marked unsupported until their schemas are mapped end to end.

## Impact

- v2 deployments can configure Claude and Gemini providers directly.
- Existing OpenAI/Ollama provider behavior is unchanged.
- Dashboard provider listings and tests automatically include configured Anthropic/Gemini providers.
