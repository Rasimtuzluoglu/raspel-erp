import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ExportMenu from '../ExportMenu.vue'

describe('ExportMenu.vue', () => {
  it('renders export button properly', () => {
    const wrapper = mount(ExportMenu, {
      global: {
        stubs: {
          Button: {
            template: `<button class="p-button"><slot /></button>`
          },
          Menu: true
        }
      },
      props: {
        data: [{ id: 1, ad: 'Test' }],
        filename: 'test_dosya'
      }
    })

    expect(wrapper.exists()).toBe(true)
    expect(wrapper.find('button').exists()).toBe(true)
  })
})
