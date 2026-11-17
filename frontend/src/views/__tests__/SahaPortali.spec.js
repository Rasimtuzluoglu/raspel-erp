import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import i18n from '../../i18n.js'
import SahaPortali from '../SahaPortali.vue'

const api = vi.hoisted(() => ({
  sahaAPI: { ozet: vi.fn() },
  teslimatAPI: {
    teslimatlar: vi.fn(),
    teslimEt: vi.fn(),
    teslimEtSiparis: vi.fn(),
    durumGuncelle: vi.fn(),
    fotoYukle: vi.fn()
  },
  notAPI: { getAll: vi.fn(), create: vi.fn() },
  personelIzinAPI: { getAll: vi.fn(), create: vi.fn() },
  personelMasrafTalepAPI: { getKullaniciTalepleri: vi.fn(), create: vi.fn() },
  tahsilatAPI: { gir: vi.fn(), ozet: vi.fn() },
  cariHesapAPI: { search: vi.fn(), getAll: vi.fn() },
  stokAPI: { ara: vi.fn(), getAll: vi.fn() },
  kasaAPI: { getAll: vi.fn() },
  bankaAPI: { getAll: vi.fn() },
  siparisAPI: { create: vi.fn(), getAll: vi.fn(), durumGuncelle: vi.fn() },
  belgeAPI: { yukle: vi.fn() }
}))

vi.mock('../../api/index.js', () => api)

vi.mock('../../stores/authStore.js', () => ({
  useAuthStore: () => ({
    kullanici: { id: 9, displayName: 'Ali Sürücü', role: 'USER' }
  })
}))

vi.mock('primevue/usetoast', () => ({
  useToast: () => ({ add: vi.fn() })
}))

const ozet = {
  tarih: '2026-11-17',
  teslimat: { bekleyen: 2, yolda: 1, bugunTeslimEdilen: 1, geciken: 0 },
  ziyaret: { bugun: 1, sonZiyaretTarihi: null },
  talepler: { bekleyenIzin: 1, bekleyenMasraf: 2 }
}

const teslimatlar = [
  {
    id: 5,
    faturaNumarasi: 'FTR-0005',
    teslimatAdresi: 'Kadıköy / İstanbul',
    musteriAdi: 'Yılmaz Ticaret',
    musteriTelefon: '05001112233',
    durum: 'BEKLEMEDE',
    gecikti: false
  },
  {
    id: 6,
    faturaNumarasi: 'FTR-0006',
    teslimatAdresi: 'Bostancı / İstanbul',
    musteriAdi: 'Demir Gıda',
    durum: 'YOLDA',
    gecikti: true
  }
]

const DialogStub = {
  name: 'Dialog',
  props: ['visible'],
  template: '<div v-if="visible"><slot /><slot name="footer" /></div>'
}

const AppDialogStub = {
  name: 'AppDialog',
  props: ['visible'],
  template: '<div v-if="visible"><slot /><slot name="footer" /></div>'
}

const ButtonStub = {
  name: 'Button',
  props: ['label', 'disabled', 'loading'],
  emits: ['click'],
  template: '<button type="button" :disabled="disabled || loading" @click="$emit(\'click\')">{{ label }}</button>'
}

const ImzaPadStub = {
  name: 'ImzaPad',
  emits: ['imza-degisti'],
  template: '<button type="button" data-test="test-imza-ciz" @click="$emit(\'imza-degisti\', \'data:image/png;base64,IMZA\')">ciz</button>'
}

const stubs = {
  Dialog: DialogStub,
  AppDialog: AppDialogStub,
  Button: ButtonStub,
  ImzaPad: ImzaPadStub,
  Dropdown: true,
  InputText: true,
  Textarea: true,
  Tag: true,
  Toast: true,
  GorevNotPaneli: true
}

const kur = async () => {
  const wrapper = mount(SahaPortali, {
    global: { plugins: [i18n], stubs }
  })
  await flushPromises()
  return wrapper
}

