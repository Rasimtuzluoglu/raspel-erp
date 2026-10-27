import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import AjandaGorevDetayDrawer from '../AjandaGorevDetayDrawer.vue'

const stubs = {
  Drawer: {
    props: ['visible', 'header'],
    template: '<div class="drawer"><div class="baslik">{{ header }}</div><slot /><slot name="footer" /></div>'
  },
  Tag: { props: ['value', 'severity'], template: '<span class="tag">{{ value }}</span>' },
  Button: { props: ['label'], template: '<button class="btn">{{ label }}</button>' }
}

const kur = (props = {}) =>
  mount(AjandaGorevDetayDrawer, {
    props: { visible: true, ...props },
    global: { plugins: [i18n], stubs }
  })

describe('AjandaGorevDetayDrawer', () => {
  it('görev detayını öncelik ve durum rozetleriyle gösterir', () => {
    const w = kur({
      kayitTuru: 'gorev',
      kayit: {
        id: 1,
        baslik: 'Teklif hazırla',
        aciklama: 'Müşteri A',
        durum: 'BEKLIYOR',
        oncelik: 'YUKSEK',
        bitisTarihi: '2099-05-10'
      }
    })
    expect(w.text()).toContain('Görev Detayı')
    expect(w.text()).toContain('Teklif hazırla')
    expect(w.text()).toContain('Yüksek')
    expect(w.text()).toContain('Bekleyen')
    expect(w.text()).toContain('10.05.2099')
    expect(w.text()).toContain('Tamamla')
  })

  it('gecikmiş görevi işaretler ve tamamlanmış görevde geri al sunar', () => {
    const w = kur({
      kayitTuru: 'gorev',
      kayit: { id: 2, baslik: 'Eski iş', durum: 'BEKLIYOR', oncelik: 'ORTA', bitisTarihi: '2020-01-01' }
    })
    expect(w.text()).toContain('Gecikmiş')
    expect(w.find('.metin-gecikmis').exists()).toBe(true)

    const w2 = kur({
      kayitTuru: 'gorev',
      kayit: { id: 3, baslik: 'Bitti', durum: 'TAMAMLANDI', oncelik: 'DUSUK', bitisTarihi: '2020-01-01' }
    })
    expect(w2.text()).toContain('Geri al')
    expect(w2.text()).not.toContain('Gecikmiş')
  })

  it('hatırlatıcı detayında hatırlatma zamanını gösterir', () => {
    const w = kur({
      kayitTuru: 'hatirlatici',
      kayit: { id: 4, baslik: 'Toplantı', hatirlatmaZamani: '2099-01-02T09:30:00' }
    })
    expect(w.text()).toContain('Hatırlatıcı Detayı')
    expect(w.text()).toContain('02.01.2099')
    expect(w.text()).toContain('09:30')
  })

  it('düzenle ve sil olaylarını yayar', async () => {
    const w = kur({
      kayitTuru: 'gorev',
      kayit: { id: 5, baslik: 'Görev', durum: 'BEKLIYOR', oncelik: 'ORTA' }
    })
    const duzenle = w.findAll('button').find((b) => b.text() === 'Düzenle')
    const sil = w.findAll('button').find((b) => b.text() === 'Sil')
    await duzenle.trigger('click')
    await sil.trigger('click')
    expect(w.emitted('duzenle')?.[0]?.[0].id).toBe(5)
    expect(w.emitted('sil')?.[0]?.[0].id).toBe(5)
  })
})
