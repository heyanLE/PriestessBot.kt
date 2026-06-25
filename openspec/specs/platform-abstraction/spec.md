# platform-abstraction Specification

## Purpose
TBD - created by archiving change priestess-v1-core. Update Purpose after archive.
## Requirements
### Requirement: Platform abstract base class
The system SHALL define an abstract `Platform` class with `run()`, `sendMessage()`, `terminate()`, and `commitEvent()` methods that all IM adapters must implement.

#### Scenario: Platform starts listening for messages
- **WHEN** `PlatformManager.start()` is called on a configured Platform
- **THEN** the platform begins receiving messages and converting them to `MessageEvent` via `commitEvent()`

#### Scenario: Platform sends a message
- **WHEN** `RespondStage` calls `platform.sendMessage(session, chain)`
- **THEN** the message is delivered to the target user/group through the platform's native API

#### Scenario: Platform terminates gracefully
- **WHEN** `PlatformManager.stop()` is called
- **THEN** the platform stops accepting new messages and closes its connection cleanly

### Requirement: Unified message event model
The system SHALL define `MessageEvent`, `MessageSession`, `MessageChain`, and `MessageComponent` (sealed class: Text/Image/At/File) as the unified message representation across all platforms.

#### Scenario: Text message received from Telegram
- **WHEN** a Telegram user sends a text message
- **THEN** a `MessageEvent` is created with a `MessageChain` containing a single `Text` component

#### Scenario: Image message received from NapCat
- **WHEN** a QQ user sends an image via NapCat
- **THEN** a `MessageEvent` is created with a `MessageChain` containing an `Image` component with the image URL

### Requirement: Platform registry via annotations
The system SHALL use annotations to register Platform implementations, enabling automatic discovery of all built-in adapters.

#### Scenario: All registered platforms listed
- **WHEN** `PlatformRegistry.getRegistered()` is called
- **THEN** it returns all Platform implementations annotated with `@RegisterPlatform`

### Requirement: Telegram platform adapter
The system SHALL provide a `TelegramPlatform` that connects via Telegram Bot API with long polling support.

#### Scenario: Telegram bot receives a message
- **WHEN** a Telegram user sends a message to the bot
- **THEN** the message is parsed into a `MessageEvent` and committed to the EventBus

#### Scenario: Telegram bot sends a reply
- **WHEN** `sendMessage()` is called with a text chain
- **THEN** the bot sends the message to the correct Telegram chat

### Requirement: NapCat platform adapter
The system SHALL provide a `NapCatPlatform` that connects to NapCat via HTTP API using IP and port configuration only.

#### Scenario: NapCat platform connects to QQ
- **WHEN** NapCat is running and the IP/port configuration is correct
- **THEN** the platform connects via HTTP API and receives QQ messages as `MessageEvent`

#### Scenario: NapCat sends a reply to QQ
- **WHEN** `sendMessage()` is called with a message chain
- **THEN** the message is sent to the correct QQ chat via NapCat HTTP API

### Requirement: Platform runtime management
The system SHALL expose configured platform status and allow runtime enable/disable through management APIs.

#### Scenario: Platform status exposed
- **WHEN** a dashboard client requests platform status
- **THEN** configured platforms include whether their adapter is currently running

#### Scenario: Platform disabled through API
- **WHEN** a dashboard client disables a platform
- **THEN** the platform config is updated
- **AND** the platform controller stops the adapter

#### Scenario: Plugin platform metadata exposed
- **GIVEN** a plugin has registered a platform adapter
- **WHEN** platform metadata is requested from the registry
- **THEN** the plugin-provided platform metadata is returned

#### Scenario: Plugin platform created from config
- **GIVEN** a plugin has registered a platform adapter
- **WHEN** a matching enabled `PlatformConfig` is used
- **THEN** the platform adapter can be created through `PlatformRegistry.createFromConfig`