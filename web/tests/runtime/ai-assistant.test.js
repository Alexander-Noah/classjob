// @vitest-environment happy-dom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { shallowMount, flushPromises } from '@vue/test-utils'
import Assistant from '../../src/views/ai-assistant/AiAssistant.vue'
import * as configApi from '../../src/api/services/aiAssistantConfig'
import * as historyApi from '../../src/api/services/aiAssistantHistory'
import { getStoredUser, getAuthToken } from '../../src/api/services/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formStubs } from '../support/formStubs'

const instances = vi.hoisted(() => ({ socket: null, audio: null, speech: null }))
vi.mock('../../src/api/services/auth', () => ({ getStoredUser: vi.fn(), getAuthToken: vi.fn() }))
vi.mock('../../src/api/services/aiAssistantConfig', () => Object.fromEntries(['getAiAssistantUserConfig','requestAiAssistantAnswer','saveAiAssistantUserConfig','testAiAssistantUserConfig','decideAiAssistantAction'].map(key => [key, vi.fn()])))
vi.mock('../../src/api/services/aiAssistantHistory', () => Object.fromEntries(['deleteAiConversationHistory','listAiConversationHistories','saveAiConversationHistories'].map(key => [key, vi.fn()])))
vi.mock('../../src/services/assistantWebSocket', () => ({ AssistantWebSocket: class {
  constructor(options) { this.options = options; this.isConnected = true; instances.socket = this }
  connect = vi.fn(); disconnect = vi.fn(); sendBinary = vi.fn(); sendAbort = vi.fn(); sendListenStart = vi.fn(); sendListenStop = vi.fn()
} }))
vi.mock('../../src/services/assistantAudio', () => ({ AssistantAudio: class {
  constructor(options) { this.options = options; instances.audio = this }
  startCapture = vi.fn(); stopCapture = vi.fn(); dispose = vi.fn(); destroy = vi.fn()
} }))
vi.mock('../../src/services/browserSpeechRecognition', () => ({ BrowserSpeechRecognition: class {
  constructor(options) { this.options = options; instances.speech = this }
  abort = vi.fn(); stop = vi.fn()
} }))
vi.mock('element-plus', () => ({ ElMessage: { warning: vi.fn(), success: vi.fn(), error: vi.fn() }, ElMessageBox: { confirm: vi.fn() } }))
const wrappers = []
it('deleting an inactive conversation preserves active messages and reports default failure',async()=>{
  historyApi.listAiConversationHistories.mockResolvedValue([conversation('older',1),conversation('newer',2)])
  const vm=await setup(),active=vm.activeConversationId
  const inactive=vm.conversationHistories.find(item=>item.id!==active)
  await vm.deleteConversation(inactive)
  expect(vm.activeConversationId).toBe(active);expect(vm.conversationHistories.some(item=>item.id===inactive.id)).toBe(false)
  historyApi.deleteAiConversationHistory.mockRejectedValueOnce({})
  await vm.deleteConversation(vm.activeConversation)
  expect(ElMessage.error).toHaveBeenCalledWith('删除失败，请重试')
  expect(vm.deletingConversationId).toBe('')
})
it('preserves newly typed input while a send begins and ignores empty assistant content',async()=>{
  const vm=await setup(),original=vm.messages.length
  await vm.appendMessage('assistant',' ');await vm.appendMessage('assistant',undefined)
  expect(vm.messages).toHaveLength(original)
  vm.inputValue='第一条问题'
  const pending=vm.sendMessage();vm.inputValue='正在写下一条'
  await pending
  expect(vm.inputValue).toBe('正在写下一条')
  expect(configApi.requestAiAssistantAnswer).toHaveBeenCalledWith(expect.objectContaining({question:'第一条问题'}))
})
it('saved incomplete custom configuration blocks send before opening an API connection',async()=>{
  configApi.getAiAssistantUserConfig.mockResolvedValueOnce({enabled:true,baseUrl:'https://api.example.com/v1',model:'model',apiKeyConfigured:false})
  const vm=await setup()
  expect(await vm.submitQuestion('测试问题')).toBe(false)
  expect(ElMessage.warning).toHaveBeenCalledWith('请先完成 AI 接口配置')
  expect(configApi.requestAiAssistantAnswer).not.toHaveBeenCalled()
  expect(vm.aiConfigDialogVisible).toBe(true)
  vm.isDirectAiLoading=true
  expect(vm.currentServiceStatusText).toBe('正在查询平台数据')
})
it('rendered destructive proposals require the explicit delete control',async()=>{
  const vm=await setup(true),w=wrappers.at(-1)
  vm.messages.push({id:'delete',role:'assistant',content:'删除前核对',pendingAction:{...draftAction(),method:'DELETE',body:{id:7},before:{data:{id:6}}}})
  await flushPromises()
  configApi.decideAiAssistantAction.mockResolvedValueOnce({message:'记录已删除'})
  const button=w.findAll('button').find(b=>b.text()==='确认删除')
  expect(configApi.decideAiAssistantAction).not.toHaveBeenCalled()
  await button.trigger('click');await flushPromises()
  expect(configApi.decideAiAssistantAction).toHaveBeenCalledWith('single-use-confirmation',expect.any(String),true)
  expect(w.text()).toContain('记录已删除')
})
it('voice fallback and configuration read failures clear capture state with actionable feedback',async()=>{const vm=await setup();vm.voiceMode='browser';vm.isCapturingVoice=true;instances.audio.startCapture.mockRejectedValueOnce(Error('denied'));instances.speech.options.onError();await flushPromises();expect(vm.isCapturingVoice).toBe(false);expect(ElMessage.error).toHaveBeenCalledWith('语音识别启动失败，请检查浏览器麦克风权限');vm.voiceMode='';instances.speech.options.onError();expect(vm.liveTranscript).toBe('');fetch.mockRejectedValueOnce(Error('offline'));await vm.fetchConfigAndConnect();expect(ElMessage.error).toHaveBeenCalledWith('AI 服务配置读取失败');getAuthToken.mockReturnValue('');expect(vm.buildWsProtocols()).toEqual([]);vm.directAiConfig.enabled=true;await vm.fetchConfigAndConnect();expect(vm.isCapturingVoice).toBe(false)})
const conversation = (id = 'old', updatedAt = 1) => ({ id, title: '预约咨询', updatedAt, messages: [{ role: 'user', content: '如何预约' }, { role: 'assistant', content: '在预约页面申请' }] })
beforeEach(() => {
  vi.resetAllMocks(); localStorage.clear(); instances.socket = null; instances.audio = null; instances.speech = null
  vi.useFakeTimers({ toFake: ['setTimeout','clearTimeout','Date'] })
  vi.setSystemTime(new Date('2026-10-18T09:00:00'))
  getStoredUser.mockReturnValue({ id: 7, realName: '张同学' }); getAuthToken.mockReturnValue('test-token')
  configApi.getAiAssistantUserConfig.mockResolvedValue({ enabled: false })
  configApi.requestAiAssistantAnswer.mockResolvedValue({ answer: '服务器回复' })
  configApi.testAiAssistantUserConfig.mockResolvedValue({ model: 'qwen-plus' })
  configApi.saveAiAssistantUserConfig.mockImplementation(async data => ({ ...data, apiKey: '', apiKeyConfigured: true }))
  historyApi.listAiConversationHistories.mockResolvedValue([])
  historyApi.saveAiConversationHistories.mockResolvedValue([])
  ElMessageBox.confirm.mockResolvedValue(true)
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ json: async () => ({ data: { ws_url: 'ws://localhost/test' } }) }))
  vi.stubGlobal('SpeechSynthesisUtterance', class { constructor(text) { this.text = text } })
  vi.stubGlobal('speechSynthesis', { cancel: vi.fn(), speak: vi.fn() })
  vi.spyOn(navigator.clipboard, 'writeText').mockResolvedValue()
})
afterEach(async () => {
  for (const wrapper of wrappers.splice(0)) wrapper.unmount()
  await flushPromises(); vi.clearAllTimers(); vi.useRealTimers(); vi.unstubAllGlobals(); vi.restoreAllMocks()
})
async function setup(render = false) {
  const names = ['button','icon','input','switch','dialog','form','form-item','select','option','alert','tag','tooltip','empty','avatar','link']
  const wrapper = shallowMount(Assistant, { global: { stubs: { ...(render ? formStubs() : Object.fromEntries(names.map(name => [`el-${name}`, true]))), SafeMessageContent: false } } })
  wrappers.push(wrapper); await flushPromises(); return wrapper.vm
}

