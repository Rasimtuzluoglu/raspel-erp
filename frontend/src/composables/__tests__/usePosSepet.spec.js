import { describe, it, expect, vi } from 'vitest'
import { ref, nextTick } from 'vue'
import { usePosSepet } from '../usePosSepet.js'

/**
 * POS SEPET CEKIRDEGI — davranis testleri.
 *
 * Bu dosya sepet mantiginin ASIL denetim yeridir. Once bu mantik
 * `HizliSatis.vue` icinde 400+ satirdi ve yalniz view render testleriyle
 * dolayli olarak gorulebiliyordu; artik dogrudan, view'siz test edilebilir.
 *
 * Korunan kurallar:
 *   - Satir benzersizligi (stokId + fiyatTipi); karttan ekleme ayni satira birikir.
 *   - Stok tavani (depoda fazlasi satilmaz) ve ondalik adim.
 *   - Geri alma: 8 sn'lik SATIR penceresi, sepetinkinden ONCE gelir.
 *   - "Cogalt" ayni fiyat tipini tekrarlamaz.
 *   - Aktif satir index'i silme/siralama sonrasi kaymaz.
 */

const urun = (ek = {}) => ({
  id: 1,
  ad: 'Vida M8',
  stokKodu: 'V-1',
  barkod: '8690000000001',
  satisFiyati: 100,
  fiyat: 100,
  miktar: 10,
  birim: 'adet',
  kdvOrani: 20,
  birimHacim: 1,
  agirlik: 0.1,
  ...ek
})

const kur = (ek = {}) => {
  const bildir = { uyari: vi.fn(), basarili: vi.fn(), hata: vi.fn() }
  const seciliMusteri = ref(null)
  const urunFiyatlariniYukleTek = vi.fn().mockResolvedValue([])
  const cariUrunFiyatGecmisi = vi.fn().mockResolvedValue({ data: null })
  const sepet = usePosSepet({
    t: (k, p) => (p ? `${k}:${JSON.stringify(p)}` : k),
    bildir,
    seciliMusteri,
    urunFiyatlariniYukleTek,
    cariUrunFiyatGecmisi,
    ...ek
  })
  return { ...sepet, bildir, seciliMusteri, urunFiyatlariniYukleTek, cariUrunFiyatGecmisi }
}

