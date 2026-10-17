import test from 'node:test'
import assert from 'node:assert/strict'
import { isGlassButtonTarget } from '../src/composables/glassButtonTarget.js'

const element = (native, role, children = false) => ({
  matches: selector => selector === 'button, .el-pager > li' ? native : role,
  querySelector: () => children ? {} : null,
})
test('upload wrappers with an inner button do not acquire a positioning backdrop', () => {
  assert.equal(isGlassButtonTarget(element(false, true, true)), false)
  assert.equal(isGlassButtonTarget(element(true, false)), true)
})
test('standalone accessible actions and pager items keep their glass surface', () => {
  assert.equal(isGlassButtonTarget(element(false, true)), true)
  assert.equal(isGlassButtonTarget(element(true, false)), true)
  assert.equal(isGlassButtonTarget(element(false, false)), false)
})
