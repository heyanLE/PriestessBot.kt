<template>
  <div class="conversation-command">
    <section class="panel conversation-hero">
      <div class="conversation-hero-grid">
        <div class="conversation-copy">
          <div class="conversation-band">
            <span>Troubleshooting</span>
            <span>{{ conversation?.platform ?? 'Conversation Detail' }}</span>
          </div>

          <h2>{{ conversation?.sessionId ?? conversationId }}</h2>
          <p>{{ conversationSummary }}</p>

          <div class="grid conversation-signal-grid">
            <article v-for="signal in conversationSignals" :key="signal.label" class="card conversation-signal" :class="`tone-${signal.tone}`">
              <span>{{ signal.label }}</span>
              <strong>{{ signal.value }}</strong>
              <p>{{ signal.detail }}</p>
            </article>
          </div>
        </div>

        <aside class="conversation-rail">
          <article class="card conversation-rail-card">
            <div class="section-title compact">
              <div>
                <h3>What matters here</h3>
                <p>Read the thread as a transcript with evidence, not a raw message dump.</p>
              </div>
            </div>

            <div class="rail-list">
              <div class="rail-item">
                <span>Platform</span>
                <strong>{{ conversation?.platform ?? 'Unknown' }}</strong>
              </div>
              <div class="rail-item">
                <span>Updated</span>
                <strong>{{ conversation ? formatTime(conversation.updatedAt) : 'Awaiting metadata' }}</strong>
              </div>
              <div class="rail-item">
                <span>Transcript state</span>
                <strong>{{ loading ? 'Refreshing' : messages.length > 0 ? 'Loaded' : 'Empty' }}</strong>
              </div>
            </div>
          </article>
        </aside>
      </div>
    </section>

    <div class="workbench-grid wide-detail">
      <section class="panel transcript-panel">
        <div class="section-title">
          <div>
            <h2>Session transcript</h2>
            <p>{{ transcriptSummary }}</p>
          </div>
          <div class="toolbar">
            <RouterLink class="button-link" to="/conversations">Back</RouterLink>
            <button type="button" class="primary" @click="loadMessages()">Refresh</button>
          </div>
        </div>

        <p v-if="loading" class="notice">Refreshing transcript from the dashboard API.</p>

        <EmptyState
          v-if="messages.length === 0 && !loading"
          title="No messages"
          detail="This conversation has no stored messages yet."
        />

        <section v-else class="transcript-ledger">
          <article v-for="message in messages" :key="message.id" class="message-card" :class="roleTone(message.role)">
            <div class="message-card-head">
              <div>
                <strong>{{ formatRole(message.role) }}</strong>
                <p>{{ formatTime(message.createdAt) }}</p>
              </div>
              <span class="inline-status" :class="roleTone(message.role)">{{ formatRole(message.role) }}</span>
            </div>

            <p v-if="message.content">{{ message.content }}</p>
            <p v-else class="muted">No text content</p>

            <div v-if="message.toolCallId" class="tool-call-pill">
              <span>Tool call id</span>
              <code>{{ message.toolCallId }}</code>
            </div>

            <pre v-if="message.toolCalls" class="code-block transcript-code">{{ prettyJson(message.toolCalls) }}</pre>
          </article>
        </section>
      </section>

      <aside class="panel detail-panel detail-rail">
        <div class="section-title">
          <div>
            <h2>Session detail</h2>
            <p>Operator, assistant, and tool posture across the current transcript.</p>
          </div>
        </div>

        <div class="detail-stack">
          <article class="card detail-card">
            <div class="grid detail-stat-grid">
              <article class="detail-stat">
                <span>Messages</span>
                <strong>{{ messages.length }}</strong>
              </article>
              <article class="detail-stat">
                <span>Operator</span>
                <strong>{{ roleCounts.user }}</strong>
              </article>
              <article class="detail-stat">
                <span>Assistant</span>
                <strong>{{ roleCounts.assistant }}</strong>
              </article>
            </div>

            <div class="detail-list">
              <div class="detail-item">
                <span>Tool rows</span>
                <strong>{{ roleCounts.tool }}</strong>
              </div>
              <div class="detail-item">
                <span>First message</span>
                <strong>{{ firstMessage ? formatTime(firstMessage.createdAt) : 'None' }}</strong>
              </div>
              <div class="detail-item">
                <span>Latest message</span>
                <strong>{{ latestMessage ? formatTime(latestMessage.createdAt) : 'None' }}</strong>
              </div>
            </div>
          </article>

          <article class="card detail-card">
            <div class="section-title compact">
              <div>
                <h3>Conversation identity</h3>
                <p>Stable thread metadata for cross-checking runtime sessions.</p>
              </div>
            </div>

            <div class="detail-list">
              <div class="detail-item">
                <span>Conversation id</span>
                <strong>{{ conversationId }}</strong>
              </div>
              <div class="detail-item">
                <span>Platform</span>
                <strong>{{ conversation?.platform ?? 'Unknown' }}</strong>
              </div>
              <div class="detail-item">
                <span>Session id</span>
                <strong>{{ conversation?.sessionId ?? 'Unknown' }}</strong>
              </div>
              <div class="detail-item">
                <span>Updated</span>
                <strong>{{ conversation ? formatTime(conversation.updatedAt) : 'Unknown' }}</strong>
              </div>
            </div>
          </article>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { dashboardApi, type MessageDto } from '../api/dashboard';
