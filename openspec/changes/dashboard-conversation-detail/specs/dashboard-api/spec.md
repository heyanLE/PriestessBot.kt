## ADDED Requirements

### Requirement: Conversation message detail API coverage
The Dashboard API SHALL expose stored message history for a conversation.

#### Scenario: Messages are returned in chronological order
- **GIVEN** a conversation has multiple stored messages
- **WHEN** the operator requests `/api/conversations/{id}/messages`
- **THEN** messages are returned oldest to newest

#### Scenario: Message limit returns recent history
- **GIVEN** a conversation has more messages than the requested count
- **WHEN** the operator requests `/api/conversations/{id}/messages?count=2`
- **THEN** the response contains the two most recent messages in chronological order

#### Scenario: Tool metadata is preserved
- **GIVEN** a stored assistant message has tool calls and a stored tool message has a tool call id
- **WHEN** the operator requests the conversation messages
- **THEN** the response includes the tool call payload and tool call id
