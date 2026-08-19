import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import GecmisZamanCizelgesi from '../GecmisZamanCizelgesi.vue'

describe('GecmisZamanCizelgesi.vue', () => {
  it('renders empty state when no logs exist', () => {
    const wrapper = mount(GecmisZamanCizelgesi, {
      global: {
        stubs: {
          Timeline: true,
          Card: true
        }
      },
      props: {
        mockData: []
      }
    })

    expect(wrapper.find('.timeline-bos').exists()).toBe(true)
  })

  it('renders timeline when logs are provided', () => {
    const wrapper = mount(GecmisZamanCizelgesi, {
      global: {
        stubs: {
          Timeline: {
            template: `<div class="custom-timeline"><slot name="content" :item="{ islem: 'Test' }" /></div>`
          },
          Card: {
            template: `<div class="p-card"><slot name="title" /><slot name="subtitle" /><slot name="content" /></div>`
          }
        }
      },
      props: {
        mockData: [
          {
            id: 1,
            islem: 'Fatura Oluşturuldu',
            kullanici: 'Admin',
            tarih: new Date().toISOString(),
            detay: '1000 TL tutarında satış faturası'
          }
        ]
      }
    })

    expect(wrapper.find('.timeline-bos').exists()).toBe(false)
    expect(wrapper.find('.custom-timeline').exists()).toBe(true)
  })
})
