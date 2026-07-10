<template>
  <div class="runtime-command">
    <section class="panel runtime-hero">
      <div class="runtime-hero-grid">
        <div class="runtime-copy">
          <div class="runtime-band">
            <span>Changes / Effective Runtime</span>
            <span>Resolved configuration</span>
          </div>

          <h2>Effective runtime preview</h2>
          <p>{{ previewSummary }}</p>

          <div class="grid runtime-signal-grid">
            <article v-for="signal in runtimeSignals" :key="signal.label" class="card runtime-signal" :class="`tone-${signal.tone}`">
              <span>{{ signal.label }}</span>
              <strong>{{ signal.value }}</strong>
              <p>{{ signal.detail }}</p>
            </article>
          </div>
        </div>

        <aside class="runtime-rail">
          <article class="card runtime-rail-card">
            <div class="section-title compact">
              <div>
              <h3>What matters here</h3>
                <p>Result first, source explicit, overrides impossible to miss.</p>
              </div>
            </div>

            <div class="rail-list">
              <div class="rail-item">
                <span>Trace rows</span>
                <strong>{{ traceRows.length }} effective value row(s)</strong>
              </div>
              <div class="rail-item">
                <span>Override count</span>
                <strong>{{ overriddenCount }} row(s) superseded by higher layers</strong>
              </div>
              <div class="rail-item">
                <span>Diagnostics</span>
                <strong>{{ diagnostics.length > 0 ? `${diagnostics.length} marker(s) available` : 'No preview diagnostics' }}</strong>
              </div>
            </div>
          </article>
        </aside>
      </div>
    </section>

    <div class="workbench-grid wide-detail">
      <section class="panel runtime-main">
        <div class="section-title">
          <div>
            <h2>Scenario controls</h2>
            <p>Preview-only overrides let us inspect the merged result without writing to persistence.</p>
          </div>
          <div class="toolbar">
            <button type="button" @click="resetOverrides" :disabled="previewLoading">Reset</button>
            <button type="button" @click="refreshPreview" :disabled="previewLoading">
              {{ previewLoading ? 'Refreshing' : 'Refresh' }}
            </button>
            <button type="button" class="primary" @click="applyPreview" :disabled="previewLoading">
              {{ previewLoading ? 'Previewing' : 'Preview' }}
            </button>
          </div>
        </div>

        <p v-if="notice" class="notice ok">{{ notice }}</p>
        <p v-if="error" class="notice error">{{ error }}</p>

        <article class="card preview-controls">
          <div class="section-title compact">
            <div>
              <h3>Preview scenario</h3>
              <p>Only affects the inspection pass below. Saved config remains untouched.</p>
            </div>
          </div>

          <div class="form-grid">
            <label class="full-span">
              <span>Override workdir path</span>
              <input v-model="workdirOverride" type="text" placeholder="Optional runtime workdir override" />
            </label>
            <label>
              <span>Override provider</span>
              <input v-model="providerOverride" type="text" placeholder="Optional provider name" />
            </label>
            <label>
              <span>Override memory max injected</span>
              <input v-model.number="maxInjectedOverride" type="number" min="0" placeholder="Optional" />
            </label>
          </div>
        </article>

        <article class="card trace-matrix-card">
          <div class="section-title compact">
            <div>
              <h3>Source trace</h3>
              <p>Why each final value won in the current runtime preview.</p>
            </div>
          </div>

          <div class="table-wrap trace-table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>Path</th>
                  <th>Final value</th>
                  <th>Source</th>
                  <th>Overridden by</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="trace in traceRows" :key="trace.path">
                  <td><code>{{ trace.path }}</code></td>
                  <td>{{ trace.summary }}</td>
                  <td>
                    <span class="inline-status" :class="sourceTone(trace.source)">{{ trace.source }}</span>
                  </td>
                  <td>{{ trace.overriddenBy || 'none' }}</td>
                </tr>
                <tr v-if="traceRows.length === 0">
                  <td colspan="4" class="muted">No effective trace loaded yet.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <section v-if="diagnostics.length" class="notice warning">
          <strong>Preview diagnostics</strong>
          <ul>
            <li v-for="item in diagnostics" :key="item">{{ item }}</li>
          </ul>
        </section>
      </section>

      <aside class="panel detail-panel runtime-detail-rail">
        <div class="section-title">
          <div>
            <h2>Runtime summary</h2>
            <p>Result-first readout of the current effective preview.</p>
          </div>
        </div>

        <div class="detail-stack">
          <article class="card runtime-rail-card">
            <div class="detail-list">
              <div class="detail-item">
                <span>Effective provider</span>
                <strong>{{ previewConfig?.agent.providerName || 'unset' }}</strong>
              </div>
              <div class="detail-item">
                <span>Effective agent</span>
                <strong>{{ previewConfig?.agent.name || 'unset' }}</strong>
              </div>
              <div class="detail-item">
                <span>Configured workdir</span>
                <strong>{{ preview?.workingDirectory.configuredPath || 'not set' }}</strong>
              </div>
              <div class="detail-item">
                <span>Effective workdir</span>
                <strong>{{ preview?.workingDirectory.effectivePath || 'not set' }}</strong>
              </div>
              <div class="detail-item">
                <span>Workdir source</span>
                <strong>{{ preview?.workingDirectory.pathSource || 'default' }}</strong>
              </div>
              <div class="detail-item">
                <span>Manifest</span>
                <strong>{{ preview?.workingDirectory.manifestFound ? 'found' : 'missing' }}</strong>
              </div>
              <div class="detail-item">
                <span>Skills detected</span>
                <strong>{{ preview?.workingDirectory.skills.length ?? 0 }}</strong>
              </div>
              <div class="detail-item">
                <span>Agents detected</span>
                <strong>{{ preview?.workingDirectory.agents.length ?? 0 }}</strong>
              </div>
            </div>
          </article>

          <article class="card runtime-rail-card">
            <div class="section-title compact">
              <div>
                <h3>Source sweep</h3>
                <p>Current trace distribution across merge layers.</p>
              </div>
            </div>

            <div class="source-list">
              <div v-for="row in sourceRows" :key="row.label" class="source-row">
                <span>{{ row.label }}</span>
                <strong>{{ row.value }}</strong>
              </div>
            </div>
          </article>

          <section class="panel nested-panel">
            <div class="section-title compact">
              <div>
                <h3>Next steps</h3>
                <p>Jump back to source layers from the effective result.</p>
              </div>
            </div>
            <div class="toolbar vertical-toolbar">
              <RouterLink class="button-link" to="/config">Open Config</RouterLink>
              <RouterLink class="button-link" to="/working-directory">Open Working Directory</RouterLink>
            </div>
          </section>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { useDashboardStore } from '../stores/dashboard';
