import { describe, it, expect, beforeEach, vi } from 'vitest'
import { nextTick } from 'vue'
import { defineComponent, h } from 'vue'
import { mount } from '@vue/test-utils'
import { usePosKatalog, VARSAYILAN_SAYFA_BOYUTU } from '../usePosKatalog.js'

/**
 * POS KATALOGU — davranis testleri.
 *
 * `usePosKatalog`, katalogu sunucudan SAYFALI getirir. Once POS'a 200 urun
 * tek seferde cekiliyor, arama/filtreleme BU KADAR urun uzerinde yerel
 * yapiliyordu; 200. urunden sonraki malzemeler POS'ta bulunamiyordu.
 *
 * Burada korunan kurallar:
 *   - Filtre parametreleri sunucuya gider (istemcide filtreleme YOK).
 *   - "Daha fazla" sonraki sayfayi EKLER, listeyi degistirmez.
 *   - YUKLEME / BOS / HATA ayri durumlardir (eslesmemeleri en sik yapilan hata).
 *   - Gecikmeli eski istek yeni sonucu EZMEZ.
 *   - Basarisiz ilk denemede mevcut liste BOSALTILMAZ (kullanici urunlerini
 *     kaybetmemeli).
 */

vi.mock('../../api/index.js', () => ({
  stokAPI: {
    filtreli: vi.fn(),
    gruplamaDagilimi: vi.fn()
  }
}))

// `vi.mock` cagrilari vitest tarafindan import satirlarinin USTUNE hoist
// edildigi icin statik import guvenlidir (dinamik `await import` eslint
// parser'inda "await outside async function" hatasi veriyor).
import { stokAPI } from '../../api/index.js'

/** Sayfa yanitini taklit eder. */
const sayfa = (icerik, totalElements, page = 0) => ({
  data: { content: icerik, totalElements, number: page, size: VARSAYILAN_SAYFA_BOYUTU }
})

const urun = (id, ad = `Urun ${id}`) => ({ id, ad, satisFiyati: 10, miktar: 5, birim: 'adet' })

const kur = () => {
  let sonuc = null
  const Bilesen = defineComponent({
    setup() {
      sonuc = usePosKatalog()
      return () => h('div')
    }
  })
  const wrapper = mount(Bilesen)
  return { ...sonuc, wrapper }
}

beforeEach(() => {
  stokAPI.filtreli.mockReset()
  stokAPI.gruplamaDagilimi.mockReset()
  stokAPI.gruplamaDagilimi.mockResolvedValue({ data: { kategoriler: [], markalar: [], stokGruplari: [] } })
})

describe('usePosKatalog — ilk yukleme', () => {
  it('sayfa parametreleriyle sunucudan ceker', async () => {
    stokAPI.filtreli.mockResolvedValue(sayfa([urun(1), urun(2)], 2))
    const k = kur()
    await k.katalogYukle()
    expect(stokAPI.filtreli).toHaveBeenCalledWith(
      expect.objectContaining({ page: 0, size: VARSAYILAN_SAYFA_BOYUTU })
    )
    expect(k.urunler.value).toHaveLength(2)
  })

  it('bos parametreler sunucuya null olarak gider (filtre yok)', async () => {
    stokAPI.filtreli.mockResolvedValue(sayfa([], 0))
    const k = kur()
    await k.katalogYukle()
    const p = stokAPI.filtreli.mock.calls[0][0]
    expect(p.q).toBeNull()
    expect(p.kategori).toBeNull()
    expect(p.marka).toBeNull()
    expect(p.stokGrubu).toBeNull()
    expect(p.sadeceStokta).toBeNull()
  })

  it('toplam kayit sunucudan gelir (200 tavaninin yerini alir)', async () => {
    stokAPI.filtreli.mockResolvedValue(sayfa([urun(1)], 3451))
    const k = kur()
    await k.katalogYukle()
    expect(k.toplam.value).toBe(3451)
  })

  it('bos sonuc "bos" durumudur, yukleniyor DEGIL', async () => {
    stokAPI.filtreli.mockResolvedValue(sayfa([], 0))
    const k = kur()
    await k.katalogYukle()
    expect(k.yukleniyor.value).toBe(false)
    expect(k.hata.value).toBeNull()
    expect(k.bosMu.value).toBe(true)
  })
})

