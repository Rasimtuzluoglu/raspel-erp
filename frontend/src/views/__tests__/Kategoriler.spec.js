import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import i18n from '../../i18n.js'
import { kategoriAPI } from '../../api/index.js'
import { useAuthStore } from '../../stores/authStore.js'
import Kategoriler from '../Kategoriler.vue'

const AGAC = [
  {
    id: 1,
    kapsam: 'KASA_GELIR',
    ad: 'Satış',
    ustKategoriId: null,
    ustKategoriAd: null,
    renk: '#10b981',
    ikon: 'pi pi-shopping-cart',
    sira: 1,
    aktif: true,
    aciklama: null,
    altKategoriSayisi: 2,
    kullanimSayisi: 4,
    altKategoriler: [
      {
        id: 11,
        kapsam: 'KASA_GELIR',
        ad: 'Market',
        ustKategoriId: 1,
        ustKategoriAd: 'Satış',
        renk: '#3b82f6',
        ikon: 'pi pi-box',
        sira: 1,
        aktif: true,
        altKategoriSayisi: 0,
        kullanimSayisi: 2,
        altKategoriler: []
      },
      {
        id: 12,
        kapsam: 'KASA_GELIR',
        ad: 'Toptan',
        ustKategoriId: 1,
        ustKategoriAd: 'Satış',
        renk: '#f59e0b',
        ikon: 'pi pi-truck',
        sira: 2,
        aktif: true,
        altKategoriSayisi: 0,
        kullanimSayisi: 0,
        altKategoriler: []
      }
    ]
  },
  {
    id: 2,
    kapsam: 'KASA_GELIR',
    ad: 'Bağış',
    ustKategoriId: null,
    ustKategoriAd: null,
    renk: null,
    ikon: 'pi pi-gift',
    sira: 2,
    aktif: false,
    altKategoriSayisi: 0,
    kullanimSayisi: 0,
    altKategoriler: []
  },
  {
    id: 3,
    kapsam: 'KASA_GELIR',
    ad: 'Kira Geliri',
    ustKategoriId: null,
    ustKategoriAd: null,
    renk: '#6366f1',
    ikon: 'pi pi-home',
    sira: 3,
    aktif: true,
    altKategoriSayisi: 0,
    kullanimSayisi: 7,
    altKategoriler: []
  }
]

vi.mock('../../api/index.js', () => ({
  kategoriAPI: {
    getAll: vi.fn(),
    listele: vi.fn(),
    agac: vi.fn(),
    ozet: vi.fn(),
    secenekler: vi.fn(),
    getById: vi.fn(),
    kullanim: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    arsivle: vi.fn(),
    aktiflestir: vi.fn(),
    birlestir: vi.fn(),
    sistemKategorileri: vi.fn()
  }
}))

const Button = {
  props: ['label', 'icon', 'disabled'],
  emits: ['click'],
  template: '<button :disabled="disabled" @click="$emit(\'click\')"><slot />{{ label }}</button>'
}

const stubs = {
  PageHeader: {
    props: ['title', 'subtitle'],
    template: '<div class="ph"><h1>{{ title }}</h1><slot name="actions" /></div>'
  },
  KpiKart: {
    props: ['baslik', 'deger'],
    template: '<div class="kpi">{{ baslik }}:{{ deger }}</div>'
  },
  AppDataTable: {
    props: ['value'],
    template: '<div class="tbl"><div v-for="r in value" :key="r.id" class="tbl-row">{{ r.ad }}</div></div>'
  },
  ListeDurumu: {
    props: ['yukleniyor', 'hata', 'bos', 'bosMesaj', 'bosIpucu', 'eylemEtiketi'],
    emits: ['eylem'],
    template:
      '<div class="liste-durumu"><div v-if="yukleniyor" class="liste-durumu__iskelet" />' +
      '<div v-else-if="hata" class="liste-durumu__hata" />' +
      '<div v-else-if="bos" class="liste-durumu__bos">{{ bosMesaj }}<button class="bos-eylem" @click="$emit(\'eylem\')">{{ eylemEtiketi }}</button></div></div>'
  },
  KategoriFormDialog: {
    name: 'KategoriFormDialog',
    props: ['visible', 'kapsam', 'kategori', 'ustKategoriler', 'varsayilanUstKategoriId', 'kaydediliyor'],
    emits: ['update:visible', 'kaydet'],
    template: '<div v-if="visible" class="form-dlg" />'
  },
  KategoriKullanimDrawer: {
    name: 'KategoriKullanimDrawer',
    props: ['visible', 'kullanim', 'yukleniyor'],
    emits: ['update:visible'],
    template: '<div v-if="visible" class="kullanim-drawer">{{ kullanim && kullanim.ad }}</div>'
  },
  Column: true,
  Dialog: {
    props: ['visible'],
    emits: ['update:visible'],
    template: '<div v-if="visible" class="dlg"><slot /><slot name="footer" /></div>'
  },
  Button,
  InputText: {
    props: ['modelValue'],
    emits: ['update:modelValue', 'input'],
    template:
      '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value); $emit(\'input\', $event)" />'
  },
  Dropdown: {
    props: ['modelValue', 'options'],
    emits: ['update:modelValue'],
    template: '<select :value="modelValue" />'
  },
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' }
}