import type { EffectiveRuntimePreviewRequest } from '../api/dashboard';

const store = useDashboardStore();
const previewLoading = ref(false);
const notice = ref('');
const error = ref('');
const workdirOverride = ref('');
const providerOverride = ref('');
const maxInjectedOverride = ref<number | null>(null);

const preview = computed(() => store.effectiveRuntimePreview);
const previewConfig = computed(() => preview.value?.config ?? null);
const traceRows = computed(() => preview.value?.trace ?? []);
const diagnostics = computed(() => preview.value?.diagnostics ?? []);
const overriddenCount = computed(() => traceRows.value.filter((trace) => Boolean(trace.overriddenBy)).length);

const previewSummary = computed(() => {
  if (!preview.value) return 'Load the effective runtime preview to explain the final parameters.';
  return 'This board explains the final runtime values after database config, workdir, environment, and preview overrides merge into one result.';
});

const runtimeSignals = computed(() => [
  {
    label: 'Provider',
    value: previewConfig.value?.agent.providerName || 'unset',
    detail: 'Current effective provider in the preview result.',
    tone: previewConfig.value?.agent.providerName ? 'ok' : 'muted',
  },
  {
    label: 'Agent',
    value: previewConfig.value?.agent.name || 'unset',
    detail: 'Resolved runtime agent after all layers merge.',
    tone: previewConfig.value?.agent.name ? 'ok' : 'muted',
  },
  {
    label: 'Workdir',
    value: preview.value?.workingDirectory.effectivePath || 'not set',
    detail: `${preview.value?.workingDirectory.pathSource || 'default'} source currently controls the active workdir.`,
    tone: preview.value?.workingDirectory.valid ? 'ok' : 'warn',
  },
  {
    label: 'Trace rows',
    value: String(traceRows.value.length),
    detail: `${overriddenCount.value} row(s) are superseded by higher-priority layers.`,
    tone: overriddenCount.value > 0 ? 'warn' : 'ok',
  },
]);

const sourceRows = computed(() => {
  const counts = new Map<string, number>();
  traceRows.value.forEach((trace) => {
    counts.set(trace.source, (counts.get(trace.source) ?? 0) + 1);
  });
  return ['database', 'workdir', 'environment', 'runtime'].map((label) => ({
    label,
    value: String(counts.get(label) ?? 0),
  }));
});

function buildRequest(): EffectiveRuntimePreviewRequest {
  return {
    workdirPath: workdirOverride.value.trim() || undefined,
    providerName: providerOverride.value.trim() || undefined,
    maxInjectedMemories: maxInjectedOverride.value ?? undefined,
  };
}

async function applyPreview() {
  previewLoading.value = true;
  error.value = '';
  notice.value = '';
  try {
    await store.previewEffectiveRuntime(buildRequest());
    notice.value = 'Effective runtime preview refreshed.';
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : String(cause);
  } finally {
    previewLoading.value = false;
  }
}

