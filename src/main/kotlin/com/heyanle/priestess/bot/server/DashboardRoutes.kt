package com.heyanle.priestess.bot.server

import com.heyanle.priestess.bot.config.PriestessConfig
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.defaultForFilePath
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import kotlinx.coroutines.flow.produceIn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Application.dashboardRoutes(service: DashboardService) {
    val json = Json { encodeDefaults = true }

    routing {
        get("/health") {
            call.respond(service.health())
        }
        get("/metrics") {
            call.respondText(service.metrics(), io.ktor.http.ContentType.Text.Plain)
        }

        route("/api") {
            get("/config") {
                call.respond(service.config())
            }
            put("/config") {
                call.respond(service.replaceConfig(call.receive<PriestessConfig>()))
            }
            post("/config/reload") {
                call.respond(service.reloadConfig())
            }
            get("/config/backups") {
                call.respond(service.configBackups())
            }
            post("/config/backups/{id}/restore") {
                val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                call.respond(service.restoreConfigBackup(id))
            }

            get("/platforms") {
                call.respond(service.platforms())
            }
            post("/platforms/{name}/start") {
                val name = call.parameters["name"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                call.respond(service.setPlatformEnabled(name, true))
            }
            post("/platforms/{name}/stop") {
                val name = call.parameters["name"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                call.respond(service.setPlatformEnabled(name, false))
            }

            get("/providers") {
                call.respond(service.providers())
            }
            post("/providers/test") {
                call.respond(service.testProviders())
            }

            get("/tools") {
                call.respond(service.tools())
            }

            get("/workspaces") {
                call.respond(service.workspaces())
            }
            post("/workspaces/reload") {
                call.respond(service.reloadWorkspaces())
            }
            get("/workspaces/{id}") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                call.respond(service.workspaceDetail(id))
            }
            post("/workspaces/{id}/reload") {
                val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                call.respond(service.reloadWorkspace(id))
            }
            get("/workspaces/{id}/tools") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                call.respond(service.workspaceTools(id))
            }
            get("/workspaces/{id}/mcp") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                call.respond(service.workspaceMcp(id))
            }
            get("/workspaces/{id}/skills") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                call.respond(service.workspaceSkills(id))
            }
            get("/workspaces/{id}/personas") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                call.respond(service.workspacePersonas(id))
            }
            get("/workspaces/{id}/memory") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                call.respond(service.workspaceMemory(id))
            }

            post("/agent/chat") {
                call.respond(service.chatAgent(call.receive<AgentChatRequest>()))
            }

            get("/personas") {
                val workspaceId = call.request.queryParameters["workspaceId"] ?: "default"
                call.respond(service.personas(workspaceId))
            }
            post("/personas") {
                call.respond(service.upsertPersona(call.receive<PersonaUpsertDto>()))
            }
            put("/personas/{id}") {
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<PersonaUpsertDto>()
                call.respond(service.upsertPersona(request.copy(id = id)))
            }
            delete("/personas/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                call.respond(service.deletePersona(id))
            }
            post("/personas/resolve") {
                call.respond(service.resolvePersona(call.receive<PersonaResolveRequest>()))
            }

            get("/memory") {
                val workspaceId = call.request.queryParameters["workspaceId"] ?: "default"
                val platformId = call.request.queryParameters["platformId"]
                val sessionId = call.request.queryParameters["sessionId"]
                val userId = call.request.queryParameters["userId"]
                val agentName = call.request.queryParameters["agentName"]
                val type = call.request.queryParameters["type"]
                val tag = call.request.queryParameters["tag"]
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                call.respond(service.memories(workspaceId, platformId, sessionId, userId, agentName, type, tag, limit))
            }
            post("/memory") {
                call.respond(service.saveMemory(call.receive<MemorySaveRequest>()))
            }
            post("/memory/search") {
                call.respond(service.searchMemory(call.receive<MemorySearchRequest>()))
            }
            delete("/memory/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val workspaceId = call.request.queryParameters["workspaceId"] ?: "default"
                val platformId = call.request.queryParameters["platformId"]
                val sessionId = call.request.queryParameters["sessionId"]
                val userId = call.request.queryParameters["userId"]
                val agentName = call.request.queryParameters["agentName"]
                call.respond(service.deleteMemory(id, workspaceId, platformId, sessionId, userId, agentName))
            }
            post("/memory/expire") {
                call.respond(service.expireMemory())
            }

            get("/sub-agents/config") {
                call.respond(service.subAgentConfig())
            }
            put("/sub-agents/config") {
                call.respond(service.replaceSubAgentConfig(call.receive()))
            }
            post("/sub-agents/test") {
                call.respond(service.testSubAgent(call.receive<SubAgentTestRequest>()))
            }

            get("/knowledge/bases") {
                call.respond(service.knowledgeBases())
            }
            post("/knowledge/bases") {
                call.respond(service.createKnowledgeBase(call.receive<CreateKnowledgeBaseRequest>()))
            }
            post("/knowledge/bases/{id}/documents") {
                val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                call.respond(service.addKnowledgeDocument(id, call.receive<AddKnowledgeDocumentRequest>()))
            }
            post("/knowledge/search") {
                call.respond(service.searchKnowledge(call.receive<KnowledgeSearchRequest>()))
            }

            get("/conversations") {
                call.respond(service.conversations())
            }
            get("/conversations/{id}/messages") {
                val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val count = call.request.queryParameters["count"]?.toIntOrNull() ?: 100
                call.respond(service.messages(id, count))
            }

            get("/plugins") {
                call.respond(service.plugins())
            }
            post("/plugins/discover") {
                call.respond(service.discoverPlugins())
            }
            post("/plugins/{id}/enable") {
                val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                call.respond(service.enablePlugin(id))
            }
            post("/plugins/{id}/disable") {
                val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                call.respond(service.disablePlugin(id))
            }
            post("/plugins/{id}/load") {
                val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                call.respond(service.loadPlugin(id))
            }
            post("/plugins/{id}/unload") {
                val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                call.respond(service.unloadPlugin(id))
            }
        }

        webSocket("/ws/logs") {
            val liveEvents = DashboardLogHub.events().produceIn(this)
            try {
                send(Frame.Text(json.encodeToString(LogEventDto(level = "INFO", message = "connected"))))
                for (event in DashboardLogHub.recent()) {
                    send(Frame.Text(json.encodeToString(event)))
                }
                for (event in liveEvents) {
                    send(Frame.Text(json.encodeToString(event)))
                }
            } finally {
                liveEvents.cancel()
            }
        }

        get("/assets/{path...}") {
            val path = call.parameters.getAll("path").orEmpty().joinToString("/")
            if (path.isBlank() || path.contains("..")) {
                return@get call.respond(HttpStatusCode.NotFound)
            }
            call.respondDashboardAsset(path)
        }

        get("/") {
            call.respondDashboardIndex()
        }
        get("/{...}") {
            val path = call.request.path()
            if (path.startsWith("/api/") || path.startsWith("/ws/")) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respondDashboardIndex()
            }
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.respondDashboardIndex() {
    val index = this::class.java.classLoader.getResource("dashboard/index.html")?.readText()
    if (index == null) {
        respond(HttpStatusCode.NotFound, mapOf("error" to "Dashboard frontend is not packaged"))
    } else {
        respondText(index, io.ktor.http.ContentType.Text.Html)
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.respondDashboardAsset(path: String) {
    val resourcePath = "dashboard/assets/$path"
    val bytes = this::class.java.classLoader.getResource(resourcePath)?.readBytes()
    if (bytes == null) {
        respond(HttpStatusCode.NotFound)
    } else {
        respondBytes(
            bytes = bytes,
            contentType = ContentType.defaultForFilePath(path),
            status = HttpStatusCode.OK,
        )
    }
}
