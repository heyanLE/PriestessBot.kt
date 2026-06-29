<template>
  <div class="subagent-command">
    <section class="panel subagent-hero">
      <div class="subagent-hero-grid">
        <div class="subagent-copy">
          <div class="subagent-band">
            <span>Priestess / Delegation Desk</span>
            <span>Sub-Agent Orchestration</span>
          </div>

          <h2>Delegation control board</h2>
          <p>
            Tune routing rules, enable or suppress specialist lanes, and run draft delegation tests
            from one daytime console before requests leave the primary agent.
          </p>

          <div class="grid subagent-signal-grid">
            <article v-for="signal in subagentSignals" :key="signal.label" class="card subagent-signal" :class="`tone-${signal.tone}`">
              <span>{{ signal.label }}</span>
              <strong>{{ signal.value }}</strong>
              <p>{{ signal.detail }}</p>
            </article>
          </div>
        </div>

        <aside class="subagent-rail">
          <article class="card subagent-rail-card">
            <div class="section-title compact">
              <div>
                <h3>Control doctrine</h3>
                <p>Routes should stay explicit, testable, and easy to disable when the shell changes.</p>
              </div>
            </div>

            <div class="rail-list">
              <div class="rail-item">
                <span>Saved posture</span>
                <strong>{{ loadedConfig.enabled ? 'Routing enabled' : 'Routing disabled' }}</strong>
              </div>
              <div class="rail-item">
                <span>Draft state</span>
                <strong>{{ canEditStructured ? 'Structured editing ready' : 'Draft invalid' }}</strong>
              </div>
              <div class="rail-item">
                <span>Latest test</span>
                <strong>{{ testResult ? testResult.selectedAgentName : 'No test run yet' }}</strong>
              </div>
            </div>
          </article>
        </aside>
      </div>
    </section>

    <section class="panel config-panel">
      <div class="section-title">
        <div>
          <h2>Orchestration config</h2>
          <p>{{ configStatus }}</p>
        </div>
        <div class="toolbar">
          <button type="button" @click="resetDraft" :disabled="loading">Reset</button>
          <button type="button" class="primary" @click="saveConfig" :disabled="loading || saving">Save</button>
        </div>
      </div>

      <div class="grid metric-grid compact-metrics">
        <article class="card metric">
          <strong>{{ draftSummary.enabled ? 'On' : 'Off' }}</strong>
          <span>Routing</span>
        </article>
        <article class="card metric">
          <strong>{{ draftSummary.agentCount }}</strong>
          <span>Agents</span>
        </article>
        <article class="card metric">
          <strong>{{ draftSummary.routeCount }}</strong>
          <span>Routes</span>
        </article>
        <article class="card metric">
          <strong>{{ draftSummary.defaultAgentName || 'Primary' }}</strong>
          <span>Default</span>
        </article>
      </div>

      <textarea v-model="draft" class="sub-agent-config-editor" spellcheck="false"></textarea>
      <p v-if="draftError" class="notice error">{{ draftError }}</p>
      <p v-if="saveNotice" class="notice ok">{{ saveNotice }}</p>
    </section>

    <div class="workbench-grid wide-detail">
      <section class="panel editor-panel">
        <div class="section-title">
          <div>
            <h2>Rule editor</h2>
            <p>Structured controls update the JSON draft without hiding the raw contract.</p>
          </div>
        </div>

        <div class="rule-controls">
          <label class="toggle-row">
            <input
              type="checkbox"
              :checked="parsedConfig.enabled"
              :disabled="!canEditStructured"
              @change="setEnabled(($event.target as HTMLInputElement).checked)"
            />
            <span>Enable sub-agent routing</span>
          </label>

          <label class="field-row">
            <span>Default agent</span>
            <select
              :value="parsedConfig.defaultAgentName"
              :disabled="!canEditStructured || parsedConfig.agents.length === 0"
              @change="setDefaultAgent(($event.target as HTMLSelectElement).value)"
            >
              <option value="">Primary agent</option>
              <option v-for="agent in parsedConfig.agents" :key="agent.name" :value="agent.name">{{ agent.name }}</option>
            </select>
          </label>
        </div>

        <form class="structured-form" @submit.prevent="addAgent">
          <input v-model="newAgentName" type="text" placeholder="Agent name" />
          <input v-model="newAgentDescription" type="text" placeholder="Description" />
          <button type="submit" class="primary" :disabled="!canEditStructured || newAgentName.trim().length === 0">
            Add Agent
          </button>
        </form>

        <div class="grid structured-card-list">
          <article v-for="agent in parsedConfig.agents" :key="agent.name" class="card structured-card">
            <div>
              <strong>{{ agent.name }}</strong>
              <p>{{ agent.description || agent.agent.providerName }}</p>
            </div>
            <div class="toolbar">
              <button type="button" @click="toggleAgent(agent.name)" :disabled="!canEditStructured">
                {{ agent.enabled ? 'Disable' : 'Enable' }}
              </button>
              <button type="button" @click="removeAgent(agent.name)" :disabled="!canEditStructured">Remove</button>
            </div>
          </article>
        </div>

        <form class="structured-form route-form" @submit.prevent="addRoute">
          <input v-model="newRouteName" type="text" placeholder="Route name" />
          <select v-model="newRouteTarget" :disabled="parsedConfig.agents.length === 0">
            <option value="">Target agent</option>
            <option v-for="agent in parsedConfig.agents" :key="agent.name" :value="agent.name">{{ agent.name }}</option>
          </select>
          <input v-model="newRouteKeywords" type="text" placeholder="Keywords, comma separated" />
          <button
            type="submit"
            class="primary"
            :disabled="!canEditStructured || newRouteName.trim().length === 0 || newRouteTarget.length === 0"
          >
            Add Route
          </button>
        </form>

        <div class="route-table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>Route</th>
                <th>Target</th>
                <th>Keywords</th>
                <th>Priority</th>
                <th>State</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="route in parsedConfig.routes" :key="route.name">
                <td>{{ route.name }}</td>
                <td>
                  <select
                    :value="route.targetAgentName"
                    :disabled="!canEditStructured"
                    @change="updateRoute(route.name, { targetAgentName: ($event.target as HTMLSelectElement).value })"
                  >
                    <option v-for="agent in parsedConfig.agents" :key="agent.name" :value="agent.name">{{ agent.name }}</option>
                  </select>
                </td>
                <td>
                  <input
                    :value="route.keywords.join(', ')"
                    :disabled="!canEditStructured"
                    @change="updateRoute(route.name, { keywords: splitKeywords(($event.target as HTMLInputElement).value) })"
                  />
                </td>
                <td>
                  <input
                    class="number-input"
                    type="number"
                    :value="route.priority"
                    :disabled="!canEditStructured"
                    @change="updateRoute(route.name, { priority: Number(($event.target as HTMLInputElement).value) || 0 })"
                  />
                </td>
                <td>
                  <div class="route-actions">
                    <button type="button" @click="updateRoute(route.name, { enabled: !route.enabled })" :disabled="!canEditStructured">
                      {{ route.enabled ? 'Disable' : 'Enable' }}
                    </button>
                    <button type="button" @click="removeRoute(route.name)" :disabled="!canEditStructured">Remove</button>
                  </div>
                </td>
              </tr>
              <tr v-if="parsedConfig.routes.length === 0">
                <td colspan="5" class="muted">No routes configured.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <aside class="panel detail-panel detail-rail">
        <div class="section-title">
          <div>
            <h2>Route summary</h2>
            <p>Delegation posture derived from the current draft.</p>
          </div>
        </div>

        <EmptyState
          v-if="parsedConfig.agents.length === 0"
          title="No sub-agents"
          detail="Add agents to the JSON config to enable routing targets."
        />

        <div v-else class="detail-stack">
          <article class="card subagent-rail-card">
            <div class="grid detail-stat-grid">
              <article class="detail-stat">
                <span>Agents</span>
                <strong>{{ parsedConfig.agents.length }}</strong>
              </article>
              <article class="detail-stat">
                <span>Routes</span>
                <strong>{{ parsedConfig.routes.length }}</strong>
              </article>
              <article class="detail-stat">
                <span>Enabled</span>
                <strong>{{ parsedConfig.agents.filter((agent) => agent.enabled).length }}</strong>
              </article>
            </div>
          </article>

          <div class="grid list-grid sub-agent-list">
            <article v-for="agent in parsedConfig.agents" :key="agent.name" class="card agent-card">
              <div class="section-title compact">
                <div>
                  <h3>{{ agent.name }}</h3>
                  <p>{{ agent.agent.providerName }} / {{ agent.agent.model || 'model unset' }}</p>
                </div>
                <span class="inline-status" :class="agent.enabled ? 'ok' : 'muted'">
                  {{ agent.enabled ? 'Enabled' : 'Disabled' }}
                </span>
              </div>
              <p>{{ agent.description || agent.agent.instructions || 'No description' }}</p>
            </article>
          </div>
        </div>
      </aside>
    </div>

    <section class="panel test-panel">
      <div class="section-title">
        <div>
          <h2>Routing test</h2>
          <p>Runs the current draft through the dashboard sub-agent API.</p>
        </div>
      </div>

      <form class="chat-form" @submit.prevent="runTest">
        <input v-model="testMessage" type="text" placeholder="Message to route..." />
        <button type="submit" class="primary" :disabled="testing || testMessage.trim().length === 0">Test</button>
      </form>

      <EmptyState
        v-if="!testResult"
        title="No test result"
        detail="Submit a message to inspect selected agent and route events."
      />

      <div v-else class="grid test-result-grid">
        <article class="card metric">
          <strong>{{ testResult.status }}</strong>
          <span>Status</span>
        </article>
        <article class="card metric">
          <strong>{{ testResult.selectedAgentName }}</strong>
          <span>Selected agent</span>
        </article>
        <article class="card metric">
          <strong>{{ testResult.selectedRouteName || 'Fallback' }}</strong>
          <span>Selected route</span>
        </article>
        <article class="card metric">
          <strong>{{ testResult.selectionReason }}</strong>
          <span>Reason</span>
        </article>

        <article class="card response-card">
          <h3>Response</h3>
          <p>{{ testResult.content }}</p>
        </article>

        <article class="card response-card">
          <h3>Events</h3>
          <div class="event-list">
            <span v-for="event in testResult.events" :key="`${event.timestamp}-${event.type}-${event.toolName}`">
              {{ event.type }}<template v-if="event.toolName"> / {{ event.toolName }}</template>
            </span>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  dashboardApi,
  type AgentConfig,
  type SubAgentConfig,
  type SubAgentOrchestrationConfig,
  type SubAgentRouteConfig,
  type SubAgentTestResponse,
} from '../api/dashboard';
import EmptyState from '../components/EmptyState.vue';

