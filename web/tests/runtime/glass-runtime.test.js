// @vitest-environment happy-dom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { h, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { attachLiquidGlass, useLiquidGlass } from '../../src/composables/liquidGlassRuntime'
import { useGlobalButtonGlass } from '../../src/composables/useGlobalButtonGlass'
import { useAdminGlass } from '../../src/composables/useAdminGlass'

let cleanups, media

/** 创建真实 DOM 控件，并固定尺寸以验证指针采样和命中区域稳定性。 */
function button() {
  const element = document.createElement('button')
  element.textContent = '操作'
  document.body.append(element)
  vi.spyOn(element, 'getBoundingClientRect').mockReturnValue({ x: 0, y: 0, left: 0, top: 0, right: 100, bottom: 40, width: 100, height: 40 })
  return element
}

/** 跟踪每个资源的释放，避免单个用例留下全局滚动订阅或 SVG。 */
function attach(element, options = { interactive: true, elasticity: 0 }) {
  const cleanup = attachLiquidGlass(element, options)
  cleanups.push(cleanup)
  return cleanup
}

/** 通过 DOM 事件进入实际指针处理逻辑，不直接调用实现内部方法。 */
function move(element, x = 25, y = 10, pointerType = 'mouse') {
  element.dispatchEvent(new PointerEvent('pointermove', { bubbles: true, pointerType, clientX: x, clientY: y }))
}

/** 等待 MutationObserver 与下一绘制帧，覆盖增量按钮管理器的异步同步。 */
async function settle() {
  await nextTick()
  await vi.advanceTimersByTimeAsync(64)
}

beforeEach(() => {
  cleanups = []
  media = new Map()
  document.body.innerHTML = ''
  vi.useFakeTimers({ toFake: ['requestAnimationFrame', 'cancelAnimationFrame', 'setTimeout', 'clearTimeout'] })
  vi.spyOn(navigator, 'userAgent', 'get').mockReturnValue('Chrome/155')
  vi.spyOn(window, 'matchMedia').mockImplementation(query => {
    if (!media.has(query)) {
      const listeners = new Set()
      media.set(query, { matches: false, listeners, addEventListener: (_, callback) => listeners.add(callback), removeEventListener: (_, callback) => listeners.delete(callback) })
    }
    return media.get(query)
  })
})

afterEach(() => {
  for (const cleanup of cleanups.reverse()) cleanup()
  expect(document.querySelectorAll('.liquid-filter-host')).toHaveLength(0)
  expect(document.documentElement.classList.contains('liquid-scrolling')).toBe(false)
  vi.useRealTimers()
  vi.restoreAllMocks()
  document.body.innerHTML = ''
})

