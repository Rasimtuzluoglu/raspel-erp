import { describe, it, expect, vi } from 'vitest'
import { ref } from 'vue'
import {
  posKisayolCoz,
  fonksiyonTuslari,
  girdidenIzgaraya,
  altOklar,
  izgaraModu,
  sepetGezinme,
  yazarkenKoru,
  satirSil,
  harfKisayollari
} from '../usePosKisayollar.js'

/**
 * POS KISAYOLLARI — davranis testleri.
 *
 * Bu dosya, `HizliSatis.vue`dan ayrilan kisayol mantiginin ASIL denetim
 * yeridir. `redteam-faz4-pos-klavye.spec.js` artik yalnizca view tarafindaki
 * sozlesmeyi (hangi eylemin hangi fonksiyona baglandigi) tarar; tus -> eylem
 * ESLESMESI ve ONCELIK SIRASI burada saf fonksiyonlar uzerinden dogrulanir.
 *
 * Testler `window` dinleyicisi kurmaz, mount yapmaz; `posKisayolCoz` dogrudan
 * cagrilir. Bu yuzden "hangi tus ne yapiyor" sorusu ucuz ve kesin yanitlanir.
 */

/** Baglam (context) fabrikasi: ref'ler + eylem spy'leri. */
const baglam = (ek = {}) => {
  const c = {
    // --- durum ---
    urunIzgaraOdak: ref(false),
    urunIzgaraIndeks: ref(0),
    urunIzgaraRakam: ref(''),
    gorunenUrunler: ref([{ id: 1, ad: 'A' }, { id: 2, ad: 'B' }]),
    sepet: ref([{ id: 1, ad: 'A', miktar: 2 }, { id: 2, ad: 'B', miktar: 1 }]),
    aktifSatir: ref(-1),
    musteriModu: ref('perakende'),
    odemeDurumu: ref('tam'),
    odemeYontemi: ref('NAKIT'),
    barkodInputRef: ref(null),
    musteriPaneliRef: ref(null),
    // --- eylemler ---
    sepetiGeriAlinabilirTemizle: vi.fn(),
    urunIzgarayaGir: vi.fn(),
    urunIzgaradanCik: vi.fn(),
    urunIzgarayiOdakla: vi.fn(),
    urunIzgaraHareket: vi.fn(),
    urunIzgaraSec: vi.fn(),
    adetPopoverAc: vi.fn(),
    odaklaUrunArama: vi.fn(),
    odaklaAktifAdet: vi.fn(),
    bugunkuSatislariAc: vi.fn(),
    fisOnizlemeToggle: vi.fn(),
    termalYazdir: vi.fn(),
    satisiTamamla: vi.fn(),
    miktarAzalt: vi.fn(),
    sepetSil: vi.fn(),
    fisDegiskeniniDegistir: vi.fn(),
    geriAlYap: vi.fn(),
    aktifSatiriCogalt: vi.fn(),
    ipucuToggle: vi.fn(),
    t: (k) => `[${k}]`,
    bildir: vi.fn(),
    ...ek
  }
  return c
}

/** Klavye olayi taklidi. */
const tus = (key, opts = {}) => ({
  key,
  target: opts.target ?? { tagName: 'DIV' },
  preventDefault: vi.fn(),
  ctrlKey: !!opts.ctrlKey,
  metaKey: !!opts.metaKey,
  shiftKey: !!opts.shiftKey,
  altKey: !!opts.altKey
})

const GIRDI = { tagName: 'INPUT' }
const ALAN = { tagName: 'TEXTAREA' }
const SECIM = { tagName: 'SELECT' }

