<template>
  <div class="log-command">
    <section class="panel log-hero">
      <div class="log-hero-grid">
        <div class="log-copy">
          <div class="log-band">
            <span>Troubleshooting</span>
            <span>Logs</span>
          </div>

          <h2>Follow the live event stream.</h2>
          <p>
            Track runtime traffic, warnings, and execution faults from one clean event stream. The
            feed stays clipped to the latest two hundred events so scan speed stays high.
          </p>

          <div class="grid log-signal-grid">
            <article v-for="signal in logSignals" :key="signal.label" class="card log-signal" :class="`tone-${signal.tone}`">
              <span>{{ signal.label }}</span>
              <strong>{{ signal.value }}</strong>
              <p>{{ signal.detail }}</p>
            </article>
          </div>
        </div>

        <aside class="log-rail">
          <article class="card log-rail-card">
            <div class="section-title compact">
              <div>
                <h3>Stream posture</h3>
                <p>{{ connectionSummary }}</p>
              </div>
            </div>

            <div class="rail-stack">
              <div class="rail-item">
                <span>Socket state</span>
                <strong>{{ logStore.connected ? 'Connected' : 'Disconnected' }}</strong>
              </div>
              <div class="rail-item">
                <span>Latest marker</span>
                <strong>{{ latestEvent ? formatTime(latestEvent.timestamp) : 'Awaiting feed' }}</strong>
              </div>
              <div class="rail-item">
                <span>Error posture</span>
                <strong>{{ latestErrorCount > 0 ? `${latestErrorCount} alert event(s)` : 'No active alerts in buffer' }}</strong>
              </div>
            </div>
          </article>
        </aside>
      </div>
    </section>

    <div class="workbench-grid wide-detail">
      <section class="panel ledger-panel">
        <div class="section-title">
          <div>
            <h2>Runtime stream</h2>
            <p>{{ logStore.connected ? 'Socket linked and accepting live runtime markers.' : 'Connect to receive runtime markers from the backend socket.' }}</p>
          </div>
          <div class="toolbar">
            <button type="button" class="primary" @click="logStore.connect()">Connect</button>
            <button type="button" @click="logStore.clear()">Clear</button>
          </div>
        </div>

        <section v-if="logStore.error" class="notice error">{{ logStore.error }}</section>

        <EmptyState
          v-if="logStore.events.length === 0"
          title="No log events"
          detail="Connect to the log socket to receive runtime events."
        />

        <div v-else class="log-ledger">
          <article v-for="event in logStore.events" :key="`${event.timestamp}-${event.level}-${event.message}`" class="log-entry" :class="`tone-${levelTone(event.level)}`">
            <div class="log-entry-head">
              <div class="log-entry-main">
                <span class="inline-status" :class="levelTone(event.level)">
                  {{ formatLevel(event.level) }}
                </span>
                <strong>{{ event.message }}</strong>
              </div>

              <div class="log-entry-meta">
                <span>{{ formatTime(event.timestamp) }}</span>
                <small>{{ formatRelative(event.timestamp) }}</small>
              </div>
            </div>
          </article>
        </div>
      </section>

      <aside class="panel detail-panel feed-rail">
        <div class="section-title">
          <div>
            <h2>Level sweep</h2>
            <p>Recent event distribution across the live buffer.</p>
          </div>
        </div>

        <div class="detail-stack">
          <article class="card log-rail-card">
            <div class="detail-list">
              <div v-for="row in levelRows" :key="row.label" class="detail-item">
                <span>{{ row.label }}</span>
                <strong>{{ row.value }}</strong>
              </div>
            </div>
          </article>

          <article class="card log-rail-card">
            <div class="section-title compact">
              <div>
                <h3>Latest event</h3>
                <p>Most recent marker in the local ledger.</p>
              </div>
            </div>

            <EmptyState
              v-if="!latestEvent"
              title="No latest event"
              detail="The side rail fills in after the first socket message arrives."
            />

            <div v-else class="detail-list">
              <div class="detail-item">
                <span>Level</span>
                <strong>{{ formatLevel(latestEvent.level) }}</strong>
              </div>
              <div class="detail-item">
                <span>Timestamp</span>
                <strong>{{ formatTime(latestEvent.timestamp) }}</strong>
              </div>
              <div class="detail-item">
                <span>Message</span>
                <strong>{{ latestEvent.message }}</strong>
              </div>
            </div>
          </article>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
import EmptyState from '../components/EmptyState.vue';
import { useLogStore } from '../stores/logs';

const logStore = useLogStore();

const latestEvent = computed(() => logStore.events[0] ?? null);
const latestErrorCount = computed(() => logStore.events.filter((event) => levelTone(event.level) === 'danger').length);
const latestWarnCount = computed(() => logStore.events.filter((event) => levelTone(event.level) === 'warn').length);

const logSignals = computed(() => [
  {
    label: 'Buffered',
    value: String(logStore.events.length),
    detail: 'Latest runtime events retained in the in-memory ledger.',
    tone: 'muted',
  },
  {
    label: 'Alerts',
    value: String(latestErrorCount.value),
    detail: latestErrorCount.value > 0 ? 'Fault markers need operator review.' : 'No fault markers in the current buffer.',
    tone: latestErrorCount.value > 0 ? 'danger' : 'ok',
  },
  {
    label: 'Warnings',
    value: String(latestWarnCount.value),
    detail: latestWarnCount.value > 0 ? 'Monitor degradations before they escalate.' : 'No warning posture in the current feed.',
    tone: latestWarnCount.value > 0 ? 'warn' : 'ok',
  },
  {
    label: 'Socket',
    value: logStore.connected ? 'Linked' : 'Idle',
    detail: logStore.connected ? 'Live runtime markers are flowing into the ledger.' : 'Reconnect the feed to restore live observation.',
    tone: logStore.connected ? 'ok' : 'warn',
  },
]);