it('rendered chat composer sends typed text and explicit action buttons execute the selected proposal',async()=>{
  const vm=await setup(true),wrapper=wrappers.at(-1)
  await wrapper.find('textarea[placeholder="询问实验室、课表或预约安排…"]').setValue('查看实验室状态')
  const send=wrapper.findAll('button').find(button=>button.text()==='发送');expect(send.attributes('disabled')).toBeUndefined();await send.trigger('click');await flushPromises()
  expect(configApi.requestAiAssistantAnswer).toHaveBeenCalledWith(expect.objectContaining({question:'查看实验室状态'}))
  expect(wrapper.text()).toContain('服务器回复')
  await wrapper.findAll('button').find(button=>button.text()==='API 配置').trigger('click');expect(vm.aiConfigDialogVisible).toBe(true)
  await wrapper.find('input[placeholder="https://dashscope.aliyuncs.com/compatible-mode/v1"]').setValue('https://api.test/v1')
  await wrapper.find('input[placeholder="请输入当前账号要使用的 API Key"]').setValue('test-key')
  expect(vm.directAiConfig).toMatchObject({baseUrl:'https://api.test/v1',apiKey:'test-key'})
})

it('rendered history, proposal confirmation and API controls follow the selected conversation',async()=>{
  historyApi.listAiConversationHistories.mockResolvedValue([conversation()]);const vm=await setup(true),w=wrappers.at(-1)
  await w.get('input[placeholder="搜索聊天记录"]').setValue('预约');expect(vm.filteredConversations).toHaveLength(1)
  await w.get('.history-select').trigger('click');await flushPromises();expect(w.text()).toContain('如何预约')
  vm.messages.push({id:'proposal',role:'assistant',content:'待审批',pendingAction:{...draftAction(),details:[{label:'人数',value:'30',previous:'20'}],warning:'需要确认'}});await flushPromises();expect(w.text()).toContain('需要确认')
  configApi.decideAiAssistantAction.mockResolvedValue({answer:'操作成功',result:{id:1}})
  await w.findAll('button').find(b=>b.text()==='确认执行').trigger('click');await flushPromises();expect(configApi.decideAiAssistantAction).toHaveBeenCalledWith('single-use-confirmation',expect.any(String),true)
  await w.get('button[aria-label="新建聊天"]').trigger('click');await flushPromises();expect(vm.messages).toHaveLength(1)
  await w.get('select[aria-label="选择模型服务商"]').setValue('deepseek');expect(vm.providerId).toBe('deepseek')
  const model=w.get('select[aria-label="选择模型或输入完整模型 ID"]');await model.setValue(model.findAll('option')[0].element.value)
  await w.findAll('textarea').at(-1).setValue('回答简洁');expect(vm.directAiConfig.systemPrompt).toBe('回答简洁')
  const switches=w.findAll('input[type="checkbox"]');await switches[0].setValue(false);expect(vm.speechEnabled).toBe(false);await switches[1].setValue(true)
  await w.get('input[placeholder="请输入当前账号要使用的 API Key"]').setValue('key');await w.findAll('button').find(b=>b.text()==='测试连接').trigger('click');await flushPromises();expect(configApi.testAiAssistantUserConfig).toHaveBeenCalled()
  await w.findAll('button').find(b=>b.text()==='保存配置').trigger('click');await flushPromises();expect(configApi.saveAiAssistantUserConfig).toHaveBeenCalled()
  await w.findAll('button').find(b=>b.text()==='恢复默认').trigger('click');expect(vm.directAiConfig.baseUrl).toContain('dashscope')
})

