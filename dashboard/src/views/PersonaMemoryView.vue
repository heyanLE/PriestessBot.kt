<template>
  <div class="memory-command">
    <section class="panel memory-hero">
      <div class="memory-hero-grid">
        <div class="memory-copy">
          <div class="memory-band">
            <span>Assets</span>
            <span>Workspace {{ workspaceId }}</span>
          </div>

          <h2>Persona and memory registry</h2>
          <p>
            Align persona overlays and retrieval records. Keep injection tone, memory scope, and
            search relevance visible while you edit the registry.
          </p>

          <div class="grid memory-signal-grid">
            <article v-for="signal in memorySignals" :key="signal.label" class="card memory-signal" :class="`tone-${signal.tone}`">
              <span>{{ signal.label }}</span>
              <strong>{{ signal.value }}</strong>
              <p>{{ signal.detail }}</p>
            </article>
          </div>
        </div>

        <aside class="memory-rail">
          <article class="card memory-rail-card">
            <div class="section-title compact">
              <div>
                <h3>What matters here</h3>
                <p>Persona tone and memory scope should stay readable, scoped, and easy to test.</p>
              </div>
            </div>

            <div class="rail-list">
              <div class="rail-item">
                <span>Active workspace</span>
                <strong>{{ workspaceId }}</strong>
              </div>
              <div class="rail-item">
                <span>Selected persona</span>
                <strong>{{ selectedPersona?.name ?? 'None' }}</strong>
              </div>
              <div class="rail-item">
                <span>Selected memory</span>
                <strong>{{ selectedMemory?.type ?? 'None' }}</strong>
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
            <h2>Persona registry</h2>
            <p>{{ filteredPersonas.length }} persona record(s) visible in {{ workspaceId }}.</p>
          </div>
          <div class="toolbar">
            <input v-model="workspaceId" type="search" aria-label="Workspace id" placeholder="workspace id" />
            <input v-model="personaQuery" type="search" aria-label="Search personas" placeholder="Search personas" />
            <button type="button" @click="loadAll" :disabled="loading">Refresh</button>
          </div>
        </div>

        <p v-if="notice" class="notice ok">{{ notice }}</p>
        <p v-if="error" class="notice error">{{ error }}</p>

        <EmptyState
          v-if="filteredPersonas.length === 0"
          title="No personas"
          detail="Create a persona or adjust the workspace and search filters."
        />

        <div v-else class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>Name</th>
                <th>State</th>
                <th>Agents</th>
                <th>Updated</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="persona in filteredPersonas"
                :key="persona.id"
                class="clickable-row"
                :class="{ selected: selectedPersonaId === persona.id }"
                @click="selectPersona(persona)"
              >
                <td>
                  <strong>{{ persona.name }}</strong>
                  <p class="muted">{{ persona.description || persona.id }}</p>
                </td>
                <td>
                  <span class="inline-status" :class="{ ok: persona.enabled, muted: !persona.enabled }">
                    {{ persona.enabled ? 'Enabled' : 'Disabled' }}
                  </span>
                </td>
                <td>{{ persona.agentNames.length || 'All' }}</td>
                <td>{{ formatTime(persona.updatedAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <form class="stacked-form persona-form" @submit.prevent="savePersona">
          <div class="section-title compact">
            <div>
              <h2>{{ personaDraft.id ? 'Edit Persona' : 'Create Persona' }}</h2>
              <p>Persona scope, tone, boundaries, and injected prompt template.</p>
            </div>
            <button type="button" @click="resetPersonaDraft">New</button>
          </div>
          <div class="form-grid">
            <label>
              <span>Name</span>
              <input v-model="personaDraft.name" required />
            </label>
            <label>
              <span>Agents</span>
              <input v-model="personaAgentsText" placeholder="comma separated or blank for all" />
            </label>
            <label>
              <span>Description</span>
              <input v-model="personaDraft.description" />
            </label>
            <label>
              <span>Tone</span>
              <input v-model="personaDraft.tone" />
            </label>
            <label class="full-span">
              <span>Boundaries</span>
              <input v-model="personaBoundariesText" placeholder="comma separated boundaries" />
            </label>
            <label class="full-span">
              <span>System prompt template</span>
              <textarea v-model="personaDraft.systemPromptTemplate" class="compact-textarea"></textarea>
            </label>
          </div>
          <div class="toolbar">
            <label class="check-row">
              <input v-model="personaDraft.enabled" type="checkbox" />
              <span>Enabled</span>
            </label>
            <button type="submit" class="primary" :disabled="savingPersona || personaDraft.name.trim().length === 0">
              {{ savingPersona ? 'Saving' : 'Save Persona' }}
            </button>
            <button v-if="personaDraft.id" type="button" @click="deleteSelectedPersona" :disabled="savingPersona">Delete</button>
          </div>
        </form>
      </section>

      <aside class="panel detail-panel detail-rail">
        <div class="section-title">
          <div>
            <h2>{{ selectedPersona?.name ?? 'Persona detail rail' }}</h2>
            <p>{{ selectedPersona ? selectedPersona.id : 'Select a persona to inspect injection inputs.' }}</p>
          </div>
        </div>

        <EmptyState
          v-if="!selectedPersona"
          title="No persona selected"
          detail="Choose a row to inspect tone, boundaries, and scoped agents."
        />

        <div v-else class="detail-stack">
          <article class="card memory-rail-card">
            <div class="detail-list">
              <div class="detail-item">
                <span>Workspace</span>
                <strong>{{ selectedPersona.workspaceId }}</strong>
              </div>
              <div class="detail-item">
                <span>Tone</span>
                <strong>{{ selectedPersona.tone || 'Not set' }}</strong>
              </div>
              <div class="detail-item">
                <span>Agents</span>
                <div class="chip-row">
                  <span v-for="agent in selectedPersona.agentNames" :key="agent" class="chip">{{ agent }}</span>
                  <span v-if="selectedPersona.agentNames.length === 0" class="muted">All agents</span>
                </div>
              </div>
              <div class="detail-item">
                <span>Boundaries</span>
                <div class="chip-row">
                  <span v-for="boundary in selectedPersona.boundaries" :key="boundary" class="chip">{{ boundary }}</span>
                  <span v-if="selectedPersona.boundaries.length === 0" class="muted">None</span>
                </div>
              </div>
              <div class="detail-item">
                <span>Prompt</span>
                <pre class="code-block">{{ selectedPersona.systemPromptTemplate || 'No template configured.' }}</pre>
              </div>
            </div>
          </article>
        </div>
      </aside>
    </div>

    <div class="workbench-grid wide-detail">
      <section class="panel registry-panel">
        <div class="section-title">
          <div>
            <h2>Memory workbench</h2>
            <p>{{ filteredMemories.length }} memory record(s) visible for the current scope.</p>
          </div>
          <div class="toolbar">
            <input v-model="memoryQuery" type="search" aria-label="Search memory" placeholder="Search memory" />
            <select v-model="memoryTypeFilter" aria-label="Memory type">
              <option value="all">All types</option>
              <option value="FACT">Fact</option>
              <option value="PREFERENCE">Preference</option>
              <option value="EVENT">Event</option>
              <option value="SUMMARY">Summary</option>
            </select>
            <button type="button" @click="searchMemory" :disabled="loadingMemory || memoryQuery.trim().length === 0">Search</button>
            <button type="button" @click="expireMemory" :disabled="loadingMemory">Expire</button>
          </div>
        </div>

        <div class="scope-grid">
          <input v-model="memoryScope.platformId" placeholder="platform id" aria-label="Platform id" />
          <input v-model="memoryScope.sessionId" placeholder="session id" aria-label="Session id" />
          <input v-model="memoryScope.userId" placeholder="user id" aria-label="User id" />
          <input v-model="memoryScope.agentName" placeholder="agent name" aria-label="Agent name" />
        </div>

        <EmptyState
          v-if="filteredMemories.length === 0"
          title="No visible memories"
          detail="Save a memory or adjust scope filters to list scoped records."
        />

        <div v-else class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>Content</th>
                <th>Scope</th>
                <th>Type</th>
                <th>Confidence</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="memory in filteredMemories"
                :key="memory.id"
                class="clickable-row"
                :class="{ selected: selectedMemoryId === memory.id }"
                @click="selectedMemoryId = memory.id"
              >
                <td>
                  <strong>{{ memory.content }}</strong>
                  <p class="muted">{{ memory.tags.join(', ') || memory.id }}</p>
                </td>
                <td>{{ memory.scope }}</td>
                <td>{{ memory.type }}</td>
                <td>{{ Math.round(memory.confidence * 100) }}%</td>
              </tr>
            </tbody>
          </table>
        </div>

        <form class="stacked-form persona-form" @submit.prevent="saveMemory">
          <div class="section-title compact">
            <div>
              <h2>Save Memory</h2>
              <p>Write a scoped fact, preference, event, or summary into the runtime store.</p>
            </div>
          </div>
          <div class="form-grid">
            <label>
              <span>Scope</span>
              <select v-model="memoryDraft.scope">
                <option value="GLOBAL">Global</option>
                <option value="PLATFORM">Platform</option>
                <option value="SESSION">Session</option>
                <option value="USER">User</option>
                <option value="AGENT">Agent</option>
              </select>
            </label>
            <label>
              <span>Type</span>
              <select v-model="memoryDraft.type">
                <option value="FACT">Fact</option>
                <option value="PREFERENCE">Preference</option>
                <option value="EVENT">Event</option>
                <option value="SUMMARY">Summary</option>
              </select>
            </label>
            <label>
              <span>Tags</span>
              <input v-model="memoryTagsText" placeholder="comma separated" />
            </label>
            <label>
              <span>Confidence</span>
              <input v-model.number="memoryDraft.confidence" type="number" min="0" max="1" step="0.05" />
            </label>
            <label class="full-span">
              <span>Content</span>
              <textarea v-model="memoryDraft.content" class="compact-textarea" required></textarea>
            </label>
          </div>
          <button type="submit" class="primary" :disabled="savingMemory || memoryDraft.content.trim().length === 0">
            {{ savingMemory ? 'Saving' : 'Save Memory' }}
          </button>
        </form>
      </section>

      <aside class="panel detail-panel detail-rail">
        <div class="section-title">
          <div>
            <h2>{{ selectedMemory ? selectedMemory.type : 'Memory detail rail' }}</h2>
            <p>{{ selectedMemory ? selectedMemory.id : 'Select a memory row or run a search.' }}</p>
          </div>
        </div>

        <EmptyState
          v-if="!selectedMemory"
          title="No memory selected"
          detail="Choose a memory row or run a scoped search."
        />

        <div v-else class="detail-stack">
          <article class="card memory-rail-card">
            <div class="detail-list">
              <div class="detail-item">
                <span>Content</span>
                <strong>{{ selectedMemory.content }}</strong>
              </div>
              <div class="detail-item">
                <span>Scope</span>
                <div class="chip-row">
                  <span class="chip">{{ selectedMemory.scope }}</span>
                  <span v-if="selectedMemory.platformId" class="chip">platform {{ selectedMemory.platformId }}</span>
                  <span v-if="selectedMemory.sessionId" class="chip">session {{ selectedMemory.sessionId }}</span>
                  <span v-if="selectedMemory.userId" class="chip">user {{ selectedMemory.userId }}</span>
                  <span v-if="selectedMemory.agentName" class="chip">agent {{ selectedMemory.agentName }}</span>
                </div>
              </div>
              <div class="detail-item">
                <span>Tags</span>
                <div class="chip-row">
                  <span v-for="tag in selectedMemory.tags" :key="tag" class="chip">{{ tag }}</span>
                  <span v-if="selectedMemory.tags.length === 0" class="muted">None</span>
                </div>
              </div>
              <div class="detail-item">
                <span>Updated</span>
                <strong>{{ formatTime(selectedMemory.updatedAt) }}</strong>
              </div>
              <button type="button" @click="deleteSelectedMemory">Delete Memory</button>
            </div>
          </article>

          <div v-if="searchResults.length" class="search-results">
            <div class="section-title compact">
              <div>
                <h2>Search results</h2>
                <p>{{ searchResults.length }} matched record(s).</p>
              </div>
            </div>
            <button
              v-for="result in searchResults"
              :key="result.record.id"
              type="button"
              class="result-row"
              @click="selectedMemoryId = result.record.id"
            >
              <strong>{{ result.record.content }}</strong>
              <span>{{ result.matchReason }} / {{ result.score.toFixed(2) }}</span>
            </button>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import {
  dashboardApi,
  type MemoryRecord,
  type MemoryScope,
  type MemorySearchResult,
  type MemoryType,
  type Persona,
  type PersonaUpsertDto,
} from '../api/dashboard';
import EmptyState from '../components/EmptyState.vue';

const workspaceId = ref('default');
const personas = ref<Persona[]>([]);
const memories = ref<MemoryRecord[]>([]);
const searchResults = ref<MemorySearchResult[]>([]);
const selectedPersonaId = ref('');
const selectedMemoryId = ref('');
const personaQuery = ref('');
const memoryQuery = ref('');
const memoryTypeFilter = ref<MemoryType | 'all'>('all');
const personaAgentsText = ref('');
const personaBoundariesText = ref('');
const memoryTagsText = ref('');
const loading = ref(false);
const loadingMemory = ref(false);
const savingPersona = ref(false);
const savingMemory = ref(false);
const notice = ref('');
const error = ref('');

const personaDraft = reactive<PersonaUpsertDto>({
  workspaceId: 'default',
  name: '',
  description: '',
  tone: '',
  boundaries: [],
  systemPromptTemplate: '',
  enabled: true,
  agentNames: [],
});

const memoryScope = reactive({
  platformId: '',
  sessionId: '',
  userId: '',
  agentName: '',
});

const memoryDraft = reactive({
  content: '',
  type: 'FACT' as MemoryType,
  scope: 'GLOBAL' as MemoryScope,
  confidence: 1,
});

const enabledPersonas = computed(() => personas.value.filter((persona) => persona.enabled).length);

const filteredPersonas = computed(() => {
  const query = personaQuery.value.trim().toLowerCase();
  return personas.value.filter((persona) => {
    if (!query) return true;
    return [persona.name, persona.description, persona.tone, persona.id, ...persona.agentNames]
      .join(' ')
      .toLowerCase()
      .includes(query);
  });
});

const filteredMemories = computed(() => {
  const query = memoryQuery.value.trim().toLowerCase();
  return memories.value.filter((memory) => {
    const matchesType = memoryTypeFilter.value === 'all' || memory.type === memoryTypeFilter.value;
    const matchesQuery =
      !query || [memory.content, memory.id, ...memory.tags, memory.scope, memory.type].join(' ').toLowerCase().includes(query);
    return matchesType && matchesQuery;
  });
});

const selectedPersona = computed(
  () => personas.value.find((persona) => persona.id === selectedPersonaId.value) ?? filteredPersonas.value[0] ?? null,
);
const selectedMemory = computed(
  () =>
    memories.value.find((memory) => memory.id === selectedMemoryId.value) ??
    searchResults.value.find((result) => result.record.id === selectedMemoryId.value)?.record ??
    filteredMemories.value[0] ??
    null,
);

const memorySignals = computed(() => [
  {
    label: 'Personas',
    value: String(personas.value.length),
    detail: `${enabledPersonas.value} enabled persona overlay(s) in the current registry.`,
    tone: personas.value.length > 0 ? 'ok' : 'muted',
  },
  {
    label: 'Visible memories',
    value: String(memories.value.length),
    detail: 'Scoped memory records currently loaded for the active filter.',
    tone: memories.value.length > 0 ? 'ok' : 'muted',
  },
  {
    label: 'Search matches',
    value: String(searchResults.value.length),
    detail: searchResults.value.length > 0 ? 'Scoped relevance test results are available.' : 'Run a search to inspect ranking behavior.',
    tone: searchResults.value.length > 0 ? 'warn' : 'muted',
  },
  {
    label: 'Workspace',
    value: workspaceId.value,
    detail: 'Current workspace scope for persona and memory operations.',
    tone: 'muted',
  },
]);

async function loadAll() {
  loading.value = true;
  error.value = '';
  try {
    personaDraft.workspaceId = workspaceId.value;
    const response = await dashboardApi.personas(workspaceId.value);
    personas.value = response.personas;
    selectedPersonaId.value = personas.value[0]?.id ?? '';
    await loadMemories();
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : String(cause);
  } finally {
    loading.value = false;
  }
}

async function loadMemories() {
  loadingMemory.value = true;
  try {
    const response = await dashboardApi.memories({
      workspaceId: workspaceId.value,
      platformId: normalize(memoryScope.platformId),
      sessionId: normalize(memoryScope.sessionId),
      userId: normalize(memoryScope.userId),
      agentName: normalize(memoryScope.agentName),
      type: memoryTypeFilter.value,
      limit: 80,
    });
    memories.value = response.memories;
    selectedMemoryId.value = memories.value[0]?.id ?? '';
  } finally {
    loadingMemory.value = false;
  }
}

function selectPersona(persona: Persona) {
  selectedPersonaId.value = persona.id;
  Object.assign(personaDraft, {
    id: persona.id,
    workspaceId: persona.workspaceId,
    name: persona.name,
    description: persona.description,
    tone: persona.tone,
    boundaries: [...persona.boundaries],
    systemPromptTemplate: persona.systemPromptTemplate,
    enabled: persona.enabled,
    agentNames: [...persona.agentNames],
  });
  personaAgentsText.value = persona.agentNames.join(', ');
  personaBoundariesText.value = persona.boundaries.join(', ');
}

function resetPersonaDraft() {
  selectedPersonaId.value = '';
  Object.assign(personaDraft, {
    id: undefined,
    workspaceId: workspaceId.value,
    name: '',
    description: '',
    tone: '',
    boundaries: [],
    systemPromptTemplate: '',
    enabled: true,
    agentNames: [],
  });
  personaAgentsText.value = '';
  personaBoundariesText.value = '';
}

async function savePersona() {
  savingPersona.value = true;
  notice.value = '';
  error.value = '';
  try {
    const body: PersonaUpsertDto = {
      ...personaDraft,
      workspaceId: workspaceId.value,
      agentNames: splitList(personaAgentsText.value),
      boundaries: splitList(personaBoundariesText.value),
    };
    const saved = body.id ? await dashboardApi.updatePersona(body.id, body) : await dashboardApi.savePersona(body);
    notice.value = `Saved persona ${saved.name}.`;
    await loadAll();
    selectPersona(saved);
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : String(cause);
  } finally {
    savingPersona.value = false;
  }
}

async function deleteSelectedPersona() {
  if (!personaDraft.id) return;
  await dashboardApi.deletePersona(personaDraft.id);
  notice.value = `Deleted persona ${personaDraft.name}.`;
  resetPersonaDraft();
  await loadAll();
}

async function saveMemory() {
  savingMemory.value = true;
  notice.value = '';
  error.value = '';
  try {
    const saved = await dashboardApi.saveMemory({
      content: memoryDraft.content,
      type: memoryDraft.type,
      scope: memoryDraft.scope,
      workspaceId: workspaceId.value,
      platformId: normalize(memoryScope.platformId),
      sessionId: normalize(memoryScope.sessionId),
      userId: normalize(memoryScope.userId),
      agentName: normalize(memoryScope.agentName),
      tags: splitList(memoryTagsText.value),
      confidence: memoryDraft.confidence,
    });
    notice.value = `Saved memory ${saved.id}.`;
    memoryDraft.content = '';
    memoryTagsText.value = '';
    await loadMemories();
    selectedMemoryId.value = saved.id;
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : String(cause);
  } finally {
    savingMemory.value = false;
  }
}

async function searchMemory() {
  loadingMemory.value = true;
  error.value = '';
  try {
    const response = await dashboardApi.searchMemory({
      query: memoryQuery.value,
      workspaceId: workspaceId.value,
      platformId: normalize(memoryScope.platformId),
      sessionId: normalize(memoryScope.sessionId),
      userId: normalize(memoryScope.userId),
      agentName: normalize(memoryScope.agentName),
      type: memoryTypeFilter.value === 'all' ? undefined : memoryTypeFilter.value,
      limit: 10,
    });
    searchResults.value = response.results;
    selectedMemoryId.value = response.results[0]?.record.id ?? selectedMemoryId.value;
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : String(cause);
  } finally {
    loadingMemory.value = false;
  }
}

async function deleteSelectedMemory() {
  if (!selectedMemory.value) return;
  await dashboardApi.deleteMemory(selectedMemory.value.id, {
    workspaceId: workspaceId.value,
    platformId: normalize(memoryScope.platformId),
    sessionId: normalize(memoryScope.sessionId),
    userId: normalize(memoryScope.userId),
    agentName: normalize(memoryScope.agentName),
  });
  notice.value = `Deleted memory ${selectedMemory.value.id}.`;
  await loadMemories();
}

async function expireMemory() {
  const response = await dashboardApi.expireMemory();
  notice.value = `Expired ${response.expired} memory record(s).`;
  await loadMemories();
}

function splitList(value: string) {
  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

function normalize(value: string) {
  return value.trim() || undefined;
}

function formatTime(value: number) {
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(value);
}

watch(workspaceId, () => {
  personaDraft.workspaceId = workspaceId.value;
});

watch(memoryTypeFilter, () => void loadMemories());

onMounted(() => {
  void loadAll();
});
</script>

<style scoped>
.memory-command {
  display: grid;
  gap: 14px;
}

.memory-hero {
  border-color: #d7cebd;
  background:
    linear-gradient(135deg, rgba(255, 252, 246, 0.98) 0%, rgba(247, 241, 231, 0.98) 55%, rgba(240, 246, 248, 0.98) 100%);
}

.memory-hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.18fr) minmax(280px, 0.82fr);
  gap: 14px;
}

.memory-band {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.memory-band span,
.memory-signal span,
.rail-item span {
  color: #887152;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.memory-band span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid rgba(182, 159, 111, 0.34);
  background: rgba(255, 251, 245, 0.92);
}

.memory-copy h2 {
  margin: 0;
  color: #18304c;
  font-size: clamp(28px, 2vw + 18px, 40px);
  line-height: 0.98;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.memory-copy > p {
  margin: 12px 0 0;
  color: #5c6776;
  line-height: 1.66;
}

.memory-signal-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 16px;
}

.memory-signal {
  display: grid;
  gap: 8px;
  border-top: 3px solid #98a2b0;
  background: rgba(255, 252, 247, 0.9);
}

.memory-signal.tone-ok {
  border-top-color: #4c8661;
}

.memory-signal.tone-warn {
  border-top-color: #bb8524;
}

.memory-signal.tone-muted {
  border-top-color: #98a2b0;
}

.memory-signal strong {
  color: #17304d;
  font-size: 28px;
  line-height: 1;
  overflow-wrap: anywhere;
}

.memory-signal p {
  margin: 0;
  color: #606a79;
  font-size: 12px;
  line-height: 1.58;
}

.memory-rail,
.detail-stack {
  display: grid;
  gap: 12px;
}

.memory-rail-card {
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

.memory-command .memory-hero,
.memory-command .memory-rail-card,
.memory-command .registry-panel,
.memory-command .detail-rail,
.memory-command .memory-signal,
.memory-command .rail-item {
  border-color: var(--line);
  background: rgba(255, 255, 255, 0.92);
}

.memory-command .memory-band span,
.memory-command .memory-signal span,
.memory-command .rail-item span {
  color: var(--weak);
  font-weight: 600;
  letter-spacing: 0.06em;
}

.memory-command .memory-copy h2 {
  color: var(--text-strong);
  letter-spacing: -0.04em;
  text-transform: none;
}

.memory-command .memory-copy > p,
.memory-command .rail-item strong,
.memory-command .memory-signal p {
  color: var(--muted);
}

@media (max-width: 1180px) {
  .memory-hero-grid,
  .workbench-grid.wide-detail {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .memory-signal-grid {
    grid-template-columns: 1fr;
  }
}
</style>
