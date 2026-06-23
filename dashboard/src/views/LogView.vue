<template>
  <section class="panel">
    <div class="section-title">
      <div>
        <h2>Live Logs</h2>
        <p>{{ logStore.connected ? 'Socket connected' : 'Socket disconnected' }}</p>
      </div>
      <div class="toolbar">
        <button type="button" class="primary" @click="logStore.connect()">Connect</button>
        <button type="button" @click="logStore.clear()">Clear</button>
      </div>
    </div>
    <section v-if="logStore.error" class="notice error">{{ logStore.error }}</section>
    <EmptyState v-if="logStore.events.length === 0" title="No log events" detail="Connect to the log socket to receive runtime events." />
    <div v-else class="grid">
      <article v-for="event in logStore.events" :key="`${event.timestamp}-${event.message}`" class="card">
        <StatusDot :label="event.level" :tone="event.level === 'ERROR' ? 'error' : event.level === 'WARN' ? 'warn' : 'ok'" />
        <p>{{ formatTime(event.timestamp) }} · {{ event.message }}</p>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import EmptyState from '../components/EmptyState.vue';
import StatusDot from '../components/StatusDot.vue';
import { useLogStore } from '../stores/logs';

const logStore = useLogStore();

onMounted(() => logStore.connect());

function formatTime(value: number) {
  return new Intl.DateTimeFormat('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit' }).format(value);
}
</script>