const draftAction = () => ({ id: 'single-use-confirmation', module: '实验室预约', method: 'POST', target: '/reservations',
  body: { labId: 204, reservationDate: '2026-10-19', timeRange: '14:00-16:00', attendeeCount: 30, reason: '考试' },
  summary: '预约204明天下午14点到16点供30人考试', expiresAt: new Date(Date.now() + 600000).toISOString() })

it('keeps a proposed write inert until explicit confirmation and sends execution context on the next turn', async () => {
  const vm = await setup()
  configApi.requestAiAssistantAnswer.mockResolvedValueOnce({ answer: '请核对预约', pendingAction: draftAction() })
  await vm.submitQuestion('借用204，明天14点到16点，30人考试')
  const proposal = vm.messages.at(-1)
  expect(proposal.pendingAction.body.reason).toBe('考试')
  expect(configApi.decideAiAssistantAction).not.toHaveBeenCalled()
  configApi.decideAiAssistantAction.mockResolvedValueOnce({ message: '预约提交成功', data: { id: 99 } })
  await vm.decideAction(proposal, true)
  await vm.decideAction(proposal, true)
  expect(configApi.decideAiAssistantAction).toHaveBeenCalledTimes(1)
  expect(configApi.decideAiAssistantAction).toHaveBeenCalledWith('single-use-confirmation', vm.activeConversationId, true)
  expect(proposal.actionStatus).toBe('已执行')
  await vm.submitQuestion('把刚才那条人数改成20人')
  const sent = configApi.requestAiAssistantAnswer.mock.calls.at(-1)[0]
  expect(sent.conversationId).toBe(vm.activeConversationId)
  expect(sent.history.some(item => item.content.includes('"id":99') && item.content.includes('已执行'))).toBe(true)
})

it('cancels, expires and supersedes drafts without confirming writes', async () => {
  const vm = await setup()
  const cancelled = { id: 'cancelled', pendingAction: draftAction() }
  configApi.decideAiAssistantAction.mockResolvedValueOnce({ message: '已取消' })
  await vm.decideAction(cancelled, false)
  expect(configApi.decideAiAssistantAction).toHaveBeenCalledWith('single-use-confirmation', vm.activeConversationId, false)
  const expired = { id: 'expired', pendingAction: { ...draftAction(), expiresAt: new Date(Date.now() - 1).toISOString() } }
  await vm.decideAction(expired, true)
  expect(expired.actionStatus).toContain('已过期')
  expect(configApi.decideAiAssistantAction).toHaveBeenCalledTimes(1)
  configApi.requestAiAssistantAnswer.mockResolvedValueOnce({ answer: '待确认', pendingAction: draftAction() })
  await vm.submitQuestion('借用204')
  const old = vm.messages.at(-1)
  await vm.submitQuestion('改成206')
  expect(old.actionStatus).toBe('已被后续对话替代')
  await vm.decideAction(old, true)
  expect(configApi.decideAiAssistantAction).toHaveBeenCalledTimes(1)
})

