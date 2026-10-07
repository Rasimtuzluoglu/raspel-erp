import { describe, it, expect, beforeEach, vi } from 'vitest'
import { usePosSatisGecmisi } from '../usePosSatisGecmisi.js'

/**
 * POS SATIS SONUCU ve GUNLUK GECMIS — davranis testleri.
 *
 * Korunan is kurallari:
 *   - Ozet, sunucu yanitindaki fatura numarasi ile YEREL tutarlari birlestirir.
 *   - Ozet yalnizca satis basarili oldugunda gosterilir.
 *   - Iptal, son satis yoksa sunucuya GITMEZ (bos istek yok) ve uyarir.
 *   - Iptal sonrasi gunluk liste tazelenir ve `onIptal` geri cagrisi calisir.
 *   - Iptal hatasi kullaniciya bildirilir, sessizce yutulmaz.
 */

vi.mock('../../api/index.js', () => ({
  faturaAPI: { getAll: vi.fn(), updateDurum: vi.fn() }
}))
vi.mock('../../utils/format.js', async (importOriginal) => {
  const gercek = await importOriginal()
  return { ...gercek, getLocalDateString: vi.fn(() => '2026-05-01') }
})

import { faturaAPI } from '../../api/index.js'

const kur = (router = { push: vi.fn() }) => {
  const bildir = { basarili: vi.fn(), uyari: vi.fn(), hata: vi.fn() }
  const s = usePosSatisGecmisi({ t: (k) => `[${k}]`, bildir, router })
  return { ...s, bildir, router }
}

const TUTARLAR = {
  genelToplam: 500,
  odenenTutar: 300,
  kalanTutar: 200,
  odemeYontemi: 'NAKIT',
  paraUstu: 0,
  fisModu: true
}

beforeEach(() => {
  faturaAPI.getAll.mockReset()
  faturaAPI.updateDurum.mockReset()
})

describe('usePosSatisGecmisi — satis ozeti', () => {
  it('ozet sunucu fatura no + yerel tutarlari birlestirir', () => {
    const s = kur()
    const ozet = s.satisOzetiOlustur({ data: { faturaNumarasi: 'FTR-1', id: 9 } }, TUTARLAR)
    expect(ozet.faturaNo).toBe('FTR-1')
    expect(ozet.toplam).toBe(500)
    expect(ozet.odenen).toBe(300)
    expect(ozet.kalan).toBe(200)
    expect(ozet.yontem).toBe('NAKIT')
    expect(ozet.fisModu).toBe(true)
  })

  it('yanit eksikse (offline) faturaNo undefined kalir, cokmez', () => {
    const s = kur()
    const ozet = s.satisOzetiOlustur(undefined, TUTARLAR)
    expect(ozet.faturaNo).toBeUndefined()
    expect(ozet.toplam).toBe(500)
  })

  it('satisSonucunuKaydet hem ozeti hem son satisi yazar ve dialogu acar', () => {
    const s = kur()
    expect(s.satisOzetDialog.value).toBe(false)
    s.satisSonucunuKaydet({ data: { faturaNumarasi: 'FTR-1', id: 9 } }, TUTARLAR)
    expect(s.sonSatis.value.id).toBe(9)
    expect(s.satisOzet.value.faturaNo).toBe('FTR-1')
    expect(s.satisOzetDialog.value).toBe(true)
  })

  it('satisOzetiKapat dialogu ve ozeti temizler, son satisi KORUR', () => {
    const s = kur()
    s.satisSonucunuKaydet({ data: { id: 9, faturaNumarasi: 'F' } }, TUTARLAR)
    s.satisOzetiKapat()
    expect(s.satisOzetDialog.value).toBe(false)
    expect(s.satisOzet.value).toBeNull()
    // "Son satisi iptal et" hala kullanilabilmeli
    expect(s.sonSatis.value.id).toBe(9)
  })
})

