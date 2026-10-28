import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ToastService from 'primevue/toastservice'
import i18n from '../../i18n.js'
import { onaylarAPI, excelAPI } from '../../api/index.js'
import Onaylar from '../Onaylar.vue'

const { izin1, izin2, masraf1, gecmisKarar, TUM_KAYITLAR, routerPush } = vi.hoisted(() => {
  const izin1 = {
    tur: 'IZIN',
    kaynakId: 1,
    baslik: 'Ahmet Yıllık İzin',
    altBaslik: 'İnsan Kaynakları',
    tutar: 0,
    tarih: '2026-08-25',
    olusturmaTarihi: '2026-08-20T10:00:00',
    durum: 'BEKLEMEDE',
    aciklama: 'Yıllık izin talebi',
    meta: { personel: 'Ahmet Yılmaz', gunSayisi: 5 }
  }
  const izin2 = {
    tur: 'IZIN',
    kaynakId: 2,
    baslik: 'Ayşe Hastalık İzni',
    altBaslik: 'Üretim',
    tutar: 0,
    tarih: '2026-08-26',
    olusturmaTarihi: '2026-08-21T10:00:00',
    durum: 'BEKLEMEDE',
    aciklama: '',
    meta: {}
  }
  const masraf1 = {
    tur: 'MASRAF',
    kaynakId: 10,
    baslik: 'Mehmet Yakıt Masrafı',
    altBaslik: 'Saha',
    tutar: 500,
    tarih: '2026-08-21',
    olusturmaTarihi: '2026-08-21T09:00:00',
    durum: 'BEKLEMEDE',
    aciklama: 'Yakıt fişi',
    meta: { kategori: 'YAKIT' }
  }
  const satinalma1 = {
    tur: 'SATINALMA',
    kaynakId: 20,
    baslik: 'SAT-2026-001',
    altBaslik: 'Satınalma',
    tutar: 12000,
    tarih: '2026-08-22',
    olusturmaTarihi: '2026-08-22T09:00:00',
    durum: 'TASLAK',
    aciklama: 'Ofis malzemesi',
    meta: {}
  }
  const gecmisKarar = {
    id: 77,
    tur: 'IZIN',
    kaynakId: 1,
    karar: 'ONAY',
    gerekce: '',
    oncekiDurum: 'BEKLEMEDE',
    yeniDurum: 'ONAYLANDI',
    kararVerenId: 9,
    kararVerenAd: 'Muhasebe Kullanıcı',
    olusturmaTarihi: '2026-08-21T09:30:00'
  }
  return {
    izin1,
    izin2,
    masraf1,
    satinalma1,
    gecmisKarar,
    TUM_KAYITLAR: [izin1, izin2, masraf1, satinalma1],
    routerPush: vi.fn()
  }
})

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush })
}))

vi.mock('../../api/index.js', () => ({
  onaylarAPI: {
    ozet: vi.fn(() =>
      Promise.resolve({
        data: { toplam: 4, izin: 2, masraf: 1, satinalma: 1, siparis: 0 }
      })
    ),
    bekleyen: vi.fn((params = {}) => {
      const icerik = params.tur
        ? TUM_KAYITLAR.filter((kayit) => kayit.tur === params.tur)
        : TUM_KAYITLAR
      return Promise.resolve({
        data: {
          content: icerik,
          totalElements: icerik.length,
          totalPages: 1,
          number: params.page || 0,
          size: params.size || 25
        }
      })
    }),
    karar: vi.fn(() => Promise.resolve({ data: {} })),
    toplu: vi.fn(() => Promise.resolve({ data: { basarili: 2, atlanan: [] } })),
    gecmis: vi.fn(() =>
      Promise.resolve({ data: { content: [gecmisKarar], totalElements: 1, totalPages: 1 } })
    )
  },
  excelAPI: {
    onaylar: vi.fn(() => Promise.resolve({ data: new Blob() }))
  }
}))

