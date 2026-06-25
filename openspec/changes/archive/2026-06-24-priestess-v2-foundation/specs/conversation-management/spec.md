## MODIFIED Requirements

### Requirement: Conversation runtime listing
The system SHALL expose stored conversations and message histories through management APIs.

#### Scenario: Conversation summaries exposed
- **WHEN** a dashboard client requests conversations
- **THEN** stored conversation summaries are returned

#### Scenario: Conversation messages exposed
- **WHEN** a dashboard client requests messages for a conversation
- **THEN** stored message history for that conversation is returned