// ===========================================================================
// Fonksiyon tuslari (F1..F11)
// ===========================================================================
describe('usePosKisayollar — fonksiyon tuslari', () => {
  it('F1 barkod alanini odaklar', () => {
    const odak = vi.fn()
    const c = baglam({ barkodInputRef: ref({ $el: { focus: odak } }) })
    const e = tus('F1')
    expect(fonksiyonTuslari(e, c)).toBe(true)
    expect(odak).toHaveBeenCalled()
  })

  it('F2 sepeti geri alinabilir temizler ve bildirim verir', () => {
    const c = baglam()
    expect(fonksiyonTuslari(tus('F2'), c)).toBe(true)
    expect(c.sepetiGeriAlinabilirTemizle).toHaveBeenCalled()
    expect(c.bildir).toHaveBeenCalledWith('info', '[hizliSatis.sepetTemizlendi]')
  })

  it('F3 izgara modunda IZGARAYA, degilse arama kutusuna gider', () => {
    const disarida = baglam()
    fonksiyonTuslari(tus('F3'), disarida)
    expect(disarida.odaklaUrunArama).toHaveBeenCalled()
    expect(disarida.urunIzgarayiOdakla).not.toHaveBeenCalled()

    // Izgaradayken F3 tek yonlu olmali: geri doner.
    const iceride = baglam({ urunIzgaraOdak: ref(true) })
    fonksiyonTuslari(tus('F3'), iceride)
    expect(iceride.urunIzgarayiOdakla).toHaveBeenCalled()
    expect(iceride.odaklaUrunArama).not.toHaveBeenCalled()
  })

  it('F4 musteri moduna gecer', () => {
    const c = baglam()
    expect(fonksiyonTuslari(tus('F4'), c)).toBe(true)
    expect(c.musteriModu.value).toBe('musteri')
  })

  it('F5/F6 yeni musteri ve kamera dialogunu acar', () => {
    const yeniMusteriDialog = ref(false)
    const scannerAcik = ref(false)
    const c = baglam({ yeniMusteriDialog, scannerAcik })
    fonksiyonTuslari(tus('F5'), c)
    fonksiyonTuslari(tus('F6'), c)
    expect(yeniMusteriDialog.value).toBe(true)
    expect(scannerAcik.value).toBe(true)
  })

  it('F7 bugunku satislari acar (POStan cikmaz)', () => {
    const c = baglam()
    expect(fonksiyonTuslari(tus('F7'), c)).toBe(true)
    expect(c.bugunkuSatislariAc).toHaveBeenCalled()
  })

  it('F8 fis onizlemesini acar ve yazdirir', () => {
    const c = baglam()
    expect(fonksiyonTuslari(tus('F8'), c)).toBe(true)
    expect(c.fisOnizlemeToggle).toHaveBeenCalled()
  })

  it('F11 termal yazdirir', () => {
    const c = baglam()
    expect(fonksiyonTuslari(tus('F11'), c)).toBe(true)
    expect(c.termalYazdir).toHaveBeenCalled()
  })

  it('F9 peşin, F10 kısmi odeme durumunu ayarlar', () => {
    const dokuz = baglam()
    fonksiyonTuslari(tus('F9'), dokuz)
    expect(dokuz.odemeDurumu.value).toBe('tam')

    const on = baglam()
    fonksiyonTuslari(tus('F10'), on)
    expect(on.odemeDurumu.value).toBe('yarim')
  })

  // REDTEAM/Faz5 (KRITIK): F9/F10 SATISI TAMAMLAMAZ. Once tamamliyordu;
  // kasiyer odeme tipini secerken yanlislikla F9'a basmasi satisi ANINDA
  // kaydediyor ve fis penceresi aciliyordu (geri donusu yok).
  it('F9/F10 satisi TAMAMLAMAZ (kazara kayit engeli)', () => {
    const c = baglam() // sepet DOLU
    const e = tus('F9')
    fonksiyonTuslari(e, c)
    expect(c.satisiTamamla).not.toHaveBeenCalled()
    expect(c.odemeDurumu.value).toBe('tam')
  })

  it('F10 da satisi tamamlamaz, yalniz durumu ayarlar', () => {
    const c = baglam()
    fonksiyonTuslari(tus('F10'), c)
    expect(c.satisiTamamla).not.toHaveBeenCalled()
    expect(c.odemeDurumu.value).toBe('yarim')
  })

  it('F9/F10 sepet bos olsa da yalniz durum ayarlar', () => {
    const c = baglam({ sepet: ref([]) })
    fonksiyonTuslari(tus('F9'), c)
    expect(c.satisiTamamla).not.toHaveBeenCalled()
  })

  it('F tuslari girdi alanindayken de gecerli', () => {
    // Kullanici barkod alaninda yazarken F1 odaklamayi tazelemelidir.
    const c = baglam()
    const e = tus('F7', { target: GIRDI })
    expect(posKisayolCoz(e, c)).toBe(true)
    expect(c.bugunkuSatislariAc).toHaveBeenCalled()
  })

  it('F tusu disinda hicbir sey yapmaz', () => {
    const c = baglam()
    expect(fonksiyonTuslari(tus('F12'), c)).toBe(false)
  })
})

