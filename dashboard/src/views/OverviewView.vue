<template>
  <div class="overview-page">
    <section class="panel overview-hero">
      <div class="overview-hero-copy">
        <span class="overview-eyebrow">Overview</span>
        <h1>Keep the runtime healthy.</h1>
        <p>
          Monitor incidents, effective runtime, validation signals, and recent traffic from one
          clean operator workspace.
        </p>
      </div>

      <div class="overview-hero-actions">
        <RouterLink class="button-link" to="/logs">Open logs</RouterLink>
        <RouterLink class="button-link" to="/effective-runtime">Inspect runtime</RouterLink>
        <RouterLink class="button-link primary" to="/agent">Run validation</RouterLink>
      </div>
    </section>

    <section class="overview-metric-grid">
      <article v-for="metric in metrics" :key="metric.label" class="card overview-metric-card">
        <span class="overview-metric-label">{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <p>{{ metric.detail }}</p>
      </article>
    </section>

    <section class="overview-workbench">
      <div class="overview-primary">
        <article class="panel">
          <div class="section-title">
            <div>
              <h2>Incident queue</h2>
              <p>Start with the newest watchpoints and jump straight into diagnosis.</p>
            </div>
            <RouterLink class="button-link" to="/logs">Open traces</RouterLink>
          </div>

          <EmptyState
            v-if="incidentItems.length === 0"
            title="No incidents reported"
            detail="Component health and runtime diagnostics look clear in the latest refresh."
          />

          <div v-else class="overview-incident-list">
            <RouterLink
              v-for="item in incidentItems"
              :key="item.id"
              class="overview-incident-item"
              :to="item.to"
            >
              <div class="overview-incident-head">
                <span class="inline-status" :class="item.tone">{{ item.badge }}</span>
                <span class="overview-incident-action">{{ item.action }}</span>
              </div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.detail }}</p>
            </RouterLink>
          </div>
        </article>

        <article class="panel">
          <div class="section-title">
            <div>
              <h2>Recent conversations</h2>
              <p>Use live traffic as the fastest way to verify behavior after a change.</p>
            </div>
            <RouterLink class="button-link" to="/conversations">All sessions</RouterLink>
          </div>

          <EmptyState
            v-if="recentConversations.length === 0"
            title="No conversations yet"
            detail="Once a platform starts receiving traffic, the newest sessions will appear here."
          />

          <div v-else class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>Platform</th>
                  <th>Session</th>
                  <th>Updated</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="conversation in recentConversations" :key="conversation.id" class="clickable-row">
                  <td>{{ conversation.platform }}</td>
                  <td>
                    <RouterLink :to="`/conversations/${conversation.id}`">
                      <code>{{ conversation.sessionId }}</code>
                    </RouterLink>
                  </td>
                  <td>{{ formatTime(conversation.updatedAt) }}</td>
                  <td>
                    <span class="inline-status" :class="recencyTone(conversation.updatedAt)">
                      {{ recencyLabel(conversation.updatedAt) }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </div>

      <aside class="overview-secondary">
        <article class="panel">
          <div class="section-title">
            <div>
              <h2>Effective runtime</h2>
              <p>See what is actually in effect before touching persistent config.</p>
            </div>
            <RouterLink class="button-link" to="/effective-runtime">Open</RouterLink>
          </div>

          <div class="detail-list">
            <div class="detail-item">
              <span>Workspace</span>
              <strong>{{ runtimeWorkspace }}</strong>
            </div>
            <div class="detail-item">
              <span>Agent</span>
              <strong>{{ runtimeAgent }}</strong>
            </div>
            <div class="detail-item">
              <span>Provider</span>
              <strong>{{ runtimeProvider }}</strong>
            </div>
            <div class="detail-item">
              <span>Tool policy</span>
              <strong>{{ toolPolicySummary }}</strong>
            </div>
            <div class="detail-item">
              <span>Detected skills</span>
              <strong>{{ detectedSkills }}</strong>
            </div>
            <div class="detail-item">
              <span>Trace rows</span>
              <strong>{{ traceRowCount }}</strong>
            </div>
          </div>
        </article>

        <article class="panel">
          <div class="section-title">
            <div>
              <h2>Validation snapshot</h2>
              <p>Close the loop after every change with one bench and one runtime check.</p>
            </div>
            <RouterLink class="button-link primary" to="/agent">Validate</RouterLink>
          </div>

          <div class="overview-check-list">
            <div class="overview-check-item">
              <span class="inline-status" :class="validationTone.manifest">{{ validationLabel.manifest }}</span>
              <div>
                <strong>Workspace manifest</strong>
                <p>{{ validationCopy.manifest }}</p>
              </div>
            </div>
            <div class="overview-check-item">
              <span class="inline-status" :class="validationTone.runtime">{{ validationLabel.runtime }}</span>
              <div>
                <strong>Runtime trace</strong>
                <p>{{ validationCopy.runtime }}</p>
              </div>
            </div>
            <div class="overview-check-item">
              <span class="inline-status" :class="validationTone.conversations">{{ validationLabel.conversations }}</span>
              <div>
                <strong>Traffic visibility</strong>
                <p>{{ validationCopy.conversations }}</p>
              </div>
            </div>
          </div>
        </article>

        <article class="panel">
          <div class="section-title">
            <div>
              <h2>Next actions</h2>
              <p>Jump into the parts of the runtime that operators usually touch next.</p>
            </div>
          </div>

          <div class="overview-action-grid">
            <RouterLink class="overview-action-card" to="/workspaces">
              <strong>Workspaces</strong>
              <small>Reload routing and inspect snapshots.</small>
            </RouterLink>
            <RouterLink class="overview-action-card" to="/providers">
              <strong>Providers</strong>
              <small>Check capability coverage and connectivity.</small>
            </RouterLink>
            <RouterLink class="overview-action-card" to="/tools">
              <strong>Tools</strong>
              <small>Review policy posture and exposure.</small>
            </RouterLink>
            <RouterLink class="overview-action-card" to="/config">
              <strong>Config</strong>
              <small>Apply controlled changes to runtime config.</small>
            </RouterLink>
          </div>
        </article>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink } from 'vue-router';
