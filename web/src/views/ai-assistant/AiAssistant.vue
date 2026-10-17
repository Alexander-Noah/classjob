<!--
  Web 管理平台的AI 助手视图组件；模板、响应式状态与局部样式在本文件内协同维护。
-->
<script setup>
import { computed, inject, nextTick, onMounted, onUnmounted, reactive, ref, shallowRef, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, CopyDocument, Delete, Microphone, Promotion, Setting } from '@element-plus/icons-vue'
import { formatMessageContent } from '../../modules/ai-assistant/formatMessage'
import { getAuthToken, getStoredUser } from '../../api/services/auth'
import { normalizeAvatarUrl } from '../../utils/avatarUrl'
import {
  getAiAssistantUserConfig,
  requestAiAssistantAnswer,
  saveAiAssistantUserConfig,
  testAiAssistantUserConfig,
} from '../../api/services/aiAssistantConfig'
import { deleteAiConversationHistory, listAiConversationHistories, saveAiConversationHistories } from '../../api/services/aiAssistantHistory'
import { AssistantAudio } from '../../services/assistantAudio'
import { AssistantWebSocket } from '../../services/assistantWebSocket'
import { BrowserSpeechRecognition } from '../../services/browserSpeechRecognition'
import { AI_PROVIDERS, aiConfigFingerprint, canReuseSavedAiKey, inferAiProvider, normalizedApiEndpoint, selectAiProvider } from '../../modules/ai-assistant/providerPresets'

defineOptions({
  name: 'AiAssistant',
})

const HISTORY_STORAGE_KEY = 'smart-lab:ai-assistant:conversations'
const TTS_STORAGE_KEY = 'smart-lab:ai-tts-enabled'
const MAX_HISTORY_COUNT = 30
const currentUser = inject('sessionUserProfile', computed(() => getStoredUser() || {}))
const currentUserName = computed(() => currentUser.value.realName || currentUser.value.name || currentUser.value.username || '我')
const currentUserAvatar = computed(() => normalizeAvatarUrl(currentUser.value.avatarUrl || currentUser.value.avatar || ''))

const createDefaultDirectAiConfig = () => ({
  enabled: false,
  baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
  apiKey: '',
  apiKeyConfigured: false,
  model: 'qwen-plus',
  systemPrompt: '你是实验室协同管控平台的企业级 AI 助手，回答应专业、简洁、可执行。',
})

const inputValue = ref('')
const messageListRef = ref(null)
const connectionStatus = ref('disconnected')
const isCapturingVoice = ref(false)
const audioLevel = ref(0)
const liveTranscript = ref('')
const voiceMode = ref('')
const speechEnabled = ref(localStorage.getItem(TTS_STORAGE_KEY) !== 'false')
const wsUrl = ref('')
const socketRef = shallowRef(null)
const audioRef = shallowRef(null)
const browserSpeechRef = shallowRef(null)
const conversationHistories = ref([])
const activeConversationId = ref('')
const historyKeyword = ref('')
const isHistoryRestoring = ref(false)
const deletingConversationId = ref('')
const aiConfigDialogVisible = ref(false)
const isDirectAiLoading = ref(false)
const isTestingDirectAi = ref(false)
const directAiConfig = reactive(createDefaultDirectAiConfig())
const testedAiConfigFingerprint = ref('')
const isSavingDirectAi = ref(false)
const draftAiConfigFingerprint = computed(() => JSON.stringify([aiConfigFingerprint(directAiConfig), directAiConfig.apiKey?.trim() || '']))
const hasPassedAiTest = computed(() => testedAiConfigFingerprint.value === draftAiConfigFingerprint.value)
watch(draftAiConfigFingerprint, () => { testedAiConfigFingerprint.value = '' }, { flush: 'sync' })
const providerId = ref('qwen')
const savedDirectAiConfig = ref(null)
const lastApiEndpoint = ref(normalizedApiEndpoint(directAiConfig.baseUrl))
const currentProvider = computed(() => AI_PROVIDERS.find(provider => provider.id === providerId.value) || AI_PROVIDERS.at(-1))
const modelOptions = computed(() => [...new Set([...currentProvider.value.models, directAiConfig.model].filter(Boolean))])
const savedApiKeyAvailable = computed(() => canReuseSavedAiKey(directAiConfig, savedDirectAiConfig.value))
const changeProvider = value => {
  Object.assign(directAiConfig, selectAiProvider(directAiConfig, value))
  lastApiEndpoint.value = normalizedApiEndpoint(directAiConfig.baseUrl)
}
const addressChanged = () => {
  const endpoint = normalizedApiEndpoint(directAiConfig.baseUrl)
  if (endpoint !== lastApiEndpoint.value) { directAiConfig.apiKey = ''; directAiConfig.apiKeyConfigured = false }
  lastApiEndpoint.value = endpoint
  providerId.value = inferAiProvider(directAiConfig.baseUrl)
}

let historySaveTimer = 0
let remoteSaveChain = Promise.resolve()
let pendingRemoteConversations = []
let hasShownSocketError = false

const createWelcomeMessage = () => ({
  id: `welcome-${Date.now()}-${Math.random()}`,
  role: 'assistant',
  content: 'AI 助手已就绪。你可以通过文字或语音咨询实验室管理相关问题。',
  time: '',
})

const createDefaultMessages = () => [createWelcomeMessage()]
const messages = ref(createDefaultMessages())