const emptyConfig: SubAgentOrchestrationConfig = {
  enabled: false,
  defaultAgentName: '',
  agents: [],
  routes: [],
};

const loading = ref(false);
const saving = ref(false);
const testing = ref(false);
const loadedConfig = ref<SubAgentOrchestrationConfig>(emptyConfig);
const draft = ref('');
const draftError = ref('');
const saveNotice = ref('');
const testMessage = ref('');
const testResult = ref<SubAgentTestResponse | null>(null);
const newAgentName = ref('');
const newAgentDescription = ref('');
const newRouteName = ref('');
const newRouteTarget = ref('');
const newRouteKeywords = ref('');

const parsedConfig = computed(() => parseDraft({ silent: true }) ?? emptyConfig);
const canEditStructured = computed(() => parseDraft({ silent: true }) !== null);
const draftSummary = computed(() => ({
  enabled: parsedConfig.value.enabled,
  defaultAgentName: parsedConfig.value.defaultAgentName,
  agentCount: parsedConfig.value.agents.length,
  routeCount: parsedConfig.value.routes.length,
}));

const configStatus = computed(() => {
  if (loading.value) return 'Loading current sub-agent config.';
  return loadedConfig.value.enabled ? 'Saved config is enabled.' : 'Saved config is disabled.';
});

