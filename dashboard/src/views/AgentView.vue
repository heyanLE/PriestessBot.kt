<template>
  <div class="grid agent-console">
    <section class="panel command-deck">
      <div class="command-deck__chrome">
        <div class="command-deck__header">
          <div class="command-deck__title">
            <p class="console-kicker">Changes / Agent</p>
            <h2>{{ previewAgent?.name ?? 'Agent validation bench' }}</h2>
            <p>{{ commandSummary }}</p>
          </div>

          <div class="command-deck__badges">
            <span class="command-badge">Validation workflow</span>
            <span class="inline-status" :class="draftState.tone">{{ draftState.label }}</span>
          </div>
        </div>

        <div class="command-deck__grid">
          <div class="grid command-metric-grid">
            <article v-for="metric in commandMetrics" :key="metric.label" class="card command-metric">
              <span class="command-metric__label">{{ metric.label }}</span>
              <strong>{{ metric.value }}</strong>
              <p>{{ metric.detail }}</p>
            </article>
          </div>

          <article class="card command-brief">
            <div class="section-title compact">
              <div>
                <h3>Instruction summary</h3>
                <p>Translate the active agent settings into a compact runtime brief before testing.</p>
              </div>
              <span class="inline-status muted">Temp {{ formatDecimal(previewAgent?.temperature) }}</span>
            </div>

            <p class="command-brief__summary">{{ instructionPreview }}</p>

            <div class="detail-list command-detail-list">
              <div v-for="detail in operationalDetails" :key="detail.label" class="detail-item">
                <span>{{ detail.label }}</span>
                <strong>{{ detail.value }}</strong>
              </div>
            </div>
          </article>
        </div>
      </div>
    </section>

    <section class="panel config-panel">
      <div class="section-title">
        <div>
          <h2>Config Orchestration</h2>
          <p>{{ draftState.detail }}</p>
        </div>

        <div class="toolbar">
          <button type="button" @click="resetDraft" :disabled="!agent">Reset</button>
          <button type="button" class="primary" @click="saveConfig" :disabled="!store.config || !parsedDraft">Save</button>
        </div>
      </div>

      <div class="grid config-insights">
        <article class="card config-insight">
          <span class="config-insight__label">Instruction Core</span>
          <strong>{{ instructionLineCount }}</strong>
          <p>{{ instructionLineCount === 1 ? 'directive line' : 'directive lines' }}</p>
        </article>

        <article class="card config-insight">
          <span class="config-insight__label">Risk Gates</span>
          <strong>{{ allowedRiskCount }}</strong>
          <p>{{ toolPolicySummary }}</p>
        </article>

        <article class="card config-insight">
          <span class="config-insight__label">Token Budget</span>
          <strong>{{ previewAgent?.maxTokens ?? '--' }}</strong>
          <p>{{ previewAgent?.toolTimeoutSeconds ?? '--' }}s tool timeout</p>
        </article>
      </div>

      <textarea v-model="draft" class="agent-config-editor" spellcheck="false"></textarea>
      <p v-if="activeDraftError" class="notice error">{{ activeDraftError }}</p>
      <p v-else-if="saveNotice" class="notice ok">{{ saveNotice }}</p>
    </section>

    <section class="panel monitor-panel">
      <div class="section-title">
        <div>
          <h2>Session Monitor</h2>
          <p>{{ monitorSummary }}</p>
        </div>

        <div class="monitor-status">
          <span class="inline-status" :class="latestTrace?.personaId ? 'ok' : 'muted'">
            Persona {{ latestTrace?.personaName ?? 'none' }}
          </span>
          <span class="inline-status" :class="latestTrace && latestTrace.memoryCount > 0 ? 'ok' : 'muted'">
            Memories {{ latestTrace?.memoryCount ?? 0 }}
          </span>
          <span class="inline-status" :class="latestEvents.length > 0 ? 'warn' : 'muted'">
            Events {{ latestEvents.length }}
          </span>
        </div>
      </div>

      <div class="monitor-grid">
        <div class="monitor-stack">
          <div class="chat-window monitor-window">
            <EmptyState
              v-if="messages.length === 0"
              title="No live operator traffic"
              detail="Run a prompt against the current draft to inspect runtime behavior before persisting it."
            />

            <article v-for="message in messages" :key="message.id" class="chat-message console-message" :class="message.role">
              <div class="console-message__header">
                <div>
                  <strong>{{ message.role === 'user' ? 'Operator' : 'Agent' }}</strong>
                  <p>{{ formatClock(message.createdAt) }}</p>
                </div>
                <span class="inline-status" :class="message.role === 'user' ? 'muted' : 'ok'">
                  {{ message.role === 'user' ? 'Input' : 'Response' }}
                </span>
              </div>

              <p>{{ message.content }}</p>

              <div v-if="message.injectionTrace" class="message-trace">
                <div class="chip-row">
                  <span class="inline-status" :class="message.injectionTrace.personaId ? 'ok' : 'muted'">
                    Persona {{ message.injectionTrace.personaName ?? 'none' }}
                  </span>
                  <span class="inline-status" :class="message.injectionTrace.memoryCount > 0 ? 'ok' : 'muted'">
                    Memories {{ message.injectionTrace.memoryCount }}
                  </span>
                  <span class="inline-status muted">Workspace {{ message.injectionTrace.workspaceId }}</span>
                </div>

                <div v-if="message.injectionTrace.memories.length" class="trace-memory-list">
                  <button
                    v-for="memory in message.injectionTrace.memories"
                    :key="memory.id"
                    type="button"
                    class="result-row trace-memory"
                    @click="copyText(memory.id)"
                  >
                    <strong>{{ memory.type }} / {{ shortId(memory.id) }}</strong>
                    <span>{{ memory.matchReason }} / {{ memory.score.toFixed(2) }} / {{ memory.contentPreview }}</span>
                  </button>
                </div>
              </div>

              <div v-if="message.events?.length" class="event-list monitor-events">
                <span v-for="event in message.events" :key="`${event.timestamp}-${event.type}-${event.toolName}`">
                  {{ formatEventPill(event) }}
                </span>
              </div>
            </article>
          </div>

          <form class="chat-form monitor-form" @submit.prevent="sendMessage">
            <input v-model="input" type="text" placeholder="Simulate operator input against the draft..." />
            <button type="submit" class="primary" :disabled="sending || !parsedDraft || input.trim().length === 0">Run Draft</button>
          </form>
        </div>

        <aside class="card trace-panel">
          <div class="section-title compact">
            <div>
              <h3>Latest Injection Trace</h3>
              <p>Persona, memory, and event posture from the newest agent response.</p>
            </div>
            <span class="inline-status" :class="latestTrace ? 'ok' : 'muted'">
              {{ latestTrace?.workspaceId ?? 'No workspace' }}
            </span>
          </div>

          <EmptyState
            v-if="!latestTrace"
            title="No trace captured"
            detail="The side panel fills in after the first successful test run."
          />

          <template v-else>
            <div class="grid trace-stat-grid">
              <article class="trace-stat">
                <span>Persona</span>
                <strong>{{ latestTrace.personaName ?? 'none' }}</strong>
              </article>
              <article class="trace-stat">
                <span>Memories</span>
                <strong>{{ latestTrace.memoryCount }}</strong>
              </article>
              <article class="trace-stat">
                <span>Events</span>
                <strong>{{ latestEvents.length }}</strong>
              </article>
            </div>

            <div v-if="latestMetadataEntries.length" class="detail-list trace-metadata">
              <div v-for="[key, value] in latestMetadataEntries" :key="key" class="detail-item">
                <span>{{ formatKey(key) }}</span>
                <code>{{ value }}</code>
              </div>
            </div>

            <div v-if="latestTrace.memories.length" class="trace-memory-list trace-panel__memories">
              <button
                v-for="memory in latestTrace.memories"
                :key="memory.id"
                type="button"
                class="result-row trace-memory"
                @click="copyText(memory.id)"
              >
                <strong>{{ memory.type }} / {{ shortId(memory.id) }}</strong>
                <span>{{ memory.matchReason }} / {{ memory.score.toFixed(2) }} / {{ memory.contentPreview }}</span>
              </button>
            </div>
          </template>

          <div class="trace-panel__events">
            <div class="section-title compact">
              <div>
                <h3>Event Feed</h3>
                <p>Latest execution markers returned by the dashboard API.</p>
              </div>
            </div>

            <EmptyState
              v-if="latestEvents.length === 0"
              title="No events yet"
              detail="Tool and policy markers will appear here once the draft starts emitting them."
            />

            <div v-else class="event-list monitor-events">
              <span v-for="event in latestEvents" :key="`${event.timestamp}-${event.type}-${event.toolName}`">
                {{ formatEventPill(event) }}
              </span>
            </div>
          </div>
        </aside>
      </div>
    </section>

    <section class="panel fabric-panel">
      <div class="section-title">
        <div>
          <h2>Execution Fabric</h2>
          <p>Provider mesh and tool lanes available to the current agent contract.</p>
        </div>

        <div class="toolbar">
          <span class="inline-status ok">Tools {{ providerCapabilityCounts.toolCalling }}/{{ store.providers.length }}</span>
          <span class="inline-status muted">Vision {{ providerCapabilityCounts.vision }}/{{ store.providers.length }}</span>
          <span class="inline-status muted">Streaming {{ providerCapabilityCounts.streaming }}/{{ store.providers.length }}</span>
        </div>
      </div>

      <div class="grid fabric-grid">
        <article class="card fabric-card">
          <div class="section-title compact">
            <div>
              <h3>Provider Mesh</h3>
              <p>{{ store.providers.length }} registered lanes with capability snapshots.</p>
            </div>
          </div>

          <div class="provider-ribbon">
            <div v-for="provider in store.providers.slice(0, 4)" :key="provider.name" class="fabric-row">
              <div>
                <strong>{{ provider.displayName }}</strong>
                <p>{{ provider.kind }} / {{ provider.name }}</p>
              </div>
              <div class="fabric-row__caps">
                <span v-if="provider.supportToolCalling" class="chip">Tools</span>
                <span v-if="provider.supportVision" class="chip">Vision</span>
                <span v-if="provider.supportStreaming" class="chip">Streaming</span>
                <span v-if="providerCapabilityLabel(provider) === 'Text'" class="chip">Text</span>
              </div>
            </div>
          </div>

          <div class="table-wrap fabric-table">
            <table class="table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Kind</th>
                  <th>Capabilities</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="provider in store.providers" :key="provider.name">
                  <td>
                    <strong>{{ provider.displayName }}</strong>
                    <p class="muted">{{ provider.name }}</p>
                  </td>
                  <td>{{ provider.kind }}</td>
                  <td>
                    <div class="chip-row">
                      <span class="chip">Tools {{ yesNo(provider.supportToolCalling) }}</span>
                      <span class="chip">Vision {{ yesNo(provider.supportVision) }}</span>
                      <span class="chip">Streaming {{ yesNo(provider.supportStreaming) }}</span>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <article class="card fabric-card">
          <div class="section-title compact">
            <div>
              <h3>Tool Lanes</h3>
              <p>{{ effectiveEnabledTools }} effective tools staged across the current runtime inventory.</p>
            </div>
          </div>

          <div class="chip-row risk-ribbon">
            <span v-for="risk in toolRiskSummary" :key="risk.level" class="inline-status" :class="risk.tone">
              {{ formatRiskLevel(risk.level) }} {{ risk.count }}
            </span>
          </div>

          <div class="table-wrap fabric-table">
            <table class="table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Source</th>
                  <th>Risk</th>
                  <th>State</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="tool in highlightedTools" :key="tool.name">
                  <td>
                    <strong>{{ tool.name }}</strong>
                    <p class="muted">{{ tool.description }}</p>
                  </td>
                  <td>{{ tool.source }}</td>
                  <td>
                    <span class="inline-status" :class="riskTone(tool.riskLevel)">
                      {{ formatRiskLevel(tool.riskLevel) }}
                    </span>
                  </td>
                  <td>
                    <span class="inline-status" :class="tool.effectiveEnabled ? 'ok' : 'muted'">
                      {{ tool.effectiveEnabled ? 'Enabled' : 'Disabled' }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import {
  dashboardApi,
  type AgentChatEventDto,
  type AgentChatInjectionTraceDto,
  type AgentConfig,
  type ProviderDto,
  type ToolDto,
} from '../api/dashboard';
import EmptyState from '../components/EmptyState.vue';
import { useDashboardStore } from '../stores/dashboard';

interface ChatMessage {
  id: string;
  role: 'user' | 'agent';
  content: string;
  createdAt: number;
  events?: AgentChatEventDto[];
  injectionTrace?: AgentChatInjectionTraceDto;
}

interface DraftState {
  label: string;
  tone: 'ok' | 'warn' | 'muted' | 'danger';
  detail: string;
}

const store = useDashboardStore();
const draft = ref('');
const draftError = ref('');
const saveNotice = ref('');
const input = ref('');
const sending = ref(false);
const messages = ref<ChatMessage[]>([]);

const agent = computed(() => store.config?.agent ?? null);
const serializedAgent = computed(() => (agent.value ? JSON.stringify(agent.value, null, 2) : ''));
const draftParse = computed(() => parseAgentDraft(draft.value));
const parsedDraft = computed(() => draftParse.value.parsed);
const previewAgent = computed(() => parsedDraft.value ?? agent.value);
const latestAgentMessage = computed(() => [...messages.value].reverse().find((message) => message.role === 'agent') ?? null);
const latestTrace = computed(() => latestAgentMessage.value?.injectionTrace ?? null);
const latestEvents = computed(() => latestAgentMessage.value?.events ?? []);
const latestMetadataEntries = computed(() => Object.entries(latestTrace.value?.metadata ?? {}).slice(0, 6));
const providerCapabilityCounts = computed(() => ({
  toolCalling: store.providers.filter((provider) => provider.supportToolCalling).length,
  vision: store.providers.filter((provider) => provider.supportVision).length,
  streaming: store.providers.filter((provider) => provider.supportStreaming).length,
}));
const instructionLineCount = computed(() => {
  const instructions = previewAgent.value?.instructions?.trim() ?? '';
  return instructions.length === 0 ? 0 : instructions.split(/\r?\n/).length;
});
const instructionPreview = computed(() => {
  const instructions = (previewAgent.value?.instructions ?? '').replace(/\s+/g, ' ').trim();
  if (instructions.length === 0) return 'No instructions loaded yet. Update the draft before running validation.';
  return instructions.length > 200 ? `${instructions.slice(0, 200)}...` : instructions;
});
const allowedRiskCount = computed(() => normalizeArray(previewAgent.value?.allowedRiskLevels).length);
const toolPolicySummary = computed(() => {
  const enabled = normalizeArray(previewAgent.value?.enabledTools).length;
  const disabled = normalizeArray(previewAgent.value?.disabledTools).length;
  if (enabled === 0 && disabled === 0) return 'Open list posture';
  if (enabled > 0 && disabled === 0) return `${enabled} pinned tool lanes`;
  if (enabled === 0) return `${disabled} blocked tool lanes`;
  return `${enabled} pinned / ${disabled} blocked`;
});
const isDraftDirty = computed(() => draft.value !== serializedAgent.value);
const draftState = computed<DraftState>(() => {
  if (!agent.value) {
    return {
      label: 'Unloaded',
      tone: 'muted',
      detail: 'No saved agent configuration is currently loaded into the dashboard.',
    };
  }
  if (draftParse.value.error) {
    return {
      label: 'Draft Invalid',
      tone: 'danger',
      detail: draftParse.value.error,
    };
  }
  if (isDraftDirty.value) {
    return {
      label: 'Unsaved Draft',
      tone: 'warn',
      detail: 'Local JSON edits are staged in the console but not yet written back to the runtime config.',
    };
  }
  return {
    label: 'Synced',
    tone: 'ok',
    detail: 'Draft and saved runtime configuration are currently aligned.',
  };
});
const commandSummary = computed(() => {
  if (!previewAgent.value) {
    return 'Load an agent profile to inspect model routing, risk posture, and live execution signals in one place.';
  }
  return `Validate ${previewAgent.value.providerName || 'the active provider'} with live execution visibility before committing agent changes.`;
});
const commandMetrics = computed(() => [
  {
    label: 'Provider Lane',
    value: previewAgent.value?.providerName || 'Unassigned',
    detail: previewAgent.value?.name || 'Primary runtime agent',
  },
  {
    label: 'Model Frame',
    value: previewAgent.value?.model || 'Unset',
    detail: previewAgent.value?.compressStrategy || 'Compression strategy unavailable',
  },
  {
    label: 'Decision Envelope',
    value: previewAgent.value ? `${previewAgent.value.maxSteps} / ${previewAgent.value.maxRounds}` : '--',
    detail: 'steps / rounds',
  },
  {
    label: 'Tool Posture',
    value: normalizeArray(previewAgent.value?.enabledTools).length > 0 ? `${normalizeArray(previewAgent.value?.enabledTools).length} pinned` : 'Open list',
    detail: `${normalizeArray(previewAgent.value?.disabledTools).length} blocked / ${allowedRiskCount.value} risk lanes`,
  },
  {
    label: 'Runtime Mesh',
    value: `${store.providers.length} / ${store.tools.length}`,
    detail: 'providers / tools',
  },
  {
    label: 'Workspace Cover',
    value: `${store.workspaces.workspaces.filter((workspace) => workspace.enabled).length}/${store.workspaces.workspaces.length}`,
    detail: 'enabled workspaces',
  },
]);
const operationalDetails = computed(() => [
  {
    label: 'Instruction Lines',
    value: String(instructionLineCount.value),
  },
  {
    label: 'Token Budget',
    value: previewAgent.value?.maxTokens?.toString() ?? '--',
  },
  {
    label: 'Tool Timeout',
    value: previewAgent.value ? `${previewAgent.value.toolTimeoutSeconds}s` : '--',
  },
  {
    label: 'Tracked Conversations',
    value: String(store.conversations.length),
  },
  {
    label: 'Live Exchanges',
    value: String(messages.value.length),
  },
  {
    label: 'Last Sync',
    value: store.lastUpdated ? formatDateTime(store.lastUpdated) : 'Not refreshed',
  },
]);
const monitorSummary = computed(() => {
  if (messages.value.length === 0) return 'Run the edited draft without saving to inspect reply content, trace injection, and emitted runtime events.';
  const operatorTurns = messages.value.filter((message) => message.role === 'user').length;
  const agentTurns = messages.value.filter((message) => message.role === 'agent').length;
  return `${operatorTurns} operator prompts and ${agentTurns} agent replies staged in the live monitor.`;
});
const toolRiskSummary = computed(() =>
  (['HIGH_RISK', 'STATE_WRITE', 'EXTERNAL_READ', 'SESSION_ACTION', 'SAFE_READ'] as ToolDto['riskLevel'][]).map((level) => ({
    level,
    count: store.tools.filter((tool) => tool.riskLevel === level).length,
    tone: riskTone(level),
  })),
);
const highlightedTools = computed(() =>
  [...store.tools]
    .sort((left, right) => {
      const severity = riskSeverity(right.riskLevel) - riskSeverity(left.riskLevel);
      if (severity !== 0) return severity;
      if (left.effectiveEnabled !== right.effectiveEnabled) return Number(right.effectiveEnabled) - Number(left.effectiveEnabled);
      return left.name.localeCompare(right.name);
    })
    .slice(0, 8),
);
const effectiveEnabledTools = computed(() => store.tools.filter((tool) => tool.effectiveEnabled).length);
const activeDraftError = computed(() => {
  if (draftError.value) return draftError.value;
  if (draft.value.trim().length === 0) return '';
  return draftParse.value.error;
});
const yesNo = (value: boolean) => (value ? 'yes' : 'no');

function resetDraft() {
  draft.value = serializedAgent.value;
  draftError.value = '';
  saveNotice.value = '';
}

function parseAgentDraft(value: string): { parsed: AgentConfig | null; error: string } {
  const trimmed = value.trim();
  if (trimmed.length === 0) return { parsed: null, error: 'Agent config draft is empty.' };
  try {
    const parsed = JSON.parse(value) as AgentConfig;
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      throw new Error('Agent config draft must be a JSON object.');
    }
    return { parsed, error: '' };
  } catch (cause) {
    return {
      parsed: null,
      error: cause instanceof Error ? cause.message : String(cause),
    };
  }
}

