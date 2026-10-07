import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PosMusteriPaneli from '../PosMusteriPaneli.vue'
import i18n from '../../i18n.js'

/**
 * POS MUSTERI + TESLIMAT PANELI — sozlesme testleri.
 *
 * Mevcut davranis "karakterizasyon" olarak sabitleniyor: buradaki testler
 * su an calisan kurali kayda gecer. Ozellikle asagidaki is kurali bilincli:
 *
 *   TESLIMAT ADRESI yalniz `seciliSofor` secildiginde render edilir.
 *
 * Bu, planlanan Faz5 duzeltmesinin (gizli zorunlu alan) hedefi: su an sofor
 * secilip panel kapaliyken adres alani HIC gorunmez, kullanici "Satisi
 * Tamamla" dediginde satir dogrulanip panel zorla acilir ve kasa durur. Test
 * mevcut kurali sabitledigi icin Faz5'te bu davranis bilincli olarak
 * DEGISTIRILECK ve test de birlikte guncellenecek.
 */

const stubs = {
  Button: {
    name: 'Button',
    props: ['label', 'icon', 'severity'],
    template: '<button><slot />{{ label }}</button>'
  },
  SelectButton: {
    name: 'SelectButton',
    props: ['modelValue', 'options', 'optionLabel', 'optionValue'],
    emits: ['update:modelValue'],
    template:
      '<div class="selectbutton"><button v-for="o in options" :key="String(o[optionValue])" class="sb-opt" @click="$emit(\'update:modelValue\', o[optionValue])">{{ o[optionLabel] }}</button></div>'
  },
  AutoComplete: {
    props: ['modelValue', 'suggestions', 'optionLabel', 'placeholder'],
    emits: ['update:modelValue', 'complete', 'option-select'],
    template: '<div class="autocomplete"><input class="ac-input" /></div>'
  },
  InputText: {
    props: ['modelValue', 'placeholder', 'id'],
    emits: ['update:modelValue'],
    template: '<input class="inputtext" :id="id" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
  },
  Dropdown: {
    props: ['modelValue', 'options', 'optionLabel', 'loading', 'id'],
    emits: ['update:modelValue'],
    template:
      '<select class="dropdown" :id="id"><option v-for="o in options" :key="o[optionLabel]" :value="o[optionLabel]">{{ o[optionLabel] }}</option></select>'
  }
}

const kur = (props = {}) =>
  mount(PosMusteriPaneli, { props, global: { plugins: [i18n], stubs } })

const MODLAR = [
  { label: 'Perakende', value: 'perakende' },
  { label: 'Müşteri', value: 'musteri' }
]

const musteriModlu = (ek = {}) => ({ musteriModu: 'musteri', musteriModlari: MODLAR, ...ek })

describe('PosMusteriPaneli — müşteri modu', () => {
  it('perakende modunda müşteri araması tamamen gizlidir', () => {
    const w = kur({ musteriModu: 'perakende', musteriModlari: MODLAR })
    expect(w.find('.autocomplete').exists()).toBe(false)
    expect(w.find('.secili-musteri-chip').exists()).toBe(false)
  })

  it('müşteri modunda arama alani gorunur', () => {
    const w = kur({ musteriModu: 'musteri', musteriModlari: MODLAR })
    expect(w.find('.autocomplete').exists()).toBe(true)
  })

  it('mod degisince update:musteriModu yayar', async () => {
    const w = kur({ musteriModu: 'perakende', musteriModlari: MODLAR })
    const musteri = w.findAll('.sb-opt').find((b) => b.text() === 'Müşteri')
    expect(musteri).toBeTruthy()
    await musteri.trigger('click')
    expect(w.emitted('update:musteriModu')).toEqual([['musteri']])
  })
})

