import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Tahsilat revizyonu — Faz 2 kaynak-tabanlı doğrulama
 * (işlem geçmişi/iptal, hedef-performans, toplu e-posta, cari drawer, Excel).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/Tahsilat.vue')
const drawer = oku('components/TahsilatCariDrawer.vue')
const finans = oku('api/modules/finans.js')
const dosya = oku('api/modules/dosya.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

describe('Tahsilat — Faz 2 (geçmiş, iptal, performans, e-posta, drawer)', () => {
  it('geçmiş sunucu lazy tablo ve filtre çubuğunu kullanır', () => {
    expect(view).toMatch(/tahsilatAPI\.gecmisFiltreli\(/)
    expect(view).toMatch(/lazy/)
    expect(view).toMatch(/:total-records="gecmisToplam"/)
    expect(view).toMatch(/@page="gecmisSayfaDegisti"/)
    expect(view).toMatch(/@sort="gecmisSiralaDegisti"/)
    expect(view).toMatch(/p\.odemeYontemi = gecmisYontem\.value/)
    expect(view).toMatch(/p\.durum = gecmisDurum\.value/)
    expect(view).toMatch(/p\.baslangic = getLocalDateString\(gecmisBaslangic\.value\)/)
    expect(view).toMatch(/excelAPI\.tahsilatGecmis/)
  })

  it('işlem iptali gerekçe doğrulaması ve API çağrısı içerir', () => {
    expect(view).toMatch(/const iptalAc = \(islem\)/)
    expect(view).toMatch(/if \(gerekce\.length < 3\)/)
    expect(view).toMatch(/tahsilatAPI\.iptal\(iptalHedef\.value\.id, gerekce\)/)
    expect(view).toMatch(/tahsilat\.iptalGerekce/)
    expect(finans).toMatch(/iptal\(islemId, gerekce\)\s*{\s*return apiClient\.post\(`\/tahsilat\/islem\/\$\{islemId\}\/iptal`, \{ gerekce \}\)/)
  })

  it('performans sekmesi hedef düzenleme ve trend grafiği sunar', () => {
    expect(view).toMatch(/tahsilatAPI\.performans\(/)
    expect(view).toMatch(/tahsilatAPI\.hedefKaydet\(/)
    expect(view).toMatch(/tahsilat\.performansHedef/)
    expect(view).toMatch(/tahsilat\.performansGerceklesme/)
    expect(view).toMatch(/tahsilat\.trendBaslik/)
    expect(view).toMatch(/gerceklesme-bar/)
    expect(view).toMatch(/role="progressbar"/)
    expect(view).toMatch(/hedefDuzenlenebilir/)
    expect(finans).toMatch(/hedefKaydet\(data\)\s*{\s*return apiClient\.put\('\/tahsilat\/hedef', data\)/)
  })

  it('seçim + toplu e-posta onayı ve sonucu vardır', () => {
    expect(view).toMatch(/v-model:selection="seciliCariler"/)
    expect(view).toMatch(/selection-mode="multiple"/)
    expect(view).toMatch(/tahsilat\.topluEpostaBaslik/)
    expect(view).toMatch(/tahsilatAPI\.topluHatirlat\(seciliCariler\.value\.map\(\(c\) => c\.cariId\)\)/)
    expect(view).toMatch(/topluSonuc\.gonderilen/)
    expect(view).toMatch(/topluSonuc\.atlanan/)
    expect(finans).toMatch(/topluHatirlat\(cariIds\)\s*{\s*return apiClient\.post\('\/tahsilat\/toplu-hatirlat', \{ cariIds \}\)/)
  })

  it('cari drawer iletişim, faturalar ve son tahsilatları gösterir', () => {
    expect(view).toMatch(/<TahsilatCariDrawer/)
    expect(drawer).toMatch(/tahsilat\.iletisim/)
    expect(drawer).toMatch(/tahsilat\.acikFaturalar/)
    expect(drawer).toMatch(/tahsilat\.sonTahsilatlar/)
    expect(drawer).toMatch(/tel:\$\{cari\.telefon\}/)
    expect(drawer).toMatch(/mailto:\$\{cari\.email\}/)
    expect(drawer).toMatch(/\$emit\('tahsilat'/)
    expect(drawer).toMatch(/\$emit\('whatsapp'/)
    expect(drawer).toMatch(/\$emit\('eposta'/)
  })

  it('Excel uçları yaşlandırma ve geçmiş için tanımlıdır', () => {
    expect(dosya).toMatch(/tahsilatGecmis\(params\)\s*{\s*return apiClient\.get\('\/exports\/tahsilat-gecmis'/)
    expect(dosya).toMatch(/tahsilatYaslandirma\(params\)\s*{\s*return apiClient\.get\('\/exports\/tahsilat-yaslandirma'/)
    expect(view).toMatch(/excelAPI\.tahsilatYaslandirma/)
  })

  it('diyaloglar mobil için 92vw kırılımı kullanır', () => {
    const sayi = (view.match(/'575px': '92vw'/g) || []).length
    expect(sayi).toBeGreaterThanOrEqual(3)
  })

  it('yeni i18n anahtarları tr ve en için simetriktir', () => {
    const anahtarlar = [
      'tabGecmis',
      'tabPerformans',
      'iptal',
      'iptalBaslik',
      'iptalGerekceKisa',
      'performansHedef',
      'performansGerceklesme',
      'hedefDuzenle',
      'hedefKaydet',
      'trendBaslik',
      'topluEposta',
      'topluEpostaBaslik',
      'topluEpostaGonder',
      'cariDetayBaslik',
      'sonTahsilatlar',
      'epostaGonder'
    ]
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
