import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import i18n from '../../i18n.js'
import CariHesapFormDialog from '../cari/CariHesapFormDialog.vue'

const { ekle, guncelle } = vi.hoisted(() => ({
  ekle: vi.fn(),
  guncelle: vi.fn()
}))

vi.mock('../../stores/cariHesapStore.js', () => ({
  useCariHesapStore: () => ({
    cariHesaplar: [],
    addCariHesap: ekle,
    updateCariHesap: guncelle
  })
}))

vi.mock('../../stores/authStore.js', () => ({
  useAuthStore: () => ({ sirketId: 1 })
}))

vi.mock('../../composables/useToastBildirim.js', () => ({
  useToastBildirim: () => ({
    basarili: vi.fn(),
    hata: vi.fn(),
    uyari: vi.fn()
  })
}))

vi.mock('../../api/index.js', () => ({
  kullaniciAPI: { getAll: vi.fn().mockResolvedValue({ data: { content: [] } }) },
  uploadAPI: { foto: vi.fn() },
  cariHesapAPI: { getById: vi.fn() }
}))

const InputStub = {
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<input v-bind="$attrs" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
}

const stubs = {
  Dialog: {
    props: ['visible', 'header'],
    template: '<div v-if="visible" class="p-dialog"><span class="p-dialog-title">{{ header }}</span><slot /><slot name="footer" /></div>'
  },
  InputText: InputStub,
  InputNumber: InputStub,
  Textarea: InputStub,
  Dropdown: true,
  InputSwitch: true,
  Button: {
    props: ['label'],
    emits: ['click'],
    template: '<button @click="$emit(\'click\')">{{ label }}</button>'
  }
}

const mountDialog = (props = {}) => mount(CariHesapFormDialog, {
  global: { plugins: [i18n], stubs },
  props: { visible: true, ...props }
})

describe('CariHesapFormDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    ekle.mockResolvedValue({ data: { id: 99 } })
    guncelle.mockResolvedValue({ data: { id: 5 } })
  })

  it('yeni kayit modunda bos formu ve yeni basligi gosterir', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    expect(wrapper.find('.dialog-form').exists()).toBe(true)
    expect(wrapper.find('.p-dialog-title').text()).toContain('Yeni Cari Hesap')
    expect(wrapper.find('#ad').element.value).toBe('')
  })

  it('zorunlu ad bosken kaydetmez ve hata gosterir', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    await wrapper.vm.kaydet()
    await flushPromises()

    expect(ekle).not.toHaveBeenCalled()
    expect(wrapper.find('.form-hata-ozet').exists()).toBe(true)
    expect(wrapper.find('#ad-hata').exists()).toBe(true)
  })

  it('yeni kaydi store uzerinden ekler ve kaydedildi yayar', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    await wrapper.find('#ad').setValue('Yeni Cari')
    await wrapper.vm.kaydet()
    await flushPromises()

    expect(ekle).toHaveBeenCalledTimes(1)
    expect(ekle.mock.calls[0][0]).toMatchObject({ ad: 'Yeni Cari', paraBirimi: 'TRY' })
    expect(wrapper.emitted('kaydedildi')).toHaveLength(1)
    expect(wrapper.emitted('update:visible')[0]).toEqual([false])
  })

  it('duzenleme modunda mevcut veriyi doldurur ve gunceller', async () => {
    const wrapper = mountDialog({
      cariId: 5,
      baslangicVerisi: {
        id: 5,
        ad: 'Mevcut Cari',
        iban: 'TR330006100519786457841326',
        krediLimiti: 1000,
        aktif: true
      }
    })
    await flushPromises()

    expect(wrapper.find('.p-dialog-title').text()).toContain('Düzenle')
    expect(wrapper.find('#ad').element.value).toBe('Mevcut Cari')
    expect(wrapper.find('#iban').element.value).toBe('TR330006100519786457841326')

    await wrapper.vm.kaydet()
    await flushPromises()

    expect(guncelle).toHaveBeenCalledTimes(1)
    expect(guncelle.mock.calls[0][0]).toBe(5)
    expect(guncelle.mock.calls[0][1]).toMatchObject({ ad: 'Mevcut Cari' })
    expect(ekle).not.toHaveBeenCalled()
  })

  it('kopya verisini yeni kayit olarak kaydeder', async () => {
    const wrapper = mountDialog({
      baslangicVerisi: {
        ad: 'ABC (Kopya)',
        iban: '',
        vergiNumarasi: '',
        aktif: true
      }
    })
    await flushPromises()

    expect(wrapper.find('#ad').element.value).toBe('ABC (Kopya)')
    await wrapper.vm.kaydet()
    await flushPromises()

    expect(ekle).toHaveBeenCalledTimes(1)
    expect(ekle.mock.calls[0][0]).toMatchObject({ ad: 'ABC (Kopya)', iban: '', vergiNumarasi: '' })
    expect(guncelle).not.toHaveBeenCalled()
  })

  it('iptal butonu visible false yayar', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    await wrapper.find('.dialog-footer button').trigger('click')

    expect(wrapper.emitted('update:visible')[0]).toEqual([false])
  })
})
