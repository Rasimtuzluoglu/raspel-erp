import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import i18n from '../../i18n.js'
import { siparisTakipAPI, uretimAPI } from '../../api/index.js'
import SiparisTakip from '../SiparisTakip.vue'

vi.mock('../../api/index.js', () => ({
  siparisTakipAPI: {
    zincir: vi.fn(() =>
      Promise.resolve({
        data: [
          {
            siparisId: 5,
            siparisNo: 'SIP-2026-000005',
            cariAd: 'Test Cari',
            siparisDurum: 'SIPARIS',
            uretimDurum: null,
            uretimSayisi: 0,
            sevkDurum: null,
            teslimatDurum: null,
            teslimatGecikti: false,
            beklenenTeslimTarihi: null,
            driverAd: null
          }
        ]
      })
    )
  },
  uretimAPI: {
    siparistenEmir: vi.fn(() => Promise.resolve({ data: [{ id: 1 }, { id: 2 }] }))
  }
}))

const stubs = {
  Button: {
    props: ['label', 'icon', 'loading'],
    emits: ['click'],
    template: '<button class="btn-stub" @click="$emit(\'click\')">{{ label }}</button>'
  },
  Dropdown: true,
  Tag: true
}

describe('SiparisTakip.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('uretim emri olmayan siparis icin buton gosterir', async () => {
    const wrapper = mount(SiparisTakip, {
      global: { stubs, plugins: [createPinia(), ToastService, i18n] }
    })
    await flushPromises()
    expect(wrapper.find('.uretim-buton').exists()).toBe(true)
  })

  it('butona tiklayinca siparistenEmir cagirir ve listeyi yeniler', async () => {
    const wrapper = mount(SiparisTakip, {
      global: { stubs, plugins: [createPinia(), ToastService, i18n] }
    })
    await flushPromises()
    await wrapper.find('.uretim-buton').trigger('click')
    await flushPromises()
    expect(uretimAPI.siparistenEmir).toHaveBeenCalledWith(5)
    expect(siparisTakipAPI.zincir).toHaveBeenCalledTimes(2)
  })
})
