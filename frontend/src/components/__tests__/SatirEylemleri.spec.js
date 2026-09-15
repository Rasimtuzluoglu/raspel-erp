import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import SatirEylemleri from '../SatirEylemleri.vue'
import i18n from '../../i18n.js'

const kur = (opts = {}) =>
  mount(SatirEylemleri, {
    ...opts,
    global: { plugins: [i18n], stubs: { teleport: true } }
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

  it('menuyu sabit konumda ve ekran disina tasmadan konumlandirir', async () => {
    vi.spyOn(Element.prototype, 'getBoundingClientRect').mockReturnValue({
      right: 1200, left: 1170, top: 100, bottom: 130, width: 30, height: 30, x: 1170, y: 100, toJSON() {}
    })
    const wrapper = kur()
    await wrapper.find('button').trigger('click')
    const menu = wrapper.find('.eylem-menu')
    expect(menu.exists()).toBe(true)
    const stil = menu.attributes('style') || ''
    expect(stil).toContain('position: fixed')
    const left = Number((stil.match(/left:\s*([\d.]+)px/) || [])[1])
    expect(left).toBeGreaterThanOrEqual(8)
    vi.restoreAllMocks()
  })
})
