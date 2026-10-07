import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import FaturaKalemleri from '../FaturaKalemleri.vue'
import i18n from '../../i18n.js'

// Onay diyalogu testte kontrol edilir: `onayDavranis` accept/reject belirler.
let onayDavranis = 'accept'
vi.mock('primevue/useconfirm', () => ({
  useConfirm: () => ({
    require: (opts) => {
      if (onayDavranis === 'accept') opts.accept?.()
      else opts.reject?.()
    }
  })
}))

// Bilesen stok aramasini `stokAPI.ara` / `barkodIleBul` ile yapar; testte disari cikmasin.
vi.mock('../../api/index.js', () => ({
  stokAPI: {
    ara: vi.fn().mockResolvedValue({ data: [] }),
    barkodIleBul: vi.fn()
  }
}))

/**
 * FATURA KALEMLERI —sozlesme ve is kurali testleri (karakterizasyon).
 *
 * Bu bilesen HEM `Satis.vue` (yeni satis penceresi) HEM `Faturalar.vue`
 * tarafindan kullanilir. Bu testler revizyon ONCESI davranisi sabitler.
 *
 * KORUNAN IS KURALLARI:
 *   - Kalem eklerken `iskontoOrani` NULL birakilir (0 degil). Backend `null`
 *     gorunce kademeli indirim kurallarini uygular; `0` gorunce atlar.
 *   - `stokMiktar` kalemle birlikte tasinir (kayittan once stok kontrolu).
 *   - Stok secilmeden yazilan aciklamalar ayrica uyarilir.
 *   - `cogalt` kayit kimligini tasimaz (yeni satir olusur).
 *
 * DUZEN: `darEkran` (>720px tablo, <=720px kart). Testler TABLO duzeninde
 * calisir (masaustu birincil duzen); `darEkran` `onMounted` icinde ayarlandigi
 * icin mount sonrasi tick beklenir.
 *
 * DataTable/Column stub'i GERCEK PrimeVue davranisini taklit eder: `#body`
 * slot'unu her satir icin `{ data, index }` ile cagirir. Bu olmadan satir
 * hucreleri (adet/stok notu/aksiyonlar) hic render edilmezdi.
 */

/** AutoComplete stub'inin `option-select` ile yayacagi secenek nesnesi
 *  (`{ stok }` sarmali — bilesen `e.value.stok` okur). */
let secilecekOption = null

const stubs = {
  Button: {
    name: 'Button',
    // `title` prop olarak TANIMLANMAZ: fallthrough ile kok `<button>`a gecsin
    // (ikon dugmelerinde metin yok; testler `title` ile bulur).
    props: ['label', 'icon', 'severity'],
    template: '<button><i v-if="icon" :class="icon" />{{ label }}</button>'
  },
  InputNumber: {
    name: 'InputNumber',
    props: ['modelValue', 'min', 'max', 'placeholder'],
    emits: ['update:modelValue'],
    template: '<input class="inputnumber" :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))" />'
  },
  InputText: {
    name: 'InputText',
    props: ['modelValue', 'placeholder'],
    emits: ['update:modelValue'],
    template: '<input class="inputtext" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
  },
  IconField: { name: 'IconField', template: '<div class="iconfield"><slot /></div>' },
  InputIcon: { name: 'InputIcon', template: '<i />' },
  Dropdown: {
    name: 'Dropdown',
    props: ['modelValue', 'options'],
    emits: ['update:modelValue'],
    template:
      '<select class="dropdown"><option v-for="o in options" :key="o" :value="o">{{ o }}</option></select>'
  },
  AutoComplete: {
    name: 'AutoComplete',
    props: ['modelValue', 'suggestions', 'optionLabel', 'placeholder'],
    emits: ['update:modelValue', 'complete', 'option-select'],
    computed: {
      secenek() {
        return secilecekOption
      }
    },
    template:
      '<div class="autocomplete"><input class="ac-input" :placeholder="placeholder" @input="$emit(\'complete\', { query: $event.target.value })" /><button class="ac-sec" @click="$emit(\'option-select\', { value: secenek })">sec</button></div>'
  },
  EmptyState: { name: 'EmptyState', template: '<div class="empty-state" />' },
  // DataTable, satirlari Column'lara `provide` ile verir; Column her satir icin
  // `#body` slotunu cagirir (PrimeVue davranisi).
  DataTable: {
    name: 'DataTable',
    props: ['value'],
    provide() {
      return { __satirlar: () => this.value || [] }
    },
    template: '<div class="datatable"><slot /></div>'
  },
  Column: {
    name: 'Column',
    inject: ['__satirlar'],
    props: ['header', 'field', 'style'],
    computed: {
      satirlar() {
        return this.__satirlar ? this.__satirlar() : []
      }
    },
    template:
      '<div class="column"><template v-for="(satir, i) in satirlar" :key="i"><slot name="body" :data="satir" :index="i" /></template></div>'
  }
}

