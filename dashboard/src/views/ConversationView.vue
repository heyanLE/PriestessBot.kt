<template>
  <section class="panel">
    <div class="section-title">
      <div>
        <h2>Conversations</h2>
        <p>{{ store.conversations.length }} stored sessions.</p>
      </div>
    </div>
    <EmptyState v-if="store.conversations.length === 0" title="No conversations stored" detail="Pipeline traffic will create conversation records." />
    <div v-else class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>Platform</th>
            <th>Session</th>
            <th>Created</th>
            <th>Updated</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="conversation in store.conversations" :key="conversation.id" class="clickable-row" @click="openConversation(conversation.id)">
            <td>{{ conversation.platform }}</td>
            <td><code>{{ conversation.sessionId }}</code></td>
            <td>{{ formatTime(conversation.createdAt) }}</td>
            <td>{{ formatTime(conversation.updatedAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import EmptyState from '../components/EmptyState.vue';
import { useDashboardStore } from '../stores/dashboard';

const store = useDashboardStore();
const router = useRouter();

function openConversation(id: string) {
  void router.push(`/conversations/${id}`);
}

function formatTime(value: number) {
  return new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeStyle: 'short' }).format(value);
}
</script>
