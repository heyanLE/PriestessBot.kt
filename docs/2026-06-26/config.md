# Config 模块

日期：2026-06-26

## 代码结构

- `PriestessConfig.kt`
- `AgentConfig.kt`
- `DatabaseConfig.kt`
- `PlatformConfig.kt`
- `ProviderConfig.kt`
- `PipelineConfig.kt`
- `PluginConfig.kt`
- `ServerConfig.kt`
- `SubAgentConfig.kt`
- `ConfigController.kt`
- `ConfigCase.kt`

## 暴露的 Case

- `ConfigCase.current()`
- `ConfigCase.update(...)`
- `ConfigCase.save(...)`
- `ConfigCase.reload()`
- `ConfigCase.*Flow`

## 业务职责

Config 模块聚合整套运行配置，负责读取、写入、替换、热更新和流式分发。

## 结构图

```mermaid
flowchart TD
    A[ConfigCase] --> B[ConfigController]
    B --> C[PriestessConfig]
    C --> D[Agent/Provider/Pipeline/Platform/Server/Plugin]
```

## 流程图

```mermaid
flowchart TD
    A[启动或文件变更] --> B[ConfigController reload]
    B --> C[解析配置]
    C --> D[校验并发布 Flow]
    D --> E[下游模块订阅更新]
```

