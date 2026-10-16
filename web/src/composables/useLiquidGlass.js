import { onBeforeUnmount, onMounted, ref } from 'vue'

// Apply the supplied canvas displacement technique to the backdrop only.
export function useLiquidGlass() {
  const glassElement = ref(null)
  let cleanup = () => {}

  onMounted(() => {
    const element = glassElement.value
    if (!element) return
    const ns = 'http://www.w3.org/2000/svg'
    const id = `login-glass-${Math.random().toString(36).slice(2)}`
    const svg = document.createElementNS(ns, 'svg')
    svg.setAttribute('width', '0')
    svg.setAttribute('height', '0')
    svg.setAttribute('aria-hidden', 'true')
    svg.style.cssText = 'position:absolute;pointer-events:none'
    const defs = document.createElementNS(ns, 'defs')
    const filter = document.createElementNS(ns, 'filter')
    filter.id = id
    filter.setAttribute('color-interpolation-filters', 'sRGB')
    filter.setAttribute('x', '0%')
    filter.setAttribute('y', '0%')
    filter.setAttribute('width', '100%')
    filter.setAttribute('height', '100%')
    const map = document.createElementNS(ns, 'feImage')
    map.setAttribute('result', 'refraction-map')
    map.setAttribute('preserveAspectRatio', 'none')
    const displacement = document.createElementNS(ns, 'feDisplacementMap')
    displacement.setAttribute('in', 'SourceGraphic')
    displacement.setAttribute('in2', 'refraction-map')
    displacement.setAttribute('xChannelSelector', 'R')
    displacement.setAttribute('yChannelSelector', 'G')
    displacement.setAttribute('scale', '44')
    filter.append(map, displacement)
    defs.append(filter)
    svg.append(defs)
    document.body.append(svg)
    const canvas = document.createElement('canvas')
    const context = canvas.getContext('2d')
    let frame = 0

    function draw() {
      const { width, height } = element.getBoundingClientRect()
      if (!width || !height || !context) return
      // One texture pixel per CSS pixel; recompute only when the panel resizes.
      const w = Math.max(1, Math.round(width))
      const h = Math.max(1, Math.round(height))
      canvas.width = w
      canvas.height = h
      const pixels = context.createImageData(w, h)
      const radius = 24
      for (let y = 0; y < h; y++) {
        for (let x = 0; x < w; x++) {
          const px = x - w / 2
          const py = y - h / 2
          const qx = Math.abs(px) - w / 2 + radius
          const qy = Math.abs(py) - h / 2 + radius
          const distance = Math.min(Math.max(qx, qy), 0) + Math.hypot(Math.max(qx, 0), Math.max(qy, 0)) - radius
          const edge = Math.max(0, Math.min(1, 1 + distance / 32))
          const bend = edge * edge * (3 - 2 * edge)
          const nx = Math.sign(px) * (qx > 0 ? Math.max(qx, 0) : qx >= qy ? 1 : 0)
          const ny = Math.sign(py) * (qy > 0 ? Math.max(qy, 0) : qy > qx ? 1 : 0)
          const normalLength = Math.hypot(nx, ny) || 1
          const i = (y * w + x) * 4
          pixels.data[i] = 128 + 100 * bend * nx / normalLength
          pixels.data[i + 1] = 128 + 100 * bend * ny / normalLength
          pixels.data[i + 2] = 128
          pixels.data[i + 3] = 255
        }
      }
      context.putImageData(pixels, 0, 0)
      map.setAttribute('width', String(w))
      map.setAttribute('height', String(h))
      map.setAttribute('href', canvas.toDataURL())
      element.style.setProperty('--glass-backdrop', `url("#${id}") blur(1.5px) saturate(1.12) brightness(1.03)`)
    }
    const observer = new ResizeObserver(() => {
      cancelAnimationFrame(frame)
      frame = requestAnimationFrame(draw)
    })
    observer.observe(element)
    draw()
    cleanup = () => {
      cancelAnimationFrame(frame)
      observer.disconnect()
      svg.remove()
      element.style.removeProperty('--glass-backdrop')
    }
  })
  onBeforeUnmount(() => cleanup())
  return { glassElement }
}
