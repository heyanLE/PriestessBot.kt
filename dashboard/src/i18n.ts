import { nextTick, ref } from 'vue';

export type DashboardLanguage = 'en' | 'zh';

const storageKey = 'priestess-dashboard-language';
const initialLanguage = (window.localStorage.getItem(storageKey) === 'zh' ? 'zh' : 'en') as DashboardLanguage;

export const dashboardLanguage = ref<DashboardLanguage>(initialLanguage);

const translations: Record<string, string> = {
  'Runtime Dashboard': '运行时控制台',
  'Dashboard navigation': '控制台导航',
  'Refresh dashboard data': '刷新控制台数据',
  Unknown: '未知',
  'Local runtime controls and operating state.': '本地运行时控制与运行状态。',
  Overview: '总览',
  Platforms: '平台',
  Providers: '提供商',
  Agent: '智能体',
  'Sub-Agents': '子智能体',
  Tools: '工具',
  Workspaces: '工作区',
  'Persona & Memory': '人格与记忆',
  Knowledge: '知识库',
  Conversations: '会话',
  Conversation: '会话',
  Plugins: '插件',
  Logs: '日志',
  Config: '配置',
  'Runtime status': '运行状态',
  'Running platforms': '运行中平台',
  'Enabled plugins': '已启用插件',
  Components: '组件',
  Component: '组件',
  Status: '状态',
  Signal: '信号',
  'Runtime Detail': '运行时详情',
  'Recent Conversations': '最近会话',
  Open: '打开',
  'No component report': '暂无组件报告',
  'Health snapshots appear after the local API responds.': '本地 API 响应后会显示健康快照。',
  'No conversations yet': '暂无会话',
  'Messages will appear after a platform starts receiving traffic.': '平台开始接收消息后会显示会话。',
  Platform: '平台',
  Session: '会话',
  Created: '创建时间',
  Updated: '更新时间',
  'Agent Config': '智能体配置',
  Reset: '重置',
  Save: '保存',
  Provider: '提供商',
  Model: '模型',
  'Max steps': '最大步数',
  'Test Chat': '测试聊天',
  'Runs the edited config without saving first.': '使用当前编辑内容运行测试，不需要先保存。',
  'No test messages': '暂无测试消息',
  'Send a prompt to run the active Agent through the Dashboard API.': '发送提示词，通过控制台 API 运行当前智能体。',
  'Ask the Agent...': '询问智能体...',
  Send: '发送',
  Name: '名称',
  Kind: '类型',
  Capabilities: '能力',
  Vision: '视觉',
  Streaming: '流式',
  'Tool Registry': '工具注册表',
  'Search tools': '搜索工具',
  'All sources': '全部来源',
  'Built-in': '内置',
  Plugin: '插件',
  'All risks': '全部风险',
  'Safe read': '安全读取',
  'Session action': '会话操作',
  'External read': '外部读取',
  'State write': '状态写入',
  'High risk': '高风险',
  'All states': '全部状态',
  Disabled: '已禁用',
  Source: '来源',
  Risk: '风险',
  State: '状态',
  Action: '操作',
  Actions: '操作',
  Required: '必填',
  Allow: '允许',
  Deny: '拒绝',
  Allowed: '已允许',
  Denied: '已拒绝',
  'No matching tools': '没有匹配的工具',
  'Adjust search or filters to widen the registry view.': '调整搜索或过滤条件以扩大工具列表范围。',
  'Tool Detail': '工具详情',
  'Select a row to inspect policy and schema.': '选择一行查看策略和 schema。',
  'No tool selected': '未选择工具',
  'Choose a tool from the registry table.': '从工具注册表中选择一个工具。',
  Description: '描述',
  Policy: '策略',
  'Default on': '默认开启',
  'Default off': '默认关闭',
  Audited: '已审计',
  'Risk level': '风险等级',
  Owner: '所有者',
  'Required parameters': '必填参数',
  None: '无',
  'Configured Platforms': '已配置平台',
  'No platforms configured': '未配置平台',
  'Add platform config through the config view or config file.': '请通过配置页或配置文件添加平台配置。',
  Type: '类型',
  Endpoint: '端点',
  Start: '启动',
  Stop: '停止',
  'Test All': '全部测试',
  Health: '健康',
  'Not tested': '未测试',
  Discover: '发现',
  'No plugins discovered': '未发现插件',
  'Place plugin manifests in the configured plugin directory and discover again.': '将插件 manifest 放入配置的插件目录后重新发现。',
  Version: '版本',
  Load: '加载',
  Enable: '启用',
  Disable: '禁用',
  Unload: '卸载',
  'Runtime Config': '运行时配置',
  'Edit the active JSON config. Sensitive values are returned by the backend as-is.': '编辑当前 JSON 配置。敏感值会按后端原样返回。',
  'Config Backups': '配置备份',
  Refresh: '刷新',
  Backup: '备份',
  Size: '大小',
  Path: '路径',
  Restore: '恢复',
  'No config backups': '暂无配置备份',
  'A backup is created before each saved config replacement.': '每次保存替换配置前都会创建备份。',
  'Live Logs': '实时日志',
  Connect: '连接',
  Clear: '清空',
  'No log events': '暂无日志事件',
  'Connect to the log socket to receive runtime events.': '连接日志 socket 以接收运行时事件。',
  'Knowledge Bases': '知识库',
  'Base name': '知识库名称',
  Create: '创建',
  'No knowledge bases': '暂无知识库',
  'Create a base, then add text documents for retrieval.': '创建知识库后添加文本，用于检索。',
  'Add Document': '添加文档',
  'Document name': '文档名称',
  'Paste Markdown, notes, or plain text...': '粘贴 Markdown、笔记或纯文本...',
  'Search Test': '搜索测试',
  'Runs the same retrieval path used by the Agent tool.': '运行智能体工具使用的同一检索路径。',
  'Search knowledge...': '搜索知识...',
  Search: '搜索',
  'No results loaded': '暂无结果',
  'Submit a query to inspect ranked chunks.': '提交查询以查看排序后的片段。',
  'Back': '返回',
  'No messages': '暂无消息',
  'This conversation has no stored messages yet.': '此会话尚无存储消息。',
  'No text content': '无文本内容',
  'No conversations stored': '暂无已存储会话',
  'Pipeline traffic will create conversation records.': '管线消息流会创建会话记录。',
  'Orchestration Config': '编排配置',
  Routing: '路由',
  Routes: '路由',
  Default: '默认',
  'Rule Editor': '规则编辑器',
  'Structured controls update the JSON draft.': '结构化控件会更新 JSON 草稿。',
  'Enable sub-agent routing': '启用子智能体路由',
  'Default agent': '默认智能体',
  'Primary agent': '主智能体',
  'Agent name': '智能体名称',
  'Add Agent': '添加智能体',
  Remove: '移除',
  'Route name': '路由名称',
  'Target agent': '目标智能体',
  'Keywords, comma separated': '关键词，逗号分隔',
  'Add Route': '添加路由',
  'Route Summary': '路由摘要',
  'Parsed from the current editor draft.': '从当前编辑器草稿解析。',
  'No sub-agents': '暂无子智能体',
  'Add agents to the JSON config to enable routing targets.': '向 JSON 配置添加智能体以启用路由目标。',
  Route: '路由',
  Target: '目标',
  Keywords: '关键词',
  Priority: '优先级',
  'No routes configured.': '未配置路由。',
  'Routing Test': '路由测试',
  'Runs the draft config through the Dashboard sub-agent API.': '通过控制台子智能体 API 运行草稿配置。',
  'Message to route...': '要路由的消息...',
  Test: '测试',
  'No test result': '暂无测试结果',
  'Submit a message to inspect selected agent and route events.': '提交消息以查看选中的智能体和路由事件。',
  'Selected agent': '选中智能体',
  'Selected route': '选中路由',
  Reason: '原因',
  Response: '响应',
  Events: '事件',
  'Persona Registry': '人格注册表',
  Personas: '人格',
  'Enabled personas': '已启用人格',
  'Visible memories': '可见记忆',
  'Search matches': '搜索匹配',
  'workspace id': '工作区 id',
  'Search personas': '搜索人格',
  'No personas': '暂无人格',
  'Create a persona or adjust the workspace and search filters.': '创建人格或调整工作区与搜索过滤条件。',
  Agents: '智能体',
  'Persona scope, tone, boundaries, and injected prompt template.': '人格作用域、语气、边界和注入提示词模板。',
  New: '新建',
  Tone: '语气',
  Boundaries: '边界',
  Enabled: '已启用',
  Delete: '删除',
  'No persona selected': '未选择人格',
  'Choose a row to inspect tone, boundaries, and scoped agents.': '选择一行查看语气、边界和作用域智能体。',
  Workspace: '工作区',
  'All agents': '全部智能体',
  Prompt: '提示词',
  'Memory Workbench': '记忆工作台',
  'Search memory': '搜索记忆',
  'All types': '全部类型',
  Fact: '事实',
  Preference: '偏好',
  Event: '事件',
  Summary: '摘要',
  Expire: '过期',
  'No visible memories': '暂无可见记忆',
  'Save a memory or adjust scope filters to list scoped records.': '保存记忆或调整作用域过滤条件以列出记录。',
  Content: '内容',
  Scope: '作用域',
  Confidence: '置信度',
  'Save Memory': '保存记忆',
  'Write a scoped fact, preference, event, or summary into the runtime store.': '将带作用域的事实、偏好、事件或摘要写入运行时存储。',
  Global: '全局',
  Tags: '标签',
  'Delete Memory': '删除记忆',
  'Search Results': '搜索结果',
  'No memory selected': '未选择记忆',
  'Choose a memory row or run a search.': '选择记忆行或运行搜索。',
  'Running': '运行中',
};