const sortedConversations = computed(() =>
  [...conversationHistories.value].sort((left, right) => (right.updatedAt || 0) - (left.updatedAt || 0)),
)
const filteredConversations = computed(() => {
  const keyword = historyKeyword.value.trim().toLowerCase()

  if (!keyword) return sortedConversations.value

  return sortedConversations.value.filter((conversation) =>
    [conversation.title, conversation.preview]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword)),
  )
})
const activeConversation = computed(() =>
  sortedConversations.value.find((conversation) => conversation.id === activeConversationId.value),
)
const activeConversationTitle = computed(() => activeConversation.value?.title || 'AI 智能助手')
const isDirectAiConfigured = computed(() =>
  Boolean(
    directAiConfig.enabled &&
      directAiConfig.baseUrl?.trim() &&
      (directAiConfig.apiKey?.trim() || savedApiKeyAvailable.value) &&
      directAiConfig.model?.trim(),
  ),
)
const canSend = computed(() => !deletingConversationId.value && !isDirectAiLoading.value && Boolean(inputValue.value.trim()))
const connectionStatusText = computed(() => {
  if (connectionStatus.value === 'connected') return '服务在线'
  if (connectionStatus.value === 'connecting') return '正在连接'
  return '服务离线'
})
const currentServiceStatusText = computed(() => {
  if (isDirectAiLoading.value) return '正在查询平台数据'
  if (!directAiConfig.enabled) return connectionStatusText.value
  if (!isDirectAiConfigured.value) return '自定义 API 未完成配置'
  return isDirectAiLoading.value ? '自定义 API 调用中' : '自定义 API 已启用'
})

