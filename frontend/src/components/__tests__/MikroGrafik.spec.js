import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import MikroGrafik from '../dashboard/kokpit/MikroGrafik.vue'

describe('MikroGrafik', () => {
  it('alan tipinde gradyan ve son nokta cizer', () => {
    const wrapper = mount(MikroGrafik, {
      props: { tip: 'alan', seriler: [{ degerler: [1, 3, 2, 5], renk: 'var(--accent)' }] }
    })
    const svg = wrapper.find('svg').html().toLowerCase()
    expect(svg).toContain('lineargradient')
    expect(svg).toContain('<circle')
  })

  it('cok serili alanda her seri icin benzersiz gradyan kimligi uretir', () => {
    const wrapper = mount(MikroGrafik, {
      props: {
        tip: 'alan',
        seriler: [
          { degerler: [1, 3, 2], renk: 'var(--success)' },
          { degerler: [4, 2, 5], renk: 'var(--danger)' }
        ]
      }
    })
    const svg = wrapper.find('svg').html()
    const kimlikler = [...svg.matchAll(/id="([^"]+)"/g)].map((eslesme) => eslesme[1])
    expect(kimlikler).toHaveLength(2)
    expect(new Set(kimlikler).size).toBe(2)
    for (const kimlik of kimlikler) {
      expect(svg).toContain(`url(#${kimlik})`)
    }
  })

  it('tek noktali alan serisinde NaN uretmez', () => {
    const wrapper = mount(MikroGrafik, {
      props: { tip: 'alan', seriler: [{ degerler: [5], renk: 'var(--accent)' }] }
    })
    const svg = wrapper.find('svg').html()
    expect(svg).not.toContain('NaN')
    expect(svg).not.toContain('undefined')
    expect(svg.toLowerCase()).toContain('<circle')
  })
})
