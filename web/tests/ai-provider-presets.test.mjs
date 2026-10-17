/*
 * ai-provider-presets.test的自动化测试；夹具用于验证行为，不是运行时配置。
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { AI_PROVIDERS, aiConfigFingerprint, canReuseSavedAiKey, inferAiProvider, normalizedApiEndpoint, selectAiProvider } from '../src/modules/ai-assistant/providerPresets.js'

test('provider changes fill the matching endpoint and model and clear credentials', () => {
  const old = { enabled: true, baseUrl: AI_PROVIDERS[0].baseUrl, model: 'qwen-plus', apiKey: 'fixture-only-key', apiKeyConfigured: true, systemPrompt: '实验室助手' }
  const next = selectAiProvider(old, 'deepseek')
  assert.equal(next.baseUrl, 'https://api.deepseek.com')
  assert.equal(next.model, 'deepseek-flash')
  assert.equal(next.apiKey, '')
  assert.equal(next.apiKeyConfigured, false)
  assert.equal(next.systemPrompt, old.systemPrompt)
  assert.equal(old.apiKey, 'fixture-only-key')
  assert.deepEqual(selectAiProvider(old, 'custom'), old)
})

test('saved credentials are scoped to their exact endpoint and survive model-only changes', () => {
  const saved = { baseUrl: 'https://api.deepseek.com', apiKeyConfigured: true, model: 'deepseek-flash' }
  assert.equal(canReuseSavedAiKey({ baseUrl: 'https://api.deepseek.com/chat/completions/', model: 'another-model' }, saved), true)
  assert.equal(canReuseSavedAiKey({ baseUrl: 'https://api.siliconflow.cn/v1' }, saved), false)
  assert.equal(canReuseSavedAiKey({ baseUrl: 'https://api.deepseek.com.attacker.example' }, saved), false)
  assert.equal(canReuseSavedAiKey({ baseUrl: 'https://api.deepseek.com?key=x' }, saved), false)
  assert.equal(normalizedApiEndpoint('https://secret:password@example.com/v1'), '')
})

test('existing configs restore their provider without guessing from model names', () => {
  assert.equal(inferAiProvider('https://api.siliconflow.cn/v1/chat/completions'), 'siliconflow')
  assert.equal(inferAiProvider('https://workspace123.cn-beijing.maas.aliyuncs.com/compatible-mode/v1'), 'qwen')
  assert.equal(inferAiProvider('https://proxy.example/v1'), 'custom')
  assert.equal(inferAiProvider('https://api.deepseek.com.attacker.example'), 'custom')
})

test('testing a changed draft cannot be mistaken for testing the saved provider', () => {
  const saved = { enabled: true, baseUrl: 'https://api.deepseek.com', model: 'deepseek-flash', systemPrompt: '助手' }
  assert.equal(aiConfigFingerprint(saved), aiConfigFingerprint({ ...saved, baseUrl: 'https://api.deepseek.com/' }))
  assert.notEqual(aiConfigFingerprint(saved), aiConfigFingerprint({ ...saved, model: 'different' }))
  assert.notEqual(aiConfigFingerprint(saved), aiConfigFingerprint({ ...saved, baseUrl: 'https://api.siliconflow.cn/v1' }))
})
