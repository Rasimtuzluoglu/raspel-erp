import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import StokTopluFiyatDialog from '../StokTopluFiyatDialog.vue'
import StokRaporDialoglari from '../StokRaporDialoglari.vue'

/**
 * FAZ 5 — Ikincil stok dialoglari: ikonlu ortak baslik (`.dialog-baslik`).
 */

const stubs = {
  Dialog: { template: '<div class="dlg"><slot name="header" /><slot /><slot name="footer" /></div>' },
  Button: { props: ['label'], template: '<button>{{ label }}<slot /></button>' },
  Dropdown: true,
  InputNumber: true,
  InputText: true,
  AutoComplete: true,
  DataTable: { template: '<div><slot /></div>' },
  Column: true,
  EmptyState: true
}

describe('Stok ikincil dialoglari — Faz5', () => {
  it('toplu fiyat dialogu ikonlu baslik kullanir', () => {
    const w = mount(StokTopluFiyatDialog, {
      props: { visible: true },
      global: { plugins: [i18n], stubs }
    })
    expect(w.find('.dialog-baslik').exists()).toBe(true)
  })

  it('rapor dialoglari ikonlu baslik kullanir', () => {
    const w = mount(StokRaporDialoglari, {
      props: { degerlemeVisible: true, oneriVisible: true },
      global: { plugins: [i18n], stubs }
    })
    expect(w.findAll('.dialog-baslik').length).toBeGreaterThanOrEqual(2)
  })
})
