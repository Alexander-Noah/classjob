// @vitest-environment happy-dom
import { expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import SafeMessageContent from '../../src/views/ai-assistant/components/SafeMessageContent.vue'
import { generateQrSvg } from '../../src/utils/qrCode'
import { normalizeAvatarUrl } from '../../src/utils/avatarUrl'

it('HTML, event handlers and markdown HTML remain literal text in actual Vue rendering', () => {
  const payload = '<img src=x onerror="alert(1)"><svg onload=alert(1)></svg><script>alert(1)</script>'
  const wrapper = mount(SafeMessageContent, { props: { content: `# 标题\n\n**${payload}**\n\n\`${payload}\`\n\n1. ${payload}` } })
  expect(wrapper.text()).toContain(payload)
  expect(wrapper.findAll('img,svg,script,iframe,object,a')).toHaveLength(0)
  expect(wrapper.find('strong').text()).toBe(payload)
  expect(wrapper.find('ol').attributes('start')).toBe('1')
  expect(wrapper.find('h3').text()).toBe('标题')
  wrapper.unmount()
})
it('mixed inline text and changing list types preserve formatting without parsing HTML', () => {
  const wrapper = mount(SafeMessageContent, { props: { content: '前缀 **<img onerror=alert(1)>** 后缀\n\n2. 第二项\n- 无序项' } })
  expect(wrapper.find('p').text()).toContain('前缀 <img onerror=alert(1)> 后缀')
  expect(wrapper.find('ol').attributes('start')).toBe('2')
  expect(wrapper.find('ul').text()).toBe('无序项')
  expect(wrapper.find('img').exists()).toBe(false)
  wrapper.unmount()
})
it('completed code fences and text after a list stay literal and remain separate blocks', () => {
  const wrapper = mount(SafeMessageContent, { props: { content: '```html\n<script>alert(1)</script>\n```\n\n- 列表项\n普通正文' } })
  expect(wrapper.find('pre code').text()).toBe('<script>alert(1)</script>')
  expect(wrapper.find('script').exists()).toBe(false)
  expect(wrapper.find('p').text()).toBe('普通正文')
  expect(wrapper.find('ul').text()).toBe('列表项')
  wrapper.unmount()
})
it('SVG options cannot escape attributes or create executable downloaded SVG', () => {
  for (const options of [{ foreground: '\"><script>alert(1)</script>' }, { background: 'url(javascript:alert(1))' }, { border: '\"><svg onload=alert(1)>' }, { border: NaN }]) {
    expect(() => generateQrSvg('test', options)).toThrow()
  }
  expect(generateQrSvg('<script>alert(1)</script>')).not.toContain('<script>')
})
it('avatar sources cannot point to executable document schemes', () => {
  for (const url of ['javascript:alert(1)', 'data:text/html,<script>alert(1)</script>', 'data:image/svg+xml,<svg onload=alert(1)>', 'blob:https://example.test/attacker', '//evil.test/a', '/\\evil.test/a']) {
    expect(normalizeAvatarUrl(url)).toBe('')
  }
  expect(normalizeAvatarUrl('/images/user.png')).toBe('/images/user.png')
  expect(normalizeAvatarUrl('images.example.test/avatar.png')).toBe('https://images.example.test/avatar.png')
  expect(normalizeAvatarUrl('avatars/user 1.png')).toContain('/files/avatar-proxy?objectKey=avatars%2Fuser%201.png')
})
