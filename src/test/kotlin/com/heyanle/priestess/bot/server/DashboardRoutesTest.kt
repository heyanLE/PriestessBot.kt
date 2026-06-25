package com.heyanle.priestess.bot.server

import com.heyanle.priestess.bot.agent.AgentCase
import com.heyanle.priestess.bot.agent.context.ContextManager
import com.heyanle.priestess.bot.agent.context.TokenCounter
import com.heyanle.priestess.bot.agent.orchestration.SubAgentOrchestrator
import com.heyanle.priestess.bot.config.AgentConfig
import com.heyanle.priestess.bot.config.ConfigBackup
import com.heyanle.priestess.bot.config.ConfigCase
import com.heyanle.priestess.bot.config.ConfigController
import com.heyanle.priestess.bot.config.PluginConfig
import com.heyanle.priestess.bot.config.PlatformConfig
import com.heyanle.priestess.bot.config.PriestessConfig
import com.heyanle.priestess.bot.config.ProviderConfig
import com.heyanle.priestess.bot.config.ServerConfig
import com.heyanle.priestess.bot.config.SubAgentConfig
import com.heyanle.priestess.bot.config.SubAgentOrchestrationConfig
import com.heyanle.priestess.bot.config.SubAgentRouteConfig
import com.heyanle.priestess.bot.conversation.ConversationCase
import com.heyanle.priestess.bot.conversation.ConversationController
import com.heyanle.priestess.bot.conversation.MessageHistory
import com.heyanle.priestess.bot.conversation.MessageRole
import com.heyanle.priestess.bot.core.db.DatabaseController
import com.heyanle.priestess.bot.knowledge.KnowledgeCase
import com.heyanle.priestess.bot.knowledge.KnowledgeController
import com.heyanle.priestess.bot.memory.MemoryCase
import com.heyanle.priestess.bot.memory.MemoryController
import com.heyanle.priestess.bot.memory.MemoryScope
import com.heyanle.priestess.bot.memory.MemoryType
import com.heyanle.priestess.bot.observability.MetricsRegistry
import com.heyanle.priestess.bot.persona.PersonaCase
import com.heyanle.priestess.bot.persona.PersonaController
import com.heyanle.priestess.bot.persona.PersonaMemoryInjector
import com.heyanle.priestess.bot.persona.PersonaUpsertRequest
import com.heyanle.priestess.bot.plugin.PluginCase
import com.heyanle.priestess.bot.plugin.PluginExtensionRegistry
import com.heyanle.priestess.bot.plugin.PluginManifest
import com.heyanle.priestess.bot.plugin.PluginController
import com.heyanle.priestess.bot.plugin.PluginState
import com.heyanle.priestess.bot.platform.PlatformCase
import com.heyanle.priestess.bot.platform.PlatformController
import com.heyanle.priestess.bot.provider.ChatProvider
import com.heyanle.priestess.bot.provider.LLMKind
import com.heyanle.priestess.bot.provider.ProviderCase
import com.heyanle.priestess.bot.provider.ProviderController
import com.heyanle.priestess.bot.provider.ProviderMetadata
import com.heyanle.priestess.bot.provider.ProviderRegistry
import com.heyanle.priestess.bot.provider.model.LLMRequest
import com.heyanle.priestess.bot.provider.model.LLMResponse
import com.heyanle.priestess.bot.provider.model.TokenUsage
import com.heyanle.priestess.bot.provider.model.ToolCall
import com.heyanle.priestess.bot.tool.AgentToolContext
import com.heyanle.priestess.bot.tool.FunctionTool
import com.heyanle.priestess.bot.tool.ToolParameters
import com.heyanle.priestess.bot.tool.ToolResult
import com.heyanle.priestess.bot.tool.ToolSchema
import com.heyanle.priestess.bot.tool.ToolController
import com.heyanle.priestess.bot.tool.ToolExecutor
import com.heyanle.priestess.bot.tool.builtin.HealthCheckTool
import com.heyanle.priestess.bot.tool.builtin.registerBuiltinTools
import com.heyanle.priestess.bot.skill.SkillCase
import com.heyanle.priestess.bot.skill.SkillController
import com.heyanle.priestess.bot.workspace.ConfigBackedWorkspaceConfigSource
import com.heyanle.priestess.bot.workspace.WorkspaceController
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import javax.tools.ToolProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DashboardRoutesTest {
    @Test
    fun `health config listings and websocket are available`() = testApplication {
        val service = testService(
            platforms = listOf(
                PlatformConfig(
                    name = "health-platform",
                    type = "health-platform",
                    enabled = false,
                    token = "secret-platform-token",
                ),
            ),
        )
        application {
            configureDashboardApplication(service, corsEnabled = false)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
            install(WebSockets)
        }

        val health = client.get("/health")
        assertEquals(HttpStatusCode.OK, health.status)
        val healthBody = health.body<HealthResponse>()
        assertEquals("UP", healthBody.status)
        assertTrue(healthBody.uptimeMillis >= 0)
        assertTrue(healthBody.diagnostics["configPath"]?.endsWith("config.json") == true)
        assertTrue(healthBody.diagnostics["databasePath"]?.isNotBlank() == true)
        assertEquals("1", healthBody.diagnostics["configuredPlatforms"])
        assertEquals("1", healthBody.diagnostics["configuredProviders"])
        assertTrue((healthBody.diagnostics["availableProviders"]?.toIntOrNull() ?: 0) >= 1)
        assertTrue((healthBody.diagnostics["registeredTools"]?.toIntOrNull() ?: 0) >= 1)
        assertEquals("UP", healthBody.components["providers"])
        assertEquals("UP", healthBody.components["tools"])
        assertEquals("UP", healthBody.components["plugins"])
        assertEquals("UP", healthBody.components["workspaceReload"])
        assertFalse(healthBody.diagnostics.values.any { value ->
            value.contains("secret-provider-key") || value.contains("secret-platform-token")
        })

        val healthTool = HealthCheckTool { service.healthProviderForTest() }
        val healthToolBody = Json.decodeFromString<HealthResponse>(
            runBlocking { healthTool.execute(AgentToolContext(), emptyMap()).output },
        )
        assertEquals(healthBody.components.keys, healthToolBody.components.keys)
        assertEquals(healthBody.diagnostics.keys, healthToolBody.diagnostics.keys)
        assertFalse(healthToolBody.toString().contains("secret-provider-key"))
        assertFalse(healthToolBody.toString().contains("secret-platform-token"))

        val metrics = client.get("/metrics")
        assertEquals(HttpStatusCode.OK, metrics.status)
        assertTrue(metrics.contentType()?.match(ContentType.Text.Plain) == true)
        assertTrue(metrics.bodyAsText().contains("# HELP priestess_pipeline_messages_total"))

        val config = client.get("/api/config").body<PriestessConfig>()
        assertEquals(18080, config.server.port)
        val initialBackupCount = client.get("/api/config/backups").body<List<ConfigBackup>>().size

        val updated = config.copy(server = config.server.copy(port = 18081))
        val saved = client.put("/api/config") {
            contentType(ContentType.Application.Json)
            setBody(updated)
        }.body<PriestessConfig>()
        assertEquals(18081, saved.server.port)

        val backups = client.get("/api/config/backups").body<List<ConfigBackup>>()
        assertEquals(initialBackupCount + 1, backups.size)
        val latestBackup = backups.first()
        assertTrue(latestBackup.id.endsWith(".json"))
        assertTrue(latestBackup.sizeBytes > 0)
        assertFalse(backups.toString().contains("secret-provider-key"))
        assertFalse(backups.toString().contains("secret-platform-token"))

        val reloaded = client.post("/api/config/reload").body<PriestessConfig>()
        assertEquals(18081, reloaded.server.port)

        val restored = client.post("/api/config/backups/${latestBackup.id}/restore").body<PriestessConfig>()
        assertEquals(18080, restored.server.port)
        assertEquals(18080, client.get("/api/config").body<PriestessConfig>().server.port)

        val platforms = client.get("/api/platforms").body<List<PlatformStatusDto>>()
        assertTrue(platforms.any { it.name == "health-platform" && !it.enabled && !it.running })
        val startedPlatformConfig = client.post("/api/platforms/health-platform/start").body<PriestessConfig>()
        assertTrue(startedPlatformConfig.platforms.single { it.name == "health-platform" }.enabled)
        val stoppedPlatformConfig = client.post("/api/platforms/health-platform/stop").body<PriestessConfig>()
        assertFalse(stoppedPlatformConfig.platforms.single { it.name == "health-platform" }.enabled)
        assertTrue(client.get("/api/providers").body<List<ProviderDto>>().isNotEmpty())
        assertTrue(client.get("/api/tools").body<List<ToolDto>>().isNotEmpty())
        assertTrue(client.get("/api/conversations").body<List<ConversationDto>>().isEmpty())
        assertTrue(client.get("/api/plugins").body<PluginListResponse>().plugins.isEmpty())

        client.webSocket("/ws/logs") {
            val frame = incoming.receive() as Frame.Text
            assertTrue(frame.readText().contains("connected"))
        }
    }

    @Test
    fun `configured dashboard api token protects api and log websocket while health stays public`() = testApplication {
        val service = testService()
        application {
            configureDashboardApplication(service, corsEnabled = false, apiToken = "dashboard-secret")
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
            install(WebSockets)
        }

        val health = client.get("/health")
        assertEquals(HttpStatusCode.OK, health.status)
        val metrics = client.get("/metrics")
        assertEquals(HttpStatusCode.OK, metrics.status)

        val unauthorized = client.get("/api/config")
        assertEquals(HttpStatusCode.Unauthorized, unauthorized.status)

        val wrong = client.get("/api/config") {
            header(io.ktor.http.HttpHeaders.Authorization, "Bearer wrong")
        }
        assertEquals(HttpStatusCode.Unauthorized, wrong.status)

        val authorized = client.get("/api/config") {
            header(io.ktor.http.HttpHeaders.Authorization, "Bearer dashboard-secret")
        }
        assertEquals(HttpStatusCode.OK, authorized.status)
        assertEquals(18080, authorized.body<PriestessConfig>().server.port)

        val socketWithoutToken = runCatching {
            client.webSocket("/ws/logs") {
                incoming.receive()
            }
        }
        assertTrue(socketWithoutToken.isFailure)

        client.webSocket("/ws/logs?token=dashboard-secret") {
            val frame = incoming.receive() as Frame.Text
            assertTrue(frame.readText().contains("connected"))
        }
    }

    @Test
    fun `log websocket sends buffered and live runtime events`() = testApplication {
        DashboardLogHub.clearForTest()
        DashboardLogHub.publish(LogEventDto(level = "WARN", message = "buffered event", timestamp = 10L))
        val service = testService()
        application {
            configureDashboardApplication(service, corsEnabled = false)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
            install(WebSockets)
        }

        client.webSocket("/ws/logs") {
            val connected = (incoming.receive() as Frame.Text).readText()
            assertTrue(connected.contains("connected"))

            val buffered = receiveLogEvent("buffered event")
            assertEquals("WARN", buffered.level)
            assertEquals("buffered event", buffered.message)

            DashboardLogHub.publish(LogEventDto(level = "ERROR", message = "live event", timestamp = 20L))
            val live = receiveLogEvent("live event")
            assertEquals("ERROR", live.level)
            assertEquals("live event", live.message)
        }
        DashboardLogHub.clearForTest()
    }

    @Test
    fun `dashboard log hub keeps a bounded recent buffer`() = kotlinx.coroutines.runBlocking {
        DashboardLogHub.clearForTest()

        repeat(205) { index ->
            DashboardLogHub.publish(LogEventDto(level = "INFO", message = "event-$index", timestamp = index.toLong()))
        }

        val recent = DashboardLogHub.recent()
        assertEquals(200, recent.size)
        assertEquals("event-5", recent.first().message)
        assertEquals("event-204", recent.last().message)
        DashboardLogHub.clearForTest()
    }

    private suspend fun DefaultClientWebSocketSession.receiveLogEvent(
        message: String,
    ): LogEventDto = withTimeout(5_000) {
        while (true) {
            val text = (incoming.receive() as Frame.Text).readText()
            val event = runCatching { Json.decodeFromString<LogEventDto>(text) }.getOrNull()
            if (event?.message == message) {
                return@withTimeout event
            }
        }
        error("unreachable")
    }

    @Test
    fun `dashboard frontend shell static assets and spa fallback are served`() = testApplication {
        val service = testService()
        application {
            configureDashboardApplication(service, corsEnabled = false)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val root = client.get("/")
        assertEquals(HttpStatusCode.OK, root.status)
        assertTrue(root.bodyAsText().contains("Priestess Dashboard Test Shell"))

        val nested = client.get("/plugins")
        assertEquals(HttpStatusCode.OK, nested.status)
        assertTrue(nested.bodyAsText().contains("Priestess Dashboard Test Shell"))

        val asset = client.get("/assets/app-test.js")
        assertEquals(HttpStatusCode.OK, asset.status)
        assertTrue(asset.bodyAsText().contains("__priestessDashboardTest"))

        val config = client.get("/api/config")
        assertEquals(HttpStatusCode.OK, config.status)
        assertEquals(18080, config.body<PriestessConfig>().server.port)
    }

    @Test
    fun `plugin load and unload routes update plugin state`() = testApplication {
        val pluginRoot = Files.createTempDirectory("priestess-dashboard-plugins")
        val pluginDir = pluginRoot.resolve("bad")
        Files.createDirectories(pluginDir)
        Files.writeString(
            pluginDir.resolve("plugin.json"),
            Json.encodeToString(
                PluginManifest(
                    id = "bad",
                    name = "Bad",
                    entrypoint = "missing.Plugin",
                ),
            ),
        )
        val service = testService(pluginRoot.toString())
        service.discoverPlugins()
        application {
            configureDashboardApplication(service, corsEnabled = false)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val loadResponse = client.post("/api/plugins/bad/load").body<PluginListResponse>()
        assertEquals(PluginState.FAILED, loadResponse.plugins.single().state)

        val unloadResponse = client.post("/api/plugins/bad/unload").body<PluginListResponse>()
        assertEquals(PluginState.DISCOVERED, unloadResponse.plugins.single().state)
    }

    @Test
    fun `plugin tools providers and platforms appear in dashboard listings`() = testApplication {
        val pluginRoot = Files.createTempDirectory("priestess-dashboard-tool-plugins")
        val pluginDir = pluginRoot.resolve("demo")
        Files.createDirectories(pluginDir)
        buildJavaPluginJar(pluginDir.toFile())
        Files.writeString(
            pluginDir.resolve("plugin.json"),
            Json.encodeToString(
                PluginManifest(
                    id = "demo",
                    name = "Demo",
                    entrypoint = "demo.plugin.DemoPlugin",
                    capabilities = listOf("tool", "provider", "platform"),
                ),
            ),
        )
        val service = testService(
            pluginDirectory = pluginRoot.toString(),
            platforms = listOf(PlatformConfig(name = "demo-platform", type = "demo-platform")),
        )
        service.discoverPlugins()
        service.enablePlugin("demo")
        application {
            configureDashboardApplication(service, corsEnabled = false)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val tools = client.get("/api/tools").body<List<ToolDto>>()
        val providers = client.get("/api/providers").body<List<ProviderDto>>()
        val platforms = client.get("/api/platforms").body<List<PlatformStatusDto>>()
        val providerTests = client.post("/api/providers/test").body<Map<String, Boolean>>()

        assertTrue(tools.any {
            it.name == "demo-tool" &&
                it.description == "Demo tool" &&
                it.source.name == "PLUGIN" &&
                it.owner == "demo"
        })
        assertTrue(tools.any {
            it.name == "list_tools" &&
                it.source.name == "BUILTIN" &&
                it.riskLevel.name == "SAFE_READ" &&
                it.defaultEnabled &&
                it.effectiveEnabled
        })
        assertTrue(providers.any { it.name == "demo-provider" && it.displayName == "Demo Provider" })
        assertTrue(platforms.any { it.name == "demo-platform" && it.type == "demo-platform" })
        assertEquals(true, providerTests["demo-provider"])
    }

    @Test
    fun `workspace routes expose status detail scoped resources and reload`() = testApplication {
        val service = testService()
        application {
            configureDashboardApplication(service, corsEnabled = false)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val list = client.get("/api/workspaces").body<WorkspaceListResponse>()
        val defaultStatus = list.workspaces.single { it.id == "default" }
        assertEquals("Default Workspace", defaultStatus.name)
        assertTrue(defaultStatus.enabled)
        assertNotNull(defaultStatus.activeSnapshotVersion)

        val detail = client.get("/api/workspaces/default").body<WorkspaceDetailDto>()
        assertEquals("default", detail.status.id)
        assertEquals("test-provider", detail.providerName)
        assertTrue(detail.agents.contains("dashboard-agent"))
        assertTrue(detail.tools.contains("list_tools"))
        assertTrue(detail.memory.enabled)

        val tools = client.get("/api/workspaces/default/tools").body<WorkspaceResourceListResponse>()
        assertEquals("default", tools.workspaceId)
        assertTrue(tools.resources.contains("list_tools"))

        val skills = client.get("/api/workspaces/default/skills").body<WorkspaceResourceListResponse>()
        assertEquals("default", skills.workspaceId)

        val memory = client.get("/api/workspaces/default/memory").body<com.heyanle.priestess.bot.workspace.WorkspaceMemoryPolicyConfig>()
        assertEquals(5, memory.maxInjectedMemories)

        val reload = client.post("/api/workspaces/default/reload").body<com.heyanle.priestess.bot.workspace.WorkspaceReloadResult>()
        assertTrue(reload.success)
        assertEquals("default", reload.workspaceId)

        val reloadAll = client.post("/api/workspaces/reload").body<List<com.heyanle.priestess.bot.workspace.WorkspaceReloadResult>>()
        assertTrue(reloadAll.any { it.workspaceId == "default" && it.success })
    }

    @Test
    fun `agent chat route returns final response`() = testApplication {
        val service = testService(testProvider = ScriptedProvider(finalContent = "dashboard reply"))
        application {
            configureDashboardApplication(service, corsEnabled = false)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val response = client.post("/api/agent/chat") {
            contentType(ContentType.Application.Json)
            setBody(AgentChatRequest(message = "hello"))
        }.body<AgentChatResponse>()

        assertEquals("FINAL", response.status)
        assertEquals("dashboard reply", response.content)
        assertEquals("test-provider", response.providerName)
        assertTrue(response.events.any { it.type == "agent_begin" })
        assertTrue(response.events.any { it.type == "agent_done" })
    }

    @Test
    fun `agent chat route returns missing provider error`() = testApplication {
        val service = testService(testProvider = null, providerName = "missing-provider")
        application {
            configureDashboardApplication(service, corsEnabled = false)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val response = client.post("/api/agent/chat") {
            contentType(ContentType.Application.Json)
            setBody(AgentChatRequest(message = "hello"))
        }.body<AgentChatResponse>()

        assertEquals("ERROR", response.status)
        assertTrue(response.content.contains("missing-provider"))
    }

    @Test
    fun `agent chat route captures tool execution events`() = testApplication {
        val service = testService(testProvider = ScriptedProvider(toolFirst = true, finalContent = "after tool"))
        application {
            configureDashboardApplication(service, corsEnabled = false)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val response = client.post("/api/agent/chat") {
            contentType(ContentType.Application.Json)
            setBody(AgentChatRequest(message = "use tool"))
        }.body<AgentChatResponse>()

        assertEquals("FINAL", response.status)
        assertEquals("after tool", response.content)
        assertTrue(response.events.any { it.type == "tool_start" && it.toolName == "dashboard_echo" })
        assertTrue(response.events.any { it.type == "tool_end" && it.toolName == "dashboard_echo" && it.success == true })
    }

    @Test
    fun `agent chat route includes persona and memory injection trace`() = testApplication {
        val service = testService(testProvider = ScriptedProvider(finalContent = "trace reply"))
        val persona = service.personaCaseForTest().upsert(
            PersonaUpsertRequest(
                workspaceId = "default",
                name = "Trace Persona",
                tone = "precise",
                systemPromptTemplate = "Use remembered preferences.",
                agentNames = listOf("dashboard-agent"),
            ),
        )
        val memory = service.memoryCaseForTest().save(
            content = "User likes concise Kotlin examples",
            type = MemoryType.PREFERENCE,
            scope = MemoryScope.SESSION,
            scopeContext = com.heyanle.priestess.bot.memory.MemoryScopeContext(
                workspaceId = "default",
                sessionId = "trace-session",
                userId = "trace-user",
                agentName = "dashboard-agent",
            ),
        )
        application {
            configureDashboardApplication(service, corsEnabled = false)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val response = client.post("/api/agent/chat") {
            contentType(ContentType.Application.Json)
            setBody(
                AgentChatRequest(
                    message = "Need concise Kotlin",
                    sessionId = "trace-session",
                    userId = "trace-user",
                ),
            )
        }.body<AgentChatResponse>()

        assertEquals("FINAL", response.status)
        assertEquals(persona.id, response.injectionTrace.personaId)
        assertEquals("Trace Persona", response.injectionTrace.personaName)
        assertEquals(1, response.injectionTrace.memoryCount)
        assertEquals(memory.id, response.injectionTrace.memories.single().id)
        assertTrue(response.injectionTrace.metadata.getValue("injected_memory_ids").contains(memory.id))
    }

    @Test
    fun `agent chat route exposes timed out tool event details`() = testApplication {
        val service = testService(
            testProvider = ScriptedProvider(toolFirst = true, finalContent = "after timeout"),
            echoToolDelayMs = 100,
            toolTimeoutSeconds = 0,
        )
        application {
            configureDashboardApplication(service, corsEnabled = false)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val response = client.post("/api/agent/chat") {
            contentType(ContentType.Application.Json)
            setBody(AgentChatRequest(message = "use slow tool"))
        }.body<AgentChatResponse>()

        assertEquals("FINAL", response.status)
        assertEquals("after timeout", response.content)
        assertTrue(response.events.any {
            it.type == "tool_end" &&
                it.toolName == "dashboard_echo" &&
                it.success == false &&
                it.errorCode == "TIMEOUT"
        })
    }

    @Test
    fun `conversation messages route returns chronological limited history with tool metadata`() = testApplication {
        val service = testService()
        val conversation = service.seedConversationForTest("napcat", "group-1")
        service.seedMessageForTest(conversation.id, MessageRole.USER, content = "first")
        service.seedMessageForTest(conversation.id, MessageRole.ASSISTANT, content = "second", toolCalls = """[{"name":"lookup"}]""")
        service.seedMessageForTest(conversation.id, MessageRole.TOOL, content = "third", toolCallId = "call-1")

        application {
            configureDashboardApplication(service, corsEnabled = false)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val conversations = client.get("/api/conversations").body<List<ConversationDto>>()
        assertTrue(conversations.any { it.id == conversation.id && it.platform == "napcat" && it.sessionId == "group-1" })

        val allMessages = client.get("/api/conversations/${conversation.id}/messages").body<List<MessageDto>>()
        assertEquals(listOf("first", "second", "third"), allMessages.map { it.content })
        assertEquals("""[{"name":"lookup"}]""", allMessages[1].toolCalls)
        assertEquals("call-1", allMessages[2].toolCallId)

        val limited = client.get("/api/conversations/${conversation.id}/messages?count=2").body<List<MessageDto>>()
        assertEquals(listOf("second", "third"), limited.map { it.content })
    }

    @Test
    fun `knowledge routes create base add document and search`() = testApplication {
        val service = testService()
        application {
            configureDashboardApplication(service, corsEnabled = false)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val created = client.post("/api/knowledge/bases") {
            contentType(ContentType.Application.Json)
            setBody(CreateKnowledgeBaseRequest(name = "Ops", description = "Operations"))
        }.body<KnowledgeBaseListResponse>()
        val base = created.bases.single { it.name == "Ops" }

        val chunks = client.post("/api/knowledge/bases/${base.id}/documents") {
            contentType(ContentType.Application.Json)
            setBody(
                AddKnowledgeDocumentRequest(
                    documentName = "nas.md",
                    content = "NAS deployment includes Dashboard assets.\n\nOther content is less relevant.",
                ),
            )
        }.body<List<com.heyanle.priestess.bot.knowledge.KnowledgeChunk>>()
        assertTrue(chunks.isNotEmpty())

        val results = client.post("/api/knowledge/search") {
            contentType(ContentType.Application.Json)
            setBody(KnowledgeSearchRequest(query = "Dashboard NAS", knowledgeBaseId = base.id, limit = 1))
        }.body<List<KnowledgeSearchResultDto>>()

        assertEquals(1, results.size)
        assertEquals("nas.md", results.single().chunk.documentName)
        assertTrue(results.single().score > 0.0)
    }

    @Test
    fun `persona and memory routes manage records and enforce scoped search`() = testApplication {
        val service = testService()
        application {
            configureDashboardApplication(service, corsEnabled = false)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val persona = client.post("/api/personas") {
            contentType(ContentType.Application.Json)
            setBody(
                PersonaUpsertDto(
                    workspaceId = "default",
                    name = "Careful Assistant",
                    tone = "careful",
                    boundaries = listOf("Do not leak secrets"),
                    systemPromptTemplate = "Ask clarifying questions.",
                    agentNames = listOf("dashboard-agent"),
                ),
            )
        }.body<com.heyanle.priestess.bot.persona.Persona>()
        assertEquals("Careful Assistant", persona.name)

        val personas = client.get("/api/personas?workspaceId=default").body<PersonaListResponse>()
        assertEquals(listOf(persona.id), personas.personas.map { it.id })

        val resolved = client.post("/api/personas/resolve") {
            contentType(ContentType.Application.Json)
            setBody(PersonaResolveRequest(workspaceId = "default", agentName = "dashboard-agent"))
        }.body<PersonaResolveResponse>()
        assertEquals(persona.id, resolved.persona?.id)

        val memory = client.post("/api/memory") {
            contentType(ContentType.Application.Json)
            setBody(
                MemorySaveRequest(
                    workspaceId = "default",
                    scope = MemoryScope.SESSION,
                    sessionId = "session-1",
                    content = "User prefers concise Kotlin answers",
                    type = MemoryType.PREFERENCE,
                    tags = listOf("kotlin"),
                ),
            )
        }.body<com.heyanle.priestess.bot.memory.MemoryRecord>()
        assertEquals("User prefers concise Kotlin answers", memory.content)

        client.post("/api/memory") {
            contentType(ContentType.Application.Json)
            setBody(
                MemorySaveRequest(
                    workspaceId = "default",
                    scope = MemoryScope.SESSION,
                    sessionId = "session-2",
                    content = "Hidden session memory",
                    type = MemoryType.PREFERENCE,
                    tags = listOf("kotlin"),
                ),
            )
        }
        val expiredMemory = client.post("/api/memory") {
            contentType(ContentType.Application.Json)
            setBody(
                MemorySaveRequest(
                    workspaceId = "default",
                    scope = MemoryScope.SESSION,
                    sessionId = "session-1",
                    content = "Expired memory should be cleaned",
                    type = MemoryType.FACT,
                    tags = listOf("expired"),
                    expiresAt = System.currentTimeMillis() - 1_000,
                ),
            )
        }.body<com.heyanle.priestess.bot.memory.MemoryRecord>()

        val listed = client.get("/api/memory?workspaceId=default&sessionId=session-1&type=PREFERENCE&tag=kotlin").body<MemoryListResponse>()
        assertTrue(listed.memories.any { it.id == memory.id })
        val listedExpired = client.get("/api/memory?workspaceId=default&sessionId=session-1&tag=expired").body<MemoryListResponse>()
        assertFalse(listedExpired.memories.any { it.id == expiredMemory.id })

        val search = client.post("/api/memory/search") {
            contentType(ContentType.Application.Json)
            setBody(
                MemorySearchRequest(
                    query = "Kotlin answers",
                    workspaceId = "default",
                    sessionId = "session-1",
                    limit = 5,
                ),
            )
        }.body<MemorySearchResponse>()
        assertEquals(listOf(memory.id), search.results.map { it.record.id })
        assertTrue(search.results.single().matchReason.contains("content"))

        val deletedMemory = client.delete("/api/memory/${memory.id}?workspaceId=default&sessionId=session-1").body<DeleteResponse>()
        assertTrue(deletedMemory.deleted)
        val deletedPersona = client.delete("/api/personas/${persona.id}").body<DeleteResponse>()
        assertTrue(deletedPersona.deleted)
        val expired = client.post("/api/memory/expire").body<ExpireMemoryResponse>()
        assertTrue(expired.expired >= 1)
        val listedAfterExpire = client.get("/api/memory?workspaceId=default&sessionId=session-1&tag=expired").body<MemoryListResponse>()
        assertFalse(listedAfterExpire.memories.any { it.id == expiredMemory.id })
    }

    @Test
    fun `sub-agent routes read replace config and test selected execution`() = testApplication {
        val service = testService(testProvider = ScriptedProvider(finalContent = "code agent reply"))
        application {
            configureDashboardApplication(service, corsEnabled = false)
        }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }

        val initial = client.get("/api/sub-agents/config").body<SubAgentOrchestrationConfig>()
        assertEquals(false, initial.enabled)

        val updated = SubAgentOrchestrationConfig(
            enabled = true,
            defaultAgentName = "general-agent",
            agents = listOf(
                SubAgentConfig(
                    name = "general-agent",
                    agent = AgentConfig(name = "general-agent", providerName = "test-provider", model = "test-model"),
                ),
                SubAgentConfig(
                    name = "code-agent",
                    agent = AgentConfig(name = "code-agent", providerName = "test-provider", model = "test-model"),
                ),
            ),
            routes = listOf(
                SubAgentRouteConfig(
                    name = "code-route",
                    targetAgentName = "code-agent",
                    keywords = listOf("code", "review"),
                    priority = 10,
                ),
            ),
        )

        val saved = client.put("/api/sub-agents/config") {
            contentType(ContentType.Application.Json)
            setBody(updated)
        }.body<SubAgentOrchestrationConfig>()
        assertEquals(true, saved.enabled)
        assertEquals("general-agent", saved.defaultAgentName)

        val response = client.post("/api/sub-agents/test") {
            contentType(ContentType.Application.Json)
            setBody(SubAgentTestRequest(message = "please review this code"))
        }.body<SubAgentTestResponse>()

        assertEquals("FINAL", response.status)
        assertEquals("code-agent", response.selectedAgentName)
        assertEquals("code-route", response.selectedRouteName)
        assertEquals("keyword_match", response.selectionReason)
        assertEquals("code agent reply", response.content)
        assertTrue(response.events.any { it.type == "agent_begin" })
        assertTrue(response.events.any { it.type == "agent_done" })
    }

    private fun testService(
        pluginDirectory: String? = null,
        platforms: List<PlatformConfig> = emptyList(),
        testProvider: ScriptedProvider? = ScriptedProvider(),
        providerName: String = "test-provider",
        echoToolDelayMs: Long = 0,
        toolTimeoutSeconds: Long = 30,
    ): DashboardService {
        ProviderRegistry.unregister("test-provider")
        if (testProvider != null) {
            ProviderRegistry.register(testProvider.metadata) { testProvider }
        }
        val configPath = Files.createTempDirectory("priestess-dashboard-config").resolve("config.json")
        val dbPath = Files.createTempFile("priestess-dashboard", ".sqlite")
        val configController = ConfigController(configPath.toString())
        configController.replace(
            PriestessConfig(
                platforms = platforms,
                providers = listOf(
                    ProviderConfig(
                        name = providerName,
                        type = providerName,
                        model = "test-model",
                        apiKey = "secret-provider-key",
                    ),
                ),
                agent = com.heyanle.priestess.bot.config.AgentConfig(
                    name = "dashboard-agent",
                    providerName = providerName,
                    model = "test-model",
                    maxSteps = 4,
                    toolTimeoutSeconds = toolTimeoutSeconds,
                ),
                server = ServerConfig(enabled = true, port = 18080),
                plugins = PluginConfig(directory = pluginDirectory ?: "plugins", autoDiscover = false),
            ),
        )
        val configCase = ConfigCase(configController)
        val db = DatabaseController(dbPath.toString())
        val conversationCase = ConversationCase(ConversationController(db), MessageHistory(db))
        val knowledgeCase = KnowledgeCase(KnowledgeController(db))
        val memoryCase = MemoryCase(MemoryController(db))
        val personaCase = PersonaCase(PersonaController(db))
        val toolController = ToolController()
        val contextManager = ContextManager(TokenCounter())
        val metricsRegistry = MetricsRegistry()
        val toolExecutor = ToolExecutor(toolController, metricsRegistry)
        val providerController = ProviderController(configCase)
        val providerCase = ProviderCase(providerController)
        val pluginRegistry = PluginExtensionRegistry()
        val pluginCase = PluginCase(
            PluginController(
                configCase = configCase,
                extensionRegistry = pluginRegistry,
                toolController = toolController,
                providerController = providerController,
            ),
            pluginRegistry,
        )
        val platformCase = PlatformCase(pipelineCaseProvider = { error("Pipeline is not used in route tests") })
        val platformController = PlatformController(configCase, platformCase)
        val healthProvider = RuntimeHealthProvider(
            configController = configController,
            configCase = configCase,
            platformController = platformController,
            providerCase = providerCase,
            toolController = toolController,
            pluginCase = pluginCase,
        )
        registerBuiltinTools(
            registry = toolController,
            knowledgeCaseProvider = { knowledgeCase },
            healthProvider = { healthProvider },
            conversationCaseProvider = { conversationCase },
            memoryCaseProvider = { memoryCase },
        )
        toolController.register(DashboardEchoTool(delayMs = echoToolDelayMs))
        val agentCase = AgentCase()
        val subAgentOrchestrator = SubAgentOrchestrator(
            agentCase = agentCase,
            contextManager = contextManager,
            providerCase = providerCase,
            toolExecutor = toolExecutor,
            toolController = toolController,
        )
        val workspaceController = WorkspaceController(
            source = ConfigBackedWorkspaceConfigSource(configCase),
            toolController = toolController,
            skillCase = SkillCase(SkillController()),
            nowProvider = { 1_000L },
        )

        return DashboardService(
            configController = configController,
            configCase = configCase,
            platformController = platformController,
            providerCase = providerCase,
            toolController = toolController,
            conversationCase = conversationCase,
            pluginCase = pluginCase,
            agentCase = agentCase,
            contextManager = contextManager,
            toolExecutor = toolExecutor,
            knowledgeCase = knowledgeCase,
            subAgentOrchestrator = subAgentOrchestrator,
            metricsRegistry = metricsRegistry,
            healthProvider = healthProvider,
            workspaceController = workspaceController,
            personaCase = personaCase,
            memoryCase = memoryCase,
            personaMemoryInjector = PersonaMemoryInjector(personaCase, memoryCase),
        )
    }

    private class ScriptedProvider(
        private val toolFirst: Boolean = false,
        private val finalContent: String = "ok",
    ) : ChatProvider {
        private var calls = 0
        override val metadata = ProviderMetadata(
            name = "test-provider",
            displayName = "Test Provider",
            kind = LLMKind.OPENAI,
            supportToolCalling = true,
            supportVision = false,
            supportStreaming = false,
        )
        override val config = ProviderConfig(name = "test-provider", type = "test-provider", model = "test-model")

        override suspend fun test(): Boolean = true

        override suspend fun textChat(request: LLMRequest): LLMResponse {
            calls++
            return if (toolFirst && calls == 1) {
                LLMResponse(
                    content = "checking",
                    toolCalls = listOf(ToolCall(id = "call-1", name = "dashboard_echo", arguments = """{"text":"hello"}""")),
                    finishReason = "tool_calls",
                    tokenUsage = TokenUsage(),
                )
            } else {
                LLMResponse(content = finalContent, finishReason = "stop", tokenUsage = TokenUsage())
            }
        }

        override suspend fun getModels(): List<String> = listOf("test-model")
    }

    private class DashboardEchoTool(
        private val delayMs: Long = 0,
    ) : FunctionTool() {
        override val schema = ToolSchema("dashboard_echo", "Dashboard echo tool", ToolParameters())

        override suspend fun execute(context: AgentToolContext, args: Map<String, String>): ToolResult {
            if (delayMs > 0) delay(delayMs)
            return ToolResult.success(args["text"] ?: "echo")
        }
    }

    private fun DashboardService.seedConversationForTest(platform: String, sessionId: String) =
        conversationCaseForTest().getOrCreate(platform, sessionId)

    private fun DashboardService.seedMessageForTest(
        conversationId: String,
        role: MessageRole,
        content: String? = null,
        toolCalls: String? = null,
        toolCallId: String? = null,
    ) = conversationCaseForTest().storeMessage(conversationId, role, content, toolCalls, toolCallId)

    private fun DashboardService.conversationCaseForTest(): ConversationCase {
        val field = DashboardService::class.java.getDeclaredField("conversationCase")
        field.isAccessible = true
        return field.get(this) as ConversationCase
    }

    private fun DashboardService.personaCaseForTest(): PersonaCase {
        val field = DashboardService::class.java.getDeclaredField("personaCase")
        field.isAccessible = true
        return field.get(this) as PersonaCase
    }

    private fun DashboardService.memoryCaseForTest(): MemoryCase {
        val field = DashboardService::class.java.getDeclaredField("memoryCase")
        field.isAccessible = true
        return field.get(this) as MemoryCase
    }

    private fun DashboardService.healthProviderForTest(): RuntimeHealthProvider {
        val field = DashboardService::class.java.getDeclaredField("healthProvider")
        field.isAccessible = true
        return field.get(this) as RuntimeHealthProvider
    }

    private fun buildJavaPluginJar(pluginDir: File) {
        val sourceRoot = pluginDir.resolve("src")
        val classesDir = pluginDir.resolve("classes")
        val packageDir = sourceRoot.resolve("demo/plugin")
        packageDir.mkdirs()
        classesDir.mkdirs()
        val source = packageDir.resolve("DemoPlugin.java")
        source.writeText(
            """
            package demo.plugin;

            import com.heyanle.priestess.bot.plugin.Plugin;
            import com.heyanle.priestess.bot.plugin.PluginContext;
            import com.heyanle.priestess.bot.config.PlatformConfig;
            import com.heyanle.priestess.bot.config.ProviderConfig;
            import com.heyanle.priestess.bot.platform.MessageChain;
            import com.heyanle.priestess.bot.platform.MessageSession;
            import com.heyanle.priestess.bot.platform.Platform;
            import com.heyanle.priestess.bot.platform.PlatformMetadata;
            import com.heyanle.priestess.bot.provider.ChatProvider;
            import com.heyanle.priestess.bot.provider.LLMKind;
            import com.heyanle.priestess.bot.provider.ProviderMetadata;
            import com.heyanle.priestess.bot.provider.model.LLMRequest;
            import com.heyanle.priestess.bot.provider.model.LLMResponse;
            import com.heyanle.priestess.bot.provider.model.TokenUsage;
            import com.heyanle.priestess.bot.tool.AgentToolContext;
            import com.heyanle.priestess.bot.tool.FunctionTool;
            import com.heyanle.priestess.bot.tool.ToolParameters;
            import com.heyanle.priestess.bot.tool.ToolResult;
            import com.heyanle.priestess.bot.tool.ToolSchema;
            import java.util.Map;
            import kotlinx.coroutines.Job;
            import kotlinx.coroutines.JobKt;

            public class DemoPlugin implements Plugin {
                @Override
                public void onEnable(PluginContext context) {
                    context.registerTool(new DemoTool());
                    context.registerProvider(new DemoProvider());
                    context.registerPlatform(
                        new PlatformMetadata("demo-platform", "Demo Platform", false, true),
                        (PlatformConfig config) -> new DemoPlatform()
                    );
                }

                public static class DemoTool extends FunctionTool {
                    private final ToolSchema schema = new ToolSchema("demo-tool", "Demo tool", new ToolParameters());

                    @Override
                    public ToolSchema getSchema() {
                        return schema;
                    }

                    @Override
                    public Object execute(AgentToolContext context, Map<String, String> args, kotlin.coroutines.Continuation<? super ToolResult> continuation) {
                        return ToolResult.Companion.success("ok");
                    }
                }

                public static class DemoProvider implements ChatProvider {
                    private final ProviderMetadata metadata = new ProviderMetadata(
                        "demo-provider",
                        "Demo Provider",
                        LLMKind.OPENAI,
                        false,
                        false,
                        false
                    );
                    private final ProviderConfig config = new ProviderConfig(
                        "demo-provider",
                        "demo-provider",
                        "demo-model",
                        "",
                        "",
                        true,
                        new java.util.LinkedHashMap<String, String>()
                    );

                    @Override
                    public ProviderMetadata getMetadata() {
                        return metadata;
                    }

                    @Override
                    public ProviderConfig getConfig() {
                        return config;
                    }

                    @Override
                    public Object test(kotlin.coroutines.Continuation<? super Boolean> continuation) {
                        return Boolean.TRUE;
                    }

                    @Override
                    public Object textChat(LLMRequest request, kotlin.coroutines.Continuation<? super LLMResponse> continuation) {
                        return new LLMResponse("plugin response", new java.util.ArrayList<>(), "stop", new TokenUsage());
                    }

                    @Override
                    public Object getModels(kotlin.coroutines.Continuation<? super java.util.List<String>> continuation) {
                        return java.util.Collections.singletonList("demo-model");
                    }
                }

                public static class DemoPlatform extends Platform {
                    private final PlatformMetadata metadata = new PlatformMetadata(
                        "demo-platform",
                        "Demo Platform",
                        false,
                        true
                    );

                    @Override
                    public PlatformMetadata getMetadata() {
                        return metadata;
                    }

                    @Override
                    public Object run(kotlin.coroutines.Continuation<? super Job> continuation) {
                        return JobKt.Job(null);
                    }

                    @Override
                    public Object terminate(kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
                        return kotlin.Unit.INSTANCE;
                    }

                    @Override
                    public Object sendMessage(MessageSession session, MessageChain chain, kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
                        return kotlin.Unit.INSTANCE;
                    }
                }
            }
            """.trimIndent(),
        )
        val compiler = ToolProvider.getSystemJavaCompiler()
        assertNotNull(compiler, "Tests require a JDK with javac")
        val compileExit = compiler.run(
            null,
            null,
            null,
            "-classpath",
            System.getProperty("java.class.path"),
            "-d",
            classesDir.absolutePath,
            source.absolutePath,
        )
        assertEquals(0, compileExit)
        val jarExit = ProcessBuilder(
            "jar",
            "--create",
            "--file",
            pluginDir.resolve("demo-plugin.jar").absolutePath,
            "-C",
            classesDir.absolutePath,
            ".",
        ).inheritIO().start().waitFor()
        assertEquals(0, jarExit)
    }
}
