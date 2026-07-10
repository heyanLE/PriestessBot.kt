<template>
  <div class="app-shell">
    <aside class="shell-sidebar">
      <RouterLink class="shell-brand" to="/">
        <span class="shell-brand-mark">
          <img src="/assets/priestess-icon.jpg" alt="AstrBot icon" />
        </span>
        <span class="shell-brand-copy">
          <strong>AstrBot Dash</strong>
          <small>Vercel-style runtime operations</small>
        </span>
      </RouterLink>

      <nav class="shell-nav" aria-label="Dashboard navigation">
        <section v-for="group in navGroups" :key="group.name" class="nav-group">
          <header class="nav-group-title">{{ group.name }}</header>
          <RouterLink
            v-for="routeRecord in group.items"
            :key="routeRecord.path"
            class="nav-link"
            :to="routeRecord.path"
          >
            <span class="nav-link-copy">
              <strong>{{ routeRecord.meta?.label }}</strong>
              <small>{{ routeRecord.meta?.summary }}</small>
            </span>
          </RouterLink>
        </section>
      </nav>

      <section class="sidebar-shortcuts">
        <header class="nav-group-title">Pinned</header>
        <div class="sidebar-shortcut-list">
          <RouterLink class="shortcut-card" to="/">
            <strong>Runtime desk</strong>
            <small>Overview, incidents, and recovery.</small>
          </RouterLink>
          <RouterLink class="shortcut-card" to="/effective-runtime">
            <strong>Effective runtime</strong>
            <small>Trace the final config before changing it.</small>
          </RouterLink>
          <RouterLink class="shortcut-card" to="/agent">
            <strong>Validation bench</strong>
            <small>Run the agent and verify behavior after changes.</small>
          </RouterLink>
        </div>
      </section>

      <section class="sidebar-status-card">
        <header>
          <span class="sidebar-status-eyebrow">Runtime</span>
          <strong>{{ healthLabel }}</strong>
        </header>
        <dl class="sidebar-status-list">
          <div>
            <dt>Workspace</dt>
            <dd>{{ workspaceLabel }}</dd>
          </div>
          <div>
            <dt>Sessions</dt>
            <dd>{{ store.conversations.length }} tracked</dd>
          </div>
          <div>
            <dt>Providers</dt>
            <dd>{{ store.providers.length }} registered</dd>
          </div>
          <div>
            <dt>Last update</dt>
            <dd>{{ updatedAt || 'Awaiting sync' }}</dd>
          </div>
        </dl>
      </section>
    </aside>

    <div class="shell-main">
      <header class="shell-topbar">
        <div class="topbar-leading topbar-page-meta">
          <span class="page-kicker">{{ currentGroup }}</span>
          <strong>{{ currentLabel }}</strong>
          <p>{{ currentSummary }}</p>
        </div>

        <div class="topbar-actions">
          <span class="status-pill" :class="healthTone">
            <span class="status-dot"></span>
            {{ healthLabel }}
          </span>
          <span class="status-pill muted">Workspace {{ workspaceLabel }}</span>
          <span class="inline-status muted" v-if="store.lastUpdated">Updated {{ updatedAt }}</span>
          <button type="button" class="primary" :disabled="store.loading" @click="store.refreshAll()">
            {{ store.loading ? 'Refreshing' : 'Refresh' }}
          </button>
        </div>
      </header>

      <main class="workspace">
        <section v-if="store.error" class="notice error">
          {{ store.error }}
        </section>

        <RouterView />
      </main>

      <footer class="shell-statusbar">
        <span>
          <i></i>
          Gateway {{ store.health?.status === 'UP' ? 'healthy' : 'offline' }}
        </span>
        <span>Active sessions: {{ store.conversations.length }}</span>
        <span>Platforms: {{ store.runningPlatforms }}/{{ store.platforms.length }}</span>
        <span>Config source: {{ store.workingDirectory?.pathSource || 'default' }}</span>
        <span>Design system: Vercel-inspired</span>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { RouterLink, RouterView, useRoute } from 'vue-router';
import { routes } from '../router';
import { useDashboardStore } from '../stores/dashboard';

const route = useRoute();
const store = useDashboardStore();
const navRoutes = routes.filter((item) => item.meta?.nav !== false);
const groupOrder = ['Overview', 'Troubleshooting', 'Changes', 'Assets'];

const navGroups = computed(() => {
  const grouped = new Map<string, typeof navRoutes>();
  navRoutes.forEach((routeRecord) => {
    const groupName = String(routeRecord.meta?.group ?? 'Other');
    if (!grouped.has(groupName)) grouped.set(groupName, []);
    grouped.get(groupName)?.push(routeRecord);
  });
  return [...grouped.entries()]
    .map(([name, items]) => ({ name, items }))
    .sort((left, right) => {
      const leftIndex = groupOrder.indexOf(left.name);
      const rightIndex = groupOrder.indexOf(right.name);
      return (leftIndex === -1 ? 99 : leftIndex) - (rightIndex === -1 ? 99 : rightIndex);
    });
});

const currentLabel = computed(() => String(route.meta.label ?? 'Dashboard'));
const currentSummary = computed(() => String(route.meta.summary ?? 'Local runtime controls and operating state.'));
const currentGroup = computed(() => String(route.meta.group ?? 'Runtime'));
const healthTone = computed(() => (store.health?.status === 'UP' ? 'ok' : 'warn'));
const healthLabel = computed(() => (store.health?.status === 'UP' ? 'Runtime healthy' : store.health?.status ?? 'Awaiting telemetry'));
const workspaceLabel = computed(() => {
  const path = store.workingDirectory?.effectivePath?.trim();
  if (!path) return 'not set';
  const segments = path.split('/').filter(Boolean);
  return segments.at(-1) ?? path;
});
const updatedAt = computed(() => {
  if (!store.lastUpdated) return '';
  return new Intl.DateTimeFormat('en-US', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(store.lastUpdated);
});

onMounted(() => {
  if (!store.lastUpdated) void store.refreshAll();
});
</script>
