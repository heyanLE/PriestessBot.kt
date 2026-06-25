<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="app-header-inner">
        <RouterLink class="brand" to="/">
          <span class="brand-mark">P</span>
          <span>
            <strong>PriestessBot</strong>
            <small>Runtime Dashboard</small>
          </span>
        </RouterLink>

        <nav class="nav-list" aria-label="Dashboard navigation">
          <RouterLink v-for="route in navRoutes" :key="route.path" :to="route.path">
            {{ route.meta?.label }}
          </RouterLink>
        </nav>

        <div class="topbar-actions">
          <span class="status-pill" :class="{ ok: store.health?.status === 'UP' }">
            <span class="status-dot"></span>
            {{ store.health?.status ?? 'Unknown' }}
          </span>
          <button type="button" class="language-button" @click="toggleDashboardLanguage()" title="Switch language" aria-label="Switch language">
            {{ dashboardLanguage === 'zh' ? 'EN' : '中文' }}
          </button>
          <button type="button" class="icon-button" :disabled="store.loading" @click="store.refreshAll()" title="Refresh dashboard data" aria-label="Refresh dashboard data">
            {{ store.loading ? '...' : 'R' }}
          </button>
        </div>
      </div>
    </header>

    <main class="workspace">
      <header class="page-heading">
        <div>
          <h1>{{ currentLabel }}</h1>
          <p v-if="store.lastUpdated">Updated {{ updatedAt }}</p>
          <p v-else>Local runtime controls and operating state.</p>
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
import { computed, nextTick, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { routes } from '../router';
import { useDashboardStore } from '../stores/dashboard';
import { applyTranslations, dashboardLanguage, toggleDashboardLanguage, translate } from '../i18n';

const route = useRoute();
const store = useDashboardStore();
const navRoutes = routes.filter((item) => item.meta?.nav !== false);

const currentLabel = computed(() => translate(String(route.meta.label ?? 'Dashboard')));
const updatedAt = computed(() => {
  if (!store.lastUpdated) return '';
  return new Intl.DateTimeFormat('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit' }).format(store.lastUpdated);
});

onMounted(() => {
  if (!store.lastUpdated) void store.refreshAll();
  void nextTick(() => applyTranslations());
});

watch(
  () => route.fullPath,
  () => {
    void nextTick(() => applyTranslations());
  },
);

watch(dashboardLanguage, () => {
  void nextTick(() => applyTranslations());
});
</script>