async function saveConfig() {
  if (!store.config) {
    draftError.value = 'Runtime config is not loaded.';
    return;
  }
  if (!parsedDraft.value) {
    draftError.value = draftParse.value.error;
    return;
  }
  try {
    await store.saveConfig({ ...store.config, agent: parsedDraft.value });
    saveNotice.value = 'Agent config saved and runtime surfaces refreshed.';
    draftError.value = '';
  } catch (cause) {
    draftError.value = cause instanceof Error ? cause.message : String(cause);
  }
}

async function sendMessage() {
  if (!parsedDraft.value || input.value.trim().length === 0) {
    draftError.value = draftParse.value.error;
    return;
  }
  const content = input.value.trim();
  input.value = '';
  draftError.value = '';
  saveNotice.value = '';
  sending.value = true;
  messages.value.push({ id: crypto.randomUUID(), role: 'user', content, createdAt: Date.now() });
  try {
    const response = await dashboardApi.chatAgent({ message: content, config: parsedDraft.value });
    messages.value.push({
      id: crypto.randomUUID(),
      role: 'agent',
      content: response.content,
      createdAt: Date.now(),
      events: response.events,
      injectionTrace: response.injectionTrace,
    });
  } catch (cause) {
    messages.value.push({
      id: crypto.randomUUID(),
      role: 'agent',
      content: cause instanceof Error ? cause.message : String(cause),
      createdAt: Date.now(),
    });
  } finally {
    sending.value = false;
  }
}