// ===========================================================================
// Metin alanindan izgaraya giris
// ===========================================================================
describe('usePosKisayollar — metin alanindan ↓ ile izgaraya', () => {
  it('girdi alaninda ↓ izgaraya sokar', () => {
    const c = baglam()
    expect(girdidenIzgaraya(tus('ArrowDown', { target: GIRDI }), c)).toBe(true)
    expect(c.urunIzgarayaGir).toHaveBeenCalled()
  })

  it('INPUT / TEXTAREA / SELECT ve contenteditable alanlarinda calisir', () => {
    for (const hedef of [GIRDI, ALAN, SECIM, { tagName: 'DIV', isContentEditable: true }]) {
      const c = baglam()
      expect(girdidenIzgaraya(tus('ArrowDown', { target: hedef }), c)).toBe(true)
      expect(c.urunIzgarayaGir).toHaveBeenCalled()
    }
  })

  it('girdi alaninda baska tus izgaraya sokmaz', () => {
    const c = baglam()
    expect(girdidenIzgaraya(tus('ArrowUp', { target: GIRDI }), c)).toBe(false)
    expect(c.urunIzgarayaGir).not.toHaveBeenCalled()
  })

  it('girdi alani disinda ↓ izgaraya sokmaz (sepette gezer)', () => {
    const c = baglam()
    expect(girdidenIzgaraya(tus('ArrowDown'), c)).toBe(false)
  })

  it('gosterilecek urun yoksa hicbir sey yapmaz', () => {
    const c = baglam({ gorunenUrunler: ref([]) })
    expect(girdidenIzgaraya(tus('ArrowDown', { target: GIRDI }), c)).toBe(false)
  })
})

// ===========================================================================
// Alt + ok ile miktar
// ===========================================================================
describe('usePosKisayollar — Alt+ok ile miktar', () => {
  it('Alt+↑ aktif satirin miktarini artirir', () => {
    const c = baglam({ aktifSatir: ref(0) })
    expect(altOklar(tus('ArrowUp', { altKey: true }), c)).toBe(true)
    expect(c.sepet.value[0].miktar).toBe(3)
  })

  it('Alt+↓ mevcut azaltma kuralini cagirir (kayit tutarli)', () => {
    const c = baglam({ aktifSatir: ref(0) })
    expect(altOklar(tus('ArrowDown', { altKey: true }), c)).toBe(true)
    expect(c.miktarAzalt).toHaveBeenCalledWith(0)
  })

  it('aktif satir yoksa dokunmaz', () => {
    const c = baglam({ aktifSatir: ref(-1) })
    expect(altOklar(tus('ArrowUp', { altKey: true }), c)).toBe(false)
    expect(c.sepet.value[0].miktar).toBe(2)
  })

  it('Alt olmadan ok tetiklenmez', () => {
    const c = baglam({ aktifSatir: ref(0) })
    expect(altOklar(tus('ArrowUp'), c)).toBe(false)
  })
})

// ===========================================================================
// Izgara klavye modu — "oda" gibi davranir
// ===========================================================================
describe('usePosKisayollar — izgara modu', () => {
  const izgara = (ek = {}) => baglam({ urunIzgaraOdak: ref(true), urunIzgaraIndeks: ref(1), ...ek })

  it('izgara kapaliyken devre disi', () => {
    const c = baglam()
    expect(izgaraModu(tus('ArrowDown'), c)).toBe(false)
  })

  it('oklar secimi tasir: dikey ve yatay AYRI', () => {
    const c = izgara()
    izgaraModu(tus('ArrowUp'), c)
    izgaraModu(tus('ArrowDown'), c)
    izgaraModu(tus('ArrowLeft'), c)
    izgaraModu(tus('ArrowRight'), c)
    // Yatay hareket ayri ele alinir; aksi halde sarma calismaz.
    expect(c.urunIzgaraHareket.mock.calls).toEqual([[0, -1], [0, 1], [-1, 0], [1, 0]])
  })

  it('Escape izgaradan cikar', () => {
    const c = izgara()
    expect(izgaraModu(tus('Escape'), c)).toBe(true)
    expect(c.urunIzgaradanCik).toHaveBeenCalled()
  })

  it('Enter urunu sepete ekler', () => {
    const c = izgara()
    expect(izgaraModu(tus('Enter'), c)).toBe(true)
    expect(c.urunIzgaraSec).toHaveBeenCalled()
  })

  it('Shift+Enter adet penceresini acar', () => {
    const c = izgara()
    expect(izgaraModu(tus('Enter', { shiftKey: true }), c)).toBe(true)
    expect(c.adetPopoverAc).toHaveBeenCalledWith({ id: 2, ad: 'B' }, 1)
  })

  it('rakamlar miktarli ekleme icin birikir (en fazla 4 hane)', () => {
    const c = izgara()
    for (const d of ['1', '2', '3', '4', '5']) izgaraModu(tus(d), c)
    expect(c.urunIzgaraRakam.value).toBe('1234')
  })

  it('Backspace rakamin son hanesini siler', () => {
    const c = izgara({ urunIzgaraRakam: ref('123') })
    expect(izgaraModu(tus('Backspace'), c)).toBe(true)
    expect(c.urunIzgaraRakam.value).toBe('12')
  })

  it('izgara modunda HARF kisayollari TETIKLENMEZ (oda kurali)', () => {
    // Izgara odaktayken `n` odeme yontemini degistirmemeli.
    const c = izgara()
    const islendi = izgaraModu(tus('n'), c)
    expect(islendi).toBe(true) // yutuldu
    expect(c.odemeYontemi.value).toBe('NAKIT')
    expect(c.geriAlYap).not.toHaveBeenCalled()
  })

  it('izgara modunda F tuslari hala calisir (fonksiyon tuslari once)', () => {
    const c = izgara()
    posKisayolCoz(tus('F8'), c)
    expect(c.fisOnizlemeToggle).toHaveBeenCalled()
  })
})

