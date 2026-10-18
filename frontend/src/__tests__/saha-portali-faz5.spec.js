import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Faz5 — Saha Portalı: ortak stil dosyası, sınıf temizliği, koyu tema ve a11y
 * (kaynak-tabanlı doğrulama).
 */

const SRC = join(process.cwd(), 'src')
const saha = readFileSync(join(SRC, 'views/SahaPortali.vue'), 'utf8')
const panel = readFileSync(join(SRC, 'components/SahaSiparislerPanel.vue'), 'utf8')
const gorev = readFileSync(join(SRC, 'components/GorevNotPaneli.vue'), 'utf8')
const imza = readFileSync(join(SRC, 'components/ImzaPad.vue'), 'utf8')
const ortak = readFileSync(join(SRC, 'assets/saha-portali.css'), 'utf8')
const main = readFileSync(join(SRC, 'main.js'), 'utf8')

const gecersizSinif = /class="[^"]*(?:text-primary|text-secondary|bg-secondary)[^"]*"/

describe('Faz5 — Saha Portalı', () => {
  it('ortak saha-portali.css çıkarıldı ve main.js yükler', () => {
    expect(main).toMatch(/saha-portali\.css/)
    for (const k of [
      '.saha-form-kart',
      '.saha-form-govde',
      '.saha-alan',
      '.saha-satir-2',
      '.saha-fade',
      '.saha-baslik-satir',
      '.saha-count-pill',
      '.saha-modal-icerik'
    ]) {
      expect(ortak).toContain(k)
    }
  })

  it('bileşenler ortak saha- sınıflarını kullanır; eski kopya sınıflar kalmadı', () => {
    expect(saha).toMatch(/saha-form-kart/)
    expect(gorev).toMatch(/saha-form-kart/)
    expect(panel).toMatch(/saha-baslik-satir/)
    // Eski (saha- onekisiz) kopya sinif adlari kalmadi. Lookbehind, saha- ile
    // onekli yeni adlarin (ornegin saha-count-pill) eslesmesini engeller.
    for (const eski of ['form-container-card', 'section-title-row', 'count-pill', 'fade-in-section']) {
      const re = new RegExp(`(?<!saha-)${eski}`)
      expect(saha).not.toMatch(re)
      expect(panel).not.toMatch(re)
      expect(gorev).not.toMatch(re)
    }
  })

  it('geçersiz yardımcı sınıflar (text-primary/text-secondary/bg-secondary) temizlendi', () => {
    expect(saha).not.toMatch(gecersizSinif)
    expect(panel).not.toMatch(gecersizSinif)
    expect(gorev).not.toMatch(gecersizSinif)
  })

  it('ImzaPad koyu temada okunur (her temada beyaz zemin)', () => {
    expect(imza).toMatch(/background:\s*#ffffff/)
  })

  it('a11y: imza canvas etiketi ve görev kutusu aria-label taşır', () => {
    expect(imza).toMatch(/aria-label/)
    expect(gorev).toMatch(/aria-label/)
  })
})
