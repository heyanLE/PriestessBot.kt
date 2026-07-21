## 1. Config Layering

- [x] 1.1 Add a default workspace directory field to the runtime config model and config publication flows.
- [x] 1.2 Add environment override handling for the default workspace directory using the existing config layering style.
- [x] 1.3 Update config-facing docs and tests to cover effective default workspace directory resolution.

## 2. Workspace Directory Runtime

- [x] 2.1 Replace config-list-backed workspace loading with directory-backed workspace preparation models for `config.yaml`, `skills/`, and `mcpserver.json`.
- [x] 2.2 Add parsing and validation for workspace directory contents, including clear diagnostics for invalid or missing files.
- [x] 2.3 Redesign `WorkspaceSnapshot` to store directory source metadata, skill descriptors, and MCP declaration objects instead of eager skill markdown and eager MCP tool resources.
- [x] 2.4 Define a stable workspace identity strategy that works with directory-backed snapshots and existing downstream consumers.

## 3. Pipeline Preparation Flow

- [x] 3.1 Add `PrepareWorkspaceStage` before `PreProcessStage` and update pipeline stage ordering accordingly.
- [x] 3.2 Implement workspace directory source precedence from config default to platform config extension to message metadata.
- [x] 3.3 Move workspace resolution and snapshot pinning logic out of `PreProcessStage` into `PrepareWorkspaceStage`.
- [x] 3.4 Update pipeline context metadata so downstream stages can read workspace id, version, effective directory path, and resolution source.

## 4. Lazy Skill Loading

- [x] 4.1 Introduce workspace skill descriptor models that keep name, description, skill directory path, and `SKILL.md` path.
- [x] 4.2 Update `SkillCase` and pipeline skill state so workspace-visible skills are exposed without eager markdown loading.
- [x] 4.3 Change `use_skill` flow to load `SKILL.md` from the descriptor path only when explicitly requested and retain it for later prompts in the same run.
- [x] 4.4 Update agent prompt and skill-related tests to verify lazy loading and loaded-skill prompt injection.

## 5. MCP Declaration Preload

- [x] 5.1 Define workspace MCP declaration models sourced from `mcpserver.json`.
- [x] 5.2 Replace eager workspace MCP tool resolution during snapshot construction with declaration parsing and snapshot pre-embedding.
- [x] 5.3 Update MCP-facing runtime seams so future executable tool wrapping can attach to the stored declarations without reshaping the snapshot contract.

## 6. Verification

- [x] 6.1 Add tests for config-layer default workspace directory publication and override precedence.
- [x] 6.2 Add tests for `PrepareWorkspaceStage` source precedence and per-message snapshot pinning.
- [x] 6.3 Add tests for directory-backed workspace parsing, diagnostics, and snapshot metadata contents.
- [x] 6.4 Add tests proving skills load `SKILL.md` lazily through explicit tool use.
- [x] 6.5 Add tests proving MCP declarations are stored in snapshots without eager tool initialization.
- [x] 6.6 Run the relevant OpenSpec status/validation checks and Kotlin test suites for touched modules.
