# 普瑞赛斯 Bot

<div align="center">

<img src="./image/icon.jpg" alt="PriestessBot" width="128" height="128">

**PriestessBot** — 基于 Kotlin 的多平台 Agent 聊天机器人框架

[简体中文](./README.md)

<br>

<img src="https://img.shields.io/badge/kotlin-2.3+-blue.svg" alt="kotlin">
<img src="https://img.shields.io/badge/jdk-21-red.svg" alt="jdk">

</div>

---

普瑞赛斯 Bot（PriestessBot）是一个开源的、基于 Kotlin 的多平台 Agent 聊天机器人框架。它提供类型安全、可扩展的对话 AI 基础设施，支持接入主流 IM 平台和 LLM 服务，帮助开发者快速构建生产级的 AI 应用。

## 特性

- **多平台接入** — 抽象 IM 平台接口，支持 Telegram、NapCat（QQ），可扩展更多平台
- **消息管道** — 9 阶段洋葱模型管道，从唤醒检查到回答装饰全链路可控
- **Agent 循环** — 内置 ReAct Runner，Thought → Action → Observation 推理循环
- **LLM Provider 抽象** — 统一接口接入 OpenAI、Ollama 等 LLM 服务
- **Tool / MCP 体系** — 内置工具 + MCP 协议支持（stdio / SSE / streamable HTTP）
- **类型安全** — 全链路 `@Serializable` data class，编译期类型检查
- **协程原生** — 基于 Kotlin Coroutines + Flow 的结构化并发

## 快速开始

> 一期开发中，敬请期待。

## 路线图

详见 [Roadmap](./roadmap/index.md)

- **[v1](./roadmap/v1/v1.md)** — 核心闭环：IM 平台 + 管道 + ReAct Agent + Tool/MCP
- **[v2](./roadmap/v2/v2.md)** — 可扩展：插件系统 + 更多平台/Provider + Dashboard + RAG

## 技术栈

| 层级 | 技术 |
|------|------|
| 语言 | Kotlin 2.3+ / JDK 21 |
| 构建 | Gradle Kotlin DSL |
| DI | Koin 4.x |
| 序列化 | kotlinx.serialization |
| 数据库 | Exposed ORM + SQLite |
| HTTP | Ktor Client / Ktor Server（二期） |
| 协程 | kotlinx.coroutines + Flow |

## 参考

本项目架构设计参考 [AstrBot](https://github.com/AstrBotDevs/AstrBot)，以 Kotlin 重新实现并优化类型安全、并发模型和可扩展性。

## License

MIT
