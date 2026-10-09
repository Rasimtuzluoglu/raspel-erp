import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PrimeVue from 'primevue/config'
import i18n from '../../i18n.js'
import ListeDurumu from '../../components/ListeDurumu.vue'

const mountLD = (props = {}) =>
  mount(ListeDurumu, {
    props,
    global: { plugins: [PrimeVue, i18n], stubs: { Skeleton: true } }
  })

describe('ListeDurumu.vue', () => {
  it('yukleniyor durumunda iskelet gosterir', () => {
    const w = mountLD({ yukleniyor: true })
    expect(w.find('.liste-durumu__iskelet').exists()).toBe(true)
    expect(w.find('.liste-durumu__bos').exists()).toBe(false)
  })

  it('yukleniyor bittiginde bos mesaji gosterilir', () => {
    const w = mountLD({ bos: true, bosMesaj: 'Kayit bulunamadi' })
    expect(w.find('.liste-durumu__bos').exists()).toBe(true)
    expect(w.text()).toContain('Kayit bulunamadi')
  })

  it('yukleniyor sirasinda bos durum GOSTERILMEZ', () => {
    // Kritik ayrim: veri gelmeden "kayit yok" demek yanilticiydi.
    const w = mountLD({ yukleniyor: true, bos: true, bosMesaj: 'Kayit yok' })
    expect(w.find('.liste-durumu__bos').exists()).toBe(false)
    expect(w.text()).not.toContain('Kayit yok')
  })

  it('hata varsa bos durum yerine hata gosterilir', () => {
    const w = mountLD({ bos: true, bosMesaj: 'Kayit yok', hata: 'Sunucuya ulasilamiyor' })
    expect(w.find('.liste-durumu__hata').exists()).toBe(true)
    expect(w.find('.liste-durumu__bos').exists()).toBe(false)
    expect(w.text()).toContain('Sunucuya ulasilamiyor')
  })

  it('hata durumunda yeniden dene butonu cagirilabilir', async () => {
    const w = mountLD({ hata: 'Hata', yenidenDene: () => {} })
    expect(w.find('.liste-durumu__hata').exists()).toBe(true)
    expect(w.text()).toBeTruthy()
  })

  it('eylem butonu tiklandiginda eylem olayi yayinlanir', async () => {
    const w = mountLD({ bos: true, bosMesaj: 'Bos', eylemEtiketi: 'Yeni Kayit' })
    const btn = w.find('.liste-durumu__bos button')
    expect(btn.exists()).toBe(true)
    await btn.trigger('click')
    expect(w.emitted('eylem')).toBeTruthy()
  })

  it('role=status ve aria-live erisilebilirlik icin mevcut', () => {
    const w = mountLD({ bos: true, bosMesaj: 'Bos' })
    expect(w.find('[role="status"]').exists()).toBe(true)
    expect(w.find('[aria-live="polite"]').exists()).toBe(true)
  })

  it('hicbir durum yoksa bos bir cerceve render eder (layout kaymaz)', () => {
    const w = mountLD({})
    expect(w.find('.liste-durumu').exists()).toBe(true)
    expect(w.find('.liste-durumu__iskelet').exists()).toBe(false)
    expect(w.find('.liste-durumu__hata').exists()).toBe(false)
    expect(w.find('.liste-durumu__bos').exists()).toBe(false)
  })

  it('Error nesnesi hata olarak da kabul edilir', () => {
    const w = mountLD({ hata: new Error('Baglanti hatasi') })
    expect(w.find('.liste-durumu__hata').exists()).toBe(true)
    expect(w.text()).toContain('Baglanti hatasi')
  })
})