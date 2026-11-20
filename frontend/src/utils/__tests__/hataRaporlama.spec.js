import { describe, it, expect, vi, beforeEach } from 'vitest'
import { hataRaporlamayiBaslat } from '../hataRaporlama.js'

const bellekDepo = () => {
  const veri = new Map()
  return {
    getItem: (k) => (veri.has(k) ? veri.get(k) : null),
    setItem: (k, v) => veri.set(k, String(v)),
    removeItem: (k) => veri.delete(k),
    _veri: veri
  }
}

const sahtePencere = () => {
  const dinleyiciler = new Map()
  return {
    addEventListener: vi.fn((ad, fn) => dinleyiciler.set(ad, fn)),
    removeEventListener: vi.fn((ad) => dinleyiciler.delete(ad)),
    dispatch: (ad, olay) => dinleyiciler.get(ad)?.(olay),
    _dinleyiciler: dinleyiciler
  }
}

const kur = (secenekler = {}) => {
  const gonderici = secenekler.gonderici || vi.fn().mockResolvedValue({})
  const depo = secenekler.depo || bellekDepo()
  const pencere = secenekler.pencere || sahtePencere()
  let simdi = secenekler.simdi ?? 1_700_000_000_000
  const raporlayici = hataRaporlamayiBaslat({
    gonderici,
    depo,
    pencere,
    konum: () => '/cari-hesaplar',
    surum: '1.0.0',
    tarayici: 'Test',
    kimlikVarMi: secenekler.kimlikVarMi || (() => true),
    simdi: () => simdi,
    rastgele: () => 0.5
  })
  return { raporlayici, gonderici, depo, pencere, zamanIlerlet: (ms) => { simdi += ms } }
}

describe('hataRaporlama', () => {
  let ortam

  beforeEach(() => {
    ortam = kur()
  })

  it('hatayı göndericiye beklenen gövdeyle iletir', async () => {
    await ortam.raporlayici.hataYakala({ tur: 'vue', mesaj: 'boom', stack: 'at x (a.js:1)' })

    expect(ortam.gonderici).toHaveBeenCalledTimes(1)
    const govde = ortam.gonderici.mock.calls[0][0]
    expect(govde.tur).toBe('vue')
    expect(govde.mesaj).toBe('boom')
    expect(govde.rota).toBe('/cari-hesaplar')
    expect(govde.surum).toBe('1.0.0')
    expect(govde.hataId).toBeTruthy()
  })

  it('aynı hatayı susturma penceresinde tekrar göndermez', async () => {
    await ortam.raporlayici.hataYakala({ tur: 'js', mesaj: 'aynı', stack: 'at x' })
    await ortam.raporlayici.hataYakala({ tur: 'js', mesaj: 'aynı', stack: 'at x' })

    expect(ortam.gonderici).toHaveBeenCalledTimes(1)
  })

  it('dakikada en fazla beş farklı hata gönderir', async () => {
    for (let i = 0; i < 8; i++) {
      await ortam.raporlayici.hataYakala({ tur: 'js', mesaj: `hata-${i}`, stack: `at x${i}` })
    }

    expect(ortam.gonderici).toHaveBeenCalledTimes(5)
  })

  it('gönderim başarısızsa kaydı kuyruğa alır', async () => {
    const ortam2 = kur({ gonderici: vi.fn().mockRejectedValue(new Error('offline')) })

    await ortam2.raporlayici.hataYakala({ tur: 'promise', mesaj: 'offline hata', stack: 'at y' })

    const kuyruk = JSON.parse(ortam2.depo.getItem('raspel_hata_kuyrugu') || '[]')
    expect(kuyruk).toHaveLength(1)
    expect(kuyruk[0].mesaj).toBe('offline hata')
  })

  it('kuyruk en fazla 20 kayıt tutar (en eski düşer)', async () => {
    const ortam2 = kur({ gonderici: vi.fn().mockRejectedValue(new Error('offline')) })
    for (let i = 0; i < 24; i++) {
      await ortam2.raporlayici.hataYakala({ tur: 'js', mesaj: `k-${i}`, stack: `at s${i}` })
    }

    const kuyruk = JSON.parse(ortam2.depo.getItem('raspel_hata_kuyrugu') || '[]')
    expect(kuyruk).toHaveLength(20)
    expect(kuyruk[kuyruk.length - 1].mesaj).toBe('k-23')
  })

  it('kuyruktaki kayıtları gönderir ve temizler', async () => {
    const gonderici = vi.fn().mockRejectedValueOnce(new Error('offline')).mockResolvedValue({})
    const ortam2 = kur({ gonderici })

    await ortam2.raporlayici.hataYakala({ tur: 'js', mesaj: 'bir', stack: 'at a' })
    expect(JSON.parse(ortam2.depo.getItem('raspel_hata_kuyrugu') || '[]')).toHaveLength(1)

    await ortam2.raporlayici.kuyruguBosalt()

    expect(JSON.parse(ortam2.depo.getItem('raspel_hata_kuyrugu') || '[]')).toHaveLength(0)
    expect(gonderici).toHaveBeenCalledTimes(2)
  })

  it('oturum yoksa göndermez, kuyruklar', async () => {
    const ortam2 = kur({ kimlikVarMi: () => false })

    await ortam2.raporlayici.hataYakala({ tur: 'js', mesaj: 'giris yok', stack: 'at b' })

    expect(ortam2.gonderici).not.toHaveBeenCalled()
    expect(JSON.parse(ortam2.depo.getItem('raspel_hata_kuyrugu') || '[]')).toHaveLength(1)
  })

  it('window hata olaylarını dinler ve durdurunca kaldırır', async () => {
    const pencere = sahtePencere()
    const ortam2 = kur({ pencere })

    ortam2.raporlayici.dinleyicileriKur()
    expect(pencere.addEventListener).toHaveBeenCalledWith('error', expect.any(Function))
    expect(pencere.addEventListener).toHaveBeenCalledWith('unhandledrejection', expect.any(Function))

    pencere.dispatch('error', { message: 'olay hatasi', error: { stack: 'at e' } })
    await Promise.resolve()
    expect(ortam2.gonderici).toHaveBeenCalled()

    ortam2.raporlayici.durdur()
    expect(pencere.removeEventListener).toHaveBeenCalledWith('error', expect.any(Function))
  })

  it('mesaj ve stack alanlarını sınırlar', async () => {
    await ortam.raporlayici.hataYakala({
      tur: 'js',
      mesaj: 'm'.repeat(3000),
      stack: 's'.repeat(9000)
    })

    const govde = ortam.gonderici.mock.calls[0][0]
    expect(govde.mesaj.length).toBeLessThanOrEqual(1000)
    expect(govde.stack.length).toBeLessThanOrEqual(4000)
  })
})
