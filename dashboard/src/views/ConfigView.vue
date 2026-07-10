<template>
  <div class="grid config-layout tactical-config-view">
    <section class="panel command-panel">
      <div class="command-header">
        <div class="command-copy">
          <p class="eyebrow">Changes / Config</p>
          <div class="command-title-row">
            <div>
              <h2>Manage persisted config.</h2>
              <p>{{ configSummary }}</p>
            </div>
            <div class="command-statuses">
              <StatusDot :label="layerControlStatus.label" :tone="layerControlStatus.tone" />
              <StatusDot :label="overridePressure.status" :tone="overridePressure.tone" />
              <StatusDot :label="diagnosticStatus.status" :tone="diagnosticStatus.tone" />
            </div>
          </div>
          <p class="command-brief">
            Keep the persisted layer readable, review higher-order overrides, and validate the effective runtime before rollout.
          </p>
        </div>
        <div class="toolbar command-toolbar">
          <button type="button" @click="resetDraft" :disabled="saving">Reset</button>
          <button type="button" @click="refresh" :disabled="refreshing || saving">
            {{ refreshing ? 'Refreshing' : 'Refresh' }}
          </button>
          <button type="button" class="primary" @click="saveDraft" :disabled="saving">
            {{ saving ? 'Saving' : 'Commit Layer' }}
          </button>
        </div>
      </div>

      <article class="card command-hero">
        <div class="hero-grid">
          <div class="hero-main">
            <p class="eyebrow">Runtime configuration</p>
            <h3>Review the database layer before it reaches production traffic.</h3>
            <p>{{ commandNarrative }}</p>
            <div class="hero-links">
              <RouterLink class="button-link" to="/effective-runtime">Open Effective Runtime</RouterLink>
              <RouterLink class="button-link" to="/working-directory">Open Working Directory</RouterLink>
            </div>
          </div>
          <div class="hero-rail">
            <div class="hero-stat">
              <span>Revision Lock</span>
              <strong>{{ revisionLabel }}</strong>
              <small>{{ revisionSourceLabel }}</small>
            </div>
            <div class="hero-stat">
              <span>Recovery Net</span>
              <strong>{{ recentBackups.length }}</strong>
              <small>{{ latestBackupLabel }}</small>
            </div>
          </div>
        </div>
      </article>

      <div class="grid metric-grid tactical-metrics">
        <article v-for="signal in signalCards" :key="signal.id" class="card tactical-metric" :class="`tone-${signal.tone}`">
          <div class="metric-topline">
            <span>{{ signal.eyebrow }}</span>
            <StatusDot :label="signal.state" :tone="signal.tone" />
          </div>
          <strong>{{ signal.value }}</strong>
          <span>{{ signal.label }}</span>
          <p>{{ signal.detail }}</p>
        </article>
      </div>

      <p v-if="notice" class="notice ok">{{ notice }}</p>
      <p v-if="error" class="notice error">{{ error }}</p>

      <section v-if="databaseDiagnostics.length > 0" class="notice warning diagnostic-callout">
        <strong>Database layer diagnostics</strong>
        <ul>
          <li v-for="item in databaseDiagnostics" :key="item">{{ item }}</li>
        </ul>
      </section>

      <div class="lane-grid">
        <div class="section-column">
          <article class="card config-card section-block">
            <div class="section-heading">
              <div class="section-lead">
                <div class="section-index">A1</div>
                <div class="section-copy">
                  <h3>Agent Core</h3>
                  <p>Shape the default agent settings that this persisted layer hands to the effective runtime.</p>
                </div>
              </div>
              <div class="section-meta">
                <StatusDot :label="agentSectionStatus.label" :tone="agentSectionStatus.tone" />
              </div>
            </div>

            <div class="trace-grid">
              <article v-for="lane in agentSourceLanes" :key="lane.id" class="trace-card" :class="`tone-${lane.tone}`">
                <div class="trace-top">
                  <span>{{ lane.label }}</span>
                  <StatusDot :label="lane.state" :tone="lane.tone" />
                </div>
                <strong>{{ lane.effective }}</strong>
                <p>Saved {{ lane.saved }}</p>
                <small>{{ lane.detail }}</small>
              </article>
            </div>

            <div class="form-grid tactical-form-grid">
              <label>
                <span>Name</span>
                <input v-model="draft.agent.name" type="text" placeholder="Primary runtime agent" />
              </label>
              <label>
                <span>Provider</span>
                <input v-model="draft.agent.providerName" type="text" placeholder="openai-main" />
                <small v-if="traceMap['agent.providerName']" class="field-hint">
                  {{ sourceHint(traceMap['agent.providerName']) }}
                </small>
              </label>
              <label>
                <span>Model</span>
                <input v-model="draft.agent.model" type="text" placeholder="gpt-5" />
              </label>
              <label>
                <span>Max steps</span>
                <input v-model.number="draft.agent.maxSteps" type="number" min="1" />
              </label>
              <label class="full-span">
                <span>Instructions</span>
                <textarea
                  v-model="draft.agent.instructions"
                  class="compact-textarea tactical-textarea"
                  placeholder="Operational guidance for the persisted runtime layer."
                ></textarea>
              </label>
            </div>
          </article>

          <article class="card config-card section-block">
            <div class="section-heading">
              <div class="section-lead">
                <div class="section-index">A2</div>
                <div class="section-copy">
                  <h3>Runtime Policies</h3>
                  <p>Control sub-agent dispatch and service bind settings stored beneath the environment layer.</p>
                </div>
              </div>
              <div class="section-meta">
                <StatusDot :label="runtimePolicyStatus.label" :tone="runtimePolicyStatus.tone" />
              </div>
            </div>

            <div class="trace-grid">
              <article v-for="lane in policySourceLanes" :key="lane.id" class="trace-card" :class="`tone-${lane.tone}`">
                <div class="trace-top">
                  <span>{{ lane.label }}</span>
                  <StatusDot :label="lane.state" :tone="lane.tone" />
                </div>
                <strong>{{ lane.effective }}</strong>
                <p>Saved {{ lane.saved }}</p>
                <small>{{ lane.detail }}</small>
              </article>
            </div>

            <div class="form-grid tactical-form-grid">
              <label>
                <span>Sub-agent routing</span>
                <select v-model="subAgentEnabledString">
                  <option value="true">Enabled</option>
                  <option value="false">Disabled</option>
                </select>
              </label>
              <label>
                <span>Default sub-agent</span>
                <input v-model="draft.subAgents.defaultAgentName" type="text" placeholder="navigator" />
              </label>
              <label>
                <span>Server host</span>
                <input v-model="draft.server.host" type="text" placeholder="0.0.0.0" />
                <small v-if="traceMap['server.host']" class="field-hint">
                  {{ sourceHint(traceMap['server.host']) }}
                </small>
              </label>
              <label>
                <span>Server port</span>
                <input v-model.number="draft.server.port" type="number" min="1" />
                <small v-if="traceMap['server.port']" class="field-hint">
                  {{ sourceHint(traceMap['server.port']) }}
                </small>
              </label>
            </div>
          </article>
        </div>

        <div class="section-column">
          <article class="card config-card section-block">
            <div class="section-heading">
              <div class="section-lead">
                <div class="section-index">B1</div>
                <div class="section-copy">
                  <h3>Provider Mesh</h3>
                  <p>Keep stable provider definitions, models, and endpoints legible before shell overlays take over.</p>
                </div>
              </div>
              <div class="section-meta">
                <StatusDot :label="providerSectionStatus.label" :tone="providerSectionStatus.tone" />
              </div>
            </div>

            <div class="section-banner">
              <strong>{{ enabledProviderCount }}/{{ draft.providers.length || 0 }} provider channels online</strong>
              <p>{{ providerDeckSummary }}</p>
            </div>

            <div class="route-table-wrap tactical-table-wrap">
              <table class="table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Type</th>
                    <th>Model</th>
                    <th>State</th>
                    <th>Endpoint</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="provider in draft.providers" :key="provider.name">
                    <td><strong>{{ provider.name }}</strong></td>
                    <td>{{ provider.type }}</td>
                    <td>{{ provider.model || 'unset' }}</td>
                    <td>
                      <StatusDot :label="provider.enabled ? 'Enabled' : 'Disabled'" :tone="provider.enabled ? 'ok' : 'muted'" />
                    </td>
                    <td>{{ provider.baseUrl || 'default endpoint' }}</td>
                  </tr>
                  <tr v-if="draft.providers.length === 0">
                    <td colspan="5" class="muted">No persisted providers configured.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </article>

          <article class="card config-card section-block">
            <div class="section-heading">
              <div class="section-lead">
                <div class="section-index">B2</div>
                <div class="section-copy">
                  <h3>Environment Locks</h3>
                  <p>Track deployment-owned overrides that can seize authority over the persisted database layer.</p>
                </div>
              </div>
              <div class="section-meta">
                <StatusDot :label="overrideSectionStatus.label" :tone="overrideSectionStatus.tone" />
              </div>
            </div>

            <section class="override-banner" :class="`tone-${overridePressure.tone}`">
              <div>
                <strong>{{ overridePressure.bannerTitle }}</strong>
                <p>{{ overridePressure.detail }}</p>
              </div>
              <StatusDot :label="overridePressure.status" :tone="overridePressure.tone" />
            </section>

            <EmptyState
              v-if="store.environmentOverrides.overrides.length === 0"
              title="No environment overrides"
              detail="The effective runtime currently follows database defaults unless workdir or runtime preview overrides apply."
            />
            <div v-else class="route-table-wrap tactical-table-wrap">
              <table class="table">
                <thead>
                  <tr>
                    <th>Path</th>
                    <th>Env key</th>
                    <th>Exposure</th>
                    <th>Summary</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="override in store.environmentOverrides.overrides" :key="`${override.path}-${override.envKey}`">
                    <td><code>{{ override.path }}</code></td>
                    <td><code>{{ override.envKey }}</code></td>
                    <td>
                      <StatusDot :label="override.sensitive ? 'Sensitive' : 'Visible'" :tone="override.sensitive ? 'warn' : 'muted'" />
                    </td>
                    <td>{{ override.summary }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </article>
        </div>
      </div>
    </section>

    <aside class="panel detail-panel tactical-rail">
      <div class="rail-header">
        <p class="eyebrow">Live Readout</p>
        <div class="section-title rail-title">
          <div>
            <h2>Effective Summary</h2>
            <p>Saved intent, active workdir, and current preview stay visible while editing.</p>
          </div>
        </div>
      </div>

      <article class="card rail-card">
        <div class="section-title compact">
          <div>
            <h3>Operational Snapshot</h3>
            <p>The live configuration rail remains parallel to the draft layer.</p>
          </div>
        </div>
        <div class="detail-list rail-detail-list">
          <div class="detail-item">
            <span>Saved default workdir</span>
            <strong>{{ persistedWorkdirLabel }}</strong>
          </div>
          <div class="detail-item">
            <span>Effective workdir</span>
            <strong>{{ effectiveWorkdirLabel }}</strong>
          </div>
          <div class="detail-item">
            <span>Workdir source</span>
            <strong>{{ workdirSourceLabel }}</strong>
          </div>
          <div class="detail-item">
            <span>Effective provider</span>
            <strong>{{ displayValue(previewConfig?.agent.providerName) }}</strong>
          </div>
          <div class="detail-item">
            <span>Effective agent</span>
            <strong>{{ displayValue(previewConfig?.agent.name) }}</strong>
          </div>
          <div class="detail-item">
            <span>Sub-agent routes</span>
            <strong>{{ draft.subAgents.routes.length }}</strong>
          </div>
          <div class="detail-item">
            <span>Memory max injected</span>
            <strong>{{ store.effectiveRuntimePreview?.memoryPolicy.maxInjectedMemories ?? 0 }}</strong>
          </div>
        </div>
      </article>

      <article class="card rail-card">
        <div class="section-title compact">
          <div>
            <h3>Source Matrix</h3>
            <p>Spot where the live preview diverges from the database layer.</p>
          </div>
        </div>
        <div class="drift-list">
          <div v-for="lane in driftMatrix" :key="lane.id" class="drift-item" :class="`tone-${lane.tone}`">
            <div class="trace-top">
              <span>{{ lane.label }}</span>
              <StatusDot :label="lane.state" :tone="lane.tone" />
            </div>
            <strong>{{ lane.effective }}</strong>
            <p>Saved {{ lane.saved }}</p>
            <small>{{ lane.detail }}</small>
          </div>
        </div>
      </article>

      <section v-if="store.workingDirectory?.diagnostics.length" class="notice warning diagnostic-callout">
        <strong>Working directory diagnostics</strong>
        <ul>
          <li v-for="item in store.workingDirectory?.diagnostics" :key="item">{{ item }}</li>
        </ul>
      </section>

      <section class="panel nested-panel recovery-panel">
        <div class="section-title compact">
          <div>
            <h3>Config Backups</h3>
            <p>{{ backupSummary }}</p>
          </div>
          <button type="button" @click="loadBackups" :disabled="loadingBackups">
            {{ loadingBackups ? 'Refreshing' : 'Refresh' }}
          </button>
        </div>
        <EmptyState
          v-if="store.configBackups.length === 0"
          title="No config backups"
          detail="A backup is created before each database-layer save."
        />
        <div v-else class="backup-list">
          <div v-for="backup in recentBackups" :key="backup.id" class="backup-row">
            <div class="backup-meta">
              <span>{{ formatBackupTime(backup.createdAt) }}</span>
              <strong>{{ backup.id }}</strong>
              <small>{{ formatBytes(backup.sizeBytes) }} · {{ backup.path }}</small>
            </div>
            <button type="button" @click="restoreBackup(backup.id)" :disabled="restoringId !== null">
              {{ restoringId === backup.id ? 'Restoring' : 'Restore' }}
            </button>
          </div>
        </div>
      </section>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import EmptyState from '../components/EmptyState.vue';
import StatusDot from '../components/StatusDot.vue';
import { useDashboardStore } from '../stores/dashboard';
import type { EffectiveValueTraceDto, PriestessConfig } from '../api/dashboard';

type StatusTone = 'ok' | 'warn' | 'muted' | 'error';

interface TacticalSignal {
  id: string;
  eyebrow: string;
  state: string;
  tone: StatusTone;
  value: string;
  label: string;
  detail: string;
}

interface SourceLane {
  id: string;
  label: string;
  saved: string;
  effective: string;
  state: string;
  tone: StatusTone;
  detail: string;
}

const store = useDashboardStore();
const draft = ref<PriestessConfig>(emptyConfig());
const error = ref('');
const notice = ref('');
const saving = ref(false);
const refreshing = ref(false);
const loadingBackups = ref(false);
const restoringId = ref<string | null>(null);

const previewConfig = computed(() => store.effectiveRuntimePreview?.config ?? null);
const traceMap = computed<Record<string, EffectiveValueTraceDto>>(() =>
  Object.fromEntries((store.effectiveRuntimePreview?.trace ?? []).map((item) => [item.path, item])),
);
const revisionLabel = computed(() => {
  const revision = store.databaseConfigLayer?.revision?.revision;
  return revision == null ? 'not saved' : `r${revision}`;
});
const persistedWorkdirLabel = computed(() => draft.value.workingDirectory?.path || 'not set');
const effectiveWorkdirLabel = computed(() => store.workingDirectory?.effectivePath || 'not set');
const workdirSourceLabel = computed(() => store.workingDirectory?.pathSource || 'default');
const enabledProviderCount = computed(() => draft.value.providers.filter((provider) => provider.enabled).length);
const overrideCount = computed(() => store.environmentOverrides.overrides.length);
const recentBackups = computed(() =>
  [...store.configBackups]
    .sort((left, right) => parseTimestamp(right.createdAt) - parseTimestamp(left.createdAt))
    .slice(0, 6),
);
const backupSummary = computed(() => {
  const count = store.configBackups.length;
  if (loadingBackups.value) return 'Refreshing backup metadata.';
  return count === 1 ? '1 backup available.' : `${count} backups available.`;
});
const configSummary = computed(() => {
  const revision = store.databaseConfigLayer?.revision;
  if (!revision) return 'Editing the persisted database layer. Higher layers may still override the effective runtime.';
  return `Editing revision ${revision.revision} saved at ${formatSavedAt(revision.savedAt)}.`;
});
const databaseDiagnostics = computed(() => store.databaseConfigLayer?.diagnostics ?? []);
const revisionSourceLabel = computed(() => {
  const source = store.databaseConfigLayer?.revision?.source;
  return source ? `Source ${source}` : 'Persisted write surface';
});
const latestBackupLabel = computed(() => {
  const backup = recentBackups.value[0];
  return backup ? `Latest ${formatBackupTime(backup.createdAt)}` : 'No snapshots cached';
});
const layerControlStatus = computed(() => {
  const revision = store.databaseConfigLayer?.revision;
  if (!revision) {
    return {
      label: 'Awaiting save',
      tone: 'muted' as StatusTone,
      detail: 'Persist a revision to pin this layer as the primary write surface.',
    };
  }
  return {
    label: 'Revision locked',
    tone: 'ok' as StatusTone,
    detail: `Saved through ${revision.source || 'database'} at ${formatSavedAt(revision.savedAt)}.`,
  };
});
const overridePressure = computed(() => {
  const count = overrideCount.value;
  if (count === 0) {
    return {
      status: 'Uncontested',
      tone: 'ok' as StatusTone,
      detail: 'No environment locks are currently superseding the database layer.',
      bannerTitle: 'Database layer currently leads the stack.',
    };
  }
  return {
    status: count === 1 ? '1 lock active' : `${count} locks active`,
    tone: 'warn' as StatusTone,
    detail:
      count === 1
        ? 'One environment variable currently overrides part of the live runtime.'
        : `${count} environment overrides are reshaping live values above the database layer.`,
    bannerTitle: 'Higher-order locks are actively steering the live runtime.',
  };
});
const providerRegistryStatus = computed(() => {
  const total = draft.value.providers.length;
  const enabled = enabledProviderCount.value;
  if (total === 0) {
    return {
      state: 'Empty',
      tone: 'muted' as StatusTone,
      detail: 'No provider entries are persisted in this layer yet.',
    };
  }
  if (enabled === 0) {
    return {
      state: 'Offline',
      tone: 'warn' as StatusTone,
      detail: 'All persisted providers are disabled, so live routing will lean on higher layers or fallbacks.',
    };
  }
  return {
    state: 'Online',
    tone: 'ok' as StatusTone,
    detail: `${enabled} of ${total} persisted provider${total === 1 ? ' is' : 's are'} enabled for routing.`,
  };
});
const routingStatus = computed(() => {
  if (!draft.value.subAgents.enabled) {
    return {
      state: 'Standby',
      tone: 'muted' as StatusTone,
      value: 'Standby',
      detail: 'Sub-agent routing is disabled in the persisted layer.',
    };
  }
  const routeCount = draft.value.subAgents.routes.length;
  return {
    state: routeCount === 0 ? 'Ready' : `${routeCount} routes`,
    tone: 'ok' as StatusTone,
    value: String(routeCount),
    detail:
      routeCount === 0
        ? 'Routing is enabled and waiting for route definitions.'
        : `${routeCount} route rule${routeCount === 1 ? '' : 's'} are armed for handoff.`,
  };
});
const diagnosticStatus = computed(() => {
  const total = databaseDiagnostics.value.length + (store.workingDirectory?.diagnostics.length ?? 0);
  if (total === 0) {
    return {
      status: 'Quiet watch',
      tone: 'ok' as StatusTone,
      detail: 'No active diagnostics are raised across config and workdir surfaces.',
    };
  }
  return {
    status: total === 1 ? '1 alert' : `${total} alerts`,
    tone: (store.workingDirectory?.valid === false ? 'error' : 'warn') as StatusTone,
    detail: 'Diagnostics are present on the saved or effective surface and should be reviewed before rollout.',
  };
});
const agentSectionStatus = computed(() => {
  if (!draft.value.agent.providerName && !draft.value.agent.model && !draft.value.agent.name) {
    return { label: 'Drafting', tone: 'muted' as StatusTone };
  }
  if (traceMap.value['agent.providerName']?.overriddenBy) {
    return { label: 'Overlayed', tone: 'warn' as StatusTone };
  }
  return { label: 'Ready', tone: 'ok' as StatusTone };
});
const runtimePolicyStatus = computed(() =>
  draft.value.subAgents.enabled
    ? { label: 'Routes armed', tone: 'ok' as StatusTone }
    : { label: 'Manual', tone: 'muted' as StatusTone },
);
const providerSectionStatus = computed(() => ({
  label: providerRegistryStatus.value.state,
  tone: providerRegistryStatus.value.tone,
}));
const overrideSectionStatus = computed(() => ({
  label: overridePressure.value.status,
  tone: overridePressure.value.tone,
}));
const providerDeckSummary = computed(() => {
  const total = draft.value.providers.length;
  if (total === 0) {
    return 'Seed provider definitions here to give the runtime a stable baseline before shell overlays engage.';
  }
  const enabled = enabledProviderCount.value;
  const disabled = total - enabled;
  return `${enabled} enabled, ${disabled} idle. Models and endpoints stay visible here even when higher layers override the live route.`;
});
const commandNarrative = computed(() => {
  const providerLine =
    draft.value.providers.length === 0
      ? 'Provider mesh has not been seeded in the database layer yet.'
      : `${enabledProviderCount.value} of ${draft.value.providers.length} persisted provider channels are online.`;
  const overrideLine =
    overrideCount.value === 0
      ? 'No environment locks are contesting the live runtime.'
      : `${overrideCount.value} environment lock${overrideCount.value === 1 ? ' is' : 's are'} currently contesting live values.`;
  const routeLine = draft.value.subAgents.enabled
    ? `${draft.value.subAgents.routes.length} route rule${draft.value.subAgents.routes.length === 1 ? '' : 's'} are prepared for sub-agent dispatch.`
    : 'Sub-agent routing is parked in manual control.';
  return `${providerLine} ${overrideLine} ${routeLine}`;
});
const signalCards = computed<TacticalSignal[]>(() => [
  {
    id: 'revision',
    eyebrow: 'Command Revision',
    state: layerControlStatus.value.label,
    tone: layerControlStatus.value.tone,
    value: revisionLabel.value,
    label: 'database layer',
    detail: layerControlStatus.value.detail,
  },
  {
    id: 'provider-mesh',
    eyebrow: 'Provider Mesh',
    state: providerRegistryStatus.value.state,
    tone: providerRegistryStatus.value.tone,
    value: draft.value.providers.length === 0 ? '0' : `${enabledProviderCount.value}/${draft.value.providers.length}`,
    label: 'providers enabled',
    detail: providerRegistryStatus.value.detail,
  },
  {
    id: 'routing-grid',
    eyebrow: 'Routing Grid',
    state: routingStatus.value.state,
    tone: routingStatus.value.tone,
    value: routingStatus.value.value,
    label: 'sub-agent posture',
    detail: routingStatus.value.detail,
  },
  {
    id: 'override-pressure',
    eyebrow: 'Override Pressure',
    state: overridePressure.value.status,
    tone: overridePressure.value.tone,
    value: String(overrideCount.value),
    label: 'environment locks',
    detail: overridePressure.value.detail,
  },
]);
const agentSourceLanes = computed<SourceLane[]>(() => [
  makeSourceLane(
    'provider-route',
    'Provider route',
    draft.value.agent.providerName,
    previewConfig.value?.agent.providerName,
    traceMap.value['agent.providerName'],
  ),
  makeSourceLane('model-rail', 'Model rail', draft.value.agent.model, previewConfig.value?.agent.model, traceMap.value['agent.model']),
  makeSourceLane('agent-name', 'Agent identity', draft.value.agent.name, previewConfig.value?.agent.name, traceMap.value['agent.name']),
]);
const policySourceLanes = computed<SourceLane[]>(() => [
  makeSourceLane(
    'server-bind',
    'Server bind',
    formatBindLabel(draft.value.server.host, draft.value.server.port),
    formatBindLabel(previewConfig.value?.server.host, previewConfig.value?.server.port),
    traceMap.value['server.host'] ?? traceMap.value['server.port'],
  ),
  makeSourceLane(
    'subagent-default',
    'Default sub-agent',
    draft.value.subAgents.defaultAgentName,
    previewConfig.value?.subAgents.defaultAgentName,
    traceMap.value['subAgents.defaultAgentName'],
  ),
  makeSourceLane(
    'route-mode',
    'Routing mode',
    draft.value.subAgents.enabled ? 'Enabled' : 'Disabled',
    previewConfig.value?.subAgents.enabled ? 'Enabled' : 'Disabled',
    traceMap.value['subAgents.enabled'],
  ),
]);
const driftMatrix = computed<SourceLane[]>(() => {
  const workdirLane = makeSourceLane(
    'workdir-pointer',
    'Workdir pointer',
    persistedWorkdirLabel.value,
    effectiveWorkdirLabel.value,
  );
  if (workdirSourceLabel.value !== 'default') {
    workdirLane.state = 'Redirected';
    workdirLane.tone = 'warn';
    workdirLane.detail = `Effective workdir currently comes from ${workdirSourceLabel.value}.`;
  }

  return [
    makeSourceLane(
      'provider-live',
      'Effective provider',
      draft.value.agent.providerName,
      previewConfig.value?.agent.providerName,
      traceMap.value['agent.providerName'],
    ),
    makeSourceLane(
      'server-live',
      'Effective server bind',
      formatBindLabel(draft.value.server.host, draft.value.server.port),
      formatBindLabel(previewConfig.value?.server.host, previewConfig.value?.server.port),
      traceMap.value['server.host'] ?? traceMap.value['server.port'],
    ),
    makeSourceLane(
      'route-live',
      'Live route captain',
      draft.value.subAgents.defaultAgentName,
      previewConfig.value?.subAgents.defaultAgentName,
      traceMap.value['subAgents.defaultAgentName'],
    ),
    workdirLane,
  ];
});

const subAgentEnabledString = computed({
  get: () => String(draft.value.subAgents.enabled),
  set: (value: string) => {
    draft.value.subAgents.enabled = value === 'true';
  },
});

function emptyConfig(): PriestessConfig {
  return {
    platforms: [],
    providers: [],
    agent: {
      name: '',
      instructions: '',
      model: '',
      providerName: '',
      maxSteps: 6,
      temperature: 0.7,
      compressStrategy: 'token_window',
      maxRounds: 20,
      maxTokens: 4096,
      toolTimeoutSeconds: 30,
      enabledTools: [],
      disabledTools: [],
      allowedRiskLevels: ['SAFE_READ', 'SESSION_ACTION', 'EXTERNAL_READ', 'STATE_WRITE', 'HIGH_RISK'],
    },
    database: { path: '' },
    pipeline: {},
    server: {
      enabled: true,
      host: '',
      port: 0,
      corsEnabled: false,
      configWatchEnabled: false,
      configWatchIntervalMillis: 0,
    },
    plugins: {
      enabled: true,
      directory: '',
      autoDiscover: false,
    },
    subAgents: {
      enabled: false,
      defaultAgentName: '',
      agents: [],
      routes: [],
    },
    workingDirectory: { path: '' },
    workspaces: [],
  };
}

function cloneConfig(config: PriestessConfig): PriestessConfig {
  return JSON.parse(JSON.stringify(config)) as PriestessConfig;
}

function displayValue(value: unknown, fallback = 'unset') {
  if (value == null) return fallback;
  if (typeof value === 'string') return value.trim() || fallback;
  return String(value);
}

function resetDraft() {
  draft.value = cloneConfig(store.databaseConfigLayer?.config ?? emptyConfig());
  error.value = '';
  notice.value = '';
}

async function refresh() {
  refreshing.value = true;
  error.value = '';
  notice.value = '';
  try {
    await store.refreshConfigSurfaces();
    resetDraft();
    notice.value = 'Config surfaces refreshed inline.';
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : String(cause);
  } finally {
    refreshing.value = false;
  }
}

async function saveDraft() {
  saving.value = true;
  error.value = '';
  notice.value = '';
  try {
    await store.saveDatabaseConfigLayer(cloneConfig(draft.value));
    resetDraft();
    notice.value = 'Database-layer config saved. Effective preview refreshed.';
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : String(cause);
  } finally {
    saving.value = false;
  }
}

async function loadBackups() {
  loadingBackups.value = true;
  try {
    await store.loadConfigBackups();
  } finally {
    loadingBackups.value = false;
  }
}

async function restoreBackup(id: string) {
  restoringId.value = id;
  error.value = '';
  notice.value = '';
  try {
    await store.restoreConfigBackup(id);
    await store.refreshConfigSurfaces();
    resetDraft();
    notice.value = `Restored backup ${id}.`;
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : String(cause);
  } finally {
    restoringId.value = null;
  }
}

function sourceHint(trace?: EffectiveValueTraceDto) {
  if (!trace) return '';
  if (trace.overriddenBy) {
    return `Saved here, but the current effective value is overridden by ${trace.overriddenBy}.`;
  }
  if (trace.source === 'workdir') {
    return 'The current effective value comes from the selected working directory.';
  }
  return `The current effective value comes from ${trace.source}.`;
}

function formatSavedAt(value: number) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(value);
}

