<template>
  <section class="panel">
    <div class="section-title">
      <div>
        <h2>{{ conversation?.platform ?? 'Conversation' }}</h2>
        <p v-if="conversation">{{ conversation.sessionId }} · Updated {{ formatTime(conversation.updatedAt) }}</p>
        <p v-else>{{ conversationId }}</p>
      </div>
      <div class="toolbar">
        <RouterLink to="/conversations">Back</RouterLink>
        <button type="button" class="primary" @click="loadMessages()">Refresh</button>
      </div>
    </div>

    <EmptyState v-if="messages.length === 0 && !loading" title="No messages" detail="This conversation has no stored messages yet." />
    <section v-else class="transcript">
      <article v-for="message in messages" :key="message.id" class="message-row" :class="message.role.toLowerCase()">
        <div class="message-meta">
          <strong>{{ message.role }}</strong>
          <span>{{ formatTime(message.createdAt) }}</span>
        </div>
        <p v-if="message.content">{{ message.content }}</p>
        <p v-else class="muted">No text content</p>
        <p v-if="message.toolCallId" class="tool-meta">Tool call id: {{ message.toolCallId }}</p>
        <pre v-if="message.toolCalls" class="code-block">{{ prettyJson(message.toolCalls) }}</pre>
      </article>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import { dashboardApi, type MessageDto } from '../api/dashboard';
import EmptyState from '../components/EmptyState.vue';
import { useDashboardStore } from '../stores/dashboard';

const route = useRoute();
const store = useDashboardStore();
const messages = ref<MessageDto[]>([]);
const loading = ref(false);

const conversationId = computed(() => String(route.params.id ?? ''));
const conversation = computed(() => store.conversations.find((item) => item.id === conversationId.value));

async function loadMessages() {
  if (!conversationId.value) return;
  loading.value = true;
  try {
    if (!store.lastUpdated) await store.refreshAll();
    messages.value = await dashboardApi.messages(conversationId.value, 200);
  } finally {
    loading.value = false;
  }
}

function prettyJson(value: string) {
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

function formatTime(value: number) {
  return new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeStyle: 'medium' }).format(value);
}

watch(conversationId, () => void loadMessages());
onMounted(() => void loadMessages());
</script>
