# provider Specification

## Purpose
TBD - created by archiving change priestess-v1-core. Update Purpose after archive.
## Requirements
### Requirement: ChatProvider interface
The system SHALL define `ChatProvider` with `textChat()` for synchronous completion and `textChatStream()` (reserved for v2). `ProviderManager` manages multiple Provider instances by name.

#### Scenario: Synchronous completion
- **WHEN** `textChat(request)` is called with messages and tools
- **THEN** a complete `LLMResponse` with text content and optional tool calls is returned

#### Scenario: Provider connectivity test
- **WHEN** `test()` is called on a configured Provider
- **THEN** true is returned if the API endpoint is reachable and credentials are valid, false otherwise

#### Scenario: Model list retrieval
- **WHEN** `getModels()` is called
- **THEN** a list of available model identifiers is returned from the Provider's API

### Requirement: OpenAI provider adapter
The system SHALL provide `OpenAIProvider` that implements the OpenAI Chat Completions API.

#### Scenario: Basic chat completion
- **WHEN** `textChat()` is called with a user message
- **THEN** the OpenAI API returns a completion with the assistant's response text

#### Scenario: Function calling
- **WHEN** `textChat()` is called with tools defined and the model decides to invoke a tool
- **THEN** the `LLMResponse` contains tool calls with function name and arguments

#### Scenario: OpenAI-compatible services
- **WHEN** the base URL is configured to a compatible service (e.g., DeepSeek, Qwen)
- **THEN** the provider works without code changes

### Requirement: Ollama provider adapter
The system SHALL provide `OllamaProvider` that implements the Ollama Chat API.

#### Scenario: Local model chat completion
- **WHEN** `textChat()` is called with a user message and Ollama is running locally
- **THEN** a completion is returned from the specified local model

#### Scenario: Model list retrieval
- **WHEN** `getModels()` is called
- **THEN** all locally available models are returned

### Requirement: Unified request/response DTO
The system SHALL define `@Serializable` data classes for `LLMRequest`, `LLMResponse`, and `ConversationMessage`.

#### Scenario: Request serialization
- **WHEN** an `LLMRequest` is serialized to JSON
- **THEN** it produces a JSON object compatible with OpenAI Chat Completions API format

#### Scenario: Response deserialization
- **WHEN** an OpenAI API response JSON is received
- **THEN** it is deserialized into an `LLMResponse` with correct text, tool calls, and token usage

### Requirement: Provider runtime management
The system SHALL expose provider metadata and connectivity tests through management APIs.

#### Scenario: Provider metadata exposed
- **WHEN** a dashboard client requests providers
- **THEN** registered provider metadata is returned

#### Scenario: Provider test exposed
- **WHEN** a dashboard client requests provider tests
- **THEN** each registered provider is tested
- **AND** a provider-name to boolean result map is returned

#### Scenario: Plugin provider metadata exposed
- **GIVEN** a plugin has registered a provider
- **WHEN** a dashboard client requests providers
- **THEN** the plugin-provided provider metadata is returned

#### Scenario: Plugin provider test exposed
- **GIVEN** a plugin has registered a provider
- **WHEN** a dashboard client requests provider tests
- **THEN** the plugin-provided provider participates in the result map
### Requirement: Provider lifecycle
The system SHALL expose configured and plugin-registered LLM providers to Agent execution.

#### Scenario: Config provider update affects later lookups
- **GIVEN** a `ProviderController` is already running
- **WHEN** provider config is updated or reloaded
- **THEN** later provider lookups use provider instances built from the latest enabled provider config

#### Scenario: Disabled config provider is removed
- **GIVEN** a provider was created from enabled provider config
- **WHEN** the provider config is updated with that provider disabled or removed
- **THEN** later provider listings and lookups no longer include the config-backed provider

#### Scenario: Plugin providers survive config refresh
- **GIVEN** a plugin or runtime component registered a provider directly
- **WHEN** provider config is updated or reloaded
- **THEN** the runtime-registered provider remains available until explicitly unregistered

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
