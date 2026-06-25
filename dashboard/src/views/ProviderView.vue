<template>
  <section class="panel">
    <div class="section-title">
      <div>
        <h2>Providers</h2>
        <p>{{ store.providers.length }} runtime providers registered.</p>
      </div>
      <button type="button" class="primary" @click="store.testProviders()">Test All</button>
    </div>
    <div class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Kind</th>
            <th>Health</th>
            <th>Capabilities</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="provider in store.providers" :key="provider.name">
            <td>
              <strong>{{ provider.displayName }}</strong>
              <p class="muted">{{ provider.name }}</p>
            </td>
            <td>{{ provider.kind }}</td>
            <td>
              <StatusDot
                v-if="provider.name in store.providerTests"
                :label="store.providerTests[provider.name] ? 'Online' : 'Failed'"
                :tone="store.providerTests[provider.name] ? 'ok' : 'error'"
              />
              <span v-else class="inline-status muted">Not tested</span>
            </td>
            <td>
              <div class="chip-row">
                <span class="chip">Tools {{ yesNo(provider.supportToolCalling) }}</span>
                <span class="chip">Vision {{ yesNo(provider.supportVision) }}</span>
                <span class="chip">Streaming {{ yesNo(provider.supportStreaming) }}</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup lang="ts">
import StatusDot from '../components/StatusDot.vue';
import { useDashboardStore } from '../stores/dashboard';

const store = useDashboardStore();
const yesNo = (value: boolean) => (value ? 'yes' : 'no');
</script>
