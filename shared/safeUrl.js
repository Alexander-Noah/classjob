/** Explicit browser URL contexts: do not allow executable/document data URLs. */
export function safeHttpUrl(value, { allowRelative = true } = {}) {
  if (typeof value !== 'string') return ''
  const text = value.trim()
  if (!text || /[\u0000-\u0020\u007f-\u009f\\]/.test(text)) return ''
  if (/^https?:\/\//i.test(text)) {
    try { const url = new URL(text); return !url.username && !url.password && url.hostname ? text : '' } catch { return '' }
  }
  if (!allowRelative || text.startsWith('//') || /^[^/?#]*:/.test(text)) return ''
  return text
}

export function safeImageUrl(value) {
  if (typeof value !== 'string') return ''
  const text = value.trim()
  if (/^data:image\/(?:png|jpeg|gif|webp);base64,[a-z0-9+/]+={0,2}$/i.test(text)) return text
  return safeHttpUrl(text)
}
