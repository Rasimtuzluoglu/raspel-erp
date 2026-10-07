import { ref, computed, watch } from 'vue'
import { kasaAPI, bankaAPI, posAPI } from '../api/index.js'
import { unwrapList } from '../api/utils/unwrap.js'

/**
 * POS ODEME / INDIRIM — odeme yontemi, taksit, kasa/banka/POS secimleri,
 * indirim ve bunlardan turetilen tutarlar.
 *
 * NEDEN AYRI: Bu blok `HizliSatis.vue` icinde ~600 satirdi ve odeme
 * panelinin her alani icin ayri ref + yukleyici + turetilmis deger
 * dagilmis durumdaydi. Burada tek sorumluluk: "satis nasil tahsil edilecek?".
 *
 * `genelToplam` sepet toplamina baglidir; bu yuzden `toplam` DISARIDAN verilir
 * (`usePosSepet`). Boylece sepet ile odeme arasindaki tek bag bu olur.
 *
 * @typedef {object} PosOdemeBagimliliklari
 * @property {Function} t vue-i18n ceviri
 * @property {import('vue').Ref<number>} toplam Sepet ara toplami (usePosSepet)
 */

/** Kasa icin sik kullanilan banknot/kagit degerleri (para ustu hizli giris). */
export const HIZLI_NAKIT = [50, 100, 200, 500]

