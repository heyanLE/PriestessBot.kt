<template>
  <div class="tactical-list-view">
    <section class="panel tactical-list-hero">
      <div class="tactical-list-hero-grid">
        <div class="tactical-list-copy">
          <div class="tactical-list-band">
            <span>Priestess / Watch Log</span>
            <span>Session Traffic Desk</span>
          </div>

          <h2>Conversation watchboard</h2>
          <p>
            Monitor live sessions, platform spread, and the freshest operator traffic from a
            daytime mission feed instead of a passive archive table.
          </p>

          <div class="tactical-stat-grid">
            <article class="card tactical-stat-card">
              <span class="tactical-stat-label">Tracked</span>
              <strong>{{ store.conversations.length }}</strong>
              <p>stored sessions currently visible in the feed.</p>
            </article>
            <article class="card tactical-stat-card">
              <span class="tactical-stat-label">Platforms</span>
              <strong>{{ platformCount }}</strong>
              <p>runtime entry points with active conversation traces.</p>
            </article>
            <article class="card tactical-stat-card">
              <span class="tactical-stat-label">Freshest Update</span>
              <strong>{{ freshestLabel }}</strong>
              <p>latest watchpoint observed by the dashboard.</p>
            </article>
            <article class="card tactical-stat-card">
              <span class="tactical-stat-label">Aging Sessions</span>
              <strong>{{ staleCount }}</strong>
              <p>sessions untouched for more than twelve hours.</p>
            </article>
          </div>
        </div>

        <aside class="tactical-ledger">
          <h3>Operational ledger</h3>
          <p>Session triage works best when recency, platform, and handoff risk are visible at a glance.</p>

          <div class="tactical-ledger-row">
            <span>Current posture</span>
            <strong>{{ store.conversations.length > 0 ? 'Live watch coverage available' : 'No tracked sessions yet' }}</strong>
          </div>
          <div class="tactical-ledger-row">
            <span>Platform spread</span>
            <strong>{{ platformSpread }}</strong>
          </div>
          <div class="tactical-ledger-row">
            <span>Operator note</span>
            <strong>Use the mission feed for recency, then drop into transcript detail only when escalation is needed.</strong>
          </div>
        </aside>
      </div>
    </section>

    <section class="panel tactical-table-panel">
      <div class="section-title">
        <div>
          <h2>Mission feed</h2>
          <p>{{ store.conversations.length }} stored sessions.</p>
        </div>
      </div>

      <EmptyState
        v-if="sortedConversations.length === 0"
        class="tactical-empty"
        title="No conversations stored"
        detail="Pipeline traffic will create conversation records once relay lanes start receiving operator messages."
      />

      <div v-else class="tactical-feed">
        <article
          v-for="conversation in sortedConversations"
          :key="conversation.id"
          class="tactical-feed-row is-clickable"
          @click="openConversation(conversation.id)"
        >
          <div class="tactical-feed-head">
            <div>
              <strong>{{ conversation.platform }}</strong>
              <p><span class="tactical-inline-code">{{ conversation.sessionId }}</span></p>
            </div>
            <StatusDot :label="recencyLabel(conversation.updatedAt)" :tone="recencyTone(conversation.updatedAt)" />
          </div>

          <div class="tactical-feed-meta">
            <span>Created {{ formatTime(conversation.createdAt) }}</span>
            <span>Updated {{ formatTime(conversation.updatedAt) }}</span>
            <span>Age {{ formatRecency(conversation.updatedAt) }}</span>
          </div>
        </article>
      </div>
    </section>

    <section v-if="sortedConversations.length > 0" class="panel tactical-table-panel">
      <div class="section-title compact">
        <div>
          <h2>Dense watch table</h2>
          <p>Fast scan layout for operators who prefer compact rows.</p>
        </div>
      </div>

      <div class="table-wrap tactical-table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>Platform</th>
              <th>Session</th>
              <th>Created</th>
              <th>Updated</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="conversation in sortedConversations"
              :key="conversation.id"
              class="clickable-row"
              @click="openConversation(conversation.id)"
            >
              <td>{{ conversation.platform }}</td>
              <td><code>{{ conversation.sessionId }}</code></td>
              <td>{{ formatTime(conversation.createdAt) }}</td>
              <td>{{ formatTime(conversation.updatedAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import EmptyState from '../components/EmptyState.vue';
import StatusDot from '../components/StatusDot.vue';
import { useDashboardStore } from '../stores/dashboard';

const store = useDashboardStore();
const router = useRouter();

const sortedConversations = computed(() =>
  [...store.conversations].sort((left, right) => right.updatedAt - left.updatedAt),
);
const platformCount = computed(() => new Set(store.conversations.map((conversation) => conversation.platform)).size);
const staleCount = computed(() =>
  store.conversations.filter((conversation) => Date.now() - conversation.updatedAt > 12 * 60 * 60 * 1000).length,
);
const freshestLabel = computed(() => {
  const latest = sortedConversations.value[0];
  return latest ? formatRecency(latest.updatedAt) : 'none';
});
const platformSpread = computed(() => {
  if (platformCount.value === 0) return 'No active channels';
  return `${platformCount.value} platform${platformCount.value === 1 ? '' : 's'} currently represented.`;
});

function openConversation(id: string) {
  void router.push(`/conversations/${id}`);
}

function formatTime(value: number) {
  return new Intl.DateTimeFormat(undefined, { month: 'short', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(value);
}

function formatRecency(value: number) {
  const diff = Math.max(0, Date.now() - value);
  const minutes = Math.floor(diff / 60000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

function recencyLabel(value: number) {
  const diff = Date.now() - value;
  if (diff < 30 * 60 * 1000) return 'Hot';
  if (diff < 12 * 60 * 60 * 1000) return 'Warm';
  return 'Cold';
}

function recencyTone(value: number): 'ok' | 'warn' | 'muted' | 'error' {
  const diff = Date.now() - value;
  if (diff < 30 * 60 * 1000) return 'ok';
  if (diff < 12 * 60 * 60 * 1000) return 'warn';
  return 'muted';
}
</script>
