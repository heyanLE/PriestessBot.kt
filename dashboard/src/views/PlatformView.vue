<template>
  <div class="tactical-list-view">
    <section class="panel tactical-list-hero">
      <div class="tactical-list-hero-grid">
        <div class="tactical-list-copy">
          <div class="tactical-list-band">
            <span>Assets</span>
            <span>Platforms</span>
          </div>

          <h2>Platform inventory</h2>
          <p>
            Track ingress relays, endpoint posture, and runtime actions from one concise platform
            inventory before traffic reaches the conversation pipeline.
          </p>

          <div class="tactical-stat-grid">
            <article class="card tactical-stat-card">
              <span class="tactical-stat-label">Configured</span>
              <strong>{{ store.platforms.length }}</strong>
              <p>platform lanes cataloged in the runtime mesh.</p>
            </article>
            <article class="card tactical-stat-card">
              <span class="tactical-stat-label">Enabled</span>
              <strong>{{ store.enabledPlatforms }}</strong>
              <p>relays currently marked available for startup.</p>
            </article>
            <article class="card tactical-stat-card">
              <span class="tactical-stat-label">Running</span>
              <strong>{{ store.runningPlatforms }}</strong>
              <p>lanes actively carrying traffic right now.</p>
            </article>
            <article class="card tactical-stat-card">
              <span class="tactical-stat-label">Standby</span>
              <strong>{{ standbyCount }}</strong>
              <p>configured routes awaiting operator activation.</p>
            </article>
          </div>
        </div>

        <aside class="tactical-ledger">
          <h3>What matters here</h3>
          <p>Keep runtime entry points obvious: status first, endpoint second, actions last.</p>

          <div class="tactical-ledger-row">
            <span>Live doctrine</span>
            <strong>{{ store.runningPlatforms > 0 ? `${store.runningPlatforms} relay lanes online` : 'No active relays' }}</strong>
          </div>
          <div class="tactical-ledger-row">
            <span>Endpoint map</span>
            <strong>{{ endpointSummary }}</strong>
          </div>
          <div class="tactical-ledger-row">
            <span>Operator note</span>
            <strong>Start and stop actions stay close to each relay instead of buried in a dense table.</strong>
          </div>
        </aside>
      </div>
    </section>

    <section class="panel tactical-table-panel">
      <div class="section-title">
        <div>
          <h2>Platform roster</h2>
          <p>{{ store.enabledPlatforms }} enabled, {{ store.runningPlatforms }} running.</p>
        </div>
      </div>

      <EmptyState
        v-if="store.platforms.length === 0"
        class="tactical-empty"
        title="No platforms configured"
        detail="Add platform config through the config surface or config file to populate the relay board."
      />

      <div v-else class="tactical-card-grid">
        <article v-for="platform in store.platforms" :key="platform.name" class="card tactical-card-row">
          <div class="tactical-card-head">
            <div>
              <strong>{{ platform.name }}</strong>
              <p>{{ platform.type }} / relay lane</p>
            </div>
            <StatusDot :label="statusLabel(platform)" :tone="statusTone(platform)" />
          </div>

          <div class="tactical-kv-list">
            <div class="tactical-kv">
              <span class="tactical-inline-code">HTTP</span>
              <strong>{{ formatEndpoint(platform.host, platform.port) }}</strong>
            </div>
            <div class="tactical-kv">
              <span class="tactical-inline-code">WS</span>
              <strong>{{ platform.wsPort > 0 ? platform.wsPort : 'disabled' }}</strong>
            </div>
            <div class="tactical-kv">
              <span class="tactical-inline-code">State</span>
              <strong>{{ platform.running ? 'Live' : platform.enabled ? 'Standby' : 'Parked' }}</strong>
            </div>
          </div>

          <div class="tactical-action-row">
            <button
              type="button"
              class="primary"
              :disabled="platform.enabled"
              @click="store.setPlatformEnabled(platform.name, true)"
            >
              Start
            </button>
            <button
              type="button"
              :disabled="!platform.enabled"
              @click="store.setPlatformEnabled(platform.name, false)"
            >
              Stop
            </button>
          </div>
        </article>
      </div>
    </section>

    <section v-if="store.platforms.length > 0" class="panel tactical-table-panel">
      <div class="section-title compact">
        <div>
          <h2>Dense endpoints</h2>
          <p>Quick comparison grid for operator checks and escalation handoff.</p>
        </div>
      </div>

      <div class="table-wrap tactical-table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Type</th>
              <th>Endpoint</th>
              <th>State</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="platform in store.platforms" :key="platform.name">
              <td><strong>{{ platform.name }}</strong></td>
              <td>{{ platform.type }}</td>
              <td>
                <code>{{ formatEndpoint(platform.host, platform.port) }}</code>
                <p class="muted">ws {{ platform.wsPort }}</p>
              </td>
              <td>
                <StatusDot :label="statusLabel(platform)" :tone="statusTone(platform)" />
              </td>
              <td>
                <div class="toolbar">
                  <button
                    type="button"
                    class="primary"
                    :disabled="platform.enabled"
                    @click="store.setPlatformEnabled(platform.name, true)"
                  >
                    Start
                  </button>
                  <button
                    type="button"
                    :disabled="!platform.enabled"
                    @click="store.setPlatformEnabled(platform.name, false)"
                  >
                    Stop
                  </button>
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
import type { PlatformStatusDto } from '../api/dashboard';

const store = useDashboardStore();

const standbyCount = computed(() => store.platforms.filter((platform) => platform.enabled && !platform.running).length);
const endpointSummary = computed(() => {
  if (store.platforms.length === 0) return 'No endpoint map loaded';
  return `${store.platforms.length} lane${store.platforms.length === 1 ? '' : 's'} with HTTP + optional WS ingress.`;
});

function statusLabel(platform: PlatformStatusDto) {
  if (platform.running) return 'Running';
  if (platform.enabled) return 'Enabled';
  return 'Stopped';
}

function statusTone(platform: PlatformStatusDto): 'ok' | 'warn' | 'muted' | 'error' {
  if (platform.running) return 'ok';
  if (platform.enabled) return 'warn';
  return 'muted';
}

function formatEndpoint(host: string, port: number) {
  return `${host}:${port}`;
}
</script>
