<template>
  <section class="panel">
    <div class="section-title">
      <div>
        <h2>Runtime Config</h2>
        <p>Edit the active JSON config. Sensitive values are returned by the backend as-is.</p>
      </div>
      <div class="toolbar">
        <button type="button" @click="resetDraft()">Reset</button>
        <button type="button" class="primary" @click="saveDraft()">Save</button>
      </div>
    </div>
    <p v-if="notice" class="notice ok">{{ notice }}</p>
    <p v-if="error" class="notice error">{{ error }}</p>
    <textarea v-model="draft" spellcheck="false"></textarea>
  </section>

  <section class="panel">
    <div class="section-title">
      <div>
        <h2>Config Backups</h2>
        <p>{{ backupSummary }}</p>
      </div>
      <div class="toolbar">
        <button type="button" @click="loadBackups()" :disabled="loadingBackups">Refresh</button>
      </div>
    </div>

    <EmptyState
      v-if="store.configBackups.length === 0"
      title="No config backups"
      detail="A backup is created before each saved config replacement."
    />
    <div v-else class="route-table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>Backup</th>
            <th>Created</th>
            <th>Size</th>
            <th>Path</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="backup in store.configBackups" :key="backup.id">
            <td>
              <code>{{ backup.id }}</code>
            </td>
            <td>{{ formatBackupTime(backup.createdAt) }}</td>
            <td>{{ formatBytes(backup.sizeBytes) }}</td>
            <td class="backup-path">{{ backup.path }}</td>
            <td>
              <button type="button" @click="restoreBackup(backup.id)" :disabled="restoringId !== null">
                {{ restoringId === backup.id ? 'Restoring' : 'Restore' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useDashboardStore } from '../stores/dashboard';
import type { PriestessConfig } from '../api/dashboard';
import EmptyState from '../components/EmptyState.vue';

const store = useDashboardStore();
const draft = ref('');
const error = ref('');
const notice = ref('');
const loadingBackups = ref(false);
const restoringId = ref<string | null>(null);

const backupSummary = computed(() => {
  const count = store.configBackups.length;
  if (loadingBackups.value) return 'Loading backup metadata.';
  return count === 1 ? '1 backup available.' : `${count} backups available.`;
});

function resetDraft() {
  draft.value = JSON.stringify(store.config, null, 2);
}

async function saveDraft() {
  try {
    error.value = '';
    notice.value = '';
    const parsed = JSON.parse(draft.value) as PriestessConfig;
    await store.saveConfig(parsed);
    resetDraft();
    notice.value = 'Config saved and backup metadata refreshed.';
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : String(cause);
  }
}

async function loadBackups() {
  loadingBackups.value = true;
  error.value = '';
  try {
    await store.loadConfigBackups();
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : String(cause);
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
    resetDraft();
    notice.value = `Restored backup ${id}.`;
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : String(cause);
  } finally {
    restoringId.value = null;
  }
}

function formatBytes(size: number) {
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}

function formatBackupTime(value: string) {
  const timestamp = Date.parse(value);
  if (Number.isNaN(timestamp)) return value;
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'medium',
  }).format(timestamp);
}

watch(
  () => store.config,
  () => resetDraft(),
);

onMounted(() => {
  resetDraft();
  void loadBackups();
});
</script>
