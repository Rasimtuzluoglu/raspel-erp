import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

describe('useYedekKlasoru', () => {
  beforeEach(() => {
    vi.resetModules()
  })
  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('API yoksa desteklenmiyor ve klasorSec hata verir', async () => {
    vi.stubGlobal('showDirectoryPicker', undefined)
    const mod = await import('../useYedekKlasoru.js')
    expect(mod.klasorSecimiDestekleniyor).toBe(false)
    const { klasorSec } = mod.useYedekKlasoru()
    await expect(klasorSec()).rejects.toThrow('DESTEKLENMIYOR')
  })

  it('klasor secince adi kaydeder ve dosyayi yazar', async () => {
    const yazilan = []
    const dosyaTutamac = {
      createWritable: async () => ({
        write: async (b) => yazilan.push(b),
        close: async () => {}
      })
    }
    const handle = {
      name: 'Yedeklerim',
      queryPermission: async () => 'granted',
      requestPermission: async () => 'granted',
      getFileHandle: async () => dosyaTutamac
    }
    vi.stubGlobal('showDirectoryPicker', vi.fn(async () => handle))
    const mod = await import('../useYedekKlasoru.js')
    expect(mod.klasorSecimiDestekleniyor).toBe(true)
    const { klasorSec, klasorAdi, yaz } = mod.useYedekKlasoru()
    await klasorSec()
    expect(klasorAdi.value).toBe('Yedeklerim')
    await yaz('yedek.sql.gz', new Blob(['veri']))
    expect(yazilan.length).toBe(1)
  })

  it('izin yoksa yazma IZIN_YOK hatasi verir', async () => {
    const handle = {
      name: 'X',
      queryPermission: async () => 'denied',
      requestPermission: async () => 'denied',
      getFileHandle: async () => ({})
    }
    vi.stubGlobal('showDirectoryPicker', vi.fn(async () => handle))
    const mod = await import('../useYedekKlasoru.js')
    const { klasorSec, yaz } = mod.useYedekKlasoru()
    await klasorSec()
    await expect(yaz('a', new Blob(['x']))).rejects.toThrow('IZIN_YOK')
  })

  it('klasor secilmeden yazma KLASOR_YOK hatasi verir', async () => {
    vi.stubGlobal('showDirectoryPicker', vi.fn(async () => ({ name: 'Y' })))
    const mod = await import('../useYedekKlasoru.js')
    const { yaz } = mod.useYedekKlasoru()
    await expect(yaz('a', new Blob(['x']))).rejects.toThrow('KLASOR_YOK')
  })
})
