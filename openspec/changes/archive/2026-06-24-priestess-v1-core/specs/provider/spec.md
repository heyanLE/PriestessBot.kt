## ADDED Requirements

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