describe('usePosSepet — sepete ekleme ve satir benzersizligi', () => {
  it('yeni urunu tek satir olarak ekler', async () => {
    const s = kur()
    await s.sepeteEkle(urun())
    expect(s.sepet.value).toHaveLength(1)
    expect(s.sepet.value[0].miktar).toBe(1)
    expect(s.sepet.value[0].stokMiktari).toBe(10)
  })

  it('ayni urunu tekrar eklemek AYNI satira biriktirir (duplicate acmaz)', async () => {
    const s = kur()
    await s.sepeteEkle(urun())
    await s.sepeteEkle(urun())
    expect(s.sepet.value).toHaveLength(1)
    expect(s.sepet.value[0].miktar).toBe(2)
  })

  it('adet parametresi ile miktarli ekler', async () => {
    const s = kur()
    await s.sepeteEkle(urun(), 5)
    expect(s.sepet.value[0].miktar).toBe(5)
  })

  it('stokta olmayan urun sepete GIRMEZ (uyari verir)', async () => {
    const s = kur()
    await s.sepeteEkle(urun({ miktar: 0 }))
    expect(s.sepet.value).toHaveLength(0)
    expect(s.bildir.uyari).toHaveBeenCalled()
  })

  it('stoktan fazla ekleme tavana kirpilir ve uyarilir', async () => {
    const s = kur()
    await s.sepeteEkle(urun({ miktar: 3 }), 500)
    expect(s.sepet.value[0].miktar).toBe(3)
    expect(s.bildir.uyari).toHaveBeenCalled()
  })

  it('var olan satirin ustune eklemede tavan TOPLAM uzerinden uygulanir', async () => {
    const s = kur()
    await s.sepeteEkle(urun({ miktar: 3 }), 2) // 2 ekle
    await s.sepeteEkle(urun({ miktar: 3 }), 2) // +2 -> tavan 3
    expect(s.sepet.value[0].miktar).toBe(3)
  })

  it('stoksuz urunde EN BUYUK deger tavana uyar (0 stok -> min)', async () => {
    const s = kur()
    // stok 0, ama stokYok kontrolu zaten engeller; min davranisini olcmek icin
    // stok 0.5 olan kg urunu kullaniyoruz.
    await s.sepeteEkle(urun({ miktar: 0.5, birim: 'kg' }), 5)
    expect(s.sepet.value[0].miktar).toBe(0.5)
  })

  it('fiyat tipi yoksa varsayilan fiyat listesi uretir (perakende/toptan/ozel)', async () => {
    const s = kur()
    await s.sepeteEkle(urun({ satisFiyati: 100 }))
    const satir = s.sepet.value[0]
    expect(satir.fiyatlar).toHaveLength(3)
    expect(satir.fiyatlar[0].fiyat).toBe(100)
    expect(satir.fiyatlar[1].fiyat).toBe(90)
    expect(satir.fiyatlar[2].fiyat).toBe(80)
  })

  it('KDV orani ve birim hacim/agirlik satira tasinir', async () => {
    const s = kur()
    await s.sepeteEkle(urun({ kdvOrani: 10, birimHacim: 2, agirlik: 1.5 }))
    const satir = s.sepet.value[0]
    expect(satir.kdvOrani).toBe(10)
    expect(satir.birimHacim).toBe(2)
    expect(satir.agirlik).toBe(1.5)
  })

  it('cevrimdisi/eksik fiyat listesi zenginlestirmesi hataya yol acmaz', async () => {
    const s = kur()
    s.urunFiyatlariniYukleTek.mockRejectedValue(new Error('ag yok'))
    await expect(s.sepeteEkle(urun())).resolves.toBeUndefined()
    expect(s.sepet.value).toHaveLength(1)
  })
})

describe('usePosSepet — farkli fiyat tipleri AYRI satir', () => {
  it('cogaltma alternatif fiyat tipiyle yeni satir acar', async () => {
    const s = kur()
    await s.sepeteEkle(urun())
    expect(s.satiriCogalt(0)).toBe(true)
    expect(s.sepet.value).toHaveLength(2)
    expect(s.sepet.value[1].fiyatTipi).toBe('hizliSatis.fiyatToptan')
    expect(s.sepet.value[1].fiyat).toBe(90)
  })

  it('kullanilabilir alternatif fiyat tipi yoksa cogaltmaz ve uyarir', async () => {
    const s = kur()
    // Tek fiyatli urun: alternatif yok
    await s.sepeteEkle(urun({ fiyatlar: [{ ad: 'Tek', fiyat: 50 }] }))
    expect(s.satiriCogalt(0)).toBe(false)
    expect(s.sepet.value).toHaveLength(1)
    expect(s.bildir.uyari).toHaveBeenCalled()
  })

  it('kullanilan fiyat tipleri cogaltmada tekrarlanmaz', async () => {
    const s = kur()
    await s.sepeteEkle(urun())
    s.satiriCogalt(0) // Toptan
    s.satiriCogalt(0) // Ozel (sirada)
    const tipler = s.sepet.value.map((i) => i.fiyatTipi)
    expect(new Set(tipler).size).toBe(tipler.length)
  })

  it('karttan ekleme AYNI (id+fiyatTipi) satirina birikir, oteki satira degil', async () => {
    const s = kur()
    await s.sepeteEkle(urun()) // Perakende
    s.satiriCogalt(0) // Toptan satiri
    await s.sepeteEkle(urun()) // varsayilan = Perakende -> ILK satira
    expect(s.sepet.value[0].miktar).toBe(2) // Perakende
    expect(s.sepet.value[1].miktar).toBe(1) // Toptan
  })
})

