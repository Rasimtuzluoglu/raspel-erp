import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('../api/client.js', () => ({
  apiClient: {
    get: vi.fn(() => Promise.resolve({ data: [] })),
    post: vi.fn(() => Promise.resolve({ data: {} })),
    put: vi.fn(() => Promise.resolve({ data: {} })),
    delete: vi.fn(() => Promise.resolve({ data: {} }))
  }
}))

import { apiClient } from '../api/client.js'
import { cariHesapAPI, hareketAPI } from '../api/modules/ticaret.js'
import { raporAPI } from '../api/modules/rapor.js'
import { importAPI } from '../api/modules/dosya.js'
import { iletisimAPI, sohbetAPI } from '../api/modules/sistem.js'

// Faz 2.1: riskli cari (kredi limiti aşımı) ucu frontend API katmanından doğru çağrılmalı.
describe('cariHesapAPI.riskliCariler', () => {
  beforeEach(() => {
    apiClient.get.mockClear()
  })

  it("riskli cariler ucunu doğru yoldan çağırır", async () => {
    const res = await cariHesapAPI.riskliCariler()
    expect(apiClient.get).toHaveBeenCalledWith('/cari-hesaplar/riskli')
    expect(res.data).toEqual([])
  })
})

// Faz 2.3: toplu güncelleme ucu doğru yola POST etmeli.
describe('cariHesapAPI.topluGuncelle', () => {
  beforeEach(() => {
    apiClient.post.mockClear()
  })

  it('toplu güncelleme gövdesini doğru yola gönderir', async () => {
    await cariHesapAPI.topluGuncelle({ idler: [1, 2], aktif: false })
    expect(apiClient.post).toHaveBeenCalledWith('/cari-hesaplar/toplu-guncelle', { idler: [1, 2], aktif: false })
  })
})

// Faz 2.5: cari çoklu adres uçları doğru yollara gitmeli.
describe('cariHesapAPI adres uçları', () => {
  beforeEach(() => {
    apiClient.get.mockClear()
    apiClient.post.mockClear()
    apiClient.delete.mockClear()
  })

  it('adresleri listeler', async () => {
    await cariHesapAPI.adresler(5)
    expect(apiClient.get).toHaveBeenCalledWith('/cari-hesaplar/5/adresler')
  })

  it('adres ekler', async () => {
    await cariHesapAPI.adresEkle(5, { adres: 'X' })
    expect(apiClient.post).toHaveBeenCalledWith('/cari-hesaplar/5/adresler', { adres: 'X' })
  })

  it('adres siler', async () => {
    await cariHesapAPI.adresSil(5, 9)
    expect(apiClient.delete).toHaveBeenCalledWith('/cari-hesaplar/5/adresler/9')
  })
})

// Faz 2.6: hareket soft iptal ucu doğru yola POST etmeli.
describe('hareketAPI.iptal', () => {
  beforeEach(() => {
    apiClient.post.mockClear()
  })

  it('iptal ucunu doğru yoldan çağırır', async () => {
    await hareketAPI.iptal(11)
    expect(apiClient.post).toHaveBeenCalledWith('/hareketler/11/iptal')
  })
})

// Faz 2.7: cari hareket CSV içe aktarma ucu doğru yola POST etmeli.
describe('importAPI.hareket', () => {
  beforeEach(() => {
    apiClient.post.mockClear()
  })

  it('hareket import ucunu doğru yoldan çağırır', async () => {
    await importAPI.hareket(new Blob(['cariId;tarih;tur;tutar;aciklama\n']))
    expect(apiClient.post).toHaveBeenCalledWith(
      '/import/hareket',
      expect.any(FormData),
      { headers: { 'Content-Type': 'multipart/form-data' } }
    )
  })
})

// Faz 2.8: açılış fişi/devir ucu doğru yola POST etmeli.
describe('hareketAPI.acilis', () => {
  beforeEach(() => {
    apiClient.post.mockClear()
  })

  it('açılış fişi ucunu doğru yoldan çağırır', async () => {
    await hareketAPI.acilis({ cariHesapId: 5, tutar: 1000 })
    expect(apiClient.post).toHaveBeenCalledWith('/hareketler/acilis', { cariHesapId: 5, tutar: 1000 })
  })
})

// Faz 3.5: Mail & WhatsApp iletişim uçları doğru yollara gitmeli.
describe('iletisimAPI', () => {
  beforeEach(() => {
    apiClient.get.mockClear()
    apiClient.post.mockClear()
  })

  it('mail gönderim ucunu doğru yola POST eder', async () => {
    await iletisimAPI.mailGonder({ cariHesapId: 5, konu: 'K', mesaj: 'M' })
    expect(apiClient.post).toHaveBeenCalledWith('/iletisim/mail', { cariHesapId: 5, konu: 'K', mesaj: 'M' })
  })

  it('whatsapp bağlantı ucunu doğru yoldan çağırır', async () => {
    await iletisimAPI.whatsapp(5)
    expect(apiClient.get).toHaveBeenCalledWith('/iletisim/whatsapp/5')
  })
})

// Faz 4.3: dashboard AI özeti ucu.
describe('sohbetAPI.aiDashboardOzet', () => {
  beforeEach(() => {
    apiClient.get.mockClear()
  })

  it('AI dashboard özeti ucunu doğru yoldan çağırır', async () => {
    await sohbetAPI.aiDashboardOzet()
    expect(apiClient.get).toHaveBeenCalledWith('/sohbet/ai-dashboard-ozet')
  })
})

// Faz 2.2: tarih aralıklı cari ekstre ucu doğru parametrelerle çağrılmalı.
describe('raporAPI.cariEkstre', () => {
  beforeEach(() => {
    apiClient.get.mockClear()
  })

  it('tarih aralığı parametreleriyle ekstre ister', async () => {
    await raporAPI.cariEkstre({ cariHesapId: 5, baslangic: '2026-01-01', bitis: '2026-01-31' })
    expect(apiClient.get).toHaveBeenCalledWith('/raporlar/cari-ekstre', {
      params: { cariHesapId: 5, baslangic: '2026-01-01', bitis: '2026-01-31' }
    })
  })
})
