<template>
  <div class="grid metric-grid">
    <article class="panel metric">
      <strong>{{ store.health?.status ?? '...' }}</strong>
      <span>Runtime status</span>
    </article>
    <article class="panel metric">
      <strong>{{ store.runningPlatforms }}/{{ store.platforms.length }}</strong>
      <span>Running platforms</span>
    </article>
    <article class="panel metric">
      <strong>{{ store.providers.length }}</strong>
      <span>Providers</span>
    </article>
    <article class="panel metric">
      <strong>{{ store.enabledPlugins }}/{{ store.plugins.plugins.length }}</strong>
      <span>Enabled plugins</span>
    </article>
  </div>

  <div class="workbench-grid">
    <section class="panel">
      <div class="section-title">
        <div>
          <h2>Components</h2>
          <p>{{ componentRows.length }} backend components reported.</p>
        </div>
        <span class="inline-status" :class="{ ok: store.health?.status === 'UP', warn: store.health?.status !== 'UP' }">
          {{ store.health?.status ?? 'Unknown' }}
        </span>
      </div>

      <EmptyState v-if="componentRows.length === 0" title="No component report" detail="Health snapshots appear after the local API responds." />
      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>Component</th>
              <th>Status</th>
              <th>Signal</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in componentRows" :key="row.name">
              <td><strong>{{ row.name }}</strong></td>
              <td>
                <StatusDot :label="row.status" :tone="row.status === 'UP' ? 'ok' : 'muted'" />
              </td>
              <td>{{ row.status === 'UP' ? 'Healthy' : 'Needs attention' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <aside class="panel detail-panel">
      <div class="section-title">
        <div>
          <h2>Runtime Detail</h2>
          <p>{{ formatDuration(store.health?.uptimeMillis ?? 0) }} uptime.</p>
        </div>
      </div>

      <div class="detail-list">
        <div v-for="(value, key) in store.health?.diagnostics" :key="key" class="detail-item">
          <span>{{ formatKey(String(key)) }}</span>
          <code>{{ value }}</code>
        </div>
        <div class="detail-item">
          <span>Conversations</span>
          <strong>{{ store.conversations.length }}</strong>
        </div>
        <div class="detail-item">
          <span>Workspaces</span>
          <strong>{{ store.workspaces.workspaces.length }}</strong>
        </div>
      </div>
    </aside>
  </div>

  <section class="panel">
    <div class="section-title">
      <div>
        <h2>Recent Conversations</h2>
        <p>{{ store.conversations.length }} tracked sessions.</p>
      </div>
      <RouterLink class="button-link" to="/conversations">Open</RouterLink>
    </div>
    <EmptyState v-if="store.conversations.length === 0" title="No conversations yet" detail="Messages will appear after a platform starts receiving traffic." />
    <div v-else class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>Platform</th>
            <th>Session</th>
            <th>Updated</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="conversation in store.conversations.slice(0, 6)" :key="conversation.id">
            <td>{{ conversation.platform }}</td>
            <td><code>{{ conversation.sessionId }}</code></td>
            <td>{{ formatTime(conversation.updatedAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink } from 'vue-router';
import EmptyState from '../components/EmptyState.vue';
import StatusDot from '../components/StatusDot.vue';
import { useDashboardStore } from '../stores/dashboard';

const store = useDashboardStore();

const componentRows = computed(() =>
  Object.entries(store.health?.components ?? {}).map(([name, status]) => ({ name, status })),
);

function formatTime(value: number) {
  return new Intl.DateTimeFormat(undefined, { month: 'short', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(value);
}

function formatKey(value: string) {
  return value.replace(/([A-Z])/g, ' $1').replace(/^./, (first) => first.toUpperCase());
}

function formatDuration(value: number) {
  const totalSeconds = Math.floor(value / 1000);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  if (hours > 0) return `${hours}h ${minutes}m`;
  if (minutes > 0) return `${minutes}m ${seconds}s`;
  return `${seconds}s`;
}
</script>