const imzaSheetAc = async (wrapper) => {
  await wrapper.get('[data-test="saha-teslimat-karti"] [data-test="saha-teslimat-imza"]').trigger('click')
  await nextTick()
  await wrapper.get('[data-test="saha-imza-ad"]').setValue('Ali Veli')
  await wrapper.get('[data-test="test-imza-ciz"]').trigger('click')
}

let wrapper = null
let cevrimiciTest = true

beforeEach(() => {
  setActivePinia(createPinia())
  localStorage.clear()
  Object.values(api).forEach((mod) => Object.values(mod).forEach((fn) => fn.mockReset()))
  api.sahaAPI.ozet.mockResolvedValue({ data: ozet })
  api.teslimatAPI.teslimatlar.mockResolvedValue({ data: JSON.parse(JSON.stringify(teslimatlar)) })
  api.notAPI.getAll.mockResolvedValue({ data: { content: [] } })
  api.personelIzinAPI.getAll.mockResolvedValue({ data: { content: [] } })
  api.personelMasrafTalepAPI.getKullaniciTalepleri.mockResolvedValue({ data: [] })
  api.kasaAPI.getAll.mockResolvedValue({ data: [] })
  api.bankaAPI.getAll.mockResolvedValue({ data: [] })
  api.teslimatAPI.teslimEt.mockResolvedValue({ data: {} })
  cevrimiciTest = true
  vi.spyOn(navigator, 'onLine', 'get').mockImplementation(() => cevrimiciTest)
})

afterEach(() => {
  wrapper?.unmount()
  wrapper = null
  vi.restoreAllMocks()
})

