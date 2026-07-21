## Context

The roadmap identifies v3 testing as the step that moves the project from "can run" to "can change safely." Existing coverage already touches providers, manual pipeline integration, NapCat parsing, plugins, compression, sub-agent orchestration, knowledge search, metrics, Dashboard routes, and coordinated shutdown. The remaining risk concentrates around cross-module behavior: `ReActRunner`, `ToolExecutor`, individual pipeline stages, Dashboard API contracts, config/workspace reload rollback, and future persona/memory interfaces.

The design must keep test boundaries aligned with production module boundaries. Tests should use fake implementations for provider, platform, tool, clock, config source, and storage dependencies rather than real network credentials or live services.

## Goals / Non-Goals

**Goals:**
- Define a repeatable unit, integration, system, and regression testing structure for v3.
- Make the Agent loop and message pipeline testable without real providers or platforms.
- Provide shared fixtures that implementation tasks can reuse across `ReActRunner`, `ToolExecutor`, pipeline, Dashboard API, workspace, persona, and memory tests.
- Require route contract coverage for Dashboard APIs and regression tests for bug fixes.
- Keep future workspace/persona/memory work accountable to tests even if those modules are implemented in later changes.

**Non-Goals:**
- Replace the production architecture only to make tests easier.
- Add browser end-to-end UI tests for the Dashboard frontend in this change.
- Depend on external provider credentials, live MCP servers, Telegram, NapCat, or internet access.
- Define performance/load testing beyond lightweight system-level quality gates.

## Decisions

### Decision: Use layered suites with explicit ownership

Unit tests own deterministic logic and boundary failures. Integration tests own module contracts. System tests own one full credential-free message flow. Regression tests own previously fixed bugs and production incidents.

Alternatives considered:
- A large end-to-end suite only: rejected because failures would be slow and hard to localize.
- Unit tests only: rejected because the main risks are handoffs between pipeline, Agent loop, tools, controllers, and Dashboard routes.

### Decision: Build shared fake fixtures instead of mocking every call inline

Create reusable fixtures for fake provider scripted responses, fake platform messages/sends, fake tools, fake clocks, fake config sources, in-memory conversations, and metrics assertions. Tests can still use lightweight mocks for narrow collaborators, but cross-module tests should use these fixtures so behavior stays readable.

Alternatives considered:
- Real provider/platform test doubles backed by live services: rejected because tests must run without credentials.
- Per-test ad hoc fakes: rejected because duplicated setup obscures scenario intent and creates inconsistent edge behavior.

### Decision: Treat Dashboard tests as route contracts

Dashboard API tests should verify request/response shape, status codes, authentication behavior when relevant, and service/controller delegation using Ktor test application support. They should not assert internal route implementation details.

Alternatives considered:
- Service-only tests: rejected because they miss serialization, routing, auth, and HTTP status regressions.
- Full frontend/backend browser tests: out of scope for this foundation and slower than needed for API contracts.

### Decision: Model workspace/persona/memory tests before implementation

Workspace, persona, and memory are future-facing v3 modules, but their interfaces have enough roadmap definition to specify required tests now. The implementation tasks should add pending module tests when those modules are introduced, or create test scaffolds adjacent to the module implementation in the same future change.

Alternatives considered:
- Wait until each module exists: rejected because it risks shipping new public APIs without a test contract.
- Over-specify storage/database internals now: rejected because the current need is interface behavior, not persistence details.

### Decision: Keep quality gates credential-free

`./gradlew test` must pass locally and in PRs without external secrets. The system test uses fake provider/tool/platform fixtures and asserts message history plus metrics, not live network behavior.

Alternatives considered:
- Optional secret-backed smoke tests in the required gate: rejected because contributors and CI environments may not have credentials.

## Risks / Trade-offs

- [Risk] Adding many tests at once can create brittle fixtures. -> Mitigation: centralize fakes and keep assertions scenario-focused.
- [Risk] Future workspace/persona/memory APIs may differ from roadmap sketches. -> Mitigation: specs require behavioral coverage, while task names allow tests to be finalized with the actual implementation interfaces.
- [Risk] System tests can become slow if they start full runtimes. -> Mitigation: use in-process fake platform/provider/tool wiring and avoid network services.
- [Risk] Route contract tests can duplicate DTO internals. -> Mitigation: assert public JSON fields, status codes, and auth behavior rather than route implementation details.

## Migration Plan

1. Add shared test fixture package and convert new tests to use it.
2. Add focused unit tests for existing high-risk modules.
3. Add integration tests for cross-module handoffs.
4. Add the credential-free message flow system test.
5. Enable quality gates through the existing Gradle test task and document any slower suites if they are split later.

Rollback is straightforward because this change introduces tests and test-only helpers; if a test fixture causes instability, remove or narrow the fixture while preserving the behavioral requirement.

## Open Questions

- Should system tests remain under the default `test` task or move to a separate Gradle source set if runtime grows slower?
- What exact package names will workspace/persona/memory controllers use when their implementation changes land?
- Should route contract tests snapshot full JSON responses or prefer explicit field assertions?
