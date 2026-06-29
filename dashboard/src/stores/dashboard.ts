import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import {
  dashboardApi,
  type ConfigBackup,
  type ConversationDto,
  type DatabaseConfigLayerResponse,
  type EffectiveRuntimePreviewRequest,
  type EffectiveRuntimePreviewResponse,
  type EnvironmentOverrideSummaryResponse,
  type HealthResponse,
  type PlatformStatusDto,
  type PluginListResponse,
  type PriestessConfig,
  type ProviderDto,
  type ToolDto,
  type WorkingDirectorySummaryResponse,
  type WorkspaceListResponse,
  type WorkspaceReloadResult,
} from '../api/dashboard';

export const useDashboardStore = defineStore('dashboard', () => {
  const health = ref<HealthResponse | null>(null);
  const config = ref<PriestessConfig | null>(null);
  const databaseConfigLayer = ref<DatabaseConfigLayerResponse | null>(null);
  const configBackups = ref<ConfigBackup[]>([]);
  const environmentOverrides = ref<EnvironmentOverrideSummaryResponse>({ overrides: [], diagnostics: [] });
  const workingDirectory = ref<WorkingDirectorySummaryResponse | null>(null);
  const effectiveRuntimePreview = ref<EffectiveRuntimePreviewResponse | null>(null);
  const platforms = ref<PlatformStatusDto[]>([]);
  const providers = ref<ProviderDto[]>([]);
  const providerTests = ref<Record<string, boolean>>({});
  const tools = ref<ToolDto[]>([]);
  const workspaces = ref<WorkspaceListResponse>({ workspaces: [] });
  const conversations = ref<ConversationDto[]>([]);
  const plugins = ref<PluginListResponse>({ plugins: [], extensions: [] });
  const loading = ref(false);
  const error = ref<string | null>(null);
  const lastUpdated = ref<number | null>(null);

  const runningPlatforms = computed(() => platforms.value.filter((platform) => platform.running).length);
  const enabledPlatforms = computed(() => platforms.value.filter((platform) => platform.enabled).length);
  const enabledPlugins = computed(() => plugins.value.plugins.filter((plugin) => plugin.state === 'ENABLED').length);

  async function refreshAll() {
    loading.value = true;
    error.value = null;
    try {
      const [
        nextHealth,
        nextConfig,
        nextDatabaseConfigLayer,
        nextEnvironmentOverrides,
        nextWorkingDirectory,
        nextEffectiveRuntimePreview,
        nextBackups,
        nextPlatforms,
        nextProviders,
        nextTools,
        nextWorkspaces,
        nextConversations,
        nextPlugins,
      ] = await Promise.all([
        dashboardApi.health(),
        dashboardApi.config(),
        dashboardApi.databaseConfigLayer(),
        dashboardApi.environmentOverrideSummary(),
        dashboardApi.workingDirectorySummary(),
        dashboardApi.effectiveRuntimePreview(),
        dashboardApi.configBackups(),
        dashboardApi.platforms(),
        dashboardApi.providers(),
        dashboardApi.tools(),
        dashboardApi.workspaces(),
        dashboardApi.conversations(),
        dashboardApi.plugins(),
      ]);
      health.value = nextHealth;
      config.value = nextConfig;
      databaseConfigLayer.value = nextDatabaseConfigLayer;
      environmentOverrides.value = nextEnvironmentOverrides;
      workingDirectory.value = nextWorkingDirectory;
      effectiveRuntimePreview.value = nextEffectiveRuntimePreview;
      configBackups.value = nextBackups;
      platforms.value = nextPlatforms;
      providers.value = nextProviders;
      tools.value = nextTools;
      workspaces.value = nextWorkspaces;
      conversations.value = nextConversations;
      plugins.value = nextPlugins;
      lastUpdated.value = Date.now();
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : String(cause);
    } finally {
      loading.value = false;
    }
  }

  async function setPlatformEnabled(name: string, enabled: boolean) {
    config.value = enabled ? await dashboardApi.startPlatform(name) : await dashboardApi.stopPlatform(name);
    platforms.value = await dashboardApi.platforms();
  }

  async function testProviders() {
    providerTests.value = await dashboardApi.testProviders();
  }

  async function loadWorkspaces() {
    workspaces.value = await dashboardApi.workspaces();
  }

  async function reloadWorkspace(id: string): Promise<WorkspaceReloadResult> {
    const result = await dashboardApi.reloadWorkspace(id);
    await loadWorkspaces();
    return result;
  }

  async function reloadWorkspaces(): Promise<WorkspaceReloadResult[]> {
    const result = await dashboardApi.reloadWorkspaces();
    await loadWorkspaces();
    return result;
  }

  async function discoverPlugins() {
    plugins.value = await dashboardApi.discoverPlugins();
  }

  async function setPluginState(id: string, action: 'load' | 'unload' | 'enable' | 'disable') {
    const api = {
      load: dashboardApi.loadPlugin,
      unload: dashboardApi.unloadPlugin,
      enable: dashboardApi.enablePlugin,
      disable: dashboardApi.disablePlugin,
    }[action];
    plugins.value = await api(id);
  }

  async function saveConfig(nextConfig: PriestessConfig) {
    config.value = await dashboardApi.replaceConfig(nextConfig);
    await refreshAll();
  }

  async function saveDatabaseConfigLayer(nextConfig: PriestessConfig) {
    databaseConfigLayer.value = await dashboardApi.replaceDatabaseConfigLayer(nextConfig);
    config.value = databaseConfigLayer.value.config;
    await refreshConfigSurfaces();
  }

  async function refreshConfigSurfaces() {
    const [nextDatabaseConfigLayer, nextEnvironmentOverrides, nextWorkingDirectory, nextEffectiveRuntimePreview, nextBackups, nextConfig] =
      await Promise.all([
        dashboardApi.databaseConfigLayer(),
        dashboardApi.environmentOverrideSummary(),
        dashboardApi.workingDirectorySummary(),
        dashboardApi.effectiveRuntimePreview(),
        dashboardApi.configBackups(),
        dashboardApi.config(),
      ]);
    databaseConfigLayer.value = nextDatabaseConfigLayer;
    environmentOverrides.value = nextEnvironmentOverrides;
    workingDirectory.value = nextWorkingDirectory;
    effectiveRuntimePreview.value = nextEffectiveRuntimePreview;
    configBackups.value = nextBackups;
    config.value = nextConfig;
    lastUpdated.value = Date.now();
  }

  async function updateWorkingDirectory(path: string) {
    workingDirectory.value = await dashboardApi.updateWorkingDirectorySelection({ path });
    await refreshConfigSurfaces();
  }

  async function previewEffectiveRuntime(request: EffectiveRuntimePreviewRequest = {}) {
    effectiveRuntimePreview.value = await dashboardApi.effectiveRuntimePreview(request);
    return effectiveRuntimePreview.value;
  }

  async function updateToolAllowance(toolName: string, allowed: boolean) {
    if (!config.value) throw new Error('Config is not loaded');
    const nextConfig = JSON.parse(JSON.stringify(config.value)) as PriestessConfig;
    const rootAllowance = applyToolAllowance(nextConfig.agent.enabledTools, nextConfig.agent.disabledTools, toolName, allowed);
    nextConfig.agent.enabledTools = rootAllowance.enabledTools;
    nextConfig.agent.disabledTools = rootAllowance.disabledTools;
    nextConfig.workspaces = (nextConfig.workspaces ?? []).map((workspace) => ({
      ...workspace,
      tools: workspace.tools
        ? {
            ...workspace.tools,
            ...applyToolAllowance(workspace.tools.enabledTools, workspace.tools.disabledTools, toolName, allowed),
          }
        : workspace.tools,
    }));
    await saveConfig(nextConfig);
  }

  function applyToolAllowance(
    enabledTools: string[],
    disabledTools: string[],
    toolName: string,
    allowed: boolean,
  ) {
    const enabled = new Set(enabledTools ?? []);
    const disabled = new Set(disabledTools ?? []);
    if (allowed) {
      enabled.add(toolName);
      disabled.delete(toolName);
    } else {
      enabled.delete(toolName);
      disabled.add(toolName);
    }
    return {
      enabledTools: [...enabled].sort(),
      disabledTools: [...disabled].sort(),
    };
  }

  async function loadConfigBackups() {
    configBackups.value = await dashboardApi.configBackups();
  }

  async function restoreConfigBackup(id: string) {
    config.value = await dashboardApi.restoreConfigBackup(id);
    await refreshAll();
  }

  return {
    health,
    config,
    databaseConfigLayer,
    configBackups,
    environmentOverrides,
    workingDirectory,
    effectiveRuntimePreview,
    platforms,
    providers,
    providerTests,
    tools,
    workspaces,
    conversations,
    plugins,
    loading,
    error,
    lastUpdated,
    runningPlatforms,
    enabledPlatforms,
    enabledPlugins,
    refreshAll,
    setPlatformEnabled,
    testProviders,
    loadWorkspaces,
    reloadWorkspace,
    reloadWorkspaces,
    discoverPlugins,
    setPluginState,
    saveConfig,
    saveDatabaseConfigLayer,
    refreshConfigSurfaces,
    updateWorkingDirectory,
    previewEffectiveRuntime,
    updateToolAllowance,
    loadConfigBackups,
    restoreConfigBackup,
  };
});