function formatBindLabel(host?: string | null, port?: number | null) {
  const normalizedHost = typeof host === 'string' ? host.trim() : '';
  if (!normalizedHost && (!port || port <= 0)) return 'unset';
  return `${normalizedHost || '0.0.0.0'}:${port && port > 0 ? port : 'port unset'}`;
}

function parseTimestamp(value: string) {
  const timestamp = Date.parse(value);
  return Number.isNaN(timestamp) ? 0 : timestamp;
}

function formatBytes(size: number) {
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

function formatBackupTime(value: string) {
  const timestamp = parseTimestamp(value);
  if (timestamp === 0) return value;
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(timestamp);
}

function makeSourceLane(
  id: string,
  label: string,
  saved: unknown,
  effective: unknown,
  trace?: EffectiveValueTraceDto,
): SourceLane {
  const savedLabel = displayValue(saved);
  const effectiveLabel = displayValue(effective);

  if (savedLabel === 'unset' && effectiveLabel === 'unset') {
    return {
      id,
      label,
      saved: savedLabel,
      effective: effectiveLabel,
      state: 'Unassigned',
      tone: 'muted',
      detail: 'No value is persisted at this layer yet.',
    };
  }

  if (trace?.overriddenBy) {
    return {
      id,
      label,
      saved: savedLabel,
      effective: effectiveLabel,
      state: 'Overridden',
      tone: 'warn',
      detail: sourceHint(trace),
    };
  }

  if (savedLabel === effectiveLabel) {
    return {
      id,
      label,
      saved: savedLabel,
      effective: effectiveLabel,
      state: 'Aligned',
      tone: 'ok',
      detail: trace ? sourceHint(trace) || 'Saved and effective values are aligned.' : 'Saved and effective values are aligned.',
    };
  }

  return {
    id,
    label,
    saved: savedLabel,
    effective: effectiveLabel,
    state: 'Live drift',
    tone: 'muted',
    detail: 'Live preview differs from the saved layer.',
  };
}

watch(
  () => store.databaseConfigLayer?.config,
  () => resetDraft(),
);

onMounted(async () => {
  if (!store.databaseConfigLayer || !store.workingDirectory || !store.effectiveRuntimePreview) {
    await store.refreshConfigSurfaces();
  }
  resetDraft();
  await loadBackups();
});
</script>

<style scoped>
.tactical-config-view {
  align-items: start;
}

.config-layout {
  grid-template-columns: minmax(0, 1.08fr) 360px;
  gap: 14px;
}

.command-panel,
.tactical-rail,
.command-hero,
.section-block,
.rail-card,
.recovery-panel {
  position: relative;
  overflow: hidden;
}

.command-panel {
  border-color: #d6dde7;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(248, 250, 252, 0.98) 52%, rgba(245, 247, 250, 0.98) 100%);
  box-shadow: 0 18px 44px rgba(97, 111, 126, 0.09);
}

.command-panel::before,
.tactical-rail::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(30, 74, 133, 0.06) 0, rgba(30, 74, 133, 0) 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0) 0, rgba(208, 215, 224, 0.22) 100%);
  pointer-events: none;
}

