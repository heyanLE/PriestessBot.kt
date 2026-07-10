<template>
  <div class="tool-governance">
    <section class="panel governance-hero">
      <div class="governance-grid">
        <div class="governance-copy">
          <div class="governance-band">
            <span>Assets</span>
            <span>Tools</span>
          </div>

          <h2>Runtime tool registry</h2>
          <p>
            Review callable surfaces, explicit allow and deny posture, and risk pressure from one
            structured registry before requests fan into the runtime.
          </p>

          <div class="grid governance-signal-grid">
            <article v-for="signal in governanceSignals" :key="signal.label" class="card governance-signal" :class="`tone-${signal.tone}`">
              <span>{{ signal.label }}</span>
              <strong>{{ signal.value }}</strong>
              <p>{{ signal.detail }}</p>
            </article>
          </div>
        </div>

        <aside class="governance-rail">
          <article class="card rail-card">
            <div class="section-title compact">
              <div>
                <h3>What matters here</h3>
                <p>Keep scope, risk, and overrides readable before changing tool policy.</p>
              </div>
            </div>

            <div class="rail-list">
              <div class="rail-item">
                <span>Policy posture</span>
                <strong>{{ policyPosture }}</strong>
              </div>
              <div class="rail-item">
                <span>Audit markers</span>
                <strong>{{ auditedCount }} tools carry review metadata</strong>
              </div>
              <div class="rail-item">
                <span>Default-off lanes</span>
                <strong>{{ defaultOffCount }} restricted by default</strong>
              </div>
            </div>
          </article>

          <article class="card rail-card">
            <div class="section-title compact">
              <div>
                <h3>Source sweep</h3>
                <p>Registry coverage by tool origin.</p>
              </div>
            </div>

            <div class="source-list">
              <div v-for="item in sourceCards" :key="item.label" class="source-row">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
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
            <h2>Tool registry</h2>
            <p>{{ filteredTools.length }} of {{ store.tools.length }} tools visible after control filters.</p>
          </div>
          <div class="toolbar">
            <span class="inline-status" :class="registryStatus.tone">{{ registryStatus.label }}</span>
          </div>
        </div>

        <div class="control-strip">
          <label class="control-field search-field">
            <span>Search</span>
            <input v-model="query" type="search" placeholder="Name or description" />
          </label>

          <label class="control-field">
            <span>Source</span>
            <select v-model="sourceFilter">
              <option value="all">All sources</option>
              <option value="BUILTIN">Built-in</option>
              <option value="PLUGIN">Plugin</option>
              <option value="MCP">MCP</option>
            </select>
          </label>

          <label class="control-field">
            <span>Risk lane</span>
            <select v-model="riskFilter">
              <option value="all">All risks</option>
              <option value="SAFE_READ">Safe read</option>
              <option value="SESSION_ACTION">Session action</option>
              <option value="EXTERNAL_READ">External read</option>
              <option value="STATE_WRITE">State write</option>
              <option value="HIGH_RISK">High risk</option>
            </select>
          </label>

          <label class="control-field">
            <span>Policy state</span>
            <select v-model="enabledFilter">
              <option value="all">All states</option>
              <option value="enabled">Allowed</option>
              <option value="disabled">Denied</option>
            </select>
          </label>
        </div>

        <p v-if="actionNotice" class="notice ok">{{ actionNotice }}</p>
        <p v-if="actionError" class="notice error">{{ actionError }}</p>

        <EmptyState
          v-if="filteredTools.length === 0"
          title="No matching tools"
          detail="Adjust the search or control strip to widen the registry view."
        />

        <div v-else class="table-wrap governance-table-wrap">
          <table class="table governance-table">
            <thead>
              <tr>
                <th>Tool</th>
                <th>Source</th>
                <th>Risk</th>
                <th>Posture</th>
                <th>Contract</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="tool in filteredTools"
                :key="tool.name"
                class="clickable-row"
                :class="{ selected: selectedTool?.name === tool.name }"
                @click="selectedToolName = tool.name"
              >
                <td>
                  <strong>{{ tool.name }}</strong>
                  <p class="muted">{{ tool.description }}</p>
                </td>
                <td>
                  <span class="inline-status muted">{{ formatSource(tool.source) }}</span>
                </td>
                <td>
                  <span class="inline-status" :class="riskTone(tool.riskLevel)">
                    {{ formatRiskLevel(tool.riskLevel) }}
                  </span>
                </td>
                <td>
                  <div class="tool-state">
                    <span class="inline-status" :class="isToolAllowed(tool.name) ? 'ok' : 'warn'">
                      {{ isToolAllowed(tool.name) ? 'Allowed' : 'Denied' }}
                    </span>
                    <small>{{ tool.defaultEnabled ? 'Default on' : 'Default off' }}</small>
                  </div>
                </td>
                <td>
                  <div class="contract-cell">
                    <strong>{{ tool.parameters.required.length }}</strong>
                    <small>{{ tool.requiredCapabilities.length }} capabilities</small>
                  </div>
                </td>
                <td>
                  <div class="toolbar compact-actions">
                    <button
                      type="button"
                      class="primary"
                      :disabled="!canEditTools || isToolAllowed(tool.name)"
                      @click.stop="allowTool(tool.name)"
                    >
                      Allow
                    </button>
                    <button
                      type="button"
                      :disabled="!canEditTools || !isToolAllowed(tool.name)"
                      @click.stop="denyTool(tool.name)"
                    >
                      Deny
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <aside class="panel detail-panel detail-rail">
        <div class="section-title">
          <div>
            <h2>{{ selectedTool?.name ?? 'Tool detail rail' }}</h2>
            <p>{{ selectedTool ? selectedDetailSummary : 'Select a registry row to inspect policy and invocation contract.' }}</p>
          </div>
        </div>

        <EmptyState
          v-if="!selectedTool"
          title="No tool selected"
          detail="Choose a tool from the registry board to inspect governance detail."
        />

        <div v-else class="detail-stack">
          <article class="card detail-card">
            <div class="section-title compact">
              <div>
                <h3>Policy posture</h3>
                <p>Live allowance, default state, and review markers.</p>
              </div>
            </div>

            <div class="chip-row detail-badges">
              <span class="inline-status" :class="isToolAllowed(selectedTool.name) ? 'ok' : 'warn'">
                {{ isToolAllowed(selectedTool.name) ? 'Allowed' : 'Denied' }}
              </span>
              <span class="inline-status" :class="selectedTool.defaultEnabled ? 'muted' : 'warn'">
                {{ selectedTool.defaultEnabled ? 'Default on' : 'Default off' }}
              </span>
              <span v-if="selectedTool.auditLog" class="inline-status muted">Audited</span>
              <span class="inline-status" :class="riskTone(selectedTool.riskLevel)">
                {{ formatRiskLevel(selectedTool.riskLevel) }}
              </span>
            </div>

            <div class="detail-list detail-list-tight">
              <div class="detail-item">
                <span>Source</span>
                <strong>{{ formatSource(selectedTool.source) }}</strong>
              </div>
              <div v-if="selectedTool.owner" class="detail-item">
                <span>Owner</span>
                <strong>{{ selectedTool.owner }}</strong>
              </div>
              <div class="detail-item">
                <span>Operational note</span>
                <strong>{{ toolOperationalNote(selectedTool) }}</strong>
              </div>
            </div>

            <div class="toolbar action-row">
              <button
                type="button"
                class="primary"
                :disabled="!canEditTools || isToolAllowed(selectedTool.name)"
                @click="allowTool(selectedTool.name)"
              >
                Allow
              </button>
              <button
                type="button"
                :disabled="!canEditTools || !isToolAllowed(selectedTool.name)"
                @click="denyTool(selectedTool.name)"
              >
                Deny
              </button>
            </div>
          </article>

          <article class="card detail-card">
            <div class="section-title compact">
              <div>
                <h3>Invocation contract</h3>
                <p>Required fields and capability gates for callers.</p>
              </div>
            </div>

            <div class="grid detail-stat-grid">
              <article class="detail-stat">
                <span>Required fields</span>
                <strong>{{ selectedTool.parameters.required.length }}</strong>
              </article>
              <article class="detail-stat">
                <span>Schema props</span>
                <strong>{{ parameterCount(selectedTool) }}</strong>
              </article>
              <article class="detail-stat">
                <span>Capabilities</span>
                <strong>{{ selectedTool.requiredCapabilities.length }}</strong>
              </article>
            </div>

            <div class="detail-item">
              <span>Required parameters</span>
              <div class="chip-row">
                <span v-for="name in selectedTool.parameters.required" :key="name" class="chip">{{ name }}</span>
                <span v-if="selectedTool.parameters.required.length === 0" class="muted">None</span>
              </div>
            </div>

            <div class="detail-item">
              <span>Capabilities</span>
              <div class="chip-row">
                <span v-for="name in selectedTool.requiredCapabilities" :key="name" class="chip">{{ name }}</span>
                <span v-if="selectedTool.requiredCapabilities.length === 0" class="muted">None</span>
              </div>
            </div>
          </article>

          <p v-if="selectedTool.statusReason" class="notice warning">{{ selectedTool.statusReason }}</p>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import EmptyState from '../components/EmptyState.vue';
