import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import PosAdetGirisi from '../PosAdetGirisi.vue'
import i18n from '../../i18n.js'

/**
 * POS ADET GIRISI — bilesen davranis testleri.
 *
 * Bu bilesen bilerek ADEDIN KENDISINI DEGISTIRMEZ. Dogrulama, stok tavani ve
 * geri-al kaydi ust tarafta (`miktarDegistir`) yapilir; bilesen yalnizca
 * OLAY YAYAR ve `miktar` prop'unu YANSITIR. Bunun sebebi:
 *
 *   Ust taraf bir degisikligi reddedebilir (orn. depoda 3 var, kullanici 500
 *   yazdi). Deger prop'tan geldigi icin girdi kendiliginden eski degerine
 *   doner ve kirmiziya boyanir — kullanici "yazdigim deger kaydedilmedi"
 *   durumunu GORUR. Bilesen kendi adedini yazsaydi, ust taraf reddedince
 *   ekrandaki sayi ile gercek miktar ayrilirdi.
 *
 * `posAdet.spec.js` saf kurallari, `PosSepetPaneli.spec.js` panelin olay
 * baglantilarini test eder.
 */

const kur = (props = {}) =>
  mount(PosAdetGirisi, { props, global: { plugins: [i18n] } })

/** Girdiye deger yazar.
 *
 *  NOT: `wrapper.setValue()` bu girdide calismiyor — `input[type=number]`
 *  uzerinde VTU degeri DOM'a yazip `input` olayini tetikliyor, ancak Vue'nin
 *  sayıya çevirme davranisiyla deger geri aliniyor (girdi eski degerde
 *  kalıyor). Gercek tarayici akisi `element.value` + `input` olayidir;
 *  testte de ayni yol izleniyor.
 */
const yaz = async (w, deger) => {
  const g = w.find('.adet-girdi')
  g.element.value = String(deger)
  await g.trigger('input')
  await nextTick()
  return g
}

describe('PosAdetGirisi — gosterim', () => {
  it('mevcut miktari gosterir', () => {
    const w = kur({ miktar: 3 })
    expect(w.find('.adet-girdi').element.value).toBe('3')
  })

  it('adet biriminde step 1, en fazla sinir yok (stok bilinmiyorsa)', () => {
    const w = kur({ miktar: 2, birim: 'adet' })
    const g = w.find('.adet-girdi')
    expect(g.attributes('step')).toBe('1')
    expect(g.attributes('min')).toBe('1')
    expect(g.attributes('max')).toBeUndefined()
  })

  it('kg biriminde step 0.5 ve en kucuk 0.5', () => {
    const w = kur({ miktar: 2, birim: 'Kg' })
    const g = w.find('.adet-girdi')
    expect(g.attributes('step')).toBe('0.5')
    expect(g.attributes('min')).toBe('0.5')
  })

  it('stok biliniyorsa en fazla degeri girdiye yazar ve baslikta gosterir', () => {
    const w = kur({ miktar: 2, stokMiktari: 5, birim: 'adet' })
    expect(w.find('.adet-girdi').attributes('max')).toBe('5')
    expect(w.find('.adet-girdi').attributes('title')).toContain('5')
  })
})

describe('PosAdetGirisi — artir/azalt olaylari', () => {
  it('azalt dugmesi azalt yayar', async () => {
    const w = kur({ miktar: 2 })
    await w.findAll('.adet-btn')[0].trigger('click')
    expect(w.emitted('azalt')).toHaveLength(1)
  })

  it('artir dugmesi artir yayar', async () => {
    const w = kur({ miktar: 2 })
    await w.findAll('.adet-btn')[1].trigger('click')
    expect(w.emitted('artir')).toHaveLength(1)
  })

  it('en kucuk adetteyken azalt DEVRE DISI (satir silme bu is degil)', () => {
    const w = kur({ miktar: 1, birim: 'adet' })
    expect(w.findAll('.adet-btn')[0].attributes('disabled')).toBeDefined()
  })

  it('stok tavanindayken artir DEVRE DISI', () => {
    const w = kur({ miktar: 3, stokMiktari: 3 })
    expect(w.findAll('.adet-btn')[1].attributes('disabled')).toBeDefined()
  })

  it('stok bilinmiyorsa artir hep acik kalir', () => {
    const w = kur({ miktar: 999, stokMiktari: null })
    expect(w.findAll('.adet-btn')[1].attributes('disabled')).toBeUndefined()
  })

  it('disabled iken hicbir olay yayilmaz', async () => {
    const w = kur({ miktar: 2, disabled: true })
    await w.findAll('.adet-btn')[1].trigger('click')
    expect(w.emitted('artir')).toBeFalsy()
  })
})