/** `darEkran` degerini zorlar (varsayilan: tablo duzeni). */
const ekranAyarla = (dar) => {
  window.matchMedia = vi.fn().mockImplementation((q) => ({
    matches: dar ? String(q).includes('720px') : false,
    media: q,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    addListener: vi.fn(),
    removeListener: vi.fn(),
    onchange: null,
    dispatchEvent: vi.fn()
  }))
}

/** Mount + `darEkran` `onMounted` sonucunun islenmesi icin tick bekler. */
const kur = async (props = {}, { dar = false } = {}) => {
  ekranAyarla(dar)
  const w = mount(FaturaKalemleri, {
    props: {
      kalemler: [],
      araToplam: 0,
      kdvToplam: 0,
      genelToplam: 0,
      kdvSecenekleri: [0, 1, 8, 10, 18, 20],
      ...props
    },
    global: { plugins: [i18n], stubs }
  })
  await nextTick()
  await nextTick()
  return w
}

const kalem = (ek = {}) => ({
  id: 1,
  stokId: 10,
  aciklama: 'Vida M8',
  adet: 2,
  birimFiyat: 50,
  iskontoOrani: null,
  kdvOrani: 20,
  stokMiktar: 100,
  ...ek
})

const ekleBtn = (w) => w.findAll('button').find((b) => b.text().includes('Ekle'))
const baslikliBtn = (w, baslik) => w.findAll('button').find((b) => b.attributes('title') === baslik)

beforeEach(() => {
  secilecekOption = null
  onayDavranis = 'accept'
})

describe('FaturaKalemleri — genel yapi', () => {
  it('hizli kalem ekleme satirini gosterir', async () => {
    const w = await kur()
    expect(w.find('.hizli-kalem').exists()).toBe(true)
    expect(w.find('.hizli-kalem .autocomplete').exists()).toBe(true)
  })

  it('tablo duzeninde kalem listesi gosterilir', async () => {
    const w = await kur({ kalemler: [kalem()] })
    expect(w.find('.datatable').exists()).toBe(true)
    expect(w.findAll('.column').length).toBeGreaterThan(0)
  })

  it('dar ekranda kart duzeni + bos durum', async () => {
    const w = await kur({ kalemler: [] }, { dar: true })
    expect(w.find('.kalem-kartlar').exists()).toBe(true)
    expect(w.find('.kalem-yok').exists()).toBe(true)
  })

  it('ozet props degerlerini gosterir', async () => {
    const w = await kur({ araToplam: 100, kdvToplam: 20, genelToplam: 120 })
    const ozet = w.find('.summary-box').text()
    expect(ozet).toContain('100')
    expect(ozet).toContain('20')
    expect(ozet).toContain('120')
  })
})

