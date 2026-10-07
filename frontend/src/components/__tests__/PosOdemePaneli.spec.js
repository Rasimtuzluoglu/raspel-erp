import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PosOdemePaneli from '../PosOdemePaneli.vue'
import i18n from '../../i18n.js'

/**
 * POS ODEME PANELI — sozlesme testleri.
 *
 * Odeme bilgisi (taksit, kasa, banka, POS terminali, komisyon) view'da durur;
 * panel yalnizca OLAY YAYAR ve hesaplanan degerleri gosterir. Faz1'de bu
 * mantik `usePosOdeme` composable'ina tasinacak; tasima sirasinda olay
 * ADLARI ve secenek listelerinin prop olarak tasinmasi korunmali.
 *
 * Panelin gizleme kurallari onemli bir is kuralidir: taksit alanlari yalniz
 * odeme yontemi TAKSIT iken, banka/POS alanlari KART-HAVALE iken gorunur.
 * Yanlis gizlenen alan "gizli zorunlu alan" hatasini dogurur; bu yuzden
 * gizleme davranislari ayri testlerle sabitlendi.
 */

const stubs = {
  Button: { props: ['label', 'icon'], template: '<button><slot />{{ label }}</button>' },
  SelectButton: {
    props: ['modelValue', 'options', 'optionLabel', 'optionValue'],
    emits: ['update:modelValue'],
    template:
      '<div class="selectbutton"><button v-for="o in options" :key="String(o[optionValue])" class="sb-opt" @click="$emit(\'update:modelValue\', o[optionValue])">{{ o[optionLabel] }}</button></div>'
  },
  InputNumber: {
    props: ['modelValue', 'min', 'max', 'suffix'],
    emits: ['update:modelValue'],
    template:
      '<input class="inputnumber" :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))" />'
  },
  InputText: {
    props: ['modelValue', 'placeholder'],
    emits: ['update:modelValue'],
    template:
      '<input class="inputtext" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
  },
  Dropdown: {
    props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder'],
    emits: ['update:modelValue'],
    template:
      '<select class="dropdown" @change="$emit(\'update:modelValue\', $event.target.value)"><option value=""></option><option v-for="o in options" :key="o[optionValue]" :value="o[optionValue]">{{ o[optionLabel] }}</option></select>'
  },
  Tag: { props: ['value', 'severity'], template: '<span class="tag">{{ value }}</span>' }
}

const kur = (props = {}) =>
  mount(PosOdemePaneli, { props, global: { plugins: [i18n], stubs } })

const ODEME_TIPLERI = [
  { label: 'Tam', value: 'tam' },
  { label: 'Yarım', value: 'yarim' },
  { label: 'Alacak', value: 'yok' }
]

const ODEME_YONTEMLERI = [
  { label: 'Nakit', value: 'NAKIT', icon: 'pi pi-wallet' },
  { label: 'Kart', value: 'KART', icon: 'pi pi-credit-card' },
  { label: 'Havale', value: 'HAVALE', icon: 'pi pi-send' },
  { label: 'Taksit', value: 'TAKSIT', icon: 'pi pi-calendar' }
]

const temel = { genelToplam: 500, odemeTipleri: ODEME_TIPLERI, odemeYontemleri: ODEME_YONTEMLERI }

describe('PosOdemePaneli — odeme durumu', () => {
  it('odeme durumu secimi update:odemeDurumu yayar', async () => {
    const w = kur({ ...temel, odemeDurumu: 'tam' })
    const yarim = w.findAll('.sb-opt').find((b) => b.text() === 'Yarım')
    expect(yarim).toBeTruthy()
    await yarim.trigger('click')
    expect(w.emitted('update:odemeDurumu')).toEqual([['yarim']])
  })

  it('odeme durumu etiketi gorunur', () => {
    const w = kur({ ...temel, odemeDurumText: 'Ödendi' })
    expect(w.find('.tag').text()).toBe('Ödendi')
  })

  it('"odeme yok" secilince tutar/yontem alanlari gizlenir', () => {
    const w = kur({ ...temel, odemeDurumu: 'yok' })
    // Odeme durumu ve kasa secimi kalir; odenen tutar + yontem gizlenir.
    expect(w.findAll('.odeme-yontem-btn')).toHaveLength(0)
    expect(w.findAll('input.inputnumber')).toHaveLength(0)
  })

  it('kasa secimi odeme yokken de gorunur (kasa gunlugu icin gerekli)', () => {
    const w = kur({ ...temel, odemeDurumu: 'yok', kasalar: [{ id: 1, ad: 'Kasa 1' }] })
    expect(w.find('select.dropdown').exists()).toBe(true)
  })
})

