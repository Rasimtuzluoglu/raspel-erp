import { describe, it, expect, vi, beforeEach } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Çek/Senet revizyonu — Faz 1 (API sözleşmesi, KPI, sekmeler, filtreler).
 * Hem API katmanı davranışı hem de görünümün kaynak sözleşmesi doğrulanır.
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
import { excelAPI } from '../api/modules/dosya.js'

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/CekSenet.vue')
const finans = oku('api/modules/finans.js')
const dosya = oku('api/modules/dosya.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

describe('Çek/Senet — Faz 1 (API ve liste)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('filtreli, özet ve takvim uçlarını doğru parametrelerle çağırır', async () => {
    await cekSenetAPI.filtreli({ q: 'abc', tur: 'CEK', page: 0, size: 25, sort: 'vadeTarihi,asc' })
    expect(apiClient.get).toHaveBeenCalledWith('/cek-senet/filtreli', {
      params: { q: 'abc', tur: 'CEK', page: 0, size: 25, sort: 'vadeTarihi,asc' }
    })

    await cekSenetAPI.ozet()
    expect(apiClient.get).toHaveBeenLastCalledWith('/cek-senet/ozet', { params: {} })

    await cekSenetAPI.takvim({ baslangic: '2026-10-01', bitis: '2026-10-31' })
    expect(apiClient.get).toHaveBeenLastCalledWith('/cek-senet/takvim', {
      params: { baslangic: '2026-10-01', bitis: '2026-10-31' }
    })
  })

  it('Excel ucu blob olarak /exports/cek-senet çağırır', async () => {
    const params = { durum: 'PORTFOY' }
    await excelAPI.cekSenet(params)
    expect(apiClient.get).toHaveBeenCalledWith('/exports/cek-senet', {
      params,
      responseType: 'blob'
    })
    expect(dosya).toMatch(/cekSenet\(params\)\s*{\s*return apiClient\.get\('\/exports\/cek-senet', \{ params, responseType: 'blob' \}\)/)
  })

  it('durum güncelleme hem string hem nesne gövdesini destekler', async () => {
    await cekSenetAPI.durumGuncelle(5, 'PORTFOY')
    expect(apiClient.put).toHaveBeenLastCalledWith('/cek-senet/5/durum', { durum: 'PORTFOY' })

    await cekSenetAPI.durumGuncelle(5, { durum: 'TAHSIL_EDILDI', kasaId: 3, faturaId: 9 })
    expect(apiClient.put).toHaveBeenLastCalledWith('/cek-senet/5/durum', {
      durum: 'TAHSIL_EDILDI',
      kasaId: 3,
      faturaId: 9
    })
  })

  it('PageHeader + 5 KPI kartı içerir', () => {
    expect(view).toMatch(/<PageHeader/)
    expect(view).toMatch(/icon="pi pi-credit-card"/)
    expect(view).toMatch(/v-for="i in 5"/)
    expect(view).toMatch(/cekSenet\.kpiPortfoy/)
    expect(view).toMatch(/cekSenet\.kpiTeminatta/)
    expect(view).toMatch(/cekSenet\.kpiVadesiGecen/)
    expect(view).toMatch(/cekSenet\.kpiBuAy/)
    expect(view).toMatch(/cekSenet\.kpiProtesto/)
    expect(view).toMatch(/cekSenetAPI\.ozet\(/)
  })

  it('üç sekme ve sekme hafızası tanımlıdır', () => {
    expect(view).toMatch(/<TabView v-model:active-index="aktifSekme">/)
    expect(view).toMatch(/cekSenet\.tabPortfoy/)
    expect(view).toMatch(/cekSenet\.tabTakvim/)
    expect(view).toMatch(/cekSenet\.tabGecmis/)
    expect(view).toMatch(/raspel_cek_senet_tab/)
    expect(view).toMatch(/const aktifSekme = ref\(sekmeOku\(\)\)/)
  })

  it('sunucu filtreleri (q, tür, durum, cari, vade aralığı, min tutar, protesto) bağlıdır', () => {
    expect(view).toMatch(/cekSenetAPI\.filtreli\(\{/)
    expect(view).toMatch(/if \(q\.value\?\.trim\(\)\) p\.q = q\.value\.trim\(\)/)
    expect(view).toMatch(/if \(tur\.value !== 'TUMU'\) p\.tur = tur\.value/)
    expect(view).toMatch(/if \(durum\.value\) p\.durum = durum\.value/)
    expect(view).toMatch(/if \(cariId\.value != null\) p\.cariId = cariId\.value/)
    expect(view).toMatch(/if \(bas\) p\.baslangic = getLocalDateString\(bas\)/)
    expect(view).toMatch(/if \(bit\) p\.bitis = getLocalDateString\(bit\)/)
    expect(view).toMatch(/if \(vadeAralik\.value\) p\.vadeAralik = vadeAralik\.value/)
    expect(view).toMatch(/p\.minTutar = minTutar\.value/)
    expect(view).toMatch(/p\.protesto = true/)
  })

  it('filtre hafızası raspel_cek_senet_filtre anahtarını kullanır', () => {
    expect(view).toMatch(/raspel_cek_senet_filtre/)
    expect(view).toMatch(/localStorage\.setItem\(\s*FILTRE_ANAHTAR/)
    expect(view).toMatch(/localStorage\.getItem\(FILTRE_ANAHTAR\)/)
  })

  it('lazy/sort/page tablo, mobil kart ve Excel aksiyonu vardır', () => {
    expect(view).toMatch(/<AppDataTable[\s\S]*?\blazy\b/)
    expect(view).toMatch(/:total-records="toplam"/)
    expect(view).toMatch(/@page="sayfaDegisti"/)
    expect(view).toMatch(/@sort="siralaDegisti"/)
    expect(view).toMatch(/class="cs-mobil"/)
    expect(view).toMatch(/excelAPI\.cekSenet\(filtreParams\(\)\)/)
    expect(view).toMatch(/cekSenet\.excelIndirildi/)
  })

  it('finans API yeni uçları tanımlar', () => {
    expect(finans).toMatch(/filtreli\(params = \{\}\)\s*{\s*return apiClient\.get\('\/cek-senet\/filtreli'/)
    expect(finans).toMatch(/ozet\(params = \{\}\)\s*{\s*return apiClient\.get\('\/cek-senet\/ozet'/)
    expect(finans).toMatch(/takvim\(params = \{\}\)\s*{\s*return apiClient\.get\('\/cek-senet\/takvim'/)
  })

  it('yeni i18n anahtarları tr ve en için simetriktir', () => {
    const anahtarlar = [
      'altBaslik',
      'tabPortfoy',
      'tabTakvim',
      'tabGecmis',
      'kpiPortfoy',
      'kpiTeminatta',
      'kpiVadesiGecen',
      'kpiBuAy',
      'kpiProtesto',
      'filtreTemizle',
      'agingBuHafta',
      'agingBuAy',
      'agingGeciken',
      'aging30',
      'aging60',
      'aging90',
      'takvimBos',
      'gecmisBos'
    ]
    for (const anahtar of anahtarlar) {
      expect(tr).toContain(`"${anahtar}"`)
      expect(en).toContain(`"${anahtar}"`)
    }
  })
})
