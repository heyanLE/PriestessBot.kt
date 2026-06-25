<template>
  <section class="panel">
    <div class="section-title">
      <div>
        <h2>Configured Platforms</h2>
        <p>{{ store.enabledPlatforms }} enabled, {{ store.runningPlatforms }} running.</p>
      </div>
    </div>
    <EmptyState v-if="store.platforms.length === 0" title="No platforms configured" detail="Add platform config through the config view or config file." />
    <div v-else class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Type</th>
            <th>Endpoint</th>
            <th>State</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="platform in store.platforms" :key="platform.name">
            <td><strong>{{ platform.name }}</strong></td>
            <td>{{ platform.type }}</td>
            <td>
              <code>{{ platform.host }}:{{ platform.port }}</code>
              <p class="muted">ws {{ platform.wsPort }}</p>
            </td>
            <td>
              <StatusDot :label="platform.running ? 'Running' : platform.enabled ? 'Enabled' : 'Stopped'" :tone="platform.running ? 'ok' : platform.enabled ? 'warn' : 'muted'" />
            </td>
            <td>
              <div class="toolbar">
                <button type="button" class="primary" :disabled="platform.enabled" @click="store.setPlatformEnabled(platform.name, true)">Start</button>
                <button type="button" :disabled="!platform.enabled" @click="store.setPlatformEnabled(platform.name, false)">Stop</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup lang="ts">
import EmptyState from '../components/EmptyState.vue';
import StatusDot from '../components/StatusDot.vue';
import { useDashboardStore } from '../stores/dashboard';

const store = useDashboardStore();
</script>
