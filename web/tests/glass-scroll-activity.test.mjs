import test from 'node:test'
import assert from 'node:assert/strict'

test('glass scroll suspension is shared, debounced and released by the last owner', /** 验证当前测试用例的预期行为。 入参：t。 */ async t => {
  t.mock.timers.enable({ apis: ['setTimeout'] })
  const events = new Map()
  const classes = new Set()
  const previousDocument = globalThis.document
  globalThis.document = {
    documentElement: { classList: { /** 添加当前操作。 入参：value（值）。 @returns 由当前表达式或分支生成的处理结果。 */ add: value => classes.add(value), /** 移除当前操作。 入参：value（值）。 @returns 由当前表达式或分支生成的处理结果。 */ remove: value => classes.delete(value) } },
    /**
     * 添加事件Listener。
     * 入参：type（类型）；callback（回调）；options（选项）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    addEventListener: (type, callback, options) => events.set(type, { callback, options }),
    /**
     * 移除事件Listener。
     * 入参：type（类型）。
     * @returns 由当前表达式或分支生成的处理结果。
     */
    removeEventListener: type => events.delete(type),
  }
  t.after(/** 处理 after 调用中的回调逻辑。 */ () => { globalThis.document = previousDocument })
  const { observeGlassScrolling } = await import('../src/composables/glassScrollActivity.js')
  const first = [], second = []
  const releaseFirst = observeGlassScrolling(/** 处理 observeGlassScrolling 调用中的回调逻辑。 入参：value。 */ value => first.push(value))
  const releaseSecond = observeGlassScrolling(/** 处理 observeGlassScrolling 调用中的回调逻辑。 入参：value。 */ value => second.push(value))
  assert.equal(events.size, 2, 'hundreds of surfaces share the same wheel/scroll listeners')
  assert.deepEqual(events.get('scroll').options, { capture: true, passive: true })
  events.get('wheel').callback()
  assert.equal(classes.has('liquid-scrolling'), true)
  t.mock.timers.tick(100)
  events.get('scroll').callback()
  t.mock.timers.tick(159)
  assert.deepEqual(first, [true], 'continuous scroll must not restore filters between events')
  t.mock.timers.tick(1)
  assert.deepEqual(first, [true, false])
  assert.deepEqual(second, [true, false])
  assert.equal(classes.has('liquid-scrolling'), false)
  releaseFirst()
  assert.equal(events.size, 2, 'other mounted surfaces still own the listeners')
  events.get('scroll').callback()
  releaseSecond()
  assert.equal(events.size, 0)
  assert.equal(classes.has('liquid-scrolling'), false)
  t.mock.timers.tick(200)
  assert.deepEqual(second, [true, false, true], 'unmount cancels the pending restore callback')
})
