import { describe, it, expect } from 'vitest'
import { usePosSepetParklari } from '../usePosSepetParklari.js'

const satir = (id, miktar = 1) => ({ id, ad: `Urun ${id}`, miktar })

describe('usePosSepetParklari', () => {
  it('sepeti park eder ve listeye ekler', () => {
    const p = usePosSepetParklari()
    const kayit = p.parkEt({ sepet: [satir(1)], musteri: { id: 9, ad: 'Ali' } })
    expect(kayit).toBeTruthy()
    expect(p.parklar.value).toHaveLength(1)
    expect(p.parklar.value[0].sepet[0].id).toBe(1)
    expect(p.parklar.value[0].musteri.ad).toBe('Ali')
  })

  it('parklanan sepet KOPYADIR (aktif sepet sonradan degisse de bozulmaz)', () => {
    const p = usePosSepetParklari()
    const aktif = [satir(1)]
    p.parkEt({ sepet: aktif })
    // Aktif sepet park sonrasi degistirilir.
    aktif[0].miktar = 99
    aktif.push(satir(2))
    expect(p.parklar.value[0].sepet).toHaveLength(1)
    expect(p.parklar.value[0].sepet[0].miktar).toBe(1)
  })

  it('park listesi ust sinirini asmaz', () => {
    const p = usePosSepetParklari({ maks: 2 })
    expect(p.parkEt({ sepet: [satir(1)] })).toBeTruthy()
    expect(p.parkEt({ sepet: [satir(2)] })).toBeTruthy()
    expect(p.parkEt({ sepet: [satir(3)] })).toBeNull()
    expect(p.parklar.value).toHaveLength(2)
    expect(p.doluMu.value).toBe(true)
  })

  it('devamEt parki dondurur ve listeden cikarir', () => {
    const p = usePosSepetParklari()
    const kayit = p.parkEt({ sepet: [satir(1)] })
    const geri = p.devamEt(kayit.id)
    expect(geri.sepet[0].id).toBe(1)
    expect(p.parklar.value).toHaveLength(0)
  })

  it('olmayan id ile devamEt null doner', () => {
    const p = usePosSepetParklari()
    expect(p.devamEt('yok')).toBeNull()
  })

  it('sil ve temizle', () => {
    const p = usePosSepetParklari()
    const a = p.parkEt({ sepet: [satir(1)] })
    p.parkEt({ sepet: [satir(2)] })
    p.sil(a.id)
    expect(p.parklar.value).toHaveLength(1)
    expect(p.parklar.value[0].sepet[0].id).toBe(2)
    p.temizle()
    expect(p.parklar.value).toHaveLength(0)
  })

  it('bos sepet de park edilebilir (kaza ile temizlige karsi)', () => {
    const p = usePosSepetParklari()
    const kayit = p.parkEt({})
    expect(kayit.sepet).toEqual([])
    expect(kayit.musteri).toBeNull()
  })
})
