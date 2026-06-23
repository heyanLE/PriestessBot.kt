## MODIFIED Requirements

### Requirement: Built-in provider adapters SHALL include major hosted LLM APIs

The provider runtime SHALL include built-in adapters for OpenAI-compatible, Ollama, Anthropic, and Gemini chat providers.

#### Scenario: Anthropic provider can be configured by type

- **GIVEN** provider config with `type` set to `anthropic`
- **WHEN** provider configuration is materialized
- **THEN** the runtime SHALL create an Anthropic chat provider
- **AND** provider metadata SHALL identify kind `ANTHROPIC`

#### Scenario: Gemini provider can be configured by type

- **GIVEN** provider config with `type` set to `gemini`
- **WHEN** provider configuration is materialized
- **THEN** the runtime SHALL create a Gemini chat provider
- **AND** provider metadata SHALL identify kind `GEMINI`

#### Scenario: Hosted provider text chat maps common messages

- **GIVEN** a configured Anthropic or Gemini provider
- **WHEN** text chat is requested with system, user, and assistant messages
- **THEN** the provider SHALL send a provider-native non-streaming request
- **AND** parse text content, finish reason, and token usage when returned
