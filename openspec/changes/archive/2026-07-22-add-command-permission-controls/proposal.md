## Why

机器人目前会将所有已唤醒消息交给 LLM，缺少可预测的本地管理命令和面向操作者的权限边界。管理员无法安全地清空当前会话历史，且高权限 Tool 与 Skill 会对不具备相应权限的消息发送者暴露，造成误用与不必要的模型调用风险。

## What Changes

- 在 Pipeline 内新增可扩展的命令注册表，使用独立且可配置的命令前缀（默认 `/`）；命令在 LLM 预处理前处理，命中后不进入 Agent/LLM。
- 新增内置 `/new` 命令：要求资深干员（管理员）权限，清空当前平台和会话的历史消息，并直接确认结果。
- 新增三层消息权限组：普通干员、资深干员和高级资深干员；高级和资深干员 ID 由全局平铺配置指定，未匹配的发送者为普通干员。
- 在流水线中解析并传递消息的权限组；Telegram 使用发送者 `from.id`，NapCat 保持其现有发送者 ID 映射。
- 在人设中新增可配置的权限不足错误文案，并供命令拒绝和 Tool 权限拒绝共同使用。
- 为 Tool 和 Skill 声明所需权限并按当前消息权限装配：高级资深干员专属能力对低权限完全不可见；资深干员专属能力对普通干员保留可见提示，但 Tool 执行和 `use_skill` 装载时均强制拒绝。
- 将 Tool 权限拒绝以 OpenAI / OpenAI-compatible Chat Completions 的 `tool` 消息结果回送模型；其他 Provider 暂不在本变更中兼容或调整。

## Capabilities

### New Capabilities

- `command-permission-controls`: 本地命令、消息权限组、权限感知的 Tool/Skill 装配及人设错误文案。

### Modified Capabilities

- `pipeline`: 在 Agent 预处理前解析权限并执行命令，同时保持可回复的短路语义。
- `conversation-management`: 支持仅清空指定平台和会话的消息历史。
- `tool-mcp`: Tool 声明所需权限、按权限展示并在执行器中拒绝未授权调用。
- `skill-management`: Skill 声明所需权限并按权限控制可加载状态与提示。
- `platform-abstraction`: 平台消息必须提供可靠的发送者身份；Telegram 解析发送者 ID。
- `persona-memory`: 人设持久化并解析权限不足错误文案。

## Impact

- 影响配置模型、Pipeline stage 顺序和 `PipelineContext`、会话存储、ToolSchema/ToolExecutor、workspace skill 描述、Telegram adapter、Persona 持久化与 Dashboard 人设 DTO/API。
- OpenAI-compatible provider 的既有 Chat Completions tool message 格式继续使用；将新增覆盖权限拒绝回送的回归测试。
- 不改动 Ollama、Anthropic、Gemini 的 tool-result 协议或其兼容性。
