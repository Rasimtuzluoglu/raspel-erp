import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import FaturaTasarimModal from '../FaturaTasarimModal.vue'

describe('FaturaTasarimModal.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  const commonStubs = {
    Dialog: {
      template: '<div class="p-dialog" v-if="visible"><slot /></div>',
      props: ['visible']
    },
    Button: {
      template: '<button class="p-button" type="button" @click="$emit(\'click\', $event)"><slot />{{ label }}</button>',
      props: ['label', 'icon', 'loading', 'disabled'],
      emits: ['click']
    },
    InputText: {
      template: '<input class="p-inputtext" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
      props: ['modelValue', 'placeholder']
    },
    InputSwitch: {
      template: '<input type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)" />',
      props: ['modelValue']
    },
    Dropdown: true,
    SelectButton: true,
    Textarea: true
  }

  it('renders correctly when visible is true', () => {
    const wrapper = mount(FaturaTasarimModal, {
      props: {
        visible: true,
        faturaData: {
          id: 1,
          faturaNumarasi: 'FTR-2026-TEST',
          cariHesapAd: 'Test Müşteri Ltd.',
          kalemler: [
            { stokKodu: 'STK-01', aciklama: 'Ürün A', adet: 2, birimFiyat: 100, tutar: 200 }
          ]
        }
      },
      global: {
        plugins: [PrimeVue, ToastService],
        stubs: commonStubs
      }
    })

    expect(wrapper.exists()).toBe(true)
    expect(wrapper.find('.fatura-a4-kagit').exists()).toBe(true)
    expect(wrapper.text()).toContain('Test Müşteri Ltd.')
    expect(wrapper.text()).toContain('FTR-2026-TEST')
  })

  it('allows changing themes and saving template to localStorage', async () => {
    const wrapper = mount(FaturaTasarimModal, {
      props: {
        visible: true
      },
      global: {
        plugins: [PrimeVue, ToastService],
        stubs: commonStubs
      }
    })

    const buttons = wrapper.findAll('.sablon-btn')
    expect(buttons.length).toBe(4)

    // Click on 'Klasik' theme button
    await buttons[1].trigger('click')
    expect(wrapper.find('.fatura-a4-kagit').classes()).toContain('tema-klasik')

    // Click on 'Varsayılan Olarak Kaydet'
    const saveBtn = wrapper.find('.sidebar-footer button')
    expect(saveBtn.exists()).toBe(true)
    await saveBtn.trigger('click')

    // Verify localStorage has saved data
    const saved = localStorage.getItem('raspel_fatura_sablon_genel')
    expect(saved).toBeTruthy()
    expect(JSON.parse(saved).sablon).toBe('klasik')
  })

  it('handles printing invocation', async () => {
    const printSpy = vi.spyOn(window, 'print').mockImplementation(() => {})

    const wrapper = mount(FaturaTasarimModal, {
      props: {
        visible: true
      },
      global: {
        plugins: [PrimeVue, ToastService],
        stubs: commonStubs
      }
    })

    const printBtn = wrapper.find('.toolbar-aksiyonlar button')
    expect(printBtn.exists()).toBe(true)
    await printBtn.trigger('click')
    expect(printSpy).toHaveBeenCalled()

    printSpy.mockRestore()
  })

  it('applies paper format and orientation classes correctly', async () => {
    const wrapper = mount(FaturaTasarimModal, {
      props: {
        visible: true
      },
      global: {
        plugins: [PrimeVue, ToastService],
        stubs: commonStubs
      }
    })

    const paper = wrapper.find('.fatura-a4-kagit')
    expect(paper.classes()).toContain('kagit-a4')
    expect(paper.classes()).toContain('yon-portrait')
  })
})

