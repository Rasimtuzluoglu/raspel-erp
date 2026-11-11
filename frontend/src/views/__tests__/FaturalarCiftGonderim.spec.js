import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Cift gonderim korumasi artik `useFaturaForm` icinde.
 * Davranis Cypress'te (fatura-form.cy.js) dblclick senaryosuyla dogrulanir;
 * burada sozlesme duzeyinde kontrol edilir.
 */

const form = readFileSync(join(process.cwd(), 'src/composables/useFaturaForm.js'), 'utf8')

describe('useFaturaForm - cift gonderim engeli', () => {
  it('saving bayragi ile ikinci cagri yok sayilir', () => {
    expect(form).toContain('if (saving.value) return false')
    expect(form).toContain('saving.value = true')
  })

  it('istek bitince bayrak finally ile sifirlanir', () => {
    expect(form).toMatch(/finally\s*\{\s*saving\.value = false/)
  })

  it('dogrulama hatasinda istek atilmaz', () => {
    expect(form).toContain('const dogrulamaHatasi = dogrula()')
    expect(form.indexOf('const dogrulamaHatasi = dogrula()')).toBeLessThan(
      form.indexOf('faturaStore.addFatura')
    )
  })
})
