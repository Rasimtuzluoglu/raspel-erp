import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import OnayDetayDrawer from '../OnayDetayDrawer.vue'

const stubs = {
  Drawer: {
    props: ['visible', 'header'],
    template:
      '<div class="drawer"><div class="baslik">{{ header }}</div><slot /><slot name="footer" /></div>'
  },
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' },
  Button: {
    props: ['label'],
    template: '<button class="btn" @click="$emit(\'click\')">{{ label }}</button>'
  },
  SkeletonLoader: { template: '<div class="skeleton-stub" />' },
  RouterLink: {
    props: ['to'],
    template: '<a class="rl" :href="String(to)"><slot /></a>'
  }
}

const kayit = {
  tur: 'IZIN',
  kaynakId: 42,
  baslik: 'Ahmet Yıllık İzin',
  altBaslik: 'İnsan Kaynakları · 5 gün',
  tutar: 5000,
  tarih: '2026-08-25',
  olusturmaTarihi: '2026-08-20T10:15:00',
  durum: 'BEKLEMEDE',
  aciklama: 'Yıllık izin talebi',
  meta: { personel: 'Ahmet Yılmaz', gunSayisi: 5, ozelAlan: 'Değer' }
}

const kur = (props = {}) =>
  mount(OnayDetayDrawer, {
    props: { visible: true, kayit, ...props },
    global: { plugins: [i18n], stubs }
  })

describe('OnayDetayDrawer', () => {
  it('ortak onay alanlarını, meta bilgilerini ve ilgili kayıt bağlantısını gösterir', () => {
    const w = kur()
    const metin = w.text()

    expect(metin).toContain('Onay Detayı')
    expect(metin).toContain('Ahmet Yıllık İzin')
    expect(metin).toContain('İnsan Kaynakları · 5 gün')
    expect(metin).toContain('İzin')
    expect(metin).toContain('Beklemede')
    expect(metin).toContain('5.000')
    expect(metin).toContain('25.08.2026')
    expect(metin).toContain('Yıllık izin talebi')
    expect(metin).toContain('Ek Bilgiler')
    expect(metin).toContain('Personel')
    expect(metin).toContain('Ahmet Yılmaz')
    expect(metin).toContain('Gün Sayısı')
    expect(metin).toContain('Ozel Alan')
    expect(w.find('.rl').attributes('href')).toBe('/izinler')
  })

  it('türe göre doğru ilgili kayıt rotasını kullanır', () => {
    const rotalar = {
      IZIN: '/izinler',
      MASRAF: '/masraflar',
      SATINALMA: '/satinalma',
      SIPARIS: '/siparisler'
    }
    for (const [tur, rota] of Object.entries(rotalar)) {
      const w = kur({ kayit: { ...kayit, tur } })
      expect(w.find('.rl').attributes('href')).toBe(rota)
    }
  })

  it('meta tarih ve boolean değerlerini yerelleştirir', () => {
    const w = kur({
      kayit: {
        ...kayit,
        meta: { bitisTarihi: '2026-08-30', avansMi: true, gunSayisi: 5 }
      }
    })
    expect(w.text()).toContain('Bitiş Tarihi')
    expect(w.text()).toContain('30.08.2026')
    expect(w.text()).toContain('Avans')
    expect(w.text()).toContain('Evet')
    expect(w.text()).toContain('Gün Sayısı')
  })

  it('bilinmeyen türde bağlantı göstermez', () => {
    const w = kur({ kayit: { ...kayit, tur: 'DIGER' } })
    expect(w.find('.rl').exists()).toBe(false)
    expect(w.text()).toContain('Bu kayıt için bağlantı tanımlı değil.')
  })

  it('bekleyen kayıtta onay/red, nihai durumda yalnızca kapat gösterir', () => {
    const etiketler = (w) => w.findAll('.btn').map((buton) => buton.text())

    expect(etiketler(kur())).toEqual(expect.arrayContaining(['Onayla', 'Reddet', 'Kapat']))

    const onayli = kur({ kayit: { ...kayit, durum: 'ONAYLANDI' } })
    const onayliEtiketler = etiketler(onayli)
    expect(onayliEtiketler).not.toContain('Onayla')
    expect(onayliEtiketler).not.toContain('Reddet')
    expect(onayliEtiketler).toContain('Kapat')
  })

  it('onayla/reddet olaylarını kayıtla birlikte yayar', async () => {
    const w = kur()

    const onayla = w.findAll('.btn').find((buton) => buton.text() === 'Onayla')
    const reddet = w.findAll('.btn').find((buton) => buton.text() === 'Reddet')

    await onayla.trigger('click')
    await reddet.trigger('click')

    expect(w.emitted('onayla')?.[0]?.[0].kaynakId).toBe(42)
    expect(w.emitted('reddet')?.[0]?.[0].tur).toBe('IZIN')
  })

  it('yüklenirken iskeleti aria-busy ile gösterir', () => {
    const w = kur({ yukleniyor: true })
    expect(w.find('.detay-iskelet[aria-busy="true"]').exists()).toBe(true)
    expect(w.find('.skeleton-stub').exists()).toBe(true)
    expect(w.find('.onay-detay').exists()).toBe(false)
  })

  it('kapat düğmesi visible olayını false olarak yayar', async () => {
    const w = kur()
    const kapat = w.findAll('.btn').find((buton) => buton.text() === 'Kapat')
    await kapat.trigger('click')
    expect(w.emitted('update:visible')?.[0]).toEqual([false])
  })
})
