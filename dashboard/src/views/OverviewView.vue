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

  <section class="panel">
    <div class="section-title">
      <div>
        <h2>Components</h2>
        <p>Current backend health report.</p>
      </div>
    </div>
    <div class="grid list-grid">
      <article v-for="(value, key) in store.health?.components" :key="key" class="card">
        <h3>{{ key }}</h3>
        <StatusDot :label="value" :tone="value === 'UP' ? 'ok' : 'muted'" />
      </article>
    </div>
  </section>

  <section class="panel">
    <div class="section-title">
      <div>
        <h2>Diagnostics</h2>
        <p>Runtime paths and extension counts.</p>
      </div>
      <span>{{ formatDuration(store.health?.uptimeMillis ?? 0) }}</span>
    </div>
    <div class="grid list-grid">
      <article v-for="(value, key) in store.health?.diagnostics" :key="key" class="card">
        <h3>{{ formatKey(String(key)) }}</h3>
        <p class="diagnostic-value">{{ value }}</p>
      </article>
    </div>
  </section>

  <section class="panel">
    <div class="section-title">
      <div>
        <h2>Recent Conversations</h2>
        <p>{{ store.conversations.length }} tracked sessions.</p>
      </div>
      <RouterLink to="/conversations">Open</RouterLink>
    </div>
    <EmptyState v-if="store.conversations.length === 0" title="No conversations yet" detail="Messages will appear after a platform starts receiving traffic." />
    <table v-else class="table">
      <thead>
        <tr>
          <th>Platform</th>
          <th>Session</th>
          <th>Updated</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="conversation in store.conversations.slice(0, 5)" :key="conversation.id">
          <td>{{ conversation.platform }}</td>
          <td>{{ conversation.sessionId }}</td>
          <td>{{ formatTime(conversation.updatedAt) }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router';
import EmptyState from '../components/EmptyState.vue';
import StatusDot from '../components/StatusDot.vue';
import { useDashboardStore } from '../stores/dashboard';

const store = useDashboardStore();

function formatTime(value: number) {
  return new Intl.DateTimeFormat('en-US', { month: 'short', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(value);
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

<style scoped>
.diagnostic-value {
  color: var(--muted);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  overflow-wrap: anywhere;
}
</style>