describe('PosAdetGirisi — girdiden commit', () => {
  it('change ile degeri commit yayar', async () => {
    const w = kur({ miktar: 2 })
    const g = await yaz(w, 5)
    await g.trigger('change')
    // Vue, `input[type=number]` üzerinde v-model değerini otomatik sayıya
    // çevirir; bu yüzden commit sayı gelir.
    expect(w.emitted('commit')).toEqual([[5]])
  })

  it('Enter ile commit yazar', async () => {
    const w = kur({ miktar: 2 })
    const g = await yaz(w, 5)
    await g.trigger('keydown.enter')
    expect(w.emitted('commit').length).toBeGreaterThanOrEqual(1)
  })

  it('degisiklik olmadan Enter da commit yazar (ayni deger teyidi)', async () => {
    const w = kur({ miktar: 2 })
    await w.find('.adet-girdi').trigger('keydown.enter')
    expect(w.emitted('commit')).toEqual([[2]])
  })

  it('ust taraf KABUL ETMEZSE girdi eski degerine doner ve uyari cikar', async () => {
    const w = kur({ miktar: 2 })
    const g = await yaz(w, 500)
    await g.trigger('change')
    await nextTick()
    // `miktar` prop'u değişmedi → değer uygulanmadı → girdi geri döndü.
    expect(w.find('.adet-girdi').element.value).toBe('2')
    expect(w.find('.adet-uyari').exists()).toBe(true)
    expect(w.find('.adet-girdi').attributes('aria-invalid')).toBe('true')
  })

  it('ust taraf DEGERI KABUL EDERSE uyari isareti kalkar', async () => {
    const w = kur({ miktar: 2 })
    const g = await yaz(w, 5)
    await g.trigger('change')
    await w.setProps({ miktar: 5 })
    await nextTick()
    expect(w.find('.adet-girdi').element.value).toBe('5')
    expect(w.find('.adet-uyari').exists()).toBe(false)
  })

  it('ust taraf stok tavana kirptiginda prop ile geri yazar', async () => {
    const w = kur({ miktar: 2, stokMiktari: 3 })
    const g = await yaz(w, 500)
    await g.trigger('change')
    await nextTick()
    // Ust taraf 3'e kirpmis olabilir → prop guncellenince girdi 3 olur.
    await w.setProps({ miktar: 3 })
    await nextTick()
    expect(w.find('.adet-girdi').element.value).toBe('3')
  })

  it('Escape degisikligi iptal eder (eski degere doner, commit YOK)', async () => {
    const w = kur({ miktar: 2 })
    const g = await yaz(w, 9)
    expect(g.element.value).toBe('9')
    await g.trigger('keydown.esc')
    await nextTick()
    expect(w.find('.adet-girdi').element.value).toBe('2')
    expect(w.emitted('commit')).toBeFalsy()
  })
})

describe('PosAdetGirisi — girdi ici klavye', () => {
  it('ArrowUp artir yayar', async () => {
    const w = kur({ miktar: 2 })
    await w.find('.adet-girdi').trigger('keydown.up')
    expect(w.emitted('artir')).toHaveLength(1)
  })

  it('ArrowDown azalt yayar', async () => {
    const w = kur({ miktar: 2 })
    await w.find('.adet-girdi').trigger('keydown.down')
    expect(w.emitted('azalt')).toHaveLength(1)
  })
})

describe('PosAdetGirisi — prop degisimi senkronlar', () => {
  it('ust taraf artirdiginda girdi guncellenir', async () => {
    const w = kur({ miktar: 2 })
    await w.setProps({ miktar: 7 })
    await nextTick()
    expect(w.find('.adet-girdi').element.value).toBe('7')
  })

  it('ust taraf uyariyi temizlediginde isaret kalkar', async () => {
    const w = kur({ miktar: 2 })
    const g = await yaz(w, 99)
    await g.trigger('change')
    await nextTick()
    expect(w.find('.adet-uyari').exists()).toBe(true)

    await w.setProps({ miktar: 4 })
    await nextTick()
    expect(w.find('.adet-uyari').exists()).toBe(false)
  })
})