<template>
  <div class="tactical-list-view">
    <section class="panel tactical-list-hero">
      <div class="tactical-list-hero-grid">
        <div class="tactical-list-copy">
          <div class="tactical-list-band">
            <span>Assets</span>
            <span>Providers</span>
          </div>

          <h2>Provider capability matrix</h2>
          <p>
            Review provider coverage, capability depth, and health posture from one clean
            inventory before the runtime hands off requests.
          </p>

          <div class="tactical-stat-grid">
            <article class="card tactical-stat-card">
              <span class="tactical-stat-label">Registered</span>
              <strong>{{ store.providers.length }}</strong>
              <p>providers cataloged in the current runtime registry.</p>
            </article>
            <article class="card tactical-stat-card">
              <span class="tactical-stat-label">Tool Ready</span>
              <strong>{{ toolReadyCount }}</strong>
              <p>lanes that can accept tool-calling contracts.</p>
            </article>
            <article class="card tactical-stat-card">
              <span class="tactical-stat-label">Vision Ready</span>
              <strong>{{ visionReadyCount }}</strong>
              <p>providers that can process image-bearing operator requests.</p>
            </article>
            <article class="card tactical-stat-card">
              <span class="tactical-stat-label">Verified Online</span>
              <strong>{{ onlineCount }}</strong>
              <p>{{ testedCount > 0 ? `${testedCount} tested in the last sweep.` : 'Awaiting a health sweep.' }}</p>
            </article>
          </div>
        </div>

        <aside class="tactical-ledger">
          <h3>What matters here</h3>
          <p>Use capability coverage to compare fit, then verify online status only when a runtime issue appears.</p>

          <div class="tactical-ledger-row">
            <span>Health sweep</span>
            <strong>{{ testedCount > 0 ? `${onlineCount}/${testedCount} online` : 'Not run yet' }}</strong>
          </div>
          <div class="tactical-ledger-row">
            <span>Streaming cover</span>
            <strong>{{ streamingReadyCount }} lanes support live response flow</strong>
          </div>
          <div class="tactical-ledger-row">
            <span>Operator note</span>
            <strong>Keep high-capability routes visible before agent routing decisions fan out.</strong>
          </div>
        </aside>
      </div>
    </section>

    <section class="panel tactical-table-panel">
      <div class="section-title">
        <div>
          <h2>Provider registry</h2>
          <p>{{ store.providers.length }} runtime providers registered.</p>
        </div>
        <button type="button" class="primary" @click="store.testProviders()">Test All</button>
      </div>

      <EmptyState
        v-if="store.providers.length === 0"
        class="tactical-empty"
        title="No providers registered"
        detail="Provider capability cards will appear after the runtime publishes its registry."
      />

      <div v-else class="tactical-card-grid">
        <article v-for="provider in store.providers" :key="provider.name" class="card tactical-card-row">
          <div class="tactical-card-head">
            <div>
              <strong>{{ provider.displayName }}</strong>
              <p>{{ provider.kind }} / <span class="tactical-inline-code">{{ provider.name }}</span></p>
            </div>
            <StatusDot :label="healthLabel(provider.name)" :tone="healthTone(provider.name)" />
          </div>

          <div class="tactical-kv-list">
            <div class="tactical-kv">
              <span class="tactical-inline-code">Tools</span>
              <strong>{{ yesNo(provider.supportToolCalling) }}</strong>
            </div>
            <div class="tactical-kv">
              <span class="tactical-inline-code">Vision</span>
              <strong>{{ yesNo(provider.supportVision) }}</strong>
            </div>
            <div class="tactical-kv">
              <span class="tactical-inline-code">Streaming</span>
              <strong>{{ yesNo(provider.supportStreaming) }}</strong>
            </div>
          </div>

          <div class="tactical-chip-list">
            <span v-if="provider.supportToolCalling" class="chip">Tool Calling</span>
            <span v-if="provider.supportVision" class="chip">Vision</span>
            <span v-if="provider.supportStreaming" class="chip">Streaming</span>
            <span v-if="!provider.supportToolCalling && !provider.supportVision && !provider.supportStreaming" class="chip">Text Only</span>
          </div>
        </article>
      </div>
    </section>

    <section v-if="store.providers.length > 0" class="panel tactical-table-panel">
      <div class="section-title compact">
        <div>
          <h2>Dense matrix</h2>
          <p>Compact operator readout for fast side-by-side comparison.</p>
        </div>
      </div>

      <div class="table-wrap tactical-table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Kind</th>
              <th>Health</th>
              <th>Capabilities</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="provider in store.providers" :key="provider.name">
              <td>
                <strong>{{ provider.displayName }}</strong>
                <p class="muted">{{ provider.name }}</p>
              </td>
              <td>{{ provider.kind }}</td>
              <td>
                <StatusDot :label="healthLabel(provider.name)" :tone="healthTone(provider.name)" />
              </td>
              <td>
                <div class="chip-row">
                  <span class="chip">Tools {{ yesNo(provider.supportToolCalling) }}</span>
                  <span class="chip">Vision {{ yesNo(provider.supportVision) }}</span>
                  <span class="chip">Streaming {{ yesNo(provider.supportStreaming) }}</span>
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

const toolReadyCount = computed(() => store.providers.filter((provider) => provider.supportToolCalling).length);
const visionReadyCount = computed(() => store.providers.filter((provider) => provider.supportVision).length);
const streamingReadyCount = computed(() => store.providers.filter((provider) => provider.supportStreaming).length);
const testedCount = computed(() => Object.keys(store.providerTests).length);
const onlineCount = computed(() => Object.values(store.providerTests).filter(Boolean).length);

function healthLabel(name: string) {
  if (!(name in store.providerTests)) return 'Not tested';
  return store.providerTests[name] ? 'Online' : 'Failed';
}

function healthTone(name: string): 'ok' | 'warn' | 'muted' | 'error' {
  if (!(name in store.providerTests)) return 'muted';
  return store.providerTests[name] ? 'ok' : 'error';
}

function yesNo(value: boolean) {
  return value ? 'Yes' : 'No';
}
</script>
