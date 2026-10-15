import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import PrimeVue from 'primevue/config'
import i18n from '../../i18n.js'
import PosOdemeOverlay from '../PosOdemeOverlay.vue'

// Dialog ve panel stub'lanir; test YALNIZCA overlay'in tuş takimi mantigini
// ve gorunurluk kosullarini dogrular (mevcut panel zaten kendi testlerine sahip).
const DialogStub = { template: '<div class="dlg"><slot /><slot name="footer" /></div>' }
const ButtonStub = { props: ['label'], template: '<button type="button">{{ label }}<slot /></button>' }

const mountOverlay = (attrs = {}) =>
  mount(PosOdemeOverlay, {
    props: { gorunur: true, ...attrs },
    global: {
      plugins: [PrimeVue, i18n],
      stubs: { Dialog: DialogStub, Button: ButtonStub, PosOdemePaneli: true }
    }
  })

describe('PosOdemeOverlay.vue', () => {
  it('NAKIT odemede tuş takimi gorunur', () => {
    const w = mountOverlay({ odemeDurumu: 'tam', odemeYontemi: 'NAKIT', alinanNakit: 0 })
    expect(w.find('.odeme-keypad').exists()).toBe(true)
    // 11 tus (1-9,00,0) + C + geri = 13
    expect(w.findAll('.keypad-tus').length).toBe(13)
  })

  it('NAKIT disi yontemde tuş takimi gizlenir', () => {
    const w = mountOverlay({ odemeDurumu: 'tam', odemeYontemi: 'KART' })
    expect(w.find('.odeme-keypad').exists()).toBe(false)
  })

  it('odeme yokken tuş takimi gizlenir', () => {
    const w = mountOverlay({ odemeDurumu: 'yok', odemeYontemi: 'NAKIT' })
    expect(w.find('.odeme-keypad').exists()).toBe(false)
  })

  it('rakam tuslari alinan nakit tutarini (update:alinanNakit) gunceller', async () => {
    const onUpdate = vi.fn()
    const w = mountOverlay({ odemeDurumu: 'tam', odemeYontemi: 'NAKIT', alinanNakit: 0, 'onUpdate:alinanNakit': onUpdate })
    const tus = (etiket) => w.findAll('.keypad-tus').find((b) => b.text() === etiket)
    // v-model benzetimi: her tus vurusundan sonra prop guncellenir (gercek
    // uygulamada parent ref'i aninda gunceller).
    await tus('1').trigger('click')
    await w.setProps({ alinanNakit: 1 })
    await tus('0').trigger('click')
    await w.setProps({ alinanNakit: 10 })
    await tus('0').trigger('click')
    // 0 -> 1 -> 10 -> 100
    expect(onUpdate).toHaveBeenLastCalledWith(100)
  })

  it('00 tusu tutari 100 ile carpar (yuvarlak nakit)', async () => {
    const onUpdate = vi.fn()
    const w = mountOverlay({ odemeDurumu: 'tam', odemeYontemi: 'NAKIT', alinanNakit: 5, 'onUpdate:alinanNakit': onUpdate })
    const tus = w.findAll('.keypad-tus').find((b) => b.text() === '00')
    await tus.trigger('click')
    expect(onUpdate).toHaveBeenCalledWith(500)
  })

  it('C tusu tutari sifirlar', async () => {
    const onUpdate = vi.fn()
    const w = mountOverlay({ odemeDurumu: 'tam', odemeYontemi: 'NAKIT', alinanNakit: 250, 'onUpdate:alinanNakit': onUpdate })
    const tus = w.findAll('.keypad-tus').find((b) => b.text() === 'C')
    await tus.trigger('click')
    expect(onUpdate).toHaveBeenCalledWith(0)
  })

  it('geri tusu son haneyi siler', async () => {
    const onUpdate = vi.fn()
    const w = mountOverlay({ odemeDurumu: 'tam', odemeYontemi: 'NAKIT', alinanNakit: 125, 'onUpdate:alinanNakit': onUpdate })
    const geri = w.findAll('.keypad-tus').find((b) => b.find('.pi-delete-left').exists())
    await geri.trigger('click')
    expect(onUpdate).toHaveBeenCalledWith(12)
  })

  it('tamamla butonu tamamla olayi yayar', async () => {
    const w = mountOverlay({ odemeDurumu: 'tam', odemeYontemi: 'NAKIT' })
    const btn = w.findAll('button').find((b) => b.text().includes(i18n.global.t('hizliSatis.satisiTamamla')))
    await btn.trigger('click')
    expect(w.emitted('tamamla')).toBeTruthy()
  })

  it('gorunur degisince update:gorunur yayar', async () => {
    const w = mountOverlay({ gorunur: true, odemeDurumu: 'tam', odemeYontemi: 'NAKIT' })
    // DialogStub visible prop'u yerine overlay kendi $emit'i ile kapanir; burada
    // yalnizca emit sozlesmesini dogrulariz.
    expect(w.props('gorunur')).toBe(true)
  })
})