describe('SahaPortali', () => {
  it('acilista yalnizca ozet ve aktif sekme verisini ister', async () => {
    wrapper = await kur()

    expect(api.sahaAPI.ozet).toHaveBeenCalledTimes(1)
    expect(api.teslimatAPI.teslimatlar).toHaveBeenCalledTimes(1)
    expect(api.teslimatAPI.teslimatlar).toHaveBeenCalledWith(9)
    expect(api.personelIzinAPI.getAll).not.toHaveBeenCalled()
    expect(api.personelMasrafTalepAPI.getKullaniciTalepleri).not.toHaveBeenCalled()
    expect(api.notAPI.getAll).not.toHaveBeenCalled()
  })

  it('bugun kartlarini ve teslimat kartlarini gosterir', async () => {
    wrapper = await kur()

    expect(wrapper.find('[data-test="saha-ozet-teslimat"]').exists()).toBe(true)
    expect(wrapper.findAll('[data-test="saha-teslimat-karti"]')).toHaveLength(2)
    expect(wrapper.get('[data-test="saha-teslimat-siradaki"]').exists()).toBe(true)
  })

  it('sekme degisince yalniz o sekmenin verisi yuklenir', async () => {
    wrapper = await kur()

    await wrapper.get('[data-test="saha-sekme-talepler"]').trigger('click')
    await flushPromises()

    expect(api.personelMasrafTalepAPI.getKullaniciTalepleri).toHaveBeenCalledTimes(1)
    expect(api.personelIzinAPI.getAll).not.toHaveBeenCalled()
    expect(api.notAPI.getAll).not.toHaveBeenCalled()

    await wrapper.get('[data-test="saha-talep-izin"]').trigger('click')
    await flushPromises()

    expect(api.personelIzinAPI.getAll).toHaveBeenCalledTimes(1)

    await wrapper.get('[data-test="saha-sekme-ziyaret"]').trigger('click')
    await flushPromises()

    expect(api.notAPI.getAll).toHaveBeenCalledTimes(1)
    // Alt sekme tekrar açıldığında önbellekten döner (ek istek yok).
    await wrapper.get('[data-test="saha-sekme-talepler"]').trigger('click')
    await flushPromises()
    expect(api.personelIzinAPI.getAll).toHaveBeenCalledTimes(1)
  })

  it('ozet karti ilgili sekmeyi acar', async () => {
    wrapper = await kur()

    await wrapper.get('[data-test="saha-ozet-ziyaret"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-test="saha-sekme-icerik-ziyaret"]').exists()).toBe(true)
    expect(api.notAPI.getAll).toHaveBeenCalled()
    expect(localStorage.getItem('raspel_saha_sekme')).toBe('ziyaret')
  })

  it('cevrimdisi imzali teslimat kuyruga girer ve gonderilmez', async () => {
    cevrimiciTest = false
    wrapper = await kur()

    await imzaSheetAc(wrapper)
    await wrapper.get('[data-test="saha-imza-kuyruk"]').trigger('click')
    await flushPromises()

    expect(api.teslimatAPI.teslimEt).not.toHaveBeenCalled()
    expect(wrapper.find('[data-test="saha-kuyruk-cubugu"]').exists()).toBe(true)

    const depo = JSON.parse(localStorage.getItem('raspel_saha_kuyruk_depo') || '[]')
    expect(depo).toHaveLength(1)
    expect(depo[0].tur).toBe('TESLIMAT')
    expect(depo[0].payload.teslimatId).toBe(5)
    expect(depo[0].dosyalar[0].dataUrl).toContain('IMZA')
  })

  it('cevrimici imzali teslimat gonderilir ve liste yenilenir', async () => {
    wrapper = await kur()

    await imzaSheetAc(wrapper)
    await wrapper.get('[data-test="saha-imza-gonder"]').trigger('click')
    await flushPromises()

    expect(api.teslimatAPI.teslimEt).toHaveBeenCalledTimes(1)
    const [teslimatId, govde] = api.teslimatAPI.teslimEt.mock.calls[0]
    expect(teslimatId).toBe(5)
    expect(govde.teslimAlanAd).toBe('Ali Veli')
    expect(api.teslimatAPI.teslimatlar).toHaveBeenCalledTimes(2)
    expect(api.sahaAPI.ozet).toHaveBeenCalledTimes(2)
  })

  it('online olayinda kuyruk gonderilir ve cubuk kaybolur', async () => {
    cevrimiciTest = false
    wrapper = await kur()

    await imzaSheetAc(wrapper)
    await wrapper.get('[data-test="saha-imza-kuyruk"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-test="saha-kuyruk-cubugu"]').exists()).toBe(true)

    cevrimiciTest = true
    window.dispatchEvent(new Event('online'))
    await flushPromises()

    expect(api.teslimatAPI.teslimEt).toHaveBeenCalledTimes(1)
    expect(wrapper.find('[data-test="saha-kuyruk-cubugu"]').exists()).toBe(false)
  })

  it('eski siparis kuyrugu teslimEtSiparis ile gonderilir', async () => {
    localStorage.setItem('raspel_saha_kuyrugu', JSON.stringify([
      {
        tur: 'TESLIMAT',
        siparisId: 7,
        teslimAlanAd: 'Eski Müşteri',
        teslimNotu: '',
        imzaDataUrl: 'data:image/png;base64,ESKI'
      }
    ]))

    wrapper = await kur()
    await flushPromises()

    expect(api.teslimatAPI.teslimEtSiparis).toHaveBeenCalledTimes(1)
    expect(api.teslimatAPI.teslimEtSiparis.mock.calls[0][0]).toBe(7)
    expect(api.teslimatAPI.teslimEt).not.toHaveBeenCalled()
  })

  it('talepler sekmesi tek istekle acilir, izinler alt sekmede yuklenir', async () => {
    localStorage.setItem('raspel_saha_sekme', 'talepler')
    wrapper = await kur()

    expect(api.personelMasrafTalepAPI.getKullaniciTalepleri).toHaveBeenCalledTimes(1)
    expect(api.personelIzinAPI.getAll).not.toHaveBeenCalled()

    await wrapper.get('[data-test="saha-talep-izin"]').trigger('click')
    await flushPromises()

    expect(api.personelIzinAPI.getAll).toHaveBeenCalledTimes(1)
  })

  it('tahsilat sekmesi acilista yalniz kasa istegi yapar', async () => {
    localStorage.setItem('raspel_saha_sekme', 'tahsilat')
    wrapper = await kur()

    expect(api.sahaAPI.ozet).toHaveBeenCalledTimes(1)
    expect(api.kasaAPI.getAll).toHaveBeenCalledTimes(1)
    expect(api.bankaAPI.getAll).not.toHaveBeenCalled()

    const durum = wrapper.vm.$.setupState
    durum.tahsilatForm.odemeYontemi = 'HAVALE'
    await nextTick()
    await flushPromises()

    expect(api.bankaAPI.getAll).toHaveBeenCalledTimes(1)
  })

  it('tahsilat hata sonrasi ayni idempotency anahtarini kullanir', async () => {
    api.tahsilatAPI.gir
      .mockRejectedValueOnce(new Error('zaman asimi'))
      .mockResolvedValueOnce({ data: {} })
    wrapper = await kur()

    const durum = wrapper.vm.$.setupState
    durum.tahsilatForm.cariHesapId = 3
    durum.tahsilatForm.tutar = 100
    await durum.tahsilatKaydet()
    await durum.tahsilatKaydet()

    const birinciAnahtar = api.tahsilatAPI.gir.mock.calls[0][1]
    const ikinciAnahtar = api.tahsilatAPI.gir.mock.calls[1][1]
    expect(birinciAnahtar).toBeTruthy()
    expect(ikinciAnahtar).toBe(birinciAnahtar)
  })

  it('gonderim hatasinda imza sheet acik kalir ve kuyruk secenegi sunar', async () => {
    api.teslimatAPI.teslimEt.mockRejectedValueOnce({
      response: { status: 500, data: { message: 'sunucu hatasi' } }
    })
    wrapper = await kur()

    await imzaSheetAc(wrapper)
    await wrapper.get('[data-test="saha-imza-gonder"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-test="saha-imza-sheet"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="saha-imza-kuyruk"]').exists()).toBe(true)
  })

  it('baska kullanicinin kuyruk kaydi gonderilmez', async () => {
    localStorage.setItem('raspel_saha_kuyruk_depo', JSON.stringify([
      {
        id: 'yabanci-1',
        clientKey: 'yabanci-key',
        tur: 'TESLIMAT',
        payload: { teslimatId: 5, teslimAlanAd: 'X' },
        dosyalar: [],
        durum: 'BEKLIYOR',
        deneme: 0,
        sonHata: null,
        olusturma: '2026-11-17T08:00:00Z',
        sirketId: 1,
        kullaniciId: 999
      }
    ]))

    wrapper = await kur()
    await flushPromises()

    expect(api.teslimatAPI.teslimEt).not.toHaveBeenCalled()
    expect(wrapper.find('[data-test="saha-kuyruk-cubugu"]').text()).toContain('1')
  })

  it('masraf fisi yuklenemezse kayit kuyrukta kalir ve talep tekrarlanmaz', async () => {
    api.personelMasrafTalepAPI.create.mockResolvedValue({ data: { id: 55 } })
    api.belgeAPI.yukle.mockRejectedValueOnce(new Error('depolama hatasi'))
    localStorage.setItem('raspel_saha_kuyruk_depo', JSON.stringify([
      {
        id: 'masraf-1',
        clientKey: 'masraf-key',
        tur: 'MASRAF',
        payload: { tur: 'MASRAF', kategori: 'YAKIT', tutar: 100, aciklama: 'yakit' },
        dosyalar: [{ ad: 'fis.jpg', mime: 'image/jpeg', dataUrl: 'data:image/jpeg;base64,FIS' }],
        durum: 'BEKLIYOR',
        deneme: 0,
        sonHata: null,
        olusturma: '2026-11-17T08:00:00Z',
        sirketId: 1,
        kullaniciId: 9
      }
    ]))

    wrapper = await kur()
    await flushPromises()

    expect(api.personelMasrafTalepAPI.create).toHaveBeenCalledTimes(1)
    expect(api.belgeAPI.yukle).toHaveBeenCalledTimes(1)
    const depo = JSON.parse(localStorage.getItem('raspel_saha_kuyruk_depo') || '[]')
    expect(depo).toHaveLength(1)
    expect(depo[0].payload.olusanId).toBe(55)
    expect(depo[0].deneme).toBe(1)
  })
})