const levelRows = computed(() => [
  {
    label: 'Info',
    value: String(logStore.events.filter((event) => normalizeLevel(event.level) === 'INFO').length),
  },
  {
    label: 'Warn',
    value: String(logStore.events.filter((event) => normalizeLevel(event.level) === 'WARN').length),
  },
  {
    label: 'Error',
    value: String(logStore.events.filter((event) => normalizeLevel(event.level) === 'ERROR').length),
  },
  {
    label: 'Other',
    value: String(
      logStore.events.filter((event) => !['INFO', 'WARN', 'ERROR'].includes(normalizeLevel(event.level))).length,
    ),
  },
]);

const connectionSummary = computed(() => {
  if (logStore.connected) {
    return 'Live socket linked. Keep the ledger clear enough to spot execution drift at a glance.';
  }
  return 'Socket is idle. Connect to resume the live runtime watch.';
});

function normalizeLevel(level: string) {
  return String(level).trim().toUpperCase();
}

function levelTone(level: string) {
  const normalized = normalizeLevel(level);
  if (normalized === 'ERROR') return 'danger';
  if (normalized === 'WARN' || normalized === 'WARNING') return 'warn';
  if (normalized === 'INFO') return 'ok';
  return 'muted';
}

function formatLevel(level: string) {
  return normalizeLevel(level);
}

function formatTime(value: number) {
  return new Intl.DateTimeFormat(undefined, {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(value);
}

function formatRelative(value: number) {
  const diff = Math.max(0, Date.now() - value);
  const seconds = Math.floor(diff / 1000);
  if (seconds < 5) return 'just now';
  if (seconds < 60) return `${seconds}s ago`;
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  return `${Math.floor(hours / 24)}d ago`;
}

onMounted(() => logStore.connect());
</script>

<style scoped>
.log-command {
  display: grid;
  gap: 14px;
}

.log-hero {
  border-color: #d7cebd;
  background:
    linear-gradient(135deg, rgba(255, 252, 246, 0.98) 0%, rgba(247, 241, 231, 0.98) 55%, rgba(240, 246, 248, 0.98) 100%);
}

.log-hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.18fr) minmax(280px, 0.82fr);
  gap: 14px;
}

.log-band {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.log-band span,
.log-signal span,
.rail-item span {
  color: #887152;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.log-band span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid rgba(182, 159, 111, 0.34);
  background: rgba(255, 251, 245, 0.92);
}

.log-copy h2 {
  margin: 0;
  color: #18304c;
  font-size: clamp(28px, 2vw + 18px, 40px);
  line-height: 0.98;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.log-copy > p {
  margin: 12px 0 0;
  max-width: 70ch;
  color: #5b6675;
  line-height: 1.66;
}

.log-signal-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 16px;
}

.log-signal {
  display: grid;
  gap: 8px;
  border-top: 3px solid #98a2b0;
  background: rgba(255, 252, 247, 0.9);
}

.log-signal.tone-ok {
  border-top-color: #4c8661;
}

.log-signal.tone-warn {
  border-top-color: #bb8524;
}

.log-signal.tone-danger {
  border-top-color: #c15b50;
}

.log-signal strong {
  color: #17304d;
  font-size: 30px;
  line-height: 1;
}

.log-signal p {
  margin: 0;
  color: #606a79;
  font-size: 12px;
  line-height: 1.58;
}

.log-rail,
.detail-stack {
  display: grid;
  gap: 12px;
}

.log-rail-card {
  border-color: #ddd4c5;
  background: rgba(255, 252, 246, 0.92);
}

.rail-stack {
  display: grid;
  gap: 10px;
}

.rail-item {
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid #e3dacb;
  background: rgba(255, 251, 245, 0.92);
}

.rail-item strong {
  color: #19314d;
  font-size: 14px;
  line-height: 1.46;
}

.ledger-panel,
.feed-rail {
  border-color: #ddd3c3;
  background: linear-gradient(180deg, rgba(255, 252, 247, 0.96) 0%, rgba(248, 243, 234, 0.94) 100%);
}

.log-ledger {
  display: grid;
  gap: 10px;
}

.log-entry {
  display: grid;
  gap: 10px;
  padding: 14px;
  border: 1px solid #e2d8ca;
  border-left-width: 4px;
  background: rgba(255, 252, 247, 0.92);
}

.log-entry.tone-ok {
  border-left-color: #4c8661;
}

.log-entry.tone-warn {
  border-left-color: #bb8524;
}

.log-entry.tone-danger {
  border-left-color: #c15b50;
}

.log-entry.tone-muted {
  border-left-color: #98a2b0;
}

.log-entry-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.log-entry-main {
  display: grid;
  gap: 8px;
}

.log-entry-main strong {
  color: #182f4b;
  line-height: 1.55;
}

.log-entry-meta {
  display: grid;
  gap: 4px;
  justify-items: end;
  color: #6e7784;
  font-size: 12px;
  text-align: right;
}

@media (max-width: 1180px) {
  .log-hero-grid,
  .workbench-grid.wide-detail {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .log-signal-grid {
    grid-template-columns: 1fr;
  }

  .log-entry-head {
    flex-direction: column;
  }

  .log-entry-meta {
    justify-items: start;
    text-align: left;
  }
}
</style>
