import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const root = resolve(import.meta.dirname, '..');
const personaMemoryView = readFileSync(resolve(root, 'src/views/PersonaMemoryView.vue'), 'utf8');
const agentView = readFileSync(resolve(root, 'src/views/AgentView.vue'), 'utf8');
const dashboardApi = readFileSync(resolve(root, 'src/api/dashboard.ts'), 'utf8');

const checks = [
  ['loads personas for selected workspace', 'await dashboardApi.personas(workspaceId.value)'],
  ['loads scoped memories', 'await dashboardApi.memories({'],
  ['creates or updates persona', 'body.id ? await dashboardApi.updatePersona(body.id, body) : await dashboardApi.savePersona(body)'],
  ['deletes selected persona', 'await dashboardApi.deletePersona(personaDraft.id)'],
  ['creates memory record', 'await dashboardApi.saveMemory({'],
  ['searches memory records', 'await dashboardApi.searchMemory({'],
  ['deletes selected memory', 'await dashboardApi.deleteMemory(selectedMemory.value.id'],
  ['expires memory records', 'await dashboardApi.expireMemory()'],
  ['shows persona registry', 'Persona Registry'],
  ['shows memory workbench', 'Memory Workbench'],
  ['shows search matches', 'Search matches'],
  ['shows injection trace', 'message.injectionTrace'],
  ['shows trace persona', 'Persona {{ message.injectionTrace.personaName ??'],
  ['shows trace memory count', 'Memories {{ message.injectionTrace.memoryCount }}'],
  ['shows trace memory rows', 'trace-memory-list'],
  ['GET personas client', 'request<PersonaListResponse>(`/api/personas${queryString({ workspaceId })}`)'],
  ['POST save persona client', "request<Persona>('/api/personas'"],
  ['PUT update persona client', 'request<Persona>(`/api/personas/${encodeURIComponent(id)}`'],
  ['DELETE persona client', 'request<DeleteResponse>(`/api/personas/${encodeURIComponent(id)}`'],
  ['POST resolve persona client', "request<PersonaResolveResponse>('/api/personas/resolve'"],
  ['GET memories client', 'request<MemoryListResponse>('],
  ['POST save memory client', "request<MemoryRecord>('/api/memory'"],
  ['POST search memory client', "request<MemorySearchResponse>('/api/memory/search'"],
  ['DELETE memory client', 'request<DeleteResponse>('],
  ['POST expire memory client', "request<ExpireMemoryResponse>('/api/memory/expire'"],
];

const failures = checks.filter(([, needle]) => {
  return !personaMemoryView.includes(needle) && !agentView.includes(needle) && !dashboardApi.includes(needle);
});

if (failures.length > 0) {
  console.error('Persona/memory smoke checks failed:');
  for (const [label, needle] of failures) {
    console.error(`- ${label}: missing ${needle}`);
  }
  process.exit(1);
}

console.log(`Persona/memory smoke checks passed (${checks.length} checks).`);