describe('FaturaKalemleri — kalem ekleme (hizli ekleme)', () => {
  const secBeton = () => {
    secilecekOption = { stok: { id: 7, ad: 'Beton', satisFiyati: 250, kdvOrani: 10, miktar: 30 } }
  }

  it('stok secilince fiyat alani stoktan dolar', async () => {
    secBeton()
    const w = await kur()
    await w.find('.hizli-kalem .ac-sec').trigger('click')
    await nextTick()
    const sayilar = w.findAll('input.inputnumber')
    expect(sayilar[1].element.value).toBe('250') // hizli ekleme fiyat alani
  })

  it('Ekle -> add olayi dogru sekilde yayilir', async () => {
    secBeton()
    const w = await kur()
    await w.find('.hizli-kalem .ac-sec').trigger('click')
    await nextTick()
    await ekleBtn(w).trigger('click')
    const satir = w.emitted('add')[0][0]
    expect(satir.stokId).toBe(7)
    expect(satir.aciklama).toBe('Beton')
    expect(satir.birimFiyat).toBe(250)
    expect(satir.kdvOrani).toBe(10)
  })

  it('KRITIK: iskontoOrani NULL birakilir (indirim motoru calissin)', async () => {
    secBeton()
    const w = await kur()
    await w.find('.hizli-kalem .ac-sec').trigger('click')
    await nextTick()
    await ekleBtn(w).trigger('click')
    expect(w.emitted('add')[0][0].iskontoOrani).toBeNull()
  })

  it('KRITIK: stokMiktar kalemle tasinir (kayittan once stok kontrolu)', async () => {
    secBeton()
    const w = await kur()
    await w.find('.hizli-kalem .ac-sec').trigger('click')
    await nextTick()
    await ekleBtn(w).trigger('click')
    expect(w.emitted('add')[0][0].stokMiktar).toBe(30)
  })

  it('stokta KDV tanimli degilse varsayilan KDV uygulanir', async () => {
    secilecekOption = { stok: { id: 8, ad: 'X', satisFiyati: 10, miktar: 5 } }
    const w = await kur({ kdvVarsayilan: 20 })
    await w.find('.hizli-kalem .ac-sec').trigger('click')
    await nextTick()
    await ekleBtn(w).trigger('click')
    expect(w.emitted('add')[0][0].kdvOrani).toBe(20)
  })

  it('stok secilmeden Ekle -> stokId null (bos aciklamali satir)', async () => {
    const w = await kur()
    await ekleBtn(w).trigger('click')
    const satir = w.emitted('add')[0][0]
    expect(satir.stokId).toBeNull()
    expect(satir.aciklama).toBe('')
  })
})

describe('FaturaKalemleri — satir islemleri', () => {
  it('cogalt kayit kimligini TASIMAZ ve add yayar', async () => {
    const w = await kur({ kalemler: [kalem()] })
    const kopyala = baslikliBtn(w, 'Çoğalt')
    expect(kopyala).toBeTruthy()
    await kopyala.trigger('click')
    const satir = w.emitted('add')[0][0]
    expect('id' in satir).toBe(false)
    expect(satir.aciklama).toBe('Vida M8')
    expect(satir.adet).toBe(2)
  })

  it('silme dugmesi ONAY sonrasi remove olayini indeksle yayar', async () => {
    const w = await kur({ kalemler: [kalem(), kalem({ id: 2, aciklama: 'Somun' })] })
    const sil = baslikliBtn(w, 'Sil')
    expect(sil).toBeTruthy()
    await sil.trigger('click')
    expect(w.emitted('remove')).toEqual([[0]])
  })

  it('onay REDDEDILIRSE kalem SILINMEZ', async () => {
    onayDavranis = 'reject'
    const w = await kur({ kalemler: [kalem()] })
    await baslikliBtn(w, 'Sil').trigger('click')
    expect(w.emitted('remove')).toBeFalsy()
    // Geri-al bandi da cikmaz (silme olmadi)
    expect(w.find('.geri-al-bar').exists()).toBe(false)
  })

  it('silme sonrasi GERI AL bandi cikar ve eski konuma geri yukler', async () => {
    const w = await kur({ kalemler: [kalem({ aciklama: 'Vida' }), kalem({ id: 2, aciklama: 'Somun' })] })
    await baslikliBtn(w, 'Sil').trigger('click')
    expect(w.find('.geri-al-bar').exists()).toBe(true)

    await w.find('.geri-al-bar button').trigger('click')
    expect(w.emitted('geri-al')).toEqual([[{ index: 0, kalem: expect.objectContaining({ aciklama: 'Vida' }) }]])
    // Bant kapanir
    expect(w.find('.geri-al-bar').exists()).toBe(false)
  })

  it('satir alanlari duzenlenebilir (adet/birimFiyat/iskonto/kdv)', async () => {
    const w = await kur({ kalemler: [kalem()] })
    expect(w.findAll('input.inputnumber').length).toBeGreaterThanOrEqual(3)
    expect(w.find('select.dropdown').exists()).toBe(true)
  })
})

