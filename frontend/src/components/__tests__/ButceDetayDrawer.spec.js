import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import ButceDetayDrawer from '../ButceDetayDrawer.vue'

const stubs = {
  Drawer: {
    props: ['visible', 'header'],
    template: '<div class="drawer"><div class="baslik">{{ header }}</div><slot /><slot name="footer" /></div>'
  },
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' },
  Button: { props: ['label'], template: '<button class="btn">{{ label }}</button>' },
  SkeletonLoader: { template: '<div class="skeleton-stub" />' }
}

const kayit = {
  id: 1,
  ad: '2026 Pazarlama',
  kategori: 'Pazarlama',
  yil: 2026,
  ay: 3,
  tur: 'GIDER',
  tutar: 50000,
  durum: 'TASLAK',
  revizyon: 2,
  onaylayanKullaniciId: null,
  onayTarihi: null,
  redGerekcesi: null,
  aciklama: 'Kampanya bütçesi'
}

const kur = (props = {}) =>
  mount(ButceDetayDrawer, {
    props: { visible: true, kayit, revizyonlar: [], ...props },
    global: { plugins: [i18n], stubs }
  })

describe('ButceDetayDrawer', () => {
  it('bütçe bilgilerini ve durum rozetlerini gösterir', () => {
    const w = kur()
    const metin = w.text()
    expect(metin).toContain('Bütçe Detayı')
    expect(metin).toContain('2026 Pazarlama')
    expect(metin).toContain('Taslak')
    expect(metin).toContain('Gider')
    expect(metin).toContain('v2')
    expect(metin).toContain('Kampanya bütçesi')
  })

  it('taslak bütçede onay/red aksiyonlarını, onaylıda yalnızca düzenlemeyi gösterir', () => {
    const etiketler = (w) => w.findAll('.btn').map((b) => b.text())
    expect(etiketler(kur())).toEqual(expect.arrayContaining(['Onayla', 'Reddet', 'Düzenle']))

    const onayli = kur({ kayit: { ...kayit, durum: 'ONAYLANDI' } })
    const onayliEtiketler = etiketler(onayli)
    expect(onayliEtiketler).not.toContain('Onayla')
    expect(onayliEtiketler).not.toContain('Reddet')
    expect(onayliEtiketler).toContain('Düzenle')
  })

  it('revizyon zaman çizelgesini işlem türüne göre renklendirir', () => {
    const w = kur({
      revizyonlar: [
        { id: 1, revizyon: 1, islem: 'OLUSTUR', tutar: 40000, olusturmaTarihi: '2026-01-01T10:00:00' },
        { id: 2, revizyon: 2, islem: 'GUNCELLE', tutar: 50000, olusturmaTarihi: '2026-01-02T10:00:00' },
        { id: 3, revizyon: 2, islem: 'ONAY', tutar: 50000, olusturmaTarihi: '2026-01-03T10:00:00' }
      ]
    })
    expect(w.findAll('.zc-adim')).toHaveLength(3)
    expect(w.find('.zc-adim.taslak').exists()).toBe(true)
    expect(w.find('.zc-adim.uretimde').exists()).toBe(true)
    expect(w.find('.zc-adim.tamam').exists()).toBe(true)
    expect(w.text()).toContain('Oluşturuldu')
    expect(w.text()).toContain('Onaylandı')
  })

  it('revizyon yüklenirken iskelet gösterir', () => {
    const w = kur({ revizyonYukleniyor: true })
    expect(w.find('.revizyon-yukleniyor[aria-busy="true"]').exists()).toBe(true)
    expect(w.find('.skeleton-stub').exists()).toBe(true)
  })

  it('düzenle/onayla/reddet olaylarını yayar', async () => {
    const w = kur()
    const duzenle = w.findAll('.btn').find((b) => b.text() === 'Düzenle')
    await duzenle.trigger('click')
    expect(w.emitted('duzenle')?.[0]?.[0].id).toBe(1)
  })
})
