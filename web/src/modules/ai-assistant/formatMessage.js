const escapeHtml = value => String(value).replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char])
const inline = value => escapeHtml(value).replace(/`([^`]+)`|\*\*(.+?)\*\*/g,
  (_, code, strong) => code !== undefined ? `<code>${code}</code>` : `<strong>${strong}</strong>`)

/** 只输出受控的文本排版标签；模型输出先转义，不接收原始 HTML 或可执行链接。 */
export function formatMessageContent(content = '') {
  const blocks = []
  let paragraph = [], items = [], listType = '', code = null
  let listStart = 1
  const flush = () => {
    if (paragraph.length) { blocks.push(`<p>${paragraph.map(inline).join('<br>')}</p>`); paragraph = [] }
    if (items.length) {
      blocks.push(`<${listType}${listType === 'ol' ? ` start="${listStart}"` : ''}>${items.map(item => `<li>${inline(item)}</li>`).join('')}</${listType}>`)
      items = []; listType = ''
    }
  }
  for (const line of String(content).replace(/\r\n?/g, '\n').split('\n')) {
    if (/^\s*```/.test(line)) {
      flush()
      if (code === null) code = []
      else { blocks.push(`<pre><code>${escapeHtml(code.join('\n'))}</code></pre>`); code = null }
      continue
    }
    if (code !== null) { code.push(line); continue }
    if (!line.trim()) { flush(); continue }
    const heading = line.match(/^#{1,6}\s+(.+)$/)
    if (heading) { flush(); blocks.push(`<h3>${inline(heading[1])}</h3>`); continue }
    const ordered = line.match(/^\s*(\d{1,6})[.)、]\s+(.+)$/)
    const bullet = line.match(/^\s*[-*+]\s+(.+)$/)
    if (ordered || bullet) {
      const type = ordered ? 'ol' : 'ul'
      if (paragraph.length || (listType && listType !== type)) flush()
      if (!items.length) listStart = ordered ? Number(ordered[1]) : 1
      listType = type
      items.push(ordered ? ordered[2] : bullet[1])
    } else {
      if (items.length) flush()
      paragraph.push(line)
    }
  }
  flush()
  if (code !== null) blocks.push(`<pre><code>${escapeHtml(code.join('\n'))}</code></pre>`)
  return blocks.join('')
}
