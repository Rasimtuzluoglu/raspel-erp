import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'

vi.mock('axios', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({
      data: {
        yil: 2026,
        ay: 8,
        gerceklesenCiro: 750000,
        hedefCiro: 1000000,
        ciroIlerlemeYuzdesi: 75.0,
        kalanCiro: 250000,
        kalanGun: 10,
        gerceklesenKar: 220000,
        hedefKar: 300000,
        netKarMarji: 29.3,
        kasaBankaToplam: 180000,
        toplamAlacak: 350000,
        vadesiGecenAlacak: 45000,
        toplamBorc: 120000,
        topMusteriler: [{ cariId: 1, unvan: 'Mega Corp', toplamCiro: 200000, faturaSayisi: 5 }],
        topUrunler: [{ stokId: 1, stokKodu: 'STK-01', stokAdi: 'ERP Lisans', satisMiktari: 10, toplamCiro: 150000 }],
        kritikAlacaklar: [{ cariId: 2, unvan: 'Borçlu Ltd.', bakiye: 45000, gecikmeGunu: 15 }],
        gunlukCiroTrendi: [{ gun: 1, tarih: '01.08', ciro: 25000 }]
      }
    })),
    post: vi.fn(() => Promise.resolve({ data: {} })),
    put: vi.fn(() => Promise.resolve({ data: {} })),
    delete: vi.fn(() => Promise.resolve({ data: {} })),
    create: vi.fn(() => ({
      get: vi.fn(() => Promise.resolve({
        data: {
          yil: 2026,
          ay: 8,
          gerceklesenCiro: 750000,
          hedefCiro: 1000000,
          ciroIlerlemeYuzdesi: 75.0,
          kalanCiro: 250000,
          kalanGun: 10,
          gerceklesenKar: 220000,
          hedefKar: 300000,
          netKarMarji: 29.3,
          kasaBankaToplam: 180000,
          toplamAlacak: 350000,
          vadesiGecenAlacak: 45000,
          toplamBorc: 120000,
          topMusteriler: [{ cariId: 1, unvan: 'Mega Corp', toplamCiro: 200000, faturaSayisi: 5 }],
          topUrunler: [{ stokId: 1, stokKodu: 'STK-01', stokAdi: 'ERP Lisans', satisMiktari: 10, toplamCiro: 150000 }],
          kritikAlacaklar: [{ cariId: 2, unvan: 'Borçlu Ltd.', bakiye: 45000, gecikmeGunu: 15 }],
          gunlukCiroTrendi: [{ gun: 1, tarih: '01.08', ciro: 25000 }]
        }
      })),
      post: vi.fn(() => Promise.resolve({ data: {} })),
      put: vi.fn(() => Promise.resolve({ data: {} })),
      delete: vi.fn(() => Promise.resolve({ data: {} })),
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } }
    }))
  }
}))

const stubs = {
  DataTable: true,
  Column: true,
  Button: true,
  Dropdown: true,
  ProgressBar: true,
  Dialog: true,
  Textarea: true,
  Chart: true,
  Bar: true
}

describe('YoneticiKokpiti.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders executive pulse and target progress metrics', async () => {
    const YoneticiKokpiti = (await import('../YoneticiKokpiti.vue')).default
    const wrapper = mount(YoneticiKokpiti, {
      global: { stubs, plugins: [createPinia(), ToastService] }
    })
    await flushPromises()
    expect(wrapper.find('.yonetici-kokpiti-sayfasi').exists()).toBe(true)
    expect(wrapper.text()).toContain('Yönetici & Finansal Nabız Kokpiti')
    expect(wrapper.text()).toContain('Aylık Ciro Gerçekleşme')
  })
})
