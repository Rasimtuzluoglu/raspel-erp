import { describe, it, expect, vi, beforeEach } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Çek/Senet revizyonu — Faz 2 (tahsil iptali, ciro, teminat, protesto, drawer).
 */

vi.mock('../api/client.js', () => ({
  apiClient: {
    get: vi.fn(() => Promise.resolve({ data: {} })),
    post: vi.fn(() => Promise.resolve({ data: {} })),
    put: vi.fn(() => Promise.resolve({ data: {} })),
    delete: vi.fn(() => Promise.resolve({ data: {} }))
  }
}))

import { apiClient } from '../api/client.js'
import { cekSenetAPI } from '../api/modules/finans.js'

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/CekSenet.vue')
const drawer = oku('components/CekSenetDetayDrawer.vue')
const finans = oku('api/modules/finans.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

describe('Çek/Senet — Faz 2 (yaşam döngüsü aksiyonları)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('tahsil iptali gerekçesini gövdede gönderir', async () => {
    await cekSenetAPI.tahsilIptal(7, 'Mükerrer kayıt')
    expect(apiClient.post).toHaveBeenCalledWith('/cek-senet/7/tahsil-iptal', {
      gerekce: 'Mükerrer kayıt'
    })
    expect(finans).toMatch(/tahsilIptal\(id, gerekce\)\s*{\s*return apiClient\.post\(`\/cek-senet\/\$\{id\}\/tahsil-iptal`, \{ gerekce \}\)/)
  })

  it('ciro ve teminat açıklamalarını gönderir, iade uçlarını çağırır', async () => {
    await cekSenetAPI.ciro(3, { cariId: 12, not: 'Tedarikçi' })
    expect(apiClient.post).toHaveBeenLastCalledWith('/cek-senet/3/ciro', {
      cariId: 12,
      not: 'Tedarikçi'
    })

    await cekSenetAPI.teminat(3, 'Bankaya')
    expect(apiClient.post).toHaveBeenLastCalledWith('/cek-senet/3/teminat', { aciklama: 'Bankaya' })

    await cekSenetAPI.ciroIade(3)
    expect(apiClient.post).toHaveBeenLastCalledWith('/cek-senet/3/ciro-iade')

    await cekSenetAPI.iadeAl(3)
    expect(apiClient.post).toHaveBeenLastCalledWith('/cek-senet/3/iade-al')

    await cekSenetAPI.teminatIade(3)
    expect(apiClient.post).toHaveBeenLastCalledWith('/cek-senet/3/teminat-iade')

    await cekSenetAPI.protesto(3, 'Karşılıksız')
    expect(apiClient.post).toHaveBeenLastCalledWith('/cek-senet/3/protesto', {
      aciklama: 'Karşılıksız'
    })
  })

  it('tahsil modalı fatura ve kasa/banka seçimini durum ucuna taşır', () => {
    expect(view).toMatch(/const tahsilDialogAc = \(kayit\)/)
    expect(view).toMatch(/faturaAPI\.getAll\(\{ cariHesapId: kayit\.cariHesapId, size: 200 \}\)/)
    expect(view).toMatch(/durum: 'TAHSIL_EDILDI'/)
    expect(view).toMatch(/faturaId: tahsilFaturaId\.value \|\| null/)
    expect(view).toMatch(/kasaId: tahsilHesapTipi\.value === 'KASA' \? tahsilKasaId\.value : null/)
    expect(view).toMatch(/bankaId: tahsilHesapTipi\.value === 'BANKA' \? tahsilBankaId\.value : null/)
    expect(view).toMatch(/cekSenet\.tahsilAciklama/)
  })

  it('tahsil iptali gerekçesini en az 3 karakter için doğrular', () => {
    expect(view).toMatch(/const iptalKaydet = async/)
    expect(view).toMatch(/if \(gerekce\.length < 3\)/)
    expect(view).toMatch(/cekSenet\.tahsilIptalGerekceKisa/)
    expect(view).toMatch(/cekSenetAPI\.tahsilIptal\(iptalHedef\.value\.id, gerekce\)/)
  })

  it('ciro hedefi zorunludur ve ciro/teminat/protesto modalları vardır', () => {
    expect(view).toMatch(/if \(ciroForm\.value\.cariId == null\)/)
    expect(view).toMatch(/cekSenet\.ciroHedefZorunlu/)
    expect(view).toMatch(/cekSenetAPI\.ciro\(ciroHedef\.value\.id, \{/)
    expect(view).toMatch(/cekSenetAPI\.teminat\(teminatHedef\.value\.id/)
    expect(view).toMatch(/cekSenetAPI\.protesto\(protestoHedef\.value\.id/)
    expect(view).toMatch(/cekSenet\.protestoUyari/)
  })

  it('ciro iadesi, teminat iadesi ve portföye alma onay ister', () => {
    expect(view).toMatch(/const onayliAksiyon = \(mesaj, baslik, basariMesaji, islem\)/)
    expect(view).toMatch(/cekSenetAPI\.ciroIade\(kayit\.id\)/)
    expect(view).toMatch(/cekSenetAPI\.teminatIade\(kayit\.id\)/)
    expect(view).toMatch(/cekSenetAPI\.iadeAl\(kayit\.id\)/)
    expect(view).toMatch(/confirm\.require\(\{/)
  })

  it('detay drawer bilgiler, ciro/teminat, tahsil bağları ve zaman çizelgesi sunar', () => {
    expect(view).toMatch(/<CekSenetDetayDrawer/)
    expect(view).toMatch(/cekSenetAPI\.getById\(kayit\.id\)/)
    expect(drawer).toMatch(/cekSenet\.bilgiler/)
    expect(drawer).toMatch(/cekSenet\.cariFatura/)
    expect(drawer).toMatch(/cekSenet\.ciroTeminat/)
    expect(drawer).toMatch(/cekSenet\.tahsilBaglari/)
    expect(drawer).toMatch(/cekSenet\.zamanCizelgesi/)
    expect(drawer).toMatch(/class="zaman-cizelgesi"/)
    expect(drawer).toMatch(/\$emit\('tahsilIptal'/)
    expect(drawer).toMatch(/\$emit\('protesto'/)
  })

  it('duruma göre aksiyon görünürlüğü tanımlıdır', () => {
    expect(drawer).toMatch(/const AKSIYONLAR = \{/)
    expect(drawer).toMatch(/tahsilIptal: \['TAHSIL_EDILDI'\]/)
    expect(drawer).toMatch(/ciroIade: \['CIRO_EDILDI'\]/)
    expect(drawer).toMatch(/teminatIade: \['TEMINATTA'\]/)
    expect(drawer).toMatch(/iadeAl: \['IADE'\]/)
    expect(drawer).toMatch(/protesto: \['PORTFOY', 'TEMINATTA'\]/)
  })

  it('erişilebilirlik: aria-live, aria-pressed, skeleton ve 92vw diyaloglar', () => {
    expect(view).toMatch(/aria-live="polite"/)
    expect(view).toMatch(/:aria-pressed="tur === secenek\.value"/)
    expect(view).toMatch(/:aria-pressed="sadeceProtesto"/)
    expect(view).toMatch(/:aria-pressed="takvimGorunum === 'grid'"/)
    expect(view).toMatch(/<SkeletonLoader/)
    const mobil = (view.match(/'575px': '92vw'/g) || []).length
    expect(mobil).toBeGreaterThanOrEqual(5)
    expect(drawer).toMatch(/'575px': '92vw'/)
    expect(view).toMatch(/prefers-reduced-motion: reduce/)
  })

  it('işlem etiketleri ve durum çevirileri tr/en simetriktir', () => {
    const anahtarlar = [
      'durumPortfoy',
      'durumTeminatta',
      'durumCiroEdildi',
      'durumTahsilEdildi',
      'durumProtesto',
      'durumIade',
      'islemOlustur',
      'islemGuncelle',
      'islemTeminat',
      'islemTeminatIade',
      'islemCiro',
      'islemCiroIade',
      'islemIadeAl',
      'islemTahsil',
      'islemTahsilIptal',
      'islemProtesto',
      'tahsilIptalGerekceKisa',
      'ciroHedefZorunlu',
      'protestoUyari',
      'detayBaslik'
    ]
    for (const anahtar of anahtarlar) {
      expect(tr).toContain(`"${anahtar}"`)
      expect(en).toContain(`"${anahtar}"`)
    }
  })
})