async function copyText(value: string) {
  await navigator.clipboard?.writeText(value);
}

function normalizeArray<T>(value: T[] | undefined | null): T[] {
  return Array.isArray(value) ? value : [];
}

function shortId(value: string) {
  return value.length > 10 ? value.slice(-10) : value;
}

function riskSeverity(level: ToolDto['riskLevel']) {
  switch (level) {
    case 'HIGH_RISK':
      return 5;
    case 'STATE_WRITE':
      return 4;
    case 'EXTERNAL_READ':
      return 3;
    case 'SESSION_ACTION':
      return 2;
    case 'SAFE_READ':
    default:
      return 1;
  }
}

function riskTone(level: ToolDto['riskLevel']): DraftState['tone'] {
  switch (level) {
    case 'HIGH_RISK':
      return 'danger';
    case 'STATE_WRITE':
      return 'warn';
    case 'EXTERNAL_READ':
      return 'muted';
    case 'SESSION_ACTION':
      return 'ok';
    case 'SAFE_READ':
    default:
      return 'muted';
  }
}

function formatRiskLevel(level: ToolDto['riskLevel']) {
  return level.replace(/_/g, ' ');
}

function formatEventPill(event: AgentChatEventDto) {
  const parts = [event.type];
  if (event.toolName) parts.push(event.toolName);
  if (event.success === false) parts.push('failed');
  if (event.errorCode) parts.push(event.errorCode);
  if (event.policyDenialCode) parts.push(event.policyDenialCode);
  return parts.join(' / ');
}

