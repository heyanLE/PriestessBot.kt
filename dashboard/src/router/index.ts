import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import OverviewView from '../views/OverviewView.vue';
import PlatformView from '../views/PlatformView.vue';
import ProviderView from '../views/ProviderView.vue';
import AgentView from '../views/AgentView.vue';
import SubAgentView from '../views/SubAgentView.vue';
import ToolView from '../views/ToolView.vue';
import KnowledgeView from '../views/KnowledgeView.vue';
import ConversationView from '../views/ConversationView.vue';
import ConversationDetailView from '../views/ConversationDetailView.vue';
import PluginView from '../views/PluginView.vue';
import LogView from '../views/LogView.vue';
import ConfigView from '../views/ConfigView.vue';

export const routes: RouteRecordRaw[] = [
  { path: '/', name: 'overview', component: OverviewView, meta: { label: 'Overview' } },
  { path: '/platforms', name: 'platforms', component: PlatformView, meta: { label: 'Platforms' } },
  { path: '/providers', name: 'providers', component: ProviderView, meta: { label: 'Providers' } },
  { path: '/agent', name: 'agent', component: AgentView, meta: { label: 'Agent' } },
  { path: '/sub-agents', name: 'sub-agents', component: SubAgentView, meta: { label: 'Sub-Agents' } },
  { path: '/tools', name: 'tools', component: ToolView, meta: { label: 'Tools' } },
  { path: '/knowledge', name: 'knowledge', component: KnowledgeView, meta: { label: 'Knowledge' } },
  { path: '/conversations', name: 'conversations', component: ConversationView, meta: { label: 'Conversations' } },
  { path: '/conversations/:id', name: 'conversation-detail', component: ConversationDetailView, meta: { label: 'Conversation', nav: false } },
  { path: '/plugins', name: 'plugins', component: PluginView, meta: { label: 'Plugins' } },
  { path: '/logs', name: 'logs', component: LogView, meta: { label: 'Logs' } },
  { path: '/config', name: 'config', component: ConfigView, meta: { label: 'Config' } },
];

export default createRouter({
  history: createWebHistory(),
  routes,
});
