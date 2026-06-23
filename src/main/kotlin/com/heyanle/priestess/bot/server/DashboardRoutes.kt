package com.heyanle.priestess.bot.server

import com.heyanle.priestess.bot.config.PriestessConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import kotlinx.coroutines.flow.collect
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

            post("/agent/chat") {
                call.respond(service.chatAgent(call.receive<AgentChatRequest>()))
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
            send(Frame.Text(json.encodeToString(LogEventDto(level = "INFO", message = "connected"))))
            for (event in DashboardLogHub.recent()) {
                send(Frame.Text(json.encodeToString(event)))
            }
            DashboardLogHub.events().collect { event ->
                send(Frame.Text(json.encodeToString(event)))
            }
        }

        staticResources("/assets", "dashboard/assets")

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
