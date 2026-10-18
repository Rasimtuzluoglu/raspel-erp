import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import PrimeVue from 'primevue/config'
import i18n from '../../i18n.js'
import CariKart360Dialog from '../CariKart360Dialog.vue'

/**
 * FAZ 3 — Cari 360 penceresi kullanislilik sozlesmesi.
 *
 * 360 penceresi tek detay merkezidir: kimlik karti (avatar/ad/tur) + 4 KPI
 * hucresi + "Ekstre" ve "Adresler" aksiyonlari (ana bilesene event yayar).
 */

vi.mock('../../api/index.js', () => ({
  cariHesapAPI: {
    kart: vi.fn().mockResolvedValue({
      data: {
        cariId: 1,
        cariAd: 'ACME Ltd',
        tur: 'Musteri',
        aktif: true,
        telefon: '0555 111 22 33',
        email: 'info@acme.com',
        temsilciAd: 'Ali Veli',
        kredi: { krediLimiti: 10000, bakiye: 2500, kullanilabilirKredi: 7500, riskOrani: 25, limitAsimi: false },
        ozet: {},
        sonFaturalar: [],
        sonSiparisler: [],
        sonIadeler: [],
        firsatlar: [],
        notlar: [],
        ozelFiyatlar: [],
        taksitler: [],
        sonHareketler: []
      }
    })
  }
}))

const stubs = {
  Dialog: { template: '<div><slot /></div>' },
  Button: { props: ['label'], template: '<button>{{ label }}<slot /></button>' },
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' },
  AppDataTable: { template: '<div><slot /></div>' },
  Column: true,
  TabView: { template: '<div><slot /></div>' },
  TabPanel: { template: '<div><slot /></div>' }
}

const kur = async (ekstraProps = {}) => {
  const w = mount(CariKart360Dialog, {
    props: { visible: false, cariId: 1, cari: { fotoThumbUrl: 'http://x/a.png' }, ...ekstraProps },
    global: { plugins: [PrimeVue, i18n], stubs }
  })
  await w.setProps({ visible: true })
  await new Promise((r) => setTimeout(r, 20))
  return w
}

describe('CariKart360Dialog — Faz3', () => {
  it('kimlik karti ve 4 KPI hucresi gosterilir', async () => {
    const w = await kur()
    expect(w.find('.kart-kimlik-kart').exists()).toBe(true)
    expect(w.find('.kart-kimlik-kart').text()).toContain('ACME Ltd')
    expect(w.find('.kart-avatar').exists()).toBe(true)
    expect(w.findAll('.kart-kpi .kpi-hucre')).toHaveLength(4)
  })

  it('Ekstre butonu ekstre olayi yayar', async () => {
    const w = await kur()
    const btn = w.findAll('button').find((b) => b.text().includes(i18n.global.t('cariHesaplar.ekstreYazdir')))
    expect(btn).toBeTruthy()
    await btn.trigger('click')
    expect(w.emitted('ekstre')).toBeTruthy()
  })

  it('Adresler butonu adresler olayi yayar', async () => {
    const w = await kur()
    const btn = w.findAll('button').find((b) => b.text().includes(i18n.global.t('cariHesaplar.adresler')))
    expect(btn).toBeTruthy()
    await btn.trigger('click')
    expect(w.emitted('adresler')).toBeTruthy()
  })
})