const kur = ({ admin = false, kapsam = null } = {}) => {
  const pinia = createPinia()
  setActivePinia(pinia)
  if (kapsam) localStorage.setItem('raspel_kategoriler_kapsam', kapsam)
  if (admin) {
    const auth = useAuthStore()
    auth.kullanici = { role: 'ADMIN' }
  }
  return mount(Kategoriler, {
    global: {
      plugins: [pinia, ToastService, ConfirmationService, i18n],
      stubs
    }
  })
}

const satirAdlari = (w) => w.findAll('.tbl-row').map((r) => r.text())

describe('Kategoriler.vue', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
    kategoriAPI.agac.mockResolvedValue({ data: AGAC })
    kategoriAPI.ozet.mockResolvedValue({ data: { toplam: 5, aktif: 4, pasif: 1, altKategori: 2 } })
    kategoriAPI.sistemKategorileri.mockResolvedValue({ data: [] })
    kategoriAPI.kullanim.mockResolvedValue({
      data: {
        kategoriId: 1,
        ad: 'Satış',
        toplam: 6,
        detaylar: [
          { modul: 'STOK', adet: 4 },
          { modul: 'KASA', adet: 2 }
        ]
      }
    })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('KPI ozetini ve agac tablosunu yukler', async () => {
    const w = kur()
    await flushPromises()

    expect(kategoriAPI.agac).toHaveBeenCalledTimes(1)
    expect(kategoriAPI.ozet).toHaveBeenCalledTimes(1)
    expect(w.text()).toContain('Toplam Kategori:5')
    expect(w.text()).toContain('Aktif Kategori:4')
    expect(w.text()).toContain('Pasif Kategori:1')
    expect(w.text()).toContain('Alt Kategori:2')

    const adlar = satirAdlari(w)
    expect(adlar).toContain('Satış')
    expect(adlar).toContain('Market')
    expect(adlar).toContain('Bağış')
  })

  it('varsayilan kapsam Kasa/Gelir olup agac isteginde kullanilir', async () => {
    kur()
    await flushPromises()

    expect(kategoriAPI.agac).toHaveBeenCalledWith(
      expect.objectContaining({ kapsam: 'KASA_GELIR' })
    )
    expect(kategoriAPI.ozet).toHaveBeenCalledWith({ kapsam: 'KASA_GELIR' })
  })

  it('kapsam sekmesi degisince hafizaya yazar ve yeni kapsami yukler', async () => {
    const w = kur()
    await flushPromises()

    await w.find('[data-kapsam="STOK"]').trigger('click')
    await flushPromises()

    expect(w.vm.kapsam).toBe('STOK')
    expect(localStorage.getItem('raspel_kategoriler_kapsam')).toBe('STOK')
    expect(kategoriAPI.agac).toHaveBeenLastCalledWith(
      expect.objectContaining({ kapsam: 'STOK' })
    )
    expect(kategoriAPI.ozet).toHaveBeenLastCalledWith({ kapsam: 'STOK' })
  })

  it('hafizadaki kapsam ile acilir ve Masraf sekmesinde sistem kategorilerini yukler', async () => {
    kategoriAPI.sistemKategorileri.mockResolvedValue({
      data: [{ kod: 'YAKIT', ad: 'Yakıt', kapsam: 'MASRAF' }]
    })
    const w = kur({ kapsam: 'MASRAF' })
    await flushPromises()

    expect(w.vm.kapsam).toBe('MASRAF')
    expect(kategoriAPI.agac).toHaveBeenCalledWith(
      expect.objectContaining({ kapsam: 'MASRAF' })
    )
    expect(kategoriAPI.sistemKategorileri).toHaveBeenCalledTimes(1)
    expect(w.text()).toContain('YAKIT')
    expect(w.text()).toContain('Sistem Kategorileri')
  })

  it('kapsam sekmeleri klavye ile gezilebilir (role=tablist)', async () => {
    const w = kur()
    await flushPromises()

    const tablist = w.find('[role="tablist"]')
    expect(tablist.exists()).toBe(true)
    const tab = w.find('[data-kapsam="KASA"]')
    expect(tab.attributes('role')).toBe('tab')
    expect(tab.attributes('aria-selected')).toBe('true')

    await tab.trigger('keydown', { key: 'ArrowRight' })
    await flushPromises()

    expect(w.vm.kapsam).toBe('STOK')
    expect(w.find('[data-kapsam="STOK"]').attributes('aria-selected')).toBe('true')
  })

  it('arama debounce ile API sorgusuna q ekler', async () => {
    const w = kur()
    await flushPromises()

    const oncekiCagri = kategoriAPI.agac.mock.calls.length
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })
    w.vm.arama = 'yemek'
    w.vm.aramaGirdi()
    await flushPromises()
    // Debounce suresi dolmadan istek gitmez.
    expect(kategoriAPI.agac.mock.calls.length).toBe(oncekiCagri)

    vi.advanceTimersByTime(300)
    vi.useRealTimers()
    await flushPromises()
    expect(kategoriAPI.agac).toHaveBeenLastCalledWith(
      expect.objectContaining({ q: 'yemek' })
    )
  })

  it('aktif/pasif filtresi API aktif parametresini ve aria-pressed durumunu yonetir', async () => {
    const w = kur()
    await flushPromises()

    const pasifCip = w.findAll('.durum-cip').find((b) => b.text().includes('Pasif'))
    await pasifCip.trigger('click')
    await flushPromises()

    expect(w.vm.durumFiltre).toBe('PASIF')
    expect(pasifCip.attributes('aria-pressed')).toBe('true')
    expect(kategoriAPI.agac).toHaveBeenLastCalledWith(
      expect.objectContaining({ aktif: false })
    )
  })

  it('agac alt kategorileri acilip kapanabilir', async () => {
    const w = kur()
    await flushPromises()

    expect(satirAdlari(w)).toContain('Market')
    w.vm.acKapa(1)
    await flushPromises()
    expect(satirAdlari(w)).not.toContain('Market')
    expect(satirAdlari(w)).toContain('Satış')

    w.vm.acKapa(1)
    await flushPromises()
    expect(satirAdlari(w)).toContain('Market')
  })

  it('ust kategoriler sayfalanir ve sayfa bilgisi gosterilir', async () => {
    const coklu = Array.from({ length: 26 }, (_, i) => ({
      id: 100 + i,
      kapsam: 'KASA_GELIR',
      ad: `Kategori ${String(i + 1).padStart(2, '0')}`,
      ustKategoriId: null,
      sira: i,
      aktif: true,
      altKategoriSayisi: 0,
      kullanimSayisi: 0,
      altKategoriler: []
    }))
    kategoriAPI.agac.mockResolvedValue({ data: coklu })
    const w = kur()
    await flushPromises()

    expect(w.vm.toplamSayfa).toBe(2)
    expect(satirAdlari(w)).toHaveLength(25)
    expect(satirAdlari(w)).not.toContain('Kategori 26')

    w.vm.sonrakiSayfa()
    await flushPromises()
    expect(satirAdlari(w)).toEqual(['Kategori 26'])
    expect(w.text()).toContain('Sayfa 2 / 2')
  })

  it('yeni kategori formunu acar; kaydet create cagirir ve listeyi tazeler', async () => {
    kategoriAPI.create.mockResolvedValue({ data: { id: 99 } })
    const w = kur()
    await flushPromises()

    const ekle = w.findAll('button').find((b) => b.text().includes('Yeni Kategori'))
    await ekle.trigger('click')
    expect(w.vm.formDialog).toBe(true)
    expect(w.vm.formKategori).toBeNull()

    const form = w.findComponent({ name: 'KategoriFormDialog' })
    const payload = {
      ad: 'Yeni',
      kapsam: 'KASA_GELIR',
      ustKategoriId: null,
      renk: '#10b981',
      ikon: 'pi pi-tag',
      sira: 1,
      aciklama: null,
      aktif: true
    }
    form.vm.$emit('kaydet', payload)
    await flushPromises()

    expect(kategoriAPI.create).toHaveBeenCalledWith(payload)
    expect(w.vm.formDialog).toBe(false)
    expect(kategoriAPI.agac).toHaveBeenCalledTimes(2)
  })

  it('duzenleme update cagirir; alt kategori ekleme ust kategoriyi on-secer', async () => {
    kategoriAPI.update.mockResolvedValue({ data: {} })
    const w = kur()
    await flushPromises()

    w.vm.duzenleAc(AGAC[0])
    expect(w.vm.formDialog).toBe(true)
    expect(w.vm.formKategori.id).toBe(1)

    w.vm.kaydet({ ad: 'Satış 2', kapsam: 'KASA_GELIR', ustKategoriId: null, sira: 1, aktif: true })
    await flushPromises()
    expect(kategoriAPI.update).toHaveBeenCalledWith(1, expect.objectContaining({ ad: 'Satış 2' }))

    w.vm.altEkle(AGAC[0])
    expect(w.vm.formKategori).toBeNull()
    expect(w.vm.formUstId).toBe(1)
  })

  it('arsivle/aktiflestir yalniz ADMIN icin calisir; digerlerinde aksiyon devre disidir', async () => {
    const w = kur()
    await flushPromises()

    await w.vm.arsivToggle(AGAC[0])
    expect(kategoriAPI.arsivle).not.toHaveBeenCalled()
    const pasifAksiyon = w.vm.satirAksiyonlari(AGAC[0]).find((a) => a.etiket === 'Arşivle')
    expect(pasifAksiyon.devreDisi).toBe(true)
    expect(pasifAksiyon.baslik).toContain('yönetici')

    const admin = kur({ admin: true })
    await flushPromises()
    kategoriAPI.arsivle.mockResolvedValue({ data: {} })
    kategoriAPI.aktiflestir.mockResolvedValue({ data: {} })

    await admin.vm.arsivToggle(AGAC[0])
    expect(kategoriAPI.arsivle).toHaveBeenCalledWith(1)

    await admin.vm.arsivToggle(AGAC[1])
    expect(kategoriAPI.aktiflestir).toHaveBeenCalledWith(2)
  })

  it('silme kullanim veya alt kategori varsa engellenir ve neden gosterilir', async () => {
    const w = kur({ admin: true })
    await flushPromises()

    const engel = w.vm.silEngel(AGAC[2])
    expect(engel).toContain('kullanılıyor')

    const altEngel = w.vm.silEngel({ ...AGAC[2], kullanimSayisi: 0, altKategoriSayisi: 2 })
    expect(altEngel).toContain('alt kategorisi')

    const temiz = w.vm.silEngel({ ...AGAC[2], kullanimSayisi: 0, altKategoriSayisi: 0 })
    expect(temiz).toBe('')

    const silAksiyon = w.vm.satirAksiyonlari(AGAC[2]).find((a) => a.etiket === 'Sil')
    expect(silAksiyon.devreDisi).toBe(true)
    expect(silAksiyon.baslik).toContain('kullanılıyor')
  })

  it('birlestirme hedefini ayni kapsamdan aktif kategorilerle sinirlar ve API cagirir', async () => {
    kategoriAPI.birlestir.mockResolvedValue({ data: {} })
    const w = kur({ admin: true })
    await flushPromises()

    w.vm.birlestirAc(AGAC[0])
    expect(w.vm.birlestirDialog).toBe(true)

    w.vm.birlestirHedefId = 3
    await w.vm.birlestirKaydet()
    await flushPromises()

    expect(kategoriAPI.birlestir).toHaveBeenCalledWith({ kaynakId: 1, hedefId: 3 })
    expect(w.vm.birlestirDialog).toBe(false)
  })

  it('kullanim drawer modul detaylarini yukler', async () => {
    const w = kur()
    await flushPromises()

    await w.vm.kullanimAc(AGAC[0])

    expect(kategoriAPI.kullanim).toHaveBeenCalledWith(1)
    expect(w.vm.kullanimDrawer).toBe(true)
    expect(w.vm.kullanimVeri.toplam).toBe(6)
    expect(w.vm.kullanimVeri.detaylar).toHaveLength(2)
    expect(w.vm.kullanimVeri.renk).toBe('#10b981')
    expect(w.find('.kullanim-drawer').text()).toContain('Satış')
  })

  it('liste bosken ortak bos durumu gosterir', async () => {
    kategoriAPI.agac.mockResolvedValue({ data: [] })
    const w = kur()
    await flushPromises()

    expect(w.find('.liste-durumu__bos').exists()).toBe(true)
    expect(w.text()).toContain('Henüz kategori bulunamadı')
    expect(w.find('.tbl').exists()).toBe(false)
  })

  it('erisilebilirlik: tablist, tab, aria-live ve sekme tabindex degerleri', async () => {
    const w = kur()
    await flushPromises()

    expect(w.findAll('[role="tablist"]').length).toBeGreaterThanOrEqual(1)
    expect(w.findAll('[role="tab"]').length).toBeGreaterThan(0)
    expect(w.find('[role="status"][aria-live="polite"]').exists()).toBe(true)
    expect(w.find('[data-kapsam="KASA"]').attributes('tabindex')).toBe('0')
    expect(w.find('[data-kapsam="STOK"]').attributes('tabindex')).toBe('-1')
  })
})