const subagentSignals = computed(() => [
  {
    label: 'Routing',
    value: draftSummary.value.enabled ? 'On' : 'Off',
    detail: draftSummary.value.enabled ? 'Delegation lanes are enabled in the current draft.' : 'Delegation remains parked in the current draft.',
    tone: draftSummary.value.enabled ? 'ok' : 'muted',
  },
  {
    label: 'Agents',
    value: String(draftSummary.value.agentCount),
    detail: 'Specialist lanes defined in the active orchestration draft.',
    tone: draftSummary.value.agentCount > 0 ? 'ok' : 'muted',
  },
  {
    label: 'Routes',
    value: String(draftSummary.value.routeCount),
    detail: 'Explicit routing rules available for delegation.',
    tone: draftSummary.value.routeCount > 0 ? 'warn' : 'muted',
  },
  {
    label: 'Latest test',
    value: testResult.value?.selectedAgentName ?? 'None',
    detail: testResult.value ? 'Most recent routing test selected this agent.' : 'Run a routing test to inspect delegation behavior.',
    tone: testResult.value ? 'ok' : 'muted',
  },
]);

function formatConfig(config: SubAgentOrchestrationConfig) {
  return JSON.stringify(config, null, 2);
}

function parseDraft(options: { silent?: boolean } = {}): SubAgentOrchestrationConfig | null {
  try {
    const parsed = JSON.parse(draft.value) as SubAgentOrchestrationConfig;
    if (!Array.isArray(parsed.agents) || !Array.isArray(parsed.routes)) {
      throw new Error('Config must include agents and routes arrays.');
    }
    if (!options.silent) draftError.value = '';
    return parsed;
  } catch (cause) {
    if (!options.silent) {
      draftError.value = cause instanceof Error ? cause.message : String(cause);
    }
    return null;
  }
}