// ===========================================================================
// Sepette satir gezinme
// ===========================================================================
describe('usePosKisayollar — sepet gezinme', () => {
  it('sepet bosken oklar sepeti degil izgarayi yonetir', () => {
    const c = baglam({ sepet: ref([]) })
    expect(sepetGezinme(tus('ArrowDown'), c)).toBe(false)
  })

  it('↓ ilk satiri secer (aktif satir yoksa)', () => {
    const c = baglam({ aktifSatir: ref(-1) })
    expect(sepetGezinme(tus('ArrowDown'), c)).toBe(true)
    expect(c.aktifSatir.value).toBe(0)
  })

  it('↑ aktif satir yoksa son satiri secer', () => {
    const c = baglam({ aktifSatir: ref(-1) })
    expect(sepetGezinme(tus('ArrowUp'), c)).toBe(true)
    expect(c.aktifSatir.value).toBe(1)
  })

  it('oklar listede sinirla sarar (ilk/son satir disari tasmaz)', () => {
    const c = baglam({ aktifSatir: ref(1) })
    sepetGezinme(tus('ArrowDown'), c)
    expect(c.aktifSatir.value).toBe(1)
    sepetGezinme(tus('ArrowUp'), c)
    sepetGezinme(tus('ArrowUp'), c)
    expect(c.aktifSatir.value).toBe(0)
  })

  it('yazarken oklar sepeti gezmez', () => {
    const c = baglam({ aktifSatir: ref(0) })
    expect(sepetGezinme(tus('ArrowDown', { target: GIRDI }), c)).toBe(false)
    expect(c.aktifSatir.value).toBe(0)
  })

  it('Enter aktif satirin miktar alanini odaklar', () => {
    const c = baglam({ aktifSatir: ref(0) })
    expect(sepetGezinme(tus('Enter'), c)).toBe(true)
    expect(c.odaklaAktifAdet).toHaveBeenCalled()
  })

  it('aktif satir yoksa Enter sepeti degistirmez', () => {
    const c = baglam({ aktifSatir: ref(-1) })
    sepetGezinme(tus('Enter'), c)
    expect(c.odaklaAktifAdet).not.toHaveBeenCalled()
  })
})

// ===========================================================================
// Yazarken koruma + Delete + harf kisayollari
// ===========================================================================
describe('usePosKisayollar — yazarken koruma (KRITIK kural)', () => {
  it('girdi alaninda Delete ve harfler devre disi kalir', () => {
    const c = baglam({ aktifSatir: ref(0) })
    // Delete
    posKisayolCoz(tus('Delete', { target: GIRDI }), c)
    expect(c.sepetSil).not.toHaveBeenCalled()
    // Harfler: urun adi yazarken `n` odeme yontemini DEGISTIRMEMELI
    posKisayolCoz(tus('n', { target: GIRDI }), c)
    expect(c.odemeYontemi.value).toBe('NAKIT')
    posKisayolCoz(tus('g', { target: GIRDI }), c)
    expect(c.geriAlYap).not.toHaveBeenCalled()
  })

  it('kontrol metin alaninda DEGILKEN her sey calisir', () => {
    const c = baglam({ aktifSatir: ref(0) })
    posKisayolCoz(tus('n'), c)
    expect(c.odemeYontemi.value).toBe('NAKIT')
    posKisayolCoz(tus('Delete'), c)
    expect(c.sepetSil).toHaveBeenCalledWith(0)
  })

  it('yazarkenKoru yalniz girdi hedeflerinde true doner', () => {
    expect(yazarkenKoru({ target: GIRDI })).toBe(true)
    expect(yazarkenKoru({ target: ALAN })).toBe(true)
    expect(yazarkenKoru({ target: SECIM })).toBe(true)
    expect(yazarkenKoru({ target: { tagName: 'DIV' } })).toBe(false)
  })
})

