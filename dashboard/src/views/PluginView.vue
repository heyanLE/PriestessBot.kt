<template>
  <section class="panel">
    <div class="section-title">
      <div>
        <h2>Plugins</h2>
        <p>{{ store.plugins.plugins.length }} discovered plugins, {{ store.plugins.extensions.length }} extensions.</p>
      </div>
      <button type="button" class="primary" @click="store.discoverPlugins()">Discover</button>
    </div>
    <EmptyState v-if="store.plugins.plugins.length === 0" title="No plugins discovered" detail="Place plugin manifests in the configured plugin directory and discover again." />
    <div v-else class="grid list-grid">
      <article v-for="plugin in store.plugins.plugins" :key="plugin.manifest.id" class="card">
        <div class="section-title">
          <h3>{{ plugin.manifest.name }}</h3>
          <StatusDot :label="plugin.state" :tone="plugin.state === 'ENABLED' ? 'ok' : plugin.state === 'FAILED' ? 'error' : 'muted'" />
        </div>
        <p>{{ plugin.manifest.id }} · {{ plugin.manifest.version }}</p>
        <p v-if="plugin.error" class="muted">{{ plugin.error }}</p>
        <div class="toolbar">
          <button type="button" @click="store.setPluginState(plugin.manifest.id, 'load')">Load</button>
          <button type="button" class="primary" @click="store.setPluginState(plugin.manifest.id, 'enable')">Enable</button>
          <button type="button" @click="store.setPluginState(plugin.manifest.id, 'disable')">Disable</button>
          <button type="button" @click="store.setPluginState(plugin.manifest.id, 'unload')">Unload</button>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import EmptyState from '../components/EmptyState.vue';
import StatusDot from '../components/StatusDot.vue';
import { useDashboardStore } from '../stores/dashboard';

const store = useDashboardStore();
</script>
