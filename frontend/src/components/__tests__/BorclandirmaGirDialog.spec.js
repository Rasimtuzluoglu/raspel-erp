import { describe, it, expect, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import i18n from '../../i18n.js'
import BorclandirmaGirDialog from '../BorclandirmaGirDialog.vue'

/**
 * Borclandırma dialogu: yon/etki onizlemesi.
 * Borclandirma cari bakiyesini AZALTIR (borcu artirir): 500 -> 400 (tutar 100).
 */

const stubs = {
  Dialog: { template: '<div><slot /><slot name="footer" /></div>' },
  Button: { props: ['label'], template: '<button>{{ label }}<slot /></button>' },
  FormField: { props: ['label'], template: '<div><label>{{ label }}</label><slot /></div>' },
  Select: { template: '<div><slot /></div>' },
  InputNumber: {
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template: '<input class="in" :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))">'
  },
  DatePicker: { template: '<div><slot /></div>' },
  Textarea: { template: '<div><slot /></div>' },
  Message: { template: '<div class="msg"><slot /></div>' }
}

const cari = { id: 1, ad: 'Test Cari', bakiye: 500 }

const kur = () =>
  mount(BorclandirmaGirDialog, {
    props: { visible: true, cari },
    global: { plugins: [createPinia(), ToastService, i18n], stubs }
  })

describe('BorclandirmaGirDialog.vue', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('etki bandi cari borcunun artacagini gosterir', async () => {
    const w = kur()
    await flushPromises()
    const bant = w.find('.etki-bandi')
    expect(bant.exists()).toBe(true)
    expect(bant.text()).toContain(i18n.global.t('cariHesaplar.etkiArtar'))
  })

  it('tutar girilince bakiye onizlemesi hesaplanir (500 -> 400)', async () => {
    const w = kur()
    await flushPromises()
    await w.find('input.in').setValue('100')
    const onizleme = w.find('.etki-bakiye')
    expect(onizleme.exists()).toBe(true)
    expect(onizleme.text()).toContain('400')
  })
})
