## ADDED Requirements

### Requirement: Core lifecycle orchestrates all submodules
The system SHALL provide a `CoreLifecycle` that starts and stops all submodules in dependency order: Database → EventBus → ToolRegistry → ProviderManager → PipelineScheduler → PlatformManager.

#### Scenario: Startup completes successfully
- **WHEN** `ImagentApp.main()` is invoked and all configurations are valid
- **THEN** all submodules initialize in correct order and the system is ready to process messages

#### Scenario: Startup fails on missing config
- **WHEN** required configuration file is missing
- **THEN** startup aborts with a clear error message indicating which config is missing

#### Scenario: Graceful shutdown
- **WHEN** SIGTERM is received
- **THEN** all platforms stop accepting new messages, in-flight pipelines complete, and resources are released

### Requirement: Type-safe configuration via @Serializable
The system SHALL use `@Serializable` data classes for all configuration, persisted as JSON files.

#### Scenario: Load config from JSON file
- **WHEN** the system starts and a valid JSON config file exists at the configured path
- **THEN** configuration is deserialized into `ImagentConfig` with compile-time type checking

#### Scenario: Default config generation
- **WHEN** no config file exists at startup
- **THEN** a default configuration file is generated with sensible defaults

### Requirement: Koin dependency injection for all components
The system SHALL register all components via Koin `CoreModule` with appropriate scopes: `single` for long-lived components, `factory` for per-message-chain instances.

#### Scenario: AgentRunner created per message
- **WHEN** ProcessStage requests an AgentRunner instance
- **THEN** Koin provides a new `ReActRunner` instance via `factory` scope, isolated from other concurrent requests

### Requirement: EventBus based on Kotlin Channel
The system SHALL use a buffered Channel to transport events from Platform adapters to PipelineScheduler.

#### Scenario: Platform commits a message event
- **WHEN** a Platform adapter calls `commitEvent(event)`
- **THEN** the event is sent to the Channel without blocking the Platform's receiving loop

#### Scenario: PipelineScheduler consumes events
- **WHEN** events are available in the EventBus Channel
- **THEN** PipelineScheduler processes each event through all pipeline stages

### Requirement: Database persistence via Exposed + SQLite
The system SHALL use Exposed ORM with SQLite for persisting conversations, messages, and configuration data.

#### Scenario: Database initialized on first start
- **WHEN** the system starts for the first time
- **THEN** SQLite database file and all required tables are created automatically