.command-header,
.command-hero,
.section-block,
.rail-card,
.recovery-panel,
.diagnostic-callout {
  z-index: 1;
}

.command-header {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  margin-bottom: 16px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #6b7480;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.command-title-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.command-title-row h2 {
  margin: 0;
  font-size: 28px;
  line-height: 1.05;
  letter-spacing: 0.02em;
}

.command-title-row p {
  margin-top: 6px;
}

.command-statuses,
.section-meta,
.hero-links {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.command-statuses {
  justify-content: flex-end;
}

.command-brief {
  margin: 10px 0 0;
  max-width: 720px;
  color: #4d5966;
  font-size: 14px;
  line-height: 1.6;
}

.command-toolbar {
  align-self: start;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.command-toolbar :deep(button.primary) {
  min-width: 126px;
}

.command-hero {
  margin-bottom: 14px;
  border-color: #d8dee8;
  border-radius: 16px;
  background:
    linear-gradient(135deg, rgba(232, 239, 248, 0.82) 0%, rgba(255, 255, 255, 0.92) 42%, rgba(245, 247, 250, 0.96) 100%);
}

.command-hero::after {
  content: '';
  position: absolute;
  inset: 14px;
  border: 1px solid rgba(142, 154, 170, 0.18);
  border-radius: 12px;
  pointer-events: none;
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 18px;
  align-items: stretch;
}

.hero-main {
  display: grid;
  gap: 10px;
}

.hero-main h3 {
  margin: 0;
  font-size: 24px;
  line-height: 1.15;
  letter-spacing: 0.01em;
}

.hero-main p:last-of-type {
  margin: 0;
  font-size: 14px;
  line-height: 1.65;
  color: #4d5966;
}

.hero-rail {
  display: grid;
  gap: 10px;
}

.hero-stat,
.section-banner,
.override-banner,
.trace-card,
.drift-item,
.backup-row {
  border: 1px solid #d7dde6;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.8);
}

.hero-stat {
  display: grid;
  gap: 6px;
  padding: 14px;
}

.hero-stat span,
.trace-top span,
.backup-meta span {
  color: #707987;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero-stat strong {
  color: #16202d;
  font-size: 24px;
  line-height: 1.05;
}

.hero-stat small,
.section-banner p,
.override-banner p,
.trace-card small,
.drift-item small,
.backup-meta small {
  color: #566272;
  line-height: 1.5;
}

.tactical-metrics {
  margin-bottom: 14px;
  gap: 12px;
}

.tactical-metric {
  display: grid;
  gap: 8px;
  min-height: 0;
  border-radius: 14px;
  border-top-width: 3px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94) 0%, rgba(248, 250, 252, 0.96) 100%);
}

