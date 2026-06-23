export interface HealthResponse {
  status: string;
  components: Record<string, string>;
  timestamp: number;
  uptimeMillis: number;
  diagnostics: Record<string, string>;
}

export interface ServerConfig {
  enabled: boolean;
  host: string;
  port: number;
  corsEnabled: boolean;
  configWatchEnabled: boolean;
  configWatchIntervalMillis: number;
}

export interface PluginConfig {
  enabled: boolean;
  directory: string;
  autoDiscover: boolean;
}

export interface PlatformConfig {
  name: string;
  type: string;
  enabled: boolean;
  host: string;
  port: number;
  wsPort: number;
  token: string;
  baseUrl: string;
  useWs: boolean;
  config: Record<string, string>;
}

export interface ProviderConfig {
  name: string;
  type: string;
  model: string;
  baseUrl: string;
  apiKey: string;
  enabled: boolean;
  config: Record<string, string>;
}

export interface AgentConfig {
  name: string;
  instructions: string;
  model: string;
  providerName: string;
  maxSteps: number;
  temperature: number;
  compressStrategy: string;
  maxRounds: number;
  maxTokens: number;
  toolTimeoutSeconds: number;
  enabledTools: string[];
}

export interface PriestessConfig {
  platforms: PlatformConfig[];
  providers: ProviderConfig[];
  agent: AgentConfig;
  subAgents: SubAgentOrchestrationConfig;
  database: { path: string };
  pipeline: Record<string, unknown>;
  server: ServerConfig;
  plugins: PluginConfig;
}

export interface ConfigBackup {
  id: string;
  createdAt: string;
  sizeBytes: number;
  path: string;
}

export interface SubAgentConfig {
  name: string;
  description: string;
  agent: AgentConfig;
  enabled: boolean;
}

export interface SubAgentRouteConfig {
  name: string;
  targetAgentName: string;
  keywords: string[];
  priority: number;
  enabled: boolean;
}

export interface SubAgentOrchestrationConfig {
  enabled: boolean;
  defaultAgentName: string;
  agents: SubAgentConfig[];
  routes: SubAgentRouteConfig[];
}

export interface PlatformStatusDto {
  name: string;
  type: string;
  enabled: boolean;
  running: boolean;
  host: string;
  port: number;
  wsPort: number;
}

export interface ProviderDto {
  name: string;
  displayName: string;
  kind: string;
  supportToolCalling: boolean;
  supportVision: boolean;
  supportStreaming: boolean;
}

export interface ToolDto {
  name: string;
  description: string;
  parameters: {
    properties: Record<string, unknown>;
    required: string[];
  };
}

export interface ConversationDto {
  id: string;
  platform: string;
  sessionId: string;
  createdAt: number;
  updatedAt: number;
}

export interface MessageDto {
  id: string;
  conversationId: string;
  role: string;
  content?: string;
  toolCalls?: string;
  toolCallId?: string;
  createdAt: number;
}

export interface PluginDescriptor {
  manifest: {
    id: string;
    name: string;
    version: string;
    description: string;
    entrypoint: string;
    capabilities: string[];
  };
  state: string;
  error?: string;
}

export interface PluginExtensionMetadata {
  pluginId: string;
  kind: string;
  name: string;
  description: string;
}

export interface PluginListResponse {
  plugins: PluginDescriptor[];
  extensions: PluginExtensionMetadata[];
}

export interface LogEventDto {
  level: string;
  message: string;
  timestamp: number;
}

export interface AgentChatRequest {
  message: string;
  config?: AgentConfig;
  conversationId?: string;
}

export interface AgentChatEventDto {
  type: string;
  message: string;
  toolName?: string;
  success?: boolean;
  timestamp: number;
}

export interface AgentChatResponse {
  status: string;
  content: string;
  events: AgentChatEventDto[];
  providerName: string;
  model: string;
  conversationId: string;
}

export interface SubAgentTestRequest {
  message: string;
  config?: SubAgentOrchestrationConfig;
  conversationId?: string;
}

export interface SubAgentTestResponse {
  status: string;
  content: string;
  selectedAgentName: string;
  selectedRouteName?: string;
  selectionReason: string;
  events: AgentChatEventDto[];
  conversationId: string;
}

export interface KnowledgeBase {
  id: string;
  name: string;
  description: string;
  createdAt: number;
  updatedAt: number;
}

export interface KnowledgeChunk {
  id: string;
  knowledgeBaseId: string;
  documentName: string;
  content: string;
  createdAt: number;
}

export interface KnowledgeBaseListResponse {
  bases: KnowledgeBase[];
}

