import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import CariKartDialog from '../cari/CariKartDialog.vue'

const stubs = {
  Dialog: {
    props: ['visible'],
    template: '<div v-if="visible" class="p-dialog"><slot name="header" /><slot /><slot name="footer" /></div>'
  },
  Button: {
    props: ['label'],
    emits: ['click'],
    template: '<button class="test-button" @click="$emit(\'click\')">{{ label }}</button>'
  },
  TabView: { template: '<div class="tabs"><slot /></div>' },
  TabPanel: { props: ['header'], template: '<section class="tab"><span class="tab-header">{{ header }}</span><slot /></section>' },
  CariHareketBasligi: {
    emits: ['ekstre'],
    template: '<button class="ekstre" @click="$emit(\'ekstre\', { baslangic: \'2026-01-01\', bitis: null })">ekstre</button>'
  },
  CariHareketTablosu: { template: '<div class="hareket-tablo"><slot /></div>' },
  CariFaturalarSekmesi: {
    emits: ['fatura-detay'],
    template: '<button class="fatura-detay" @click="$emit(\'fatura-detay\', { id: 1 })">fatura</button>'
  },
  CariOzelFiyatlarSekmesi: {
    emits: ['ekle', 'sil'],
    template: '<div><button class="fiyat-ekle" @click="$emit(\'ekle\', { stokId: 1, tutar: 10 })">ekle</button><button class="fiyat-sil" @click="$emit(\'sil\', { id: 1 })">sil</button></div>'
  },
  CariUrunGecmisiSekmesi: {
    emits: ['goster', 'kapat'],
    template: '<div><button class="urun-goster" @click="$emit(\'goster\', { id: 1 })">goster</button><button class="urun-kapat" @click="$emit(\'kapat\')">kapat</button></div>'
  },
  CariNotlarSekmesi: {
    emits: ['ekle', 'sil'],
    template: '<div><button class="not-ekle" @click="$emit(\'ekle\', { metin: \'x\' })">ekle</button><button class="not-sil" @click="$emit(\'sil\', { id: 1 })">sil</button></div>'
  }
}

const mountDialog = (props = {}) => mount(CariKartDialog, {
  global: { plugins: [i18n], stubs },
  props: {
    visible: true,
    cari: { id: 1, ad: 'Cari A' },
    ozet: {},
    hareketler: [],
    bakiye: [],
    yukleniyor: false,
    faturalar: [],
    ozelFiyatlar: [],
    sonUrunler: [],
    seciliUrun: null,
    notlar: [],
    ...props
  }
})

describe('CariKartDialog', () => {
  it('basligi, hareket tablosunu ve sekmeleri gosterir', () => {
    const wrapper = mountDialog()

    expect(wrapper.find('.hareket-baslik').text()).toContain('Hareketler')
    expect(wrapper.find('.hareket-tablo').exists()).toBe(true)
    expect(wrapper.findAll('.tab')).toHaveLength(4)
    expect(wrapper.text()).toContain('Faturalar')
    expect(wrapper.text()).toContain('Notlar')
  })

  it('ekstre, fatura ve sekme olaylarini yukari iletir', async () => {
    const wrapper = mountDialog()

    await wrapper.find('.ekstre').trigger('click')
    await wrapper.find('.fatura-detay').trigger('click')
    await wrapper.find('.not-ekle').trigger('click')
    await wrapper.find('.not-sil').trigger('click')

    expect(wrapper.emitted('ekstre')[0][0].baslangic).toBe('2026-01-01')
    expect(wrapper.emitted('fatura-detay')[0][0]).toEqual({ id: 1 })
    expect(wrapper.emitted('not-ekle')[0][0]).toEqual({ metin: 'x' })
    expect(wrapper.emitted('not-sil')[0][0]).toEqual({ id: 1 })
  })

  it('kapat butonu visible false yayar', async () => {
    const wrapper = mountDialog()

    await wrapper.find('.test-button').trigger('click')

    expect(wrapper.emitted('update:visible')[0]).toEqual([false])
  })

  it('gizliyken dialog govdesini gostermez', () => {
    const wrapper = mountDialog({ visible: false })

    expect(wrapper.find('.p-dialog').exists()).toBe(false)
  })
})