it('shows actual before and after values without losing confirmation metadata on persistence', async () => {
  const vm = await setup()
  const action = { ...draftAction(), before: { data: { attendeeCount: 48 } } }
  expect(vm.actionDetails(action).find(row => row.label === '人数')).toMatchObject({ previous: '48', value: '30' })
  const normalized = vm.normalizeConversation({ messages: [{ role: 'assistant', content: '确认操作', pendingAction: action, actionStatus: '已执行', actionResult: { data: { id: 99 } } }] })
  expect(normalized.messages[0].pendingAction.id).toBe(action.id)
  expect(normalized.messages[0].actionResult.data.id).toBe(99)
})

it('initializes authenticated socket, user display, welcome message and user-scoped history', async () => {
  const vm = await setup()
  expect(vm.currentUserName).toBe('张同学')
  expect(vm.messages[0].role).toBe('assistant')
  expect(vm.conversationHistories).toHaveLength(1)
  expect(vm.activeConversationTitle).toBe('新的咨询')
  expect(fetch).toHaveBeenCalledWith('/api/ai-assistant/config', { headers: { Authorization: 'Bearer test-token' } })
  expect(instances.socket.connect).toHaveBeenCalledWith('ws://localhost/test', ['smart-lab.ai.v1', 'smart-lab.jwt.test-token'])
  expect(localStorage.getItem('smart-lab:ai-assistant:conversations:7')).toBeTruthy()
  expect(vm.canSend).toBe(false)
  instances.socket.options.onStatusChange('connecting'); expect(vm.connectionStatusText).toBe('正在连接')
  instances.socket.options.onStatusChange('connected'); expect(vm.currentServiceStatusText).toBe('服务在线')
})

it('merges local and remote histories by latest update, limits size, searches and switches conversations', async () => {
  localStorage.setItem('smart-lab:ai-assistant:conversations', JSON.stringify([conversation('old', 1)]))
  historyApi.listAiConversationHistories.mockResolvedValue([conversation('old', 2), { ...conversation('new', 3), title: '设备问题' }])
  const vm = await setup()
  expect(vm.conversationHistories).toHaveLength(2)
  expect(vm.conversationHistories.find(item => item.id === 'old').updatedAt).toBe(2)
  expect(localStorage.getItem('smart-lab:ai-assistant:conversations')).toBeNull()
  vm.historyKeyword = '设备'; expect(vm.filteredConversations.map(item => item.id)).toEqual(['new'])
  await vm.switchConversation('old'); expect(vm.activeConversationId).toBe('old')
  await vm.switchConversation('missing'); expect(vm.activeConversationId).toBe('old')
  await vm.startNewConversation(); expect(vm.messages).toHaveLength(1)
  expect(vm.inputValue).toBe('')
  const merged = vm.mergeConversationLists(Array.from({ length: 35 }, (_, i) => conversation(`id-${i}`, i + 1)))
  expect(merged).toHaveLength(30); expect(merged[0].id).toBe('id-34')
  expect(vm.getConversationTitle([{ role: 'user', content: 'a'.repeat(30) }])).toBe('a'.repeat(18) + '...')
  expect(vm.getMessagePreview([])).toBe('暂无对话内容')
  expect(vm.formatConversationTime(Date.now())).toMatch(/\d\d:\d\d/)
  expect(vm.formatConversationTime(new Date('2025-01-01').getTime())).toContain('01-01')
})

it('send posts bounded history, clears only submitted text and appends server answers', async () => {
  const vm = await setup()
  await vm.sendMessage(); expect(ElMessage.warning).toHaveBeenCalledWith('请输入咨询内容')
  vm.inputValue = '  如何预约实验室  '
  await vm.sendMessage()
  expect(configApi.requestAiAssistantAnswer).toHaveBeenCalledWith(expect.objectContaining({ question: '如何预约实验室', history: expect.any(Array) }))
  expect(vm.messages.at(-1).content).toBe('服务器回复')
  expect(vm.inputValue).toBe('')
  expect(vm.isDirectAiLoading).toBe(false)
  expect(speechSynthesis.speak).toHaveBeenCalled()
  configApi.requestAiAssistantAnswer.mockResolvedValueOnce({ answer: '' })
  await vm.submitQuestion('再次咨询')
  expect(vm.messages.at(-1).content).toContain('AI 接口未返回可展示内容')
  await vi.advanceTimersByTimeAsync(501)
  expect(historyApi.saveAiConversationHistories).toHaveBeenCalled()
})

