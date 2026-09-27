import { describe, it, expect, beforeEach } from 'vitest'
import { useOfflineSatisKuyrugu } from '../useOfflineSatisKuyrugu.js'

describe('useOfflineSatisKuyrugu', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('ekle kuyruga ekler ve bekleyen sayisini gunceller', () => {
    const { bekleyen, ekle, hepsi } = useOfflineSatisKuyrugu()
    ekle({ tur: 'SATIS', genelToplam: 100 })
    expect(hepsi()).toHaveLength(1)
    expect(bekleyen.value).toBe(1)
  })

  it('kaldir kaydi siler', () => {
    const { ekle, kaldir, hepsi, bekleyen } = useOfflineSatisKuyrugu()
    ekle({ a: 1 })
    kaldir(hepsi()[0].id)
    expect(hepsi()).toHaveLength(0)
    expect(bekleyen.value).toBe(0)
  })

  it('senkronizeEt basarili gonderimleri kuyruktan cikarir', async () => {
    const { ekle, senkronizeEt, hepsi } = useOfflineSatisKuyrugu()
    ekle({ a: 1 })
    ekle({ a: 2 })
    const gonderilen = await senkronizeEt(async () => {})
    expect(gonderilen).toBe(2)
    expect(hepsi()).toHaveLength(0)
  })

  it('senkronizeEt hata olunca durur ve kalanlari korur', async () => {
    const { ekle, senkronizeEt, hepsi } = useOfflineSatisKuyrugu()
    ekle({ a: 1 })
    ekle({ a: 2 })
    let cagri = 0
    const gonderilen = await senkronizeEt(async () => {
      cagri++
      if (cagri === 2) throw new Error('offline')
    })
    expect(gonderilen).toBe(1)
    expect(hepsi()).toHaveLength(1)
  })

  it('bos kuyrukta senkronizeEt 0 doner', async () => {
    const { senkronizeEt } = useOfflineSatisKuyrugu()
    expect(await senkronizeEt(async () => {})).toBe(0)
  })

  it('meta kaydedilir ve senkron sonrasi teslimat callback\'i cagrilir', async () => {
    const { ekle, senkronizeEt, hepsi } = useOfflineSatisKuyrugu()
    const meta = { driverId: 5, teslimatAdresi: 'Adres 1', durum: 'BEKLEMEDE' }
    ekle({ a: 1 }, meta)

    const teslimatCagrilari = []
    const gonderilen = await senkronizeEt(
      async () => ({ data: { id: 42 } }),
      async (m, yanit) => { teslimatCagrilari.push({ m, yanit }) }
    )

    expect(gonderilen).toBe(1)
    expect(hepsi()).toHaveLength(0)
    expect(teslimatCagrilari).toHaveLength(1)
    expect(teslimatCagrilari[0].m).toEqual(meta)
    expect(teslimatCagrilari[0].yanit.data.id).toBe(42)
  })

  it('teslimat callback hatasi satisi kuyrukta birakmaz', async () => {
    const { ekle, senkronizeEt, hepsi } = useOfflineSatisKuyrugu()
    ekle({ a: 1 }, { driverId: 5 })

    const gonderilen = await senkronizeEt(
      async () => ({ data: { id: 42 } }),
      async () => { throw new Error('teslimat hatasi') }
    )

    expect(gonderilen).toBe(1)
    expect(hepsi()).toHaveLength(0)
  })

  it('meta yoksa teslimat callback\'i cagrilmaz', async () => {
    const { ekle, senkronizeEt } = useOfflineSatisKuyrugu()
    ekle({ a: 1 })
    let cagri = 0
    await senkronizeEt(async () => ({ data: { id: 1 } }), async () => { cagri++ })
    expect(cagri).toBe(0)
  })
})
