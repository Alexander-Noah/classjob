import test from 'node:test'
import assert from 'node:assert/strict'
import { isGlassButtonTarget } from '../src/composables/glassButtonTarget.js'

/**
 * element的处理逻辑。
 * 入参：native（native）；role（角色）；children（children）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const element = (native, role, children = false) => ({
  /**
   * 判断是否匹配当前操作。
   * 入参：selector（selector）。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  matches: selector => selector === 'button, .el-pager > li' ? native : role,
  /**
   * 查询Selector。
   * @returns 由当前表达式或分支生成的处理结果。
   */
  querySelector: () => children ? {} : null,
})
test('upload wrappers with an inner button do not acquire a positioning backdrop', /** 验证当前测试用例的预期行为。 */ () => {
  assert.equal(isGlassButtonTarget(element(false, true, true)), false)
  assert.equal(isGlassButtonTarget(element(true, false)), true)
})
test('standalone accessible actions and pager items keep their glass surface', /** 验证当前测试用例的预期行为。 */ () => {
  assert.equal(isGlassButtonTarget(element(false, true)), true)
  assert.equal(isGlassButtonTarget(element(true, false)), true)
  assert.equal(isGlassButtonTarget(element(false, false)), false)
})