export function usePosOdeme({ t, toplam }) {
  // --- indirim ---
  const indirimTipi = ref('tutar')
  const indirimTipleri = ref([
    { label: '₺', value: 'tutar' },
    { label: '%', value: 'yuzde' }
  ])
  const indirimDegeri = ref(0)

  const indirimTutari = computed(() => {
    if (indirimDegeri.value <= 0) return 0
    if (indirimTipi.value === 'yuzde') {
      return toplam.value * (Math.min(indirimDegeri.value, 100) / 100)
    }
    return Math.min(indirimDegeri.value, toplam.value)
  })

  /** Indirim sonrasi tahsil edilecek toplam. */
  const genelToplam = computed(() => Math.max(0, toplam.value - indirimTutari.value))

  // --- odeme durumu ---
  const odemeDurumu = ref('tam')
  const odemeTipleri = computed(() => [
    { label: t('hizliSatis.odemeTipTam'), value: 'tam' },
    { label: t('hizliSatis.odemeTipYarim'), value: 'yarim' },
    { label: t('hizliSatis.odemeTipYok'), value: 'yok' }
  ])
  const odenenTutar = ref(0)

  // --- odeme yontemi ---
  const odemeYontemi = ref('NAKIT')
  const odemeYontemleri = computed(() => [
    { label: t('hizliSatis.nakit'), value: 'NAKIT', icon: 'pi pi-money-bill' },
    { label: t('hizliSatis.kart'), value: 'KART', icon: 'pi pi-credit-card' },
    { label: t('hizliSatis.havale'), value: 'HAVALE', icon: 'pi pi-send' },
    { label: t('hizliSatis.taksit'), value: 'TAKSIT', icon: 'pi pi-calendar' }
  ])

  // --- taksit ---
  const taksitKurum = ref('')
  const taksitTutar = ref(0)
  const taksitSayisi = ref(1)

  // --- kasa / banka / POS terminali ---
  const seciliKasa = ref(null)
  const kasalar = ref([])
  const seciliBanka = ref(null)
  const bankalar = ref([])
  const seciliPos = ref(null)
  const posTerminalleri = ref([])

  const seciliPosBilgi = computed(
    () => posTerminalleri.value.find((p) => p.id === seciliPos.value) || null
  )

  /** POS komisyonu: tutar * oran% (gun sonunda bankaya aktarilirken dusulur). */
  const hesaplananKomisyon = computed(() => {
    const oran = Number(seciliPosBilgi.value?.komisyonOrani || 0)
    if (!oran || !odenenTutar.value) return 0
    return Math.round(odenenTutar.value * oran) / 100
  })

  // --- para ustu ---
  const alinanNakit = ref(0)
  const paraUstu = computed(() => {
    if (odemeYontemi.value !== 'NAKIT' || odemeDurumu.value === 'yok') return 0
    return Math.max(0, (alinanNakit.value || 0) - (odenenTutar.value || 0))
  })

  // --- turetilen durumlar ---
  const kalanTutar = computed(() => Math.max(0, genelToplam.value - odenenTutar.value))

  const odemeDurumText = computed(() => {
    if (odemeDurumu.value === 'yok' || odenenTutar.value === 0) return t('hizliSatis.odemedi')
    if (odenenTutar.value >= genelToplam.value) return t('hizliSatis.tamamenOdendi')
    return t('hizliSatis.kismiOdedi')
  })

  const odemeDurumEnum = computed(() => {
    if (odemeDurumu.value === 'yok' || odenenTutar.value === 0) return 'ODENMEDI'
    if (odenenTutar.value >= genelToplam.value) return 'ODENDI'
    return 'KISMI_ODENDI'
  })

  const odemeDurumSeverity = computed(() => {
    if (odemeDurumu.value === 'yok' || odenenTutar.value === 0) return 'danger'
    if (odenenTutar.value >= genelToplam.value) return 'success'
    return 'warning'
  })

  // Odeme durumu degisince odenen tutar otomatik ayarlanir; genel toplam
  // degisince (urun eklendi/indirim) de ayni kural isler.
  watch(odemeDurumu, (v) => {
    if (v === 'tam') odenenTutar.value = genelToplam.value
    else if (v === 'yarim') odenenTutar.value = genelToplam.value / 2
    else odenenTutar.value = 0
  })

  watch(genelToplam, () => {
    if (odemeDurumu.value === 'tam') odenenTutar.value = genelToplam.value
    else if (odemeDurumu.value === 'yarim') odenenTutar.value = genelToplam.value / 2
  })

  // --- yukleyiciler ---

  /** Kasalari yukler; hicbiri secili degilse ilkini secer. */
  const kasalariYukle = async () => {
    try {
      const r = await kasaAPI.getAllKasalar()
      kasalar.value = unwrapList(r)
      if (kasalar.value.length > 0 && !seciliKasa.value) {
        seciliKasa.value = kasalar.value[0].id
      }
    } catch {
      kasalar.value = []
    }
  }

  const bankalariYukle = async () => {
    try {
      const r = await bankaAPI.getAll()
      bankalar.value = unwrapList(r)
    } catch {
      bankalar.value = []
    }
  }

  const poslariYukle = async () => {
    try {
      const r = await posAPI.aktif()
      posTerminalleri.value = Array.isArray(r.data) ? r.data : unwrapList(r)
    } catch {
      posTerminalleri.value = []
    }
  }

  /**
   * Satis sonrasi odeme alanlarini sifirlar (yeni satis icin).
   * `sepetiTemizle` (view) ve satis basarisi bu yolu kullanir.
   */
  const odemeSifirla = () => {
    indirimDegeri.value = 0
    odemeDurumu.value = 'tam'
    odenenTutar.value = 0
    alinanNakit.value = 0
    taksitKurum.value = ''
    taksitTutar.value = 0
    taksitSayisi.value = 1
    seciliPos.value = null
  }

  return {
    // indirim
    indirimTipi,
    indirimTipleri,
    indirimDegeri,
    indirimTutari,
    genelToplam,
    // odeme durumu
    odemeDurumu,
    odemeTipleri,
    odenenTutar,
    odemeYontemi,
    odemeYontemleri,
    // taksit
    taksitKurum,
    taksitTutar,
    taksitSayisi,
    // kasa / banka / pos
    seciliKasa,
    kasalar,
    seciliBanka,
    bankalar,
    seciliPos,
    posTerminalleri,
    seciliPosBilgi,
    hesaplananKomisyon,
    // nakit
    alinanNakit,
    paraUstu,
    hizliNakit: HIZLI_NAKIT,
    // turetilen
    kalanTutar,
    odemeDurumText,
    odemeDurumEnum,
    odemeDurumSeverity,
    // yukleyiciler / sifirlama
    kasalariYukle,
    bankalariYukle,
    poslariYukle,
    odemeSifirla
  }
}
