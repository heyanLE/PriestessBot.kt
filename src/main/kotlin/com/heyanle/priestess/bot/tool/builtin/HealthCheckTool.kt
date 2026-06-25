package com.heyanle.priestess.bot.tool.builtin

import com.heyanle.priestess.bot.server.RuntimeHealthProvider
import com.heyanle.priestess.bot.tool.AgentToolContext
import com.heyanle.priestess.bot.tool.FunctionTool
import com.heyanle.priestess.bot.tool.ToolParameters
import com.heyanle.priestess.bot.tool.ToolResult
import com.heyanle.priestess.bot.tool.ToolRiskLevel
import com.heyanle.priestess.bot.tool.ToolSchema
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class HealthCheckTool(
    private val healthProvider: () -> RuntimeHealthProvider,
) : FunctionTool() {
    override val schema: ToolSchema = ToolSchema(
        name = "health_check",
        description = "Return a non-sensitive runtime health summary aligned with Dashboard health.",
        parameters = ToolParameters(),
        riskLevel = ToolRiskLevel.SAFE_READ,
        requiredCapabilities = emptyList(),
        defaultEnabled = true,
        auditLog = false,
    )

    private val json = Json {
        encodeDefaults = true
        prettyPrint = true
    }

    override suspend fun execute(context: AgentToolContext, args: Map<String, String>): ToolResult {
        return ToolResult.success(json.encodeToString(healthProvider().snapshot()))
    }
}