function resetDraft() {
  draft.value = formatConfig(loadedConfig.value);
  draftError.value = '';
  saveNotice.value = '';
}

function updateDraftConfig(mutator: (config: SubAgentOrchestrationConfig) => SubAgentOrchestrationConfig) {
  const parsed = parseDraft();
  if (!parsed) return;
  try {
    const next = mutator(structuredClone(parsed));
    draft.value = formatConfig(next);
    draftError.value = '';
    saveNotice.value = '';
  } catch (cause) {
    draftError.value = cause instanceof Error ? cause.message : String(cause);
  }
}

function setEnabled(enabled: boolean) {
  updateDraftConfig((config) => ({ ...config, enabled }));
}

function setDefaultAgent(defaultAgentName: string) {
  updateDraftConfig((config) => ({ ...config, defaultAgentName }));
}

function addAgent() {
  const name = newAgentName.value.trim();
  if (!name) return;
  updateDraftConfig((config) => {
    if (config.agents.some((agent) => agent.name === name)) {
      throw new Error(`Agent '${name}' already exists.`);
    }
    const template = config.agents[0]?.agent ?? defaultAgentConfig(name);
    const agent: SubAgentConfig = {
      name,
      description: newAgentDescription.value.trim(),
      enabled: true,
      agent: {
        ...template,
        name,
      },
    };
    return { ...config, agents: [...config.agents, agent] };
  });
  newAgentName.value = '';
  newAgentDescription.value = '';
}

function toggleAgent(name: string) {
  updateDraftConfig((config) => ({
    ...config,
    agents: config.agents.map((agent) => (agent.name === name ? { ...agent, enabled: !agent.enabled } : agent)),
  }));
}

function removeAgent(name: string) {
  updateDraftConfig((config) => ({
    ...config,
    defaultAgentName: config.defaultAgentName === name ? '' : config.defaultAgentName,
    agents: config.agents.filter((agent) => agent.name !== name),
    routes: config.routes.filter((route) => route.targetAgentName !== name),
  }));
}

function addRoute() {
  const name = newRouteName.value.trim();
  const targetAgentName = newRouteTarget.value;
  if (!name || !targetAgentName) return;
  updateDraftConfig((config) => {
    if (config.routes.some((route) => route.name === name)) {
      throw new Error(`Route '${name}' already exists.`);
    }
    const route: SubAgentRouteConfig = {
      name,
      targetAgentName,
      keywords: splitKeywords(newRouteKeywords.value),
      priority: 0,
      enabled: true,
    };
    return { ...config, routes: [...config.routes, route] };
  });
  newRouteName.value = '';
  newRouteTarget.value = '';
  newRouteKeywords.value = '';
}

function updateRoute(name: string, patch: Partial<SubAgentRouteConfig>) {
  updateDraftConfig((config) => ({
    ...config,
    routes: config.routes.map((route) => (route.name === name ? { ...route, ...patch } : route)),
  }));
}

function removeRoute(name: string) {
  updateDraftConfig((config) => ({
    ...config,
    routes: config.routes.filter((route) => route.name !== name),
  }));
}

