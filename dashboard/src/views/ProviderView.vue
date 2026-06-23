<template>
  <section class="panel">
    <div class="section-title">
      <div>
        <h2>Providers</h2>
        <p>{{ store.providers.length }} runtime providers registered.</p>
      </div>
      <button type="button" class="primary" @click="store.testProviders()">Test All</button>
    </div>
    <div class="grid list-grid">
      <article v-for="provider in store.providers" :key="provider.name" class="card">
        <div class="section-title">
          <h3>{{ provider.displayName }}</h3>
          <StatusDot v-if="provider.name in store.providerTests" :label="store.providerTests[provider.name] ? 'Online' : 'Failed'" :tone="store.providerTests[provider.name] ? 'ok' : 'error'" />
        </div>
        <p>{{ provider.name }} · {{ provider.kind }}</p>
        <p>
          Tools {{ yesNo(provider.supportToolCalling) }},
          vision {{ yesNo(provider.supportVision) }},
          streaming {{ yesNo(provider.supportStreaming) }}
        </p>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import StatusDot from '../components/StatusDot.vue';
import { useDashboardStore } from '../stores/dashboard';

const store = useDashboardStore();
const yesNo = (value: boolean) => (value ? 'yes' : 'no');
</script>