it('an answer arriving after conversation switch is stored under its original conversation', async () => {
  const vm = await setup()
  let resolve
  configApi.requestAiAssistantAnswer.mockImplementationOnce(() => new Promise(done => { resolve = done }))
  const originalId = vm.activeConversationId
  const pending = vm.submitQuestion('原会话问题')
  await flushPromises()
  await vm.startNewConversation()
  const nextId = vm.activeConversationId
  resolve({ answer: '原会话答案' }); await pending
  expect(vm.activeConversationId).toBe(nextId)
  expect(vm.messages.some(message => message.content === '原会话答案')).toBe(false)
  expect(vm.conversationHistories.find(item => item.id === originalId).messages.at(-1).content).toBe('原会话答案')
})

it('deletion flushes pending saves first, supports cancel, and keeps a usable new conversation', async () => {
  const vm = await setup()
  const target = vm.activeConversation
  ElMessageBox.confirm.mockRejectedValueOnce('cancel')
  await vm.deleteConversation(target)
  expect(historyApi.deleteAiConversationHistory).not.toHaveBeenCalled()
  await vm.appendMessage('user', '要删除的问题')
  await vm.deleteConversation(target)
  expect(historyApi.deleteAiConversationHistory).toHaveBeenCalledWith(target.id)
  expect(historyApi.saveAiConversationHistories.mock.invocationCallOrder[0]).toBeLessThan(historyApi.deleteAiConversationHistory.mock.invocationCallOrder[0])
  expect(vm.activeConversationId).not.toBe(target.id)
  expect(vm.conversationHistories).toHaveLength(1)
  expect(vm.deletingConversationId).toBe('')
  historyApi.deleteAiConversationHistory.mockRejectedValueOnce(new Error('删除失败'))
  const previous = vm.activeConversationId
  await vm.deleteConversation(vm.activeConversation)
  expect(vm.activeConversationId).toBe(previous)
  expect(ElMessage.error).toHaveBeenCalledWith('删除失败')
})

it('custom provider requires testing before saving and invalidates test results after edits', async () => {
  const vm = await setup()
  await vm.testBackendAiConfig()
  expect(ElMessage.warning).toHaveBeenCalledWith('请先启用自定义 API')
  Object.assign(vm.directAiConfig, { enabled: true, apiKey: 'test-only-key', model: 'qwen-plus' })
  await vm.saveBackendAiConfig()
  expect(configApi.saveAiAssistantUserConfig).not.toHaveBeenCalled()
  await vm.testBackendAiConfig()
  expect(vm.hasPassedAiTest).toBe(true)
  vm.directAiConfig.model = 'qwen-turbo'
  expect(vm.hasPassedAiTest).toBe(false)
  await vm.testBackendAiConfig(); await vm.saveBackendAiConfig()
  expect(configApi.saveAiAssistantUserConfig).toHaveBeenCalledWith(expect.objectContaining({ apiKey: 'test-only-key', model: 'qwen-turbo', enabled: true }))
  expect(vm.directAiConfig.apiKey).toBe('')
  expect(vm.savedApiKeyAvailable).toBe(true)
  expect(vm.isDirectAiConfigured).toBe(true)
  expect(vm.currentServiceStatusText).toBe('自定义 API 已启用')
  expect(vm.modelOptions).toContain('qwen-turbo')
})

it('provider endpoint changes clear keys and unsaved drafts cannot send questions', async () => {
  const vm = await setup()
  Object.assign(vm.directAiConfig, { enabled: true, apiKey: 'test-only-key' })
  expect(await vm.submitQuestion('测试')).toBe(false)
  expect(vm.aiConfigDialogVisible).toBe(true)
  vm.changeProvider('deepseek')
  expect(vm.directAiConfig.apiKey).toBe('')
  vm.directAiConfig.apiKey = 'another-key'; vm.directAiConfig.baseUrl = 'https://example.com/v1'; vm.addressChanged()
  expect(vm.directAiConfig.apiKey).toBe('')
  expect(vm.directAiConfig.apiKeyConfigured).toBe(false)
  vm.resetDirectAiConfig(); expect(vm.providerId).toBe('qwen')
  vm.directAiConfig.enabled = true
  await vm.testBackendAiConfig()
  expect(configApi.testAiAssistantUserConfig).not.toHaveBeenCalled()
})

it('connection testing rejects stale results and exposes test/save failures without leaving busy flags', async () => {
  const vm = await setup()
  Object.assign(vm.directAiConfig, { enabled: true, apiKey: 'test-only-key' })
  let resolve
  configApi.testAiAssistantUserConfig.mockImplementationOnce(() => new Promise(done => { resolve = done }))
  const pending = vm.testBackendAiConfig(); vm.directAiConfig.model = 'changed'; resolve({ model: 'old' }); await pending
  expect(vm.hasPassedAiTest).toBe(false)
  expect(ElMessage.warning).toHaveBeenCalledWith('配置已修改，请重新测试连接')
  configApi.testAiAssistantUserConfig.mockRejectedValueOnce(new Error('测试失败'))
  await vm.testBackendAiConfig(); expect(vm.isTestingDirectAi).toBe(false)
  await vm.testBackendAiConfig()
  configApi.saveAiAssistantUserConfig.mockRejectedValueOnce(new Error('保存失败'))
  await vm.saveBackendAiConfig()
  expect(vm.isSavingDirectAi).toBe(false); expect(ElMessage.error).toHaveBeenCalledWith('保存失败')
})