const stubs = {
  PageHeader: {
    props: ['title', 'description'],
    template: '<div class="ph"><span class="ph-title">{{ title }}</span><slot name="actions" /></div>'
  },
  KpiKart: {
    props: ['baslik', 'deger'],
    template: '<div class="kpi">{{ baslik }}:{{ deger }}</div>'
  },
  SkeletonLoader: { template: '<div class="skeleton-stub" />' },
  EmptyState: { props: ['message'], template: '<div class="empty">{{ message }}</div>' },
  AppDataTable: {
    props: ['value', 'selection', 'loading'],
    template:
      '<div class="adt"><div class="batch"><slot name="batch-actions" /></div><slot name="empty" /><slot /></div>'
  },
  Column: true,
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' },
  Button: {
    props: ['label', 'disabled', 'loading', 'ariaLabel'],
    template: '<button class="btn" :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>'
  },
  InputText: true,
  IconField: { template: '<div><slot /></div>' },
  InputIcon: true,
  Select: true,
  Dropdown: true,
  DatePicker: true,
  Dialog: {
    props: ['visible', 'header'],
    template:
      '<div v-if="visible" class="dialog"><span class="dialog-header">{{ header }}</span><slot /><slot name="footer" /></div>'
  },
  Textarea: {
    props: ['modelValue'],
    emits: ['update:modelValue'],
    template:
      '<textarea class="textarea" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />'
  },
  OnayDetayDrawer: {
    props: ['visible', 'kayit'],
    template: '<div v-if="visible" class="drawer">{{ kayit ? kayit.baslik : "" }}</div>'
  }
}

const kur = () =>
  mount(Onaylar, {
    global: { plugins: [ToastService, i18n], stubs }
  })

