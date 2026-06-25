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
  workspaces?: WorkspaceConfig[];
  subAgents: SubAgentOrchestrationConfig;
  database: { path: string };
  pipeline: Record<string, unknown>;
  server: ServerConfig;
  plugins: PluginConfig;
}

export interface WorkspaceSkillConfig {
  name: string;
  enabled: boolean;
  settings: Record<string, string>;
}

export interface WorkspaceMcpServerConfig {
  id: string;
  enabled: boolean;
  transport: string;
  command: string;
  args: string[];
  url: string;
  env: Record<string, string>;
}

export interface WorkspaceToolConfig {
  enabledTools: string[];
  disabledTools: string[];
  allowedRiskLevels: ToolDto['riskLevel'][];
}

export interface WorkspacePersonaConfig {
  id: string;
  enabled: boolean;
  agentNames: string[];
}

export interface WorkspaceMemoryPolicyConfig {
  enabled: boolean;
  allowedScopes: string[];
  knowledgeBaseIds: string[];
  maxInjectedMemories: number;
}

export interface WorkspaceResolutionConfig {
  platformNames: string[];
  sessionIds: string[];
  userIds: string[];
}

export interface WorkspaceConfig {
  id: string;
  name: string;
  enabled: boolean;
  isDefault: boolean;
  agents: AgentConfig[];
  providerName: string;
  skills: WorkspaceSkillConfig[];
  mcpServers: WorkspaceMcpServerConfig[];
  tools: WorkspaceToolConfig;
  personas: WorkspacePersonaConfig[];
  memory: WorkspaceMemoryPolicyConfig;
  subAgents: SubAgentOrchestrationConfig;
  resolution: WorkspaceResolutionConfig;
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
  source: 'BUILTIN' | 'PLUGIN' | 'MCP';
  owner?: string;
  riskLevel: 'SAFE_READ' | 'SESSION_ACTION' | 'EXTERNAL_READ' | 'STATE_WRITE' | 'HIGH_RISK';
  requiredCapabilities: string[];
  defaultEnabled: boolean;
  effectiveEnabled: boolean;
  auditLog: boolean;
  statusReason?: string;
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
  workspaceId?: string;
  platformId?: string;
  sessionId?: string;
  userId?: string;
}

export interface AgentChatEventDto {
  type: string;
  message: string;
  toolName?: string;
  success?: boolean;
  errorCode?: string;
  policyDenialCode?: string;
  timestamp: number;
}

export interface AgentChatResponse {
  status: string;
  content: string;
  events: AgentChatEventDto[];
  providerName: string;
  model: string;
  conversationId: string;
  injectionTrace: AgentChatInjectionTraceDto;
}

export interface AgentChatInjectionTraceDto {
  workspaceId: string;
  personaId?: string;
  personaName?: string;
  memoryCount: number;
  memories: AgentChatInjectedMemoryDto[];
  metadata: Record<string, string>;
}

