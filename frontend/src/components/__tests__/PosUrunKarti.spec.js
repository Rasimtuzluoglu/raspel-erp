import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PosUrunKarti from '../PosUrunKarti.vue'
import i18n from '../../i18n.js'

/**
 * POS URUN KARTI — sozlesme ve is kurali testleri.
 *
 * Katalog ızgarasinin tek kart tipi. Iki sorumlulugu var:
 *   1) Bilgi: ad, kod, stok durumu, fiyat, KDV.
 *   2) Etkilesim: tikla -> `sec` (1 adet), sag tikla -> `adet-ist` (N adet).
 *
 * Stok rozetindeki renk esigi bir is kuralidir (0 = yok, minMiktar alti veya
 * <=10 = kritik, disi normal). Kasa operatoru stogu karttan okuyup karar
 * verdiği icin bu esik testle sabitleniyor.
 */

const stubs = {
  Button: { props: ['label', 'icon'], template: '<button><slot />{{ label }}</button>' }
}

const kur = (props = {}) =>
  mount(PosUrunKarti, { props: { urun: { ...urun() }, ...props }, global: { plugins: [i18n], stubs } })

const urun = (ek = {}) => ({
  id: 1,
  ad: 'Vida M8',
  stokKodu: 'V-001',
  barkod: '8690000000001',
  satisFiyati: 12.5,
  fiyat: 12.5,
  miktar: 100,
  minMiktar: 20,
  birim: 'adet',
  kdvOrani: 20,
  ...ek
})

describe('PosUrunKarti — bilgi gosterimi', () => {
  it('urun adi, kodu ve fiyati gosterir', () => {
    const w = kur()
    expect(w.text()).toContain('Vida M8')
    expect(w.find('.product-kod').text()).toBe('8690000000001')
    expect(w.find('.product-price').text()).toContain('12,50')
  })

  it('barkod yoksa stok kodu gosterilir', () => {
    const w = kur({ urun: urun({ barkod: null }) })
    expect(w.find('.product-kod').text()).toBe('V-001')
  })

  it('stok adedi ve birimi rozet icinde gorunur', () => {
    const w = kur()
    const rozet = w.find('.product-stok-satir')
    expect(rozet.text()).toContain('100')
    expect(rozet.text()).toContain('adet')
  })

  it('KDV orani tanimliysa yuzde olarak gosterilir', () => {
    const w = kur()
    expect(w.text()).toContain('20')
  })

  it('KDV orani tanimsizsa "KDV dahil" yazar', () => {
    const w = kur({ urun: urun({ kdvOrani: null }) })
    expect(w.text()).toMatch(/KDV d[aâ]hil/i)
  })

  it('sepetteki adet rozet olarak gorunur', () => {
    const w = kur({ sepetteAdet: 5 })
    expect(w.text()).toContain('5')
  })
})

describe('PosUrunKarti — stok durumu renkleri', () => {
  it('stok sifirsa "stok yok" ve yok sinifi', () => {
    const w = kur({ urun: urun({ miktar: 0 }) })
    expect(w.find('.product-card').classes()).toContain('stok-yok')
    expect(w.text()).toMatch(/stok yok/i)
  })

  it('stok minMiktar altindaysa kritik', () => {
    const w = kur({ urun: urun({ miktar: 5, minMiktar: 20 }) })
    expect(w.find('.product-stok-satir').classes()).toContain('kritik')
  })

  it('stok 10 veya altindaysa (minMiktar olmasa da) kritik', () => {
    const w = kur({ urun: urun({ miktar: 10, minMiktar: null }) })
    expect(w.find('.product-stok-satir').classes()).toContain('kritik')
  })

  it('stok yeterliyse normal', () => {
    const w = kur({ urun: urun({ miktar: 100, minMiktar: 20 }) })
    expect(w.find('.product-stok-satir').classes()).toContain('normal')
  })

  it('stokta olmayan urun aria-disabled ile duyurulur', () => {
    const w = kur({ urun: urun({ miktar: 0 }) })
    expect(w.find('.product-card').attributes('aria-disabled')).toBe('true')
  })
})

describe('PosUrunKarti — etkilesim', () => {
  it('tiklama 1 adet sec olayi yayar', async () => {
    const w = kur()
    await w.find('.product-card').trigger('click')
    expect(w.emitted('sec')).toBeTruthy()
  })

  it('sag tiklama adet-ist olayi yayar (hizli adet penceresi)', async () => {
    const w = kur()
    await w.find('.product-card').trigger('contextmenu')
    expect(w.emitted('adet-ist')).toBeTruthy()
  })

  it('cariye ozel fiyat varsa ayri rozet gosterilir', () => {
    const w = kur({ cariFiyat: 9.5 })
    expect(w.text()).toContain('9,50')
  })

  it('cariye ozel fiyat yoksa o rozet gorunmez', () => {
    const w = kur({ cariFiyat: null })
    expect(w.find('.product-cari-fiyat').exists()).toBe(false)
  })
})

describe('PosUrunKarti — klavye odagi', () => {
  it('odakli kart aria-selected ile duyurulur (roving tabindex)', () => {
    const odaksiz = kur({ odakli: false, tabindex: -1 })
    expect(odaksiz.find('.product-card').attributes('aria-selected')).toBe('false')

    const odakli = kur({ odakli: true, tabindex: 0 })
    expect(odakli.find('.product-card').attributes('aria-selected')).toBe('true')
  })

  it('odakli kart tabindex 0 alir (ok tuslari icinde gezinir)', () => {
    const w = kur({ odakli: true, tabindex: 0 })
    expect(w.find('.product-card').attributes('tabindex')).toBe('0')
  })

  it('odakli olmayan kart tabindex -1 alir', () => {
    const w = kur({ odakli: false, tabindex: -1 })
    expect(w.find('.product-card').attributes('tabindex')).toBe('-1')
  })

  it('domId verildiginde kart o id ile isaretlenir', () => {
    const w = kur({ domId: 'pos-urun-kart-3' })
    expect(w.find('.product-card').attributes('id')).toBe('pos-urun-kart-3')
  })
})