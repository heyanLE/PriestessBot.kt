## Why

当前项目只有一个空壳 `Main.kt`，需要搭建 imagent 系统的 v1 最小闭环——一条消息从 IM 端流入，经过管道处理、Agent ReAct 循环调用 LLM 与工具，最终返回回答。一期不引入 Dashboard 和插件系统，所有功能 inner 实现，接口抽象预留扩展点。

## What Changes

- 新建 `imagent-core` 模块，包含完整的包结构和所有 v1 功能
- 实现核心生命周期编排（CoreLifecycle）与 Koin 依赖注入
- 实现类型安全配置系统（`@Serializable` data class + JSON 持久化）
- 实现基于 Channel 的事件总线（EventBus）
- 实现 IM 平台抽象层（Platform 接口 + Telegram + NapCat 适配器）
- 实现 9 阶段洋葱模型消息管道（PipelineScheduler + 全部 Stage）
- 实现 LLM Provider 抽象层（ChatProvider 接口 + OpenAI + Ollama）
- 实现 ReAct Agent 循环（ReActRunner + 上下文压缩策略）
- 实现 Tool / MCP 体系（FunctionTool + ToolSet + MCP 三种传输方式 + 4 个内置工具）
- 实现 Skill 管理和会话持久化

## Capabilities

### New Capabilities

- `core-infrastructure`: 核心生命周期、类型安全配置、Koin DI、事件总线、SQLite 持久化
- `platform-abstraction`: IM 平台抽象接口与 Telegram/NapCat 适配器、统一消息模型
- `pipeline`: 9 阶段洋葱模型消息管道，从唤醒检查到最终回答
- `provider`: LLM Provider 抽象接口与 OpenAI/Ollama 适配器
- `agent-loop`: ReActRunner 实现与上下文压缩策略（轮次截断/Token 窗口/LLM 压缩，接口预埋）
- `tool-mcp`: FunctionTool/ToolSet 体系、MCP 客户端（stdio/SSE/streamable HTTP）、4 个内置工具
- `skill-management`: Skill 接口与 DefaultSkill
- `conversation-management`: 会话与消息历史持久化

### Modified Capabilities

<!-- 无现有 capability 需修改 -->

## Impact

- 项目从单文件变为完整的多包 `imagent-core` 模块
- 新增依赖：Koin、kotlinx.serialization、Exposed、Ktor Client、SQLite JDBC
- 无 API 服务器和前端 Dashboard（二期实现）
- 所有配置通过 JSON 文件 + inner config 管理
