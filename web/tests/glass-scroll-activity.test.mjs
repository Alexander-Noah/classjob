import test from 'node:test'
import assert from 'node:assert/strict'

test('glass scroll suspension is shared, debounced and released by the last owner', async t => {
  t.mock.timers.enable({ apis: ['setTimeout'] })
  const events = new Map()
  const classes = new Set()
  const previousDocument = globalThis.document
  globalThis.document = {
    documentElement: { classList: { add: value => classes.add(value), remove: value => classes.delete(value) } },
    addEventListener: (type, callback, options) => events.set(type, { callback, options }),
    removeEventListener: type => events.delete(type),
  }
  t.after(() => { globalThis.document = previousDocument })
  const { observeGlassScrolling } = await import('../src/composables/glassScrollActivity.js')
  const first = [], second = []
  const releaseFirst = observeGlassScrolling(value => first.push(value))
  const releaseSecond = observeGlassScrolling(value => second.push(value))
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
