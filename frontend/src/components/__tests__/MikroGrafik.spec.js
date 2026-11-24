import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import MikroGrafik from '../dashboard/kokpit/MikroGrafik.vue'

const mountGrafik = (props = {}) => mount(MikroGrafik, { props })

describe('MikroGrafik', () => {
  it('alan tipinde gradyan ve seri basina gercek son nokta cizer', () => {
    const wrapper = mountGrafik({
      tip: 'alan',
      seriler: [{ degerler: [1, 3, 2, 5], renk: 'var(--accent)' }]
    })
    const svg = wrapper.find('svg').html().toLowerCase()
    expect(svg).toContain('lineargradient')
    // SVG germe (preserveAspectRatio="none") daireyi damla yapiyordu; artik CSS nokta var.
    expect(svg).not.toContain('<circle')
    const noktalar = wrapper.findAll('.mikro-son-nokta')
    expect(noktalar).toHaveLength(1)
    expect(noktalar[0].attributes('style')).toContain('background')
  })

  it('cok serili alanda benzersiz gradyan kimligi, ayri nokta ve soluk dolgu uretir', () => {
    const wrapper = mountGrafik({
      tip: 'alan',
      seriler: [
        { degerler: [1, 3, 2], renk: 'var(--success)' },
        { degerler: [4, 2, 5], renk: 'var(--danger)' }
      ]
    })
    const svg = wrapper.find('svg').html()
    const kimlikler = [...svg.matchAll(/id="([^"]+)"/g)].map((eslesme) => eslesme[1])
    expect(kimlikler).toHaveLength(2)
    expect(new Set(kimlikler).size).toBe(2)
    for (const kimlik of kimlikler) {
      expect(svg).toContain(`url(#${kimlik})`)
    }

    expect(wrapper.findAll('.mikro-son-nokta')).toHaveLength(2)

    const duraklar = [...wrapper.element.querySelectorAll('stop')]
    expect(duraklar).toHaveLength(4)
    const seriRenkleri = [duraklar[0].getAttribute('style'), duraklar[2].getAttribute('style')]
    expect(seriRenkleri[0]).toContain('var(--success)')
    expect(seriRenkleri[1]).toContain('var(--danger)')
    expect(seriRenkleri[0]).not.toBe(seriRenkleri[1])
    // Cok seride ust uste binen dolgu daha soluk (0.16), tek seride 0.28.
    expect(duraklar[0].getAttribute('stop-opacity')).toBe('0.16')
  })

  it('tek serili alanda dolgu opakligi 0.28 kalir', () => {
    const wrapper = mountGrafik({
      tip: 'alan',
      seriler: [{ degerler: [1, 2, 3], renk: 'var(--accent)' }]
    })
    const duraklar = [...wrapper.element.querySelectorAll('stop')]
    expect(duraklar[0].getAttribute('stop-opacity')).toBe('0.28')
  })

  it('iki bilesen ornegi farkli gradyan kimligi kullanir', () => {
    const birinci = mountGrafik({
      tip: 'alan',
      seriler: [{ degerler: [1, 2], renk: 'var(--accent)' }]
    })
    const ikinci = mountGrafik({
      tip: 'alan',
      seriler: [{ degerler: [1, 2], renk: 'var(--accent)' }]
    })
    const kimlik = (wrapper) => wrapper.element.querySelector('[id]')?.id
    expect(kimlik(birinci)).toBeTruthy()
    expect(kimlik(ikinci)).toBeTruthy()
    expect(kimlik(birinci)).not.toBe(kimlik(ikinci))
  })

  it('tek noktali alan serisinde NaN uretmez', () => {
    const wrapper = mountGrafik({
      tip: 'alan',
      seriler: [{ degerler: [5], renk: 'var(--accent)' }]
    })
    const svg = wrapper.find('svg').html()
    expect(svg).not.toContain('NaN')
    expect(svg).not.toContain('undefined')
    expect(wrapper.findAll('.mikro-son-nokta')).toHaveLength(1)
  })

  it('ham sayi dizisinde gecerli deger kalmazsa bos durum cizer', () => {
    const wrapper = mountGrafik({ tip: 'alan', seriler: [NaN, Infinity] })
    expect(wrapper.find('svg').exists()).toBe(false)
    expect(wrapper.findAll('.mikro-son-nokta')).toHaveLength(0)
    expect(wrapper.find('.mikro-grafik-bos').exists()).toBe(true)
    expect(wrapper.html()).not.toContain('NaN')
  })
})
