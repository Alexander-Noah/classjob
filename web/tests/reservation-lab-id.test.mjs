/*
 * 实验室预约的自动化测试；夹具用于验证行为，不是运行时配置。
 */
import { readFileSync } from 'node:fs'
import assert from 'node:assert/strict'
import { test } from 'node:test'
import { resolveReservationLabId } from '../src/utils/reservationLabId.js'

/**
 * 读取来源。
 * 入参：path（路径）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const readSource = (path) => readFileSync(new URL(path, import.meta.url), 'utf8')

test('resolves a positive integer identifier from id or labId', /** 验证当前测试用例的预期行为。 */ () => {
  assert.equal(resolveReservationLabId({ id: 42 }), 42)
  assert.equal(resolveReservationLabId({ labId: ' 007 ' }), 7)
  assert.equal(resolveReservationLabId({ id: 'not-an-id', labId: '8' }), 8)
})

test('rejects missing and non-positive-integer laboratory identifiers', /** 验证当前测试用例的预期行为。 */ () => {
  for (const lab of [
    undefined,
    {},
    { id: 0 },
    { id: -1 },
    { id: 1.5 },
    { id: '   ' },
    { id: '1.5' },
    { id: 'A101' },
    { labId: '0' },
    { labId: '-8' },
  ]) {
    assert.equal(resolveReservationLabId(lab), null)
  }
})

test('rejects unsafe numeric and decimal-string laboratory identifiers', /** 验证当前测试用例的预期行为。 */ () => {
  for (const lab of [
    { id: Number.MAX_SAFE_INTEGER + 1 },
    { id: String(Number.MAX_SAFE_INTEGER + 1) },
    { labId: Number.MAX_SAFE_INTEGER + 1 },
    { labId: String(Number.MAX_SAFE_INTEGER + 1) },
  ]) {
    assert.equal(resolveReservationLabId(lab), null)
  }
})

test('real reservation creation validates selected metadata id and submits canonical labId', /** 验证当前测试用例的预期行为。 */ () => {
  const reservationManagement = readSource('../src/composables/useReservationManagement.js')

  assert.match(reservationManagement, /resolveReservationLabId\(selectedLabMeta\.value\)/)
  assert.match(
    reservationManagement,
    /if \(!API_USE_MOCK && !selectedLabId\.value\) \{[\s\S]*?ElMessage\.warning\('[^']*刷新[^']*重试[^']*'\)/,
  )
  assert.match(reservationManagement, /\.\.\.\(API_USE_MOCK \? \{\} : \{ labId: selectedLabId\.value \}\)/)
})