it('socket text events distinguish speech recognition, model output and ignored greetings', async () => {
  const vm = await setup()
  const callback = instances.socket.options.onTextMessage
  const initialCount = vm.messages.length
  await callback({ type: 'hello', text: 'ignored' })
  expect(vm.messages).toHaveLength(initialCount)
  await callback({ type: 'stt', text: '  识别文本  ' })
  expect(vm.inputValue).toBe('识别文本'); expect(vm.liveTranscript).toBe('识别文本')
  for (const type of ['llm', 'text', 'tts']) {
    await callback({ type, state: 'sentence_start', text: `${type}回复` })
    expect(vm.messages.at(-1).content).toBe(`${type}回复`)
  }
  await callback({ type: 'stt', state: 'error', text: '识别服务失败' })
  expect(vm.isCapturingVoice).toBe(false)
  expect(vm.messages.at(-1).content).toBe('识别服务失败')
  instances.socket.options.onError(); instances.socket.options.onError()
  expect(ElMessage.error).toHaveBeenCalledTimes(1)
})

it('voice capture sends listen controls, forwards frames and clears capture on stop or failure', async () => {
  const vm = await setup()
  await vm.startVoiceCapture()
  expect(vm.isCapturingVoice).toBe(true)
  expect(instances.socket.sendListenStart).toHaveBeenCalled()
  instances.audio.options.onLevelChange(0.7); expect(vm.audioLevel).toBe(0.7)
  const frame = new ArrayBuffer(4); instances.audio.options.onAudioFrame(frame)
  expect(instances.socket.sendBinary).toHaveBeenCalledWith(frame)
  await vm.stopVoiceCapture()
  expect(vm.isCapturingVoice).toBe(false); expect(instances.socket.sendListenStop).toHaveBeenCalled()
  instances.audio.startCapture.mockRejectedValueOnce(new Error('麦克风拒绝'))
  await vm.startVoiceCapture()
  expect(vm.isCapturingVoice).toBe(false); expect(ElMessage.error).toHaveBeenCalledWith('麦克风拒绝')
  instances.socket.isConnected = false; await vm.startVoiceCapture()
  expect(ElMessage.warning).toHaveBeenCalledWith('AI 服务正在连接，请稍后再试')
})

it('browser voice stop and fallback callbacks preserve transcript and handle empty recognition', async () => {
  const vm = await setup()
  vm.voiceMode = 'browser'; vm.isCapturingVoice = true
  instances.speech.stop.mockResolvedValueOnce('  浏览器识别  ')
  await vm.stopVoiceCapture()
  expect(vm.inputValue).toBe('浏览器识别')
  vm.voiceMode = 'browser'; vm.isCapturingVoice = true
  instances.speech.stop.mockResolvedValueOnce('')
  await vm.stopVoiceCapture()
  expect(vm.messages.at(-1).content).toContain('未识别到有效语音')
  instances.speech.options.onTranscript('实时文本'); expect(vm.liveTranscript).toBe('实时文本')
  vm.voiceMode = 'browser'; vm.isCapturingVoice = true
  instances.speech.options.onError(); await flushPromises()
  expect(instances.speech.abort).toHaveBeenCalled(); expect(vm.voiceMode).toBe('server')
})

it('speech toggle persists preferences, composition enter does not send, and clear resets messages', async () => {
  const vm = await setup()
  vm.toggleSpeech(false); expect(localStorage.getItem('smart-lab:ai-tts-enabled')).toBe('false')
  vm.speakText('ignored'); expect(speechSynthesis.speak).not.toHaveBeenCalled()
  const event = { isComposing: true, preventDefault: vi.fn() }
  vm.handleComposerEnter(event); expect(event.preventDefault).not.toHaveBeenCalled()
  vm.inputValue = '新问题'; vm.handleComposerEnter({ isComposing: false, preventDefault: event.preventDefault }); await flushPromises()
  expect(configApi.requestAiAssistantAnswer).toHaveBeenCalled()
  await vm.copyMessage('回答'); expect(navigator.clipboard.writeText).toHaveBeenCalledWith('回答')
  navigator.clipboard.writeText.mockRejectedValueOnce(new Error('denied')); await vm.copyMessage('回答')
  expect(ElMessage.warning).toHaveBeenCalledWith('复制失败，请选中文字后复制')
  vm.clearMessages(); expect(vm.messages).toHaveLength(1)
})


