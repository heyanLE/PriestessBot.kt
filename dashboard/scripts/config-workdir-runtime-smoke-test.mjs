import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const configView = readFileSync(resolve(root, 'src/views/ConfigView.vue'), 'utf8');
const workingDirectoryView = readFileSync(resolve(root, 'src/views/WorkingDirectoryView.vue'), 'utf8');
const effectiveRuntimeView = readFileSync(resolve(root, 'src/views/EffectiveRuntimeView.vue'), 'utf8');
const router = readFileSync(resolve(root, 'src/router/index.ts'), 'utf8');
const appLayout = readFileSync(resolve(root, 'src/components/AppLayout.vue'), 'utf8');
const dashboardApi = readFileSync(resolve(root, 'src/api/dashboard.ts'), 'utf8');
const dashboardStore = readFileSync(resolve(root, 'src/stores/dashboard.ts'), 'utf8');

const checks = [
  ['config route exists', "path: '/config'"],
  ['working directory route exists', "path: '/working-directory'"],
  ['effective runtime route exists', "path: '/effective-runtime'"],
  ['diagnostics route exists', "path: '/workspaces'"],
  ['diagnostics route uses new label', "label: 'Diagnostics'"],
  ['layout exposes diagnostics shortcut', 'Diagnostics'],
  ['config view uses day-operations wording', 'Config Day Operations'],
  ['config view keeps database layer framing', 'Daylight tactical board for the database layer.'],
  ['config view distinguishes default workdir', 'Default workdir'],
  ['config view links to effective runtime', 'Open Effective Runtime'],
  ['working directory view exists', 'Working Directory'],
  ['working directory view shows effective workdir', 'Effective workdir'],
  ['working directory view warns about environment source', 'current effective workdir comes from the environment layer'],
  ['working directory view shows detected skills', 'Detected skills'],
  ['effective runtime view exists', 'Effective Runtime'],
  ['effective runtime view shows source trace', 'Source trace'],
  ['effective runtime view exposes preview-only wording', 'Saved config remains untouched.'],
  ['api database layer endpoint', "request<DatabaseConfigLayerResponse>('/api/config/layers/database')"],
  ['api environment summary endpoint', "request<EnvironmentOverrideSummaryResponse>('/api/config/layers/environment')"],
  ['api workdir summary endpoint', "request<WorkingDirectorySummaryResponse>('/api/config/workdir')"],
  ['api preview endpoint', "request<EffectiveRuntimePreviewResponse>('/api/config/preview'"],
  ['store refreshes config surfaces', 'async function refreshConfigSurfaces()'],
  ['store updates workdir', 'async function updateWorkingDirectory(path: string)'],
  ['store previews effective runtime', 'async function previewEffectiveRuntime(request: EffectiveRuntimePreviewRequest = {})'],
];

const haystacks = [configView, workingDirectoryView, effectiveRuntimeView, router, appLayout, dashboardApi, dashboardStore];
const failures = checks.filter(([, needle]) => !haystacks.some((content) => content.includes(needle)));

if (failures.length > 0) {
  console.error('Config/workdir/runtime smoke checks failed:');
  for (const [label, needle] of failures) {
    console.error(`- ${label}: missing ${needle}`);
  }
  process.exit(1);
}

console.log(`Config/workdir/runtime smoke checks passed (${checks.length} checks).`);
