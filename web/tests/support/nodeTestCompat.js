import { test as vitestTest, vi } from 'vitest'

/** 将现有 node:test 用例接入同一套覆盖率运行，保留原有断言与计时器语义。 */
export function test(name, callback) {
  vitestTest(name, async context => {
    const cleanups = []
    const nodeContext = {
      after: callback => cleanups.push(callback),
      mock: {
        timers: {
          enable: ({ apis }) => vi.useFakeTimers({ toFake: [...apis, ...apis.filter(api => api.startsWith('set')).map(api => api.replace('set', 'clear'))] }),
          tick: milliseconds => vi.advanceTimersByTime(milliseconds),
        },
      },
    }
    try { await callback(nodeContext) }
    finally {
      for (const cleanup of cleanups.reverse()) await cleanup()
      vi.useRealTimers()
    }
  })
}

export default test
