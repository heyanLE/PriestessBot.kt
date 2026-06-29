<template>
  <div class="workingdir-command">
    <section class="panel workingdir-hero">
      <div class="workingdir-hero-grid">
        <div class="workingdir-copy">
          <div class="workingdir-band">
            <span>Priestess / Local Overlay</span>
            <span>Working Directory Control</span>
          </div>

          <h2>Working directory pointer</h2>
          <p>
            Resolve the active workdir, verify discovered overlays, and keep path-source drift
            obvious before workspace skills or local agents affect the runtime shell.
          </p>

          <div class="grid workingdir-signal-grid">
            <article v-for="signal in workingdirSignals" :key="signal.label" class="card workingdir-signal" :class="`tone-${signal.tone}`">
              <span>{{ signal.label }}</span>
              <strong>{{ signal.value }}</strong>
              <p>{{ signal.detail }}</p>
            </article>
          </div>
        </div>

        <aside class="workingdir-rail">
          <article class="card workingdir-rail-card">
            <div class="section-title compact">
              <div>
                <h3>Path doctrine</h3>
                <p>Make the active pointer, source layer, and overlay discoveries impossible to miss.</p>
              </div>
            </div>

            <div class="rail-list">
              <div class="rail-item">
                <span>Configured path</span>
                <strong>{{ configuredPathLabel }}</strong>
              </div>
              <div class="rail-item">
                <span>Effective path</span>
                <strong>{{ effectivePathLabel }}</strong>
              </div>
              <div class="rail-item">
                <span>Path source</span>
                <strong>{{ pathSourceLabel }}</strong>
              </div>
            </div>
          </article>
        </aside>
      </div>
    </section>

    <div class="workbench-grid wide-detail">
      <section class="panel registry-panel">
        <div class="section-title">
          <div>
            <h2>Working directory</h2>
            <p>{{ summaryText }}</p>
          </div>
          <div class="toolbar">
            <button type="button" @click="resetDraft" :disabled="loading">Reset</button>
            <button type="button" @click="reloadSummary" :disabled="loading">
              {{ loading ? 'Reloading' : 'Reload' }}
            </button>
            <button type="button" class="primary" @click="applyWorkingDirectory" :disabled="loading || !canApply">
              {{ applying ? 'Applying' : 'Apply' }}
            </button>
          </div>
        </div>

        <p v-if="notice" class="notice ok">{{ notice }}</p>
        <p v-if="error" class="notice error">{{ error }}</p>

        <div class="grid metric-grid compact-metrics">
          <article class="card metric">
            <strong>{{ configuredPathLabel }}</strong>
            <span>Default workdir</span>
          </article>
          <article class="card metric">
            <strong>{{ effectivePathLabel }}</strong>
            <span>Effective workdir</span>
          </article>
          <article class="card metric">
            <strong>{{ pathSourceLabel }}</strong>
            <span>Path source</span>
          </article>
          <article class="card metric">
            <strong>{{ workingDirectory?.valid ? 'Valid' : 'Fallback' }}</strong>
            <span>Validation</span>
          </article>
        </div>

        <div class="form-grid">
          <label class="full-span">
            <span>Default working directory path</span>
            <input v-model="pathDraft" type="text" placeholder="C:\\path\\to\\workdir" />
          </label>
        </div>

        <section class="notice" :class="{ warning: pathSourceLabel === 'environment', ok: workingDirectory?.valid }">
          <strong>How this path behaves</strong>
          <p v-if="pathSourceLabel === 'environment'">
            The database default can still be saved, but the current effective workdir comes from the environment layer.
          </p>
          <p v-else-if="pathSourceLabel === 'runtime'">
            The preview is currently pointing at a runtime-selected workdir.
          </p>
          <p v-else>
            Saving here updates the persisted default workdir pointer used when no higher layer overrides it.
          </p>
        </section>

        <div class="grid detected-grid">
          <article class="card">
            <div class="section-title compact">
              <div>
                <h3>Detected agents</h3>
                <p>{{ workingDirectory?.agents.length ?? 0 }} discovered.</p>
              </div>
            </div>
            <EmptyState
              v-if="(workingDirectory?.agents.length ?? 0) === 0"
              title="No agents discovered"
              detail="Agent overlay files appear here after the workdir manifest loads."
            />
            <div v-else class="detail-list">
              <div v-for="agent in workingDirectory?.agents" :key="agent.filePath" class="detail-item">
                <span>{{ agent.name }}</span>
                <code>{{ agent.filePath }}</code>
              </div>
            </div>
          </article>

          <article class="card">
            <div class="section-title compact">
              <div>
                <h3>Detected skills</h3>
                <p>{{ workingDirectory?.skills.length ?? 0 }} discovered.</p>
              </div>
            </div>
            <EmptyState
              v-if="(workingDirectory?.skills.length ?? 0) === 0"
              title="No skills discovered"
              detail="User-defined skills are only sourced from the selected working directory."
            />
            <div v-else class="detail-list">
              <div v-for="skill in workingDirectory?.skills" :key="skill.directoryPath" class="detail-item">
                <span>{{ skill.name }}</span>
                <code>{{ skill.directoryPath }}</code>
                <strong>{{ skill.enabled ? 'enabled' : 'disabled' }}</strong>
              </div>
            </div>
          </article>
        </div>
      </section>

      <aside class="panel detail-panel detail-rail">
        <div class="section-title">
          <div>
            <h2>Validation detail</h2>
            <p>Source, manifest, and diagnostics stay visible while switching paths.</p>
          </div>
        </div>

        <div class="detail-stack">
          <article class="card workingdir-rail-card">
            <div class="detail-list">
              <div class="detail-item">
                <span>Configured path</span>
                <strong>{{ configuredPathLabel }}</strong>
              </div>
              <div class="detail-item">
                <span>Effective path</span>
                <strong>{{ effectivePathLabel }}</strong>
              </div>
              <div class="detail-item">
                <span>Path source</span>
                <strong>{{ pathSourceLabel }}</strong>
              </div>
              <div class="detail-item">
                <span>Manifest</span>
                <strong>{{ workingDirectory?.manifestFound ? 'found' : 'missing' }}</strong>
              </div>
              <div class="detail-item">
                <span>Last loaded</span>
                <strong>{{ lastLoadedAt }}</strong>
              </div>
            </div>
          </article>

          <section v-if="workingDirectory?.unsupportedFields.length" class="notice warning">
            <strong>Unsupported fields</strong>
            <ul>
              <li v-for="item in workingDirectory?.unsupportedFields" :key="item">{{ item }}</li>
            </ul>
          </section>

          <section v-if="workingDirectory?.diagnostics.length" class="notice" :class="{ error: !workingDirectory?.valid }">
            <strong>Diagnostics</strong>
            <ul>
              <li v-for="item in workingDirectory?.diagnostics" :key="item">{{ item }}</li>
            </ul>
          </section>

          <section class="panel nested-panel">
            <div class="section-title compact">
              <div>
                <h3>Effective runtime</h3>
                <p>Apply or reload to refresh the runtime preview inline.</p>
              </div>
              <RouterLink class="button-link" to="/effective-runtime">Open</RouterLink>
            </div>
            <div class="detail-list">
              <div class="detail-item">
                <span>Provider</span>
                <strong>{{ store.effectiveRuntimePreview?.config.agent.providerName || 'unset' }}</strong>
              </div>
              <div class="detail-item">
                <span>Agent</span>
                <strong>{{ store.effectiveRuntimePreview?.config.agent.name || 'unset' }}</strong>
              </div>
              <div class="detail-item">
                <span>Memory max injected</span>
                <strong>{{ store.effectiveRuntimePreview?.memoryPolicy.maxInjectedMemories ?? 0 }}</strong>
              </div>
            </div>
          </section>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import EmptyState from '../components/EmptyState.vue';
