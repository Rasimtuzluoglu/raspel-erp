import { defineStore } from 'pinia'
import { ref } from 'vue'
import { dashboardAPI } from '../api/index.js'

/**
 * Anasayfa verisi.
 *
 * Iki sozlesme bir arada yasar:
 *  - Legacy: `getDashboardData()` tek `/dashboard` ucundan tum alanlari doldurur
 *    (eski tuketiciler ve geriye uyum icin korunur).
 *  - Parcali: `getOzet/getGorevler/getGrafikler/getAkis/getIcgoruler/getTercih`
 *    her bolumu bagimsiz yukler; her birinin kendi `yukleniyor` ve `hata`
 *    bayragi vardir. Bir bolumun hatasi digerlerini engellemez.
 */
export const useDashboardStore = defineStore('dashboard', () => {
  const toplamCariSayisi = ref(0)
  const toplamBakiye = ref(0)
  const sonHareketler = ref([])
  const aktifCalisan = ref(0)
  const bugunIzinli = ref(0)
  const buAyIseBaslayacak = ref(0)
  const bugunkuSiparis = ref(0)
  const bekleyenTeslimat = ref(0)
  const iadeOrani = ref(0)
  const stokDevirHizi = ref(0)
  const enCokSatanlar = ref([])
  const pozitifBakiye = ref(0)
  const negatifBakiye = ref(0)
  const bugunkuTahsilat = ref(0)
  const bugunkuOdeme = ref(0)
  const bekleyenIzinSayisi = ref(0)
  const aylikGelirGider = ref([])
  const gunlukNakitAkisi = ref([])
  const vadesiGecenFaturalar = ref([])
  const vadesiYaklasanFaturalar = ref([])
  const toplamStok = ref(0)
  const kritikStokSayisi = ref(0)
  const kritikStoklar = ref([])
  const toplamFatura = ref(0)
  const kesilenFatura = ref(0)
  const toplamBankaBakiye = ref(0)
  const toplamKasaBakiye = ref(0)
  const hedefCiro = ref(0)
  const gerceklesenCiro = ref(0)
  const ciroIlerlemeYuzdesi = ref(0)
  const hedefKar = ref(0)
  const gerceklesenKar = ref(0)
  const enCokBorcCariler = ref([])
  const enCokAlacakCariler = ref([])
  const kategoriSatislari = ref([])
  const alacakYaslandirma = ref([])
  const toplamStokDegeri = ref(0)
  // Legacy uctaki gunun ozeti metni. Yeni `ozet` (nesne) ile karismamasi icin
  // ayri adlandirilir.
  const gununOzetiMetni = ref('')
  const veriEksik = ref(false)
  const loading = ref(false)
  const error = ref(null)

  // --- Parcali (yeni) durumlar ---
  const ozet = ref(null)
  const gorevler = ref([])
  const grafikler = ref(null)
  const akis = ref([])
  const icgoruler = ref([])
  const tercih = ref(null)

  const ozetYukleniyor = ref(false)
  const gorevlerYukleniyor = ref(false)
  const grafiklerYukleniyor = ref(false)
  const akisYukleniyor = ref(false)
  const icgorulerYukleniyor = ref(false)
  const tercihYukleniyor = ref(false)
  const tercihKaydediliyor = ref(false)

  const ozetHata = ref(null)
  const gorevlerHata = ref(null)
  const grafiklerHata = ref(null)
  const akisHata = ref(null)
  const icgorulerHata = ref(null)
  const tercihHata = ref(null)

  const hataMesaji = (err) => (err?.message ? String(err.message) : 'Bilinmeyen hata')

  const getDashboardData = async () => {
    loading.value = true
    error.value = null
    try {
      const response = await dashboardAPI.getData()
      const d = response.data
      toplamCariSayisi.value = d.toplamCariSayisi || 0
      toplamBakiye.value = d.toplamBakiye || 0
      sonHareketler.value = d.sonHareketler || []
      aktifCalisan.value = d.aktifCalisan || 0
      bugunIzinli.value = d.bugunIzinli || 0
      buAyIseBaslayacak.value = d.buAyIseBaslayacak || 0
      bugunkuSiparis.value = d.bugunkuSiparis || 0
      bekleyenTeslimat.value = d.bekleyenTeslimat || 0
      iadeOrani.value = d.iadeOrani || 0
      stokDevirHizi.value = d.stokDevirHizi || 0
      enCokSatanlar.value = d.enCokSatanlar || []
      pozitifBakiye.value = d.pozitifBakiye || 0
      negatifBakiye.value = d.negatifBakiye || 0
      bugunkuTahsilat.value = d.bugunkuTahsilat || 0
      bugunkuOdeme.value = d.bugunkuOdeme || 0
      bekleyenIzinSayisi.value = d.bekleyenIzinSayisi || 0
      aylikGelirGider.value = d.aylikGelirGider || []
      gunlukNakitAkisi.value = d.gunlukNakitAkisi || []
      vadesiGecenFaturalar.value = d.vadesiGecenFaturalar || []
      vadesiYaklasanFaturalar.value = d.vadesiYaklasanFaturalar || []
      toplamStok.value = d.toplamStok || 0
      kritikStokSayisi.value = d.kritikStokSayisi || 0
      kritikStoklar.value = d.kritikStoklar || []
      toplamFatura.value = d.toplamFatura || 0
      kesilenFatura.value = d.kesilenFatura || 0
      toplamBankaBakiye.value = d.toplamBankaBakiye || 0
      toplamKasaBakiye.value = d.toplamKasaBakiye || 0
      hedefCiro.value = d.hedefCiro || 0
      gerceklesenCiro.value = d.gerceklesenCiro || 0
      ciroIlerlemeYuzdesi.value = d.ciroIlerlemeYuzdesi || 0
      hedefKar.value = d.hedefKar || 0
      gerceklesenKar.value = d.gerceklesenKar || 0
      enCokBorcCariler.value = d.enCokBorcCariler || []
      enCokAlacakCariler.value = d.enCokAlacakCariler || []
      kategoriSatislari.value = d.kategoriSatislari || []
      alacakYaslandirma.value = d.alacakYaslandirma || []
      toplamStokDegeri.value = d.toplamStokDegeri || 0
      gununOzetiMetni.value = d.ozet || ''
      veriEksik.value = d.veriEksik === true
      return d
    } catch (err) {
      error.value = hataMesaji(err)
      console.error('Dashboard verileri yuklenirken hata:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /** Hero + KPI + gorev sayaclari. Sayfanin kritik yolu budur. */
  const getOzet = async () => {
    ozetYukleniyor.value = true
    ozetHata.value = null
    try {
      const response = await dashboardAPI.ozet()
      ozet.value = response.data || null
      return ozet.value
    } catch (err) {
      ozetHata.value = hataMesaji(err)
      console.error('Dashboard ozeti yuklenemedi:', err)
      return null
    } finally {
      ozetYukleniyor.value = false
    }
  }

  /** Oncelik sirali gorev merkezi (onay/vade/kritik stok/teslimat). */
  const getGorevler = async () => {
    gorevlerYukleniyor.value = true
    gorevlerHata.value = null
    try {
      const response = await dashboardAPI.gorevler()
      gorevler.value = Array.isArray(response.data) ? response.data : []
      return gorevler.value
    } catch (err) {
      gorevlerHata.value = hataMesaji(err)
      console.error('Dashboard gorevleri yuklenemedi:', err)
      return []
    } finally {
      gorevlerYukleniyor.value = false
    }
  }

  /** Kural tabanli icgoruler (en fazla 3). */
  const getIcgoruler = async () => {
    icgorulerYukleniyor.value = true
    icgorulerHata.value = null
    try {
      const response = await dashboardAPI.icgoruler()
      icgoruler.value = Array.isArray(response.data) ? response.data : []
      return icgoruler.value
    } catch (err) {
      icgorulerHata.value = hataMesaji(err)
      console.error('Dashboard icgoruleri yuklenemedi:', err)
      return []
    } finally {
      icgorulerYukleniyor.value = false
    }
  }

  /** Ana + genisletilmis grafik verileri. */
  const getGrafikler = async () => {
    grafiklerYukleniyor.value = true
    grafiklerHata.value = null
    try {
      const response = await dashboardAPI.grafikler()
      grafikler.value = response.data || null
      return grafikler.value
    } catch (err) {
      grafiklerHata.value = hataMesaji(err)
      console.error('Dashboard grafikleri yuklenemedi:', err)
      return null
    } finally {
      grafiklerYukleniyor.value = false
    }
  }

  /** Birlesik canli akis. */
  const getAkis = async (params = { limit: 20 }) => {
    akisYukleniyor.value = true
    akisHata.value = null
    try {
      const response = await dashboardAPI.akis(params)
      akis.value = Array.isArray(response.data) ? response.data : []
      return akis.value
    } catch (err) {
      akisHata.value = hataMesaji(err)
      console.error('Dashboard akisi yuklenemedi:', err)
      return []
    } finally {
      akisYukleniyor.value = false
    }
  }

  /** Kullanici duzeni (backend'de yoksa `duzen` null doner). */
  const getTercih = async () => {
    tercihYukleniyor.value = true
    tercihHata.value = null
    try {
      const response = await dashboardAPI.tercihGetir()
      tercih.value = response.data || null
      return tercih.value
    } catch (err) {
      tercihHata.value = hataMesaji(err)
      // Tercih okunamazsa rol varsayilanina dusulur; akisi bozmayiz.
      console.error('Dashboard tercihi okunamadi:', err)
      return null
    } finally {
      tercihYukleniyor.value = false
    }
  }

  /** Duzeni kaydeder. Hata firlatir; cagiran taraf localStorage'a dusurur. */
  const kaydetTercih = async (duzen) => {
    tercihKaydediliyor.value = true
    tercihHata.value = null
    try {
      const response = await dashboardAPI.tercihKaydet({ duzen })
      tercih.value = response.data || { duzen }
      return tercih.value
    } catch (err) {
      tercihHata.value = hataMesaji(err)
      console.error('Dashboard tercihi kaydedilemedi:', err)
      throw err
    } finally {
      tercihKaydediliyor.value = false
    }
  }

  return {
    // Legacy
    toplamCariSayisi,
    toplamBakiye,
    sonHareketler,
    aktifCalisan,
    bugunIzinli,
    buAyIseBaslayacak,
    bugunkuSiparis,
    bekleyenTeslimat,
    iadeOrani,
    stokDevirHizi,
    enCokSatanlar,
    pozitifBakiye,
    negatifBakiye,
    bugunkuTahsilat,
    bugunkuOdeme,
    bekleyenIzinSayisi,
    aylikGelirGider,
    gunlukNakitAkisi,
    vadesiGecenFaturalar,
    vadesiYaklasanFaturalar,
    toplamStok,
    kritikStokSayisi,
    kritikStoklar,
    toplamFatura,
    kesilenFatura,
    toplamBankaBakiye,
    toplamKasaBakiye,
    hedefCiro,
    gerceklesenCiro,
    ciroIlerlemeYuzdesi,
    hedefKar,
    gerceklesenKar,
    enCokBorcCariler,
    enCokAlacakCariler,
    kategoriSatislari,
    alacakYaslandirma,
    toplamStokDegeri,
    gununOzetiMetni,
    veriEksik,
    loading,
    error,
    getDashboardData,
    // Parcali
    ozet,
    gorevler,
    grafikler,
    akis,
    icgoruler,
    tercih,
    ozetYukleniyor,
    gorevlerYukleniyor,
    grafiklerYukleniyor,
    akisYukleniyor,
    icgorulerYukleniyor,
    tercihYukleniyor,
    tercihKaydediliyor,
    ozetHata,
    gorevlerHata,
    grafiklerHata,
    akisHata,
    icgorulerHata,
    tercihHata,
    getOzet,
    getGorevler,
    getGrafikler,
    getAkis,
    getIcgoruler,
    getTercih,
    kaydetTercih
  }
})
