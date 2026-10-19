import test from 'node:test'
import assert from 'node:assert/strict'
import { safeHttpUrl, safeImageUrl } from '../../shared/safeUrl.js'

test('rejects executable URL schemes, control-character obfuscation, credentials and network paths', () => {
  for (const value of ['javascript:alert(1)', 'JaVaScRiPt:alert(1)', 'java\tscript:alert(1)', 'data:text/html,test', 'vbscript:msgbox(1)', '//evil.test', '/\\evil.test', 'https://user:pass@evil.test/a']) {
    assert.equal(safeHttpUrl(value), '', value)
  }
  assert.equal(safeHttpUrl('/camera/stream'), '/camera/stream')
  assert.equal(safeHttpUrl('https://camera.test/a'), 'https://camera.test/a')
})
test('raster data images are permitted, HTML and SVG data documents are rejected', () => {
  assert.equal(safeImageUrl('data:image/png;base64,YQ=='), 'data:image/png;base64,YQ==')
  for (const value of ['data:text/html;base64,YQ==', 'data:image/svg+xml;base64,YQ==', 'file:///C:/test.html']) assert.equal(safeImageUrl(value), '')
})