async function refreshPreview() {
  previewLoading.value = true;
  error.value = '';
  notice.value = '';
  try {
    await store.refreshConfigSurfaces();
    notice.value = 'Effective runtime surfaces refreshed.';
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : String(cause);
  } finally {
    previewLoading.value = false;
  }
}

function resetOverrides() {
  workdirOverride.value = '';
  providerOverride.value = '';
  maxInjectedOverride.value = null;
  notice.value = '';
  error.value = '';
}

function sourceTone(source: string) {
  switch (source) {
    case 'runtime':
      return 'ok';
    case 'environment':
      return 'warn';
    case 'workdir':
      return 'muted';
    case 'database':
      return 'ok';
    default:
      return 'muted';
  }
}

onMounted(async () => {
  if (!store.effectiveRuntimePreview) {
    await store.refreshConfigSurfaces();
  }
});
</script>

<style scoped>
.runtime-command {
  display: grid;
  gap: 14px;
}

.runtime-hero {
  border-color: #d7cebd;
  background:
    linear-gradient(135deg, rgba(255, 252, 246, 0.98) 0%, rgba(247, 241, 231, 0.98) 55%, rgba(240, 246, 248, 0.98) 100%);
}

.runtime-hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.18fr) minmax(280px, 0.82fr);
  gap: 14px;
}

.runtime-band {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.runtime-band span,
.runtime-signal span,
.rail-item span,
.source-row span {
  color: #887152;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.runtime-band span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid rgba(182, 159, 111, 0.34);
  background: rgba(255, 251, 245, 0.92);
}

.runtime-copy h2 {
  margin: 0;
  color: #18304c;
  font-size: clamp(28px, 2vw + 18px, 40px);
  line-height: 0.98;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.runtime-copy > p {
  margin: 12px 0 0;
  color: #5c6776;
  line-height: 1.66;
}

.runtime-signal-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 16px;
}

.runtime-signal {
  display: grid;
  gap: 8px;
  border-top: 3px solid #98a2b0;
  background: rgba(255, 252, 247, 0.9);
}

.runtime-signal.tone-ok {
  border-top-color: #4c8661;
}

.runtime-signal.tone-warn {
  border-top-color: #bb8524;
}

.runtime-signal.tone-muted {
  border-top-color: #98a2b0;
}

.runtime-signal strong {
  color: #17304d;
  font-size: 28px;
  line-height: 1;
  overflow-wrap: anywhere;
}

.runtime-signal p {
  margin: 0;
  color: #606a79;
  font-size: 12px;
  line-height: 1.58;
}

.runtime-rail,
.detail-stack {
  display: grid;
  gap: 12px;
}

.runtime-main,
.runtime-detail-rail {
  border-color: #ddd3c3;
  background: linear-gradient(180deg, rgba(255, 252, 247, 0.96) 0%, rgba(248, 243, 234, 0.94) 100%);
}

.runtime-rail-card,
.preview-controls,
.trace-matrix-card {
  border-color: #ddd4c5;
  background: rgba(255, 252, 246, 0.92);
}

.rail-list,
.source-list {
  display: grid;
  gap: 10px;
}

.rail-item,
.source-row {
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid #e3dacb;
  background: rgba(255, 251, 245, 0.92);
}

.rail-item strong,
.source-row strong {
  color: #19314d;
  font-size: 14px;
  line-height: 1.46;
}

.preview-controls,
.trace-matrix-card {
  display: grid;
  gap: 12px;
}

.trace-table-wrap {
  border-color: #dfd5c6;
  background: rgba(255, 252, 247, 0.78);
}

.nested-panel {
  border-style: dashed;
}

.vertical-toolbar {
  flex-direction: column;
  align-items: stretch;
}

@media (max-width: 1180px) {
  .runtime-hero-grid,
  .workbench-grid.wide-detail {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .runtime-signal-grid {
    grid-template-columns: 1fr;
  }
}
</style>

<style scoped>
.runtime-hero,
.runtime-main,
.runtime-detail-rail {
  border-color: var(--line);
  background: var(--surface);
}

.runtime-band span,
.runtime-signal span,
.rail-item span,
.source-row span {
  color: var(--weak);
}

.runtime-band span {
  border-color: var(--line);
  background: var(--surface-subtle);
}

.runtime-copy h2,
.runtime-signal strong,
.rail-item strong,
.source-row strong {
  color: var(--ink);
}

.runtime-copy h2 {
  text-transform: none;
  letter-spacing: -0.04em;
}

.runtime-copy > p,
.runtime-signal p {
  color: var(--muted);
}

.runtime-rail-card,
.runtime-signal,
.preview-controls,
.trace-matrix-card,
.rail-item,
.source-row,
.trace-table-wrap {
  border-color: var(--line);
  background: var(--surface-subtle);
}
</style>
