import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'

vi.mock('axios', () => ({
  default: {
    get: vi.fn((url) => {
      if (url.includes('/tahsilat')) {
        return Promise.resolve({
          data: {
            toplamAlacak: 6000,
            vadesiGecmisToplam: 4000,
            vadesiYaklasanToplam: 2000,
            acikFaturaSayisi: 3,
            gecikmisCariSayisi: 2,
            cariler: [
              {
                cariId: 1,
                cariAd: 'A Ltd',
                telefon: '05555555555',
                email: 'a@x.com',
                toplamAlacak: 3000,
                gecikmisAlacak: 1000,
                faturaSayisi: 2,
                maxGecikmeGunu: 10,
                aralik: '0-30 Gün',
                faturalar: [
                  { faturaId: 1, faturaNumarasi: 'F-1', vadeTarihi: '2026-08-19', kalanTutar: 1000, gecikmeGunu: 10 }
                ]
              },
              {
                cariId: 2,
                cariAd: 'B Ltd',
                telefon: null,
                email: null,
                toplamAlacak: 3000,
                gecikmisAlacak: 3000,
                faturaSayisi: 1,
                maxGecikmeGunu: 45,
                aralik: '31-60 Gün',
                faturalar: [
                  { faturaId: 2, faturaNumarasi: 'F-2', vadeTarihi: '2026-07-15', kalanTutar: 3000, gecikmeGunu: 45 }
                ]
              }
            ]
          }
        })
      }
      return Promise.resolve({ data: {} })
    }),
    post: vi.fn(() => Promise.resolve({ data: { gonderilen: 2 } })),
    create: vi.fn(() => ({
      get: vi.fn((url) => {
        if (url.includes('/tahsilat')) {
          return Promise.resolve({
            data: {
              toplamAlacak: 6000,
              vadesiGecmisToplam: 4000,
              vadesiYaklasanToplam: 2000,
              acikFaturaSayisi: 3,
              gecikmisCariSayisi: 2,
              cariler: [
                {
                  cariId: 1,
                  cariAd: 'A Ltd',
                  telefon: '05555555555',
                  email: 'a@x.com',
                  toplamAlacak: 3000,
                  gecikmisAlacak: 1000,
                  faturaSayisi: 2,
                  maxGecikmeGunu: 10,
                  aralik: '0-30 Gün',
                  faturalar: [
                    { faturaId: 1, faturaNumarasi: 'F-1', vadeTarihi: '2026-08-19', kalanTutar: 1000, gecikmeGunu: 10 }
                  ]
                }
              ]
            }
          })
        }
        return Promise.resolve({ data: {} })
      }),
      post: vi.fn(() => Promise.resolve({ data: { gonderilen: 2 } })),
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } }
    }))
  }
}))

const stubs = {
  Button: true,
  Tag: true,
  Toolbar: { template: '<div><slot name="start" /><slot name="end" /></div>' },
  DataTable: { template: '<div><slot /></div>' },
  Column: true,
  EmptyState: true
}

describe('Tahsilat.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders collections summary and cari list', async () => {
    const Tahsilat = (await import('../Tahsilat.vue')).default
    const wrapper = mount(Tahsilat, {
      global: { stubs, plugins: [createPinia(), ToastService] }
    })
    await flushPromises()
    expect(wrapper.find('.tahsilat-container').exists()).toBe(true)
    expect(wrapper.text()).toContain('Tahsilat Merkezi')
    expect(wrapper.text()).toContain('Toplam Alacak')
  })
})
