import test from 'node:test'
import assert from 'node:assert/strict'
import { formatMessageContent } from '../src/modules/ai-assistant/formatMessage.js'

test('assistant answers render paragraphs and ordered laboratory lists', () => {
  const html = formatMessageContent('今天有课的实验室：\n\n1. **软件开发实训室**\n2. 移动互联实训室\n\n以上为计划课表。')
  assert.match(html, /<p>今天有课的实验室：<\/p>/)
  assert.match(html, /<ol start="1"><li><strong>软件开发实训室<\/strong><\/li><li>移动互联实训室<\/li><\/ol>/)
  assert.ok(html.endsWith('<p>以上为计划课表。</p>'))
})
test('normal line breaks stay in the same paragraph and CRLF is normalized', () => {
  assert.equal(formatMessageContent('第一行\r\n第二行\r\n\r\n第三行'), '<p>第一行<br>第二行</p><p>第三行</p>')
})
test('model HTML is escaped in text, emphasis and code', () => {
  const html = formatMessageContent('**<img src=x onerror=alert(1)>**\n\n`<script>alert(1)</script>`')
  assert.ok(!html.includes('<img'))
  assert.ok(!html.includes('<script'))
  assert.match(html, /&lt;img/)
  assert.match(html, /<code>&lt;script&gt;/)
})
test('fenced code stays literal even before the streaming closing fence arrives', () => {
  const html = formatMessageContent('### 示例\n```html\n<strong>**保持原文**</strong>')
  assert.equal(html, '<h3>示例</h3><pre><code>&lt;strong&gt;**保持原文**&lt;/strong&gt;</code></pre>')
})
