/** Parse supported message formatting into data; raw HTML is always a text node. */
const escapeHtml = value => String(value).replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'})[char])
const node = (tag, children = [], attrs = {}) => ({ tag, children, attrs })
const inline = value => {
  const result = [], pattern = /`([^`]+)`|\*\*(.+?)\*\*/g
  let start = 0
  for (const match of value.matchAll(pattern)) {
    if (match.index > start) result.push(value.slice(start, match.index))
    result.push(node(match[1] !== undefined ? 'code' : 'strong', [match[1] ?? match[2]]))
    start = match.index + match[0].length
  }
  if (start < value.length) result.push(value.slice(start))
  return result
}
export function parseMessageContent(content = '') {
  const blocks = []
  let paragraph = [], items = [], listType = '', code = null, listStart = 1
  const flush = () => {
    if (paragraph.length) {
      const children = []
      paragraph.forEach((line, index) => { if (index) children.push(node('br')); children.push(...inline(line)) })
      blocks.push(node('p', children)); paragraph = []
    }
    if (items.length) {
      blocks.push(node(listType, items.map(item => node('li', inline(item))), listType === 'ol' ? { start: listStart } : {}))
      items = []; listType = ''
    }
  }
  for (const line of String(content).replace(/\r\n?/g, '\n').split('\n')) {
    if (/^\s*```/.test(line)) {
      flush()
      if (code === null) code = []
      else { blocks.push(node('pre', [node('code', [code.join('\n')])])); code = null }
      continue
    }
    if (code !== null) { code.push(line); continue }
    if (!line.trim()) { flush(); continue }
    const heading = line.match(/^#{1,6}\s+(.+)$/)
    if (heading) { flush(); blocks.push(node('h3', inline(heading[1]))); continue }
    const ordered = line.match(/^\s*(\d{1,6})[.)、]\s+(.+)$/)
    const bullet = line.match(/^\s*[-*+]\s+(.+)$/)
    if (ordered || bullet) {
      const type = ordered ? 'ol' : 'ul'
      if (paragraph.length || (listType && listType !== type)) flush()
      if (!items.length) listStart = ordered ? Number(ordered[1]) : 1
      listType = type; items.push(ordered ? ordered[2] : bullet[1])
    } else { if (items.length) flush(); paragraph.push(line) }
  }
  flush()
  if (code !== null) blocks.push(node('pre', [node('code', [code.join('\n')])]))
  return blocks
}
/** Compatibility serializer for exports/tests; the live UI renders text VNodes instead. */
export function formatMessageContent(content = '') {
  const serialize = item => typeof item === 'string' ? escapeHtml(item)
    : item.tag === 'br' ? '<br>'
    : '<' + item.tag + (item.attrs.start !== undefined ? ' start="' + item.attrs.start + '"' : '') + '>' + item.children.map(serialize).join('') + '</' + item.tag + '>'
  return parseMessageContent(content).map(serialize).join('')
}