describe('usePosSepet — adet degistirme (tek giris noktasi)', () => {
  it('gecerli degeri uygular', async () => {
    const s = kur()
    await s.sepeteEkle(urun())
    const r = s.miktarDegistir(0, 5)
    expect(r.uygulandi).toBe(true)
    expect(s.sepet.value[0].miktar).toBe(5)
  })

  it('negatif/sifir degeri en kucuk adede indirir ve uyarir', async () => {
    const s = kur()
    await s.sepeteEkle(urun())
    s.miktarDegistir(0, -5)
    expect(s.sepet.value[0].miktar).toBe(1)
    expect(s.bildir.uyari).toHaveBeenCalled()
  })

  it('kg biriminde 0.5 adim kabul eder', async () => {
    const s = kur()
    await s.sepeteEkle(urun({ birim: 'kg', miktar: 10 }))
    s.miktarDegistir(0, 2.5)
    expect(s.sepet.value[0].miktar).toBe(2.5)
  })

  it('stok tavanini asmaz ve uyarir', async () => {
    const s = kur()
    await s.sepeteEkle(urun({ miktar: 4 }))
    s.miktarDegistir(0, 99)
    expect(s.sepet.value[0].miktar).toBe(4)
    expect(s.bildir.uyari).toHaveBeenCalled()
  })

  it('adim modunda bir adim artirir (stok tavanina saygi ile)', async () => {
    const s = kur()
    await s.sepeteEkle(urun({ miktar: 3 }))
    s.miktarDegistir(0, 1, 'adim')
    expect(s.sepet.value[0].miktar).toBe(2)
  })

  it('en kucuk adette azaltma DEGISTIRMEZ (satir silinmez)', async () => {
    const s = kur()
    await s.sepeteEkle(urun())
    const r = s.miktarAzalt(0)
    expect(r.uygulandi).toBe(false)
    expect(s.sepet.value).toHaveLength(1)
    expect(s.sepet.value[0].miktar).toBe(1)
  })

  it('adet degisikligi geri alinabilir (kayit yazilir)', async () => {
    const s = kur()
    await s.sepeteEkle(urun())
    s.miktarDegistir(0, 7)
    expect(s.geriAlSatir.value).toBeTruthy()
    s.geriAlYap()
    expect(s.sepet.value[0].miktar).toBe(1)
  })

  it('adedi sifirla en kucuk adede indirir', async () => {
    const s = kur()
    await s.sepeteEkle(urun(), 5)
    s.adediSifirla(0)
    expect(s.sepet.value[0].miktar).toBe(1)
  })

  it('olmayan satirda guvenli sekilde doner', () => {
    const s = kur()
    expect(s.miktarDegistir(99, 5).uygulandi).toBe(false)
  })
})

describe('usePosSepet — silme ve cogaltma', () => {
  it('satiri siler ve geri alinabilir birakir', async () => {
    const s = kur()
    await s.sepeteEkle(urun())
    s.sepetSil(0)
    expect(s.sepet.value).toHaveLength(0)
    s.geriAlYap()
    expect(s.sepet.value).toHaveLength(1)
  })

  it('silme sonrasi aktif satir kaymaz (index duzeltilir)', async () => {
    const s = kur()
    await s.sepeteEkle(urun({ id: 1 }))
    await s.sepeteEkle(urun({ id: 2 }))
    await s.sepeteEkle(urun({ id: 3 }))
    s.aktifSatir.value = 2
    s.sepetSil(0)
    expect(s.aktifSatir.value).toBe(1)
  })

  it('son satir silinince aktif satir -1 olur', async () => {
    const s = kur()
    await s.sepeteEkle(urun())
    s.aktifSatir.value = 0
    s.sepetSil(0)
    expect(s.aktifSatir.value).toBe(-1)
  })

  it('aktif satiri cogaltir ve aktif satiri kopyaya tasir', async () => {
    const s = kur()
    await s.sepeteEkle(urun())
    s.aktifSatir.value = 0
    s.aktifSatiriCogalt()
    expect(s.sepet.value).toHaveLength(2)
    expect(s.aktifSatir.value).toBe(1)
  })

  it('cogaltma geri alindiginda KOPYA kaldirilir (yeniden eklenmez)', async () => {
    const s = kur()
    await s.sepeteEkle(urun())
    s.satiriCogalt(0)
    expect(s.sepet.value).toHaveLength(2)
    s.geriAlYap()
    expect(s.sepet.value).toHaveLength(1)
    expect(s.sepet.value[0].id).toBe(1)
  })
})

