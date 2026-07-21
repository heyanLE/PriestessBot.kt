# conversation-management Specification

## Purpose
TBD - created by archiving change priestess-v1-core. Update Purpose after archive.
## Requirements

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

### Requirement: Conversation runtime listing
The system SHALL expose stored conversations and message histories through management APIs.

#### Scenario: Conversation summaries exposed
- **WHEN** a dashboard client requests conversations
- **THEN** stored conversation summaries are returned

#### Scenario: Conversation messages exposed
- **WHEN** a dashboard client requests messages for a conversation
- **THEN** stored message history for that conversation is returned

### Requirement: Clear current conversation history
The system SHALL clear persisted messages for a conversation identified by platform and session without deleting that conversation record.

#### Scenario: History is empty after clear
- **GIVEN** a known platform and session with persisted user, assistant, and Tool messages
- **WHEN** the current conversation history is cleared
- **THEN** subsequent history retrieval for that conversation returns no messages
- **AND** retrieving or creating that platform and session returns the same conversation identity

### Requirement: Conversation history SHALL support scoped search
The conversation management layer SHALL support bounded, workspace-scoped message search for Agent tools and management surfaces.

#### Scenario: Search messages by keyword
- **WHEN** conversation history is searched with a keyword query
- **THEN** matching stored messages SHALL be returned with conversation id, message id, role, timestamp, and content snippet

#### Scenario: Search messages by time and conversation
- **WHEN** conversation history is searched with a time range or conversation id
- **THEN** only messages matching those filters SHALL be returned

#### Scenario: Search is workspace scoped
- **GIVEN** stored messages exist for multiple workspaces
- **WHEN** a workspace-scoped search is executed
- **THEN** messages from other workspaces SHALL NOT be returned

#### Scenario: Search limit is enforced
- **WHEN** a search request specifies a limit
- **THEN** no more than that limit or the configured maximum SHALL be returned