.tactical-metric strong {
  font-size: 28px;
  line-height: 1.05;
}

.tactical-metric span:last-of-type {
  display: block;
  margin-top: -2px;
  color: #69717d;
  font-size: 13px;
}

.tactical-metric p {
  margin: 0;
  color: #54606d;
  font-size: 12px;
  line-height: 1.55;
}

.metric-topline,
.trace-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: flex-start;
}

.tone-ok {
  border-color: #c7dfcc;
}

.tone-ok.tactical-metric,
.tone-ok.trace-card,
.tone-ok.drift-item,
.tone-ok.override-banner {
  border-top-color: #2f7d42;
}

.tone-warn {
  border-color: #ead6aa;
}

.tone-warn.tactical-metric,
.tone-warn.trace-card,
.tone-warn.drift-item,
.tone-warn.override-banner {
  border-top-color: #b67b12;
}

.tone-muted {
  border-color: #d6dde6;
}

.tone-muted.tactical-metric,
.tone-muted.trace-card,
.tone-muted.drift-item,
.tone-muted.override-banner {
  border-top-color: #7c8796;
}

.tone-error {
  border-color: #efcbc5;
}

.tone-error.tactical-metric,
.tone-error.trace-card,
.tone-error.drift-item,
.tone-error.override-banner {
  border-top-color: #cf4e44;
}