describe('usePosSepet — geri alma onceligi', () => {
  it('SATIR penceresi sepetinkinden ONCE gelir', async () => {
    const s = kur()
    await s.sepeteEkle(urun())
    // Once sepetin tamamini temizle (sepet geri alma penceresi acilir)
    s.sepetiGeriAlinabilirTemizle()
    expect(s.geriAlSepet.value).toBeTruthy()
    expect(s.sepet.value).toHaveLength(0)
    // Sonra bir satir ekleyip sil -> SATIR penceresi daha yeni
    await s.sepeteEkle(urun())
    s.sepetSil(0)
    s.geriAlYap()
    // Satir geri gelmeli (sepetin tamami degil)
    expect(s.sepet.value).toHaveLength(1)
  })

  it('geri alinacak bir sey yoksa false doner', () => {
    const s = kur()
    expect(s.geriAlYap()).toBe(false)
  })

  it('sepetin tamami geri alinabilir', async () => {
    const s = kur()
    await s.sepeteEkle(urun({ id: 1 }))
    await s.sepeteEkle(urun({ id: 2, ad: 'Somun' }))
    s.sepetiGeriAlinabilirTemizle()
    expect(s.sepet.value).toHaveLength(0)
    s.geriAlYap()
    expect(s.sepet.value).toHaveLength(2)
  })

  it('sepetiSifirla geri alma penceresini KAPATIR (kalici temizlik)', async () => {
    const s = kur()
    await s.sepeteEkle(urun())
    s.sepetiSifirla()
    expect(s.sepet.value).toHaveLength(0)
    expect(s.geriAlSepet.value).toBeNull()
    expect(s.geriAlYap()).toBe(false)
  })
})

describe('usePosSepet — toplamlar', () => {
  it('toplam = miktar * fiyat', async () => {
    const s = kur()
    await s.sepeteEkle(urun({ satisFiyati: 100 }), 3)
    expect(s.toplam.value).toBe(300)
  })

  it('ft3 ve agirlik hesaplanir', async () => {
    const s = kur()
    await s.sepeteEkle(urun({ birimHacim: 2, agirlik: 1.5 }), 3)
    expect(s.toplamFt3.value).toBe(6)
    expect(s.toplamAgirlik.value).toBe(4.5)
    expect(s.agirlikVarMi.value).toBe(true)
  })

  it('agirlik yoksa agirlikVarMi false', async () => {
    const s = kur()
    await s.sepeteEkle(urun({ agirlik: 0 }))
    expect(s.agirlikVarMi.value).toBe(false)
  })
})

describe('usePosSepet — kart rozeti adedi', () => {
  it('ayni urunun TUM satirlarini toplar', async () => {
    const s = kur()
    await s.sepeteEkle(urun(), 2)
    s.satiriCogalt(0)
    s.miktarDegistir(1, 3)
    expect(s.sepetteAdet(1)).toBe(5)
  })
})

describe('usePosSepet — surukle-birak siralama', () => {
  it('satiri hedef konuma tasir', async () => {
    const s = kur()
    await s.sepeteEkle(urun({ id: 1 }))
    await s.sepeteEkle(urun({ id: 2 }))
    await s.sepeteEkle(urun({ id: 3 }))
    s.suruklemeBasla(0)
    s.suruklemeBirak(2)
    expect(s.sepet.value.map((i) => i.id)).toEqual([2, 3, 1])
    expect(s.aktifSatir.value).toBe(2)
  })

  it('ayni konuma birakmak hicbir sey yapmaz', async () => {
    const s = kur()
    await s.sepeteEkle(urun({ id: 1 }))
    s.suruklemeBasla(0)
    s.suruklemeBirak(0)
    expect(s.sepet.value).toHaveLength(1)
  })

  it('suruklemeBitir durumu temizler', async () => {
    const s = kur()
    await s.sepeteEkle(urun())
    s.suruklemeBasla(0)
    s.suruklemeUzerine(0)
    s.suruklemeBitir()
    expect(s.suruklenenIdx.value).toBeNull()
    expect(s.suruklenenUzerinde.value).toBeNull()
  })
})