import EmptyState from '../components/EmptyState.vue';
import { useDashboardStore } from '../stores/dashboard';

type Tone = 'ok' | 'warn' | 'muted' | 'error';

const store = useDashboardStore();

const runtimeConfig = computed(() => store.effectiveRuntimePreview?.config ?? store.config);
const runtimeWorkingDirectory = computed(() => store.effectiveRuntimePreview?.workingDirectory ?? store.workingDirectory);
const traceRowCount = computed(() => store.effectiveRuntimePreview?.trace.length ?? 0);
const detectedSkills = computed(() => String(runtimeWorkingDirectory.value?.skills.length ?? 0));
const runtimeWorkspace = computed(() => runtimeWorkingDirectory.value?.effectivePath || 'Not set');
const runtimeAgent = computed(() => runtimeConfig.value?.agent.name || 'Unset');
const runtimeProvider = computed(() => runtimeConfig.value?.agent.providerName || 'Unset');
const toolPolicySummary = computed(() => {
  const enabled = runtimeConfig.value?.agent.enabledTools.length ?? 0;
  const disabled = runtimeConfig.value?.agent.disabledTools.length ?? 0;
  return `${enabled} enabled · ${disabled} denied`;
});

const componentAlerts = computed(() =>
  Object.entries(store.health?.components ?? {})
    .filter(([, status]) => toneForStatus(status) !== 'ok')
    .map(([name, status]) => ({
      id: `component-${name}`,
      title: humanize(name),
      detail: `${status} reported by the health endpoint. Review runtime traces and related assets.`,
      tone: toneForStatus(status),
      badge: status,
      action: 'Inspect',
      to: '/logs',
    })),
);

const diagnosticAlerts = computed(() =>
  Object.entries(store.health?.diagnostics ?? {})
    .slice(0, 4)
    .map(([key, value]) => ({
      id: `diagnostic-${key}`,
      title: humanize(key),
      detail: String(value),
      tone: String(value).toLowerCase().includes('missing') || String(value).toLowerCase().includes('error') ? 'warn' : 'muted',
      badge: 'Diagnostic',
      action: 'Review',
      to: '/effective-runtime',
    })),
);

const incidentItems = computed(() => [...componentAlerts.value, ...diagnosticAlerts.value].slice(0, 6));

const recentConversations = computed(() =>
  [...store.conversations].sort((left, right) => right.updatedAt - left.updatedAt).slice(0, 6),
);

const metrics = computed(() => [
  {
    label: 'Overall health',
    value: store.health?.status ?? 'Unknown',
    detail: store.health ? `${componentAlerts.value.length} components need review right now.` : 'Awaiting runtime telemetry.',
  },
  {
    label: 'Running platforms',
    value: `${store.runningPlatforms}/${store.platforms.length || 0}`,
    detail: `${store.enabledPlatforms} enabled lanes ready to receive traffic.`,
  },
  {
    label: 'Tracked sessions',
    value: `${store.conversations.length}`,
    detail: `${new Set(store.conversations.map((conversation) => conversation.platform)).size} platforms represented in the latest feed.`,
  },
  {
    label: 'Providers',
    value: `${store.providers.length}`,
    detail: `${store.providers.filter((provider) => provider.supportToolCalling).length} tool-ready and ${store.providers.filter((provider) => provider.supportVision).length} vision-ready.`,
  },
  {
    label: 'Trace rows',
    value: `${traceRowCount.value}`,
    detail: 'Effective runtime rows currently available for inspection.',
  },
  {
    label: 'Plugins enabled',
    value: `${store.enabledPlugins}/${store.plugins.plugins.length || 0}`,
    detail: `${store.plugins.extensions.length} extensions currently mapped into the runtime.`,
  },
]);