describe('usePosKatalog — filtreleme sunucu tarafindadir', () => {
  it('arama metni sunucuya gider (istemcide filtreleme YOK)', async () => {
    stokAPI.filtreli.mockResolvedValue(sayfa([urun(1)], 1))
    const k = kur()
    k.aramaMetni.value = '  vida  '
    await nextTick()
    await nextTick()
    expect(stokAPI.filtreli).toHaveBeenCalledWith(expect.objectContaining({ q: 'vida' }))
  })

  it('kategori / marka / stok grubu sunucuya gider', async () => {
    stokAPI.filtreli.mockResolvedValue(sayfa([], 0))
    const k = kur()
    k.kategori.value = 'Yapi Malzemesi'
    k.marka.value = 'MarkaX'
    k.stokGrubu.value = 'Mamul'
    await nextTick()
    await nextTick()
    expect(stokAPI.filtreli).toHaveBeenCalledWith(
      expect.objectContaining({ kategori: 'Yapi Malzemesi', marka: 'MarkaX', stokGrubu: 'Mamul' })
    )
  })

  it('"sadece stokta" true olarak gider (false gonderilmez)', async () => {
    stokAPI.filtreli.mockResolvedValue(sayfa([], 0))
    const k = kur()
    k.sadeceStokta.value = true
    await nextTick()
    await nextTick()
    expect(stokAPI.filtreli).toHaveBeenCalledWith(expect.objectContaining({ sadeceStokta: true }))
  })

  it('filtre degisince sayfa 0"dan yeniden baslar', async () => {
    stokAPI.filtreli.mockResolvedValue(sayfa([urun(1)], 500))
    const k = kur()
    await k.katalogYukle()
    k.kategori.value = 'X'
    await nextTick()
    await nextTick()
    expect(stokAPI.filtreli).toHaveBeenLastCalledWith(expect.objectContaining({ page: 0 }))
    // onceki sayfalar biriktirilmez
    expect(k.urunler.value).toHaveLength(1)
  })

  it('aktif filtre sayisi rozet icin hesaplanir', async () => {
    stokAPI.filtreli.mockResolvedValue(sayfa([], 0))
    const k = kur()
    expect(k.aktifFiltreSayisi.value).toBe(0)
    k.kategori.value = 'A'
    k.sadeceStokta.value = true
    await nextTick()
    await nextTick()
    expect(k.aktifFiltreSayisi.value).toBe(2)
  })

  it('filtreTemizle tum filtreleri sifirlar (arama metni haric)', async () => {
    stokAPI.filtreli.mockResolvedValue(sayfa([], 0))
    const k = kur()
    k.kategori.value = 'A'
    k.marka.value = 'B'
    k.stokGrubu.value = 'C'
    k.sadeceStokta.value = true
    k.filtreTemizle()
    expect(k.kategori.value).toBeNull()
    expect(k.marka.value).toBeNull()
    expect(k.stokGrubu.value).toBeNull()
    expect(k.sadeceStokta.value).toBe(false)
  })
})

