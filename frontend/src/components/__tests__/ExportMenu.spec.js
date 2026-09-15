import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ExportMenu from '../ExportMenu.vue'
import i18n from '../../i18n.js'

describe('ExportMenu.vue', () => {
  it('renders export button properly', () => {
    const wrapper = mount(ExportMenu, {
      global: {
        plugins: [i18n],
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
