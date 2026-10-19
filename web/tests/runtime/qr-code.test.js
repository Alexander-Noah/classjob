import { expect, it } from 'vitest'
import { generateQrSvg, generateQrSvgDataUrl } from '../../src/utils/qrCode'
function modules(svg,border=4){return new Set([...svg.matchAll(/M(\d+) (\d+)h1v1h-1z/g)].map(match=>`${Number(match[1])-border},${Number(match[2])-border}`))}
it.each([[0,21],[17,21],[18,25],[32,25],[33,29],[53,29],[54,33],[78,33],[79,37],[106,37]])('chooses QR capacity for %i UTF-8 bytes', (length,size)=>{
  const svg=generateQrSvg('x'.repeat(length));expect(svg).toContain(`viewBox="0 0 ${size+8} ${size+8}"`)
  const matrix=modules(svg)
  // All versions retain the three seven-module finder patterns and their quiet separators.
  for(const [left,top] of [[0,0],[size-7,0],[0,size-7]]) for(let y=0;y<7;y++) for(let x=0;x<7;x++){
    const dark=x===0||x===6||y===0||y===6||(x>=2&&x<=4&&y>=2&&y<=4)
    expect(matrix.has(`${left+x},${top+y}`)).toBe(dark)
  }
  expect(matrix.has(`8,${size-8}`)).toBe(true)
  expect(matrix.size).toBeGreaterThan(size*size/3)
})
it('counts multibyte UTF-8 and rejects content above capacity',()=>{
  expect(generateQrSvg('实'.repeat(6))).toContain('viewBox="0 0 33 33"')
  expect(()=>generateQrSvg('x'.repeat(107))).toThrow('二维码内容过长')
})
it('produces deterministic payload-specific symbols and round-trippable SVG data URLs',()=>{
  const options={border:0,foreground:'#123456',background:'#abcdef'}
  const svg=generateQrSvg('https://lab.test/a',options)
  expect(svg).toContain('viewBox="0 0 25 25"');expect(svg).toContain('fill="#123456"');expect(svg).toContain('fill="#abcdef"')
  expect(generateQrSvg('https://lab.test/a',options)).toBe(svg)
  expect(generateQrSvg('https://lab.test/b',options)).not.toBe(svg)
  const url=generateQrSvgDataUrl('https://lab.test/a',options)
  expect(url).toMatch(/^data:image\/svg\+xml;charset=utf-8,/)
  expect(decodeURIComponent(url.slice(url.indexOf(',')+1))).toBe(svg)
})