import { useDashboardStore } from '../stores/dashboard';
import type { ToolDto } from '../api/dashboard';

const store = useDashboardStore();
const query = ref('');
const sourceFilter = ref('all');
const riskFilter = ref('all');
const enabledFilter = ref('all');
const selectedToolName = ref('');
const actionNotice = ref('');
const actionError = ref('');

const filteredTools = computed(() => {
  const normalizedQuery = query.value.trim().toLowerCase();
  return store.tools.filter((tool) => {
    const matchesQuery =
      normalizedQuery.length === 0 ||
      tool.name.toLowerCase().includes(normalizedQuery) ||
      tool.description.toLowerCase().includes(normalizedQuery);
    const matchesSource = sourceFilter.value === 'all' || tool.source === sourceFilter.value;
    const matchesRisk = riskFilter.value === 'all' || tool.riskLevel === riskFilter.value;
    const allowed = isToolAllowed(tool.name);
    const matchesEnabled =
      enabledFilter.value === 'all' ||
      (enabledFilter.value === 'enabled' && allowed) ||
      (enabledFilter.value === 'disabled' && !allowed);
    return matchesQuery && matchesSource && matchesRisk && matchesEnabled;
  });
});

const selectedTool = computed(
  () => filteredTools.value.find((tool) => tool.name === selectedToolName.value) ?? filteredTools.value[0] ?? null,
);

