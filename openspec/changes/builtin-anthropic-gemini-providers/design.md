# Design

## Provider Adapters

`AnthropicProvider` maps the internal message list to the Messages API:

- `system` messages are joined into the top-level `system` field.
- `user` and `assistant` messages become Anthropic `messages` entries.
- `tool` messages are represented as user text entries with their tool name, because native tool result blocks are not wired through the shared provider contract yet.

`GeminiProvider` maps messages to `generateContent`:

- `system` messages are joined into `system_instruction`.
- `assistant` messages map to Gemini `model` role.
- `user` and `tool` messages map to Gemini `user` role text parts.

## URLs

Anthropic defaults to `https://api.anthropic.com/v1`. If a config `baseUrl` already ends in `/messages`, it is used as-is for chat; otherwise `/messages` is appended. Model listing defaults to `/models`.

Gemini defaults to `https://generativelanguage.googleapis.com/v1beta`. Chat calls use `models/{model}:generateContent`; model listing uses `/models`.

## Limits

The first built-in adapters support text-only non-streaming chat and model listing. Tool calling, streaming, and vision remain disabled in metadata until the internal provider contract carries provider-specific schemas safely.