it.each([[null,'我','anonymous'],[{name:'姓名'},'姓名','anonymous'],[{username:'reader'},'reader','reader']])('handles accounts without numeric identity',async(user,label,key)=>{getStoredUser.mockReturnValue(user);const vm=await setup();expect(vm.currentUserName).toBe(label);expect(vm.getHistoryStorageKey()).toBe(`smart-lab:ai-assistant:conversations:${key}`)})
it('normalizes incomplete histories and retains the newest duplicate conversation',async()=>{
 const warn=vi.spyOn(console,'warn').mockImplementation(()=>{});historyApi.listAiConversationHistories.mockRejectedValueOnce(new Error('offline'));configApi.getAiAssistantUserConfig.mockRejectedValueOnce(new Error('offline'));localStorage.setItem('smart-lab:ai-assistant:conversations:7','{');const vm=await setup();expect(vm.conversationHistories).toHaveLength(1);expect(warn).toHaveBeenCalled()
 localStorage.setItem(vm.getHistoryStorageKey(),'{}');expect(vm.readLocalConversations()).toEqual([]);const normalized=vm.normalizeConversation({messages:[{}]});expect(normalized.messages[0]).toMatchObject({role:'assistant',content:'',time:'',pendingAction:null,actionResult:null});expect(vm.normalizeConversation({messages:[]} ).messages).toHaveLength(1)
 expect(vm.getMessagePreview([{content:' '}])).toBe('暂无对话内容');expect(vm.mergeConversationLists([conversation('same',5)],[conversation('same',1)])[0].updatedAt).toBe(5)
 const storage=vi.spyOn(localStorage,'setItem').mockImplementation(()=>{throw new Error('quota')});vm.writeLocalConversations([]);expect(warn).toHaveBeenCalledWith('[AiAssistant] conversation storage failed',expect.any(Error));storage.mockRestore()
 historyApi.saveAiConversationHistories.mockRejectedValueOnce(new Error('offline'));await vm.flushRemoteSave();expect(warn).toHaveBeenCalledWith('[AiAssistant] failed to save conversation history',expect.any(Error));vm.conversationHistories=[];await vm.flushRemoteSave();vm.activeConversationId='';vm.persistCurrentConversation();expect(vm.conversationHistories).toEqual([]);vm.activeConversationId='new-id';vm.messages=[{role:'user',content:'新问题'}];vm.persistCurrentConversation();expect(vm.conversationHistories[0].id).toBe('new-id')
})
it('busy history state blocks duplicate user actions without losing existing conversation',async()=>{
 const vm=await setup();const original=vm.activeConversationId;vm.deletingConversationId='busy';await vm.switchConversation('missing');await vm.startNewConversation();await vm.deleteConversation(vm.activeConversation);vm.clearMessages();await vm.appendMessage('user','ignored');vm.persistCurrentConversation();expect(await vm.submitQuestion('ignored')).toBe(false);expect(vm.activeConversationId).toBe(original);vm.deletingConversationId='';expect(await vm.submitQuestion(' ')).toBe(false);vm.isDirectAiLoading=true;await vm.sendMessage();await vm.deleteConversation(vm.activeConversation);expect(historyApi.deleteAiConversationHistory).not.toHaveBeenCalled();vm.isHistoryRestoring=true;vm.queueRemoteSave([])
})
it('reports action execution failure and stores completed decisions in the originating conversation',async()=>{
 const vm=await setup();expect(vm.actionDetails({body:{records:[],nested:{},reason:null},query:{id:2},before:{data:{reason:'原值'}}})).toEqual(expect.arrayContaining([{label:'记录',value:'空列表'},{label:'nested',value:'空对象'},{label:'用途／原因',value:'清空',previous:'原值'},{label:'操作参数 · 记录编号',value:'2',previous:null}]))
 const failed={id:'failed',pendingAction:draftAction()};configApi.decideAiAssistantAction.mockRejectedValueOnce({});await vm.decideAction(failed,true);expect(failed.actionStatus).toContain('未确认成功');expect(ElMessage.error).toHaveBeenCalledWith('操作未确认成功，请到业务列表核实')
 let resolve;const origin=vm.activeConversationId;const proposal={id:'later',role:'assistant',pendingAction:draftAction(),content:'确认'};vm.messages.push(proposal);vm.persistCurrentConversation();configApi.decideAiAssistantAction.mockImplementationOnce(()=>new Promise(done=>{resolve=done}));const pending=vm.decideAction(proposal,true);await vm.startNewConversation();resolve({});await pending;const saved=vm.conversationHistories.find(c=>c.id===origin);expect(saved.messages.find(m=>m.id==='later').actionStatus).toBe('已执行');expect(saved.messages.at(-1).content).toContain('操作成功')
})
it('tests default-error feedback, disabled configuration saves, and socket fallback payloads',async()=>{
 const vm=await setup();vm.isSavingDirectAi=true;await vm.testBackendAiConfig();await vm.saveBackendAiConfig();expect(configApi.saveAiAssistantUserConfig).not.toHaveBeenCalled();vm.isSavingDirectAi=false;vm.directAiConfig.baseUrl='javascript:bad';await vm.saveBackendAiConfig();expect(ElMessage.warning).toHaveBeenCalledWith('请填写有效的 HTTP 或 HTTPS 接口基础地址');vm.resetDirectAiConfig();vm.directAiConfig.systemPrompt='';await vm.saveBackendAiConfig();expect(configApi.saveAiAssistantUserConfig).toHaveBeenCalledWith(expect.objectContaining({enabled:false,apiKey:''}))
 Object.assign(vm.directAiConfig,{enabled:true,apiKey:'key'});configApi.testAiAssistantUserConfig.mockResolvedValueOnce({});await vm.testBackendAiConfig();expect(ElMessage.success).toHaveBeenCalledWith(expect.stringContaining('qwen-plus'));configApi.testAiAssistantUserConfig.mockRejectedValueOnce({});await vm.testBackendAiConfig();expect(ElMessage.error).toHaveBeenCalledWith('AI 接口连接测试失败，请检查 API 地址、Key、模型名称或服务器网络');await vm.testBackendAiConfig();configApi.saveAiAssistantUserConfig.mockRejectedValueOnce({});await vm.saveBackendAiConfig();expect(ElMessage.error).toHaveBeenCalledWith('AI 接口配置保存失败')
 await instances.socket.options.onTextMessage({type:'stt',state:'error'});expect(vm.messages.at(-1).content).toContain('语音识别服务暂不可用');await instances.socket.options.onTextMessage({type:'text',text:' '});instances.socket.options.onAudioMessage(new ArrayBuffer(1));vm.directAiConfig.enabled=false;getAuthToken.mockReturnValue('');fetch.mockResolvedValueOnce({json:async()=>({})});await vm.fetchConfigAndConnect();expect(vm.wsUrl).toBe('')
})
it('query errors after a chat switch are reported without appending into the new chat',async()=>{
 const vm=await setup();let reject;configApi.requestAiAssistantAnswer.mockImplementationOnce(()=>new Promise((_,fail)=>{reject=fail}));const pending=vm.submitQuestion('延迟失败');await flushPromises();await vm.startNewConversation();reject({});await pending;expect(ElMessage.error).toHaveBeenCalledWith('AI 查询失败，请重试');expect(vm.messages).toHaveLength(1)
 configApi.requestAiAssistantAnswer.mockRejectedValueOnce({});await vm.submitQuestion('当前失败');expect(vm.messages.at(-1).content).toContain('请检查 AI 配置或稍后重试。');vm.toggleSpeech(true);expect(localStorage.getItem('smart-lab:ai-tts-enabled')).toBe('true')
})

