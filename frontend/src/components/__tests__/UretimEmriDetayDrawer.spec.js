import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import UretimEmriDetayDrawer from '../UretimEmriDetayDrawer.vue'

const stubs = {
  Drawer: {
    props: ['visible', 'header'],
    template: '<div class="drawer-stub"><slot /><slot name="footer" /></div>'
  },
  Tag: { props: ['value', 'severity'], template: '<span class="tag-stub">{{ value }}</span>' },
  Button: { props: ['label'], template: '<button>{{ label }}</button>' }
}

const emir = {
  id: 7,
  urunAd: 'Masa',
  durum: 'URETIMDE',
  oncelik: 'YUKSEK',
  miktar: 5,
  uretilenMiktar: null,
  fireMiktar: 0,
  hammaddeMaliyeti: 0,
  iscilikMaliyeti: 0,
  toplamMaliyet: 1500,
  planlananBaslangic: '2026-09-01',
  planlananBitis: '2026-09-10',
  gecikti: true
}
const gecmis = [{ id: 1, yeniDurum: 'URETIMDE', aciklama: 'Basladi', olusturmaTarihi: '2026-09-02T10:00:00' }]

describe('UretimEmriDetayDrawer.vue', () => {
  it('emir bilgilerini, gecikme uyarisini ve gecmisi gosterir', () => {
    const wrapper = mount(UretimEmriDetayDrawer, {
      props: { visible: true, emir, gecmis },
      global: { stubs, plugins: [i18n] }
    })
    const metin = wrapper.text()
    expect(metin).toContain('Masa')
    expect(metin).toContain('Basladi')
    expect(wrapper.find('.zaman-cizelgesi').exists()).toBe(true)
    expect(wrapper.find('.gecikme-uyari').exists()).toBe(true)
  })

  it('tamamla aksiyonunu yayar', async () => {
    const wrapper = mount(UretimEmriDetayDrawer, {
      props: { visible: true, emir, gecmis },
      global: { stubs, plugins: [i18n] }
    })
    const tamamla = wrapper.findAll('button').find((b) => b.text().includes('Tamamla'))
    expect(tamamla).toBeTruthy()
    await tamamla.trigger('click')
    expect(wrapper.emitted('tamamla')).toBeTruthy()
  })

  it('gecmis yoksa bos durum gosterir', () => {
    const wrapper = mount(UretimEmriDetayDrawer, {
      props: { visible: true, emir, gecmis: [] },
      global: { stubs, plugins: [i18n] }
    })
    expect(wrapper.find('.zaman-cizelgesi').exists()).toBe(false)
  })
})