describe('Onaylar.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    window.URL.createObjectURL = vi.fn(() => 'blob:onaylar')
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('5 KPI kartı, başlık ve 6 sekmeyi bekleyen sayılarıyla gösterir', async () => {
    const w = kur()
    await flushPromises()

    expect(w.find('.onaylar-sayfasi').exists()).toBe(true)
    expect(w.find('.ph-title').text()).toBe('Yönetici & Muhasebe Onay Merkezi')
    const kpiMetni = w.findAll('.kpi').map((kart) => kart.text())
    expect(kpiMetni).toEqual([
      'Toplam Bekleyen:4',
      'İzin:2',
      'Masraf / Avans:1',
      'Satınalma:1',
      'Sipariş:0'
    ])

    const sekmeler = w.findAll('.onay-sekme')
    expect(sekmeler).toHaveLength(6)
    expect(sekmeler.map((sekme) => sekme.text())).toEqual([
      'Tümü4',
      'İzin2',
      'Masraf1',
      'Satınalma1',
      'Sipariş',
      'Geçmiş'
    ])
    expect(sekmeler[0].attributes('aria-selected')).toBe('true')
    expect(w.find('.sr-only[aria-live="polite"]').exists()).toBe(true)
  })

  it('sekmeyi hatırlar ve seçilen tür için sunucu parametreleriyle yükler', async () => {
    localStorage.setItem('raspel_onaylar_tab', '3')
    const w = kur()
    await flushPromises()

    expect(onaylarAPI.bekleyen).toHaveBeenCalledWith(
      expect.objectContaining({ tur: 'SATINALMA', page: 0, size: 25 })
    )
    expect(w.findAll('.onay-sekme')[3].classes()).toContain('aktif')

    await w.findAll('.onay-sekme')[1].trigger('click')
    await flushPromises()

    expect(localStorage.getItem('raspel_onaylar_tab')).toBe('1')
    expect(onaylarAPI.bekleyen).toHaveBeenLastCalledWith(
      expect.objectContaining({ tur: 'IZIN', page: 0, size: 25 })
    )
  })

  it('aramayı geciktirerek uygular ve filtreyi temizler', async () => {
    const w = kur()
    await flushPromises()
    const ilkCagri = onaylarAPI.bekleyen.mock.calls.length

    vi.useFakeTimers()
    w.vm.q = 'yıllık'
    w.vm.aramaDegisti()
    expect(onaylarAPI.bekleyen).toHaveBeenCalledTimes(ilkCagri)

    vi.advanceTimersByTime(300)
    await flushPromises()

    expect(onaylarAPI.bekleyen).toHaveBeenLastCalledWith(
      expect.objectContaining({ q: 'yıllık' })
    )

    w.vm.filtreTemizle()
    await flushPromises()
    expect(onaylarAPI.bekleyen).toHaveBeenLastCalledWith({})
  })

  it('tür seçiliyken lazy sayfalama ve sıralama parametrelerini gönderir', async () => {
    const w = kur()
    await flushPromises()

    w.vm.sekmeSec(1)
    await flushPromises()
    w.vm.sayfaDegisti({ page: 2, rows: 50 })
    await flushPromises()

    expect(onaylarAPI.bekleyen).toHaveBeenLastCalledWith({
      tur: 'IZIN',
      page: 2,
      size: 50
    })

    w.vm.siralaDegisti({ sortField: 'tutar', sortOrder: -1 })
    await flushPromises()

    expect(onaylarAPI.bekleyen).toHaveBeenLastCalledWith({
      tur: 'IZIN',
      page: 0,
      size: 50,
      sort: 'tutar,desc'
    })
  })

  it('tek kaydı onaylar ve listeyi/özeti tazeler', async () => {
    const w = kur()
    await flushPromises()

    await w.vm.tekOnayla(izin1)
    await flushPromises()

    expect(onaylarAPI.karar).toHaveBeenCalledWith({
      tur: 'IZIN',
      kaynakId: 1,
      karar: 'ONAY',
      gerekce: ''
    })
    expect(onaylarAPI.ozet).toHaveBeenCalledTimes(2)
    expect(onaylarAPI.bekleyen).toHaveBeenCalledTimes(2)
  })

  it('ret için modal açar, 3 karakterden kısa gerekçeyi reddeder', async () => {
    const w = kur()
    await flushPromises()

    w.vm.retAc(izin1)
    await flushPromises()

    expect(w.vm.retDialog).toBe(true)
    expect(w.find('.dialog[aria-modal="true"]').exists()).toBe(true)
    expect(w.find('.textarea').exists()).toBe(true)

    w.vm.gerekce = 'ab'
    await w.vm.retGonder()
    await flushPromises()

    expect(onaylarAPI.karar).not.toHaveBeenCalled()
    expect(w.vm.retHata).toBe('Gerekçe en az 3 karakter olmalıdır.')

    w.vm.gerekce = 'Belge eksik'
    await w.vm.retGonder()
    await flushPromises()

    expect(onaylarAPI.karar).toHaveBeenCalledWith({
      tur: 'IZIN',
      kaynakId: 1,
      karar: 'RED',
      gerekce: 'Belge eksik'
    })
    expect(w.vm.retDialog).toBe(false)
  })

  it('farklı tür seçiminde önceki tutarlı seçimi korur', async () => {
    const w = kur()
    await flushPromises()

    w.vm.secimDegisti([izin1, izin2])
    expect(w.vm.secim).toHaveLength(2)
    expect(w.vm.secimTuru).toBe('IZIN')

    w.vm.secimDegisti([izin1, masraf1])
    expect(w.vm.secim).toHaveLength(2)
    expect(w.vm.secimTuru).toBe('IZIN')
  })

  it('seçilenleri toplu onaylar ve seçimi temizler', async () => {
    const w = kur()
    await flushPromises()

    w.vm.secimDegisti([izin1, izin2])
    await w.vm.topluOnayla()
    await flushPromises()

    expect(onaylarAPI.toplu).toHaveBeenCalledWith({
      tur: 'IZIN',
      kaynakIds: [1, 2],
      karar: 'ONAY',
      gerekce: ''
    })
    expect(w.vm.secim).toEqual([])
  })

  it('toplu reddi ortak gerekçe modalıyla gönderir', async () => {
    const w = kur()
    await flushPromises()

    w.vm.secimDegisti([izin1, izin2])
    w.vm.topluRetAc()
    await flushPromises()

    expect(w.vm.retMod).toBe('toplu')
    expect(w.text()).toContain('2 kaydı reddetmek üzeresiniz')

    w.vm.gerekce = 'Toplu ret gerekçesi'
    await w.vm.retGonder()
    await flushPromises()

    expect(onaylarAPI.toplu).toHaveBeenCalledWith({
      tur: 'IZIN',
      kaynakIds: [1, 2],
      karar: 'RED',
      gerekce: 'Toplu ret gerekçesi'
    })
  })

  it('kısmi toplu sonucu atlanan kayıtlarla birlikte özetler', async () => {
    onaylarAPI.toplu.mockResolvedValueOnce({
      data: { basarili: 1, atlanan: [{ kaynakId: 2, mesaj: 'Yetki yok' }] }
    })
    const w = kur()
    await flushPromises()

    w.vm.secimDegisti([izin1, izin2])
    await w.vm.topluOnayla()
    await flushPromises()

    expect(w.vm.sonucDialog).toBe(true)
    expect(w.vm.sonuc.basarili).toBe(1)
    expect(w.vm.sonuc.atlanan).toHaveLength(1)
    expect(w.text()).toContain('Yetki yok')
    expect(w.text()).toContain('Kayıt #2')
  })

  it('detay drawer’ını seçilen kayıtla açar', async () => {
    const w = kur()
    await flushPromises()

    w.vm.detayAc(izin1)
    await flushPromises()

    expect(w.vm.detayAcik).toBe(true)
    expect(w.find('.drawer').text()).toContain('Ahmet Yıllık İzin')
  })

  it('geçmiş sekmesinde filtreleri ve sunucu sayfalamasını kullanır', async () => {
    const w = kur()
    await flushPromises()

    await w.findAll('.onay-sekme')[5].trigger('click')
    await flushPromises()

    expect(onaylarAPI.gecmis).toHaveBeenCalledWith({ page: 0, size: 25 })
    expect(w.vm.gecmisListe).toHaveLength(1)
    expect(w.vm.gecmisListe[0].karar).toBe('ONAY')

    w.vm.gecmisTur = 'IZIN'
    w.vm.gecmisKarar = 'RED'
    w.vm.gecmisQ = 'Ali'
    w.vm.gecmisFiltreDegisti()
    await flushPromises()

    expect(onaylarAPI.gecmis).toHaveBeenLastCalledWith({
      page: 0,
      size: 25,
      tur: 'IZIN',
      karar: 'RED',
      q: 'Ali'
    })

    w.vm.gecmisSayfaDegisti({ page: 1, rows: 10 })
    await flushPromises()
    expect(onaylarAPI.gecmis).toHaveBeenLastCalledWith(
      expect.objectContaining({ page: 1, size: 10 })
    )
  })

  it('geçmiş boşsa boş durum gösterir', async () => {
    onaylarAPI.gecmis.mockResolvedValueOnce({
      data: { content: [], totalElements: 0, totalPages: 0 }
    })
    const w = kur()
    await flushPromises()

    await w.findAll('.onay-sekme')[5].trigger('click')
    await flushPromises()

    expect(w.vm.gecmisListe).toEqual([])
    expect(w.find('.empty').text()).toBe('Henüz onay kararı kaydı bulunmuyor')
  })

  it('bekleyen yoksa boş durum gösterir', async () => {
    onaylarAPI.bekleyen.mockResolvedValueOnce({
      data: { content: [], totalElements: 0 }
    })
    const w = kur()
    await flushPromises()

    expect(w.find('.empty').text()).toBe('Onay bekleyen kayıt bulunmuyor')
  })

  it('Excel dışa aktarımını geçerli tür ve arama ile çağırır', async () => {
    const w = kur()
    await flushPromises()

    w.vm.sekmeSec(2)
    await flushPromises()
    w.vm.q = 'yakıt'
    await w.vm.excelIndir()

    expect(excelAPI.onaylar).toHaveBeenCalledWith({ tur: 'MASRAF', q: 'yakıt' })
  })

  it('mobil kartlarda aria-pressed ile seçim yapar', async () => {
    const w = kur()
    await flushPromises()

    const secimDugmeleri = w.findAll('.kart-sec')
    expect(secimDugmeleri.length).toBe(4)
    expect(secimDugmeleri[0].attributes('aria-pressed')).toBe('false')

    await secimDugmeleri[0].trigger('click')
    await flushPromises()

    expect(w.vm.secim).toHaveLength(1)
    expect(w.findAll('.kart-sec')[0].attributes('aria-pressed')).toBe('true')
  })

  it('klavye ile sekmeler arasında gezinir', async () => {
    const w = kur()
    await flushPromises()

    await w.findAll('.onay-sekme')[0].trigger('keydown', { key: 'ArrowRight' })
    await flushPromises()

    expect(w.vm.aktifSekme).toBe(1)
    expect(w.findAll('.onay-sekme')[1].attributes('aria-selected')).toBe('true')
    expect(onaylarAPI.bekleyen).toHaveBeenLastCalledWith(
      expect.objectContaining({ tur: 'IZIN' })
    )
  })

  it('Onay Ayarları bağlantısı ayarlar rotasına gider', async () => {
    const w = kur()
    await flushPromises()

    const ayarButonu = w.findAll('.btn').find((buton) => buton.text() === 'Onay Ayarları')
    expect(ayarButonu).toBeTruthy()
    await ayarButonu.trigger('click')

    expect(routerPush).toHaveBeenCalledWith('/onay-ayarlari')
  })
})