describe('液态玻璃资源与交互', () => {
  it('空闲不分配 SVG，重复挂载共享实例，清理操作幂等', () => {
    const element = button()
    const cleanup = attach(element)
    expect(attach(element)).toBe(cleanup)
    expect(document.querySelectorAll('.liquid-filter-host')).toHaveLength(0)
    expect(element.style.getPropertyValue('--glass-backdrop')).toBe('none')
    element.focus()
    expect(document.querySelectorAll('.liquid-filter-host')).toHaveLength(1)
    expect(element.style.getPropertyValue('--glass-backdrop')).toContain('url(')
    cleanup(); cleanup()
    expect(element.hasAttribute('data-liquid-kind')).toBe(false)
    expect(element.style.getPropertyValue('--glass-backdrop')).toBe('')
    for (const value of media.values()) expect(value.listeners.size).toBe(0)
  })

  it('同一帧的多次移动只读取一次布局，折射增强但命中区域不位移', () => {
    const element = button()
    attach(element)
    for (let index = 0; index < 12; index++) move(element, 200, -20)
    expect(element.getBoundingClientRect).not.toHaveBeenCalled()
    vi.advanceTimersByTime(20)
    expect(element.getBoundingClientRect).toHaveBeenCalledTimes(1)
    expect(element.style.getPropertyValue('--liquid-light-x')).toBe('100%')
    expect(element.style.getPropertyValue('--liquid-light-y')).toBe('0%')
    expect(element.style.getPropertyValue('--liquid-transform')).toBe('')
    const initial = Number(document.querySelector('feDisplacementMap').getAttribute('scale'))
    element.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true }))
    vi.advanceTimersByTime(1000)
    expect(Math.abs(Number(document.querySelector('feDisplacementMap').getAttribute('scale')))).toBeGreaterThan(Math.abs(initial))
    element.dispatchEvent(new PointerEvent('pointerup', { bubbles: true }))
    vi.advanceTimersByTime(1000)
    element.dispatchEvent(new PointerEvent('pointerleave'))
    expect(document.querySelectorAll('.liquid-filter-host')).toHaveLength(0)
  })

  it('聚焦后的移出保留同一个滤镜，失焦后完整释放', () => {
    const element = button()
    attach(element)
    element.focus()
    const host = document.querySelector('.liquid-filter-host')
    element.dispatchEvent(new PointerEvent('pointerleave'))
    vi.advanceTimersByTime(1000)
    expect(document.querySelector('.liquid-filter-host')).toBe(host)
    element.blur()
    expect(document.querySelectorAll('.liquid-filter-host')).toHaveLength(0)
  })

  it.each(['disabled', 'is-disabled', 'is-loading', 'aria-disabled', 'aria-busy'])('禁用或加载态 %s 不创建折射', state => {
    const element = button()
    if (state === 'disabled') element.disabled = true
    else if (state.startsWith('aria-')) element.setAttribute(state, 'true')
    else element.classList.add(state)
    attach(element)
    move(element)
    element.dispatchEvent(new PointerEvent('pointerdown'))
    element.dispatchEvent(new FocusEvent('focusin'))
    vi.advanceTimersByTime(100)
    expect(document.querySelectorAll('.liquid-filter-host')).toHaveLength(0)
  })

  it('已激活按钮变为禁用时，在绘制前停止动画', () => {
    const element = button()
    attach(element)
    move(element)
    element.disabled = true
    vi.advanceTimersByTime(32)
    expect(document.querySelectorAll('.liquid-filter-host')).toHaveLength(0)
  })

  it('滚动立即释放重滤镜，停止后恢复聚焦按钮', () => {
    const element = button()
    attach(element)
    element.focus()
    document.dispatchEvent(new Event('scroll'))
    expect(document.documentElement.classList.contains('liquid-scrolling')).toBe(true)
    expect(document.querySelectorAll('.liquid-filter-host')).toHaveLength(0)
    move(element)
    element.dispatchEvent(new PointerEvent('pointerdown'))
    expect(document.querySelectorAll('.liquid-filter-host')).toHaveLength(0)
    vi.advanceTimersByTime(160)
    expect(document.querySelectorAll('.liquid-filter-host')).toHaveLength(1)
  })

  it('滚动中新增控件也保持轻量，取消指针后释放资源', () => {
    const first = button()
    attach(first)
    document.dispatchEvent(new WheelEvent('wheel'))
    const second = button()
    attach(second)
    expect(second.style.getPropertyValue('--glass-backdrop')).toBe('none')
    vi.advanceTimersByTime(160)
    move(second)
    second.dispatchEvent(new PointerEvent('pointercancel'))
    expect(document.querySelectorAll('.liquid-filter-host')).toHaveLength(0)
  })

  it('减少动画、粗指针与触摸事件不启动指针动画', () => {
    const element = button()
    attach(element)
    move(element, 10, 10, 'touch')
    const reduced = media.get('(prefers-reduced-motion: reduce)')
    reduced.matches = true
    move(element)
    element.dispatchEvent(new PointerEvent('pointerdown'))
    reduced.matches = false
    const coarse = media.get('(pointer: coarse)')
    coarse.matches = true
    move(element)
    element.dispatchEvent(new PointerEvent('pointerdown'))
    expect(document.querySelectorAll('.liquid-filter-host')).toHaveLength(0)
    coarse.matches = false
    move(element)
    expect(document.querySelectorAll('.liquid-filter-host')).toHaveLength(1)
    for (const listener of reduced.listeners) listener(new Event('change'))
    expect(document.querySelectorAll('.liquid-filter-host')).toHaveLength(0)
  })

  it('Firefox 使用静态模糊回退，不分配 SVG', () => {
    vi.spyOn(navigator, 'userAgent', 'get').mockReturnValue('Firefox/140')
    const element = button()
    attach(element)
    element.focus()
    expect(element.style.getPropertyValue('--glass-backdrop')).toContain('blur(')
    expect(element.style.getPropertyValue('--glass-backdrop')).not.toContain('url(')
    move(element)
    vi.advanceTimersByTime(1000)
    expect(document.querySelectorAll('.liquid-filter-host')).toHaveLength(0)
  })

  it('父卡片不叠加子按钮折射，卡片在滚动后恢复静态背景', () => {
    const panel = document.createElement('section')
    document.body.append(panel)
    vi.spyOn(panel, 'getBoundingClientRect').mockReturnValue({ width: 0, height: 0, left: 0, top: 0 })
    attach(panel, { interactive: false })
    move(panel)
    vi.advanceTimersByTime(1000)
    expect(panel.style.getPropertyValue('--liquid-light-x')).toBe('50%')
    const element = button()
    panel.append(element)
    attach(element)
    move(element)
    element.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true }))
    element.dispatchEvent(new FocusEvent('focusin', { bubbles: true }))
    expect(document.querySelectorAll('.liquid-filter-host')).toHaveLength(1)
    document.dispatchEvent(new Event('scroll'))
    vi.advanceTimersByTime(160)
    expect(panel.style.getPropertyValue('--glass-backdrop')).toContain('blur(')
  })

  it('可选弹性仅在启用时更新变换，移出后还原', () => {
    const element = button()
    attach(element, { interactive: true, elasticity: .1 })
    move(element)
    vi.advanceTimersByTime(1000)
    expect(element.style.getPropertyValue('--liquid-transform')).toContain('translate3d(')
    element.dispatchEvent(new PointerEvent('pointerleave'))
    expect(element.style.getPropertyValue('--liquid-transform')).toBe('')
  })
})

