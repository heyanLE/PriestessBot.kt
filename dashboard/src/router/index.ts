import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import OverviewView from '../views/OverviewView.vue';
import PlatformView from '../views/PlatformView.vue';
import ProviderView from '../views/ProviderView.vue';
import AgentView from '../views/AgentView.vue';
import SubAgentView from '../views/SubAgentView.vue';
import ToolView from '../views/ToolView.vue';
import WorkspaceView from '../views/WorkspaceView.vue';
import WorkingDirectoryView from '../views/WorkingDirectoryView.vue';
import EffectiveRuntimeView from '../views/EffectiveRuntimeView.vue';
import PersonaMemoryView from '../views/PersonaMemoryView.vue';
import KnowledgeView from '../views/KnowledgeView.vue';
import ConversationView from '../views/ConversationView.vue';
import ConversationDetailView from '../views/ConversationDetailView.vue';
import PluginView from '../views/PluginView.vue';
import LogView from '../views/LogView.vue';
import ConfigView from '../views/ConfigView.vue';

export const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'overview',
    component: OverviewView,
    meta: {
      label: 'Overview',
      group: 'Overview',
      summary: 'Operator workbench for runtime health, incidents, and recovery.',
    },
  },
  {
    path: '/conversations',
    name: 'conversations',
    component: ConversationView,
    meta: {
      label: 'Conversations',
      group: 'Troubleshooting',
      summary: 'Inspect traffic, session history, and execution context.',
    },
  },
  {
    path: '/logs',
    name: 'logs',
    component: LogView,
    meta: {
      label: 'Logs',
      group: 'Troubleshooting',
      summary: 'Follow live runtime events, warnings, and execution traces.',
    },
  },
  {
    path: '/config',
    name: 'config',
    component: ConfigView,
    meta: {
      label: 'Config',
      group: 'Changes',
      summary: 'Review persisted configuration and apply controlled updates.',
    },
  },
  {
    path: '/agent',
    name: 'agent',
    component: AgentView,
    meta: {
      label: 'Agent',
      group: 'Changes',
      summary: 'Tune the primary agent and validate behavior in one place.',
    },
  },
  {
    path: '/providers',
    name: 'providers',
    component: ProviderView,
    meta: {
      label: 'Providers',
      group: 'Assets',
      summary: 'Browse provider inventory, health, and capabilities.',
    },
  },
  {
    path: '/sub-agents',
    name: 'sub-agents',
    component: SubAgentView,
    meta: {
      label: 'Sub-Agents',
      group: 'Changes',
      summary: 'Edit delegation routes and orchestration rules.',
    },
  },
  {
    path: '/tools',
    name: 'tools',
    component: ToolView,
    meta: {
      label: 'Tools',
      group: 'Assets',
      summary: 'Inspect the tool registry, policy posture, and exposure.',
    },
  },
  {
    path: '/platforms',
    name: 'platforms',
    component: PlatformView,
    meta: {
      label: 'Platforms',
      group: 'Assets',
      summary: 'Review platform endpoints, state, and runtime entry points.',
    },
  },
  {
    path: '/plugins',
    name: 'plugins',
    component: PluginView,
    meta: {
      label: 'Plugins',
      group: 'Assets',
      summary: 'Manage plugin lifecycle, extension state, and discovery.',
    },
  },
  {
    path: '/persona-memory',
    name: 'persona-memory',
    component: PersonaMemoryView,
    meta: {
      label: 'Persona & Memory',
      group: 'Assets',
      summary: 'Control persona overlays, memory records, and injection.',
    },
  },
  {
    path: '/knowledge',
    name: 'knowledge',
    component: KnowledgeView,
    meta: {
      label: 'Knowledge',
      group: 'Assets',
      summary: 'Manage knowledge bases, retrieval quality, and source tests.',
    },
  },
  {
    path: '/working-directory',
    name: 'working-directory',
    component: WorkingDirectoryView,
    meta: {
      label: 'Working Directory',
      group: 'Changes',
      summary: 'Control workspace selection, local overlays, and discovery.',
    },
  },
  {
    path: '/effective-runtime',
    name: 'effective-runtime',
    component: EffectiveRuntimeView,
    meta: {
      label: 'Effective Runtime',
      group: 'Changes',
      summary: 'Trace the layered runtime configuration from result to source.',
    },
  },
  {
    path: '/workspaces',
    name: 'workspaces',
    component: WorkspaceView,
    meta: {
      label: 'Workspaces',
      group: 'Changes',
      summary: 'Review workspace snapshots, reload plans, and diagnostics.',
    },
  },
  { path: '/conversations/:id', name: 'conversation-detail', component: ConversationDetailView, meta: { label: 'Conversation', nav: false } },
];

export default createRouter({
  history: createWebHistory(),
  routes,
});
