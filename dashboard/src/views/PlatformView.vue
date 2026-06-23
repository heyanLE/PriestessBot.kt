<template>
  <section class="panel">
    <div class="section-title">
      <div>
        <h2>Configured Platforms</h2>
        <p>{{ store.enabledPlatforms }} enabled, {{ store.runningPlatforms }} running.</p>
      </div>
    </div>
    <EmptyState v-if="store.platforms.length === 0" title="No platforms configured" detail="Add platform config through the config view or config file." />
    <div v-else class="grid list-grid">
      <article v-for="platform in store.platforms" :key="platform.name" class="card">
        <div class="section-title">
          <h3>{{ platform.name }}</h3>
          <StatusDot :label="platform.running ? 'Running' : platform.enabled ? 'Enabled' : 'Stopped'" :tone="platform.running ? 'ok' : platform.enabled ? 'warn' : 'muted'" />
        </div>
        <p>{{ platform.type }} at {{ platform.host }}:{{ platform.port }} / ws {{ platform.wsPort }}</p>
        <div class="toolbar">
          <button type="button" class="primary" :disabled="platform.enabled" @click="store.setPlatformEnabled(platform.name, true)">Start</button>
          <button type="button" :disabled="!platform.enabled" @click="store.setPlatformEnabled(platform.name, false)">Stop</button>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import EmptyState from '../components/EmptyState.vue';
import StatusDot from '../components/StatusDot.vue';
import { useDashboardStore } from '../stores/dashboard';

const store = useDashboardStore();
</script>