describe('PosOdemePaneli — odenen tutar, nakit, para ustu', () => {
  it('odenen tutar degisince update:odenenTutar yayar', async () => {
    const w = kur({ ...temel, odemeDurumu: 'yarim', odenenTutar: 0 })
    await w.findAll('input.inputnumber')[0].setValue('300')
    expect(w.emitted('update:odenenTutar')).toBeTruthy()
  })

  it('NAKIT yonteminde alinan nakit alani gorunur ve update:alinanNakit yayar', async () => {
    const w = kur({ ...temel, odemeDurumu: 'yarim', odemeYontemi: 'NAKIT', alinanNakit: 0 })
    const nakitGirdi = w.findAll('input.inputnumber')[1]
    await nakitGirdi.setValue('1000')
    expect(w.emitted('update:alinanNakit')).toBeTruthy()
  })

  it('KART yonteminde alinan nakit alani gizlidir', () => {
    const w = kur({ ...temel, odemeDurumu: 'yarim', odemeYontemi: 'KART' })
    expect(w.find('.hizli-nakit').exists()).toBe(false)
  })

  it('para ustu yalniz pozitifse gorunur', () => {
    const w = kur({ ...temel, odemeYontemi: 'NAKIT', paraUstu: 0 })
    expect(w.find('.para-ustu').exists()).toBe(false)
  })

  it('para ustu pozitifse gorunur', () => {
    const w = kur({ ...temel, odemeYontemi: 'NAKIT', alinanNakit: 1000, paraUstu: 500 })
    expect(w.find('.para-ustu').exists()).toBe(true)
  })

  it('hizli nakit butonu mevcut tutari uzerine ekler', async () => {
    const w = kur({
      ...temel,
      genelToplam: 750,
      odemeYontemi: 'NAKIT',
      alinanNakit: 200,
      hizliNakit: [50, 100]
    })
    const btn = w.findAll('.hizli-nakit-btn').find((b) => b.text() === '50')
    await btn.trigger('click')
    expect(w.emitted('update:alinanNakit')).toEqual([[250]])
  })

  it('"nakit tam" butonu toplami alinan nakit yapar', async () => {
    const w = kur({ ...temel, genelToplam: 750, odemeYontemi: 'NAKIT', alinanNakit: 0 })
    const tam = w.find('.hizli-nakit-btn.tam')
    await tam.trigger('click')
    expect(w.emitted('update:alinanNakit')).toEqual([[750]])
  })
})

