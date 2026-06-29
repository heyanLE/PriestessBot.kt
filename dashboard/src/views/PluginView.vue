<template>
  <div class="tactical-list-view">
    <section class="panel tactical-list-hero">
      <div class="tactical-list-hero-grid">
        <div class="tactical-list-copy">
          <div class="tactical-list-band">
            <span>Priestess / Extension Deck</span>
            <span>Shell Discovery Board</span>
          </div>

          <h2>Plugin extension board</h2>
          <p>
            Track plugin lifecycle, failure posture, and extension spread from a structured daytime
            operations deck before optional capabilities enter the shell.
          </p>

          <div class="tactical-stat-grid">
            <article class="card tactical-stat-card">
              <span class="tactical-stat-label">Discovered</span>
              <strong>{{ store.plugins.plugins.length }}</strong>
              <p>plugin packages mapped into the current plugin directory.</p>
            </article>
            <article class="card tactical-stat-card">
              <span class="tactical-stat-label">Enabled</span>
              <strong>{{ enabledCount }}</strong>
              <p>extensions actively wired into the dashboard shell.</p>
            </article>
            <article class="card tactical-stat-card">
              <span class="tactical-stat-label">Failed</span>
              <strong>{{ failedCount }}</strong>
              <p>packages currently flagged for operator review.</p>
            </article>
            <article class="card tactical-stat-card">
              <span class="tactical-stat-label">Extensions</span>
              <strong>{{ store.plugins.extensions.length }}</strong>
              <p>registered extension surfaces exposed by discovered plugins.</p>
            </article>
          </div>
        </div>

        <aside class="tactical-ledger">
          <h3>Operational ledger</h3>
          <p>Discovery stays separate from enablement so the shell can stay tidy while optional power grows.</p>

          <div class="tactical-ledger-row">
            <span>Discovery posture</span>
            <strong>{{ store.plugins.plugins.length > 0 ? 'Plugin inventory loaded' : 'Awaiting discovery scan' }}</strong>
          </div>
          <div class="tactical-ledger-row">
            <span>Extension spread</span>
            <strong>{{ extensionSummary }}</strong>
          </div>
          <div class="tactical-ledger-row">
            <span>Operator note</span>
            <strong>Failed packages should read like alerts, not hidden footnotes inside a generic table row.</strong>
          </div>
        </aside>
      </div>
    </section>

    <section class="panel tactical-table-panel">
      <div class="section-title">
        <div>
          <h2>Plugin roster</h2>
          <p>{{ store.plugins.plugins.length }} discovered plugins, {{ store.plugins.extensions.length }} extensions.</p>
        </div>
        <button type="button" class="primary" @click="store.discoverPlugins()">Discover</button>
      </div>

      <EmptyState
        v-if="store.plugins.plugins.length === 0"
        class="tactical-empty"
        title="No plugins discovered"
        detail="Place plugin manifests in the configured directory, then run discovery to populate the extension deck."
      />

      <div v-else class="tactical-card-grid">
        <article v-for="plugin in store.plugins.plugins" :key="plugin.manifest.id" class="card tactical-card-row">
          <div class="tactical-card-head">
            <div>
              <strong>{{ plugin.manifest.name }}</strong>
              <p>{{ plugin.manifest.id }} / v{{ plugin.manifest.version }}</p>
            </div>
            <StatusDot :label="plugin.state" :tone="pluginTone(plugin.state)" />
          </div>

          <div class="tactical-chip-list">
            <span v-for="capability in plugin.manifest.capabilities" :key="capability" class="chip">{{ capability }}</span>
            <span v-if="plugin.manifest.capabilities.length === 0" class="chip">No declared capabilities</span>
          </div>

          <p>{{ plugin.manifest.description || 'No description provided.' }}</p>

          <p v-if="plugin.error" class="notice error plugin-error">{{ plugin.error }}</p>

          <div class="tactical-action-row">
            <button type="button" @click="store.setPluginState(plugin.manifest.id, 'load')">Load</button>
            <button type="button" class="primary" @click="store.setPluginState(plugin.manifest.id, 'enable')">Enable</button>
            <button type="button" @click="store.setPluginState(plugin.manifest.id, 'disable')">Disable</button>
            <button type="button" @click="store.setPluginState(plugin.manifest.id, 'unload')">Unload</button>
          </div>
        </article>
      </div>
    </section>

    <section v-if="store.plugins.plugins.length > 0" class="panel tactical-table-panel">
      <div class="section-title compact">
        <div>
          <h2>Dense plugin matrix</h2>
          <p>Compact lifecycle readout for fast operator sweeps.</p>
        </div>
      </div>

      <div class="table-wrap tactical-table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>Plugin</th>
              <th>Version</th>
              <th>State</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="plugin in store.plugins.plugins" :key="plugin.manifest.id">
              <td>
                <strong>{{ plugin.manifest.name }}</strong>
                <p class="muted">{{ plugin.manifest.id }}</p>
                <p v-if="plugin.error" class="notice error plugin-error">{{ plugin.error }}</p>
              </td>
              <td>{{ plugin.manifest.version }}</td>
              <td>
                <StatusDot :label="plugin.state" :tone="pluginTone(plugin.state)" />
              </td>
              <td>
                <div class="toolbar">
                  <button type="button" @click="store.setPluginState(plugin.manifest.id, 'load')">Load</button>
                  <button type="button" class="primary" @click="store.setPluginState(plugin.manifest.id, 'enable')">Enable</button>
                  <button type="button" @click="store.setPluginState(plugin.manifest.id, 'disable')">Disable</button>
                  <button type="button" @click="store.setPluginState(plugin.manifest.id, 'unload')">Unload</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import EmptyState from '../components/EmptyState.vue';
import StatusDot from '../components/StatusDot.vue';
import { useDashboardStore } from '../stores/dashboard';

const store = useDashboardStore();

const enabledCount = computed(() => store.plugins.plugins.filter((plugin) => plugin.state === 'ENABLED').length);
const failedCount = computed(() => store.plugins.plugins.filter((plugin) => plugin.state === 'FAILED').length);
const extensionSummary = computed(() => {
  if (store.plugins.extensions.length === 0) return 'No extension surfaces registered yet';
  return `${store.plugins.extensions.length} extension surface${store.plugins.extensions.length === 1 ? '' : 's'} currently exposed.`;
});

function pluginTone(state: string): 'ok' | 'warn' | 'muted' | 'error' {
  if (state === 'ENABLED') return 'ok';
  if (state === 'FAILED') return 'error';
  if (state === 'LOADED') return 'warn';
  return 'muted';
}
</script>

<style scoped>
.plugin-error {
  margin-bottom: 0;
}
</style>
