<template>
  <div class="app-shell">
    <aside class="sidebar">
      <RouterLink class="brand" to="/">
        <span class="brand-mark">P</span>
        <span>
          <strong>PriestessBot</strong>
          <small>Runtime Dashboard</small>
        </span>
      </RouterLink>
      <nav class="nav-list">
        <RouterLink v-for="route in navRoutes" :key="route.path" :to="route.path">
          {{ route.meta?.label }}
        </RouterLink>
      </nav>
    </aside>

    <main class="workspace">
      <header class="topbar">
        <div>
          <h1>{{ currentLabel }}</h1>
          <p v-if="store.lastUpdated">Updated {{ updatedAt }}</p>
          <p v-else>Connects to the local Dashboard API.</p>
        </div>
        <div class="topbar-actions">
          <span class="status-pill" :class="{ ok: store.health?.status === 'UP' }">
            <span class="status-dot"></span>
            {{ store.health?.status ?? 'Unknown' }}
          </span>
          <button type="button" class="icon-button" :disabled="store.loading" @click="store.refreshAll()" title="Refresh">
            Refresh
          </button>
        </div>
      </header>

      <section v-if="store.error" class="notice error">
        {{ store.error }}
      </section>

      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { routes } from '../router';
import { useDashboardStore } from '../stores/dashboard';

const route = useRoute();
const store = useDashboardStore();
const navRoutes = routes.filter((item) => item.meta?.nav !== false);

const currentLabel = computed(() => String(route.meta.label ?? 'Dashboard'));
const updatedAt = computed(() => {
  if (!store.lastUpdated) return '';
  return new Intl.DateTimeFormat('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit' }).format(store.lastUpdated);
});

onMounted(() => {
  if (!store.lastUpdated) void store.refreshAll();
});
</script>