import EmptyState from '../components/EmptyState.vue';
import { useDashboardStore } from '../stores/dashboard';

const route = useRoute();
const store = useDashboardStore();
const messages = ref<MessageDto[]>([]);
const loading = ref(false);

const conversationId = computed(() => String(route.params.id ?? ''));
const conversation = computed(() => store.conversations.find((item) => item.id === conversationId.value));
const firstMessage = computed(() => messages.value.at(-1) ?? null);
const latestMessage = computed(() => messages.value[0] ?? null);

const roleCounts = computed(() => ({
  user: messages.value.filter((message) => normalizeRole(message.role) === 'user').length,
  assistant: messages.value.filter((message) => normalizeRole(message.role) === 'assistant').length,
  tool: messages.value.filter((message) => normalizeRole(message.role) === 'tool').length,
}));

const conversationSummary = computed(() => {
  if (!conversation.value) return `Inspect stored messages for session ${conversationId.value}.`;
  return `${conversation.value.platform} session ${conversation.value.sessionId} updated ${formatTime(conversation.value.updatedAt)} with ${messages.value.length} stored message(s).`;
});

const transcriptSummary = computed(() => {
  if (messages.value.length === 0) return 'The session ledger is empty until stored messages arrive.';
  return `${roleCounts.value.user} operator, ${roleCounts.value.assistant} assistant, and ${roleCounts.value.tool} tool row(s) in the current transcript.`;
});

const conversationSignals = computed(() => [
  {
    label: 'Messages',
    value: String(messages.value.length),
    detail: 'Stored transcript rows currently loaded for this session.',
    tone: messages.value.length > 0 ? 'ok' : 'muted',
  },
  {
    label: 'Operator turns',
    value: String(roleCounts.value.user),
    detail: 'User-originated messages in the visible transcript.',
    tone: roleCounts.value.user > 0 ? 'ok' : 'muted',
  },
  {
    label: 'Tool traces',
    value: String(roleCounts.value.tool),
    detail: roleCounts.value.tool > 0 ? 'Tool activity is embedded in the session record.' : 'No tool calls surfaced in stored rows.',
    tone: roleCounts.value.tool > 0 ? 'warn' : 'muted',
  },
  {
    label: 'Latest update',
    value: conversation.value ? formatTime(conversation.value.updatedAt) : 'Unknown',
    detail: conversation.value ? 'Conversation metadata from the current runtime feed.' : 'Conversation metadata is still loading from the dashboard store.',
    tone: conversation.value ? 'ok' : 'muted',
  },
]);

async function loadMessages() {
  if (!conversationId.value) return;
  loading.value = true;
  try {
    if (!store.lastUpdated) await store.refreshAll();
    messages.value = await dashboardApi.messages(conversationId.value, 200);
  } finally {
    loading.value = false;
  }
}

