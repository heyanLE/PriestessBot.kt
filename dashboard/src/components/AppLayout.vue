<template>
  <div class="app-shell">
    <aside class="shell-sidebar">
      <div class="shell-brand">
        <RouterLink class="brand brand-link" to="/">
          <span class="brand-mark">
            <img src="/assets/priestess-icon.jpg" alt="PriestessBot emblem" />
          </span>
          <span class="brand-copy">
            <strong>PriestessBot</strong>
            <small>Runtime Control Center</small>
          </span>
        </RouterLink>
      </div>

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

      <section class="sidebar-status-card">
        <header>
          <span class="sidebar-status-eyebrow">Priestess Core</span>
          <strong>{{ store.health?.status ?? 'Unknown' }}</strong>
        </header>
        <dl class="sidebar-status-list">
          <div>
            <dt>Runtime</dt>
            <dd>{{ store.health?.status === 'UP' ? 'Online' : 'Awaiting signal' }}</dd>
          </div>
          <div>
            <dt>Sessions</dt>
            <dd>{{ store.conversations.length }} tracked</dd>
          </div>
          <div>
            <dt>Providers</dt>
            <dd>{{ store.providers.length }} registered</dd>
          </div>
        </dl>
      </section>

      <figure class="sidebar-persona">
        <img src="/assets/priestess-persona.png" alt="Priestess persona illustration" />
        <figcaption>
          <span>[R3D] STRATEGIST</span>
          <small>Stronghold protocol active.</small>
        </figcaption>
      </figure>
    </aside>

    <div class="shell-main">
      <header class="shell-topbar">
        <div class="topbar-leading">
          <span class="topbar-emblem">
            <img src="/assets/priestess-icon.jpg" alt="" aria-hidden="true" />
          </span>
          <div>
            <strong>{{ currentLabel }}</strong>
            <p>{{ currentSummary }}</p>
          </div>
        </div>

        <div class="topbar-actions">
          <span class="status-pill" :class="{ ok: store.health?.status === 'UP' }">
            <span class="status-dot"></span>
            {{ store.health?.status === 'UP' ? 'Runtime Online' : store.health?.status ?? 'Unknown' }}
          </span>
          <span class="inline-status muted" v-if="store.lastUpdated">Updated {{ updatedAt }}</span>
          <button
            type="button"
            :disabled="store.loading"
            @click="store.refreshAll()"
            title="Refresh dashboard data"
            aria-label="Refresh dashboard data"
          >
            {{ store.loading ? '...' : 'Refresh' }}
          </button>
        </div>
      </header>

      <main class="workspace">
        <header class="page-heading">
          <div>
            <span class="page-kicker">{{ currentGroup }}</span>
            <h1>{{ currentLabel }}</h1>
            <p>{{ currentSummary }}</p>
          </div>

          <div class="page-heading-aside">
            <RouterLink class="button-link subtle-link" to="/workspaces">
              Diagnostics
            </RouterLink>
            <div class="heading-metrics">
              <span>
                <strong>{{ store.runningPlatforms }}</strong>
                <small>Platforms</small>
              </span>
              <span>
                <strong>{{ store.providers.length }}</strong>
                <small>Providers</small>
              </span>
              <span>
                <strong>{{ store.tools.length }}</strong>
                <small>Tools</small>
              </span>
            </div>
          </div>
        </header>

        <section v-if="store.error" class="notice error">
          {{ store.error }}
        </section>

        <RouterView />
      </main>

      <footer class="shell-statusbar">
        <span>
          <i></i>
          Gateway {{ store.health?.status === 'UP' ? 'stable' : 'idle' }}
        </span>
        <span>Active sessions: {{ store.conversations.length }}</span>
        <span>Workdir: {{ store.workingDirectory?.effectivePath || 'not set' }}</span>
        <span>Config source: {{ store.workingDirectory?.pathSource || 'default' }}</span>
        <span>Priestess protocol ready.</span>
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

const navGroups = computed(() => {
  const grouped = new Map<string, typeof navRoutes>();
  navRoutes.forEach((routeRecord) => {
    const groupName = String(routeRecord.meta?.group ?? 'Other');
    if (!grouped.has(groupName)) grouped.set(groupName, []);
    grouped.get(groupName)?.push(routeRecord);
  });
  return [...grouped.entries()].map(([name, items]) => ({ name, items }));
});

const currentLabel = computed(() => String(route.meta.label ?? 'Dashboard'));
const currentSummary = computed(() => String(route.meta.summary ?? 'Local runtime controls and operating state.'));
const currentGroup = computed(() => String(route.meta.group ?? 'Runtime'));
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
