import { describe, it, expect } from 'vitest'
import {
  kdvOrani,
  kalemMiktar,
  kalemNetTutar,
  kalemKdv,
  kalemTutar,
  kalemBrutKdv,
  teklifOzet
} from '../faturaHesapla.js'

describe('faturaHesapla', () => {
  it('kdvOrani bosken varsayilan 20 dondurur', () => {
    expect(kdvOrani({})).toBe(20)
    expect(kdvOrani({ kdvOrani: 0 })).toBe(0)
    expect(kdvOrani({ kdvOrani: 10 })).toBe(10)
  })

  it('kalemMiktar miktar veya adet alanini okur', () => {
    expect(kalemMiktar({ miktar: 3, adet: 5 })).toBe(3)
    expect(kalemMiktar({ adet: 5 })).toBe(5)
    expect(kalemMiktar({})).toBe(1)
  })

  it('kalemNetTutar satir iskontosu sonrasi neti verir', () => {
    const net = kalemNetTutar({ birimFiyat: 1000, miktar: 2, iskontoOrani: 10 })
    expect(net).toBe(1800)
  })

  it('kalemKdv net (matrah) uzerinden hesaplar', () => {
    const kdv = kalemKdv({ birimFiyat: 1000, miktar: 2, iskontoOrani: 10, kdvOrani: 20 })
    expect(kdv).toBe(360)
  })

  it('kalemBrutKdv brüt uzerinden hesaplar (teklif konvensiyonu)', () => {
    const kdv = kalemBrutKdv({ birimFiyat: 10000, miktar: 1, iskontoOrani: 10, kdvOrani: 20 })
    expect(kdv).toBe(2000)
  })

  it('kalemTutar net + kdv icerir', () => {
    const tutar = kalemTutar({ birimFiyat: 100, adet: 1, kdvOrani: 20 })
    expect(tutar).toBeTruthy()
  })

  it('teklifOzet genel iskonto KDVyi etkilemeden 11.000 verir', () => {
    const ozet = teklifOzet(
      [{ birimFiyat: 10000, miktar: 1, iskontoOrani: 0, kdvOrani: 20 }],
      10
    )
    expect(ozet.araToplam).toBe(10000)
    expect(ozet.kdv).toBe(2000)
    expect(ozet.iskontoTutari).toBe(1000)
    expect(ozet.genelToplam).toBe(11000)
  })

  it('teklifOzet satir iskontosu KDVyi etkilemeden 11.000 verir', () => {
    const ozet = teklifOzet(
      [{ birimFiyat: 10000, miktar: 1, iskontoOrani: 10, kdvOrani: 20 }],
      0
    )
    expect(ozet.araToplam).toBe(9000)
    expect(ozet.kdv).toBe(2000)
    expect(ozet.genelToplam).toBe(11000)
  })

  it('teklifOzet bos kalemlerde sifir dondurur', () => {
    const ozet = teklifOzet([], 10)
    expect(ozet.genelToplam).toBe(0)
  })
})