function prettyJson(value: string) {
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

function normalizeRole(role: string) {
  return String(role).trim().toLowerCase();
}

function formatRole(role: string) {
  const normalized = normalizeRole(role);
  if (normalized === 'assistant') return 'Assistant';
  if (normalized === 'user') return 'Operator';
  if (normalized === 'tool') return 'Tool';
  return role;
}

function roleTone(role: string) {
  const normalized = normalizeRole(role);
  if (normalized === 'assistant') return 'ok';
  if (normalized === 'tool') return 'warn';
  return 'muted';
}

function formatTime(value: number) {
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(value);
}

watch(conversationId, () => void loadMessages());
onMounted(() => void loadMessages());
</script>

<style scoped>
.conversation-command {
  display: grid;
  gap: 14px;
}

.conversation-hero {
  border-color: #d7cebd;
  background:
    linear-gradient(135deg, rgba(255, 252, 246, 0.98) 0%, rgba(247, 241, 231, 0.98) 55%, rgba(240, 246, 248, 0.98) 100%);
}

.conversation-hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.18fr) minmax(280px, 0.82fr);
  gap: 14px;
}

.conversation-band {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.conversation-band span,
.conversation-signal span,
.rail-item span,
.detail-stat span {
  color: #887152;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.conversation-band span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid rgba(182, 159, 111, 0.34);
  background: rgba(255, 251, 245, 0.92);
}

.conversation-copy h2 {
  margin: 0;
  color: #18304c;
  font-size: clamp(26px, 1.7vw + 18px, 36px);
  line-height: 1.02;
  overflow-wrap: anywhere;
}

.conversation-copy > p {
  margin: 12px 0 0;
  color: #5c6776;
  line-height: 1.66;
}

.conversation-signal-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 16px;
}

.conversation-signal {
  display: grid;
  gap: 8px;
  border-top: 3px solid #98a2b0;
  background: rgba(255, 252, 247, 0.9);
}

.conversation-signal.tone-ok {
  border-top-color: #4c8661;
}

.conversation-signal.tone-warn {
  border-top-color: #bb8524;
}

.conversation-signal.tone-muted {
  border-top-color: #98a2b0;
}

.conversation-signal strong {
  color: #17304d;
  font-size: 28px;
  line-height: 1;
}

.conversation-signal p {
  margin: 0;
  color: #606a79;
  font-size: 12px;
  line-height: 1.58;
}

.conversation-rail,
.detail-stack {
  display: grid;
  gap: 12px;
}

.conversation-rail-card,
.detail-card {
  border-color: #ddd4c5;
  background: rgba(255, 252, 246, 0.92);
}

.rail-list {
  display: grid;
  gap: 10px;
}

.rail-item {
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid #e3dacb;
  background: rgba(255, 251, 245, 0.92);
}

.rail-item strong {
  color: #19314d;
  font-size: 14px;
  line-height: 1.46;
}

.transcript-panel,
.detail-rail {
  border-color: #ddd3c3;
  background: linear-gradient(180deg, rgba(255, 252, 247, 0.96) 0%, rgba(248, 243, 234, 0.94) 100%);
}

.transcript-ledger {
  display: grid;
  gap: 10px;
}

.message-card {
  display: grid;
  gap: 10px;
  padding: 14px;
  border: 1px solid #e2d8ca;
  border-left-width: 4px;
  background: rgba(255, 252, 247, 0.92);
}

.message-card.ok {
  border-left-color: #4c8661;
}

.message-card.warn {
  border-left-color: #bb8524;
}

.message-card.muted {
  border-left-color: #98a2b0;
}

.message-card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.message-card-head p {
  margin: 4px 0 0;
  color: #6d7785;
  font-size: 12px;
}

.message-card > p {
  margin: 0;
  color: #21313f;
  line-height: 1.7;
  white-space: pre-wrap;
}

.tool-call-pill {
  display: inline-grid;
  gap: 4px;
  width: fit-content;
  padding: 10px 12px;
  border: 1px solid #e1d7c8;
  background: rgba(255, 249, 240, 0.94);
}

.tool-call-pill span {
  color: #887152;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.transcript-code {
  margin: 0;
}

.detail-stat-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-bottom: 12px;
}

.detail-stat {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid #e3d9c8;
  background: rgba(255, 252, 247, 0.92);
}

.detail-stat strong {
  color: #18314d;
  font-size: 22px;
  line-height: 1;
}

@media (max-width: 1180px) {
  .conversation-hero-grid,
  .workbench-grid.wide-detail {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .conversation-signal-grid,
  .detail-stat-grid {
    grid-template-columns: 1fr;
  }

  .message-card-head {
    flex-direction: column;
  }
}
</style>