describe('usePosKatalog — sayfalama', () => {
  // Sayfa boyutu 60 oldugu icin "birden fazla sayfa" senaryosunda toplam
  // kayit sayisi 60'in uzerinde olmak ZORUNDA. (6 kayit + 60 boyut = tek sayfa;
  // boyle bir test "daha fazla" butonunun kapali olmasini bekler ve yanilir.)
  const sayfa1 = sayfa(
    Array.from({ length: 3 }, (_, i) => urun(i + 1)),
    200,
    0
  )
  const sayfa2 = sayfa(
    Array.from({ length: 3 }, (_, i) => urun(100 + i)),
    200,
    1
  )

  it('daha fazla sonraki sayfayi EKLER (listeyi degistirmez)', async () => {
    stokAPI.filtreli.mockResolvedValueOnce(sayfa1).mockResolvedValueOnce(sayfa2)
    const k = kur()
    await k.katalogYukle()
    expect(k.urunler.value).toHaveLength(3)
    expect(k.dahaFazlaVar.value).toBe(true)

    await k.dahaFazlaYukle()
    expect(k.urunler.value).toHaveLength(6)
    expect(k.urunler.value.map((u) => u.id)).toEqual([1, 2, 3, 100, 101, 102])
    expect(stokAPI.filtreli).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 }))
  })

  it('buton yalnizca daha fazla varken gorunur', async () => {
    stokAPI.filtreli.mockResolvedValue(sayfa([urun(1)], 1))
    const k = kur()
    await k.katalogYukle()
    expect(k.dahaFazlaVar.value).toBe(false)
  })

  it('son sayfada daha fazla butonu kapanir', async () => {
    stokAPI.filtreli.mockResolvedValue(sayfa([urun(1)], 3))
    const k = kur()
    await k.katalogYukle()
    expect(k.dahaFazlaVar.value).toBe(false)
  })

  it('son sayfada "daha fazla" cagrisi yapilmaz', async () => {
    stokAPI.filtreli.mockResolvedValue(sayfa([urun(1)], 1))
    const k = kur()
    await k.katalogYukle()
    const sayi = stokAPI.filtreli.mock.calls.length
    await k.dahaFazlaYukle()
    expect(stokAPI.filtreli).toHaveBeenCalledTimes(sayi)
  })

  it('yukleniyorken "daha fazla" ikinci istek YAPILMAZ', async () => {
    let coz = null
    stokAPI.filtreli
      .mockImplementationOnce(() => sayfa(Array.from({ length: 3 }, (_, i) => urun(i + 1)), 200, 0))
      .mockImplementation(() => new Promise((r) => { coz = r }))

    const k = kur()
    await k.katalogYukle()

    // 1. cagri: gercekten sunucuya gider ve sonucu bekler
    const bekleyen = k.dahaFazlaYukle()
    const ilkSayi = stokAPI.filtreli.mock.calls.length
    expect(ilkSayi).toBe(2)

    // 2. cagri: istek surerken ATILMALI (cift istek / yanlis sayfa)
    await k.dahaFazlaYukle()
    expect(stokAPI.filtreli).toHaveBeenCalledTimes(ilkSayi)

    coz(sayfa([urun(100)], 200, 1))
    await bekleyen
  })
})

describe('usePosKatalog — yükleme / bos / hata ayrimi', () => {
  it('yukleniyor sirasiinda "bos" HIC yazilmaz', async () => {
    let coz = null
    stokAPI.filtreli.mockImplementation(() => new Promise((r) => { coz = r }))
    const k = kur()
    const p = k.katalogYukle()
    // istek sürerken: yükleniyor, boş DEĞİL
    expect(k.yukleniyor.value).toBe(true)
    expect(k.bosMu.value).toBe(false)
    coz(sayfa([], 0))
    await p
    expect(k.yukleniyor.value).toBe(false)
    expect(k.bosMu.value).toBe(true)
  })

  it('hata halinde "bos" yazilmaz, hata durumu gosterilir', async () => {
    stokAPI.filtreli.mockRejectedValue(new Error('Sunucu yok'))
    const k = kur()
    await k.katalogYukle()
    expect(k.hata.value).toBe('Sunucu yok')
    expect(k.bosMu.value).toBe(false)
    expect(k.yukleniyor.value).toBe(false)
  })

  it('hata sonrasi "daha fazla" kapanir', async () => {
    stokAPI.filtreli.mockRejectedValue(new Error('x'))
    const k = kur()
    await k.katalogYukle()
    expect(k.dahaFazlaVar.value).toBe(false)
  })

  it('BASARISIZ ILK DENEMEDE mevcut urunler BOSALTILMAZ', async () => {
    // Kullanici elindeki urunleri ag hatasi yuzunden kaybetmemeli.
    stokAPI.filtreli.mockResolvedValueOnce(sayfa([urun(1), urun(2)], 2))
    const k = kur()
    await k.katalogYukle()
    expect(k.urunler.value).toHaveLength(2)

    stokAPI.filtreli.mockRejectedValueOnce(new Error('Zaman asimi'))
    await k.filtreYenile()
    expect(k.urunler.value).toHaveLength(2)
    expect(k.hata.value).toBe('Zaman asimi')
  })
})