describe('FaturaKalemleri — stok uyarilari', () => {
  it('adet > stok ise hucre "yetersiz" isaretlenir', async () => {
    const w = await kur({ kalemler: [kalem({ adet: 5, stokMiktar: 3 })] })
    expect(w.find('.adet-sarici.stok-yetersiz').exists()).toBe(true)
  })

  it('adet <= stok ise yetersiz isareti YOK', async () => {
    const w = await kur({ kalemler: [kalem({ adet: 2, stokMiktar: 50 })] })
    expect(w.find('.adet-sarici.stok-yetersiz').exists()).toBe(false)
  })

  it('stok miktari bilinmiyorsa "stok yetersiz" denemez', async () => {
    const w = await kur({ kalemler: [kalem({ adet: 999, stokMiktar: null })] })
    expect(w.find('.adet-sarici.stok-yetersiz').exists()).toBe(false)
  })

  it('stokta kac adet oldugu satirda gosterilir', async () => {
    const w = await kur({ kalemler: [kalem({ adet: 2, stokMiktar: 50 })] })
    expect(w.find('.stok-notu').exists()).toBe(true)
    expect(w.find('.stok-notu').text()).toContain('50')
  })

  it('stok secilmeden yazilan aciklamalar uyarilir', async () => {
    const w = await kur({ kalemler: [kalem({ stokId: null, aciklama: 'Serbest metin' })] })
    expect(w.find('.stok-secsi-uyari').exists()).toBe(true)
  })

  it('stok secili satirlar icin uyari cikmaz', async () => {
    const w = await kur({ kalemler: [kalem({ stokId: 5, aciklama: 'Urun' })] })
    expect(w.find('.stok-secsi-uyari').exists()).toBe(false)
  })
})

