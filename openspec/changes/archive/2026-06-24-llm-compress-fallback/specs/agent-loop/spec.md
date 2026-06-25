## MODIFIED Requirements

### Requirement: Context compression strategies SHALL be safe to execute

Configured context compression strategies SHALL compress conversation history without crashing the Agent loop for supported strategy names.

#### Scenario: LLM compression falls back safely

- **GIVEN** an Agent uses the `llm_compress` strategy
- **AND** the message history exceeds the configured context budget
- **WHEN** the context manager compresses the history
- **THEN** compression SHALL complete without throwing `NotImplementedError`
- **AND** the compressed result SHALL keep the system message when one is provided
- **AND** the compressed result SHALL preserve recent messages within the configured budget

#### Scenario: Strategy name remains observable

- **WHEN** the `llm_compress` strategy is inspected
- **THEN** its strategy name SHALL remain `llm_compress`