import { useDashboardStore } from '../stores/dashboard';

const store = useDashboardStore();
const pathDraft = ref('');
const loading = ref(false);
const applying = ref(false);
const notice = ref('');
const error = ref('');

const workingDirectory = computed(() => store.workingDirectory);
const configuredPathLabel = computed(() => workingDirectory.value?.configuredPath || 'not set');
const effectivePathLabel = computed(() => workingDirectory.value?.effectivePath || 'not set');
const pathSourceLabel = computed(() => workingDirectory.value?.pathSource || 'default');
const canApply = computed(() => pathDraft.value.trim() !== configuredPathLabel.value);
const summaryText = computed(() => {
  const working = workingDirectory.value;
  if (!working) return 'Load the working-directory summary to validate runtime extensions.';
  if (!working.configuredPath && !working.effectivePath) return 'No default workdir is persisted yet.';
  return `${working.skills.length} skills and ${working.agents.length} agent overlays discovered from the selected path.`;
});
const lastLoadedAt = computed(() => {
  const value = workingDirectory.value?.lastLoadedAt;
  if (!value) return 'not loaded';
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(value);
});

const workingdirSignals = computed(() => [
  {
    label: 'Configured',
    value: configuredPathLabel.value,
    detail: 'Persisted default pointer for the local overlay layer.',
    tone: configuredPathLabel.value !== 'not set' ? 'ok' : 'muted',
  },
  {
    label: 'Effective',
    value: effectivePathLabel.value,
    detail: 'Path currently driving local overlay discovery.',
    tone: effectivePathLabel.value !== 'not set' ? 'ok' : 'warn',
  },
  {
    label: 'Skills',
    value: String(workingDirectory.value?.skills.length ?? 0),
    detail: 'Working-directory skills visible to the current shell.',
    tone: (workingDirectory.value?.skills.length ?? 0) > 0 ? 'ok' : 'muted',
  },
  {
    label: 'Agents',
    value: String(workingDirectory.value?.agents.length ?? 0),
    detail: 'Agent overlays discovered from the active path.',
    tone: (workingDirectory.value?.agents.length ?? 0) > 0 ? 'warn' : 'muted',
  },
]);

