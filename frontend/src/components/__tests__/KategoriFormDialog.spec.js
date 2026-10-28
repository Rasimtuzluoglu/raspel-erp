import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import KategoriFormDialog from '../KategoriFormDialog.vue'

const Button = {
  props: ['label', 'icon', 'disabled', 'loading'],
  emits: ['click'],
  template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot />{{ label }}</button>'
}

const stubs = {
  Dialog: {
    props: ['visible', 'header'],
    emits: ['update:visible'],
    template:
      '<div v-if="visible" class="dlg" role="dialog"><h2 class="dlg-baslik">{{ header }}</h2><slot /><slot name="footer" /></div>'
  },
  InputText: {
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template:
      '<input :value="modelValue" v-bind="$attrs" @input="$emit(\'update:modelValue\', $event.target.value)" />'
  },
  Textarea: {
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template:
      '<textarea :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
  },
  InputNumber: {
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template:
      '<input type="number" :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))" />'
  },
  InputSwitch: {
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template:
      '<input type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)" />'
  },
  Dropdown: {
    props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'disabled'],
    emits: ['update:modelValue'],
    template:
      '<select :disabled="disabled" :value="modelValue" @change="degis">' +
      '<option value="">--</option>' +
      '<option v-for="o in options" :key="o[optionValue]" :value="o[optionValue]">{{ o[optionLabel] }}</option>' +
      '</select>',
    methods: {
      degis(olay) {
        const deger = olay.target.value
        const secili = (this.options || []).find((o) => String(o[this.optionValue]) === deger)
        this.$emit('update:modelValue', secili ? secili[this.optionValue] : null)
      }
    }
  },
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' },
  Button
}

const USTLER = [
  { id: 1, ad: 'Satış', ustKategoriId: null, aktif: true, altKategoriSayisi: 0 },
  { id: 2, ad: 'Kira', ustKategoriId: null, aktif: true, altKategoriSayisi: 0 },
  { id: 3, ad: 'Pasif Ust', ustKategoriId: null, aktif: false, altKategoriSayisi: 0 }
]

const kur = (props = {}) =>
  mount(KategoriFormDialog, {
    props: {
      visible: true,
      kapsam: 'KASA_GELIR',
      kategori: null,
      ustKategoriler: USTLER,
      varsayilanUstKategoriId: null,
      kaydediliyor: false,
      ...props
    },
    global: { plugins: [i18n], stubs }
  })

const kaydetButonu = (w) => w.findAll('button').find((b) => b.text().includes('Kaydet'))

describe('KategoriFormDialog.vue', () => {
  it('12 renk paleti ve ikon izgarasini aria-pressed ile sunar', () => {
    const w = kur()
    expect(w.findAll('.renk-nokta')).toHaveLength(12)
    expect(w.findAll('.ikon-secim').length).toBeGreaterThan(10)
    // Varsayilan ilk renk secili.
    expect(w.findAll('.renk-nokta')[0].attributes('aria-pressed')).toBe('true')
    expect(w.find('.ikon-secim').attributes('aria-pressed')).toBe('true')
  })

  it('ad ve renk degistikce canli onizlemeyi gunceller', async () => {
    const w = kur()
    await w.find('#kategori-ad').setValue('Yemek')

    expect(w.find('.onizleme-ad').text()).toBe('Yemek')

    const kirmizi = w.findAll('.renk-nokta')[0]
    await kirmizi.trigger('click')
    expect(kirmizi.attributes('aria-pressed')).toBe('true')
    expect(w.vm.form.renk).toBe('#ef4444')
  })

  it('bos ad ile kaydetmez ve hata gosterir', async () => {
    const w = kur()
    await kaydetButonu(w).trigger('click')

    expect(w.emitted('kaydet')).toBeUndefined()
    expect(w.find('.p-error').text()).toBe('Kategori adı zorunludur')
    expect(w.find('#kategori-ad').attributes('aria-invalid')).toBe('true')
  })

  it('doldurulan alanlari payload olarak yayar (renk, ikon, sira, aktif, ust kategori)', async () => {
    const w = kur()
    await w.find('#kategori-ad').setValue('Yemek')
    await w.find('#kategori-ust').setValue('1')
    await w.findAll('.renk-nokta')[3].trigger('click')
    await w.findAll('.ikon-secim')[4].trigger('click')
    await w.find('input[type="number"]').setValue('7')
    await w.find('textarea').setValue('Günlük yemek gideri')
    await w.find('input[type="checkbox"]').setValue(false)

    await kaydetButonu(w).trigger('click')

    const [payload] = w.emitted('kaydet')[0]
    expect(payload).toMatchObject({
      ad: 'Yemek',
      kapsam: 'KASA_GELIR',
      ustKategoriId: 1,
      sira: 7,
      aciklama: 'Günlük yemek gideri',
      aktif: false
    })
    expect(payload.renk).toMatch(/^#[0-9a-f]{6}$/i)
    expect(payload.ikon).toMatch(/^pi pi-/)
  })

  it('duzenleme modunda alanlari doldurur ve basligi degistirir', () => {
    const w = kur({
      kategori: {
        id: 1,
        ad: 'Satış',
        ustKategoriId: null,
        renk: '#3b82f6',
        ikon: 'pi pi-box',
        sira: 3,
        aciklama: 'Mevcut',
        aktif: true,
        altKategoriSayisi: 0
      }
    })

    expect(w.find('.dlg-baslik').text()).toBe('Kategori Düzenle')
    expect(w.find('#kategori-ad').element.value).toBe('Satış')
    expect(w.find('.onizleme-ad').text()).toBe('Satış')
    // Kendisi ust kategori listesinde yer almaz.
    expect(w.find('#kategori-ust').text()).not.toContain('Satış')
    expect(w.find('#kategori-ust').text()).toContain('Kira')
  })

  it('alt kategorisi olan kayitta ust kategori secimini kilitler', () => {
    const w = kur({
      kategori: {
        id: 1,
        ad: 'Satış',
        ustKategoriId: null,
        renk: '#3b82f6',
        ikon: 'pi pi-box',
        sira: 1,
        aktif: true,
        altKategoriSayisi: 2
      }
    })

    expect(w.find('#kategori-ust').attributes('disabled')).toBeDefined()
    expect(w.text()).toContain('Alt kategorisi olan bir kategori üst kategoriye taşınamaz')
  })

  it('MASRAF kapsaminda adi buyuk harfe cevirir', async () => {
    const w = kur({ kapsam: 'MASRAF' })
    await w.find('#kategori-ad').setValue('yakıt')

    await kaydetButonu(w).trigger('click')

    const [payload] = w.emitted('kaydet')[0]
    expect(payload.ad).toBe('YAKIT')
    expect(payload.kapsam).toBe('MASRAF')
  })

  it('vazgec dialogu kapatir', async () => {
    const w = kur()
    const iptal = w.findAll('button').find((b) => b.text().includes('İptal'))
    await iptal.trigger('click')

    expect(w.emitted('update:visible')).toEqual([[false]])
  })

  it('kaydediliyor iken cift gonderim yapmaz', async () => {
    const w = kur({ kaydediliyor: true })
    await w.find('#kategori-ad').setValue('Yemek')
    await kaydetButonu(w).trigger('click')

    expect(w.emitted('kaydet')).toBeUndefined()
  })
})
