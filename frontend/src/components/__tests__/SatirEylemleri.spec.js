import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import SatirEylemleri from '../SatirEylemleri.vue'

const kur = (opts = {}) =>
  mount(SatirEylemleri, {
    ...opts,
    global: { stubs: { teleport: true } }
  })

describe('SatirEylemleri', () => {
  it('menüyü açar ve düzenle/çoğalt/sil seçeneklerini gösterir', async () => {
    const wrapper = kur({ props: { gorunur: { duzenle: true, cogalt: true, sil: true } } })
    await wrapper.find('button').trigger('click')
    expect(wrapper.text()).toContain('Düzenle')
    expect(wrapper.text()).toContain('Çoğalt')
    expect(wrapper.text()).toContain('Sil')
  })

  it('düzenle tıklandığında düzenle eventi yayar', async () => {
    const wrapper = kur()
    await wrapper.find('button').trigger('click')
    await wrapper.findAll('button')[1].trigger('click') // menüdeki Düzenle
    expect(wrapper.emitted('duzenle')).toBeTruthy()
  })

  it('sil tıklandığında sil eventi yayar ve menüyü kapatır', async () => {
    const wrapper = kur()
    await wrapper.find('button').trigger('click')
    const silBtn = wrapper.findAll('button').find((b) => b.text().includes('Sil'))
    await silBtn.trigger('click')
    expect(wrapper.emitted('sil')).toBeTruthy()
    expect(wrapper.find('.eylem-menu').exists()).toBe(false)
  })

  it('cogalt kapalıysa çoğalt seçeneği görünmez', async () => {
    const wrapper = kur({ props: { gorunur: { duzenle: true, cogalt: false, sil: true } } })
    await wrapper.find('button').trigger('click')
    expect(wrapper.text()).not.toContain('Çoğalt')
  })

  it('items prop ile ek aksiyonları render eder ve çalıştırır', async () => {
    let cagrildi = false
    const wrapper = kur({
      props: {
        gorunur: { duzenle: false, cogalt: false, sil: false },
        items: [{ etiket: 'PDF', ikon: 'pi pi-file-pdf', islem: () => { cagrildi = true } }]
      }
    })
    await wrapper.find('button').trigger('click')
    const pdfBtn = wrapper.findAll('button').find((b) => b.text().includes('PDF'))
    expect(pdfBtn).toBeTruthy()
    await pdfBtn.trigger('click')
    expect(cagrildi).toBe(true)
    expect(wrapper.emitted('eylem')).toBeTruthy()
  })

  it('Escape tusu menuyu kapatir', async () => {
    const wrapper = kur()
    await wrapper.find('button').trigger('click')
    expect(wrapper.find('.eylem-menu').exists()).toBe(true)
    await wrapper.find('.eylem-menu').trigger('keydown', { key: 'Escape' })
    expect(wrapper.find('.eylem-menu').exists()).toBe(false)
  })
})
