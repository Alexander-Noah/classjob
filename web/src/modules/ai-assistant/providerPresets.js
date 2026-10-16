// OpenAI-compatible chat providers. Model availability follows each provider's console.
export const AI_PROVIDERS = [
  {
    id: 'qwen', label: '通义千问（阿里云百炼）',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    models: ['qwen-plus', 'qwen3.8-max'],
    docsUrl: 'https://help.aliyun.com/zh/model-studio/compatibility-of-openai-with-dashscope',
    hint: '使用百炼对应地域的 API Key；也可填写控制台提供的业务空间专属地址。',
  },
  {
    id: 'deepseek', label: 'DeepSeek（官方）',
    baseUrl: 'https://api.deepseek.com', models: ['deepseek-flash', 'deepseek-v4-pro'],
    docsUrl: 'https://api-docs.deepseek.com/', hint: '填写 DeepSeek 开放平台的 API Key。',
  },
  {
    id: 'siliconflow', label: '硅基流动（DeepSeek / GLM / Kimi）',
    baseUrl: 'https://api.siliconflow.cn/v1',
    models: ['deepseek-ai/DeepSeek-V4-Flash', 'Pro/zai-org/GLM-5.1', 'Pro/moonshotai/Kimi-K2.6'],
    docsUrl: 'https://docs.siliconflow.cn/docs/api/chat-completions-post',
    hint: '使用硅基流动的 API Key，模型名称需与该平台控制台一致。',
  },
  { id: 'custom', label: '其他 / 自定义（OpenAI 兼容）', baseUrl: '', models: [], docsUrl: '', hint: '填写服务商提供的 Base URL、API Key 和完整模型 ID。' },
]

export function normalizedApiEndpoint(value) {
  try {
    const url = new URL(String(value || '').trim())
    if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.search || url.hash) return ''
    url.pathname = url.pathname.replace(/\/+$/, '').replace(/\/chat\/completions$/, '').replace(/\/+$/, '') || '/'
    return url.toString().replace(/\/$/, '')
  } catch { return '' }
}

export function inferAiProvider(baseUrl) {
  const endpoint = normalizedApiEndpoint(baseUrl)
  if (!endpoint) return 'custom'
  const exact = AI_PROVIDERS.find(provider => provider.baseUrl && normalizedApiEndpoint(provider.baseUrl) === endpoint)
  if (exact) return exact.id
  const url = new URL(endpoint)
  if (url.protocol === 'https:' && url.hostname.endsWith('.cn-beijing.maas.aliyuncs.com') && url.pathname === '/compatible-mode/v1') return 'qwen'
  return 'custom'
}

export function selectAiProvider(config, providerId) {
  const provider = AI_PROVIDERS.find(row => row.id === providerId)
  if (!provider || provider.id === 'custom') return { ...config }
  const changed = normalizedApiEndpoint(config.baseUrl) !== normalizedApiEndpoint(provider.baseUrl)
  return { ...config, baseUrl: provider.baseUrl, model: provider.models[0],
    ...(changed ? { apiKey: '', apiKeyConfigured: false } : {}) }
}

export function canReuseSavedAiKey(draft, saved) {
  const endpoint = normalizedApiEndpoint(draft.baseUrl)
  return Boolean(saved?.apiKeyConfigured && endpoint && endpoint === normalizedApiEndpoint(saved.baseUrl))
}

export function aiConfigFingerprint(config = {}) {
  return JSON.stringify([Boolean(config.enabled), normalizedApiEndpoint(config.baseUrl), String(config.model || '').trim(), String(config.systemPrompt || '').trim()])
}
