<template>
  <div class="workbench-grid wide-detail">
    <section class="panel">
      <div class="section-title">
        <div>
          <h2>Tool Registry</h2>
          <p>{{ filteredTools.length }} of {{ store.tools.length }} tools visible.</p>
        </div>
        <div class="toolbar">
          <input v-model="query" type="search" placeholder="Search tools" />
          <select v-model="sourceFilter">
            <option value="all">All sources</option>
            <option value="BUILTIN">Built-in</option>
            <option value="PLUGIN">Plugin</option>
            <option value="MCP">MCP</option>
          </select>
          <select v-model="riskFilter">
            <option value="all">All risks</option>
            <option value="SAFE_READ">Safe read</option>
            <option value="SESSION_ACTION">Session action</option>
            <option value="EXTERNAL_READ">External read</option>
            <option value="STATE_WRITE">State write</option>
            <option value="HIGH_RISK">High risk</option>
          </select>
          <select v-model="enabledFilter">
            <option value="all">All states</option>
            <option value="enabled">Enabled</option>
            <option value="disabled">Disabled</option>
          </select>
        </div>
      </div>

      <EmptyState v-if="filteredTools.length === 0" title="No matching tools" detail="Adjust search or filters to widen the registry view." />
      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Source</th>
              <th>Risk</th>
              <th>State</th>
              <th>Required</th>
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
              <td>{{ tool.source }}</td>
              <td>
                <span class="inline-status" :class="{ danger: tool.riskLevel === 'HIGH_RISK', warn: tool.riskLevel === 'STATE_WRITE' }">
                  {{ tool.riskLevel }}
                </span>
              </td>
              <td>
                <span class="inline-status" :class="{ ok: tool.effectiveEnabled, danger: !tool.effectiveEnabled }">
                  {{ tool.effectiveEnabled ? 'Enabled' : 'Disabled' }}
                </span>
              </td>
              <td>{{ tool.parameters.required.length }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <aside class="panel detail-panel">
      <div class="section-title">
        <div>
          <h2>{{ selectedTool?.name ?? 'Tool Detail' }}</h2>
          <p>{{ selectedTool ? selectedTool.source : 'Select a row to inspect policy and schema.' }}</p>
        </div>
      </div>

      <EmptyState v-if="!selectedTool" title="No tool selected" detail="Choose a tool from the registry table." />
      <div v-else class="detail-list">
        <div class="detail-item">
          <span>Description</span>
          <strong>{{ selectedTool.description }}</strong>
        </div>
        <div class="detail-item">
          <span>Policy</span>
          <div class="chip-row">
            <span class="inline-status" :class="{ ok: selectedTool.effectiveEnabled, danger: !selectedTool.effectiveEnabled }">
              {{ selectedTool.effectiveEnabled ? 'Enabled' : 'Disabled' }}
            </span>
            <span class="inline-status" :class="{ muted: selectedTool.defaultEnabled, warn: !selectedTool.defaultEnabled }">
              {{ selectedTool.defaultEnabled ? 'Default on' : 'Default off' }}
            </span>
            <span v-if="selectedTool.auditLog" class="inline-status muted">Audited</span>
          </div>
        </div>
        <div class="detail-item">
          <span>Risk level</span>
          <strong>{{ selectedTool.riskLevel }}</strong>
        </div>
        <div v-if="selectedTool.owner" class="detail-item">
          <span>Owner</span>
          <strong>{{ selectedTool.owner }}</strong>
        </div>
        <div class="detail-item">
          <span>Required parameters</span>
          <div class="chip-row">
            <span v-for="name in selectedTool.parameters.required" :key="name" class="chip">{{ name }}</span>
            <span v-if="selectedTool.parameters.required.length === 0" class="muted">None</span>
          </div>
        </div>
        <div v-if="selectedTool.requiredCapabilities.length" class="detail-item">
          <span>Capabilities</span>
          <div class="chip-row">
            <span v-for="name in selectedTool.requiredCapabilities" :key="name" class="chip">{{ name }}</span>
          </div>
        </div>
        <p v-if="selectedTool.statusReason" class="notice warning">{{ selectedTool.statusReason }}</p>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import EmptyState from '../components/EmptyState.vue';
import { useDashboardStore } from '../stores/dashboard';

const store = useDashboardStore();
const query = ref('');
const sourceFilter = ref('all');
const riskFilter = ref('all');
const enabledFilter = ref('all');
const selectedToolName = ref('');

const filteredTools = computed(() => {
  const normalizedQuery = query.value.trim().toLowerCase();
  return store.tools.filter((tool) => {
    const matchesQuery =
      normalizedQuery.length === 0 ||
      tool.name.toLowerCase().includes(normalizedQuery) ||
      tool.description.toLowerCase().includes(normalizedQuery);
    const matchesSource = sourceFilter.value === 'all' || tool.source === sourceFilter.value;
    const matchesRisk = riskFilter.value === 'all' || tool.riskLevel === riskFilter.value;
    const matchesEnabled =
      enabledFilter.value === 'all' ||
      (enabledFilter.value === 'enabled' && tool.effectiveEnabled) ||
      (enabledFilter.value === 'disabled' && !tool.effectiveEnabled);
    return matchesQuery && matchesSource && matchesRisk && matchesEnabled;
  });
});

const selectedTool = computed(() => filteredTools.value.find((tool) => tool.name === selectedToolName.value) ?? filteredTools.value[0] ?? null);

watch(filteredTools, (nextTools) => {
  if (!nextTools.some((tool) => tool.name === selectedToolName.value)) {
    selectedToolName.value = nextTools[0]?.name ?? '';
  }
});
</script>
