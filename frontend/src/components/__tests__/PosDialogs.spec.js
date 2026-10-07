import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PosSatisOzetDialog from '../PosSatisOzetDialog.vue'
import PosBugunkuSatislarDialog from '../PosBugunkuSatislarDialog.vue'
import i18n from '../../i18n.js'

/**
 * POS SATIS SONUC DIALOGLARI — sozlesme testleri.
 *
 * Satis bittikten sonra kasa operatorunun gorsuclugu: fis tutari, kalan
 * (alacak), para ustu ve fis modu. Gunluk satis listesi de ayni akisin
 * parcasidir (F7).
 *
 * Kural: kalan ve para ustu satirlari yalniz deger POZITIF oldugunda
 * gorunur; sifir olan kalemler ekranda yer kaplamaz.
 */

const stubs = {
  Dialog: {
    name: 'Dialog',
    props: ['visible', 'header', 'modal'],
    emits: ['update:visible'],
    template: '<div class="dialog" v-if="visible"><h3 class="dialog-header">{{ header }}</h3><slot /><slot name="footer" /></div>'
  },
  Button: {
    name: 'Button',
    props: ['label', 'icon'],
    template: '<button><slot />{{ label }}</button>'
  }
}

const kur = (bile, props = {}) =>
  mount(bile, { props, global: { plugins: [i18n], stubs } })

describe('PosSatisOzetDialog', () => {
  const ozet = {
    satisOzet: {
      faturaNo: 'FTR-2026-0001',
      toplam: 500,
      odenen: 300,
      kalan: 200,
      paraUstu: 0,
      fisModu: true
    }
  }

  it('gizliyken render edilmez', () => {
    const w = kur(PosSatisOzetDialog, { visible: false, satisOzet: ozet.satisOzet })
    expect(w.find('.dialog').exists()).toBe(false)
  })

  it('fatura no, toplam ve odenen tutar gosterilir', () => {
    const w = kur(PosSatisOzetDialog, { visible: true, ...ozet })
    const text = w.text()
    expect(text).toContain('FTR-2026-0001')
    expect(text).toContain('500')
    expect(text).toContain('300')
  })

  it('alacak varsa kalan satiri gorunur', () => {
    const w = kur(PosSatisOzetDialog, { visible: true, ...ozet })
    // Renk sinifi satirin kendisinde degil DEGERIN `strong` etiketinde.
    const borc = w.find('.satis-ozet-satir strong.borc')
    expect(borc.exists()).toBe(true)
    expect(borc.text()).toContain('200')
  })

  it('alacak yoksa kalan satiri gizlenir', () => {
    const w = kur(PosSatisOzetDialog, {
      visible: true,
      satisOzet: { ...ozet.satisOzet, kalan: 0 }
    })
    expect(w.find('strong.borc').exists()).toBe(false)
  })

  it('para ustu varsa gorunur, yoksa gizlenir', () => {
    const ustulu = kur(PosSatisOzetDialog, {
      visible: true,
      satisOzet: { ...ozet.satisOzet, paraUstu: 50, odenen: 550, kalan: 0 }
    })
    expect(ustulu.find('strong.para').text()).toContain('50')

    const parasiz = kur(PosSatisOzetDialog, { visible: true, satisOzet: ozet.satisOzet })
    expect(parasiz.find('strong.para').exists()).toBe(false)
  })

  it('fis modu (fiyatli/fiyatsiz) gosterilir', () => {
    const w = kur(PosSatisOzetDialog, { visible: true, satisOzet: { ...ozet.satisOzet, fisModu: false } })
    expect(w.text()).toMatch(/fiyats[ıi]z/i)
  })

  it('ozet yoksa govde render edilmez (dialog bos acilmaz)', () => {
    const w = kur(PosSatisOzetDialog, { visible: true, satisOzet: null })
    expect(w.find('.satis-ozet').exists()).toBe(false)
  })

  it('Kapat dugmesi update:visible false yayar', async () => {
    const w = kur(PosSatisOzetDialog, { visible: true, ...ozet })
    const kapat = w.findAll('button').find((b) => b.text().includes('Kapat'))
    expect(kapat).toBeTruthy()
    await kapat.trigger('click')
    expect(w.emitted('update:visible')).toEqual([[false]])
  })

  it('Yeni Satis dugmesi yeni-satis yayar (bir sonraki satis icin hizli akis)', async () => {
    const w = kur(PosSatisOzetDialog, { visible: true, ...ozet })
    const yeni = w.findAll('button').find((b) => b.text().includes('Yeni Satış'))
    expect(yeni).toBeTruthy()
    await yeni.trigger('click')
    expect(w.emitted('yeni-satis')).toBeTruthy()
  })
})

describe('PosBugunkuSatislarDialog', () => {
  const satislar = [
    { id: 1, faturaNumarasi: 'FTR-2026-0001', cariHesapAd: 'ABC Ltd', genelToplam: 500 },
    { id: 2, faturaNumarasi: 'FTR-2026-0002', cariHesapAd: null, genelToplam: 250 }
  ]

  it('satislar yoksa bos mesaji gosterir', () => {
    const w = kur(PosBugunkuSatislarDialog, { visible: true, satislar: [] })
    const bos = w.find('.sepet-bos')
    expect(bos.exists()).toBe(true)
    expect(bos.text()).toContain('satış yapılmadı')
    expect(w.findAll('.gunluk-satis-satir')).toHaveLength(0)
  })

  it('satirlari fatura no ve tutarla listeler', () => {
    const w = kur(PosBugunkuSatislarDialog, { visible: true, satislar })
    const satirlar = w.findAll('.gunluk-satis-satir')
    expect(satirlar).toHaveLength(2)
    expect(satirlar[0].text()).toContain('FTR-2026-0001')
    expect(satirlar[0].text()).toContain('ABC Ltd')
  })

  it('cari adi olmayan satis "anlik" olarak gosterilir', () => {
    const w = kur(PosBugunkuSatislarDialog, { visible: true, satislar })
    expect(w.findAll('.gunluk-satis-satir')[1].text()).toMatch(/anl[ıi]k/i)
  })

  it('gunluk toplam tum satirlarin toplamidir', () => {
    const w = kur(PosBugunkuSatislarDialog, { visible: true, satislar })
    // 500 + 250 = 750
    expect(w.find('.gunluk-toplam').text()).toContain('750')
  })

  it('satira tiklamak goruntule olayini satis nesnesiyle yayar', async () => {
    const w = kur(PosBugunkuSatislarDialog, { visible: true, satislar })
    await w.findAll('.gunluk-satis-goruntule')[1].trigger('click')
    expect(w.emitted('goruntule')).toEqual([[satislar[1]]])
  })

  it('Yenile dugmesi yenile yayar', async () => {
    const w = kur(PosBugunkuSatislarDialog, { visible: true, satislar })
    const yenile = w.findAll('button').find((b) => b.text().includes('Yenile'))
    expect(yenile).toBeTruthy()
    await yenile.trigger('click')
    expect(w.emitted('yenile')).toBeTruthy()
  })

  it('Kapat dugmesi update:visible false yayar', async () => {
    const w = kur(PosBugunkuSatislarDialog, { visible: true, satislar })
    const kapat = w.findAll('button').find((b) => b.text().includes('Kapat'))
    await kapat.trigger('click')
    expect(w.emitted('update:visible')).toEqual([[false]])
  })
})