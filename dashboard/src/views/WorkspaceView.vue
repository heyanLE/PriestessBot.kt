<template>
  <div class="workbench-grid wide-detail">
    <section class="panel">
      <div class="section-title">
        <div>
          <h2>Workspaces</h2>
          <p>{{ store.workspaces.workspaces.length }} configured runtime scopes.</p>
        </div>
        <div class="toolbar">
          <button type="button" @click="loadWorkspaces" :disabled="loading">Refresh</button>
          <button type="button" class="primary" @click="reloadAll" :disabled="loading || reloadingId !== null">Reload All</button>
        </div>
      </div>

      <p v-if="notice" class="notice ok">{{ notice }}</p>
      <p v-if="error" class="notice error">{{ error }}</p>

      <EmptyState
        v-if="store.workspaces.workspaces.length === 0"
        title="No workspaces"
        detail="Workspace status will appear after the backend publishes snapshots."
      />

      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>State</th>
              <th>Snapshot</th>
              <th>Loaded</th>
              <th>Last Reload</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="workspace in store.workspaces.workspaces"
              :key="workspace.id"
              class="clickable-row"
              :class="{ selected: selectedWorkspaceId === workspace.id }"
              @click="selectWorkspace(workspace.id)"
            >
              <td>
                <strong>{{ workspace.name }}</strong>
                <p class="muted"><code>{{ workspace.id }}</code></p>
              </td>
              <td>
                <StatusDot :label="workspace.enabled ? 'Enabled' : 'Disabled'" :tone="workspace.enabled ? 'ok' : 'muted'" />
              </td>
              <td>{{ workspace.activeSnapshotVersion ?? 'none' }}</td>
              <td>{{ formatTime(workspace.loadedAt) }}</td>
              <td>
                <StatusDot
                  v-if="workspace.lastReload"
                  :label="workspace.lastReload.status"
                  :tone="workspace.lastReload.success ? 'ok' : 'error'"
                />
                <span v-else class="muted">none</span>
                <p v-if="workspace.lastReload?.errorSummary" class="muted">{{ workspace.lastReload.errorSummary }}</p>
              </td>
              <td>
                <button type="button" class="primary" :disabled="reloadingId !== null" @click.stop="reloadOne(workspace.id)">
                  {{ reloadingId === workspace.id ? 'Reloading' : 'Reload' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="panel detail-panel workspace-detail">
      <div class="section-title">
        <div>
          <h2>{{ detail?.status.name ?? 'Workspace Detail' }}</h2>
          <p v-if="detail">{{ detail.providerName }} · snapshot {{ detail.status.activeSnapshotVersion ?? 'none' }}</p>
          <p v-else>Select a workspace to inspect scoped resources.</p>
        </div>
      </div>

      <EmptyState
        v-if="!detail && !loadingDetail"
        title="No workspace selected"
        detail="Choose a workspace from the list to inspect agents, tools, skills, MCP, personas, and memory policy."
      />

      <div v-if="detail" class="grid detail-grid">
        <article class="card metric">
          <strong>{{ detail.agents.length }}</strong>
          <span>Agents</span>
        </article>
        <article class="card metric">
          <strong>{{ detail.tools.length }}</strong>
          <span>Tools</span>
        </article>
        <article class="card metric">
          <strong>{{ detail.skills.length }}</strong>
          <span>Skills</span>
        </article>
        <article class="card metric">
          <strong>{{ detail.mcpServers.length }}</strong>
          <span>MCP servers</span>
        </article>

        <article class="card resource-card">
          <h3>Agents</h3>
          <ResourceList :items="detail.agents" empty-label="No scoped agents" />
        </article>
        <article class="card resource-card">
          <h3>Tools</h3>
          <ResourceList :items="detail.tools" empty-label="No scoped tools" />
        </article>
        <article class="card resource-card">
          <h3>Skills</h3>
          <ResourceList :items="detail.skills" empty-label="No scoped skills" />
          <div v-if="Object.keys(detail.skillSettings).length" class="resource-list">
            <span v-for="(settings, name) in detail.skillSettings" :key="name" class="chip">
              {{ name }} settings {{ Object.keys(settings).length }}
            </span>
          </div>
        </article>
        <article class="card resource-card">
          <h3>MCP</h3>
          <ResourceList :items="detail.mcpServers" empty-label="No MCP servers" />
          <div v-if="detail.mcpServerDetails.length" class="resource-list">
            <span v-for="server in detail.mcpServerDetails" :key="server.id" class="chip">
              {{ server.id }} · {{ server.transport }}
            </span>
          </div>
        </article>
        <article class="card resource-card">
          <h3>Personas</h3>
          <ResourceList :items="detail.personas" empty-label="No personas" />
        </article>
        <article class="card resource-card">
          <h3>Memory</h3>
          <p>{{ detail.memory.enabled ? 'Enabled' : 'Disabled' }}</p>
          <p>Scopes: {{ detail.memory.allowedScopes.join(', ') || 'none' }}</p>
          <p>Knowledge bases: {{ detail.memory.knowledgeBaseIds.join(', ') || 'none' }}</p>
          <p>Max injected: {{ detail.memory.maxInjectedMemories }}</p>
        </article>
      </div>

      <section v-if="detail?.status.diagnostics.length" class="notice">
        <strong>Diagnostics</strong>
        <ul>
          <li v-for="item in detail.status.diagnostics" :key="item">{{ item }}</li>
        </ul>
      </section>

      <section v-if="lastReload" class="notice" :class="{ error: !lastReload.success, ok: lastReload.success }">
        <strong>Reload {{ lastReload.status }}</strong>
        <p>Workspace {{ lastReload.workspaceId }} · snapshot {{ lastReload.snapshotVersion ?? 'unchanged' }}</p>
        <p v-if="lastReload.errorSummary">{{ lastReload.errorSummary }}</p>
        <div v-if="lastReload.plan" class="reload-plan">
          <span>Added {{ lastReload.plan.added.length }}</span>
          <span>Removed {{ lastReload.plan.removed.length }}</span>
          <span>Modified {{ lastReload.plan.modified.length }}</span>
        </div>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, onMounted, ref, watch } from 'vue';
import { dashboardApi, type WorkspaceDetailDto, type WorkspaceReloadResult } from '../api/dashboard';
import EmptyState from '../components/EmptyState.vue';
import StatusDot from '../components/StatusDot.vue';
import { useDashboardStore } from '../stores/dashboard';

const store = useDashboardStore();
const selectedWorkspaceId = ref('');
const detail = ref<WorkspaceDetailDto | null>(null);
const loading = ref(false);
const loadingDetail = ref(false);
const reloadingId = ref<string | null>(null);
const lastReload = ref<WorkspaceReloadResult | null>(null);
const notice = ref('');
const error = ref('');

const selectedWorkspace = computed(() =>
  store.workspaces.workspaces.find((workspace) => workspace.id === selectedWorkspaceId.value) ?? null,
);

const ResourceList = defineComponent({
  props: {
    items: { type: Array<string>, required: true },
    emptyLabel: { type: String, required: true },
  },
  setup(props) {
    return () =>
      props.items.length === 0
        ? h('p', { class: 'muted' }, props.emptyLabel)
        : h(
            'div',
            { class: 'resource-list' },
            props.items.map((item) => h('span', { class: 'inline-status muted', key: item }, item)),
          );
  },
});

async function loadWorkspaces() {
  loading.value = true;
  error.value = '';
  try {
    await store.loadWorkspaces();
    if (!selectedWorkspaceId.value && store.workspaces.workspaces.length > 0) {
      selectedWorkspaceId.value = store.workspaces.workspaces[0].id;
    }
    await loadDetail();
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : String(cause);
  } finally {
    loading.value = false;
  }
}

async function loadDetail() {
  if (!selectedWorkspaceId.value) {
    detail.value = null;
    return;
  }
  loadingDetail.value = true;
  try {
    detail.value = await dashboardApi.workspaceDetail(selectedWorkspaceId.value);
  } finally {
    loadingDetail.value = false;
  }
}

function selectWorkspace(id: string) {
  selectedWorkspaceId.value = id;
}

async function reloadOne(id: string) {
  reloadingId.value = id;
  notice.value = '';
  error.value = '';
  try {
    lastReload.value = await store.reloadWorkspace(id);
    notice.value = lastReload.value.success ? `Reloaded ${id}.` : '';
    await loadDetail();
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : String(cause);
  } finally {
    reloadingId.value = null;
  }
}

async function reloadAll() {
  reloadingId.value = '*';
  notice.value = '';
  error.value = '';
  try {
    const results = await store.reloadWorkspaces();
    const failed = results.filter((result) => !result.success);
    lastReload.value = results.find((result) => result.workspaceId === selectedWorkspaceId.value) ?? results[0] ?? null;
    notice.value = failed.length === 0 ? `Reloaded ${results.length} workspace(s).` : '';
    if (failed.length > 0) error.value = `${failed.length} workspace reload(s) failed.`;
    await loadDetail();
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : String(cause);
  } finally {
    reloadingId.value = null;
  }
}

function formatTime(value?: number) {
  if (!value) return 'not loaded';
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(value);
}

watch(selectedWorkspace, () => void loadDetail());

onMounted(() => {
  void loadWorkspaces();
});
</script>

<style scoped>
.detail-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.resource-card {
  min-height: 132px;
  grid-column: span 2;
}

.resource-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.reload-plan {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 8px;
}

@media (max-width: 980px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .resource-card {
    grid-column: auto;
  }
}
</style>