function formatClock(value: number) {
  return new Intl.DateTimeFormat(undefined, {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(value);
}

function formatDateTime(value: number) {
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(value);
}

function formatDecimal(value: number | undefined) {
  return typeof value === 'number' ? value.toFixed(2) : '--';
}

function formatKey(value: string) {
  return value.replace(/([A-Z])/g, ' $1').replace(/^./, (first) => first.toUpperCase());
}

function providerCapabilityLabel(provider: ProviderDto) {
  const capabilities = [];
  if (provider.supportToolCalling) capabilities.push('Tools');
  if (provider.supportVision) capabilities.push('Vision');
  if (provider.supportStreaming) capabilities.push('Streaming');
  return capabilities.length === 0 ? 'Text' : capabilities.join(' / ');
}

watch(agent, resetDraft, { immediate: true });
watch(draft, () => {
  draftError.value = '';
  saveNotice.value = '';
});
</script>

<style scoped>
.agent-console {
  align-items: start;
}

.command-deck {
  padding: 0;
  overflow: hidden;
  border-color: #ddd1bb;
  background:
    linear-gradient(135deg, rgba(255, 252, 245, 0.98) 0%, rgba(247, 242, 232, 0.98) 58%, rgba(239, 244, 248, 0.98) 100%);
}

.command-deck__chrome {
  position: relative;
  padding: 20px;
  background-image:
    linear-gradient(rgba(63, 76, 92, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(63, 76, 92, 0.07) 1px, transparent 1px);
  background-size: 24px 24px;
}

.command-deck__chrome::before {
  content: 'DAY SHIFT';
  position: absolute;
  top: 0;
  right: 0;
  padding: 8px 16px;
  background: #273241;
  color: #f9f4ea;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.18em;
}

.command-deck__header,
.console-message__header,
.fabric-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.console-kicker,
.command-metric__label,
.config-insight__label,
.trace-stat span {
  display: inline-block;
  color: #8f6a32;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.command-deck__title h2 {
  margin: 6px 0 0;
  font-size: 30px;
  line-height: 1.02;
  letter-spacing: -0.03em;
}

.command-deck__title p:last-child {
  margin: 8px 0 0;
  max-width: 68ch;
  color: #516070;
  font-size: 14px;
  line-height: 1.5;
}

.command-deck__badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.command-badge {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid #d8ccb9;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: #66533a;
  font-size: 12px;
  font-weight: 700;
}

.command-deck__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(290px, 0.65fr);
  gap: 12px;
  align-items: start;
}

.command-metric-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.command-metric,
.config-insight,
.trace-stat {
  position: relative;
  overflow: hidden;
  border-color: #dfd5c9;
  background: rgba(255, 255, 255, 0.74);
}

.command-metric::before,
.config-insight::before,
.trace-stat::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 44px;
  height: 3px;
  background: #b7792d;
}

.command-metric strong,
.config-insight strong,
.trace-stat strong {
  display: block;
  margin-top: 14px;
  font-size: 21px;
  line-height: 1.15;
  color: #24303e;
  overflow-wrap: anywhere;
}

.command-metric p,
.config-insight p,
.trace-stat p {
  margin-top: 8px;
  font-size: 12px;
  color: #667180;
}

.command-brief {
  border-color: #d5cab9;
  background: linear-gradient(180deg, rgba(251, 247, 237, 0.96) 0%, rgba(244, 239, 229, 0.96) 100%);
}

.command-brief__summary {
  margin: 0 0 14px;
  color: #314050;
  font-size: 13px;
  line-height: 1.6;
}

.command-detail-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.config-panel {
  background: linear-gradient(180deg, #ffffff 0%, #fbfaf6 100%);
}

.config-insights {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-bottom: 12px;
}

.agent-config-editor {
  min-height: 420px;
  border-color: #d8d1c4;
  background: linear-gradient(180deg, #f8f7f3 0%, #f2f1ec 100%);
  color: #263240;
}

.monitor-panel {
  background: linear-gradient(180deg, #fcfcfb 0%, #f5f7f8 100%);
}

.monitor-status {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.monitor-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(300px, 0.8fr);
  gap: 12px;
  align-items: start;
}

.monitor-stack {
  display: grid;
  gap: 10px;
}

.monitor-window {
  min-height: 420px;
  max-height: 680px;
  border-color: #d9dde2;
  background: linear-gradient(180deg, #f7f6f2 0%, #f1f3f5 100%);
}

.console-message {
  border-color: #d8dcd9;
  background: rgba(255, 255, 255, 0.88);
}

.console-message.user {
  border-color: #d1dae6;
  background: #f3f6fb;
}

.console-message.agent {
  border-color: #d8cebf;
  background: #fcfbf8;
}

.console-message__header p {
  margin: 4px 0 0;
  color: #707b88;
  font-size: 12px;
}

.console-message > p {
  line-height: 1.6;
}

.message-trace {
  display: grid;
  gap: 8px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #e3e7eb;
}

.trace-memory {
  border-color: #ddd5c9;
  background: #fffdfa;
}

.trace-panel {
  position: sticky;
  top: 82px;
  border-color: #dbd9d2;
  background: linear-gradient(180deg, #fffdf8 0%, #f5f6f7 100%);
}

.trace-stat-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-bottom: 12px;
}

.trace-stat {
  padding: 10px;
  border-radius: 8px;
}

.trace-metadata {
  margin-bottom: 12px;
}

.trace-panel__memories {
  margin-bottom: 14px;
}

.trace-panel__events {
  display: grid;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #e1e5e8;
}

.monitor-events span {
  background: rgba(255, 255, 255, 0.78);
}

.monitor-form {
  margin-top: 0;
}

.agent-console .command-deck,
.agent-console .config-panel,
.agent-console .monitor-panel,
.agent-console .trace-panel,
.agent-console .fabric-panel,
.agent-console .fabric-card,
.agent-console .fabric-row,
.agent-console .command-brief,
.agent-console .command-metric {
  border-color: var(--line);
  background: rgba(255, 255, 255, 0.92);
}

.agent-console .command-deck__chrome::before {
  display: none;
}

.agent-console .command-deck__title h2,
.agent-console .command-brief h3 {
  color: var(--text-strong);
  letter-spacing: -0.03em;
  text-transform: none;
}

.agent-console .console-kicker,
.agent-console .command-badge,
.agent-console .command-metric__label {
  color: var(--weak);
}

.agent-console .command-brief__summary,
.agent-console .fabric-row p,
.agent-console .console-message__header p {
  color: var(--muted);
}

.agent-console .monitor-window,
.agent-console .trace-panel {
  background: rgba(250, 250, 250, 0.92);
}

.agent-console .console-message,
.agent-console .console-message.user,
.agent-console .console-message.agent,
.agent-console .trace-memory {
  border-color: var(--line);
  background: rgba(255, 255, 255, 0.94);
}

.fabric-panel {
  background: linear-gradient(180deg, #ffffff 0%, #f8f8f5 100%);
}

.fabric-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.fabric-card {
  display: grid;
  gap: 12px;
  border-color: #dfe2e5;
  background: linear-gradient(180deg, #ffffff 0%, #f7f8f6 100%);
}

.provider-ribbon {
  display: grid;
  gap: 8px;
}

.fabric-row {
  padding: 10px 12px;
  border: 1px solid #e3e6e9;
  border-radius: 8px;
  background: #fcfcfb;
}

.fabric-row p {
  margin: 4px 0 0;
  color: #687281;
  font-size: 12px;
}

.fabric-row__caps {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.risk-ribbon {
  margin-bottom: 2px;
}

.fabric-table .table {
  min-width: 0;
}

@media (max-width: 1180px) {
  .command-deck__grid,
  .monitor-grid,
  .fabric-grid {
    grid-template-columns: 1fr;
  }

  .trace-panel {
    position: static;
  }
}

@media (max-width: 900px) {
  .command-metric-grid,
  .config-insights,
  .command-detail-list,
  .trace-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .command-deck__chrome {
    padding-top: 50px;
  }
}

@media (max-width: 640px) {
  .command-deck__header,
  .command-deck__badges,
  .monitor-status,
  .fabric-row {
    flex-direction: column;
    align-items: stretch;
  }

  .command-metric-grid,
  .config-insights,
  .command-detail-list,
  .trace-stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
