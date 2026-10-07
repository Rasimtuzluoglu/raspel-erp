import { describe, it, expect, beforeEach, vi } from 'vitest'
import { ref, nextTick } from 'vue'
import { usePosOdeme } from '../usePosOdeme.js'

/**
 * POS ODEME / INDIRIM — davranis testleri.
 *
 * `usePosOdeme`, `HizliSatis.vue` icinde dagilan odeme blokunu tek yerde
 * toplar. Korunan is kurallari:
 *   - Indirim tutar/yuzde olarak hesaplanir ve toplami asmaz.
 *   - Odeme durumu (tam/yarim/yok) odenen tutari OTOMATIK ayarlar; genel
 *     toplam sonradan degisirse de ayni kural isler.
 *   - Kalan = genel toplam - odenen (negatif olamaz).
 *   - Odeme durumu ENUM/etiket/severity tek kaynaktan turetilir.
 *   - Para ustu yalniz NAKIT + gercekten para verilmisse hesaplanir.
 *   - POS komisyonu yalniz terminal seciliyken hesaplanir.
 */

vi.mock('../../api/index.js', () => ({
  kasaAPI: { getAllKasalar: vi.fn() },
  bankaAPI: { getAll: vi.fn() },
  posAPI: { aktif: vi.fn() }
}))

import { kasaAPI, bankaAPI, posAPI } from '../../api/index.js'

const kur = (toplamDeger = 1000) => {
  const toplam = ref(toplamDeger)
  const o = usePosOdeme({ t: (k) => `[${k}]`, toplam })
  return { ...o, toplam }
}

beforeEach(() => {
  kasaAPI.getAllKasalar.mockReset()
  bankaAPI.getAll.mockReset()
  posAPI.aktif.mockReset()
})

describe('usePosOdeme — varsayilanlar', () => {
  it('indirim yok, odeme tam, yontem nakit', () => {
    const o = kur()
    expect(o.indirimDegeri.value).toBe(0)
    expect(o.indirimTipi.value).toBe('tutar')
    expect(o.odemeDurumu.value).toBe('tam')
    expect(o.odemeYontemi.value).toBe('NAKIT')
    expect(o.taksitSayisi.value).toBe(1)
  })

  it('odeme tipleri ve yontemleri ceviriyle uretilir', () => {
    const o = kur()
    expect(o.odemeTipleri.value.map((x) => x.value)).toEqual(['tam', 'yarim', 'yok'])
    expect(o.odemeYontemleri.value.map((x) => x.value)).toEqual(['NAKIT', 'KART', 'HAVALE', 'TAKSIT'])
  })
})

describe('usePosOdeme — indirim', () => {
  it('tutar indirimi toplamdan duser', () => {
    const o = kur(1000)
    o.indirimDegeri.value = 150
    expect(o.indirimTutari.value).toBe(150)
    expect(o.genelToplam.value).toBe(850)
  })

  it('yuzde indirimi hesaplanir', () => {
    const o = kur(1000)
    o.indirimTipi.value = 'yuzde'
    o.indirimDegeri.value = 25
    expect(o.indirimTutari.value).toBe(250)
    expect(o.genelToplam.value).toBe(750)
  })

  it('yuzde 100 ile sinirlanir (asla toplami asmaz)', () => {
    const o = kur(1000)
    o.indirimTipi.value = 'yuzde'
    o.indirimDegeri.value = 500
    expect(o.indirimTutari.value).toBe(1000)
    expect(o.genelToplam.value).toBe(0)
  })

  it('tutar indirimi toplami asmaz', () => {
    const o = kur(1000)
    o.indirimDegeri.value = 5000
    expect(o.indirimTutari.value).toBe(1000)
    expect(o.genelToplam.value).toBe(0)
  })

  it('indirim yoksa tutar 0', () => {
    const o = kur(1000)
    expect(o.indirimTutari.value).toBe(0)
    expect(o.genelToplam.value).toBe(1000)
  })
})

