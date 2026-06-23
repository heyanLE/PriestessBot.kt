<template>
  <section class="panel">
    <div class="section-title">
      <div>
        <h2>Tools</h2>
        <p>{{ store.tools.length }} tools available to agent runners.</p>
      </div>
    </div>
    <div class="grid list-grid">
      <article v-for="tool in store.tools" :key="tool.name" class="card">
        <h3>{{ tool.name }}</h3>
        <p>{{ tool.description }}</p>
        <p>{{ tool.parameters.required.length }} required parameters</p>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useDashboardStore } from '../stores/dashboard';

const store = useDashboardStore();
</script>
