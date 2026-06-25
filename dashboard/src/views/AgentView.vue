<template>
  <div class="grid agent-layout">
    <section class="panel">
      <div class="section-title">
        <div>
          <h2>Agent Config</h2>
          <p>{{ store.config?.agent.name ?? 'No agent loaded' }}</p>
        </div>
        <div class="toolbar">
          <button type="button" @click="resetDraft()">Reset</button>
          <button type="button" class="primary" @click="saveConfig()">Save</button>
        </div>
      </div>

      <div v-if="agent" class="grid metric-grid compact-metrics">
        <article class="card metric">
          <strong>{{ agent.providerName }}</strong>
          <span>Provider</span>
        </article>
        <article class="card metric">
          <strong>{{ agent.model }}</strong>
          <span>Model</span>
        </article>
        <article class="card metric">
          <strong>{{ agent.maxSteps }}</strong>
          <span>Max steps</span>
        </article>
        <article class="card metric">
          <strong>{{ store.tools.length }}</strong>
          <span>Tools</span>
        </article>
      </div>

      <textarea v-model="draft" class="agent-config-editor" spellcheck="false"></textarea>
      <p v-if="draftError" class="notice error">{{ draftError }}</p>
    </section>

    <section class="panel">
      <div class="section-title">
        <div>
          <h2>Test Chat</h2>
          <p>Runs the edited config without saving first.</p>
        </div>
      </div>

      <div class="chat-window">
        <EmptyState v-if="messages.length === 0" title="No test messages" detail="Send a prompt to run the active Agent through the Dashboard API." />
        <article v-for="message in messages" :key="message.id" class="chat-message" :class="message.role">
          <strong>{{ message.role === 'user' ? 'You' : 'Agent' }}</strong>
          <p>{{ message.content }}</p>
          <div v-if="message.injectionTrace" class="injection-trace">
            <span class="inline-status" :class="{ ok: Boolean(message.injectionTrace.personaId), muted: !message.injectionTrace.personaId }">
              Persona {{ message.injectionTrace.personaName ?? 'none' }}
            </span>
            <span class="inline-status" :class="{ ok: message.injectionTrace.memoryCount > 0, muted: message.injectionTrace.memoryCount === 0 }">
              Memories {{ message.injectionTrace.memoryCount }}
            </span>
            <span class="inline-status muted">Workspace {{ message.injectionTrace.workspaceId }}</span>
            <div v-if="message.injectionTrace.memories.length" class="trace-memory-list">
              <button
                v-for="memory in message.injectionTrace.memories"
                :key="memory.id"
                type="button"
                class="result-row"
                @click="copyText(memory.id)"
              >
                <strong>{{ memory.type }} · {{ memory.id }}</strong>
                <span>{{ memory.matchReason }} · {{ memory.score.toFixed(2) }} · {{ memory.contentPreview }}</span>
              </button>
            </div>
          </div>
          <div v-if="message.events?.length" class="event-list">
            <span v-for="event in message.events" :key="`${event.timestamp}-${event.type}-${event.toolName}`">
              {{ event.type }}<template v-if="event.toolName"> · {{ event.toolName }}</template>
              <template v-if="event.success === false"> · failed</template>
              <template v-if="event.errorCode"> · {{ event.errorCode }}</template>
              <template v-if="event.policyDenialCode"> · {{ event.policyDenialCode }}</template>
            </span>
          </div>
        </article>
      </div>

      <form class="chat-form" @submit.prevent="sendMessage">
        <input v-model="input" type="text" placeholder="Ask the Agent..." />
        <button type="submit" class="primary" :disabled="sending || input.trim().length === 0">Send</button>
      </form>
    </section>

    <section class="panel">
      <div class="section-title">
        <div>
          <h2>Providers</h2>
          <p>{{ store.providers.length }} registered.</p>
        </div>
      </div>
      <div class="table-wrap">
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
    </section>

    <section class="panel">
      <div class="section-title">
        <div>
          <h2>Tools</h2>
          <p>{{ store.tools.length }} available.</p>
        </div>
      </div>
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Source</th>
              <th>Risk</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="tool in store.tools.slice(0, 8)" :key="tool.name">
              <td>
                <strong>{{ tool.name }}</strong>
                <p class="muted">{{ tool.description }}</p>
              </td>
              <td>{{ tool.source }}</td>
              <td>
                <span class="inline-status" :class="{ danger: tool.riskLevel === 'HIGH_RISK', warn: tool.riskLevel === 'STATE_WRITE' }">
                  {{ tool.riskLevel }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { dashboardApi, type AgentChatEventDto, type AgentChatInjectionTraceDto, type AgentConfig } from '../api/dashboard';
import EmptyState from '../components/EmptyState.vue';
import { useDashboardStore } from '../stores/dashboard';

interface ChatMessage {
  id: string;
  role: 'user' | 'agent';
  content: string;
  events?: AgentChatEventDto[];
  injectionTrace?: AgentChatInjectionTraceDto;
}

const store = useDashboardStore();
const draft = ref('');
const draftError = ref('');
const input = ref('');
const sending = ref(false);
const messages = ref<ChatMessage[]>([]);

const agent = computed(() => store.config?.agent ?? null);
const yesNo = (value: boolean) => (value ? 'yes' : 'no');

function resetDraft() {
  draft.value = JSON.stringify(agent.value, null, 2);
  draftError.value = '';
}

function parseDraft(): AgentConfig | null {
  try {
    draftError.value = '';
    return JSON.parse(draft.value) as AgentConfig;
  } catch (cause) {
    draftError.value = cause instanceof Error ? cause.message : String(cause);
    return null;
  }
}

async function saveConfig() {
  if (!store.config) return;
  const parsed = parseDraft();
  if (!parsed) return;
  await store.saveConfig({ ...store.config, agent: parsed });
}

async function sendMessage() {
  const parsed = parseDraft();
  if (!parsed || input.value.trim().length === 0) return;
  const content = input.value.trim();
  input.value = '';
  sending.value = true;
  messages.value.push({ id: crypto.randomUUID(), role: 'user', content });
  try {
    const response = await dashboardApi.chatAgent({ message: content, config: parsed });
    messages.value.push({
      id: crypto.randomUUID(),
      role: 'agent',
      content: response.content,
      events: response.events,
      injectionTrace: response.injectionTrace,
    });
  } catch (cause) {
    messages.value.push({
      id: crypto.randomUUID(),
      role: 'agent',
      content: cause instanceof Error ? cause.message : String(cause),
    });
  } finally {
    sending.value = false;
  }
}

async function copyText(value: string) {
  await navigator.clipboard?.writeText(value);
}

watch(agent, resetDraft, { immediate: true });
</script>