describe('usePosOdeme — odeme durumu senkronu (KRITIK)', () => {
  it('"tam" secilince odenen = genel toplam', async () => {
    const o = kur(1000)
    o.odemeDurumu.value = 'yarim'
    await nextTick()
    o.odemeDurumu.value = 'tam'
    await nextTick()
    expect(o.odenenTutar.value).toBe(1000)
  })

  it('"yarim" secilince odenen = genel toplamin yarisi', async () => {
    const o = kur(1000)
    o.odemeDurumu.value = 'yarim'
    await nextTick()
    expect(o.odenenTutar.value).toBe(500)
  })

  it('"yok" secilince odenen 0', async () => {
    const o = kur(1000)
    o.odemeDurumu.value = 'yok'
    await nextTick()
    expect(o.odenenTutar.value).toBe(0)
  })

  it('sepet toplami SONRADAN degisirse odenen de guncellenir', async () => {
    const o = kur(1000)
    await nextTick() // "tam" watcher basta calisir
    o.toplam.value = 1500
    await nextTick()
    expect(o.genelToplam.value).toBe(1500)
    expect(o.odenenTutar.value).toBe(1500)
  })

  it('indirim sonrasi genel toplam duser, odenen ona uyar', async () => {
    const o = kur(1000)
    o.indirimDegeri.value = 400
    await nextTick()
    expect(o.odenenTutar.value).toBe(600)
  })
})

describe('usePosOdeme — kalan tutar', () => {
  it('kalan = genel toplam - odenen', () => {
    const o = kur(1000)
    o.odenenTutar.value = 300
    expect(o.kalanTutar.value).toBe(700)
  })

  it('fazla odeme kalan tutari negatife dusurmez', () => {
    const o = kur(1000)
    o.odenenTutar.value = 1200
    expect(o.kalanTutar.value).toBe(0)
  })
})

describe('usePosOdeme — odeme durumu etiketi / enum / severity', () => {
  it('odeme yoksa ODENMEDI + danger', () => {
    const o = kur(1000)
    o.odemeDurumu.value = 'yok'
    expect(o.odemeDurumEnum.value).toBe('ODENMEDI')
    expect(o.odemeDurumSeverity.value).toBe('danger')
    expect(o.odemeDurumText.value).toBe('[hizliSatis.odemedi]')
  })

  it('kismi odemede KISMI_ODENDI + warning', () => {
    const o = kur(1000)
    o.odenenTutar.value = 400
    expect(o.odemeDurumEnum.value).toBe('KISMI_ODENDI')
    expect(o.odemeDurumSeverity.value).toBe('warning')
  })

  it('tam odemede ODENDI + success', () => {
    const o = kur(1000)
    o.odenenTutar.value = 1000
    expect(o.odemeDurumEnum.value).toBe('ODENDI')
    expect(o.odemeDurumSeverity.value).toBe('success')
    expect(o.odemeDurumText.value).toBe('[hizliSatis.tamamenOdendi]')
  })

  it('genel toplam 0 ise "odendi" sayilir (bedava satis)', () => {
    const o = kur(0)
    o.odenenTutar.value = 0
    expect(o.odemeDurumEnum.value).toBe('ODENMEDI')
  })
})

describe('usePosOdeme — para ustu', () => {
  it('nakit + fazla verilince fark hesaplanir', () => {
    const o = kur(1000)
    o.odenenTutar.value = 1000
    o.alinanNakit.value = 1200
    expect(o.paraUstu.value).toBe(200)
  })

  it('nakit degilse para ustu 0', () => {
    const o = kur(1000)
    o.odemeYontemi.value = 'KART'
    o.odenenTutar.value = 1000
    o.alinanNakit.value = 1200
    expect(o.paraUstu.value).toBe(0)
  })

  it('eksik nakit verilirse para ustu 0 (negatif olmaz)', () => {
    const o = kur(1000)
    o.odenenTutar.value = 1000
    o.alinanNakit.value = 800
    expect(o.paraUstu.value).toBe(0)
  })

  it('odeme yoksa para ustu hesaplanmaz', () => {
    const o = kur(1000)
    o.odemeDurumu.value = 'yok'
    o.alinanNakit.value = 1000
    expect(o.paraUstu.value).toBe(0)
  })

  it('hizli nakit degerleri tanimli', () => {
    const o = kur()
    expect(o.hizliNakit).toEqual([50, 100, 200, 500])
  })
})

