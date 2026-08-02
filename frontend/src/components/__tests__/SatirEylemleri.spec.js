import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import SatirEylemleri from '../SatirEylemleri.vue'

describe('SatirEylemleri', () => {
  it('menüyü açar ve düzenle/çoğalt/sil seçeneklerini gösterir', async () => {
    const wrapper = mount(SatirEylemleri, {
      props: { gorunur: { duzenle: true, cogalt: true, sil: true } }
    })
    await wrapper.find('button').trigger('click')
    expect(wrapper.text()).toContain('Düzenle')
    expect(wrapper.text()).toContain('Çoğalt')
    expect(wrapper.text()).toContain('Sil')
  })

  it('düzenle tıklandığında düzenle eventi yayar', async () => {
    const wrapper = mount(SatirEylemleri)
    await wrapper.find('button').trigger('click')
    await wrapper.findAll('button')[1].trigger('click') // menüdeki Düzenle
    expect(wrapper.emitted('duzenle')).toBeTruthy()
  })

  it('sil tıklandığında sil eventi yayar ve menüyü kapatır', async () => {
    const wrapper = mount(SatirEylemleri)
    await wrapper.find('button').trigger('click')
    const silBtn = wrapper.findAll('button').find(b => b.text().includes('Sil'))
    await silBtn.trigger('click')
    expect(wrapper.emitted('sil')).toBeTruthy()
    expect(wrapper.find('.eylem-menu').exists()).toBe(false)
  })

  it('cogalt kapalıysa çoğalt seçeneği görünmez', async () => {
    const wrapper = mount(SatirEylemleri, {
      props: { gorunur: { duzenle: true, cogalt: false, sil: true } }
    })
    await wrapper.find('button').trigger('click')
    expect(wrapper.text()).not.toContain('Çoğalt')
  })
})