describe('usePosSatisGecmisi — gunluk satislar', () => {
  it('bugun + SATIS filtresiyle yukler', async () => {
    faturaAPI.getAll.mockResolvedValue({ data: { content: [{ id: 1 }] } })
    const s = kur()
    await s.gunlukSatislariYukle()
    expect(faturaAPI.getAll).toHaveBeenCalledWith({ size: 200, tur: 'SATIS', bas: '2026-05-01', bit: '2026-05-01' })
    expect(s.gunlukSatislar.value).toHaveLength(1)
  })

  it('hata halinde liste bosalir (ekran cokmez)', async () => {
    faturaAPI.getAll.mockRejectedValue(new Error('yok'))
    const s = kur()
    await s.gunlukSatislariYukle()
    expect(s.gunlukSatislar.value).toEqual([])
  })

  it('fatura detayina yonlendirir', () => {
    const s = kur()
    s.bugunkuSatisGoruntule({ id: 42 })
    expect(s.router.push).toHaveBeenCalledWith('/faturalar/42')
  })

  it('id yoksa yonlendirme yapilmaz', () => {
    const s = kur()
    s.bugunkuSatisGoruntule({})
    expect(s.router.push).not.toHaveBeenCalled()
  })

  it('router yoksa cokmez', () => {
    const s = kur(undefined)
    expect(() => s.bugunkuSatisGoruntule({ id: 1 })).not.toThrow()
  })
})

describe('usePosSatisGecmisi — son satisi iptal', () => {
  it('son satis yoksa sunucuya GITMEZ ve uyarir', async () => {
    const s = kur()
    const sonuc = await s.sonSatisiIptalEt()
    expect(sonuc).toBe(false)
    expect(faturaAPI.updateDurum).not.toHaveBeenCalled()
    expect(s.bildir.uyari).toHaveBeenCalled()
  })

  it('iptal basarili olunca son satis temizlenir ve liste tazelenir', async () => {
    faturaAPI.updateDurum.mockResolvedValue({})
    faturaAPI.getAll.mockResolvedValue({ data: { content: [] } })
    const s = kur()
    s.satisSonucunuKaydet({ data: { id: 9, faturaNumarasi: 'F' } }, TUTARLAR)

    const onIptal = vi.fn().mockResolvedValue(undefined)
    const sonuc = await s.sonSatisiIptalEt(onIptal)

    expect(sonuc).toBe(true)
    expect(faturaAPI.updateDurum).toHaveBeenCalledWith(9, 'IPTAL')
    expect(s.sonSatis.value).toBeNull()
    expect(s.bildir.basarili).toHaveBeenCalled()
    expect(onIptal).toHaveBeenCalled()
  })

  it('iptal hatasi kullaniciya bildirilir ve son satis KORUNUR', async () => {
    faturaAPI.updateDurum.mockRejectedValue({ response: { data: { message: 'Yetki yok' } } })
    const s = kur()
    s.satisSonucunuKaydet({ data: { id: 9 } }, TUTARLAR)

    const sonuc = await s.sonSatisiIptalEt()

    expect(sonuc).toBe(false)
    expect(s.bildir.hata).toHaveBeenCalledWith('Yetki yok')
    // Iptal edilemedi -> son satis hala duruyor (tekrar denenebilir)
    expect(s.sonSatis.value.id).toBe(9)
  })

  it('onIptal verilmezse de guvenli', async () => {
    faturaAPI.updateDurum.mockResolvedValue({})
    faturaAPI.getAll.mockResolvedValue({ data: { content: [] } })
    const s = kur()
    s.satisSonucunuKaydet({ data: { id: 9 } }, TUTARLAR)
    await expect(s.sonSatisiIptalEt()).resolves.toBe(true)
  })

  it('onIptal (tazeleme) hata verse de iptal BASARILI sayilir', async () => {
    // KRITIK: iptal cagrisi basarili oldugu halde tazeleme hatasi
    // "iptal basarisiz" gibi gosterilmemeli.
    faturaAPI.updateDurum.mockResolvedValue({})
    faturaAPI.getAll.mockResolvedValue({ data: { content: [] } })
    const s = kur()
    s.satisSonucunuKaydet({ data: { id: 9 } }, TUTARLAR)
    const onIptal = vi.fn().mockRejectedValue(new Error('stok tazeleme hatasi'))
    await expect(s.sonSatisiIptalEt(onIptal)).resolves.toBe(true)
    expect(s.bildir.basarili).toHaveBeenCalled()
    expect(s.bildir.hata).not.toHaveBeenCalled()
    expect(s.sonSatis.value).toBeNull()
  })
})