describe('usePosSepet — aktif satir sarmalayici', () => {
  it('islemi calistirmadan once aktif satiri hizalar', async () => {
    const s = kur()
    await s.sepeteEkle(urun({ id: 1 }))
    await s.sepeteEkle(urun({ id: 2 }))
    const fn = vi.fn()
    s.satirAktifYap(fn)(1, 'ek')
    expect(s.aktifSatir.value).toBe(1)
    expect(fn).toHaveBeenCalledWith(1, 'ek')
  })
})

describe('usePosSepet — cari fiyat gecmisi', () => {
  it('musteri fiyat gecmisi varsa satira islenir', async () => {
    const s = kur()
    s.cariUrunFiyatGecmisi.mockResolvedValue({
      data: { sonFiyat: 80, gecmis: [{ tarih: '2026-01-01' }] }
    })
    s.seciliMusteri.value = { id: 5, ad: 'ABC' }
    await s.sepeteEkle(urun())
    const satir = s.sepet.value[0]
    expect(satir.sonAldigiFiyat).toBe(80)
    expect(satir.sonAldigiTarih).toBe('2026-01-01')
    expect(satir.fiyat).toBe(80)
  })

  it('musteri secilince tum satirlara cari fiyati uygulanir', async () => {
    const s = kur()
    await s.sepeteEkle(urun())
    s.cariUrunFiyatGecmisi.mockResolvedValue({ data: { sonFiyat: 70, gecmis: [] } })
    s.seciliMusteri.value = { id: 5 }
    await s.sepeteCariFiyatUygula()
    expect(s.sepet.value[0].fiyat).toBe(70)
  })

  it('cari fiyat gecmisi alinamazsa temel fiyatla devam eder', async () => {
    const s = kur()
    s.cariUrunFiyatGecmisi.mockRejectedValue(new Error('yok'))
    s.seciliMusteri.value = { id: 5 }
    await s.sepeteEkle(urun({ satisFiyati: 100 }))
    expect(s.sepet.value[0].fiyat).toBe(100)
  })
})

describe('usePosSepet — fiyat tipi degisimi', () => {
  it('fiyat tipi degisince birim fiyat guncellenir', async () => {
    const s = kur()
    await s.sepeteEkle(urun())
    const satir = s.sepet.value[0]
    satir.fiyatTipi = 'hizliSatis.fiyatOzel'
    s.fiyatTipiDegisti(satir)
    expect(satir.fiyat).toBe(80)
  })
})

describe('usePosSepet — dayaniklilik', () => {
  it('bos sepette toplamlar 0', () => {
    const s = kur()
    expect(s.toplam.value).toBe(0)
    expect(s.toplamFt3.value).toBe(0)
    expect(s.toplamAgirlik.value).toBe(0)
    expect(s.agirlikVarMi.value).toBe(false)
  })

  it('sepetteAdet olmayan urunde 0', () => {
    const s = kur()
    expect(s.sepetteAdet(99)).toBe(0)
  })

  it('olmayan satiri silmek/cogaltmak cokmez', () => {
    const s = kur()
    expect(() => s.sepetSil(5)).not.toThrow()
    expect(s.satiriCogalt(5)).toBe(false)
  })

  it('karttan ekleme sonrasi satir VURGULANIR (gorsel geri bildirim)', async () => {
    const s = kur()
    await s.sepeteEkle(urun())
    expect(s.vurguluId.value).toBe(1)
    await nextTick()
  })
})