const textOriginals = new WeakMap<Node, string>();
const attrOriginals = new WeakMap<Element, Map<string, string>>();

export function setDashboardLanguage(language: DashboardLanguage) {
  dashboardLanguage.value = language;
  window.localStorage.setItem(storageKey, language);
  document.documentElement.lang = language === 'zh' ? 'zh-CN' : 'en';
  void nextTick(() => applyTranslations());
}

export function toggleDashboardLanguage() {
  setDashboardLanguage(dashboardLanguage.value === 'zh' ? 'en' : 'zh');
}

export function translate(value: string): string {
  if (dashboardLanguage.value !== 'zh') return value;
  return translations[value] ?? value;
}

export function applyTranslations(root: ParentNode = document.body) {
  if (!root) return;
  translateTextNodes(root);
  translateAttributes(root);
}

export function startDashboardI18n(root: ParentNode = document.body) {
  document.documentElement.lang = dashboardLanguage.value === 'zh' ? 'zh-CN' : 'en';
  const frame = window.requestAnimationFrame(() => applyTranslations(root));
  return () => window.cancelAnimationFrame(frame);
}

function translateTextNodes(root: ParentNode) {
  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, {
    acceptNode(node) {
      const parent = node.parentElement;
      if (!parent || ['SCRIPT', 'STYLE', 'TEXTAREA', 'INPUT', 'CODE', 'PRE'].includes(parent.tagName)) {
        return NodeFilter.FILTER_REJECT;
      }
      return node.textContent?.trim() ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_REJECT;
    },
  });
  const nodes: Text[] = [];
  while (walker.nextNode()) nodes.push(walker.currentNode as Text);
  nodes.forEach((node) => {
    const original = textOriginals.get(node) ?? node.textContent ?? '';
    textOriginals.set(node, original);
    node.textContent = translateText(original);
  });
}

function translateAttributes(root: ParentNode) {
  const elements = root instanceof Element ? [root, ...Array.from(root.querySelectorAll('*'))] : Array.from(root.querySelectorAll('*'));
  elements.forEach((element) => {
    ['placeholder', 'title', 'aria-label'].forEach((attr) => {
      const current = element.getAttribute(attr);
      if (!current) return;
      let originals = attrOriginals.get(element);
      if (!originals) {
        originals = new Map();
        attrOriginals.set(element, originals);
      }
      if (!originals.has(attr)) originals.set(attr, current);
      const original = originals.get(attr) ?? current;
      element.setAttribute(attr, translateExact(original));
    });
  });
}

function translateText(text: string): string {
  const leading = text.match(/^\s*/)?.[0] ?? '';
  const trailing = text.match(/\s*$/)?.[0] ?? '';
  const trimmed = text.trim();
  return `${leading}${translateExact(trimmed)}${trailing}`;
}

function translateExact(value: string): string {
  if (dashboardLanguage.value !== 'zh') return value;
  if (value.startsWith('Updated ')) return value.replace(/^Updated /, '已更新 ');
  return translations[value] ?? value;
}
