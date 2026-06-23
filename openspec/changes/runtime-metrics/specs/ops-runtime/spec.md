## MODIFIED Requirements

### Requirement: Runtime metrics SHALL support Prometheus scraping

The runtime SHALL expose process-local operational metrics in Prometheus text exposition format.

#### Scenario: Metrics endpoint returns Prometheus text

- **GIVEN** the Dashboard/API server is enabled
- **WHEN** an operator requests `GET /metrics`
- **THEN** the response SHALL be `text/plain`
- **AND** the body SHALL include HELP and TYPE metadata for runtime metrics

#### Scenario: Pipeline messages are counted

- **GIVEN** a platform message is accepted by the pipeline
- **WHEN** pipeline processing completes or fails
- **THEN** the metrics registry SHALL increment `priestess_pipeline_messages_total`
- **AND** it SHALL record pipeline duration with platform and status labels

#### Scenario: LLM requests are measured

- **GIVEN** the process stage invokes an LLM provider
- **WHEN** the request returns a final response or error
- **THEN** the metrics registry SHALL increment `priestess_llm_requests_total`
- **AND** it SHALL record request duration with provider and status labels

#### Scenario: Tool calls are counted

- **GIVEN** an agent attempts to execute a tool
- **WHEN** the tool call succeeds, fails, or references an unknown tool
- **THEN** the metrics registry SHALL increment `priestess_tool_calls_total`
- **AND** the metric SHALL include tool and status labels

#### Scenario: Metrics exclude sensitive runtime data

- **GIVEN** metrics have been recorded for messages, LLM calls, and tools
- **WHEN** the Prometheus endpoint is scraped
- **THEN** the metrics SHALL NOT include prompts, message text, session IDs, user IDs, API keys, tool arguments, or exception messages
