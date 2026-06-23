package com.heyanle.priestess.bot.observability

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder

class MetricsRegistry {
    private val counters = ConcurrentHashMap<MetricKey, LongAdder>()
    private val durations = ConcurrentHashMap<MetricKey, DurationValue>()

    fun incrementCounter(name: String, labels: Map<String, String> = emptyMap(), amount: Long = 1) {
        if (amount <= 0) return
        counters.computeIfAbsent(MetricKey(name, labels.toSortedMap())) { LongAdder() }.add(amount)
    }

    fun recordDuration(name: String, labels: Map<String, String> = emptyMap(), durationMillis: Long) {
        val value = durations.computeIfAbsent(MetricKey(name, labels.toSortedMap())) { DurationValue() }
        value.count.increment()
        value.sum.add(durationMillis.coerceAtLeast(0))
    }

    fun renderPrometheus(): String {
        val builder = StringBuilder()
        for (definition in definitions) {
            builder.append("# HELP ").append(definition.name).append(' ').append(definition.help).append('\n')
            builder.append("# TYPE ").append(definition.name).append(' ').append(definition.type).append('\n')
            when (definition.kind) {
                MetricKind.COUNTER -> appendCounters(builder, definition.name)
                MetricKind.DURATION -> appendDurations(builder, definition.name)
            }
        }
        return builder.toString()
    }

    private fun appendCounters(builder: StringBuilder, name: String) {
        counters.entries
            .filter { it.key.name == name }
            .sortedWith(compareBy({ it.key.name }, { it.key.labelText() }))
            .forEach { (key, value) ->
                builder.append(key.sampleName()).append(' ').append(value.sum()).append('\n')
            }
    }

    private fun appendDurations(builder: StringBuilder, name: String) {
        durations.entries
            .filter { it.key.name == name }
            .sortedWith(compareBy({ it.key.name }, { it.key.labelText() }))
            .forEach { (key, value) ->
                builder.append(key.sampleName("${name}_count")).append(' ').append(value.count.sum()).append('\n')
                builder.append(key.sampleName("${name}_sum")).append(' ').append(value.sum.sum()).append('\n')
            }
    }

    private data class DurationValue(
        val count: LongAdder = LongAdder(),
        val sum: LongAdder = LongAdder(),
    )

    private data class MetricKey(
        val name: String,
        val labels: Map<String, String>,
    ) {
        fun sampleName(sampleName: String = name): String {
            if (labels.isEmpty()) return sampleName
            return "$sampleName{${labelText()}}"
        }

        fun labelText(): String {
            return labels.entries.joinToString(",") { (key, value) ->
                "$key=\"${value.escapeLabelValue()}\""
            }
        }
    }

    private data class MetricDefinition(
        val name: String,
        val help: String,
        val type: String,
        val kind: MetricKind,
    )

    private enum class MetricKind {
        COUNTER,
        DURATION,
    }

    private companion object {
        val definitions = listOf(
            MetricDefinition(
                name = "priestess_pipeline_messages_total",
                help = "Total platform messages processed by the pipeline.",
                type = "counter",
                kind = MetricKind.COUNTER,
            ),
            MetricDefinition(
                name = "priestess_pipeline_duration_milliseconds",
                help = "Pipeline processing duration in milliseconds.",
                type = "summary",
                kind = MetricKind.DURATION,
            ),
            MetricDefinition(
                name = "priestess_llm_requests_total",
                help = "Total LLM requests issued by the runtime.",
                type = "counter",
                kind = MetricKind.COUNTER,
            ),
            MetricDefinition(
                name = "priestess_llm_request_duration_milliseconds",
                help = "LLM request duration in milliseconds.",
                type = "summary",
                kind = MetricKind.DURATION,
            ),
            MetricDefinition(
                name = "priestess_tool_calls_total",
                help = "Total tool calls attempted by agents.",
                type = "counter",
                kind = MetricKind.COUNTER,
            ),
        )

        private fun String.escapeLabelValue(): String {
            return replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"")
        }
    }
}
