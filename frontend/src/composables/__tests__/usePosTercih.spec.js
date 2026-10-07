import { describe, it, expect, beforeEach, vi } from 'vitest'
import { defineComponent, nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { usePosTercih } from '../usePosTercih.js'

/**
 * POS TERCILERI — birim testleri.
 *
 * Bu composable'in tek islevi: "bir tercih ne zaman kaydedilir?" sorusunun
 * cevabini TEK yerde tutmak. View'da once ayni kuralin dort farkli
 * uygulamasi vardi (watch / @change / dogrudan setItem / hic yazmiyordu) ve
 * ipucu anahtari uc ayri yerde yaziliyordu.
 *
 * Testler `localStorage`u dogrudan okuyarak KALICI FORMATI dogrular: eski kod
 * `String(x)` + `getItem() === 'true'` ile yaziyordu, `safeStorage` ise
 * `JSON.stringify` kullanir. Ikisi de boolean'i `"true"` olarak yazdigindan
 * operatorun kayitli ayarlari bozulmaz.
 */

/** Composable'i Vue yasam dongusune baglayan minimal sarmalayici. */
const kur = (t = (k) => `[${k}]`) => {
  let sonuc = null
  const Bilesen = defineComponent({
    setup() {
      sonuc = usePosTercih({ t })
      return () => null
    }
  })
  const wrapper = mount(Bilesen)
  return { ...sonuc, wrapper }
}

beforeEach(() => {
  localStorage.clear()
})

describe('usePosTercih — varsayilanlar', () => {
  it('ana kasa akisi (musteri -> sepet -> odeme) varsayilan ACIK gelir', () => {
    const t = kur()
    expect(t.musteriAcik.value).toBe(true)
    expect(t.sepetAcik.value).toBe(true)
    expect(t.odemeAcik.value).toBe(true)
  })

  it('gelismis alanlar (teslimat/fis/detay) varsayilan KAPALI gelir', () => {
    const t = kur()
    expect(t.teslimatAcik.value).toBe(false)
    expect(t.fisAcik.value).toBe(false)
    expect(t.detayAcik.value).toBe(false)
  })

  it('kullanilabilirlik tercihleri varsayilan kapali, otomatik yazdirma acik', () => {
    const t = kur()
    expect(t.buyukYazi.value).toBe(false)
    expect(t.onayIste.value).toBe(false)
    expect(t.ipucuAcik.value).toBe(false)
    expect(t.otomatikYazdir.value).toBe(true)
  })

  it('fis ayarlari varsayilan: fiyatli fis, 80mm', () => {
    const t = kur()
    expect(t.fisFiyatli.value).toBe(true)
    expect(t.fisGenislik.value).toBe('80')
  })

  it('fis alt notu bos ise ceviri varsayilanina duser', () => {
    const t = kur()
    expect(t.fisAltNotu.value).toBe('[hizliSatis.fisAltNotVarsayilan]')
  })

  it('t verilmezse alt not bos kalir (cokmaz)', () => {
    const t = kur(null)
    expect(t.fisAltNotu.value).toBe('')
  })
})

describe('usePosTercih — kalicilik', () => {
  it('deger degisince localStorage a yazilir', async () => {
    const t = kur()
    t.buyukYazi.value = true
    await nextTick()
    expect(localStorage.getItem('raspel_pos_buyuk_yazi')).toBe('true')
  })

  it('false deger de dogru yazilir (|| tuzagi olmamali)', async () => {
    const t = kur()
    // once varsayilan acik olan bir alani kapat
    expect(t.sepetAcik.value).toBe(true)
    t.sepetAcik.value = false
    await nextTick()
    expect(localStorage.getItem('raspel_pos_sepet_acik')).toBe('false')
  })

  it('ayni kural butun tercihler icin gecerli (dort ayri yontem degil)', async () => {
    const t = kur()
    t.teslimatAcik.value = true
    t.detayAcik.value = true
    t.onayIste.value = true
    await nextTick()
    expect(localStorage.getItem('raspel_pos_teslimat_acik')).toBe('true')
    expect(localStorage.getItem('raspel_pos_detay_acik')).toBe('true')
    expect(localStorage.getItem('raspel_pos_onay_iste')).toBe('true')
  })

  it('kayitli deger mount aninda okunur', () => {
    localStorage.setItem('raspel_pos_buyuk_yazi', 'true')
    localStorage.setItem('raspel_pos_otomatik_yazdir', 'false')
    const t = kur()
    expect(t.buyukYazi.value).toBe(true)
    expect(t.otomatikYazdir.value).toBe(false)
  })

  it('fis ayarlari da kalicidir', async () => {
    const t = kur()
    t.fisGenislik.value = '58'
    t.fisFiyatli.value = false
    await nextTick()
    expect(localStorage.getItem('raspel_fis_genislik')).toBe('"58"')
    expect(localStorage.getItem('raspel_fis_fiyatli')).toBe('false')
  })

  it('fis alt notu bos olabilir ve bos olarak saklanir', async () => {
    const t = kur()
    t.fisAltNotu.value = ''
    await nextTick()
    // Bos string kaydedilmis olmali; `||` ile varsayilana dusmemeli
    expect(localStorage.getItem('raspel_fis_notu')).toBe('""')
  })
})

describe('usePosTercih — degistir / ipucuKapat', () => {
  it('degistir degeri tersine cevirir ve yeni degeri dondurur', async () => {
    const t = kur()
    expect(t.degistir('ipucuAcik')).toBe(true)
    await nextTick()
    expect(localStorage.getItem('raspel_pos_ipucu_acik')).toBe('true')
    expect(t.degistir('ipucuAcik')).toBe(false)
  })

  it('degistir sepet/odeme paneli icin de calisir', async () => {
    const t = kur()
    t.degistir('sepetAcik')
    t.degistir('odemeAcik')
    await nextTick()
    expect(t.sepetAcik.value).toBe(false)
    expect(t.odemeAcik.value).toBe(false)
  })

  it('ipucuKapat yalniz ipucunu kapatir, diger tercihleri etkilemez', async () => {
    const t = kur()
    t.ipucuAcik.value = true
    t.buyukYazi.value = true
    await nextTick()
    t.ipucuKapat()
    await nextTick()
    expect(t.ipucuAcik.value).toBe(false)
    expect(t.buyukYazi.value).toBe(true)
  })
})

describe('usePosTercih — sunucu ayarlari', () => {
  it('sunucudan gelen fis ayarlarini uygular ve kalicilik ayni yola gider', async () => {
    const t = kur()
    t.sunucuAyarlariniUygula({ fisGenislik: '58', fisFiyatli: false, fisAltNotu: 'Firma fis notu' })
    await nextTick()
    expect(t.fisGenislik.value).toBe('58')
    expect(t.fisFiyatli.value).toBe(false)
    expect(t.fisAltNotu.value).toBe('Firma fis notu')
    expect(localStorage.getItem('raspel_fis_genislik')).toBe('"58"')
    expect(localStorage.getItem('raspel_fis_notu')).toBe('"Firma fis notu"')
  })

  it('sunucu deger gondermezse operatorun yerel secimi KALIR', async () => {
    // Operator 58mm secmisti; sunucuda genislik tanimi YOK.
    const t = kur()
    t.fisGenislik.value = '58'
    await nextTick()

    t.sunucuAyarlariniUygula({ fisGenislik: null, fisFiyatli: undefined })
    await nextTick()
    expect(t.fisGenislik.value).toBe('58')
  })

  it('bos nesve uygulanirsa hicbir sey degismez', async () => {
    const t = kur()
    t.sunucuAyarlariniUygula({})
    await nextTick()
    expect(t.fisGenislik.value).toBe('80')
    expect(t.fisFiyatli.value).toBe(true)
  })

  it('cagirmazsa hicbir sey olmaz (null guard)', () => {
    const t = kur()
    expect(() => t.sunucuAyarlariniUygula()).not.toThrow()
  })
})

describe('usePosTercih — dayaniklilik', () => {
  it('bozuk localStorage verisi varsayilana dusulur (sayfa acilmaz)', () => {
    // Bozuk JSON: safeStorage parse edemez, anahtari siler ve varsayilani doner.
    localStorage.setItem('raspel_pos_buyuk_yazi', '{bozuk')
    const t = kur()
    expect(t.buyukYazi.value).toBe(false)
  })

  it('localStorage yazilamiyorsa (gizli mod) sayfa yine calisir', () => {
    const setItem = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('QuotaExceededError')
    })
    const t = kur()
    expect(() => {
      t.buyukYazi.value = true
    }).not.toThrow()
    setItem.mockRestore()
  })
})

describe('usePosTercih — tercihOzeti', () => {
  it('her tercih icin anahtar, varsayilan ve aciklama listeler', () => {
    const t = kur()
    const ozet = t.tercihOzeti.value
    expect(ozet.length).toBeGreaterThanOrEqual(10)
    for (const o of ozet) {
      expect(o).toHaveProperty('ad')
      expect(o).toHaveProperty('anahtar')
      expect(o).toHaveProperty('varsayilan')
      expect(o.aciklama.length).toBeGreaterThan(0)
    }
  })
})