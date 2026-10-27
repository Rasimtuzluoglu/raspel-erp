import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import UretimEmriDetayDrawer from '../UretimEmriDetayDrawer.vue'

const stubs = {
  Drawer: {
    props: ['visible', 'header'],
    template: '<div class="drawer"><div class="baslik">{{ header }}</div><slot /><slot name="footer" /></div>'
  },
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' },
  Button: { props: ['label'], template: '<button class="btn">{{ label }}</button>' },
  SkeletonLoader: { template: '<div class="skeleton-stub" />' }
}

const emir = {
  id: 7,
  urunAd: 'Masa',
  miktar: 10,
  durum: 'TAMAMLANDI',
  oncelik: 'YUKSEK',
  uretilenMiktar: 9,
  fireMiktar: 1,
  hammaddeMaliyeti: 90,
  iscilikMaliyeti: 90,
  toplamMaliyet: 180,
  birimMaliyet: 20,
  fireOrani: 10,
  sureSaat: 4.5,
  gecikti: true,
  gecikmeGun: 3,
  planlananBaslangic: '2026-09-01',
  planlananBitis: '2026-09-02'
}

const kur = (props = {}) =>
  mount(UretimEmriDetayDrawer, {
    props: { visible: true, emir, gecmis: [], ...props },
    global: { plugins: [i18n], stubs }
  })

describe('UretimEmriDetayDrawer', () => {
  it('maliyet kırılımı ve hesaplanan alanları gösterir', () => {
    const w = kur()
    const metin = w.text()
    expect(metin).toContain('Birim Maliyet')
    expect(metin).toContain('Gerçekleşen Fire Oranı')
    expect(metin).toContain('10%')
    expect(metin).toContain('Gerçekleşen Süre')
    expect(metin).toContain('4.5')
    expect(metin).toContain('Gecikme')
    expect(metin).toContain('3 gün')
    expect(w.find('.metin-gecikmis').exists()).toBe(true)
  })

  it('geçmiş adımlarını duruma göre renklendirir', () => {
    const gecmis = [
      { id: 1, yeniDurum: 'TASLAK', olusturmaTarihi: '2026-09-01T08:00:00' },
      { id: 2, yeniDurum: 'URETIMDE', olusturmaTarihi: '2026-09-01T09:00:00' },
      { id: 3, yeniDurum: 'IPTAL', olusturmaTarihi: '2026-09-01T10:00:00' }
    ]
    const w = kur({ gecmis })
    expect(w.findAll('.zc-adim.taslak')).toHaveLength(1)
    expect(w.findAll('.zc-adim.uretimde')).toHaveLength(1)
    expect(w.findAll('.zc-adim.iptal')).toHaveLength(1)
    expect(w.findAll('.zc-nokta').length).toBe(3)
  })

  it('geçmiş yüklenirken iskelet gösterir', () => {
    const w = kur({ gecmisYukleniyor: true })
    expect(w.find('.gecmis-yukleniyor[aria-busy="true"]').exists()).toBe(true)
    expect(w.find('.skeleton-stub').exists()).toBe(true)
  })

  it('duruma göre aksiyon düğmeleri sunar', () => {
    const w = kur({ emir: { ...emir, durum: 'TASLAK' } })
    const metin = w.text()
    expect(metin).toContain('Başlat')
    expect(metin).toContain('Tamamla')
    expect(metin).toContain('İptal Et')
  })
})
