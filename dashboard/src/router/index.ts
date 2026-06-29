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
      group: 'Runtime',
      summary: 'Strategic runtime overview and operational health.',
    },
  },
  {
    path: '/conversations',
    name: 'conversations',
    component: ConversationView,
    meta: {
      label: 'Conversations',
      group: 'Runtime',
      summary: 'Tracked session traffic and response timelines.',
    },
  },
  {
    path: '/logs',
    name: 'logs',
    component: LogView,
    meta: {
      label: 'Logs',
      group: 'Runtime',
      summary: 'Live runtime events, warnings, and execution traces.',
    },
  },
  {
    path: '/config',
    name: 'config',
    component: ConfigView,
    meta: {
      label: 'Config',
      group: 'Configuration',
      summary: 'Persisted database layer and effective runtime posture.',
    },
  },
  {
    path: '/agent',
    name: 'agent',
    component: AgentView,
    meta: {
      label: 'Agent',
      group: 'Configuration',
      summary: 'Primary agent profile, tools, and validation chat.',
    },
  },
  {
    path: '/providers',
    name: 'providers',
    component: ProviderView,
    meta: {
      label: 'Providers',
      group: 'Configuration',
      summary: 'Runtime provider inventory, health, and capabilities.',
    },
  },
  {
    path: '/sub-agents',
    name: 'sub-agents',
    component: SubAgentView,
    meta: {
      label: 'Sub-Agents',
      group: 'Configuration',
      summary: 'Delegation routes, target agents, and orchestration rules.',
    },
  },
  {
    path: '/tools',
    name: 'tools',
    component: ToolView,
    meta: {
      label: 'Tools',
      group: 'Configuration',
      summary: 'Tool registry, policy posture, and runtime exposure.',
    },
  },
  {
    path: '/platforms',
    name: 'platforms',
    component: PlatformView,
    meta: {
      label: 'Platforms',
      group: 'System',
      summary: 'Platform endpoints, state, and runtime entry points.',
    },
  },
  {
    path: '/plugins',
    name: 'plugins',
    component: PluginView,
    meta: {
      label: 'Plugins',
      group: 'System',
      summary: 'Plugin lifecycle, extension state, and discovery actions.',
    },
  },
  {
    path: '/persona-memory',
    name: 'persona-memory',
    component: PersonaMemoryView,
    meta: {
      label: 'Persona & Memory',
      group: 'System',
      summary: 'Persona overlays, memory records, and injection surfaces.',
    },
  },
  {
    path: '/knowledge',
    name: 'knowledge',
    component: KnowledgeView,
    meta: {
      label: 'Knowledge',
      group: 'System',
      summary: 'Knowledge bases, retrieval quality, and source testing.',
    },
  },
  {
    path: '/working-directory',
    name: 'working-directory',
    component: WorkingDirectoryView,
    meta: {
      label: 'Working Directory',
      group: 'Operations',
      summary: 'Workspace selection, discovered skills, and local overlays.',
    },
  },
  {
    path: '/effective-runtime',
    name: 'effective-runtime',
    component: EffectiveRuntimeView,
    meta: {
      label: 'Effective Runtime',
      group: 'Operations',
      summary: 'Result-first view of the current layered runtime configuration.',
    },
  },
  {
    path: '/workspaces',
    name: 'workspaces',
    component: WorkspaceView,
    meta: {
      label: 'Diagnostics',
      group: 'Operations',
      summary: 'Workspace snapshots, reload plans, and tooling diagnostics.',
    },
  },
  { path: '/conversations/:id', name: 'conversation-detail', component: ConversationDetailView, meta: { label: 'Conversation', nav: false } },
];

export default createRouter({
  history: createWebHistory(),
  routes,
});
