import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import i18n from '../../i18n.js'
import { siparisTakipAPI } from '../../api/index.js'
import SiparisTakip from '../SiparisTakip.vue'

vi.mock('../../api/index.js', () => ({
  siparisTakipAPI: {
    zincir: vi.fn(() =>
      Promise.resolve({
        data: {
          content: [
            {
              siparisId: 5,
              siparisNo: 'SIP-2026-000005',
              cariAd: 'Test Cari',
              siparisDurum: 'SIPARIS',
              uretimDurum: null,
              uretimSayisi: 0,
              sevkDurum: null,
              sevkSayisi: 0,
              teslimatDurum: null,
              teslimatSayisi: 0,
              teslimatGecikti: false,
              beklenenTeslimTarihi: null,
              driverAd: null
            }
          ],
          totalElements: 1
        }
      })
    ),
    soforler: vi.fn(() => Promise.resolve({ data: [] }))
  },
  uretimAPI: {
    siparistenEmir: vi.fn(() => Promise.resolve({ data: [{ id: 1 }, { id: 2 }] }))
  }
}))

const stubs = {
  PageHeader: { template: '<div class="ph"><slot name="actions" /></div>' },
  Button: {
    props: ['label', 'icon', 'loading'],
    emits: ['click'],
    template: '<button class="btn-stub" @click="$emit(\'click\')">{{ label }}</button>'
  },
  Dropdown: true,
  Paginator: true,
  Tag: true,
  IconField: { template: '<div><slot /></div>' },
  InputIcon: true,
  InputText: true
}

describe('SiparisTakip.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  const kur = () =>
    mount(SiparisTakip, {
      global: { stubs, plugins: [createPinia(), ToastService, ConfirmationService, i18n] }
    })

  it('üretim emri olmayan sipariş için buton gösterir', async () => {
    const wrapper = kur()
    await flushPromises()
    expect(wrapper.find('.uretim-buton').exists()).toBe(true)
  })

  it('zincir uç noktasını sayfalama parametreleriyle çağırır', async () => {
    kur()
    await flushPromises()
    expect(siparisTakipAPI.zincir).toHaveBeenCalledWith({ page: 0, size: 25 })
    expect(siparisTakipAPI.soforler).toHaveBeenCalled()
  })

  it('durum kodunu çevirir (ham enum görünmez)', async () => {
    const wrapper = kur()
    await flushPromises()
    // SIPARIS durumu Türkçe "Sipariş" olarak görünür (ham kod değil)
    expect(wrapper.text()).toContain('Sipariş')
    expect(wrapper.text()).not.toContain('SIPARIS')
  })
})
