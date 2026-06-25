## ADDED Requirements

### Requirement: ToolExecutor SHALL have unit test coverage for validation and execution outcomes
`ToolExecutor` SHALL be covered by unit tests for unknown tools, JSON argument parsing, schema validation, tool exceptions, permissions, timeouts, batch execution, partial failures, and metrics.

#### Scenario: Unknown tool is tested
- **WHEN** the LLM requests a tool name that is not registered
- **THEN** a unit test SHALL verify the executor returns an error `ToolResult` and records a failed tool metric

#### Scenario: Invalid arguments are tested
- **WHEN** the LLM sends malformed JSON or arguments that do not match the tool schema
- **THEN** a unit test SHALL verify the executor returns a validation error without invoking the tool

#### Scenario: Tool exception is tested
- **WHEN** a registered tool throws during execution
- **THEN** a unit test SHALL verify the executor converts the exception into an error `ToolResult`

#### Scenario: Permission denial is tested
- **WHEN** a tool is disabled or denied by tool policy
- **THEN** a unit test SHALL verify execution is skipped and a permission-denied result is returned

#### Scenario: Tool timeout is tested
- **WHEN** a tool exceeds its configured timeout
- **THEN** a unit test SHALL verify execution ends with a timeout result and records failure metrics

#### Scenario: Batch partial failure is tested
- **WHEN** a batch contains multiple tool calls and one call fails
- **THEN** a unit test SHALL verify calls are processed in order and successful results are preserved beside failed results

#### Scenario: Tool metrics are tested
- **WHEN** tool execution succeeds, fails, times out, or references an unknown tool
- **THEN** a unit test SHALL verify the metrics registry records the tool name and status without sensitive arguments
