## Context

项目当前仅有一个 `Main.kt` 打印 "Hello Kotlin"，build.gradle.kts 仅引入 Koin。需要从零搭建 imagent v1 核心模块，实现 IM 消息接收 → 管道处理 → Agent ReAct 循环 → 回答返回的完整闭环。一期只有单模块 `imagent-core`，无 Dashboard/插件系统，所有功能 inner 实现。

## Goals / Non-Goals

**Goals:**
- 搭建完整的包结构和 Kotlin 多模块 Gradle 构建
- 实现所有 8 个核心能力模块的接口抽象 + inner 实现
- 全链路跑通：Telegram/NapCat 收到消息 → Pipeline → ReAct → LLM + Tool → 回答

**Non-Goals:**
- 不做 Dashboard 前端和 API 服务器（二期）
- 不做插件系统/ClassLoader 隔离（二期）
- 不做 LLM 流式响应（二期）
- 不做子 Agent 编排/第三方 Runner（二期）
- 不做多 Provider 负载均衡（二期）

## Decisions

### 1. 包结构：`com.heyanle.kt.astrbot` + 按功能分子包

选择 `com.heyanle.kt.astrbot` 作为根 package，内部按功能分子包（`core/`, `platform/`, `pipeline/` 等）。避免 `org.example` 的占位包名。每个子包内接口和实现同包（如 `platform/Platform.kt` 接口 + `platform/adapters/telegram/TelegramPlatform.kt` 实现）。

**替代方案**: 每个功能拆独立 Gradle 模块。一期过早，徒增构建复杂度，二期再拆。

### 2. 依赖注入：Koin 4.x DSL 模块

使用 Koin 单模块注册所有组件（`single`, `factory`）。`CoreModule` 集中声明所有依赖关系。AgentRunner 用 `factory`（每条消息链一个实例），其余用 `single`。

**替代方案**: Dagger/Hilt（编译期 DI）。Kotlin 生态中 Koin 更轻量，无注解处理开销，一期足够。

### 3. 事件总线：Kotlinx Channel (BUFFERED)

使用 `Channel<Event>(Channel.BUFFERED)` 实现 EventBus。Platform 通过 `send()` 推入事件（非挂起），PipelineScheduler 通过 `consumeEach {}` 消费。BUFFERED 模式避免 Platform 发送阻塞，且允许启动顺序容错。

**替代方案**: Kotlin Flow（多订阅者）。一期只有一个消费者（PipelineScheduler），Flow 的共享机制是过度设计。

### 4. 洋葱模型实现：递归 Flow

PipelineScheduler 使用递归 + Flow 实现洋葱模型。Stage 返回 `Flow<Unit>?`，null 表示线性，非 null 表示洋葱（前置逻辑已执行，yield 后执行后续阶段，回到该 Flow 执行后置逻辑）。一期仅 PreProcessStage 和 ProcessStage 需要洋葱模式。

**替代方案**: 纯协程 + 回调。Flow 对 Kotlin 更自然，且天然支持取消。

### 5. AgentRunner 实例隔离

每个 Runner 实例只服务一条消息链。ProcessStage 收到消息事件后 `factory` 创建新 Runner，执行完即销毁。不共享可变状态，无需并发控制。

### 6. 上下文压缩策略：接口预埋 + 可配置

定义 `ContextCompressStrategy` 接口，内置三种实现——`RoundTruncationStrategy`（按轮次截断）、`TokenWindowStrategy`（Token 窗口截断）、`LLMCompressStrategy`（LLM 摘要压缩，接口预埋）。`AgentConfig.compressStrategy` 配置切换。一期实现前两种，第三种预埋接口。

### 7. MCP 传输层：三种全实现

`McpTransport` 接口下实现 `StdioTransport`（子进程 stdin/stdout）、`SseTransport`（HTTP SSE 长连接）、`StreamableHttpTransport`（HTTP POST）。每种含独立的重连策略。

### 8. 数据库：Exposed + SQLite

使用 Exposed DSL 定义 conversations 和 messages 两张表。`Database` 接口抽象，`ImagentDb` 为 SQLite 内置实现。

### 9. Provider 请求/响应：统一 DTO

`LLMRequest` / `LLMResponse` / `ConversationMessage` 使用 `@Serializable` data class，确保与 OpenAI API 兼容，同时可扩展到其他 Provider。一期只做同步 `textChat()`，`textChatStream()` 接口预留。

## Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                        PriestessBot.main()                        │
└─────────────────────────────┬────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
        ┌──────────┐  ┌────────────┐  ┌──────────────┐
        │  Config  │  │ CoreLife   │  │  CoreModule  │
        │(@Serial) │  │  cycle     │  │  (Koin DI)   │
        └──────────┘  └─────┬──────┘  └──────────────┘
                              │
         ┌──────────┬─────────┼─────────┬──────────┐
         ▼          ▼         ▼         ▼          ▼
    ┌────────┐ ┌────────┐ ┌───────┐ ┌──────┐ ┌──────────┐
    │  DB    │ │EventBus│ │PlatMgr│ │Pipe  │ │Provider  │
    │(SQLite)│ │(Chan)  │ │       │ │Sched │ │Mgr       │
    └────────┘ └───┬────┘ └───┬───┘ └──┬───┘ └────┬─────┘
                   │          │         │          │
                   └──────────┼─────────┘          │
                              ▼                    ▼
                       ┌────────────┐       ┌──────────┐
                       │   Stages   │       │ OpenAI/  │
                       │   1..9     │       │ Ollama   │
                       └─────┬──────┘       └──────────┘
                             │
                    ┌────────┴────────┐
                    ▼                 ▼
              ┌───────────┐    ┌───────────┐
              │ReActRunner│    │ToolSet/   │
              │           │    │MCPClient  │
              └───────────┘    └───────────┘
```

## Risks / Trade-offs

- [NapCat API 稳定性] → 配置 IP+端口直连 HTTP API，无需管理进程，降低复杂度
- [上下文压缩效果] → 一期提供轮次截断和 Token 窗口两种策略，LLM 压缩接口预埋
- [MCP 传输兼容性] → 三种传输方式全部实现，确保覆盖主流 MCP Server
- [单模块后期膨胀] → 二期按需拆子模块，接口已抽象，拆解成本低
- [无 Dashboard 调试不便] → 一期用日志输出 + JSON 配置文件直接编辑
