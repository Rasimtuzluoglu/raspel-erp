import { defineStore } from 'pinia'
import { ref } from 'vue'
import { dashboardAPI } from '../api/index.js'

/** Kokpit bölümlerinin küratörlü varsayılan sırası. */
export const KOKPIT_BOLUMLERI = ['nakit', 'alacakVerecek', 'karMarj', 'faturaSiparis']

/**
 * Finans kokpiti durumu.
 *
 * Tek `GET /api/dashboard/kokpit` yanıtını tutar; dönem değişiminde yeniden
 * istek atar. Tercih (bölüm görünürlüğü + sıra) mevcut
 * `/api/dashboard/tercihler` sözleşmesi üzerinden saklanır.
 */
export const useKokpitStore = defineStore('kokpit', () => {
  const kokpit = ref(null)
  const donem = ref('BU_AY')
  const paraBirimi = ref('TRY')
  const yukleniyor = ref(false)
  const hata = ref(null)
  const sonGuncelleme = ref(null)

  const tercih = ref(null)
  const tercihYukleniyor = ref(false)
  const tercihKaydediliyor = ref(false)
  const tercihHata = ref(null)
  /** Görünür bölümler, kullanıcının sırasıyla. */
  const gorunurBolumler = ref([...KOKPIT_BOLUMLERI])

  const hataMesaji = (err) => (err?.message ? String(err.message) : 'Bilinmeyen hata')

  // Son işlem akışı: `GET /api/dashboard/akis` (tek istek, limit ile).
  // Kokpit yanıtından bağımsızdır; hata olsa bile sayfanın geri kalanı çizilir.
  const akis = ref([])
  const akisYukleniyor = ref(false)
  const akisHata = ref(null)

  const akisYukle = async (limit = 20) => {
    const adet = Math.max(1, Math.min(Number(limit) || 20, 50))
    akisYukleniyor.value = true
    akisHata.value = null
    try {
      const response = await dashboardAPI.akis({ limit: adet })
      akis.value = Array.isArray(response.data) ? response.data.slice(0, adet) : []
      return akis.value
    } catch (err) {
      akisHata.value = hataMesaji(err)
      akis.value = []
      console.error('Islem akisi yuklenemedi:', err)
      return []
    } finally {
      akisYukleniyor.value = false
    }
  }

  // Bekleyen işler: `/api/dashboard/gorevler` (izin, masraf, satın alma,
  // sipariş, vadesi geçen, kritik stok, bekleyen teslimat).
  const gorevler = ref([])
  const gorevlerYukleniyor = ref(false)
  const gorevlerHata = ref(null)

  const gorevlerYukle = async () => {
    gorevlerYukleniyor.value = true
    gorevlerHata.value = null
    try {
      const response = await dashboardAPI.gorevler()
      gorevler.value = Array.isArray(response.data) ? response.data : []
      return gorevler.value
    } catch (err) {
      gorevlerHata.value = hataMesaji(err)
      gorevler.value = []
      console.error('Bekleyen gorevler yuklenemedi:', err)
      return []
    } finally {
      gorevlerYukleniyor.value = false
    }
  }

  const gecerliBolumler = (liste) =>
    Array.isArray(liste) ? liste.filter((anahtar, index) =>
      KOKPIT_BOLUMLERI.includes(anahtar) && liste.indexOf(anahtar) === index) : []

  const getKokpit = async (kod = donem.value) => {
    yukleniyor.value = true
    hata.value = null
    try {
      const response = await dashboardAPI.kokpit({ donem: kod })
      kokpit.value = response.data || null
      sonGuncelleme.value = new Date()
      return kokpit.value
    } catch (err) {
      hata.value = hataMesaji(err)
      // Hata durumunda son başarılı veri korunur; ekran boşalmaz.
      console.error('Kokpit verileri yuklenemedi:', err)
      return null
    } finally {
      yukleniyor.value = false
    }
  }

  const donemDegistir = async (kod) => {
    donem.value = kod || 'BU_AY'
    return getKokpit(donem.value)
  }

  const paraBirimiDegistir = (kod) => {
    paraBirimi.value = kod || 'TRY'
  }

  /** Kayıtlı düzeni yükler; kayıt yoksa küratörlü varsayılana döner. */
  const tercihYukle = async () => {
    tercihYukleniyor.value = true
    tercihHata.value = null
    try {
      const response = await dashboardAPI.tercihGetir()
      tercih.value = response.data || null
      const duzen = tercih.value?.duzen
      const kayitli = gecerliBolumler(duzen?.bolumSirasi ?? duzen?.gorunurBolumler)
      gorunurBolumler.value = kayitli.length > 0 ? kayitli : [...KOKPIT_BOLUMLERI]
      return tercih.value
    } catch (err) {
      tercihHata.value = hataMesaji(err)
      console.error('Kokpit tercihi okunamadi:', err)
      return null
    } finally {
      tercihYukleniyor.value = false
    }
  }

  const tercihKaydet = async (duzen) => {
    const kayit = duzen || {
      bolumSirasi: [...gorunurBolumler.value],
      gorunurBolumler: [...gorunurBolumler.value]
    }
    tercihKaydediliyor.value = true
    tercihHata.value = null
    try {
      const response = await dashboardAPI.tercihKaydet({ duzen: kayit })
      tercih.value = response.data || { duzen: kayit }
      return tercih.value
    } catch (err) {
      tercihHata.value = hataMesaji(err)
      console.error('Kokpit tercihi kaydedilemedi:', err)
      throw err
    } finally {
      tercihKaydediliyor.value = false
    }
  }

  const bolumSirala = (sira) => {
    const gecerli = gecerliBolumler(sira)
    if (gecerli.length > 0) {
      gorunurBolumler.value = gecerli
    }
    return [...gorunurBolumler.value]
  }

  const bolumGorunurlukDegistir = (anahtar, gorunur) => {
    if (!KOKPIT_BOLUMLERI.includes(anahtar)) {
      return [...gorunurBolumler.value]
    }
    const mevcut = gorunurBolumler.value
    if (gorunur) {
      if (!mevcut.includes(anahtar)) {
        gorunurBolumler.value = [...mevcut, anahtar]
      }
    } else if (mevcut.includes(anahtar) && mevcut.length > 1) {
      // En az bir bölüm görünür kalmalı.
      gorunurBolumler.value = mevcut.filter((anahtarDegeri) => anahtarDegeri !== anahtar)
    }
    return [...gorunurBolumler.value]
  }

  const tercihiSifirla = async () => {
    gorunurBolumler.value = [...KOKPIT_BOLUMLERI]
    return tercihKaydet({
      bolumSirasi: [...KOKPIT_BOLUMLERI],
      gorunurBolumler: [...KOKPIT_BOLUMLERI]
    })
  }

  return {
    kokpit,
    donem,
    paraBirimi,
    yukleniyor,
    hata,
    sonGuncelleme,
    tercih,
    tercihYukleniyor,
    tercihKaydediliyor,
    tercihHata,
    gorunurBolumler,
    akis,
    akisYukleniyor,
    akisHata,
    akisYukle,
    gorevler,
    gorevlerYukleniyor,
    gorevlerHata,
    gorevlerYukle,
    getKokpit,
    donemDegistir,
    paraBirimiDegistir,
    tercihYukle,
    tercihKaydet,
    bolumSirala,
    bolumGorunurlukDegistir,
    tercihiSifirla
  }
})
