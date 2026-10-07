import { ref, computed, watch } from 'vue'
import { stokAPI } from '../api/index.js'
import { unwrapList } from '../api/utils/unwrap.js'

/**
 * POS urun katalogu — sunucu tarafi arama, sayfalama ve filtreleme.
 *
 * SORUN (kullanicinin "stoktan malzeme ekleyip satis yaparken zorlaniyorum"
 * sikayetinin arka planindaki neden): Katalog POS'a ilk acilista TEK seferde
 * 200 urun ile cekiliyor, filtreleme ve arama BU 200 urun uzerinde YEREL
 * olarak yapiliyordu. Sonuc:
 *   - 200. urunden sonraki hicbir malzeme POS'ta bulunamiyordu; kasiyer
 *     urunun var oldugunu biliyor ama "stok yok" saniyordu,
 *   - kategori/marka/liste secenekleri yalnizca o 200 urunden turetiyordu,
 *     sayilar YANLIS ve gruplara ulasmak mumkun degildi,
 *   - yukleme sirasinda izgara bos gorunuyor ve "urun bulunamadi" yaziyordu
 *     (bos durum ile yukleniyor durumu karisiyordu).
 *
 * COZUM: Katalog artik sunucudan SAYFALI gelir (`GET /api/stoklar/filtreli`).
 * Arama metni, kategori, marka, stok grubu ve "sadece stokta" filtreleri
 * sunucuya gider; "daha fazla" sonraki sayfayi ister. Liste secenekleri
 * `GET /api/stoklar/gruplama-dagilimi` ile TUM katalogdan gelir ve gercek
 * urun sayilariyla etiketlenir.
 *
 * YUKLEME / BOS / HATA AYRIMI: `yukleniyor`, `bosMu` ve `hata` ayri
 * durumlardir. Once ucu de ayniydi: istek sirasinda izgara bos gorunuyor ve
 * "urun bulunamadi" yaziyordu; hata halinde de ayni mesaj cikiyordu.
 *
 * @typedef {object} PosKatalogSecenekleri
 * @property {string} [sirketId] Tenant (raporlama/etiketler icin)
 * @property {number} [sayfaBoyutu] Bir sayfadaki urun sayisi
 */

/** Varsayilan sayfa boyutu: bir ekrana sigacak kadar, dokunmatik kartlar icin. */
export const VARSAYILAN_SAYFA_BOYUTU = 60

