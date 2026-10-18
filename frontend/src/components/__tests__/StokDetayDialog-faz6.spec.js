import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import i18n from '../../i18n.js'
import StokDetayDialog from '../StokDetayDialog.vue'

/**
 * FAZ 6 — Stok 360 tek detay: baslik aksiyonlari (Giris/Cikis/Transfer/Etiket)
 * ve hareket silme. Satir ici panel kaldirildi; tum aksiyonlar bu pencerede.
 */

vi.mock('../../api/index.js', () => ({
  stokAPI: {
    analiz: vi.fn().mockResolvedValue({ data: {} }),
    islemGecmisiSayfali: vi.fn().mockResolvedValue({ data: { satirlar: [], toplam: 0 } }),
    aylikFiyat: vi.fn().mockResolvedValue({ data: [] }),
    musteriAnaliz: vi.fn().mockResolvedValue({ data: [] }),
    tedarikciAnaliz: vi.fn().mockResolvedValue({ data: [] }),
    getFiyatlar: vi.fn().mockResolvedValue({ data: [] })
  },
  depoAPI: { transfer: vi.fn().mockResolvedValue({ data: {} }) }
}))

vi.mock('vue-chartjs', () => ({
  Line: { props: ['data', 'options'], template: '<div />' },
  Bar: { template: '<div />' },
  Doughnut: { template: '<div />' }
}))

const stubs = {
  Dialog: { template: '<div class="dlg"><slot name="header" /><slot /><slot name="footer" /></div>' },
  Button: { props: ['label', 'icon'], emits: ['click'], template: '<button @click="$emit(\'click\')"><i v-if="icon" :class="icon" />{{ label }}<slot /></button>' },
  Dropdown: true,
  InputNumber: true,
  DatePicker: true,
  Textarea: true,
  TabView: { template: '<div><slot /></div>' },
  TabPanel: { template: '<div><slot /></div>' },
  AppDataTable: { template: '<div><slot /></div>' },
  Column: { template: '<div><slot name="body" :data="{ id: 9, tur: \'GIRIS\', miktar: 1 }" /></div>' },
  EmptyState: true
}

const kur = () =>
  mount(StokDetayDialog, {
    props: {
      visible: true,
      stok: { id: 1, ad: 'PVC Bant', birim: 'Rulo', miktar: 10, minMiktar: 5, fiyat: 100, satisFiyati: 150 },
      hareketler: [{ id: 9, tur: 'GIRIS', miktar: 1, aciklama: 'test' }],
      depolar: [{ id: 1, ad: 'Merkez' }]
    },
    global: { plugins: [createPinia(), ToastService, i18n], stubs }
  })

describe('StokDetayDialog — Faz6 tek detay', () => {
  beforeEach(() => vi.clearAllMocks())

  it('baslik aksiyonlari (Giris/Cikis/Transfer/Etiket) bulunur', async () => {
    const w = kur()
    await flushPromises()
    const t = (k) => i18n.global.t(k)
    const metin = w.find('.detay-baslik').text()
    expect(metin).toContain(t('stoklar.stokGiris'))
    expect(metin).toContain(t('stoklar.stokCikis'))
    expect(metin).toContain(t('depolar.depolarArasiTransfer'))
    expect(metin).toContain(t('stoklar.barkodEtiket'))
  })

  it('giris/cikis aksiyonlari olay yayar', async () => {
    const w = kur()
    await flushPromises()
    const btn = (etiket) => w.findAll('button').find((b) => b.text().includes(etiket))
    await btn(i18n.global.t('stoklar.stokGiris')).trigger('click')
    await btn(i18n.global.t('stoklar.stokCikis')).trigger('click')
    expect(w.emitted('giris')).toBeTruthy()
    expect(w.emitted('cikis')).toBeTruthy()
  })

  it('hareket silme olayi yayar', async () => {
    const w = kur()
    await flushPromises()
    const sil = w.findAll('button').find((b) => b.find('.pi-trash').exists())
    expect(sil).toBeTruthy()
    await sil.trigger('click')
    expect(w.emitted('hareket-sil')).toEqual([[9]])
  })

  it('modal sabit boyut kullanir (sekmeler boyutu degistirmez)', async () => {
    const w = kur()
    await flushPromises()
    const stil = w.find('.dlg').attributes('style') || ''
    expect(stil).toContain('width: 1020px')
    expect(stil).toContain('height')
  })
})