export interface AgentChatInjectedMemoryDto {
  id: string;
  type: MemoryType;
  score: number;
  matchReason: string;
  contentPreview: string;
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

export interface WorkspaceReloadPlan {
  workspaceId: string;
  oldVersion?: number;
  newVersion: number;
  added: string[];
  removed: string[];
  modified: string[];
}

export interface WorkspaceReloadResult {
  workspaceId: string;
  success: boolean;
  status: string;
  snapshotVersion?: number;
  timestamp: number;
  plan?: WorkspaceReloadPlan;
  diagnostics: string[];
  errorSummary?: string;
}

export interface WorkspaceStatusDto {
  id: string;
  name: string;
  enabled: boolean;
  activeSnapshotVersion?: number;
  loadedAt?: number;
  lastReload?: WorkspaceReloadResult;
  diagnostics: string[];
}

export interface WorkspaceListResponse {
  workspaces: WorkspaceStatusDto[];
}

export interface WorkspaceDetailDto {
  status: WorkspaceStatusDto;
  providerName: string;
  agents: string[];
  tools: string[];
  skills: string[];
  skillSettings: Record<string, Record<string, string>>;
  mcpServers: string[];
  mcpServerDetails: WorkspaceMcpServerSummaryDto[];
  personas: string[];
  memory: WorkspaceMemoryPolicyConfig;
  reloadPlan?: WorkspaceReloadPlan;
}

export interface WorkspaceMcpServerSummaryDto {
  id: string;
  transport: string;
  command: string;
  args: string[];
  url: string;
}

export interface WorkspaceResourceListResponse {
  workspaceId: string;
  resources: string[];
}

export interface Persona {
  id: string;
  workspaceId: string;
  name: string;
  description: string;
  tone: string;
  boundaries: string[];
  systemPromptTemplate: string;
  enabled: boolean;
  agentNames: string[];
  createdAt: number;
  updatedAt: number;
  deletedAt?: number;
}

export interface PersonaListResponse {
  personas: Persona[];
}

export interface PersonaUpsertDto {
  id?: string;
  workspaceId: string;
  name: string;
  description: string;
  tone: string;
  boundaries: string[];
  systemPromptTemplate: string;
  enabled: boolean;
  agentNames: string[];
}

export interface PersonaResolveRequest {
  workspaceId: string;
  agentName: string;
}

export interface PersonaResolveResponse {
  persona?: Persona;
}

export type MemoryScope = 'GLOBAL' | 'PLATFORM' | 'SESSION' | 'USER' | 'AGENT';
export type MemoryType = 'FACT' | 'PREFERENCE' | 'EVENT' | 'SUMMARY';

export interface MemoryRecord {
  id: string;
  workspaceId: string;
  scope: MemoryScope;
  platformId?: string;
  sessionId?: string;
  userId?: string;
  agentName?: string;
  type: MemoryType;
  content: string;
  tags: string[];
  confidence: number;
  createdAt: number;
  updatedAt: number;
  expiresAt?: number;
  deletedAt?: number;
}

export interface MemoryListResponse {
  memories: MemoryRecord[];
}

export interface MemorySaveRequest {
  content: string;
  type: MemoryType;
  scope: MemoryScope;
  workspaceId: string;
  platformId?: string;
  sessionId?: string;
  userId?: string;
  agentName?: string;
  tags: string[];
  confidence: number;
  expiresAt?: number;
}

export interface MemorySearchRequest {
  query: string;
  workspaceId: string;
  platformId?: string;
  sessionId?: string;
  userId?: string;
  agentName?: string;
  scope?: MemoryScope;
  type?: MemoryType;
  limit: number;
}

export interface MemorySearchResult {
  record: MemoryRecord;
  score: number;
  matchReason: string;
}

export interface MemorySearchResponse {
  results: MemorySearchResult[];
}

export interface DeleteResponse {
  deleted: boolean;
}

export interface ExpireMemoryResponse {
  expired: number;
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

function queryString(values: Record<string, string | number | undefined>) {
  const params = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && String(value).trim().length > 0) params.set(key, String(value));
  });
  const serialized = params.toString();
  return serialized.length > 0 ? `?${serialized}` : '';
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
  workspaces: () => request<WorkspaceListResponse>('/api/workspaces'),
  workspaceDetail: (id: string) => request<WorkspaceDetailDto>(`/api/workspaces/${encodeURIComponent(id)}`),
  reloadWorkspace: (id: string) =>
    request<WorkspaceReloadResult>(`/api/workspaces/${encodeURIComponent(id)}/reload`, { method: 'POST' }),
  reloadWorkspaces: () => request<WorkspaceReloadResult[]>('/api/workspaces/reload', { method: 'POST' }),
  workspaceTools: (id: string) => request<WorkspaceResourceListResponse>(`/api/workspaces/${encodeURIComponent(id)}/tools`),
  workspaceMcp: (id: string) => request<WorkspaceResourceListResponse>(`/api/workspaces/${encodeURIComponent(id)}/mcp`),
  workspaceSkills: (id: string) => request<WorkspaceResourceListResponse>(`/api/workspaces/${encodeURIComponent(id)}/skills`),
  workspacePersonas: (id: string) => request<WorkspaceResourceListResponse>(`/api/workspaces/${encodeURIComponent(id)}/personas`),
  workspaceMemory: (id: string) => request<WorkspaceMemoryPolicyConfig>(`/api/workspaces/${encodeURIComponent(id)}/memory`),
  personas: (workspaceId = 'default') => request<PersonaListResponse>(`/api/personas${queryString({ workspaceId })}`),
  savePersona: (body: PersonaUpsertDto) =>
    request<Persona>('/api/personas', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  updatePersona: (id: string, body: PersonaUpsertDto) =>
    request<Persona>(`/api/personas/${encodeURIComponent(id)}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  deletePersona: (id: string) => request<DeleteResponse>(`/api/personas/${encodeURIComponent(id)}`, { method: 'DELETE' }),
  resolvePersona: (body: PersonaResolveRequest) =>
    request<PersonaResolveResponse>('/api/personas/resolve', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  memories: (params: {
    workspaceId?: string;
    platformId?: string;
    sessionId?: string;
    userId?: string;
    agentName?: string;
    type?: MemoryType | 'all';
    tag?: string;
    limit?: number;
  } = {}) =>
    request<MemoryListResponse>(
      `/api/memory${queryString({
        workspaceId: params.workspaceId ?? 'default',
        platformId: params.platformId,
        sessionId: params.sessionId,
        userId: params.userId,
        agentName: params.agentName,
        type: params.type === 'all' ? undefined : params.type,
        tag: params.tag,
        limit: params.limit ?? 50,
      })}`,
    ),
  saveMemory: (body: MemorySaveRequest) =>
    request<MemoryRecord>('/api/memory', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  searchMemory: (body: MemorySearchRequest) =>
    request<MemorySearchResponse>('/api/memory/search', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  deleteMemory: (
    id: string,
    params: { workspaceId?: string; platformId?: string; sessionId?: string; userId?: string; agentName?: string } = {},
  ) =>
    request<DeleteResponse>(
      `/api/memory/${encodeURIComponent(id)}${queryString({
        workspaceId: params.workspaceId ?? 'default',
        platformId: params.platformId,
        sessionId: params.sessionId,
        userId: params.userId,
        agentName: params.agentName,
      })}`,
      { method: 'DELETE' },
    ),
  expireMemory: () => request<ExpireMemoryResponse>('/api/memory/expire', { method: 'POST' }),
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
