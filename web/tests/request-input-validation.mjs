import test from 'node:test'
import assert from 'node:assert/strict'
import { validateRequestData } from '../../shared/requestValidation.js'

test('accepts ordinary punctuation, multilingual text and multiline descriptions without mutation', () => {
  const data = { name: "O'Brien 实验室", description: '第一行\n第二行 <测试>', password: ' pass1234 ' }
  const before = JSON.stringify(data)
  assert.doesNotThrow(() => validateRequestData(data))
  assert.equal(JSON.stringify(data), before)
})
test('rejects overlong input, control characters, nonfinite numbers and excessive batches', () => {
  for (const data of [{ username: 'x'.repeat(101) }, { email: 'x'.repeat(256) }, { name: 'a\0b' }, { stock: Infinity }, { records: Array(1001).fill({}) }]) {
    assert.throws(() => validateRequestData(data))
  }
})
test('bounds nesting and rejects prototype-related keys and circular structures', () => {
  let nested = {}; for (let i = 0; i < 22; i++) nested = { nested }
  assert.throws(() => validateRequestData(nested))
  assert.throws(() => validateRequestData(JSON.parse('{"__proto__":{"polluted":true}}')))
  const cycle = {}; cycle.self = cycle
  assert.throws(() => validateRequestData(cycle))
})
test('preserves larger explicitly allowed academic HTML and AI image formats', () => {
  assert.doesNotThrow(() => validateRequestData({ html: 'x'.repeat(20000), images: ['x'.repeat(20000), { dataUrl: 'x'.repeat(20000) }, { url: 'x'.repeat(20000) }], image_url: { url: 'x'.repeat(20000) } }))
  assert.throws(() => validateRequestData({ html: 'x'.repeat(2 * 1024 * 1024 + 1) }))
})
