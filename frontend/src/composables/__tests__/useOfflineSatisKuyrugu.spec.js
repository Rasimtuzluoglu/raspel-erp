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
})