const canEditTools = computed(() => store.config !== null);
const allowedCount = computed(() => store.tools.filter((tool) => isToolAllowed(tool.name)).length);
const deniedCount = computed(() => store.tools.length - allowedCount.value);
const highRiskCount = computed(() => store.tools.filter((tool) => tool.riskLevel === 'HIGH_RISK').length);
const auditedCount = computed(() => store.tools.filter((tool) => tool.auditLog).length);
const defaultOffCount = computed(() => store.tools.filter((tool) => !tool.defaultEnabled).length);
const highRiskAllowedCount = computed(
  () => store.tools.filter((tool) => tool.riskLevel === 'HIGH_RISK' && isToolAllowed(tool.name)).length,
);

const governanceSignals = computed(() => [
  {
    label: 'Visible',
    value: String(filteredTools.value.length),
    detail: `${store.tools.length} tools cataloged in the full runtime inventory.`,
    tone: 'muted',
  },
  {
    label: 'Allowed',
    value: String(allowedCount.value),
    detail: `${deniedCount.value} lanes currently blocked by explicit policy.`,
    tone: allowedCount.value > 0 ? 'ok' : 'warn',
  },
  {
    label: 'Denied',
    value: String(deniedCount.value),
    detail: 'Default-off and manually denied lanes remain visible for audit.',
    tone: deniedCount.value > 0 ? 'warn' : 'muted',
  },
  {
    label: 'High risk',
    value: String(highRiskCount.value),
    detail: `${highRiskAllowedCount.value} high-risk lanes currently permitted.`,
    tone: highRiskAllowedCount.value > 0 ? 'warn' : 'ok',
  },
]);

