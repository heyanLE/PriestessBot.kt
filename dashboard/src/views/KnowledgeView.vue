<template>
  <div class="grid knowledge-layout">
    <section class="panel">
      <div class="section-title">
        <div>
          <h2>Knowledge Bases</h2>
          <p>{{ bases.length }} bases available to `knowledge_search`.</p>
        </div>
        <button type="button" class="primary" @click="loadBases">Refresh</button>
      </div>

      <form class="stacked-form" @submit.prevent="createBase">
        <input v-model="newBaseName" type="text" placeholder="Base name" />
        <input v-model="newBaseDescription" type="text" placeholder="Description" />
        <button type="submit" class="primary" :disabled="newBaseName.trim().length === 0">Create</button>
      </form>

      <EmptyState v-if="bases.length === 0" title="No knowledge bases" detail="Create a base, then add text documents for retrieval." />
      <div v-else class="grid list-grid">
        <article v-for="base in bases" :key="base.id" class="card selectable-card" :class="{ selected: base.id === selectedBaseId }" @click="selectedBaseId = base.id">
          <h3>{{ base.name }}</h3>
          <p>{{ base.description || 'No description' }}</p>
          <p>{{ formatTime(base.updatedAt) }}</p>
        </article>
      </div>
    </section>

    <section class="panel">
      <div class="section-title">
        <div>
          <h2>Add Document</h2>
          <p v-if="selectedBase">Target: {{ selectedBase.name }}</p>
          <p v-else>Select or create a base first.</p>
        </div>
      </div>

      <form class="grid" @submit.prevent="addDocument">
        <input v-model="documentName" type="text" placeholder="Document name" />
        <textarea v-model="documentContent" class="document-editor" placeholder="Paste Markdown, notes, or plain text..." spellcheck="false"></textarea>
        <button type="submit" class="primary" :disabled="!selectedBaseId || documentName.trim().length === 0 || documentContent.trim().length === 0">
          Add Document
        </button>
      </form>
      <section v-if="lastChunkCount !== null" class="notice">
        Stored {{ lastChunkCount }} chunks.
      </section>
    </section>

    <section class="panel search-panel">
      <div class="section-title">
        <div>
          <h2>Search Test</h2>
          <p>Runs the same retrieval path used by the Agent tool.</p>
        </div>
      </div>

      <form class="chat-form" @submit.prevent="search">
        <input v-model="query" type="text" placeholder="Search knowledge..." />
        <button type="submit" class="primary" :disabled="query.trim().length === 0">Search</button>
      </form>

      <EmptyState v-if="results.length === 0" title="No results loaded" detail="Submit a query to inspect ranked chunks." />
      <div v-else class="grid">
        <article v-for="result in results" :key="result.chunk.id" class="card result-card">
          <div class="section-title">
            <h3>{{ result.chunk.documentName }}</h3>
            <span class="status-pill ok">score {{ result.score.toFixed(2) }}</span>
          </div>
          <p>{{ result.chunk.content }}</p>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { dashboardApi, type KnowledgeBase, type KnowledgeSearchResultDto } from '../api/dashboard';
import EmptyState from '../components/EmptyState.vue';

const bases = ref<KnowledgeBase[]>([]);
const selectedBaseId = ref('');
const newBaseName = ref('');
const newBaseDescription = ref('');
const documentName = ref('');
const documentContent = ref('');
const lastChunkCount = ref<number | null>(null);
const query = ref('');
const results = ref<KnowledgeSearchResultDto[]>([]);

const selectedBase = computed(() => bases.value.find((base) => base.id === selectedBaseId.value) ?? null);

async function loadBases() {
  const response = await dashboardApi.knowledgeBases();
  bases.value = response.bases;
  if (!selectedBaseId.value && response.bases.length > 0) {
    selectedBaseId.value = response.bases[0].id;
  }
}

async function createBase() {
  const response = await dashboardApi.createKnowledgeBase({
    name: newBaseName.value.trim(),
    description: newBaseDescription.value.trim(),
  });
  bases.value = response.bases;
  selectedBaseId.value = response.bases[0]?.id ?? selectedBaseId.value;
  newBaseName.value = '';
  newBaseDescription.value = '';
}

async function addDocument() {
  if (!selectedBaseId.value) return;
  const chunks = await dashboardApi.addKnowledgeDocument(selectedBaseId.value, {
    documentName: documentName.value.trim(),
    content: documentContent.value,
  });
  lastChunkCount.value = chunks.length;
  documentName.value = '';
  documentContent.value = '';
}

async function search() {
  results.value = await dashboardApi.searchKnowledge({
    query: query.value.trim(),
    knowledgeBaseId: selectedBaseId.value || undefined,
    limit: 5,
  });
}

function formatTime(value: number) {
  return new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeStyle: 'short' }).format(value);
}

onMounted(() => void loadBases());
</script>