describe('PosOdemePaneli — odeme yontemi ve taksit', () => {
  it('odeme yontemi butonu update:odemeYontemi yayar', async () => {
    const w = kur({ ...temel, odemeDurumu: 'tam', odemeYontemi: 'NAKIT' })
    const kart = w.findAll('.odeme-yontem-btn').find((b) => b.text().includes('Kart'))
    expect(kart).toBeTruthy()
    await kart.trigger('click')
    expect(w.emitted('update:odemeYontemi')).toEqual([['KART']])
  })

  it('aktif odeme yontemi isaretlenir', () => {
    const w = kur({ ...temel, odemeDurumu: 'tam', odemeYontemi: 'KART' })
    const aktif = w.findAll('.odeme-yontem-btn').filter((b) => b.classes().includes('active'))
    expect(aktif).toHaveLength(1)
    expect(aktif[0].text()).toContain('Kart')
  })

  it('taksit paneli yalniz TAKSIT yonteminde gorunur', () => {
    const w = kur({ ...temel, odemeDurumu: 'tam', odemeYontemi: 'NAKIT', taksitSayisi: 6 })
    expect(w.find('.taksit-panel').exists()).toBe(false)
  })

  it('TAKSIT yonteminde kurum, tutar ve sayi alanlari gorunur', async () => {
    const w = kur({
      ...temel,
      odemeDurumu: 'yarim',
      odemeYontemi: 'TAKSIT',
      taksitKurum: 'Garanti',
      taksitTutar: 200,
      taksitSayisi: 6
    })
    const panel = w.find('.taksit-panel')
    expect(panel.exists()).toBe(true)
    // Kurum bir `InputText`, tutar/sayi `InputNumber`: degerler DOM metnine
    // degil input.value'a yazildigi icin input uzerinden dogrulanir.
    const sayilar = panel.findAll('input.inputnumber')
    expect(panel.find('input.inputtext').element.value).toBe('Garanti')
    expect(sayilar[0].element.value).toBe('200')
    expect(sayilar[1].element.value).toBe('6')

    await sayilar[1].setValue('12')
    expect(w.emitted('update:taksitSayisi')).toBeTruthy()
  })
})

describe('PosOdemePaneli — kasa, banka ve POS hedefleri', () => {
  it('kasa secilince update:seciliKasa yayar', async () => {
    const w = kur({ ...temel, odemeDurumu: 'tam', kasalar: [{ id: 1, ad: 'Kasa 1' }, { id: 2, ad: 'Kasa 2' }] })
    await w.find('select.dropdown').setValue('2')
    expect(w.emitted('update:seciliKasa')).toBeTruthy()
  })

  it('KART/HAVALE yonteminde banka secimi gorunur', () => {
    const w = kur({ ...temel, odemeDurumu: 'tam', odemeYontemi: 'KART', bankalar: [{ id: 5, ad: 'Ziraat' }] })
    expect(w.findAll('select.dropdown').length).toBeGreaterThanOrEqual(2)
  })

  it('NAKIT yonteminde banka secimi gizlidir', () => {
    const w = kur({ ...temel, odemeDurumu: 'tam', odemeYontemi: 'NAKIT', bankalar: [{ id: 5, ad: 'Ziraat' }] })
    expect(w.findAll('select.dropdown')).toHaveLength(1)
  })

  it('POS terminali secilince komisyon notu gorunur', () => {
    const w = kur({
      ...temel,
      genelToplam: 1000,
      odemeDurumu: 'tam',
      odemeYontemi: 'KART',
      seciliPos: 3,
      posTerminalleri: [{ id: 3, ad: 'Terminal 1' }],
      seciliPosBilgi: { komisyonOrani: 2.5 },
      hesaplananKomisyon: 25
    })
    const not = w.find('.pos-komisyon-not')
    expect(not.exists()).toBe(true)
    expect(not.text()).toContain('25')
  })

  it('POS terminali secilmediginde komisyon notu gizlenir', () => {
    const w = kur({ ...temel, genelToplam: 1000, odemeDurumu: 'tam', odemeYontemi: 'KART' })
    expect(w.find('.pos-komisyon-not').exists()).toBe(false)
  })
})

describe('PosOdemePaneli — kalan tutar ve katlama', () => {
  it('kalan tutar pozitifse gorunur', () => {
    const w = kur({ ...temel, kalanTutar: 150 })
    expect(w.find('.odeme-kalan').exists()).toBe(true)
  })

  it('kalan tutar sifirsa gizlenir', () => {
    const w = kur({ ...temel, kalanTutar: 0 })
    expect(w.find('.odeme-kalan').exists()).toBe(false)
  })

  it('baslik toggle yayar', async () => {
    const w = kur({ ...temel })
    await w.find('.katlanir-baslik').trigger('click')
    expect(w.emitted('toggle')).toBeTruthy()
  })

  it('kapaliyken icerik v-show ile gizlenir', () => {
    const w = kur({ ...temel, acik: false })
    expect(w.find('.odeme-icerik').attributes('style')).toContain('display: none')
  })
})