const sourceCards = computed(() => [
  {
    label: 'Built-in',
    value: String(store.tools.filter((tool) => tool.source === 'BUILTIN').length),
  },
  {
    label: 'Plugin',
    value: String(store.tools.filter((tool) => tool.source === 'PLUGIN').length),
  },
  {
    label: 'MCP',
    value: String(store.tools.filter((tool) => tool.source === 'MCP').length),
  },
]);

const policyPosture = computed(() => {
  if (store.tools.length === 0) return 'Registry waiting for runtime sync';
  if (highRiskAllowedCount.value > 0) return `${highRiskAllowedCount.value} high-risk lane(s) open`;
  if (deniedCount.value > 0) return `${deniedCount.value} lane(s) under restriction`;
  return 'Nominal governance posture';
});

const registryStatus = computed(() => {
  if (filteredTools.value.length === 0) {
    return { label: 'Filtered empty', tone: 'muted' };
  }
  if (highRiskAllowedCount.value > 0) {
    return { label: 'High-risk exposure present', tone: 'warn' };
  }
  return { label: 'Policy sweep nominal', tone: 'ok' };
});

const selectedDetailSummary = computed(() => {
  if (!selectedTool.value) return '';
  return `${formatSource(selectedTool.value.source)} lane with ${formatRiskLevel(selectedTool.value.riskLevel).toLowerCase()} posture and ${selectedTool.value.parameters.required.length} required field(s).`;
});

function isToolAllowed(name: string) {
  const config = store.config?.agent;
  if (!config) return false;
  const enabledTools = config.enabledTools ?? [];
  const disabledTools = config.disabledTools ?? [];
  if (disabledTools.includes(name)) return false;
  if (enabledTools.length > 0) return enabledTools.includes(name);
  const tool = store.tools.find((item) => item.name === name);
  return tool ? tool.defaultEnabled && !disabledTools.includes(name) : false;
}

async function allowTool(name: string) {
  actionNotice.value = '';
  actionError.value = '';
  try {
    await store.updateToolAllowance(name, true);
    actionNotice.value = `Allowed ${name}.`;
  } catch (cause) {
    actionError.value = cause instanceof Error ? cause.message : String(cause);
  }
}

async function denyTool(name: string) {
  actionNotice.value = '';
  actionError.value = '';
  try {
    await store.updateToolAllowance(name, false);
    actionNotice.value = `Denied ${name}.`;
  } catch (cause) {
    actionError.value = cause instanceof Error ? cause.message : String(cause);
  }
}

function formatRiskLevel(level: ToolDto['riskLevel']) {
  return level.replace(/_/g, ' ');
}

function formatSource(source: ToolDto['source']) {
  switch (source) {
    case 'BUILTIN':
      return 'Built-in';
    case 'PLUGIN':
      return 'Plugin';
    case 'MCP':
      return 'MCP';
    default:
      return source;
  }
}

function riskTone(level: ToolDto['riskLevel']) {
  switch (level) {
    case 'HIGH_RISK':
      return 'danger';
    case 'STATE_WRITE':
      return 'warn';
    case 'EXTERNAL_READ':
      return 'muted';
    case 'SESSION_ACTION':
      return 'ok';
    case 'SAFE_READ':
    default:
      return 'muted';
  }
}

function parameterCount(tool: ToolDto) {
  return Object.keys(tool.parameters.properties ?? {}).length;
}

function toolOperationalNote(tool: ToolDto) {
  if (tool.statusReason) return tool.statusReason;
  if (tool.requiredCapabilities.length > 0) {
    return `Requires ${tool.requiredCapabilities.length} capability gate(s) before invocation.`;
  }
  if (tool.parameters.required.length > 0) {
    return `${tool.parameters.required.length} field(s) are required for a valid call.`;
  }
  return 'No extra gate beyond runtime policy and caller scope.';
}

