/*
 * ui-issue-regressions.test的自动化测试；夹具用于验证行为，不是运行时配置。
 * 主要导出：testAiAssistantUserConfig。
 */
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import assert from 'node:assert/strict'

const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('room control background iot reads do not surface generic server error toasts', () => {
  const iotService = readSource('../src/api/services/iot.js')
  const roomControl = readSource('../src/composables/useRoomControl.js')

  assert.match(iotService, /requestOptions\s*=\s*\{\}/)
  assert.match(iotService, /skipErrorMessage/)
  assert.match(roomControl, /SILENT_IOT_REQUEST\s*=\s*\{\s*skipErrorMessage:\s*true\s*\}/)
  assert.doesNotMatch(roomControl, /ElMessage\.warning\(error\?\.message \|\| '实验室物联设备读取失败'\)/)
  assert.doesNotMatch(roomControl, /ElMessage\.warning\(error\?\.message \|\| '物联网设备数据加载失败/)
})

test('custom api assistant mode does not raise websocket connection error on page load', () => {
  const assistant = readSource('../src/views/ai-assistant/AiAssistant.vue')
  const aiService = readSource('../src/api/services/aiAssistantConfig.js')
  const endpoints = readSource('../src/api/endpoints.js')

  assert.doesNotMatch(assistant, /onError:\s*\(\)\s*=>\s*ElMessage\.error\('AI 服务连接异常，请稍后重试'\)/)
  assert.match(assistant, /const handleAssistantSocketError = \(\) =>/)
  assert.match(assistant, /if \(directAiConfig\.enabled\) return/)
  assert.match(assistant, /await loadBackendAiConfig\(\)/)
  assert.match(assistant, /if \(!directAiConfig\.enabled\) \{\s*await fetchConfigAndConnect\(\)/)
  assert.match(endpoints, /testUserConfig:\s*'\/ai-assistant\/user-config\/test'/)
  assert.match(aiService, /export const testAiAssistantUserConfig/)
  assert.match(assistant, /const testBackendAiConfig = async \(\) =>/)
  assert.match(assistant, />测试连接<\/el-button>/)
  assert.doesNotMatch(assistant, /onMounted[\s\S]*testBackendAiConfig\(\)/)
})

test('reservation conflict detection does not trigger contact validation or hide conflict column', () => {
  const reservationLogic = readSource('../src/composables/useReservationManagement.js')
  const table = readSource('../src/views/reservations/components/ReservationListTable.vue')
  const detectConflictBlock = reservationLogic.match(/const detectConflicts = \(showMessage = true\) => \{[\s\S]*?\n  \}/)?.[0] || ''

  assert.doesNotMatch(detectConflictBlock, /persistReservations\(\)/)
  assert.match(detectConflictBlock, /persistConflictMarks\(\)/)
  assert.doesNotMatch(table, /fixed="right" label="操作" width="388"/)
  assert.match(table, /label="操作" width="300"/)
  assert.match(table, /class="operation-column"/)
})