const formatTime = () => {
  const now = new Date()
  return `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
}

const formatConversationTime = (timestamp) => {
  const date = new Date(timestamp || Date.now())
  const now = new Date()
  const timeText = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`

  if (date.toDateString() === now.toDateString()) {
    return timeText
  }

  return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${timeText}`
}

const getMessagePreview = (messageList = []) => {
  const lastMessage = [...messageList]
    .reverse()
    .find((message) => message.content?.trim())

  if (!lastMessage) return '暂无对话内容'

  return lastMessage.content?.trim() || '暂无对话内容'
}

const getConversationTitle = (messageList = []) => {
  const firstUserMessage = messageList.find(
    (message) => message.role === 'user' && message.content?.trim(),
  )
  const title = firstUserMessage?.content?.trim() || '新的咨询'
  return title.length > 18 ? `${title.slice(0, 18)}...` : title
}

const copyMessage = async (content) => {
  try { await navigator.clipboard.writeText(content); ElMessage.success('已复制回答') }
  catch { ElMessage.warning('复制失败，请选中文字后复制') }
}

// 输入法确认候选字时的 Enter 不发送消息，避免中文输入被提前提交。
const handleComposerEnter = (event) => {
  if (event.isComposing) return
  event.preventDefault()
  sendMessage()
}

const normalizeConversation = (conversation = {}) => {
  const messageList = Array.isArray(conversation.messages) && conversation.messages.length
    ? conversation.messages
    : createDefaultMessages()

  return {
    id: conversation.id || `conversation-${Date.now()}-${Math.random()}`,
    title: conversation.title || getConversationTitle(messageList),
    preview: conversation.preview || getMessagePreview(messageList),
    updatedAt: conversation.updatedAt || Date.now(),
    messages: messageList.map((message) => ({
      id: message.id || `${Date.now()}-${Math.random()}`,
      role: message.role === 'user' ? 'user' : 'assistant',
      content: message.content || '',
      time: message.time || '',
    })),
  }
}

const createConversation = () => ({
  id: `conversation-${Date.now()}-${Math.random()}`,
  title: '新的咨询',
  preview: '暂无对话内容',
  updatedAt: Date.now(),
  messages: createDefaultMessages(),
})

const getHistoryStorageKey = () => {
  const user = getStoredUser()
  const owner = user?.id || user?.username || 'anonymous'
  return `${HISTORY_STORAGE_KEY}:${owner}`
}

const writeLocalConversations = (conversationList) => {
  const conversations = conversationList.slice(0, MAX_HISTORY_COUNT)

  try {
    localStorage.setItem(getHistoryStorageKey(), JSON.stringify(conversations))
    localStorage.removeItem(HISTORY_STORAGE_KEY)
  } catch (error) {
    console.warn('[AiAssistant] conversation storage failed', error)
  }
}

const readLocalConversations = () => {
  try {
    const rawConversations =
      localStorage.getItem(getHistoryStorageKey()) || localStorage.getItem(HISTORY_STORAGE_KEY) || '[]'
    const storedConversations = JSON.parse(rawConversations)
    return Array.isArray(storedConversations) ? storedConversations : []
  } catch {
    return []
  }
}

const mergeConversationLists = (...conversationLists) => {
  const conversationMap = new Map()

  conversationLists
    .flat()
    .filter(Boolean)
    .map(normalizeConversation)
    .forEach((conversation) => {
      const currentConversation = conversationMap.get(conversation.id)
      if (!currentConversation || (conversation.updatedAt || 0) >= (currentConversation.updatedAt || 0)) {
        conversationMap.set(conversation.id, conversation)
      }
    })

  return [...conversationMap.values()]
    .sort((left, right) => (right.updatedAt || 0) - (left.updatedAt || 0))
    .slice(0, MAX_HISTORY_COUNT)
}

const applyConversationList = (conversationList = []) => {
  const nextConversations = mergeConversationLists(conversationList)
  conversationHistories.value = nextConversations.length ? nextConversations : [createConversation()]

  const nextActiveConversation =
    conversationHistories.value.find((conversation) => conversation.id === activeConversationId.value) ||
    sortedConversations.value[0]
  activeConversationId.value = nextActiveConversation.id
  messages.value = nextActiveConversation.messages
}

const flushRemoteSave = async () => {
  if (historySaveTimer) {
    window.clearTimeout(historySaveTimer)
    historySaveTimer = 0
  }

  const conversations = pendingRemoteConversations.length
    ? pendingRemoteConversations
    : conversationHistories.value.slice(0, MAX_HISTORY_COUNT)
  if (!conversations.length) return

  try {
    pendingRemoteConversations = []
    remoteSaveChain = remoteSaveChain.catch(() => {}).then(() => saveAiConversationHistories(conversations))
    await remoteSaveChain
  } catch (error) {
    console.warn('[AiAssistant] failed to save conversation history', error)
  }
}

const queueRemoteSave = (conversationList = conversationHistories.value) => {
  if (isHistoryRestoring.value) return

  pendingRemoteConversations = conversationList.map(normalizeConversation).slice(0, MAX_HISTORY_COUNT)
  if (historySaveTimer) {
    window.clearTimeout(historySaveTimer)
  }
  historySaveTimer = window.setTimeout(flushRemoteSave, 500)
}

const saveConversations = ({ syncRemote = true } = {}) => {
  const conversations = conversationHistories.value.slice(0, MAX_HISTORY_COUNT)
  writeLocalConversations(conversations)
  if (syncRemote) {
    queueRemoteSave(conversations)
  }
}

const persistCurrentConversation = () => {
  if (deletingConversationId.value) return
  if (!activeConversationId.value) return

  const nextConversation = normalizeConversation({
    id: activeConversationId.value,
    title: getConversationTitle(messages.value),
    preview: getMessagePreview(messages.value),
    updatedAt: Date.now(),
    messages: messages.value,
  })
  const index = conversationHistories.value.findIndex((conversation) => conversation.id === activeConversationId.value)

  if (index >= 0) {
    conversationHistories.value.splice(index, 1, nextConversation)
  } else {
    conversationHistories.value.unshift(nextConversation)
  }

  conversationHistories.value = sortedConversations.value.slice(0, MAX_HISTORY_COUNT)
  saveConversations()
}

const loadConversations = async () => {
  applyConversationList(readLocalConversations())
  saveConversations({ syncRemote: false })

  let shouldSeedRemote = false
  isHistoryRestoring.value = true
  try {
    const remoteConversations = await listAiConversationHistories()
    if (remoteConversations.length) {
      applyConversationList(mergeConversationLists(conversationHistories.value, remoteConversations))
      saveConversations({ syncRemote: false })
      shouldSeedRemote = true
    } else {
      shouldSeedRemote = true
    }
  } catch (error) {
    console.warn('[AiAssistant] failed to load remote conversation history', error)
  } finally {
    isHistoryRestoring.value = false
    if (shouldSeedRemote) {
      queueRemoteSave()
    }
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

const startNewConversation = async () => {
  if (deletingConversationId.value) return
  persistCurrentConversation()
  const conversation = createConversation()
  conversationHistories.value = [conversation, ...sortedConversations.value].slice(0, MAX_HISTORY_COUNT)
  activeConversationId.value = conversation.id
  messages.value = conversation.messages
  inputValue.value = ''
  saveConversations()
  await scrollToBottom()
}

const switchConversation = async (conversationId) => {
  if (deletingConversationId.value) return
  if (conversationId === activeConversationId.value) return
  persistCurrentConversation()

  const conversation = conversationHistories.value.find((item) => item.id === conversationId)
  if (!conversation) return

  activeConversationId.value = conversation.id
  messages.value = conversation.messages
  inputValue.value = ''
  await scrollToBottom()
}

const deleteConversation = async (conversation) => {
  if (isHistoryRestoring.value || deletingConversationId.value || isDirectAiLoading.value) return
  try {
    await ElMessageBox.confirm(`确定删除“${conversation.title}”及其全部聊天记录吗？删除后无法恢复。`, '删除聊天记录', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  if (isDirectAiLoading.value || deletingConversationId.value) return
  deletingConversationId.value = conversation.id
  try {
    // Drain earlier upserts before deleting so they cannot recreate this conversation.
    await flushRemoteSave()
    await deleteAiConversationHistory(conversation.id)
    const wasActive = activeConversationId.value === conversation.id
    const remaining = sortedConversations.value.filter(item => item.id !== conversation.id)
    conversationHistories.value = remaining.length ? remaining : [createConversation()]
    if (wasActive) {
      const next = conversationHistories.value[0]
      activeConversationId.value = next.id
      messages.value = next.messages
      inputValue.value = ''
      stopVoiceCapture()
      window.speechSynthesis?.cancel()
    }
    saveConversations()
    ElMessage.success('聊天记录已删除')
    await scrollToBottom()
  } catch (error) {
    ElMessage.error(error?.message || '删除失败，请重试')
  } finally {
    deletingConversationId.value = ''
  }
}

const appendMessage = async (role, content, { shouldSpeak = false } = {}) => {
  if (deletingConversationId.value) return
  if (!content?.trim()) return

  messages.value.push({
    id: `${Date.now()}-${Math.random()}`,
    role,
    content,
    time: formatTime(),
  })
  persistCurrentConversation()

  if (shouldSpeak && content?.trim()) {
    speakText(content)
  }

  await scrollToBottom()
}

const appendAssistantText = async (content) => {
  await appendMessage('assistant', content, { shouldSpeak: true })
}

const handleSocketTextMessage = async (message) => {
  if (message.type === 'hello') return

  if (message.type === 'stt') {
    if (message.state === 'error') {
      audioRef.value?.stopCapture()
      isCapturingVoice.value = false
      voiceMode.value = ''
      liveTranscript.value = ''
      await appendAssistantText(message.text || '语音识别服务暂不可用，请检查后端语音模型配置。')
      return
    }

    const transcript = message.text?.trim()
    if (transcript) {
      inputValue.value = transcript
      liveTranscript.value = transcript
      ElMessage.success('语音已转为文字，可编辑后发送')
    }
    return
  }

  if (message.type === 'tts' && message.state === 'sentence_start' && message.text?.trim()) {
    await appendAssistantText(message.text)
    return
  }

  if (message.type === 'llm' && message.text?.trim()) {
    await appendAssistantText(message.text)
    return
  }

  if (message.type === 'text' && message.text?.trim()) {
    await appendAssistantText(message.text)
  }
}

const ensureServices = () => {
  if (!socketRef.value) {
    socketRef.value = new AssistantWebSocket({
      onStatusChange: (status) => {
        connectionStatus.value = status
        if (status === 'connected') {
          hasShownSocketError = false
        }
      },
      onTextMessage: handleSocketTextMessage,
      onAudioMessage: () => {},
      onError: handleAssistantSocketError,
    })
  }

  if (!audioRef.value) {
    audioRef.value = new AssistantAudio({
      onLevelChange: (level) => {
        audioLevel.value = level
      },
      onAudioFrame: (frame) => {
        socketRef.value?.sendBinary(frame)
      },
    })
  }

  if (!browserSpeechRef.value) {
    browserSpeechRef.value = new BrowserSpeechRecognition({
      onTranscript: (text) => {
        liveTranscript.value = text
      },
      onError: () => {
        if (voiceMode.value === 'browser' && isCapturingVoice.value) {
          browserSpeechRef.value?.abort()
          startServerVoiceCapture().catch(() => {
            isCapturingVoice.value = false
            voiceMode.value = ''
            liveTranscript.value = ''
            ElMessage.error('语音识别启动失败，请检查浏览器麦克风权限')
          })
          return
        }

        isCapturingVoice.value = false
        voiceMode.value = ''
        liveTranscript.value = ''
        ElMessage.error('语音识别启动失败，请检查浏览器麦克风权限')
      },
    })
  }
}

const buildWsProtocols = () => {
  const token = getAuthToken()
  if (!token) return []

  return ['smart-lab.ai.v1', `smart-lab.jwt.${token}`]
}

const handleAssistantSocketError = () => {
  if (directAiConfig.enabled) return
  if (hasShownSocketError) return
  hasShownSocketError = true
  ElMessage.error('智能助手连接失败，请点击“重新连接”后重试')
}

const connectService = () => {
  if (directAiConfig.enabled) return
  if (!wsUrl.value || socketRef.value?.isConnected) return
  ensureServices()
  socketRef.value.connect(wsUrl.value, buildWsProtocols())
}

const fetchConfigAndConnect = async () => {
  try {
    const token = getAuthToken()
    const response = await fetch('/api/ai-assistant/config', {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    })
    const json = await response.json()
    const config = json?.data || {}
    wsUrl.value = config.ws_proxy_url || config.ws_url || ''
    connectService()
  } catch {
    if (directAiConfig.enabled) return
    ElMessage.error('AI 服务配置读取失败')
  }
}

const resetDirectAiConfig = () => {
  Object.assign(directAiConfig, createDefaultDirectAiConfig())
  providerId.value = 'qwen'
  lastApiEndpoint.value = normalizedApiEndpoint(directAiConfig.baseUrl)
}

const applyBackendAiConfig = (config = {}) => {
  Object.assign(directAiConfig, {
    ...createDefaultDirectAiConfig(),
    ...config,
    apiKey: '',
    apiKeyConfigured: Boolean(config.apiKeyConfigured),
  })
  providerId.value = inferAiProvider(directAiConfig.baseUrl)
  savedDirectAiConfig.value = { ...directAiConfig }
  lastApiEndpoint.value = normalizedApiEndpoint(directAiConfig.baseUrl)
}

const loadBackendAiConfig = async () => {
  try {
    applyBackendAiConfig(await getAiAssistantUserConfig())
  } catch (error) {
    console.warn('[AiAssistant] failed to load user ai config', error)
  }
}

const saveBackendAiConfig = async () => {
  if (isSavingDirectAi.value || isTestingDirectAi.value) return
  if (directAiConfig.enabled && !hasPassedAiTest.value) {
    ElMessage.warning('请先测试连接，测试成功后再保存配置')
    return
  }
  if (!normalizedApiEndpoint(directAiConfig.baseUrl)) {
    ElMessage.warning('请填写有效的 HTTP 或 HTTPS 接口基础地址')
    return
  }
  if (directAiConfig.enabled && !isDirectAiConfigured.value) {
    ElMessage.warning('请填写 API 地址、API Key 和文本模型')
    return
  }

  isSavingDirectAi.value = true
  try {
    const savedConfig = await saveAiAssistantUserConfig({
      enabled: Boolean(directAiConfig.enabled),
      baseUrl: directAiConfig.baseUrl?.trim() || '',
      apiKey: directAiConfig.apiKey?.trim() || '',
      model: directAiConfig.model?.trim() || '',
      systemPrompt: directAiConfig.systemPrompt?.trim() || createDefaultDirectAiConfig().systemPrompt,
    })
    applyBackendAiConfig(savedConfig)
    aiConfigDialogVisible.value = false
    ElMessage.success('AI 接口配置已保存')
    if (!directAiConfig.enabled) await fetchConfigAndConnect()
  } catch (error) {
    ElMessage.error(error?.message || 'AI 接口配置保存失败')
  } finally {
    isSavingDirectAi.value = false
  }
}

const testBackendAiConfig = async () => {
  if (isTestingDirectAi.value || isSavingDirectAi.value) return
  if (!directAiConfig.enabled) {
    ElMessage.warning('请先启用自定义 API')
    return
  }

  if (!isDirectAiConfigured.value) {
    ElMessage.warning('请填写 API 地址、API Key 和文本模型')
    return
  }

  isTestingDirectAi.value = true
  testedAiConfigFingerprint.value = ''
  const fingerprint = draftAiConfigFingerprint.value
  const draft = { ...directAiConfig }
  try {
    const result = await testAiAssistantUserConfig(draft)
    if (draftAiConfigFingerprint.value !== fingerprint) {
      ElMessage.warning('配置已修改，请重新测试连接')
      return
    }
    testedAiConfigFingerprint.value = fingerprint
    ElMessage.success(`AI 接口连接正常：${result?.model || draft.model}，可以保存配置`)
  } catch (error) {
    ElMessage.error(error?.message || 'AI 接口连接测试失败，请检查 API 地址、Key、模型名称或服务器网络')
  } finally {
    isTestingDirectAi.value = false
  }
}

const requestBackendAiAnswer = async (question, history) => {
  const payload = await requestAiAssistantAnswer({
    question,
    history,
  })
  const answer = String(payload?.answer || '').trim()
  if (!answer) {
    throw new Error('AI 接口未返回可展示内容')
  }
  return answer
}

const submitQuestion = async (text, onSubmitted) => {
  if (deletingConversationId.value || isDirectAiLoading.value) return false
  const question = text?.trim()
  if (!question) return false

  if (directAiConfig.enabled) {
    if (directAiConfig.apiKey?.trim() || aiConfigFingerprint(directAiConfig) !== aiConfigFingerprint(savedDirectAiConfig.value || {})) {
      aiConfigDialogVisible.value = true
      ElMessage.warning('请先保存当前模型服务商和模型配置')
      return false
    }
    if (!isDirectAiConfigured.value) {
      aiConfigDialogVisible.value = true
      ElMessage.warning('请先完成 AI 接口配置')
      return false
    }
  }

  const conversationId = activeConversationId.value
  const history = messages.value.slice(-12).map(({ role, content }) => ({ role, content }))
  isDirectAiLoading.value = true
  await appendMessage('user', question)
  onSubmitted?.()
  try {
    const answer = await requestBackendAiAnswer(question, history)
    if (activeConversationId.value === conversationId) {
      await appendAssistantText(answer)
    } else {
      const conversation = conversationHistories.value.find(item => item.id === conversationId)
      if (conversation) {
        conversation.messages.push({ id: `${Date.now()}-${Math.random()}`, role: 'assistant', content: answer, time: formatTime() })
        conversation.preview = getMessagePreview(conversation.messages)
        conversation.updatedAt = Date.now()
        saveConversations()
      }
    }
    return true
  } catch (error) {
    if (activeConversationId.value === conversationId) {
      await appendAssistantText(`AI 查询失败：${error?.message || '请检查 AI 配置或稍后重试。'}`)
    } else {
      ElMessage.error(error?.message || 'AI 查询失败，请重试')
    }
    return true
  } finally {
    isDirectAiLoading.value = false
  }
}

const sendMessage = async () => {
  if (isDirectAiLoading.value) return
  const text = inputValue.value.trim()

  if (!text) {
    ElMessage.warning('请输入咨询内容')
    return
  }

  await submitQuestion(text, () => {
    if (inputValue.value.trim() === text) inputValue.value = ''
  })
}

const toggleSpeech = (value) => {
  speechEnabled.value = value
  localStorage.setItem(TTS_STORAGE_KEY, value ? 'true' : 'false')
  if (!value) {
    window.speechSynthesis?.cancel()
  }
}

const speakText = (text) => {
  if (!speechEnabled.value || !window.speechSynthesis || !text?.trim()) return
  window.speechSynthesis.cancel()
  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = 'zh-CN'
  utterance.rate = 1
  utterance.pitch = 1
  window.speechSynthesis.speak(utterance)
}

const startServerVoiceCapture = async () => {
  voiceMode.value = 'server'
  socketRef.value.sendAbort()
  socketRef.value.sendListenStart()
  await audioRef.value.startCapture()
  isCapturingVoice.value = true
}

const startVoiceCapture = async () => {
  ensureServices()
  if (!socketRef.value?.isConnected) {
    connectService()
    ElMessage.warning('AI 服务正在连接，请稍后再试')
    return
  }

  try {
    window.speechSynthesis?.cancel()
    liveTranscript.value = ''
    isCapturingVoice.value = true

    await startServerVoiceCapture()
  } catch (error) {
    isCapturingVoice.value = false
    voiceMode.value = ''
    liveTranscript.value = ''
    ElMessage.error(error?.message || '无法启动麦克风')
  }
}

const stopVoiceCapture = async () => {
  if (!isCapturingVoice.value) return

  if (voiceMode.value === 'browser') {
    const transcript = await browserSpeechRef.value?.stop()
    isCapturingVoice.value = false
    voiceMode.value = ''
    liveTranscript.value = ''

    if (!transcript?.trim()) {
      await appendAssistantText('未识别到有效语音内容，请重新输入。')
      return
    }

    inputValue.value = transcript.trim()
    ElMessage.success('语音已转为文字，可编辑后发送')
    return
  }

  if (voiceMode.value === 'server') {
    socketRef.value?.sendListenStop()
  }
  audioRef.value?.stopCapture()
  isCapturingVoice.value = false
  voiceMode.value = ''
  liveTranscript.value = ''
}

const clearMessages = () => {
  if (deletingConversationId.value) return
  messages.value = createDefaultMessages()
  persistCurrentConversation()
  scrollToBottom()
}

onMounted(async () => {
  loadConversations().finally(scrollToBottom)
  await loadBackendAiConfig()
  if (!directAiConfig.enabled) {
    await fetchConfigAndConnect()
  }
})

onUnmounted(() => {
  persistCurrentConversation()
  flushRemoteSave()
  stopVoiceCapture()
  browserSpeechRef.value?.abort()
  socketRef.value?.disconnect()
  audioRef.value?.destroy()
  window.speechSynthesis?.cancel()
})
</script>

<template>
  <section class="assistant-shell">
    <aside class="history-panel">
      <div class="history-searchbar">
        <el-input v-model="historyKeyword" clearable placeholder="搜索聊天记录" aria-label="搜索聊天记录" />
        <el-button class="new-chat-button" circle aria-label="新建聊天" title="新建聊天" @click="startNewConversation">+</el-button>
      </div>

      <div class="history-list">
        <div
          v-for="conversation in filteredConversations"
          :key="conversation.id"
          class="history-item"
          :class="{ active: conversation.id === activeConversationId }"
        >
          <button class="history-select" type="button" :aria-pressed="conversation.id === activeConversationId" @click="switchConversation(conversation.id)">
          <span class="history-avatar">
            <el-icon><ChatDotRound /></el-icon>
          </span>
          <span class="history-main">
            <span class="history-topline">
              <span class="history-item-title">{{ conversation.title }}</span>
              <span class="history-item-time">{{ formatConversationTime(conversation.updatedAt) }}</span>
            </span>
            <span class="history-item-preview">{{ conversation.preview }}</span>
          </span>
          </button>
          <el-button
            class="history-delete"
            :icon="Delete"
            text
            circle
            :aria-label="`删除聊天：${conversation.title}`"
            title="删除聊天记录"
            :loading="deletingConversationId === conversation.id"
            :disabled="isHistoryRestoring || isDirectAiLoading || Boolean(deletingConversationId)"
            @click.stop="deleteConversation(conversation)"
          />
        </div>

        <el-empty v-if="!filteredConversations.length" description="暂无会话" />
      </div>
    </aside>

    <main class="chat-main">
      <header class="chat-topbar">
        <div class="chat-title-block">
          <h1>{{ activeConversationTitle }}</h1>
          <span class="chat-subtitle">
            <i class="status-dot" :class="connectionStatus"></i>
            {{ currentServiceStatusText }}
            <span class="data-context" title="数据范围不含设备资产">平台业务数据已接入</span>
            <span class="message-count">{{ messages.length }} 条消息</span>
          </span>
        </div>

        <div class="header-actions">
          <el-button v-if="!directAiConfig.enabled && connectionStatus !== 'connected'" :loading="connectionStatus === 'connecting'" @click="fetchConfigAndConnect">重新连接</el-button>
          <el-button :icon="Setting" plain @click="aiConfigDialogVisible = true">API 配置</el-button>
          <el-switch v-model="speechEnabled" active-text="播报" inactive-text="静音" @change="toggleSpeech" />
          <el-button
            v-if="!isCapturingVoice"
            :icon="Microphone"
            :disabled="connectionStatus !== 'connected'"
            @click="startVoiceCapture"
          >
            语音输入
          </el-button>
          <el-button v-else type="danger" @click="stopVoiceCapture">结束识别</el-button>
          <el-button plain @click="clearMessages">清空</el-button>
        </div>
      </header>

      <div v-if="isCapturingVoice" class="voice-status">
        <span>{{ liveTranscript || (voiceMode === 'browser' ? '正在听取语音，请说话' : '正在识别语音') }}</span>
        <i :style="{ width: `${Math.max(8, Math.min(100, audioLevel * 420))}%` }"></i>
      </div>

      <div ref="messageListRef" class="message-list">
        <article v-for="message in messages" :key="message.id" class="message-row" :class="message.role">
          <el-avatar
            v-if="message.role === 'user'"
            class="avatar user"
            :src="currentUserAvatar"
            :size="44"
            :aria-label="`${currentUserName}的头像`"
            shape="square"
          >{{ currentUserName.slice(0, 1) }}</el-avatar>
          <div v-else class="avatar assistant">智</div>
          <div class="message-column">
            <div class="message-meta">
              <strong>{{ message.role === 'assistant' ? '智能助手' : currentUserName }}</strong>
              <span>{{ message.time }}</span>
            </div>
            <div class="message-bubble">
              <div
                v-if="message.content"
                class="message-content"
                v-html="formatMessageContent(message.content)"
              ></div>
            </div>
            <div v-if="message.role === 'assistant' && message.content" class="message-actions">
              <el-button text size="small" :icon="CopyDocument" aria-label="复制回答" @click="copyMessage(message.content)">复制</el-button>
            </div>
          </div>
        </article>
      </div>

      <footer class="composer">
        <div class="composer-box">
        <el-input
          v-model="inputValue"
          :autosize="{ minRows: 1, maxRows: 5 }"
          class="composer-input"
          placeholder="询问实验室、课表或预约安排…"
          aria-label="咨询内容"
          resize="none"
          type="textarea"
          @keydown.enter.exact="handleComposerEnter"
        />
        <div class="composer-footer">
          <div class="composer-tools">
            <button
              class="micro-tool"
              :class="{ active: isCapturingVoice }"
              :disabled="connectionStatus !== 'connected'"
              type="button"
              :aria-label="isCapturingVoice ? '结束语音识别' : '语音输入'"
              @click="isCapturingVoice ? stopVoiceCapture() : startVoiceCapture()"
            >
              <el-icon><Microphone /></el-icon>
            </button>
            <span class="composer-hint">Enter 发送 · Shift + Enter 换行</span>
          </div>
          <el-button :disabled="!canSend" :loading="isDirectAiLoading" :icon="Promotion" type="primary" @click="sendMessage">发送</el-button>
        </div>
        </div>
      </footer>
    </main>

    <el-dialog v-model="aiConfigDialogVisible" title="AI 接口配置" width="min(800px, calc(100vw - 32px))" align-center :close-on-click-modal="false" append-to-body>
      <div class="api-config-panel">
        <el-alert
          show-icon
          :closable="false"
          type="info"
          title="配置保存到当前登录用户账号下，其他用户不可见。API Key 不会在页面回显。"
        />

        <el-form label-position="top" class="api-config-form">
          <el-form-item label="启用自定义 API" class="api-config-full-row">
            <el-switch v-model="directAiConfig.enabled" active-text="启用" inactive-text="停用" />
          </el-form-item>

          <el-form-item label="模型服务商">
            <el-select v-model="providerId" placeholder="选择模型服务商" style="width: 100%" @change="changeProvider">
              <el-option v-for="provider in AI_PROVIDERS" :key="provider.id" :label="provider.label" :value="provider.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="文本模型">
            <el-select v-model="directAiConfig.model" filterable allow-create default-first-option placeholder="选择模型或输入完整模型 ID" style="width: 100%">
              <el-option v-for="model in modelOptions" :key="model" :label="model" :value="model" />
            </el-select>
          </el-form-item>

          <div class="api-config-full-row provider-hint">
            <span>{{ currentProvider.hint }}</span>
            <el-link v-if="currentProvider.docsUrl" :href="currentProvider.docsUrl" target="_blank" rel="noopener noreferrer" type="primary">官方接入文档</el-link>
          </div>

          <el-form-item label="API 地址">
            <el-input
              v-model="directAiConfig.baseUrl"
              placeholder="https://dashscope.aliyuncs.com/compatible-mode/v1"
              @change="addressChanged"
            />
          </el-form-item>

          <el-form-item :label="savedApiKeyAvailable ? 'API Key（已保存，留空则不修改）' : 'API Key'">
            <el-input
              v-model="directAiConfig.apiKey"
              clearable
              placeholder="请输入当前账号要使用的 API Key"
              show-password
              type="password"
            />
          </el-form-item>

          <el-form-item label="系统提示词" class="api-config-full-row">
            <el-input
              v-model="directAiConfig.systemPrompt"
              :autosize="{ minRows: 4, maxRows: 6 }"
              resize="none"
              type="textarea"
            />
          </el-form-item>
        </el-form>

      </div>
      <template #footer>
        <div class="api-config-actions">
          <span v-if="directAiConfig.enabled" class="config-test-status">{{ hasPassedAiTest ? '连接测试成功，可以保存' : '请先测试连接，成功后再保存' }}</span>
          <el-button plain :disabled="isTestingDirectAi || isSavingDirectAi" @click="resetDirectAiConfig">恢复默认</el-button>
          <el-button plain :loading="isTestingDirectAi" :disabled="isSavingDirectAi" @click="testBackendAiConfig">测试连接</el-button>
          <el-button type="primary" :loading="isSavingDirectAi" :disabled="isTestingDirectAi || (directAiConfig.enabled && !hasPassedAiTest)" @click="saveBackendAiConfig">保存配置</el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.assistant-shell {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  height: calc(100vh - 112px);
  max-height: calc(100vh - 112px);
  min-height: min(620px, calc(100dvh - 112px));
  overflow: hidden;
  border: 1px solid var(--border-color);
  border-radius: 18px;
  background: transparent;
}

.history-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  border-right: 1px solid #d9d9dc;
  background: rgba(235, 242, 252, 0.18);
}

.history-searchbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 0 0 auto;
  height: 74px;
  padding: 16px;
  border-bottom: 1px solid #dedee2;
}

.history-searchbar :deep(.el-input__wrapper) {
  min-height: 38px;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 6px;
  box-shadow: none;
}

.new-chat-button {
  flex: 0 0 auto;
  width: 34px;
  height: 34px;
  color: #303133;
  background: transparent;
  border-color: #aeb1b8;
  font-size: 22px;
  line-height: 1;
}

.history-list {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 78px;
  padding: 12px 14px;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  transition: background 0.16s ease;
}

.history-item:hover {
  background: rgba(255, 255, 255, 0.55);
}

.history-select {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
  padding: 0;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
  font: inherit;
}

.history-delete {
  flex-shrink: 0;
  color: #737985;
}

.history-item.active .history-delete {
  color: white;
}

.history-delete:not(:disabled):hover {
  color: #f56c6c;
  background: #fef0f0;
}

.history-item.active {
  background: rgba(219, 234, 254, 0.52);
  box-shadow: inset 3px 0 #3b82f6;
}

.history-avatar,
.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 44px;
  height: 44px;
  border-radius: 7px;
  background: #ffffff;
  color: #07c160;
  font-size: 20px;
  font-weight: 700;
}

.history-main {
  display: grid;
  gap: 6px;
  min-width: 0;
  flex: 1 1 auto;
}

.history-topline {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.history-item-title,
.history-item-preview,
.history-item-time {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-item-title {
  color: #111827;
  font-size: 15px;
  font-weight: 600;
}

.history-item-preview {
  color: #8b9099;
  font-size: 13px;
}

.history-item-time {
  color: #9aa0aa;
  font-size: 12px;
}

.history-item.active .history-avatar {
  background: rgba(255, 255, 255, 0.92);
}

.history-item.active .history-item-title,
.history-item.active .history-item-preview,
.history-item.active .history-item-time {
  color: #1e40af;
}

.history-item.active .history-delete {
  color: #15803d;
}

.chat-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  background: transparent;
}

.chat-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex: 0 0 auto;
  min-height: 74px;
  padding: 12px 24px;
  flex-wrap: wrap;
  border-bottom: 1px solid #e5e5e5;
  background: rgba(255, 255, 255, 0.2);
}

.chat-title-block {
  min-width: 0;
}

.chat-title-block h1 {
  margin: 0;
  overflow: hidden;
  color: #111827;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-title-block > .chat-subtitle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  color: #8b9099;
  font-size: 12px;
  flex-wrap: wrap;
}

.chat-subtitle .data-context { padding-left: 10px; border-left: 1px solid #d5dfed; }
.chat-subtitle .message-count { color: #64748b; }
.header-actions { margin-left: auto; }

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #b8bdc7;
}

.status-dot.connected {
  background: #07c160;
}

.status-dot.connecting {
  background: #e6a23c;
}

.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.api-config-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
  max-height: calc(100dvh - 190px);
  overflow-y: auto;
  padding-right: 4px;
}

.api-config-panel :deep(.el-form-item) {
  margin-bottom: 16px;
  min-width: 0;
}

.api-config-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 24px;
}

.api-config-full-row {
  grid-column: 1 / -1;
}

.provider-hint {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 13px;
  margin: -4px 0 18px;
}

@media (max-width: 600px) {
  .api-config-form {
    grid-template-columns: minmax(0, 1fr);
  }
}

.api-config-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 8px;
}

.config-test-status {
  align-self: center;
  margin-right: auto;
  color: var(--text-secondary);
  font-size: 13px;
}

.voice-status {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  flex: 0 0 auto;
  padding: 10px 24px;
  border-bottom: 1px solid #dbeafe;
  background: #eef7ff;
  color: #1677ff;
  font-size: 13px;
}

.voice-status i {
  display: block;
  height: 6px;
  border-radius: 999px;
  background: #1677ff;
  transition: width 0.12s ease;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 26px;
  flex: 1 1 auto;
  min-height: 0;
  padding: 28px clamp(20px, 3vw, 48px) 24px;
  overflow-y: auto;
  background: transparent;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: #cbd5e1 transparent;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  width: fit-content;
  max-width: min(920px, 94%);
}

.message-row.user {
  align-self: flex-end;
  flex-direction: row-reverse;
  max-width: min(680px, 82%);
}

.avatar {
  color: #ffffff;
  background: #4f6bed;
  font-size: 15px;
}

.avatar.user {
  color: #ffffff;
  background: #07c160;
}

.avatar.user :deep(img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-column {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.message-row.user .message-column {
  justify-items: end;
}

.message-bubble {
  position: relative;
  min-width: 0;
  padding: 16px 20px;
  color: #111827;
  background: rgba(255, 255, 255, 0.24);
  border: 1px solid #e9eaec;
  border-radius: 14px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.message-bubble::before {
  display: none;
  position: absolute;
  top: 12px;
  left: -6px;
  width: 10px;
  height: 10px;
  background: inherit;
  border-left: 1px solid #e9eaec;
  border-bottom: 1px solid #e9eaec;
  content: '';
  transform: rotate(45deg);
}

.message-row.user .message-bubble {
  background: rgba(149, 236, 105, 0.24);
  border-color: rgba(94, 185, 62, 0.24);
}

.message-row.user .message-bubble::before {
  right: -6px;
  left: auto;
  border: 0;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #8b9099;
  font-size: 12px;
}

.message-meta strong {
  font-size: 12px;
  font-weight: 500;
}

.message-meta span {
  color: #a1a6b1;
  font-size: 12px;
}

.message-row.user .message-meta {
  flex-direction: row-reverse;
}

.message-content {
  word-break: break-word;
  white-space: normal;
  font-size: 15px;
  line-height: 1.85;
}

/* 受控 Markdown 的块级排版；用段距代替多重换行，长答案更容易扫描。 */
.message-content :deep(p) { margin: 0 0 12px; }
.message-content :deep(p:last-child) { margin-bottom: 0; }
.message-content :deep(ol), .message-content :deep(ul) { margin: 12px 0; padding-left: 24px; }
.message-content :deep(li) { padding-left: 4px; margin: 6px 0; }
.message-content :deep(li::marker) { color: #64748b; font-weight: 600; }
.message-content :deep(h3) { margin: 16px 0 8px; font-size: 16px; }
.message-content :deep(h3:first-child) { margin-top: 0; }
.message-content :deep(code) { border-radius: 5px; padding: 2px 5px; background: rgba(226, 232, 240, .6); font-size: .9em; }
.message-content :deep(pre) { max-width: 100%; overflow-x: auto; padding: 14px; border-radius: 10px; background: #f1f5f9; line-height: 1.6; }
.message-content :deep(pre code) { padding: 0; background: none; white-space: pre; }
.message-actions { display: flex; gap: 8px; padding-left: 2px; }
.message-actions :deep(.el-button) { font-size: 12px; color: #64748b; }

.message-content :deep(strong) {
  color: #111827;
  font-weight: 700;
}

.composer {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 0 0 auto;
  padding: 10px 24px 12px;
  border-top: 1px solid rgba(203, 213, 225, .45);
  background: rgba(255, 255, 255, 0.16);
}

.history-panel,
.chat-topbar {
  -webkit-backdrop-filter: blur(1px) saturate(1.15);
  backdrop-filter: blur(1px) saturate(1.15);
}

/* 消息只用底色保证对比度，避免每个气泡叠加背景模糊影响长会话滚动。 */
.composer, .message-bubble { -webkit-backdrop-filter: none; backdrop-filter: none; }
.composer-box {
  border: 1px solid rgba(148, 163, 184, .3);
  border-radius: 16px;
  background: rgba(255, 255, 255, .76);
  padding: 12px 14px 10px;
  box-shadow: 0 4px 18px rgba(30, 41, 59, .04), inset 0 1px 0 #fff;
  transition: border-color .2s, box-shadow .2s;
}
.composer-box:focus-within { border-color: rgba(59, 130, 246, .55); box-shadow: 0 0 0 3px rgba(59, 130, 246, .07); }
.composer-footer { padding-top: 8px; }

.history-search :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.24);
}

.composer-toolbar,
.composer-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.composer-toolbar span,
.composer-footer span {
  color: #9aa0aa;
  font-size: 12px;
}

.composer-tools {
  display: flex;
  align-items: center;
  gap: 6px;
}

.composer-tools button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  color: #4b5563;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 6px;
  font-size: 20px;
}

.composer-tools button:hover {
  background: #f1f3f5;
}

.composer-tools button:disabled {
  color: #c0c4cc;
  cursor: not-allowed;
}

.composer-tools .micro-tool.active {
  color: #f56c6c;
  background: #fff1f0;
}

.composer :deep(.el-textarea__inner) {
  min-height: 44px !important;
  padding: 6px 2px;
  color: #111827;
  background: transparent;
  border: 0;
  border-radius: 0;
  box-shadow: none;
  font-size: 15px;
  line-height: 1.7;
}

.composer :deep(.el-textarea__inner:focus) {
  box-shadow: none;
}

@media (max-width: 1100px) {
  .assistant-shell {
    grid-template-columns: 260px minmax(0, 1fr);
  }
}

@media (max-width: 900px) {
  .assistant-shell {
    grid-template-columns: 1fr;
    grid-template-rows: 128px minmax(0, 1fr);
    min-height: 0;
  }

  .history-panel {
    max-height: 128px;
    border-right: 0;
    border-bottom: 1px solid #d9d9dc;
  }

  .chat-topbar {
    align-items: flex-start;
    flex-direction: column;
    height: auto;
    padding: 14px 16px;
  }

  .header-actions { margin-left: 0; gap: 8px; }
  .history-searchbar { height: 54px; padding: 8px 12px; }
  .history-list { flex-direction: row; overflow-x: auto; overflow-y: hidden; }
  .history-item { min-width: 230px; min-height: 68px; padding: 8px 12px; }
  .history-avatar { width: 34px; height: 34px; }
  .history-main { gap: 4px; }

  .message-list {
    padding: 22px 18px;
  }

  .message-row {
    max-width: 92%;
  }

  .composer {
    padding: 10px 14px 12px;
  }
}

@media (max-width: 600px) {
  .message-row { max-width: 100%; gap: 8px; }
  .message-row.user { max-width: 94%; }
  .avatar { width: 32px; height: 32px; }
  .message-row :deep(.el-avatar) { width: 32px !important; height: 32px !important; }
  .message-bubble { padding: 12px 14px; }
  .composer-hint { display: none; }
  .chat-subtitle .data-context { display: none; }
}
</style>
