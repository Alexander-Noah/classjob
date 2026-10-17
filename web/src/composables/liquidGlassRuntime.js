import { h, onBeforeUnmount, onMounted, ref, render } from 'vue'
import { GlassFilter } from '@wxperia/liquid-glass-vue'
import { observeGlassScrolling } from './glassScrollActivity'

let nextId = 0
const attached = new WeakMap()
const properties = ['--glass-backdrop', '--liquid-radius', '--liquid-transform', '--liquid-light-x', '--liquid-light-y', '--liquid-light-alpha']
/**
 * 生成动画的静止状态。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const resting = () => ({ x: 0, y: 0, scaleX: 1, scaleY: 1, lens: 1 })
const unavailable = ':disabled, .is-disabled, .is-loading, [aria-disabled="true"], [aria-busy="true"]'

/**
 * 静止表面保留透明材质，只有指针/键盘交互时才创建分色折射滤镜。
 * 避免为整张表格的按钮常驻 SVG、合成层与 IntersectionObserver。
 * 指针采样每帧至多一次；滚动开始立即释放重滤镜和动画，结束后恢复静态材质。
 * @returns {() => void} 幂等释放函数，由页面或全局按钮管理器持有
 */
export function attachLiquidGlass(element, {
  strength = 44, blur = .6, radius = 18, aberration = .8,
  mode = 'standard', interactive = false, elasticity = .12,
} = {}) {
  if (attached.has(element)) return attached.get(element)
  const id = `zhiyun-liquid-${++nextId}`
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)')
  const coarse = window.matchMedia('(pointer: coarse)')
  const fallbackOnly = /firefox|safari/i.test(navigator.userAgent) && !/chrome|chromium|edg/i.test(navigator.userAgent)
  const idleBackdrop = interactive ? 'none' : `blur(${blur}px) saturate(1.12)`
  let host
  let channels = []
  let frame = 0
  let disposed = false
  let scrolling = document.documentElement.classList.contains('liquid-scrolling')
  let active = false
  let hovered = false
  let pressed = false
  let pointer
  let current = resting()
  let target = resting()
  element.classList.add('liquid-glass-surface')
  element.dataset.liquidKind = interactive ? 'button' : 'panel'
  element.style.setProperty('--liquid-radius', `${radius}px`)
  element.style.setProperty('--liquid-light-x', '30%')
  element.style.setProperty('--liquid-light-y', '0%')
  element.style.setProperty('--liquid-light-alpha', '.1')
  element.style.setProperty('--glass-backdrop', scrolling ? 'none' : idleBackdrop)

  /**
   * 激活当前操作。
   */
  function activate() {
    if (active || disposed || scrolling || element.matches(unavailable)) return
    active = true
    if (!fallbackOnly) {
      host = document.createElement('div')
      host.className = 'liquid-filter-host'
      host.setAttribute('aria-hidden', 'true')
      document.body.append(host)
      // standard 无 Worker 或外部图片请求；离开按钮时连同滤镜一起释放。
      render(h(GlassFilter, { id, mode, displacementScale: strength, aberrationIntensity: aberration, width: 512, height: 512 }), host)
      channels = [...host.querySelectorAll('feDisplacementMap')].map(/** 将当前条目转换为目标结果。 入参：node。 */ node => ({ node, scale: Number(node.getAttribute('scale')) }))
      host.querySelector('feImage')?.removeAttribute('id')
    }
    element.style.setProperty('--glass-backdrop', `${fallbackOnly ? '' : `url("#${id}") `}blur(${blur}px) saturate(1.12)`)
  }
  /**
   * 停用当前操作。
   */
  function deactivate() {
    cancelAnimationFrame(frame)
    frame = 0
    pointer = undefined
    pressed = hovered = active = false
    current = resting()
    target = resting()
    if (host) { render(null, host); host.remove(); host = undefined }
    channels = []
    element.style.setProperty('--glass-backdrop', scrolling ? 'none' : idleBackdrop)
    element.style.removeProperty('--liquid-transform')
    element.style.setProperty('--liquid-light-alpha', '.1')
  }
  /**
   * 更新动画当前操作。
   */
  function animate() {
    frame = 0
    if (disposed || scrolling || !active) return
    if (element.matches(unavailable)) { deactivate(); return }
    // 布局读取先于所有样式写入，高频 pointermove 只保存最后一次坐标。
    if (pointer) {
      const rect = element.getBoundingClientRect()
      const x = rect.width ? Math.max(-1, Math.min(1, (pointer.x - rect.left) / rect.width * 2 - 1)) : 0
      const y = rect.height ? Math.max(-1, Math.min(1, (pointer.y - rect.top) / rect.height * 2 - 1)) : 0
      pointer = undefined
      element.style.setProperty('--liquid-light-x', `${(x + 1) * 50}%`)
      element.style.setProperty('--liquid-light-y', `${(y + 1) * 50}%`)
      element.style.setProperty('--liquid-light-alpha', interactive ? '.3' : '.17')
      target = { x: interactive ? x * elasticity * 14 : 0, y: interactive ? y * elasticity * 10 : 0,
        scaleX: interactive ? 1 + Math.abs(x) * elasticity * .09 : 1,
        scaleY: interactive ? 1 + Math.abs(y) * elasticity * .09 : 1,
        lens: pressed ? 1.32 : interactive ? 1.22 : 1.1 }
    }
    let distance = 0
    for (const key of Object.keys(current)) {
      current[key] += (target[key] - current[key]) * .2
      distance += Math.abs(target[key] - current[key])
    }
    if (interactive && elasticity > 0) element.style.setProperty('--liquid-transform', `translate3d(${current.x.toFixed(3)}px, ${current.y.toFixed(3)}px, 0) scale(${current.scaleX.toFixed(4)}, ${current.scaleY.toFixed(4)})`)
    for (const { node, scale } of channels) node.setAttribute('scale', String(scale * current.lens))
    if (distance > .005) schedule()
  }
  /**
   * 安排当前操作。
   */
  function schedule() {
    if (!frame && !disposed && !scrolling && active) frame = requestAnimationFrame(animate)
  }
  /**
   * 响应指针移动，更新当前交互状态。
   * 入参：event（事件）。
   */
  function pointerMove(event) {
    if (scrolling || event.pointerType !== 'mouse' || reduced.matches || coarse.matches || element.matches(unavailable)) return
    // 子按钮自行折射，父卡片不叠加第二套动态分色滤镜。
    if (!interactive && event.target.closest('[data-liquid-kind="button"]')) { if (active) deactivate(); return }
    hovered = true
    pointer = { x: event.clientX, y: event.clientY }
    activate()
    schedule()
  }
  /**
   * 响应指针移出，清理或恢复当前交互状态。
   */
  function pointerLeave() {
    if (document.activeElement === element && !scrolling) {
      // 已聚焦时保留同一个滤镜实例，避免 leave 后立刻销毁、重建造成闪帧。
      hovered = pressed = false
      pointer = undefined
      target = resting()
      element.style.setProperty('--liquid-light-alpha', '.1')
      schedule()
    } else deactivate()
  }
  /**
   * 响应指针按下，更新按压状态。
   * 入参：event（事件）。
   */
  function pointerDown(event) {
    if (!interactive && event.target.closest('[data-liquid-kind="button"]')) return
    if (reduced.matches || coarse.matches || scrolling || element.matches(unavailable)) return
    activate()
    pressed = true
    target.lens = 1.32
    schedule()
  }
  /**
   * 响应指针抬起，恢复未按压状态。
   */
  function pointerUp() { pressed = false; target.lens = hovered ? 1.22 : 1; schedule() }
  /**
   * 响应焦点进入当前元素。
   * 入参：event（事件）。
   */
  function focusIn(event) { if (event.target === element) activate() }
  /**
   * 响应焦点离开当前元素。
   * 入参：event（事件）。
   */
  function focusOut(event) { if (event.target === element && !hovered) deactivate() }
  /**
   * 偏好变化的处理逻辑。
   */
  function preferenceChanged() { deactivate() }
  const stopScrolling = observeGlassScrolling(/** 处理 observeGlassScrolling 调用中的回调逻辑。 入参：value。 */ value => {
    scrolling = value
    if (value) { if (active) deactivate() }
    else {
      if (!interactive) element.style.setProperty('--glass-backdrop', idleBackdrop)
      if (document.activeElement === element) activate()
    }
  })
  const listeners = { pointermove: pointerMove, pointerleave: pointerLeave, pointerdown: pointerDown,
    pointerup: pointerUp, pointercancel: pointerLeave, focusin: focusIn, focusout: focusOut }
  for (const [event, handler] of Object.entries(listeners)) element.addEventListener(event, handler, { passive: true })
  reduced.addEventListener('change', preferenceChanged)
  coarse.addEventListener('change', preferenceChanged)
  /**
   * 清理当前操作。
   */
  const cleanup = () => {
    if (disposed) return
    disposed = true
    stopScrolling()
    deactivate()
    for (const [event, handler] of Object.entries(listeners)) element.removeEventListener(event, handler)
    reduced.removeEventListener('change', preferenceChanged)
    coarse.removeEventListener('change', preferenceChanged)
    element.classList.remove('liquid-glass-surface')
    delete element.dataset.liquidKind
    for (const property of properties) element.style.removeProperty(property)
    attached.delete(element)
  }
  attached.set(element, cleanup)
  return cleanup
}

/** 登录卡片拥有独立背景材质；按钮交由全局管理器接管。 */
export function useLiquidGlass() {
  const glassElement = ref(null)
  /**
   * 清理当前操作。
   */
  let cleanup = () => {}
  onMounted(/** 在组件挂载后初始化相关状态。 */ () => {
    if (glassElement.value) cleanup = attachLiquidGlass(glassElement.value, { strength: 48, blur: .5, radius: 24 })
  })
  onBeforeUnmount(/** 在组件卸载前释放相关资源。 */ () => cleanup())
  return { glassElement }
}