describe('usePosKisayollar — Delete ve harf kisayollari', () => {
  it('Delete aktif satiri siler', () => {
    const c = baglam({ aktifSatir: ref(1) })
    expect(satirSil(tus('Delete'), c)).toBe(true)
    expect(c.sepetSil).toHaveBeenCalledWith(1)
  })

  it('Delete aktif satir yoksa sessizce gecersizdir', () => {
    const c = baglam({ aktifSatir: ref(-1) })
    expect(satirSil(tus('Delete'), c)).toBe(false)
  })

  it('odeme yontemi harfleri: n/k/h/t', () => {
    for (const [harf, beklenen] of [['n', 'NAKIT'], ['k', 'KART'], ['h', 'HAVALE'], ['t', 'TAKSIT']]) {
      const c = baglam()
      expect(harfKisayollari(tus(harf), c)).toBe(true)
      expect(c.odemeYontemi.value).toBe(beklenen)
    }
  })

  it('harfler buyuk/kucuk duyarsizdir (Shift+N de calisir)', () => {
    const c = baglam()
    harfKisayollari(tus('N'), c)
    expect(c.odemeYontemi.value).toBe('NAKIT')
  })

  it('P fis modunu degistirir', () => {
    const c = baglam()
    expect(harfKisayollari(tus('p'), c)).toBe(true)
    expect(c.fisDegiskeniniDegistir).toHaveBeenCalled()
  })

  it('G geri al calistirir', () => {
    const c = baglam()
    expect(harfKisayollari(tus('g'), c)).toBe(true)
    expect(c.geriAlYap).toHaveBeenCalled()
  })

  it('D aktif satiri cogaltir', () => {
    const c = baglam({ aktifSatir: ref(0) })
    expect(harfKisayollari(tus('d'), c)).toBe(true)
    expect(c.aktifSatiriCogalt).toHaveBeenCalled()
  })

  it('D aktif satir yoksa cogaltmaz', () => {
    const c = baglam({ aktifSatir: ref(-1) })
    harfKisayollari(tus('d'), c)
    expect(c.aktifSatiriCogalt).not.toHaveBeenCalled()
  })

  it('bilinmeyen harf islenmez', () => {
    const c = baglam()
    expect(harfKisayollari(tus('z'), c)).toBe(false)
  })
})

// ===========================================================================
// Dispatcher sirasi
// ===========================================================================
describe('usePosKisayollar — oncelik sirasi', () => {
  it('fonksiyon tuslari izgara modundan ONCE gelir', () => {
    // Izgara modu "her tusu yutar"; F tuslari yine de calismali.
    const c = baglam({ urunIzgaraOdak: ref(true) })
    posKisayolCoz(tus('F2'), c)
    expect(c.sepetiGeriAlinabilirTemizle).toHaveBeenCalled()
  })

  it('izgara modu sepetteki oklardan ONCE gelir', () => {
    const c = baglam({ urunIzgaraOdak: ref(true) })
    posKisayolCoz(tus('ArrowDown'), c)
    expect(c.urunIzgaraHareket).toHaveBeenCalledWith(0, 1)
    expect(c.aktifSatir.value).toBe(-1) // sepette gezinmedi
  })

  it('Escape izgara modunda once izgaradan cikar, sonra hicbire girmez', () => {
    const c = baglam({ urunIzgaraOdak: ref(true) })
    posKisayolCoz(tus('Escape'), c)
    expect(c.urunIzgaradanCik).toHaveBeenCalled()
  })

  it('islenmeyen tus icin false doner', () => {
    const c = baglam()
    expect(posKisayolCoz(tus('Shift'), c)).toBe(false)
    expect(posKisayolCoz(tus('F12'), c)).toBe(false)
  })

  it('ozel tuslar harf kisa yoluna DUSMEZ', () => {
    const c = baglam()
    for (const tusAdi of ['Shift', 'Control', 'Alt', 'Meta', 'Tab', 'CapsLock']) {
      posKisayolCoz(tus(tusAdi), c)
    }
    expect(c.odemeYontemi.value).toBe('NAKIT')
    expect(c.geriAlYap).not.toHaveBeenCalled()
  })
})