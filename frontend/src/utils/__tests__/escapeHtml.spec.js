import { describe, it, expect } from 'vitest'
import { escapeHtml } from '../escapeHtml.js'

describe('escapeHtml.js', () => {
  it('escapes special characters', () => {
    expect(escapeHtml('<script>alert("x")</script>')).toBe('&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt;')
  })

  it('escapes ampersand and single quote', () => {
    expect(escapeHtml("a & b 'c'")).toBe('a &amp; b &#39;c&#39;')
  })

  it('handles null/undefined as empty string', () => {
    expect(escapeHtml(null)).toBe('')
    expect(escapeHtml(undefined)).toBe('')
  })

  it('leaves normal text unchanged', () => {
    expect(escapeHtml('merhaba dünya')).toBe('merhaba dünya')
  })
})