describe('Vue 组合函数的动态资源管理', () => {
  it('登录卡片随组件挂载和卸载管理资源，空引用不抛错', () => {
    const wrapper = mount({ setup() { const { glassElement } = useLiquidGlass(); return () => h('section', { ref: glassElement }) } }, { attachTo: document.body })
    cleanups.push(() => wrapper.unmount())
    expect(wrapper.element.dataset.liquidKind).toBe('panel')
    const empty = mount({ setup() { useLiquidGlass(); return () => null } })
    empty.unmount()
  })

  it('上传外层不套滤镜，角色变化和按钮移除释放对应资源', async () => {
    const wrapper = mount({ setup() { useGlobalButtonGlass(); return () => h('main', [h('button', { class: 'login-button' }, '登录'), h('div', { role: 'button' }, [h('button', { class: 'avatar-upload-button' }, '上传')])]) } }, { attachTo: document.body })
    cleanups.push(() => wrapper.unmount())
    const outer = wrapper.element.querySelector('[role="button"]')
    expect(outer.dataset.liquidKind).toBeUndefined()
    expect(outer.querySelector('button').style.getPropertyValue('--liquid-radius')).toBe('999px')
    const role = document.createElement('div')
    role.setAttribute('role', 'button')
    wrapper.element.append(role)
    await settle()
    expect(role.dataset.liquidKind).toBe('button')
    role.textContent = '纯文本更新'
    await settle()
    const nested = document.createElement('button')
    role.append(nested)
    await settle()
    expect(role.dataset.liquidKind).toBeUndefined()
    expect(nested.dataset.liquidKind).toBe('button')
    nested.remove()
    await settle()
    expect(nested.dataset.liquidKind).toBeUndefined()
    role.removeAttribute('role')
    await settle()
    expect(role.dataset.liquidKind).toBeUndefined()
    const transient = document.createElement('button')
    wrapper.element.append(transient); transient.remove()
    await settle()
    expect(transient.dataset.liquidKind).toBeUndefined()
  })

  it('管理布局追踪动态卡片与 Teleport 弹窗并在卸载时释放', async () => {
    const wrapper = mount({ setup() { const { glassRoot } = useAdminGlass(); return () => h('main', { ref: glassRoot }, [h('section', { class: 'el-card' })]) } }, { attachTo: document.body })
    cleanups.push(() => wrapper.unmount())
    expect(wrapper.find('.el-card').element.dataset.liquidKind).toBe('panel')
    const dialog = document.createElement('section')
    dialog.className = 'el-dialog'
    document.body.append(dialog)
    await settle()
    expect(dialog.style.getPropertyValue('--glass-backdrop')).toContain('blur(4px)')
    dialog.remove()
    await settle()
    expect(dialog.dataset.liquidKind).toBeUndefined()
    const empty = mount({ setup() { useAdminGlass(); return () => null } })
    empty.unmount()
  })
})