const validationTone = computed(() => ({
  manifest: runtimeWorkingDirectory.value?.manifestFound ? 'ok' : 'warn',
  runtime: traceRowCount.value > 0 ? 'ok' : 'muted',
  conversations: recentConversations.value.length > 0 ? 'ok' : 'muted',
}));

const validationLabel = computed(() => ({
  manifest: runtimeWorkingDirectory.value?.manifestFound ? 'Ready' : 'Missing',
  runtime: traceRowCount.value > 0 ? 'Trace loaded' : 'No trace',
  conversations: recentConversations.value.length > 0 ? 'Visible' : 'No traffic',
}));

const validationCopy = computed(() => ({
  manifest: runtimeWorkingDirectory.value?.manifestFound
    ? 'Local workspace inputs were discovered and merged into the runtime preview.'
    : 'No runtime workdir manifest is currently contributing to the merged result.',
  runtime: traceRowCount.value > 0
    ? 'Effective runtime data is available to validate before saving config changes.'
    : 'Refresh config surfaces to populate the layered runtime trace.',
  conversations: recentConversations.value.length > 0
    ? 'Recent traffic is available for replay and post-change validation.'
    : 'No recent sessions yet. Use the agent bench when validating changes.',
}));

function toneForStatus(value: string | undefined): Tone {
  const normalized = String(value ?? '').trim().toUpperCase();
  if (!normalized || normalized === 'UNKNOWN') return 'muted';
  if (['UP', 'OK', 'HEALTHY', 'RUNNING', 'ENABLED', 'READY'].some((token) => normalized.includes(token))) return 'ok';
  if (['WARN', 'DEGRADED', 'PARTIAL', 'STARTING', 'MISSING'].some((token) => normalized.includes(token))) return 'warn';
  if (['IDLE', 'PENDING', 'DISABLED', 'STOPPED'].some((token) => normalized.includes(token))) return 'muted';
  return 'error';
}

function recencyTone(value: number): Tone {
  const diff = Date.now() - value;
  if (diff < 30 * 60 * 1000) return 'ok';
  if (diff < 12 * 60 * 60 * 1000) return 'warn';
  return 'muted';
}

function recencyLabel(value: number) {
  const diff = Date.now() - value;
  if (diff < 30 * 60 * 1000) return 'Fresh';
  if (diff < 12 * 60 * 60 * 1000) return 'Warm';
  return 'Stale';
}

function formatTime(value: number) {
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(value);
}

function humanize(value: string) {
  return value.replace(/([A-Z])/g, ' $1').replace(/[-_]/g, ' ').replace(/^./, (first) => first.toUpperCase());
}
</script>

<style scoped>
.overview-page {
  display: grid;
  gap: 24px;
}

.overview-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: end;
}

.overview-hero-copy {
  max-width: 760px;
}

.overview-eyebrow,
.overview-metric-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--weak);
  font-size: 0.72rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.overview-hero h1 {
  margin: 10px 0 12px;
  font-size: clamp(2.4rem, 4vw, 4rem);
  line-height: 1;
  letter-spacing: -0.05em;
  color: var(--text-strong);
}

.overview-hero p {
  margin: 0;
  max-width: 680px;
  color: var(--muted);
  font-size: 1rem;
  line-height: 1.65;
}

.overview-hero-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.overview-metric-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 16px;
}

.overview-metric-card {
  display: grid;
  gap: 10px;
  min-height: 148px;
}

.overview-metric-card strong {
  font-size: 2rem;
  line-height: 1;
  letter-spacing: -0.06em;
  color: var(--text-strong);
}

.overview-metric-card p {
  margin: 0;
  color: var(--muted);
  font-size: 0.92rem;
  line-height: 1.5;
}

.overview-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) 360px;
  gap: 18px;
  align-items: start;
}

.overview-primary,
.overview-secondary {
  display: grid;
  gap: 18px;
}

.overview-incident-list,
.overview-action-grid,
.overview-check-list {
  display: grid;
  gap: 12px;
}

.overview-incident-item,
.overview-action-card {
  display: grid;
  gap: 8px;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: var(--surface-soft);
  color: inherit;
  transition:
    border-color 160ms ease,
    background 160ms ease,
    transform 160ms ease;
}

.overview-incident-item:hover,
.overview-action-card:hover {
  border-color: var(--line-strong);
  background: var(--surface);
  transform: translateY(-1px);
}

.overview-incident-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.overview-incident-action {
  color: var(--weak);
  font-size: 0.8rem;
  font-weight: 600;
}

.overview-incident-item strong,
.overview-action-card strong,
.overview-check-item strong {
  color: var(--text-strong);
  font-size: 0.98rem;
}

.overview-incident-item p,
.overview-action-card small,
.overview-check-item p {
  margin: 0;
  color: var(--muted);
  line-height: 1.5;
}

.overview-check-item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 12px;
  align-items: start;
}

@media (max-width: 1240px) {
  .overview-metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .overview-workbench {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .overview-hero {
    flex-direction: column;
    align-items: stretch;
  }

  .overview-hero-actions {
    justify-content: flex-start;
  }

  .overview-metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .overview-metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