describe('usePosKatalog — yarisma korumasi', () => {
  it('gecikmeli eski istek yeni sonucu EZMEZ', async () => {
    const yavas = sayfa([urun(99)], 1, 0)
    const hizli = sayfa([urun(1)], 1, 0)
    let yavasCoz = null
    stokAPI.filtreli.mockImplementationOnce(() => new Promise((r) => { yavasCoz = r }))
    stokAPI.filtreli.mockResolvedValue(hizli)

    const k = kur()
    const yavasIstek = k.katalogYukle()
    // arama degisti -> yeni istek hemen calisti
    k.aramaMetni.value = 'vida'
    await nextTick()
    await nextTick()

    yavasCoz(yavas)
    await yavasIstek
    await nextTick()

    // sonuc YENI istekten gelmis olmali
    expect(k.urunler.value.map((u) => u.id)).toEqual([1])
  })
})

describe('usePosKatalog — filtre dagilimi (gercek sayilar)', () => {
  it('kategori/marka/stokGrubu dagilimini ayirir', async () => {
    stokAPI.gruplamaDagilimi.mockResolvedValue({
      data: {
        kategoriler: [{ deger: 'Yapi', adet: 320 }],
        markalar: [{ deger: 'ABC', adet: 12 }],
        stokGruplari: [{ deger: 'Mamul', adet: 88 }]
      }
    })
    const k = kur()
    await k.dagilimYukle()
    expect(k.kategoriDagilimi.value).toEqual([{ deger: 'Yapi', adet: 320 }])
    expect(k.markaDagilimi.value).toEqual([{ deger: 'ABC', adet: 12 }])
    expect(k.stokGrubuDagilimi.value).toEqual([{ deger: 'Mamul', adet: 88 }])
  })

  it('dagilim gelmezse katalog calismaya devam eder (secenekler bos)', async () => {
    stokAPI.gruplamaDagilimi.mockRejectedValue(new Error('yok'))
    const k = kur()
    await k.dagilimYukle()
    expect(k.kategoriDagilimi.value).toEqual([])
    expect(k.hata.value).toBeNull() // dagilim hatasi katalog hatasi DEGILDIR
  })

  it('dagilimde null alanlar varsa guvenli sekilde bos kalir', async () => {
    stokAPI.gruplamaDagilimi.mockResolvedValue({ data: {} })
    const k = kur()
    await k.dagilimYukle()
    expect(k.kategoriDagilimi.value).toEqual([])
  })
})

describe('usePosKatalog — sonuc ozeti', () => {
  it('arama/filtre yokken ozet GOSTERILMEZ (her sayfa yeniden sayilir)', async () => {
    stokAPI.filtreli.mockResolvedValue(sayfa([urun(1)], 1))
    const k = kur()
    await k.katalogYukle()
    expect(k.sonucOzeti.value).toBeNull()
  })

  it('arama varken ozet gercek toplami gosterir', async () => {
    stokAPI.filtreli.mockResolvedValue(sayfa([urun(1)], 1))
    const k = kur()
    k.aramaMetni.value = 'vida'
    await nextTick()
    await nextTick()
    expect(k.sonucOzeti.value).toBe(1)
  })

  it('yukleniyorken ozet null (yanlis sayi gosterilmez)', async () => {
    let coz = null
    stokAPI.filtreli.mockImplementation(() => new Promise((r) => { coz = r }))
    const k = kur()
    const p = k.katalogYukle()
    expect(k.sonucOzeti.value).toBeNull()
    coz(sayfa([urun(1)], 1))
    await p
  })
})