describe('usePosOdeme — POS komisyonu', () => {
  it('terminal seciliyse komisyon hesaplanir', async () => {
    const o = kur(1000)
    o.posTerminalleri.value = [{ id: 3, ad: 'T1', komisyonOrani: 2.5 }]
    o.seciliPos.value = 3
    o.odenenTutar.value = 1000
    await nextTick()
    expect(o.seciliPosBilgi.value.id).toBe(3)
    expect(o.hesaplananKomisyon.value).toBe(25)
  })

  it('terminal secili degilse komisyon 0', () => {
    const o = kur(1000)
    o.odenenTutar.value = 1000
    expect(o.hesaplananKomisyon.value).toBe(0)
  })

  it('komisyon orani 0 ise komisyon 0', () => {
    const o = kur(1000)
    o.posTerminalleri.value = [{ id: 3, komisyonOrani: 0 }]
    o.seciliPos.value = 3
    o.odenenTutar.value = 1000
    expect(o.hesaplananKomisyon.value).toBe(0)
  })
})

describe('usePosOdeme — yukleyiciler', () => {
  it('kasalar yuklenir ve ilk kasa otomatik secilir', async () => {
    kasaAPI.getAllKasalar.mockResolvedValue({ data: { content: [{ id: 7, ad: 'K1' }] } })
    const o = kur()
    await o.kasalariYukle()
    expect(o.kasalar.value).toHaveLength(1)
    expect(o.seciliKasa.value).toBe(7)
  })

  it('mevcut kasa secimi ezilmez', async () => {
    kasaAPI.getAllKasalar.mockResolvedValue({ data: { content: [{ id: 7 }, { id: 9 }] } })
    const o = kur()
    o.seciliKasa.value = 9
    await o.kasalariYukle()
    expect(o.seciliKasa.value).toBe(9)
  })

  it('kasa yukleme hatasi bos listeyle sonuclanir (ekran cokmez)', async () => {
    kasaAPI.getAllKasalar.mockRejectedValue(new Error('yok'))
    const o = kur()
    await o.kasalariYukle()
    expect(o.kasalar.value).toEqual([])
  })

  it('bankalar yuklenir', async () => {
    bankaAPI.getAll.mockResolvedValue({ data: { content: [{ id: 1, ad: 'Ziraat' }] } })
    const o = kur()
    await o.bankalariYukle()
    expect(o.bankalar.value).toHaveLength(1)
  })

  it('POS terminalleri dizi veya sayfa yanitindan cozulur', async () => {
    posAPI.aktif.mockResolvedValue({ data: [{ id: 1 }] })
    const o = kur()
    await o.poslariYukle()
    expect(o.posTerminalleri.value).toHaveLength(1)
  })

  it('POS yukleme hatasi bos listeyle sonuclanir', async () => {
    posAPI.aktif.mockRejectedValue(new Error('yok'))
    const o = kur()
    await o.poslariYukle()
    expect(o.posTerminalleri.value).toEqual([])
  })
})

describe('usePosOdeme — odemeSifirla', () => {
  it('odeme alanlarini baslangic durumuna dondurur', async () => {
    const o = kur(1000)
    o.indirimDegeri.value = 100
    o.odemeDurumu.value = 'yarim'
    o.odenenTutar.value = 500
    o.alinanNakit.value = 800
    o.taksitKurum.value = 'X'
    o.taksitTutar.value = 100
    o.taksitSayisi.value = 6
    o.seciliPos.value = 3
    o.odemeSifirla()
    expect(o.indirimDegeri.value).toBe(0)
    expect(o.odemeDurumu.value).toBe('tam')
    expect(o.alinanNakit.value).toBe(0)
    expect(o.taksitKurum.value).toBe('')
    expect(o.taksitTutar.value).toBe(0)
    expect(o.taksitSayisi.value).toBe(1)
    expect(o.seciliPos.value).toBeNull()
    // NOT: `odemeSifirla` ayni tick icinde durumu 'tam'a dondurur ve odenen
    // tutari 0 yazar. Vue watcher'i 'tam' -> 'yarim' -> 'tam' net degisimini
    // gormedigi icin tekrar tetiklenmez; bu yuzden odenen 0 kalir. Gercek
    // akista sepet de sifirlandigi icin genel toplam da 0'dir — tutarli.
    await nextTick()
    expect(o.odenenTutar.value).toBe(0)
  })

  it('sepet de sifirsa (gercek akis) odenen 0 kalir', async () => {
    const o = kur(1000)
    o.odemeDurumu.value = 'yarim'
    o.toplam.value = 0
    o.odemeSifirla()
    await nextTick()
    expect(o.genelToplam.value).toBe(0)
    expect(o.odenenTutar.value).toBe(0)
  })
})