function resetDraft() {
  pathDraft.value = workingDirectory.value?.configuredPath ?? '';
}

async function reloadSummary() {
  loading.value = true;
  error.value = '';
  notice.value = '';
  try {
    await store.refreshConfigSurfaces();
    resetDraft();
    notice.value = 'Working-directory summary refreshed.';
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : String(cause);
  } finally {
    loading.value = false;
  }
}

async function applyWorkingDirectory() {
  applying.value = true;
  error.value = '';
  notice.value = '';
  try {
    await store.updateWorkingDirectory(pathDraft.value.trim());
    resetDraft();
    notice.value =
      pathSourceLabel.value === 'environment'
        ? 'Default workdir saved, but the current effective workdir still comes from the environment layer.'
        : 'Default workdir saved and runtime preview refreshed.';
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : String(cause);
  } finally {
    applying.value = false;
  }
}

watch(workingDirectory, () => resetDraft());

onMounted(async () => {
  if (!store.workingDirectory) {
    await store.refreshConfigSurfaces();
  }
  resetDraft();
});
</script>

<style scoped>
.workingdir-command {
  display: grid;
  gap: 14px;
}

.workingdir-hero {
  border-color: #d7cebd;
  background:
    linear-gradient(135deg, rgba(255, 252, 246, 0.98) 0%, rgba(247, 241, 231, 0.98) 55%, rgba(240, 246, 248, 0.98) 100%);
}

.workingdir-hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.18fr) minmax(280px, 0.82fr);
  gap: 14px;
}

.workingdir-band {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.workingdir-band span,
.workingdir-signal span,
.rail-item span {
  color: #887152;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.workingdir-band span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid rgba(182, 159, 111, 0.34);
  background: rgba(255, 251, 245, 0.92);
}

.workingdir-copy h2 {
  margin: 0;
  color: #18304c;
  font-size: clamp(28px, 2vw + 18px, 40px);
  line-height: 0.98;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.workingdir-copy > p {
  margin: 12px 0 0;
  color: #5c6776;
  line-height: 1.66;
}

.workingdir-signal-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 16px;
}

.workingdir-signal {
  display: grid;
  gap: 8px;
  border-top: 3px solid #98a2b0;
  background: rgba(255, 252, 247, 0.9);
}

.workingdir-signal.tone-ok {
  border-top-color: #4c8661;
}

.workingdir-signal.tone-warn {
  border-top-color: #bb8524;
}

.workingdir-signal.tone-muted {
  border-top-color: #98a2b0;
}

.workingdir-signal strong {
  color: #17304d;
  font-size: 28px;
  line-height: 1;
  overflow-wrap: anywhere;
}

.workingdir-signal p {
  margin: 0;
  color: #606a79;
  font-size: 12px;
  line-height: 1.58;
}

.workingdir-rail,
.detail-stack {
  display: grid;
  gap: 12px;
}

.workingdir-rail-card,
.registry-panel,
.detail-rail {
  border-color: #ddd4c5;
  background: rgba(255, 252, 246, 0.92);
}

.rail-list {
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

.registry-panel,
.detail-rail {
  border-color: #ddd3c3;
  background: linear-gradient(180deg, rgba(255, 252, 247, 0.96) 0%, rgba(248, 243, 234, 0.94) 100%);
}

.detected-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.nested-panel {
  border-style: dashed;
}

@media (max-width: 1180px) {
  .workingdir-hero-grid,
  .workbench-grid.wide-detail {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .workingdir-signal-grid,
  .detected-grid {
    grid-template-columns: 1fr;
  }
}
</style>