it('custom API mode suppresses platform socket failures and keeps status accurate while loading',async()=>{
 const vm=await setup();vm.applyBackendAiConfig({enabled:true,apiKeyConfigured:true,baseUrl:'https://api.example.com/v1',model:'chat-model'})
 vm.isDirectAiLoading=false;expect(vm.currentServiceStatusText).toBe('自定义 API 已启用')
 vm.isDirectAiLoading=true;expect(vm.currentServiceStatusText).toBe('正在查询平台数据')
 ElMessage.error.mockClear();instances.socket.options.onError()
 expect(ElMessage.error).not.toHaveBeenCalled()
 fetch.mockRejectedValueOnce({});await vm.fetchConfigAndConnect()
 expect(ElMessage.error).not.toHaveBeenCalled()
})
it('sparse history previews and query-only action proposals retain usable content',async()=>{
 const vm=await setup();expect(vm.getMessagePreview([{}])).toBe('暂无对话内容')
 expect(vm.formatConversationTime(null)).toMatch(/\d\d:\d\d/)
 expect(vm.actionDetails({query:{id:7}})).toEqual([{label:'操作参数 · 记录编号',value:'7',previous:null}])
 const merged=vm.mergeConversationLists([{id:'a',messages:[],updatedAt:0}],[{id:'a',messages:[],updatedAt:0}],[{id:'b',messages:[]}])
 expect(merged.map(item=>item.id).sort()).toEqual(['a','b'])
})
it('unchanged endpoint edits preserve a stored credential and unstructured microphone failures clear capture',async()=>{
 const vm=await setup();vm.directAiConfig.apiKey='existing-key';vm.directAiConfig.apiKeyConfigured=true
 vm.addressChanged();expect(vm.directAiConfig.apiKey).toBe('existing-key');expect(vm.directAiConfig.apiKeyConfigured).toBe(true)
 instances.audio.startCapture.mockRejectedValueOnce({});await vm.startVoiceCapture()
 expect(ElMessage.error).toHaveBeenLastCalledWith('无法启动麦克风');expect(vm.isCapturingVoice).toBe(false)
 vm.isCapturingVoice=true;vm.voiceMode='';await vm.stopVoiceCapture()
 expect(vm.isCapturingVoice).toBe(false);expect(instances.audio.stopCapture).toHaveBeenCalled()
})