describe('FaturaKalemleri — barkod ile hizli ekleme', () => {
  it('barkod alani render edilir', async () => {
    const w = await kur()
    expect(w.find('.hizli-barkod input').exists()).toBe(true)
  })

  it('barkod bulununca kalem ANINDA eklenir', async () => {
    const { stokAPI } = await import('../../api/index.js')
    stokAPI.barkodIleBul.mockResolvedValue({
      data: { id: 55, ad: 'Barkodlu Ürün', satisFiyati: 40, kdvOrani: 20, miktar: 12 }
    })
    const w = await kur()
    await w.find('.hizli-barkod input').setValue('8690000000001')
    await w.find('.hizli-barkod input').trigger('keyup.enter')
    await nextTick()
    const satir = w.emitted('add')[0][0]
    expect(stokAPI.barkodIleBul).toHaveBeenCalledWith('8690000000001')
    expect(satir.stokId).toBe(55)
    expect(satir.aciklama).toBe('Barkodlu Ürün')
    expect(satir.adet).toBe(1)
    expect(satir.stokMiktar).toBe(12)
    expect(satir.iskontoOrani).toBeNull()
  })

  it('barkod bulunamazsa kalem EKLENMEZ ve mesaj gosterilir', async () => {
    const { stokAPI } = await import('../../api/index.js')
    stokAPI.barkodIleBul.mockResolvedValue({ data: null })
    const w = await kur()
    await w.find('.hizli-barkod input').setValue('YOK999')
    await w.find('.hizli-barkod input').trigger('keyup.enter')
    await nextTick()
    expect(w.emitted('add')).toBeFalsy()
    expect(w.find('.hizli-barkod-mesaj').exists()).toBe(true)
  })

  it('bos barkodda istek atilmaz', async () => {
    const { stokAPI } = await import('../../api/index.js')
    stokAPI.barkodIleBul.mockClear()
    const w = await kur()
    await w.find('.hizli-barkod input').trigger('keyup.enter')
    await nextTick()
    expect(stokAPI.barkodIleBul).not.toHaveBeenCalled()
  })

  it('barkod basarili eklemeden sonra temizlenir (ardisik okutma)', async () => {
    const { stokAPI } = await import('../../api/index.js')
    stokAPI.barkodIleBul.mockResolvedValue({ data: { id: 1, ad: 'X', miktar: 1 } })
    const w = await kur()
    const girdi = w.find('.hizli-barkod input')
    await girdi.setValue('111')
    await girdi.trigger('keyup.enter')
    await nextTick()
    expect(w.find('.hizli-barkod input').element.value).toBe('')
  })
})

describe('FaturaKalemleri — aciklama serbest metindir (tek ekleme yolu)', () => {
  it('satir aciklamasi her zaman duz metin girdisidir (stok aramasi degil)', async () => {
    const w = await kur({ kalemler: [kalem()] })
    expect(w.find('.datatable .inputtext').exists()).toBe(true)
  })

  it('stok-sec olayi ARTIK YOK (satir ici arama kaldirildi)', async () => {
    const w = await kur({ kalemler: [kalem()] })
    expect(w.emitted('stok-sec')).toBeFalsy()
  })
})

describe('FaturaKalemleri — yerlesim kurallari (kayma onlemi)', () => {
  const kaynak = () => readFileSync(join(process.cwd(), 'src/components/FaturaKalemleri.vue'), 'utf8')

  it('hizli ekle satiri TEK SIRA ve ETIKETLI (alanlar hizali)', () => {
    // Once etiketsiz placeholder'lar vardi; alanlar dikey hizasizdi ve barkod
    // ayri tam satira kiriliyordu. Artik her alan `.hizli-alan` sarmalayicisinda
    // ve ustunde etiketi var; barkod normal bir kolon.
    const k = kaynak()
    expect(k).toMatch(/class="hizli-kalem-grid"/)
    expect(k).toMatch(/\.hizli-alan\s*\{/)
    expect(k).toMatch(/\.hizli-alan\s*>\s*label/)
    expect(k).toMatch(/faturaKalemleri\.etiketBarkod/)
    expect(k).toMatch(/faturaKalemleri\.etiketFiyat/)
  })

  it('barkod girisi IKON PAYI ile (ikon placeholder uzerine binmez)', () => {
    expect(kaynak()).toMatch(/\.hizli-barkod :deep\(\.p-inputtext\)\s*\{[^}]*padding-left:\s*2\.25rem/)
  })

  it('orta genislik icin ara breakpoint var (720-1150 arasi tasma onlenir)', () => {
    expect(kaynak()).toMatch(/@media \(max-width: 1150px\)/)
  })

  it('KRITIK: oneri rozeti GLOBAL CSS`te (teleport edilen icerige scoped yetmez)', () => {
    const appCss = readFileSync(join(process.cwd(), 'src/assets/app.css'), 'utf8')
    expect(appCss).toMatch(/\.stok-opsiyon-stok\s*\{/)
    // Bilesenin scoped stilinde OLMAMALI (orada etkisiz kalir)
    expect(kaynak()).not.toMatch(/^\s*\.stok-opsiyon-stok\s*\{/m)
  })
})
