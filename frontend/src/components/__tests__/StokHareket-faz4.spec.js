import { describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import i18n from '../../i18n.js'
import StokHareketDialog from '../StokHareketDialog.vue'

/**
 * FAZ 4 — Stok hareketi: yon bandi (giris/cikis).
 */

vi.mock('../../api/index.js', () => ({
  cariHesapAPI: { filtreli: vi.fn().mockResolvedValue({ data: { content: [] } }) },
  depoAPI: { transfer: vi.fn().mockResolvedValue({ data: {} }) }
}))

const stubs = {
  Dialog: { template: '<div class="dlg"><slot name="header" /><slot /><slot name="footer" /></div>' },
  Button: { props: ['label', 'loading'], emits: ['click'], template: '<button @click="$emit(\'click\')">{{ label }}</button>' },
  InputNumber: true,
  DatePicker: true,
  Dropdown: true,
  Textarea: true,
  DataTable: { template: '<div><slot /></div>' },
  Column: true,
  EmptyState: true
}

describe('StokHareketDialog — yon bandi', () => {
  const kur = (tur) =>
    mount(StokHareketDialog, {
      props: { visible: true, baslik: 'Stok', tur, depolar: [] },
      global: { plugins: [createPinia(), ToastService, i18n], stubs }
    })

  it('girişte pozitif yon bandi gosterir', async () => {
    const w = kur('GIRIS')
    await flushPromises()
    const bant = w.find('.hareket-bandi.pozitif')
    expect(bant.exists()).toBe(true)
    expect(bant.text()).toContain(i18n.global.t('stoklar.girisEtki'))
  })

  it('çıkışta negatif yon bandi gosterir', async () => {
    const w = kur('CIKIS')
    await flushPromises()
    const bant = w.find('.hareket-bandi.negatif')
    expect(bant.exists()).toBe(true)
    expect(bant.text()).toContain(i18n.global.t('stoklar.cikisEtki'))
  })
})