export interface CreateKnowledgeBaseRequest {
  name: string;
  description?: string;
}

export interface AddKnowledgeDocumentRequest {
  documentName: string;
  content: string;
}

export interface KnowledgeSearchRequest {
  query: string;
  knowledgeBaseId?: string;
  limit?: number;
}

export interface KnowledgeSearchResultDto {
  chunk: KnowledgeChunk;
  score: number;
}

const dashboardTokenKey = 'priestess.dashboardToken';

export function dashboardApiToken(): string {
  if (typeof window === 'undefined') return '';
  const tokenFromUrl = new URLSearchParams(window.location.search).get('token')?.trim();
  if (tokenFromUrl) {
    window.localStorage.setItem(dashboardTokenKey, tokenFromUrl);
    return tokenFromUrl;
  }
  return window.localStorage.getItem(dashboardTokenKey)?.trim() ?? '';
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = dashboardApiToken();
  const response = await fetch(path, {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init?.headers ?? {}),
    },
    ...init,
  });
  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}`);
  }
  return response.json() as Promise<T>;
}

export const dashboardApi = {
  health: () => request<HealthResponse>('/health'),
  config: () => request<PriestessConfig>('/api/config'),
  configBackups: () => request<ConfigBackup[]>('/api/config/backups'),
  replaceConfig: (config: PriestessConfig) =>
    request<PriestessConfig>('/api/config', {
      method: 'PUT',
      body: JSON.stringify(config),
    }),
  reloadConfig: () => request<PriestessConfig>('/api/config/reload', { method: 'POST' }),
  restoreConfigBackup: (id: string) =>
    request<PriestessConfig>(`/api/config/backups/${encodeURIComponent(id)}/restore`, { method: 'POST' }),
  platforms: () => request<PlatformStatusDto[]>('/api/platforms'),
  startPlatform: (name: string) => request<PriestessConfig>(`/api/platforms/${encodeURIComponent(name)}/start`, { method: 'POST' }),
  stopPlatform: (name: string) => request<PriestessConfig>(`/api/platforms/${encodeURIComponent(name)}/stop`, { method: 'POST' }),
  providers: () => request<ProviderDto[]>('/api/providers'),
  testProviders: () => request<Record<string, boolean>>('/api/providers/test', { method: 'POST' }),
  tools: () => request<ToolDto[]>('/api/tools'),
  chatAgent: (body: AgentChatRequest) =>
    request<AgentChatResponse>('/api/agent/chat', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  subAgentConfig: () => request<SubAgentOrchestrationConfig>('/api/sub-agents/config'),
  replaceSubAgentConfig: (config: SubAgentOrchestrationConfig) =>
    request<SubAgentOrchestrationConfig>('/api/sub-agents/config', {
      method: 'PUT',
      body: JSON.stringify(config),
    }),
  testSubAgent: (body: SubAgentTestRequest) =>
    request<SubAgentTestResponse>('/api/sub-agents/test', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  knowledgeBases: () => request<KnowledgeBaseListResponse>('/api/knowledge/bases'),
  createKnowledgeBase: (body: CreateKnowledgeBaseRequest) =>
    request<KnowledgeBaseListResponse>('/api/knowledge/bases', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  addKnowledgeDocument: (baseId: string, body: AddKnowledgeDocumentRequest) =>
    request<KnowledgeChunk[]>(`/api/knowledge/bases/${encodeURIComponent(baseId)}/documents`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  searchKnowledge: (body: KnowledgeSearchRequest) =>
    request<KnowledgeSearchResultDto[]>('/api/knowledge/search', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  conversations: () => request<ConversationDto[]>('/api/conversations'),
  messages: (id: string, count = 100) =>
    request<MessageDto[]>(`/api/conversations/${encodeURIComponent(id)}/messages?count=${count}`),
  plugins: () => request<PluginListResponse>('/api/plugins'),
  discoverPlugins: () => request<PluginListResponse>('/api/plugins/discover', { method: 'POST' }),
  enablePlugin: (id: string) => request<PluginListResponse>(`/api/plugins/${encodeURIComponent(id)}/enable`, { method: 'POST' }),
  disablePlugin: (id: string) => request<PluginListResponse>(`/api/plugins/${encodeURIComponent(id)}/disable`, { method: 'POST' }),
  loadPlugin: (id: string) => request<PluginListResponse>(`/api/plugins/${encodeURIComponent(id)}/load`, { method: 'POST' }),
  unloadPlugin: (id: string) => request<PluginListResponse>(`/api/plugins/${encodeURIComponent(id)}/unload`, { method: 'POST' }),
};