describe('PosMusteriPaneli — seçili müşteri', () => {
  it('secili musteri rozeti gorunur ve kaldirilabilir', async () => {
    const w = kur(musteriModlu({ seciliMusteri: { id: 5, ad: 'ABC Ltd' } }))
    expect(w.find('.secili-musteri-ad').text()).toBe('ABC Ltd')
    await w.find('.secili-musteri-sil').trigger('click')
    expect(w.emitted('musteri-temizle')).toBeTruthy()
  })

  it('bakiye uyarisi seviyesiyle gosterilir', () => {
    const w = kur(
      musteriModlu({
        seciliMusteri: { id: 5, ad: 'ABC' },
        musteriBakiyeUyarisi: { seviye: 'danger', mesaj: 'Kredi limiti asildi' }
      })
    )
    const uyari = w.find('.musteri-bakiye-uyari')
    expect(uyari.exists()).toBe(true)
    expect(uyari.classes()).toContain('danger')
    expect(uyari.text()).toContain('Kredi limiti asildi')
  })

  it('degisim modunda bilgi seridi ve kapatma dugmesi gorunur', async () => {
    const w = kur(musteriModlu({ degisimIadeId: 77 }))
    expect(w.find('.degisim-bilgi').text()).toContain('77')
    await w.find('.degisim-kapat').trigger('click')
    expect(w.emitted('degisim-kapat')).toBeTruthy()
  })

  it('yeni musteri dugmesi yeni-musteri yayar', async () => {
    const w = kur(musteriModlu())
    const yeni = w.findComponent({ name: 'Button' })
    expect(yeni).toBeTruthy()
    await yeni.trigger('click')
    expect(w.emitted('yeni-musteri')).toBeTruthy()
  })
})

describe('PosMusteriPaneli — teslimat paneli', () => {
  it('panel kapaliyken sofor alanlari render edilmez', () => {
    const w = kur({ teslimatAcik: false, soforler: [{ ad: 'Ali' }] })
    expect(w.find('#hizli-teslim-sofor').exists()).toBe(false)
    expect(w.find('.teslimat-ipucu').exists()).toBe(false)
  })

  it('baslik update:teslimatAcik yayar', async () => {
    const w = kur({ teslimatAcik: false })
    await w.findAll('.katlanir-baslik')[1].trigger('click')
    expect(w.emitted('update:teslimatAcik')).toEqual([[true]])
  })

  it('panel kapaliyken secili sofor rozeti baslikta gorunur', () => {
    const w = kur({ teslimatAcik: false, seciliSofor: { ad: 'Ali V.' } })
    expect(w.find('.katlanir-rozet').text()).toBe('Ali V.')
  })

  // FAZ5 NOTU: su an sofor seciliyse adres ALANI render ediliyor ama
  // teslimatAdresi bos birakilirsa kayit durur. Faz5'te alan, sofor secilir
  // secilmez belirgin hale gelecek.
  it('sofor secildiginde adres alani gorunur ve zorunlu isaretlidir', () => {
    const w = kur({ teslimatAcik: true, seciliSofor: { ad: 'Ali V.' }, teslimatAdresi: '' })
    expect(w.find('#hizli-teslim-adres').exists()).toBe(true)
    expect(w.find('.zorunlu').exists()).toBe(true)
  })

  it('sofor secilmeden adres alani gizlenir (mevcut kural)', () => {
    const w = kur({ teslimatAcik: true, seciliSofor: null })
    expect(w.find('#hizli-teslim-adres').exists()).toBe(false)
  })

  it('adres yazilince update:teslimatAdresi yayar', async () => {
    const w = kur({ teslimatAcik: true, seciliSofor: { ad: 'Ali' }, teslimatAdresi: '' })
    await w.find('#hizli-teslim-adres').setValue('Ataturk Cad. No 5')
    expect(w.emitted('update:teslimatAdresi')).toEqual([['Ataturk Cad. No 5']])
  })

  it('sofor yuklenirken sofor secimi gorunur', () => {
    const w = kur({ teslimatAcik: true, soforlerYukleniyor: true, soforler: [] })
    expect(w.find('#hizli-teslim-sofor').exists()).toBe(true)
  })

  it('teslim durumu secimi update:teslimDurumu yayar', async () => {
    const w = kur({
      teslimatAcik: true,
      seciliSofor: { ad: 'Ali' },
      teslimDurumu: 'BEKLIYOR',
      teslimDurumSecenekleri: [
        { label: 'Bekliyor', value: 'BEKLIYOR' },
        { label: 'Yolda', value: 'YOLDA' }
      ]
    })
    const secenekler = w.findAll('.teslim-durum-secim .sb-opt')
    expect(secenekler).toHaveLength(2)
    await secenekler[1].trigger('click')
    expect(w.emitted('update:teslimDurumu')).toEqual([['YOLDA']])
  })
})