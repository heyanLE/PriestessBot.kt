<template>
  <div class="overview-command">
    <section class="panel command-deck">
      <div class="command-shell">
        <div class="command-band">
          <span class="band-label">Priestess / Day Shift</span>
          <span class="band-index">Hermes Command Shell</span>
        </div>

        <div class="command-head">
          <div class="command-copy">
            <p class="command-kicker">Tactical Overview</p>
            <h1>Daylight command board</h1>
            <p>
              Priestess core aligned to Hermes shell order. Review runtime stability, relay
              coverage, and live session movement from one industrial daytime surface.
            </p>
          </div>

          <div class="command-side">
            <div class="command-identity">
              <span class="command-identity-mark">
                <img src="/assets/priestess-icon.jpg" alt="Priestess tactical crest" />
              </span>
              <div class="command-identity-copy">
                <span>Priestess Core</span>
                <strong>Day protocol active</strong>
                <p>Light-theme command doctrine with Hermes shell discipline and industrial signal framing.</p>
              </div>
            </div>

            <div class="command-tag-list">
              <span class="command-tag">Runtime Watch</span>
              <span class="command-tag">Relay Order</span>
              <span class="command-tag">Escalation Ready</span>
            </div>

            <figure class="command-persona">
              <figcaption>
                <span>Visual Anchor</span>
                <strong>Sidebar persona engaged</strong>
              </figcaption>
            </figure>

            <div class="command-status-block">
              <span class="inline-status" :class="healthTone">
                <span class="status-dot"></span>
                {{ healthStatus }}
              </span>
              <strong>{{ commandHeadline }}</strong>
              <p>{{ commandDetail }}</p>
            </div>
          </div>
        </div>

        <div class="command-meta">
          <div class="meta-cell">
            <span>Last sync</span>
            <strong>{{ lastPulseLabel }}</strong>
          </div>
          <div class="meta-cell">
            <span>Uptime</span>
            <strong>{{ formatDuration(store.health?.uptimeMillis ?? 0) }}</strong>
          </div>
          <div class="meta-cell">
            <span>Nominal sectors</span>
            <strong>{{ healthyComponentCount }}/{{ componentRows.length }}</strong>
          </div>
          <div class="meta-cell">
            <span>Live sessions</span>
            <strong>{{ store.conversations.length }}</strong>
          </div>
        </div>

        <div class="command-rail">
          <RouterLink class="button-link primary" to="/conversations">Open Live Sessions</RouterLink>
          <RouterLink class="button-link" to="/effective-runtime">Inspect Runtime</RouterLink>
          <RouterLink class="button-link" to="/config">Review Config</RouterLink>
        </div>
      </div>
    </section>

    <section class="grid tactical-grid">
      <article
        v-for="card in tacticalCards"
        :key="card.code"
        class="panel tactical-card"
        :class="`tone-${card.tone}`"
      >
        <span class="card-code">{{ card.code }}</span>
        <div class="card-row">
          <div>
            <p class="card-label">{{ card.label }}</p>
            <strong>{{ card.value }}</strong>
          </div>
          <span class="inline-status" :class="card.tone">{{ card.badge }}</span>
        </div>
        <p class="card-detail">{{ card.detail }}</p>
        <RouterLink class="card-link" :to="card.to">Open {{ card.linkLabel }}</RouterLink>
      </article>
    </section>

    <div class="overview-grid">
      <section class="panel sector-panel">
        <div class="section-title">
          <div>
            <p class="section-kicker">Sector Map</p>
            <h2>Component readiness</h2>
            <p>
              {{ healthyComponentCount }}/{{ componentRows.length }} sectors nominal.
              {{ attentionComponentCount > 0 ? `${attentionComponentCount} sectors require review.` : 'No watchpoints in the latest snapshot.' }}
            </p>
          </div>
          <span class="inline-status" :class="healthTone">{{ healthBadge }}</span>
        </div>

        <EmptyState
          v-if="componentRows.length === 0"
          title="No component report"
          detail="Health snapshots appear after the local API responds."
        />
        <div v-else class="sector-list">
          <article
            v-for="row in componentRows"
            :key="row.name"
            class="sector-row"
            :class="`tone-${row.tone}`"
          >
            <div class="sector-main">
              <div class="sector-label">
                <strong>{{ row.name }}</strong>
                <span>{{ row.signal }}</span>
              </div>
              <StatusDot :label="row.status" :tone="row.tone" />
            </div>
            <div class="sector-bar">
              <span :style="{ width: `${row.coverage}%` }"></span>
            </div>
          </article>
        </div>
      </section>

      <aside class="overview-side">
        <section class="panel pulse-panel">
          <div class="section-title compact">
            <div>
              <p class="section-kicker">Pulse</p>
              <h2>Runtime pulse</h2>
              <p>{{ pulseCaption }}</p>
            </div>
          </div>

          <div class="pulse-stack">
            <div class="pulse-item">
              <span>Conversations</span>
              <strong>{{ store.conversations.length }}</strong>
              <small>{{ trackedConversationPlatforms }} platforms with live traces</small>
            </div>
            <div class="pulse-item">
              <span>Workspaces</span>
              <strong>{{ store.workspaces.workspaces.length }}</strong>
              <small>{{ enabledWorkspaceCount }} enabled routing cells</small>
            </div>
            <div class="pulse-item">
              <span>Diagnostics</span>
              <strong>{{ diagnosticEntries.length }}</strong>
              <small>{{ diagnosticEntries.length > 0 ? 'Runtime markers available for inspection' : 'No extra trace markers reported' }}</small>
            </div>
          </div>
        </section>

        <section class="panel directive-panel">
          <div class="section-title compact">
            <div>
              <p class="section-kicker">Directives</p>
              <h2>Operator lanes</h2>
              <p>Jump into the densest control surfaces without leaving the board.</p>
            </div>
          </div>

          <nav class="directive-list">
            <RouterLink
              v-for="link in directiveLinks"
              :key="link.code"
              :to="link.to"
              class="directive-link"
            >
              <span class="directive-code">{{ link.code }}</span>
              <span class="directive-copy">
                <strong>{{ link.title }}</strong>
                <span>{{ link.detail }}</span>
              </span>
              <span class="directive-value">{{ link.value }}</span>
            </RouterLink>
          </nav>
        </section>
      </aside>
    </div>

    <div class="workbench-grid tactical-workbench">
      <section class="panel diagnostics-panel">
        <div class="section-title">
          <div>
            <p class="section-kicker">Trace</p>
            <h2>Diagnostics ledger</h2>
            <p>
              {{ diagnosticEntries.length > 0 ? 'Active runtime markers from the latest telemetry pass.' : 'Awaiting runtime diagnostics from the local API.' }}
            </p>
          </div>
          <RouterLink class="button-link" to="/effective-runtime">Open Effective Runtime</RouterLink>
        </div>

        <EmptyState
          v-if="diagnosticEntries.length === 0"
          title="No diagnostics reported"
          detail="Additional runtime details will appear once the health endpoint returns telemetry."
        />
        <div v-else class="detail-list diagnostics-list">
          <div
            v-for="([key, value]) in diagnosticEntries"
            :key="key"
            class="detail-item diagnostic-item"
          >
            <span>{{ formatKey(String(key)) }}</span>
            <code>{{ value }}</code>
          </div>
        </div>
      </section>

      <aside class="panel detail-panel session-panel">
        <div class="section-title">
          <div>
            <p class="section-kicker">Watch Log</p>
            <h2>Recent conversations</h2>
            <p>{{ store.conversations.length }} tracked sessions in the current feed.</p>
          </div>
          <RouterLink class="button-link" to="/conversations">Open</RouterLink>
        </div>

        <EmptyState
          v-if="recentConversations.length === 0"
          title="No conversations yet"
          detail="Messages will appear after a platform starts receiving traffic."
        />
        <div v-else class="table-wrap mission-table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>Platform</th>
                <th>Session</th>
                <th>Updated</th>
                <th>Age</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="conversation in recentConversations" :key="conversation.id">
                <td>{{ conversation.platform }}</td>
                <td>
                  <RouterLink class="session-link" :to="`/conversations/${conversation.id}`">
                    <code>{{ conversation.sessionId }}</code>
                  </RouterLink>
                </td>
                <td>{{ formatTime(conversation.updatedAt) }}</td>
                <td>{{ formatRecency(conversation.updatedAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink } from 'vue-router';
import EmptyState from '../components/EmptyState.vue';
import StatusDot from '../components/StatusDot.vue';
import { useDashboardStore } from '../stores/dashboard';

type StatusTone = 'ok' | 'warn' | 'muted' | 'error';

const store = useDashboardStore();

const toneRank: Record<StatusTone, number> = {
  error: 0,
  warn: 1,
  muted: 2,
  ok: 3,
};

const healthStatus = computed(() => store.health?.status ?? 'UNKNOWN');
const healthTone = computed<StatusTone>(() => resolveTone(healthStatus.value));
const healthBadge = computed(() => toneLabel(healthTone.value));
const lastPulseAt = computed(() => store.lastUpdated ?? store.health?.timestamp ?? null);
const lastPulseLabel = computed(() => (lastPulseAt.value ? formatTime(lastPulseAt.value) : 'Awaiting sync'));

const componentRows = computed(() =>
  Object.entries(store.health?.components ?? {})
    .map(([name, status]) => {
      const tone = resolveTone(status);
      return {
        name,
        status,
        tone,
        signal: signalCopy(tone),
        coverage: tone === 'ok' ? 100 : tone === 'warn' ? 64 : tone === 'muted' ? 42 : 24,
      };
    })
    .sort((left, right) => toneRank[left.tone] - toneRank[right.tone] || left.name.localeCompare(right.name)),
);

const diagnosticEntries = computed(() => Object.entries(store.health?.diagnostics ?? {}));
const recentConversations = computed(() =>
  [...store.conversations].sort((left, right) => right.updatedAt - left.updatedAt).slice(0, 6),
);
const healthyComponentCount = computed(() => componentRows.value.filter((row) => row.tone === 'ok').length);
const attentionComponentCount = computed(() => componentRows.value.filter((row) => row.tone !== 'ok').length);
const enabledWorkspaceCount = computed(() => store.workspaces.workspaces.filter((workspace) => workspace.enabled).length);
const trackedConversationPlatforms = computed(() => new Set(store.conversations.map((conversation) => conversation.platform)).size);
const toolReadyProviderCount = computed(() => store.providers.filter((provider) => provider.supportToolCalling).length);
const visionReadyProviderCount = computed(() => store.providers.filter((provider) => provider.supportVision).length);

const commandHeadline = computed(() => {
  if (!store.health) return 'Awaiting runtime telemetry';
  if (healthTone.value === 'ok' && attentionComponentCount.value === 0) return 'All sectors nominal';
  if (attentionComponentCount.value > 0) {
    return `${attentionComponentCount.value} sector${attentionComponentCount.value === 1 ? '' : 's'} need review`;
  }
  return 'Telemetry linked';
});

const commandDetail = computed(() => {
  if (!store.health) return 'The board will populate after the local API responds.';
  return `${store.runningPlatforms}/${store.platforms.length} relays online, ${store.conversations.length} tracked conversations, ${diagnosticEntries.value.length} diagnostic markers.`;
});

const pulseCaption = computed(() => {
  if (!store.health) return 'Waiting for the first stable heartbeat from the runtime.';
  if (attentionComponentCount.value === 0) return 'Runtime steady. All reported sectors remain within nominal bands.';
  return 'Runtime is live with active watchpoints. Use the ledger and operator lanes for deeper inspection.';
});

const tacticalCards = computed(() => [
  {
    code: 'SYS-01',
    label: 'Runtime sanctum',
    value: healthStatus.value,
    badge: healthBadge.value,
    detail: store.health
      ? `Uptime ${formatDuration(store.health.uptimeMillis)} across ${componentRows.value.length} reported sectors.`
      : 'Awaiting local API telemetry.',
    tone: healthTone.value,
    to: '/effective-runtime',
    linkLabel: 'Runtime',
  },
  {
    code: 'PLT-02',
    label: 'Relay coverage',
    value: `${store.runningPlatforms}/${store.platforms.length}`,
    badge: relayBadge(store.runningPlatforms, store.platforms.length),
    detail: `${store.enabledPlatforms} enabled lanes prepared for incoming traffic.`,
    tone: relayTone(store.runningPlatforms, store.platforms.length),
    to: '/platforms',
    linkLabel: 'Platforms',
  },
  {
    code: 'PRV-03',
    label: 'Provider matrix',
    value: `${store.providers.length}`,
    badge: `${toolReadyProviderCount.value} tool-ready`,
    detail: `${visionReadyProviderCount.value} vision-ready providers within the current registry.`,
    tone: providerTone(store.providers.length, toolReadyProviderCount.value, visionReadyProviderCount.value),
    to: '/providers',
    linkLabel: 'Providers',
  },
  {
    code: 'PLG-04',
    label: 'Plugin doctrine',
    value: `${store.enabledPlugins}/${store.plugins.plugins.length}`,
    badge: `${store.plugins.extensions.length} ext`,
    detail: `${store.plugins.plugins.length} plugin packages mapped into the current shell.`,
    tone: pluginTone(store.enabledPlugins, store.plugins.plugins.length),
    to: '/plugins',
    linkLabel: 'Plugins',
  },
]);

const directiveLinks = computed(() => [
  {
    code: 'WS',
    title: 'Workspace diagnostics',
    detail: `${enabledWorkspaceCount.value}/${store.workspaces.workspaces.length} routing cells enabled`,
    value: `${store.workspaces.workspaces.length}`,
    to: '/workspaces',
  },
  {
    code: 'CN',
    title: 'Conversation watch',
    detail: `${store.conversations.length} live sessions under observation`,
    value: `${trackedConversationPlatforms.value} ch`,
    to: '/conversations',
  },
  {
    code: 'RT',
    title: 'Effective runtime',
    detail: `${diagnosticEntries.value.length} trace markers available`,
    value: diagnosticEntries.value.length > 0 ? 'Trace' : 'Open',
    to: '/effective-runtime',
  },
  {
    code: 'CF',
    title: 'Config surface',
    detail: `${store.platforms.length + store.providers.length} runtime endpoints cataloged`,
    value: 'Review',
    to: '/config',
  },
]);

function resolveTone(value: string | undefined): StatusTone {
  const normalized = String(value ?? '').trim().toUpperCase();
  if (!normalized || normalized === 'UNKNOWN') return 'muted';
  if (['UP', 'OK', 'HEALTHY', 'RUNNING', 'ENABLED', 'READY'].some((token) => normalized.includes(token))) return 'ok';
  if (['WARN', 'DEGRADED', 'PARTIAL', 'STARTING'].some((token) => normalized.includes(token))) return 'warn';
  if (['IDLE', 'PENDING', 'DISABLED', 'STOPPED'].some((token) => normalized.includes(token))) return 'muted';
  return 'error';
}

function signalCopy(tone: StatusTone) {
  if (tone === 'ok') return 'Nominal alignment';
  if (tone === 'warn') return 'Observe drift';
  if (tone === 'muted') return 'Standby telemetry';
  return 'Escalate review';
}

function toneLabel(tone: StatusTone) {
  if (tone === 'ok') return 'Nominal';
  if (tone === 'warn') return 'Monitor';
  if (tone === 'muted') return 'Standby';
  return 'Alert';
}

function relayTone(running: number, total: number): StatusTone {
  if (total === 0) return 'muted';
  if (running === total) return 'ok';
  if (running > 0) return 'warn';
  return 'error';
}

function relayBadge(running: number, total: number) {
  if (total === 0) return 'Standby';
  if (running === total) return 'Full relay';
  if (running > 0) return 'Partial relay';
  return 'Offline';
}

function providerTone(total: number, toolReady: number, visionReady: number): StatusTone {
  if (total === 0) return 'muted';
  if (toolReady === total && visionReady > 0) return 'ok';
  if (toolReady > 0 || visionReady > 0) return 'warn';
  return 'error';
}

function pluginTone(enabled: number, total: number): StatusTone {
  if (total === 0) return 'muted';
  if (enabled === total) return 'ok';
  if (enabled > 0) return 'warn';
  return 'error';
}

function formatTime(value: number) {
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(value);
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

function formatRecency(value: number) {
  const diff = Math.max(0, Date.now() - value);
  const minutes = Math.floor(diff / 60000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}
</script>

<style scoped>
.overview-command {
  display: grid;
  gap: 14px;
}

.overview-command :deep(.inline-status) {
  border-color: #ddd2c0;
  background: #f6f0e3;
  color: #516072;
}

.overview-command :deep(.inline-status.ok) {
  border-color: #b6d7c2;
  background: #edf7ef;
  color: #1e6d40;
}

.overview-command :deep(.inline-status.warn) {
  border-color: #ebcb90;
  background: #fff6e2;
  color: #996208;
}

.overview-command :deep(.inline-status.error) {
  border-color: #e8b6b0;
  background: #fff0ee;
  color: #ae4237;
}

.overview-command :deep(.inline-status.muted) {
  border-color: #ddd2c0;
  background: #f5f1e8;
  color: #677181;
}

.overview-command :deep(.empty-state) {
  border-color: #e3d8c8;
  background: #fffdf8;
}

.command-deck {
  padding: 0;
  overflow: hidden;
  border-color: #daccb8;
  background:
    linear-gradient(90deg, rgba(25, 58, 100, 0.06) 0, rgba(25, 58, 100, 0.06) 1px, transparent 1px, transparent 24px),
    linear-gradient(180deg, #fffdf7 0%, #f4efe2 100%);
}

.command-shell {
  position: relative;
  padding: 20px;
}

.command-shell::after {
  content: '';
  position: absolute;
  inset: auto 20px 20px auto;
  width: 112px;
  height: 112px;
  border: 1px solid rgba(28, 52, 80, 0.16);
  opacity: 0.4;
  pointer-events: none;
}

.command-band {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
  color: #8d7558;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.band-label,
.band-index {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid rgba(191, 173, 140, 0.55);
  background: rgba(255, 250, 240, 0.92);
}

.command-head {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(280px, 0.92fr);
  gap: 18px;
  align-items: start;
}

.command-side {
  display: grid;
  gap: 12px;
}

.command-copy {
  display: grid;
  align-content: start;
}

.command-identity {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  padding: 14px;
  border: 1px solid #d9ccb9;
  background: linear-gradient(180deg, rgba(255, 252, 246, 0.98), rgba(246, 240, 229, 0.96));
}

.command-identity-mark {
  display: grid;
  place-items: center;
  width: 72px;
  height: 72px;
  overflow: hidden;
  border: 1px solid rgba(31, 58, 92, 0.18);
  border-radius: 18px;
  background: rgba(255, 251, 245, 0.94);
  box-shadow: 0 10px 22px rgba(83, 67, 40, 0.14);
}

.command-identity-mark img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.command-identity-copy {
  display: grid;
  gap: 4px;
}

.command-identity-copy span,
.command-tag {
  color: #8f7657;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.command-identity-copy strong {
  color: #17314c;
  font-size: 18px;
  line-height: 1.1;
  text-transform: uppercase;
}

.command-identity-copy p {
  margin: 0;
  color: #5d6775;
  font-size: 12px;
  line-height: 1.6;
}

.command-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.command-tag {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid rgba(191, 173, 140, 0.55);
  background: rgba(255, 250, 240, 0.9);
}

.command-copy h1,
.section-title h2,
.card-row strong,
.meta-cell strong,
.pulse-item strong {
  font-family: "Bahnschrift", "DIN Alternate", "Segoe UI", "PingFang SC", sans-serif;
}

.command-kicker,
.section-kicker,
.card-label,
.card-code {
  margin: 0;
  color: #8f7657;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.command-copy h1 {
  margin: 0;
  color: #172f4d;
  font-size: clamp(32px, 4vw, 46px);
  line-height: 0.94;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.command-copy p:last-child {
  max-width: 720px;
  margin: 12px 0 0;
  color: #566070;
  font-size: 14px;
  line-height: 1.7;
}

.command-status-block {
  display: grid;
  gap: 8px;
  padding: 16px;
  border: 1px solid #d9ccb9;
  background: linear-gradient(180deg, rgba(255, 250, 241, 0.96), rgba(249, 245, 236, 0.96));
}

.command-persona {
  position: relative;
  margin: 0;
  display: grid;
  gap: 0;
  min-height: 0;
  padding: 14px;
  overflow: hidden;
  border: 1px solid #d9ccb9;
  background: linear-gradient(180deg, rgba(240, 246, 243, 0.92) 0%, rgba(230, 239, 234, 0.92) 100%);
}

.command-persona::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(30, 76, 130, 0.06) 0, rgba(30, 76, 130, 0.06) 1px, transparent 1px, transparent 18px),
    linear-gradient(180deg, rgba(30, 76, 130, 0.04) 0, rgba(30, 76, 130, 0.04) 1px, transparent 1px, transparent 18px);
  opacity: 0.7;
}

.command-persona figcaption {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 4px;
}

.command-persona figcaption span {
  color: #567866;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.command-persona figcaption strong {
  color: #17314c;
  font-size: 16px;
  line-height: 1.1;
  text-transform: uppercase;
}

.command-status-block strong {
  color: #172f4d;
  font-size: 22px;
  line-height: 1.1;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.command-status-block p {
  margin: 0;
  color: #5e6979;
  font-size: 13px;
  line-height: 1.6;
}

.command-meta {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 18px;
}

.meta-cell {
  display: grid;
  gap: 5px;
  padding: 12px;
  border: 1px solid #dfd3c1;
  background: rgba(255, 252, 246, 0.92);
}

.meta-cell span,
.pulse-item span {
  color: #748193;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.meta-cell strong {
  color: #1d324d;
  font-size: 18px;
  line-height: 1.2;
}

.command-rail {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.command-rail :deep(.button-link) {
  border-color: #d2c5b2;
  background: rgba(255, 251, 244, 0.96);
}

.command-rail :deep(.button-link.primary) {
  border-color: #1d4f87;
  background: #1d4f87;
}

.tactical-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.tactical-card {
  position: relative;
  display: grid;
  gap: 12px;
  min-height: 170px;
  overflow: hidden;
  border-color: #ded2bf;
  background: linear-gradient(180deg, #fffdfa 0%, #f8f2e7 100%);
}

.tactical-card::after {
  content: '';
  position: absolute;
  inset: 0 0 auto auto;
  width: 72px;
  height: 72px;
  border-left: 1px solid rgba(23, 47, 77, 0.12);
  border-bottom: 1px solid rgba(23, 47, 77, 0.12);
  opacity: 0.7;
}

.tactical-card.tone-ok {
  border-top: 3px solid #4e8e68;
}

.tactical-card.tone-warn {
  border-top: 3px solid #d1a24e;
}

.tactical-card.tone-muted {
  border-top: 3px solid #98a3b2;
}

.tactical-card.tone-error {
  border-top: 3px solid #c96b62;
}

.card-row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
}

.card-row strong {
  display: block;
  margin-top: 8px;
  color: #182f4d;
  font-size: 30px;
  line-height: 1;
  text-transform: uppercase;
  overflow-wrap: anywhere;
}

.card-detail {
  margin: 0;
  color: #5d6775;
  font-size: 13px;
  line-height: 1.65;
}

.card-link,
.session-link {
  color: #174d87;
}

.card-link {
  margin-top: auto;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) 336px;
  gap: 14px;
  align-items: start;
}

.sector-panel,
.pulse-panel,
.directive-panel,
.diagnostics-panel,
.session-panel {
  border-color: #ded3c1;
  background: linear-gradient(180deg, #fffdfa 0%, #f8f4eb 100%);
}

.section-title p {
  max-width: 640px;
}

.sector-list {
  display: grid;
  gap: 10px;
}

.sector-row {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid #e5dac9;
  background: rgba(255, 252, 246, 0.94);
}

.sector-row.tone-ok {
  border-left: 3px solid #4e8e68;
}

.sector-row.tone-warn {
  border-left: 3px solid #d1a24e;
}

.sector-row.tone-muted {
  border-left: 3px solid #97a1af;
}

.sector-row.tone-error {
  border-left: 3px solid #c96b62;
}

.sector-main {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.sector-label strong,
.directive-copy strong {
  display: block;
  color: #1d324d;
  font-size: 14px;
  line-height: 1.3;
}

.sector-label span,
.directive-copy span,
.pulse-item small {
  color: #667180;
  font-size: 12px;
  line-height: 1.5;
}

.sector-bar {
  height: 7px;
  overflow: hidden;
  background: #e8e0d3;
}

.sector-bar span {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #d0a55d 0%, #1b4c82 100%);
}

.overview-side {
  display: grid;
  gap: 14px;
}

.pulse-stack,
.directive-list {
  display: grid;
  gap: 10px;
}

.pulse-item {
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid #e4d8c7;
  background: rgba(255, 252, 247, 0.95);
}

.pulse-item strong {
  color: #172f4d;
  font-size: 28px;
  line-height: 1;
}

.directive-link {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border: 1px solid #ddd1be;
  background: rgba(255, 252, 247, 0.95);
  color: inherit;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.directive-link:hover {
  border-color: #bfa27b;
  transform: translateY(-1px);
}

.directive-code {
  color: #a28157;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.directive-value {
  color: #1d4f87;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.tactical-workbench {
  gap: 14px;
}

.diagnostics-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.diagnostic-item {
  min-height: 88px;
  padding: 12px;
  border: 1px solid #e4d8c8;
  background: rgba(255, 252, 246, 0.96);
}

.diagnostic-item code {
  font-size: 12px;
  line-height: 1.6;
}

.mission-table-wrap {
  border-color: #e0d4c3;
}

.session-panel :deep(.table) {
  min-width: 0;
  background: transparent;
}

.session-panel :deep(.table th),
.session-panel :deep(.table td) {
  border-bottom-color: #e6dccd;
  background: rgba(255, 252, 247, 0.6);
}

@media (max-width: 1180px) {
  .tactical-grid,
  .command-meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .overview-grid,
  .tactical-workbench {
    grid-template-columns: 1fr;
  }

  .overview-side {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .session-panel {
    position: static;
  }
}

@media (max-width: 900px) {
  .command-head,
  .overview-side,
  .diagnostics-list {
    grid-template-columns: 1fr;
  }

  .command-shell {
    padding: 16px;
  }

  .command-copy h1 {
    font-size: 30px;
  }

  .command-identity {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .tactical-grid,
  .command-meta {
    grid-template-columns: 1fr;
  }

  .command-band,
  .command-rail {
    flex-direction: column;
    align-items: stretch;
  }

  .directive-link {
    grid-template-columns: 1fr;
  }
}
</style>
