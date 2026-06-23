<template>
  <div class="grid sub-agent-layout">
    <section class="panel">
      <div class="section-title">
        <div>
          <h2>Orchestration Config</h2>
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
      <p v-if="saveNotice" class="notice">{{ saveNotice }}</p>
    </section>

    <section class="panel">
      <div class="section-title">
        <div>
          <h2>Rule Editor</h2>
          <p>Structured controls update the JSON draft.</p>
        </div>
      </div>

      <div class="rule-controls">
        <label class="toggle-row">
          <input type="checkbox" :checked="parsedConfig.enabled" :disabled="!canEditStructured" @change="setEnabled(($event.target as HTMLInputElement).checked)" />
          <span>Enable sub-agent routing</span>
        </label>

        <label class="field-row">
          <span>Default agent</span>
          <select :value="parsedConfig.defaultAgentName" :disabled="!canEditStructured || parsedConfig.agents.length === 0" @change="setDefaultAgent(($event.target as HTMLSelectElement).value)">
            <option value="">Primary agent</option>
            <option v-for="agent in parsedConfig.agents" :key="agent.name" :value="agent.name">{{ agent.name }}</option>
          </select>
        </label>
      </div>

      <form class="structured-form" @submit.prevent="addAgent">
        <input v-model="newAgentName" type="text" placeholder="Agent name" />
        <input v-model="newAgentDescription" type="text" placeholder="Description" />
        <button type="submit" class="primary" :disabled="!canEditStructured || newAgentName.trim().length === 0">Add Agent</button>
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
        <button type="submit" class="primary" :disabled="!canEditStructured || newRouteName.trim().length === 0 || newRouteTarget.length === 0">Add Route</button>
      </form>
    </section>

    <section class="panel">
      <div class="section-title">
        <div>
          <h2>Route Summary</h2>
          <p>Parsed from the current editor draft.</p>
        </div>
      </div>

      <EmptyState v-if="parsedConfig.agents.length === 0" title="No sub-agents" detail="Add agents to the JSON config to enable routing targets." />
      <div v-else class="grid list-grid sub-agent-list">
        <article v-for="agent in parsedConfig.agents" :key="agent.name" class="card">
          <div class="section-title">
            <h3>{{ agent.name }}</h3>
            <span class="inline-status" :class="agent.enabled ? 'ok' : 'muted'">{{ agent.enabled ? 'Enabled' : 'Disabled' }}</span>
          </div>
          <p>{{ agent.description || agent.agent.instructions || 'No description' }}</p>
          <p>{{ agent.agent.providerName }} / {{ agent.agent.model }}</p>
        </article>
      </div>

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
                <select :value="route.targetAgentName" :disabled="!canEditStructured" @change="updateRoute(route.name, { targetAgentName: ($event.target as HTMLSelectElement).value })">
                  <option v-for="agent in parsedConfig.agents" :key="agent.name" :value="agent.name">{{ agent.name }}</option>
                </select>
              </td>
              <td>
                <input :value="route.keywords.join(', ')" :disabled="!canEditStructured" @change="updateRoute(route.name, { keywords: splitKeywords(($event.target as HTMLInputElement).value) })" />
              </td>
              <td>
                <input class="number-input" type="number" :value="route.priority" :disabled="!canEditStructured" @change="updateRoute(route.name, { priority: Number(($event.target as HTMLInputElement).value) || 0 })" />
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

    <section class="panel test-panel">
      <div class="section-title">
        <div>
          <h2>Routing Test</h2>
          <p>Runs the draft config through the Dashboard sub-agent API.</p>
        </div>
      </div>

      <form class="chat-form" @submit.prevent="runTest">
        <input v-model="testMessage" type="text" placeholder="Message to route..." />
        <button type="submit" class="primary" :disabled="testing || testMessage.trim().length === 0">Test</button>
      </form>

      <EmptyState v-if="!testResult" title="No test result" detail="Submit a message to inspect selected agent and route events." />
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
    agents: config.agents.map((agent) => (
      agent.name === name ? { ...agent, enabled: !agent.enabled } : agent
    )),
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
    routes: config.routes.map((route) => (
      route.name === name ? { ...route, ...patch } : route
    )),
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
