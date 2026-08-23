import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'

vi.mock('axios', () => ({
  default: {
    get: vi.fn((url) => {
      if (url.includes('/personel-izin')) {
        return Promise.resolve({
          data: [
            { id: 1, personelAdi: 'Ahmet Personel', izinTuru: 'Yıllık İzin', baslangic: '2026-08-25', bitis: '2026-08-30', gunSayisi: 5, durum: 'BEKLEMEDE' }
          ]
        })
      }
      if (url.includes('/personel-masraf-talepler')) {
        return Promise.resolve({
          data: [
            { id: 1, personelAdi: 'Mehmet Saha', tur: 'MASRAF', kategori: 'YAKIT', tutar: 500, tarih: '2026-08-21', aciklama: 'Yakıt', durum: 'BEKLEMEDE' }
          ]
        })
      }
      return Promise.resolve({ data: [] })
    }),
    post: vi.fn(() => Promise.resolve({ data: {} })),
    patch: vi.fn(() => Promise.resolve({ data: {} })),
    put: vi.fn(() => Promise.resolve({ data: {} })),
    delete: vi.fn(() => Promise.resolve({ data: {} })),
    create: vi.fn(() => ({
      get: vi.fn((url) => {
        if (url.includes('/personel-izin')) {
          return Promise.resolve({
            data: [
              { id: 1, personelAdi: 'Ahmet Personel', izinTuru: 'Yıllık İzin', baslangic: '2026-08-25', bitis: '2026-08-30', gunSayisi: 5, durum: 'BEKLEMEDE' }
            ]
          })
        }
        if (url.includes('/personel-masraf-talepler')) {
          return Promise.resolve({
            data: [
              { id: 1, personelAdi: 'Mehmet Saha', tur: 'MASRAF', kategori: 'YAKIT', tutar: 500, tarih: '2026-08-21', aciklama: 'Yakıt', durum: 'BEKLEMEDE' }
            ]
          })
        }
        return Promise.resolve({ data: [] })
      }),
      post: vi.fn(() => Promise.resolve({ data: {} })),
      patch: vi.fn(() => Promise.resolve({ data: {} })),
      put: vi.fn(() => Promise.resolve({ data: {} })),
      delete: vi.fn(() => Promise.resolve({ data: {} })),
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } }
    }))
  }
}))

const stubs = {
  Card: {
    template: '<div class="p-card"><div class="p-card-title"><slot name="title" /></div><div class="p-card-content"><slot name="content" /></div></div>'
  },
  Button: true,
  Badge: true,
  Tag: true,
  TabView: {
    template: '<div class="tabview-stub"><slot /></div>'
  },
  TabPanel: {
    template: '<div class="tabpanel-stub"><slot /><slot name="header" /></div>'
  }
}

describe('Onaylar.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders approval center with leave and expense requests', async () => {
    const Onaylar = (await import('../Onaylar.vue')).default
    const wrapper = mount(Onaylar, {
      global: { stubs, plugins: [createPinia(), ToastService] }
    })
    await flushPromises()
    expect(wrapper.find('.onaylar-sayfasi').exists()).toBe(true)
    expect(wrapper.text()).toContain('Yönetici & Muhasebe Onay Merkezi')
    expect(wrapper.text()).toContain('Saha Masraf & Avans')
  })
})