.lane-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.04fr) minmax(320px, 0.96fr);
  gap: 14px;
}

.section-column {
  display: grid;
  gap: 14px;
}

.config-card {
  display: grid;
  gap: 14px;
}

.section-block {
  border-radius: 16px;
  border-color: #d8dee8;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(249, 250, 252, 0.96) 100%);
}

.section-heading {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
}

.section-lead {
  display: flex;
  gap: 12px;
}

.section-index {
  display: grid;
  place-items: center;
  width: 44px;
  min-width: 44px;
  height: 44px;
  border: 1px solid #ced6e0;
  border-radius: 13px;
  background: linear-gradient(180deg, #ffffff 0%, #eef2f6 100%);
  color: #485669;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.16em;
}

.section-copy h3 {
  margin: 0;
  font-size: 18px;
}

.section-copy p {
  margin: 5px 0 0;
  max-width: 560px;
}

.section-banner {
  padding: 14px;
}

.section-banner strong,
.trace-card strong,
.drift-item strong {
  display: block;
  color: #17202b;
  line-height: 1.25;
}

.section-banner strong {
  font-size: 15px;
  margin-bottom: 4px;
}

.section-banner p {
  margin: 0;
  font-size: 13px;
}

.trace-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.trace-card,
.drift-item {
  display: grid;
  gap: 7px;
  padding: 12px;
  border-top-width: 3px;
}

.trace-card strong,
.drift-item strong {
  font-size: 15px;
  overflow-wrap: anywhere;
}

.trace-card p,
.drift-item p {
  margin: 0;
  color: #66717e;
  font-size: 12px;
}

.tactical-form-grid label {
  padding: 10px;
  border: 1px solid #e4e8ee;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.72);
}

.tactical-form-grid label span {
  color: #55606d;
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.tactical-form-grid :deep(input),
.tactical-form-grid :deep(select),
.tactical-form-grid :deep(textarea) {
  border-color: #d5dce6;
  background: rgba(255, 255, 255, 0.94);
}

.tactical-textarea {
  min-height: 120px;
}

.tactical-table-wrap {
  border-radius: 14px;
  border-color: #d8dfe8;
  background: rgba(255, 255, 255, 0.72);
}

.tactical-table-wrap :deep(.table) {
  min-width: 640px;
  background: transparent;
}

.tactical-table-wrap :deep(.table th) {
  color: #5d6672;
  letter-spacing: 0.08em;
}

.tactical-table-wrap :deep(.table td) {
  background: transparent;
}

.override-banner {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  padding: 14px;
  border-top-width: 3px;
}

.override-banner strong {
  display: block;
  margin-bottom: 4px;
  color: #17202b;
}

.tactical-rail {
  border-color: #d6dde7;
  background:
    linear-gradient(180deg, rgba(253, 254, 255, 0.98) 0%, rgba(246, 248, 251, 0.98) 100%);
}

.rail-header {
  position: relative;
  z-index: 1;
  margin-bottom: 12px;
}

.rail-title {
  margin-bottom: 0;
}

.rail-card {
  z-index: 1;
  display: grid;
  gap: 12px;
  margin-bottom: 12px;
  border-radius: 16px;
  border-color: #d8dee8;
  background: rgba(255, 255, 255, 0.88);
}

.rail-detail-list {
  gap: 12px;
}

.drift-list,
.backup-list {
  display: grid;
  gap: 10px;
}

.backup-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  padding: 12px;
}

.backup-meta {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.backup-meta strong {
  color: #17202b;
  font-size: 14px;
  overflow-wrap: anywhere;
}

.backup-meta small {
  overflow-wrap: anywhere;
}

.nested-panel {
  margin-top: 14px;
  border-style: dashed;
  border-color: #d7dde6;
  background: rgba(255, 255, 255, 0.86);
}

.field-hint {
  color: #67717d;
  font-size: 11px;
  line-height: 1.5;
}

.diagnostic-callout {
  position: relative;
  z-index: 1;
}

.diagnostic-callout :deep(ul) {
  margin: 8px 0 0;
  padding-left: 18px;
}

.diagnostic-callout :deep(li + li) {
  margin-top: 4px;
}

.command-panel :deep(.empty-state),
.tactical-rail :deep(.empty-state) {
  border-radius: 12px;
  border-color: #d8dfe8;
  background: rgba(248, 250, 252, 0.88);
}

.command-panel :deep(.button-link),
.command-panel :deep(button),
.tactical-rail :deep(button) {
  box-shadow: none;
}

.command-panel :deep(.button-link:hover),
.command-panel :deep(button:hover),
.tactical-rail :deep(button:hover) {
  border-color: #bcc7d4;
}

.command-panel :deep(code),
.tactical-rail :deep(code) {
  padding: 1px 4px;
  border-radius: 5px;
  background: rgba(233, 238, 245, 0.9);
}

.tactical-config-view .command-panel,
.tactical-config-view .tactical-rail,
.tactical-config-view .command-hero,
.tactical-config-view .section-block,
.tactical-config-view .rail-card,
.tactical-config-view .recovery-panel {
  border-color: var(--line);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 10px 30px rgba(9, 9, 11, 0.04);
}

.tactical-config-view .command-panel::before,
.tactical-config-view .tactical-rail::before,
.tactical-config-view .command-hero::after {
  display: none;
}

.tactical-config-view .eyebrow,
.tactical-config-view .hero-stat span,
.tactical-config-view .trace-top span,
.tactical-config-view .backup-meta span,
.tactical-config-view .tactical-form-grid label span {
  color: var(--weak);
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.tactical-config-view .command-title-row h2,
.tactical-config-view .hero-main h3,
.tactical-config-view .section-copy h3 {
  color: var(--text-strong);
  letter-spacing: -0.03em;
  text-transform: none;
}

.tactical-config-view .command-brief,
.tactical-config-view .hero-main p:last-of-type,
.tactical-config-view .section-copy p,
.tactical-config-view .hero-stat small,
.tactical-config-view .section-banner p,
.tactical-config-view .trace-card small,
.tactical-config-view .drift-item small {
  color: var(--muted);
}

.tactical-config-view .tactical-metric,
.tactical-config-view .trace-card,
.tactical-config-view .drift-item,
.tactical-config-view .override-banner,
.tactical-config-view .hero-stat,
.tactical-config-view .section-banner,
.tactical-config-view .backup-row {
  border-color: var(--line);
  background: rgba(250, 250, 250, 0.92);
}

.tactical-config-view .tactical-form-grid label {
  border-color: var(--line);
  background: rgba(250, 250, 250, 0.78);
}

@media (max-width: 1180px) {
  .hero-grid,
  .lane-grid {
    grid-template-columns: 1fr;
  }

  .trace-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 980px) {
  .config-layout {
    grid-template-columns: 1fr;
  }

  .command-header,
  .command-title-row,
  .section-heading,
  .override-banner,
  .backup-row {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .command-statuses,
  .section-meta {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .command-title-row h2 {
    font-size: 24px;
  }

  .hero-main h3 {
    font-size: 21px;
  }

  .tactical-metrics,
  .compact-metrics {
    grid-template-columns: 1fr;
  }

  .command-toolbar :deep(button.primary) {
    min-width: 0;
  }
}
</style>
