## ADDED Requirements

### Requirement: Sender identity metadata
The system SHALL expose the actual message sender ID in unified message session metadata as `senderId` for every supported inbound platform adapter.

#### Scenario: Telegram group sender is identified independently of chat
- **GIVEN** a Telegram group message with a chat ID and a `from.id`
- **WHEN** the Telegram adapter creates a `MessageEvent`
- **THEN** `session.metadata["senderId"]` equals `from.id`
- **AND** it does not equal the group chat ID unless those values are actually identical

#### Scenario: NapCat sender identity remains available
- **GIVEN** a NapCat inbound message with a sender user ID
- **WHEN** the adapter creates a `MessageEvent`
- **THEN** `session.metadata["senderId"]` equals that sender user ID
