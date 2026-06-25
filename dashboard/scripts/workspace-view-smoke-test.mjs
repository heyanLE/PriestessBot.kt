import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const root = resolve(import.meta.dirname, '..');
const workspaceView = readFileSync(resolve(root, 'src/views/WorkspaceView.vue'), 'utf8');
const dashboardApi = readFileSync(resolve(root, 'src/api/dashboard.ts'), 'utf8');

const checks = [
  ['lists workspaces from store', 'store.workspaces.workspaces'],
  ['loads workspace statuses', 'await store.loadWorkspaces()'],
  ['loads selected workspace detail', 'dashboardApi.workspaceDetail(selectedWorkspaceId.value)'],
  ['reloads one workspace', 'await store.reloadWorkspace(id)'],
  ['reloads all workspaces', 'await store.reloadWorkspaces()'],
  ['shows active snapshot version', "workspace.activeSnapshotVersion ?? 'none'"],
  ['shows detail snapshot version', "detail.status.activeSnapshotVersion ?? 'none'"],
  ['shows last reload status', 'workspace.lastReload.status'],
  ['shows reload failure summary', 'lastReload.errorSummary'],
  ['keeps failed reload visible', ':class="{ error: !lastReload.success, ok: lastReload.success }"'],
  ['shows scoped skills', 'detail.skills'],
  ['shows scoped MCP servers', 'detail.mcpServers'],
  ['shows scoped tools', 'detail.tools'],
  ['shows personas', 'detail.personas'],
  ['shows memory policy', 'detail.memory'],
  ['GET /api/workspaces client', "request<WorkspaceListResponse>('/api/workspaces')"],
  ['GET workspace detail client', 'request<WorkspaceDetailDto>(`/api/workspaces/${encodeURIComponent(id)}`)'],
  ['POST reload one client', 'request<WorkspaceReloadResult>(`/api/workspaces/${encodeURIComponent(id)}/reload`, { method: \'POST\' })'],
  ['POST reload all client', "request<WorkspaceReloadResult[]>('/api/workspaces/reload', { method: 'POST' })"],
];

const failures = checks.filter(([, needle]) => !workspaceView.includes(needle) && !dashboardApi.includes(needle));

if (failures.length > 0) {
  console.error('Workspace view smoke checks failed:');
  for (const [label, needle] of failures) {
    console.error(`- ${label}: missing ${needle}`);
  }
  process.exit(1);
}

console.log(`Workspace view smoke checks passed (${checks.length} checks).`);