watch(filteredTools, (nextTools) => {
  if (!nextTools.some((tool) => tool.name === selectedToolName.value)) {
    selectedToolName.value = nextTools[0]?.name ?? '';
  }
});
</script>

<style scoped>
.tool-governance {
  display: grid;
  gap: 14px;
}

.governance-hero {
  overflow: hidden;
  border-color: #d8ceb9;
  background:
    linear-gradient(135deg, rgba(255, 252, 245, 0.98) 0%, rgba(248, 241, 229, 0.98) 58%, rgba(241, 246, 249, 0.98) 100%);
}

.governance-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.18fr) minmax(280px, 0.82fr);
  gap: 14px;
  align-items: start;
}

.governance-band {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.governance-band span,
.governance-signal span,
.rail-item span,
.source-row span,
.control-field span,
.detail-stat span {
  color: #8a7351;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.governance-band span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid rgba(185, 162, 113, 0.34);
  background: rgba(255, 251, 244, 0.92);
}

.governance-copy h2 {
  margin: 0;
  font-size: clamp(28px, 2vw + 18px, 40px);
  line-height: 0.98;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: #17314c;
}

.governance-copy > p {
  max-width: 68ch;
  margin: 12px 0 0;
  color: #596473;
  line-height: 1.68;
}

.governance-signal-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 16px;
}

.governance-signal {
  display: grid;
  gap: 8px;
  border-top: 3px solid #97a1b1;
  background: rgba(255, 252, 247, 0.88);
}

.governance-signal.tone-ok {
  border-top-color: #4c8661;
}

.governance-signal.tone-warn {
  border-top-color: #be8621;
}

.governance-signal.tone-muted {
  border-top-color: #97a1b1;
}

.governance-signal strong {
  color: #182f49;
  font-size: 30px;
  line-height: 1;
}

.governance-signal p {
  margin: 0;
  color: #5f6a78;
  font-size: 12px;
  line-height: 1.58;
}

.governance-rail {
  display: grid;
  gap: 12px;
}

.rail-card,
.detail-card {
  border-color: #ddd4c3;
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
  border: 1px solid #e3d9ca;
  background: rgba(255, 251, 245, 0.92);
}

.rail-item strong,
.source-row strong {
  color: #182f49;
  font-size: 14px;
  line-height: 1.45;
}

.registry-panel,
.detail-rail {
  border-color: #ddd3c2;
  background: linear-gradient(180deg, rgba(255, 252, 247, 0.96) 0%, rgba(248, 243, 234, 0.94) 100%);
}

.control-strip {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) repeat(3, minmax(160px, 0.8fr));
  gap: 10px;
  margin-bottom: 14px;
  padding: 14px;
  border: 1px solid #dfd5c5;
  border-radius: 16px;
  background: rgba(255, 251, 244, 0.9);
}

.control-field {
  display: grid;
  gap: 6px;
}

.control-field input,
.control-field select {
  min-width: 0;
}

.governance-table-wrap {
  border-color: #ddd4c4;
  background: rgba(255, 252, 247, 0.74);
}

.governance-table {
  min-width: 860px;
}

.tool-state,
.contract-cell {
  display: grid;
  gap: 4px;
}

.tool-state small,
.contract-cell small {
  color: #6c7786;
  font-size: 12px;
}

.contract-cell strong {
  color: #18314d;
  font-size: 16px;
  line-height: 1.1;
}

.compact-actions {
  flex-wrap: nowrap;
}

.detail-stack {
  display: grid;
  gap: 12px;
}

.detail-badges {
  margin-bottom: 12px;
}

.detail-list-tight {
  gap: 10px;
}

.action-row {
  margin-top: 12px;
}

.detail-stat-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-bottom: 12px;
}

.detail-stat {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid #e3d9c8;
  background: rgba(255, 252, 247, 0.92);
}

.detail-stat strong {
  color: #18314d;
  font-size: 22px;
  line-height: 1;
}

@media (max-width: 1180px) {
  .governance-grid,
  .workbench-grid.wide-detail {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .control-strip,
  .governance-signal-grid,
  .detail-stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
