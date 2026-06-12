## ADDED Requirements

### Requirement: Conversation CRUD operations
The system SHALL provide `ConversationManager` that creates, queries, updates, and deletes conversations keyed by platform and session.

#### Scenario: Create a new conversation
- **WHEN** a message arrives from a new platform+session combination
- **THEN** a new conversation record is created with the current timestamp

#### Scenario: Retrieve existing conversation
- **WHEN** a message arrives from a known platform+session combination
- **THEN** the existing conversation record is retrieved and its `updated_at` field is refreshed

### Requirement: Conversation expiration cleanup
The system SHALL automatically clean up conversations that have been inactive beyond a configurable TTL.

#### Scenario: Expired conversation removed
- **WHEN** a conversation's `updated_at` exceeds the TTL
- **THEN** the conversation and its messages are deleted during the next cleanup cycle

### Requirement: Message history persistence
The system SHALL persist all conversation messages with role, content, tool call data, and timestamp.

#### Scenario: Message stored after Agent response
- **WHEN** the pipeline completes processing a message
- **THEN** the user message, assistant response, and any tool call results are persisted

#### Scenario: Message history retrieval
- **WHEN** `MessageHistory.getMessages(conversationId)` is called
- **THEN** all messages for that conversation are returned in chronological order

### Requirement: Message history used for context
The system SHALL load message history from persistence when building the Agent's conversation context in PreProcessStage.

#### Scenario: Context loaded from history
- **WHEN** PreProcessStage processes an event with an existing conversation
- **THEN** the most recent messages are loaded and added to the Agent's context
