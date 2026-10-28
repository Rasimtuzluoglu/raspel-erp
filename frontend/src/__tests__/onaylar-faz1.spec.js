import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Onaylar revizyonu — Faz 1 kaynak-tabanlı doğrulama
 * (birleşik onay API sözleşmesi, KPI/sekme yapısı, ayarlar rotası ve i18n simetrisi).
 */

const SRC = join(process.cwd(), 'src')
const oku = (yol) => readFileSync(join(SRC, yol), 'utf8')

const view = oku('views/Onaylar.vue')
const ayarView = oku('views/OnayAyarlari.vue')
const sistemApi = oku('api/modules/sistem.js')
const dosyaApi = oku('api/modules/dosya.js')
const apiIndex = oku('api/index.js')
const router = oku('router/index.js')
const sidebar = oku('components/AppSidebar.vue')
const tr = JSON.parse(oku('locales/tr.json'))
const en = JSON.parse(oku('locales/en.json'))

const duzAnahtarlar = (nesne, onek = '') =>
  Object.entries(nesne).flatMap(([anahtar, deger]) => {
    const tam = onek ? `${onek}.${anahtar}` : anahtar
    if (deger && typeof deger === 'object' && !Array.isArray(deger)) {
      return duzAnahtarlar(deger, tam)
    }
    return [tam]
  })

describe('Onaylar — Faz 1', () => {
  it('api: onaylarAPI sözleşmesi ve Excel ucu tanımlı', () => {
    expect(sistemApi).toMatch(/export const onaylarAPI = \{/)
    expect(sistemApi).toMatch(/ozet\(\)\s*\{[\s\S]*?\/onaylar\/ozet/)
    expect(sistemApi).toMatch(/bekleyen\(params\)\s*\{[\s\S]*?\/onaylar\/bekleyen/)
    expect(sistemApi).toMatch(/karar\(data\)\s*\{[\s\S]*?\/onaylar\/karar/)
    expect(sistemApi).toMatch(/toplu\(data\)\s*\{[\s\S]*?\/onaylar\/toplu/)
    expect(sistemApi).toMatch(/gecmis\(params\)\s*\{[\s\S]*?\/onaylar\/gecmis/)
    expect(dosyaApi).toMatch(/onaylar\(params\)\s*\{[\s\S]*?\/exports\/onaylar/)
    expect(dosyaApi).toMatch(/onaylar\(params\)[\s\S]*?responseType: 'blob'/)
    expect(apiIndex).toMatch(/onaylarAPI/)
  })

  it('view: PageHeader, 5 KPI, 6 sekme ve hafıza anahtarı kullanılır', () => {
    expect(view).toMatch(/<PageHeader/)
    expect(view).toMatch(/kpiToplam/)
    expect(view).toMatch(/kpiIzin/)
    expect(view).toMatch(/kpiMasraf/)
    expect(view).toMatch(/kpiSatinalma/)
    expect(view).toMatch(/kpiSiparis/)

    expect(view).toMatch(/tabTumu/)
    expect(view).toMatch(/tabIzin/)
    expect(view).toMatch(/tabMasraf/)
    expect(view).toMatch(/tabSatinalma/)
    expect(view).toMatch(/tabSiparis/)
    expect(view).toMatch(/tabGecmis/)

    expect(view).toMatch(/const TAB_ANAHTAR = 'raspel_onaylar_tab'/)
    expect(view).toMatch(/localStorage\.getItem\(TAB_ANAHTAR\)/)
    expect(view).toMatch(/localStorage\.setItem\(TAB_ANAHTAR/)
    expect(view).toMatch(/onaylarAPI\.ozet\(\)/)
    expect(view).toMatch(/onaylarAPI\.bekleyen\(listeParams\(\)\)/)
    expect(view).toMatch(/onaylarAPI\.karar\(/)
    expect(view).toMatch(/onaylarAPI\.toplu\(/)
    expect(view).toMatch(/onaylarAPI\.gecmis\(/)
    expect(view).toMatch(/excelAPI\.onaylar\(/)
  })

  it('router + menü: /onay-ayarlari rotası ve başlık anahtarı', () => {
    expect(router).toMatch(/path: '\/onay-ayarlari'/)
    expect(router).toMatch(/name: 'OnayAyarlari'/)
    expect(router).toMatch(/import\('\.\.\/views\/OnayAyarlari\.vue'\)/)
    expect(router).toMatch(/'\/onay-ayarlari': 'nav\.onayAyarlari'/)
    expect(sidebar).toMatch(/path: '\/onay-ayarlari'/)
    expect(sidebar).toMatch(/nav\.onayAyarlari/)
  })

  it('onay ayarları ekranı mevcut onayAyariAPI ile çalışır', () => {
    expect(ayarView).toMatch(/import \{ onayAyariAPI \} from '\.\.\/api\/index\.js'/)
    expect(ayarView).toMatch(/onayAyariAPI\.listele\(\)/)
    expect(ayarView).toMatch(/onayAyariAPI\.kaydet\(/)
    expect(ayarView).toMatch(/esikTutar/)
    expect(ayarView).toMatch(/otomatikOnay/)
    expect(ayarView).toMatch(/<PageHeader/)
  })

  it('i18n: onaylar ve onayAyarlari blokları tr/en simetrik', () => {
    expect(duzAnahtarlar(tr.onaylar).sort()).toEqual(duzAnahtarlar(en.onaylar).sort())
    expect(duzAnahtarlar(tr.onayAyarlari).sort()).toEqual(duzAnahtarlar(en.onayAyarlari).sort())
    expect(tr.nav.onayAyarlari).toBe('Onay Ayarları')
    expect(en.nav.onayAyarlari).toBe('Approval Settings')
  })

  it('i18n: view’da kullanılan yeni anahtarlar iki dilde de var', () => {
    const anahtarlar = [
      'altBaslik',
      'kpiToplam',
      'kpiIzin',
      'kpiMasraf',
      'kpiSatinalma',
      'kpiSiparis',
      'tabTumu',
      'tabGecmis',
      'aramaPlaceholder',
      'topluOnayla',
      'topluReddet',
      'retGerekceEtiket',
      'retMinHata',
      'gecmisYuklemeHatasi',
      'sonucBaslik',
      'detayBaslik',
      'ilgiliKaydaGit'
    ]
    for (const anahtar of anahtarlar) {
      expect(tr.onaylar[anahtar]).toBeTruthy()
      expect(en.onaylar[anahtar]).toBeTruthy()
    }
    expect(tr.onaylar.topluOnayBasarili).toContain('{n}')
    expect(en.onaylar.topluOnayBasarili).toContain('{n}')
    expect(tr.onaylar.retTopluAciklama).toContain('{n}')
    expect(en.onaylar.retTopluAciklama).toContain('{n}')
  })
})