function splitKeywords(value: string): string[] {
  return value.split(',').map((item) => item.trim()).filter(Boolean);
}

function defaultAgentConfig(name: string): AgentConfig {
  return {
    name,
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
  };
}

async function loadConfig() {
  loading.value = true;
  draftError.value = '';
  try {
    loadedConfig.value = await dashboardApi.subAgentConfig();
    resetDraft();
  } catch (cause) {
    draftError.value = cause instanceof Error ? cause.message : String(cause);
  } finally {
    loading.value = false;
  }
}

async function saveConfig() {
  const parsed = parseDraft();
  if (!parsed) return;
  saving.value = true;
  saveNotice.value = '';
  try {
    loadedConfig.value = await dashboardApi.replaceSubAgentConfig(parsed);
    resetDraft();
    saveNotice.value = 'Config saved.';
  } catch (cause) {
    draftError.value = cause instanceof Error ? cause.message : String(cause);
  } finally {
    saving.value = false;
  }
}

async function runTest() {
  const parsed = parseDraft();
  if (!parsed || testMessage.value.trim().length === 0) return;
  testing.value = true;
  testResult.value = null;
  try {
    testResult.value = await dashboardApi.testSubAgent({
      message: testMessage.value.trim(),
      config: parsed,
    });
  } catch (cause) {
    draftError.value = cause instanceof Error ? cause.message : String(cause);
  } finally {
    testing.value = false;
  }
}

onMounted(() => void loadConfig());
</script>

<style scoped>
.subagent-command {
  display: grid;
  gap: 14px;
}

.subagent-hero {
  border-color: #d7cebd;
  background:
    linear-gradient(135deg, rgba(255, 252, 246, 0.98) 0%, rgba(247, 241, 231, 0.98) 55%, rgba(240, 246, 248, 0.98) 100%);
}

.subagent-hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.18fr) minmax(280px, 0.82fr);
  gap: 14px;
}

.subagent-band {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.subagent-band span,
.subagent-signal span,
.rail-item span,
.detail-stat span {
  color: #887152;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.subagent-band span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid rgba(182, 159, 111, 0.34);
  background: rgba(255, 251, 245, 0.92);
}

.subagent-copy h2 {
  margin: 0;
  color: #18304c;
  font-size: clamp(28px, 2vw + 18px, 40px);
  line-height: 0.98;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.subagent-copy > p {
  margin: 12px 0 0;
  color: #5c6776;
  line-height: 1.66;
}

.subagent-signal-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 16px;
}

.subagent-signal {
  display: grid;
  gap: 8px;
  border-top: 3px solid #98a2b0;
  background: rgba(255, 252, 247, 0.9);
}

.subagent-signal.tone-ok {
  border-top-color: #4c8661;
}

.subagent-signal.tone-warn {
  border-top-color: #bb8524;
}

.subagent-signal.tone-muted {
  border-top-color: #98a2b0;
}

.subagent-signal strong {
  color: #17304d;
  font-size: 28px;
  line-height: 1;
  overflow-wrap: anywhere;
}

.subagent-signal p {
  margin: 0;
  color: #606a79;
  font-size: 12px;
  line-height: 1.58;
}

.subagent-rail,
.detail-stack {
  display: grid;
  gap: 12px;
}

.subagent-rail-card,
.config-panel,
.editor-panel,
.detail-rail,
.test-panel {
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

.config-panel,
.editor-panel,
.detail-rail,
.test-panel {
  border-color: #ddd3c3;
  background: linear-gradient(180deg, rgba(255, 252, 247, 0.96) 0%, rgba(248, 243, 234, 0.94) 100%);
}

.sub-agent-config-editor {
  min-height: 300px;
}

.rule-controls,
.structured-card-list,
.route-actions {
  display: grid;
  gap: 10px;
}

.structured-form {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.structured-card-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 14px;
}

.structured-card {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.toggle-row,
.field-row {
  display: grid;
  gap: 6px;
}

.field-row span {
  color: #6f7a88;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.detail-stat-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
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

.test-result-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.response-card {
  grid-column: span 2;
}

@media (max-width: 1180px) {
  .subagent-hero-grid,
  .workbench-grid.wide-detail,
  .test-result-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .subagent-signal-grid,
  .structured-form,
  .structured-card-list,
  .detail-stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