export function usePosKatalog({ sayfaBoyutu = VARSAYILAN_SAYFA_BOYUTU } = {}) {
  /** Sayfalar biriktirilerek gosterilir ("daha fazla" butonu). */
  const urunler = ref([])
  const sayfa = ref(0)
  const toplam = ref(0)
  const sonSayfa = ref(false)

  /** Durumlar: ucu de AYRI tutulur. */
  const yukleniyor = ref(false)
  const hata = ref(null)
  const ilkYukleme = ref(true)

  // --- filtreler ---
  const aramaMetni = ref('')
  const kategori = ref(null)
  const marka = ref(null)
  const stokGrubu = ref(null)
  const sadeceStokta = ref(false)
  const siralama = ref('ad')

  // --- filtre secenekleri (tum katalogdan, gercek sayilarla) ---
  const kategoriDagilimi = ref([])
  const markaDagilimi = ref([])
  const stokGrubuDagilimi = ref([])

  /** Arama isteğini iptal etmek icin artan sayac (yaris yarisa yarisma). */
  let istekSeq = 0

  /** Etkin filtre parametreleri (sunucuya giden tek yer). */
  const istekParametreleri = computed(() => ({
    q: aramaMetni.value?.trim() || null,
    kategori: kategori.value || null,
    marka: marka.value || null,
    stokGrubu: stokGrubu.value || null,
    sadeceStokta: sadeceStokta.value ? true : null
  }))

  /**
   * Bir sayfayi sunucudan getirir.
   * @param {number} hedefSayfa 0 tabanli
   * @param {boolean} ekle true ise mevcut listenin USTUNE ekler
   * @returns {Promise<number|null}> Alinan sayfa no (hata halinde null)
   */
  const sayfaYukle = async (hedefSayfa, ekle = false) => {
    const benimSeq = ++istekSeq
    yukleniyor.value = true
    hata.value = null
    try {
      const r = await stokAPI.filtreli({
        ...istekParametreleri.value,
        page: hedefSayfa,
        size: sayfaBoyutu
      })
      // Gecikmeli eski istek yeni sonucu ezmesin.
      if (benimSeq !== istekSeq) return null

      const gelen = unwrapList(r)
      const toplamKayit = r?.data?.totalElements ?? gelen.length
      toplam.value = toplamKayit
      sonSayfa.value = hedefSayfa + 1 >= Math.max(1, Math.ceil(toplamKayit / sayfaBoyutu))
      urunler.value = ekle ? [...urunler.value, ...gelen] : gelen
      sayfa.value = hedefSayfa
      return hedefSayfa
    } catch (e) {
      if (benimSeq === istekSeq) {
        hata.value = e?.message || 'Katalog yüklenemedi'
        // Basarisiz ilk denemede listeyi BOSALTMA: kullanici elindeki urunleri
        // kaybetmemeli. Yeni bir filtreye gecildiginde liste degisecek zaten.
        if (!ekle && ilkYukleme.value) urunler.value = []
      }
      return null
    } finally {
      if (benimSeq === istekSeq) {
        yukleniyor.value = false
        ilkYukleme.value = false
      }
    }
  }

  /** Filtreleri sifirlar ve ilk sayfayi yeniden getirir. */
  const katalogYukle = () => {
    return sayfaYukle(0, false)
  }

  /** Sonraki sayfayi ekler ("daha fazla"). */
  const dahaFazlaYukle = async () => {
    if (yukleniyor.value || sonSayfa.value) return null
    return sayfaYukle(sayfa.value + 1, true)
  }

  /** Filtre degisimi: listeyi bastan al (sayfalama sifirlanir). */
  const filtreYenile = () => {
    sayfa.value = 0
    return sayfaYukle(0, false)
  }

  /**
   * Filtre seceneklerini tum katalogdan alir.
   *
   * Once bu listeler YUKLENEN SAYFADAN turetiliyordu; sayfali katalog
   * gecisiyle birlikte bu, kullanicinin "Gida" kategorisini gorup 3000
   * urunden sadece 40 tanesini gormesi gibi yaniltici sonuclar uretiyordu.
   */
  const dagilimYukle = async () => {
    try {
      const r = await stokAPI.gruplamaDagilimi()
      const d = r?.data || {}
      kategoriDagilimi.value = d.kategoriler || []
      markaDagilimi.value = d.markalar || []
      stokGrubuDagilimi.value = d.stokGruplari || []
    } catch {
      // Dagilim opsiyoneldir: gelmezse secenek listeleri bos kalir, katalog
      // calismaya devam eder. Kullaniciyi uyarmak icin ayrica seviyesinde
      // mesaj gostermek bu ekranda gurultu yaratir.
      kategoriDagilimi.value = []
      markaDagilimi.value = []
      stokGrubuDagilimi.value = []
    }
  }

  // --- turetilmis durumlar ---

  /** Kullanicinin yazdigi metin (arama kutusu + ızgara aramasi icin ortak). */
  const aramaVar = computed(() => !!aramaMetni.value?.trim())

  /** Etkin filtre sayisi (rozet basligi icin). */
  const aktifFiltreSayisi = computed(
    () => (kategori.value ? 1 : 0) + (marka.value ? 1 : 0) + (stokGrubu.value ? 1 : 0) + (sadeceStokta.value ? 1 : 0)
  )

  /** Bos mu? YUKLENIYOR veya HATALIYKEN "bos" sayilmaz. */
  const bosMu = computed(() => !yukleniyor.value && !hata.value && urunler.value.length === 0)

  /** "Daha fazla" butonu gorunecek mi? */
  const dahaFazlaVar = computed(() => !sonSayfa.value && !yukleniyor.value && urunler.value.length > 0)

  /** Arama sonrasi ozet: kac sonuc bulundu (ilk yuklemede gosterilir). */
  const sonucOzeti = computed(() => {
    if (yukleniyor.value) return null
    if (aramaVar.value || aktifFiltreSayisi.value > 0) return toplam.value
    return null
  })

  // Filtreler degisince listeyi bastan al. `siralama` bilerek DISARIDA:
  // sunucu tarafi siralama desteklemiyor ve her tusa bastiginda ag yeniden
  // cikmasina yol acar; siralama ayni yuklenen sayfa uzerinde uygulanir.
  watch([aramaMetni, kategori, marka, stokGrubu, sadeceStokta], () => {
    filtreYenile()
  })

  /** Etkin filtreleri temizler. */
  const filtreTemizle = () => {
    kategori.value = null
    marka.value = null
    stokGrubu.value = null
    sadeceStokta.value = false
  }

  return {
    // veri
    urunler,
    toplam,
    // durum
    yukleniyor,
    ilkYukleme,
    hata,
    bosMu,
    dahaFazlaVar,
    sonucOzeti,
    // filtreler
    aramaMetni,
    kategori,
    marka,
    stokGrubu,
    sadeceStokta,
    siralama,
    aramaVar,
    aktifFiltreSayisi,
    // secenekler
    kategoriDagilimi,
    markaDagilimi,
    stokGrubuDagilimi,
    // eylemler
    katalogYukle,
    dahaFazlaYukle,
    filtreYenile,
    filtreTemizle,
    dagilimYukle,
    sayfaYukle
  }
}
