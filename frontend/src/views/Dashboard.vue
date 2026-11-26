<template>
  <div
    class="kokpit-sayfa"
    :class="`kokpit-sayfa--${yogunluk}`"
    data-test="dashboard-kokpit"
  >
    <KokpitGlobalFiltre
      :donem="kokpitStore.donem"
      :para-birimi="kokpitStore.paraBirimi"
      :canli="canliGosterge"
      :yogunluk="yogunluk"
      :son-guncelleme="kokpitStore.sonGuncelleme"
      :yukleniyor="kokpitStore.yukleniyor"
      @update:donem="donemDegistir"
      @update:para-birimi="paraBirimiDegistir"
      @update:yogunluk="yogunlukDegistir"
      @canli-degistir="canliDegistir"
      @yenile="yenile"
      @excel="excelIndir"
      @pdf="pdfIndir"
      @ozellestir="ozellestirmeAcik = true"
    />

    <div class="kokpit-hero">
      <div class="kokpit-hero-sol">
        <p
          class="sr-only"
          role="status"
          aria-live="polite"
        >
          {{ duyuru }}
        </p>

        <div
          v-if="kokpitStore.hata && !kokpit"
          class="kokpit-hata"
          role="alert"
          data-test="kokpit-hata"
        >
          <i
            class="pi pi-exclamation-triangle"
            aria-hidden="true"
          />
          <span>{{ kokpitStore.hata }}</span>
          <button
            type="button"
            class="kokpit-hata-buton"
            @click="yenile"
          >
            {{ t('dashboard.yenidenDene') }}
          </button>
        </div>

        <FinansOzetBandi
          :kpi="kpi"
          :yukleniyor="ilkYukleme"
          :para-birimi="kokpitStore.paraBirimi"
        />

        <KritikDurumSeridi
          :ogeler="kritikDurum"
          :yukleniyor="ilkYukleme"
          :para-birimi="kokpitStore.paraBirimi"
          @oge-sec="kritikOgeSec"
        />
      </div>

      <KokpitHizliIslemler
        class="kokpit-hero-sag"
        :bekleyen-onay="bekleyenOnaySayisi"
        :onay-gorebilir="onayGorebilir"
        @tahsilat="hizliTahsilatAc('TAHSILAT')"
        @odeme="hizliTahsilatAc('ODEME')"
        @onaylar="onayDialogAcik = true"
      />
    </div>

    <div class="kokpit-orta">
      <KokpitAkis
        class="kokpit-orta-akis"
        :olaylar="gosterilenAkis"
        :yukleniyor="kokpitStore.akisYukleniyor"
        :hata="kokpitStore.akisHata"
        :para-birimi="kokpitStore.paraBirimi"
        :canli="canliGosterge"
        @sec="akisSec"
        @yenile="akisYenile"
      />

      <KokpitBekleyenIsler
        class="kokpit-orta-bekleyen"
        :ogeler="bekleyenIsler"
        :yukleniyor="ilkYukleme"
        :para-birimi="kokpitStore.paraBirimi"
        :onay-gorebilir="onayGorebilir"
        @oge-sec="gorevSec"
        @onaylar="onayDialogAcik = true"
      />
    </div>

    <h2 class="kokpit-bolum-baslik">
      {{ t('dashboard.detayliAnaliz') }}
    </h2>

    <div
      class="kokpit-bolumler"
      data-test="kokpit-bolumler"
    >
      <div
        v-for="anahtar in gorunurBolumler"
        :key="anahtar"
        class="kokpit-bolum"
        :data-test="`kokpit-bolum-${anahtar}`"
      >
        <component
          :is="bolumBileseni(anahtar)"
          :veri="bolumVerisi(anahtar)"
          :yukleniyor="ilkYukleme"
          :para-birimi="kokpitStore.paraBirimi"
          @detay="detayaGit"
          @hizli-islem="hizliIslem"
        />
      </div>
    </div>

    <KokpitOzellestirme
      v-if="ozellestirmeAcik"
      :bolumler="ozellestirmeBolumleri"
      :gorunur-bolumler="gorunurBolumler"
      :kaydediliyor="kokpitStore.tercihKaydediliyor"
      @kapat="ozellestirmeAcik = false"
      @gorunurluk-degistir="bolumGorunurlukDegistir"
      @sira-degistir="bolumSirala"
      @sifirla="bolumleriSifirla"
      @kaydet="tercihiKaydet"
    />

    <div
      ref="altBolge"
      class="kokpit-alt"
    >
      <template v-if="altGorunur">
        <KokpitIcgoruler
          :icgoruler="icgoruler"
          :yukleniyor="ilkYukleme"
        />

        <section class="kokpit-takvim">
          <button
            type="button"
            class="kokpit-takvim-baslik"
            :aria-expanded="nakitTakvimiAcik"
            aria-controls="kokpit-nakit-takvimi"
            data-test="kokpit-takvim-ac"
            @click="nakitTakvimiAcik = !nakitTakvimiAcik"
          >
            <i
              class="pi pi-calendar"
              aria-hidden="true"
            />
            <span>{{ t('dashboard.kokpitNakitTakvimBaslik') }}</span>
            <i
              class="pi pi-angle-down kokpit-takvim-ok"
              :class="{ 'kokpit-takvim-ok--acik': nakitTakvimiAcik }"
              aria-hidden="true"
            />
          </button>

          <KokpitNakitTakvimi
            v-if="nakitTakvimiAcik"
            id="kokpit-nakit-takvimi"
            :projeksiyon="nakitProjeksiyon"
            :takvim="nakitTakvim"
            :yukleniyor="ilkYukleme"
            :para-birimi="kokpitStore.paraBirimi"
            baslik-gizle
          />
        </section>
      </template>
    </div>

    <KokpitTahsilatDialog
      v-model:visible="hizliTahsilatAcik"
      :tur="hizliTahsilatTur"
      :cari-id="hizliTahsilatCariId"
      @kaydedildi="hizliIslemKaydedildi"
    />

    <KokpitOnayDialog
      v-if="onayGorebilir"
      v-model:visible="onayDialogAcik"
      @guncellendi="hizliIslemKaydedildi"
    />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore.js'
import { useKokpitStore } from '../stores/kokpitStore.js'
import { KOKPIT_BOLUMLERI } from '../stores/kokpitStore.js'
import { useDovizStore } from '../stores/dovizStore.js'
import { CANLI_YENILEME_GECIKME_MS, useDashboardCanli } from '../composables/useDashboardCanli.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { excelAPI } from '../api/index.js'
import { formatDateTime } from '../utils/format.js'
import KokpitGlobalFiltre from '../components/dashboard/kokpit/KokpitGlobalFiltre.vue'
import KokpitHizliIslemler from '../components/dashboard/kokpit/KokpitHizliIslemler.vue'
import KokpitTahsilatDialog from '../components/dashboard/kokpit/KokpitTahsilatDialog.vue'
import KokpitOnayDialog from '../components/dashboard/kokpit/KokpitOnayDialog.vue'
import FinansOzetBandi from '../components/dashboard/kokpit/FinansOzetBandi.vue'
import KritikDurumSeridi from '../components/dashboard/kokpit/KritikDurumSeridi.vue'
import NakitBolumu from '../components/dashboard/kokpit/NakitBolumu.vue'
import AlacakVerecekBolumu from '../components/dashboard/kokpit/AlacakVerecekBolumu.vue'
import KarMarjBolumu from '../components/dashboard/kokpit/KarMarjBolumu.vue'
import FaturaSiparisBolumu from '../components/dashboard/kokpit/FaturaSiparisBolumu.vue'
import KokpitOzellestirme from '../components/dashboard/kokpit/KokpitOzellestirme.vue'
import KokpitIcgoruler from '../components/dashboard/kokpit/KokpitIcgoruler.vue'
import KokpitNakitTakvimi from '../components/dashboard/kokpit/KokpitNakitTakvimi.vue'
import KokpitAkis from '../components/dashboard/kokpit/KokpitAkis.vue'
import KokpitBekleyenIsler from '../components/dashboard/kokpit/KokpitBekleyenIsler.vue'

const { t } = useI18n()
const router = useRouter()
const authStore = useAuthStore()
const kokpitStore = useKokpitStore()
const dovizStore = useDovizStore()
const toastBildirim = useToastBildirim()

const ozellestirmeAcik = ref(false)
const duyuru = ref('')
const hizliTahsilatAcik = ref(false)
const hizliTahsilatTur = ref('TAHSILAT')
const hizliTahsilatCariId = ref(null)
const onayDialogAcik = ref(false)

// Görünüm yoğunluğu: kompakt (varsayılan) / ferah; tercih kalıcı.
const YOGUNLUK_ANAHTAR = 'raspel_dashboard_yogunluk'
const YOGUNLUKLAR = ['kompakt', 'ferah']
const yogunluk = ref('kompakt')

const yogunlukDegistir = (deger) => {
  if (!YOGUNLUKLAR.includes(deger)) return
  yogunluk.value = deger
  try {
    localStorage.setItem(YOGUNLUK_ANAHTAR, deger)
  } catch {
    /* depolama kapalıysa oturum boyunca bellekte kalır */
  }
}

// Alt bölgeler (içgörüler + nakit takvimi) görünür olunca mount edilir:
// ilk boyamada DOM ve grafik maliyeti oluşmaz.
const altBolge = ref(null)
const altGorunur = ref(false)
const nakitTakvimiAcik = ref(false)
let altGozlemci = null

const kokpit = computed(() => kokpitStore.kokpit)
const gosterilenKokpit = computed(() =>
  paraBirimineCevir(kokpit.value, kokpitStore.paraBirimi, dovizStore)
)
const kpi = computed(() => (Array.isArray(gosterilenKokpit.value?.ozet) ? gosterilenKokpit.value.ozet : []))
const kritikDurum = computed(() =>
  Array.isArray(gosterilenKokpit.value?.kritikDurum) ? gosterilenKokpit.value.kritikDurum : []
)
const icgoruler = computed(() =>
  Array.isArray(gosterilenKokpit.value?.icgoruler) ? gosterilenKokpit.value.icgoruler : []
)
const nakitProjeksiyon = computed(() => gosterilenKokpit.value?.nakit?.projeksiyon || null)
const nakitTakvim = computed(() => gosterilenKokpit.value?.takvim || null)
const ilkYukleme = computed(() => kokpitStore.yukleniyor && !kokpit.value)
const onayGorebilir = computed(() => {
  const rol = authStore.kullanici?.role
  return rol === 'ADMIN' || rol === 'MUHASEBE'
})
const bekleyenOnaySayisi = computed(() => {
  const oge = (gosterilenKokpit.value?.kritikDurum || []).find(
    (kritik) => kritik.anahtar === 'bekleyenOnay'
  )
  return Number(oge?.adet) || 0
})

// Bekleyen işler kartı: `/api/dashboard/gorevler` (izin, masraf, satın alma,
// sipariş, vadesi geçen, kritik stok, bekleyen teslimat). Uç alınamazsa kokpitteki
// kritik durum kalemlerine düşer; sayfa boş kalmaz.
const gorevSeviyesi = (oncelik) => {
  const sayi = Number(oncelik) || 0
  if (sayi >= 90) return 'KRITIK'
  if (sayi >= 82) return 'WARN'
  return 'BILGI'
}

const bekleyenIsler = computed(() => {
  const birim = kokpitStore.paraBirimi
  const gorevler = Array.isArray(kokpitStore.gorevler) ? kokpitStore.gorevler : []
  if (gorevler.length > 0) {
    return gorevler
      .filter((gorev) => Number(gorev.adet) > 0 || Number(gorev.tutar) > 0)
      .slice(0, 6)
      .map((gorev) => ({
        anahtar: gorev.tur,
        etiket: gorev.baslik,
        adet: gorev.adet,
        tutar: gorev.tutar !== null && gorev.tutar !== undefined && birim !== 'TRY'
          ? dovizStore.convert(Number(gorev.tutar), 'TRY', birim)
          : gorev.tutar,
        seviye: gorevSeviyesi(gorev.oncelik),
        route: gorev.route
      }))
  }
  return (gosterilenKokpit.value?.kritikDurum || [])
    .filter((oge) => ['bekleyenOnay', 'vadesiGecen', 'kritikStok'].includes(oge.anahtar))
    .map((oge) => ({ ...oge, route: oge.hedef?.route || null }))
})

// Akış tutarları seçili para birimine çevrilir (kokpitteki dönüşümle aynı kur).
const gosterilenAkis = computed(() => {
  const birim = kokpitStore.paraBirimi
  const liste = Array.isArray(kokpitStore.akis) ? kokpitStore.akis : []
  if (birim === 'TRY') return liste
  return liste.map((olay) => {
    if (olay?.tutar === null || olay?.tutar === undefined) return olay
    return { ...olay, tutar: dovizStore.convert(Number(olay.tutar), 'TRY', birim) }
  })
})

const BILESENLER = {
  nakit: NakitBolumu,
  alacakVerecek: AlacakVerecekBolumu,
  karMarj: KarMarjBolumu,
  faturaSiparis: FaturaSiparisBolumu
}

const gorunurBolumler = computed(() => {
  const liste = Array.isArray(kokpitStore.gorunurBolumler) ? kokpitStore.gorunurBolumler : []
  // Yetkisiz rollerde kâr/marj bölümü hiç gösterilmez.
  if (kokpit.value && kokpit.value.karMarj === null) {
    const gorunen = liste.filter((anahtar) => anahtar !== 'karMarj')
    return gorunen.length > 0 ? gorunen : ['nakit']
  }
  return liste
})

const ozellestirmeBolumleri = computed(() => {
  if (kokpit.value && kokpit.value.karMarj === null) {
    return KOKPIT_BOLUMLERI.filter((anahtar) => anahtar !== 'karMarj')
  }
  return KOKPIT_BOLUMLERI
})

const bolumBileseni = (anahtar) => BILESENLER[anahtar] || null
const bolumVerisi = (anahtar) => gosterilenKokpit.value?.[anahtar] || null

/**
 * Kokpit tutarlarını TRY'den seçilen para birimine çevirir.
 *
 * Yüzdeler (marj, tahsilat oranı, trend) ve adetler dönüştürülmez; yalnızca
 * parasal alanlar çevrilir. Oranlar için kaynak veri TRY kabul edilir.
 */
const paraBirimineCevir = (kaynak, birim, doviz) => {
  if (!kaynak || birim === 'TRY') return kaynak
  // Sığ kopya yeterli: aşağıda dönüştürülen her alan yeni nesnelerle değiştirilir.
  // (Eski `JSON.parse(JSON.stringify(...))` tüm kokpiti kopyalıyordu.)
  const veri = { ...kaynak }
  const para = (deger) => {
    if (deger === null || deger === undefined) return deger
    const sayi = Number(deger)
    return Number.isFinite(sayi) ? doviz.convert(sayi, 'TRY', birim) : deger
  }

  veri.ozet = (veri.ozet || []).map((oge) => {
    if (oge.anahtar === 'marj') return oge
    return {
      ...oge,
      deger: para(oge.deger),
      onceki: para(oge.onceki),
      sparkline: Array.isArray(oge.sparkline) ? oge.sparkline.map(para) : oge.sparkline
    }
  })

  veri.kritikDurum = (veri.kritikDurum || []).map((oge) => (
    oge.anahtar === 'likiditeTrendi' ? oge : { ...oge, tutar: para(oge.tutar) }
  ))

  if (veri.nakit) {
    veri.nakit = {
      ...veri.nakit,
      kasa: para(veri.nakit.kasa),
      banka: para(veri.nakit.banka),
      toplam: para(veri.nakit.toplam),
      sparkline: Array.isArray(veri.nakit.sparkline) ? veri.nakit.sparkline.map(para) : veri.nakit.sparkline,
      gunlukAkis: (veri.nakit.gunlukAkis || []).map((gun) => ({
        ...gun,
        giris: para(gun.giris),
        cikis: para(gun.cikis),
        net: para(gun.net)
      })),
      projeksiyon: veri.nakit.projeksiyon
        ? {
            ...veri.nakit.projeksiyon,
            baslangicBakiyesi: para(veri.nakit.projeksiyon.baslangicBakiyesi),
            toplamBeklenenGiris: para(veri.nakit.projeksiyon.toplamBeklenenGiris),
            toplamBeklenenCikis: para(veri.nakit.projeksiyon.toplamBeklenenCikis),
            tahminiBitisBakiyesi: para(veri.nakit.projeksiyon.tahminiBitisBakiyesi),
            gunluk: (veri.nakit.projeksiyon.gunluk || []).map((gun) => ({
              ...gun,
              giris: para(gun.giris),
              cikis: para(gun.cikis),
              net: para(gun.net),
              kumulatif: para(gun.kumulatif)
            }))
          }
        : veri.nakit.projeksiyon
    }
  }

  if (veri.alacakVerecek) {
    veri.alacakVerecek = {
      ...veri.alacakVerecek,
      alacakToplam: para(veri.alacakVerecek.alacakToplam),
      verecekToplam: para(veri.alacakVerecek.verecekToplam),
      net: para(veri.alacakVerecek.net),
      yaslanma: (veri.alacakVerecek.yaslanma || []).map((kova) => ({ ...kova, tutar: para(kova.tutar) })),
      borclular: (veri.alacakVerecek.borclular || []).map((cari) => ({ ...cari, tutar: para(cari.tutar) })),
      tedarikciler: (veri.alacakVerecek.tedarikciler || []).map((cari) => ({ ...cari, tutar: para(cari.tutar) }))
    }
  }

  if (veri.takvim) {
    veri.takvim = {
      ...veri.takvim,
      toplamGiris: para(veri.takvim.toplamGiris),
      toplamCikis: para(veri.takvim.toplamCikis),
      net: para(veri.takvim.net),
      gunler: (veri.takvim.gunler || []).map((gun) => ({
        ...gun,
        giris: para(gun.giris),
        cikis: para(gun.cikis),
        net: para(gun.net),
        kumulatif: para(gun.kumulatif)
      }))
    }
  }

  if (veri.karMarj) {
    veri.karMarj = {
      ...veri.karMarj,
      ciro: para(veri.karMarj.ciro),
      maliyet: para(veri.karMarj.maliyet),
      kar: para(veri.karMarj.kar),
      netKar: para(veri.karMarj.netKar),
      trend: (veri.karMarj.trend || []).map((ay) => ({
        ...ay,
        ciro: para(ay.ciro),
        maliyet: para(ay.maliyet),
        kar: para(ay.kar)
      })),
      urunler: (veri.karMarj.urunler || []).map((kalem) => ({
        ...kalem,
        ciro: para(kalem.ciro),
        maliyet: para(kalem.maliyet),
        kar: para(kalem.kar)
      })),
      cariler: (veri.karMarj.cariler || []).map((kalem) => ({
        ...kalem,
        ciro: para(kalem.ciro),
        maliyet: para(kalem.maliyet),
        kar: para(kalem.kar)
      }))
    }
  }

  if (veri.faturaSiparis) {
    veri.faturaSiparis = {
      ...veri.faturaSiparis,
      vadesiGecenTutar: para(veri.faturaSiparis.vadesiGecenTutar),
      durumlar: (veri.faturaSiparis.durumlar || []).map((kova) => ({ ...kova, tutar: para(kova.tutar) })),
      siparisDurumlari: (veri.faturaSiparis.siparisDurumlari || []).map((kova) => ({
        ...kova,
        tutar: para(kova.tutar)
      })),
      vadesiGecenFaturalar: (veri.faturaSiparis.vadesiGecenFaturalar || []).map((fatura) => ({
        ...fatura,
        kalanTutar: para(fatura.kalanTutar)
      }))
    }
  }

  return veri
}

// --- Canlı mod ---------------------------------------------------------------
const canli = useDashboardCanli({
  sirketId: () => authStore.sirketId,
  yedekYenile: () => yenile()
})
const canliGosterge = computed(() => canli.aktif.value && canli.bagli.value)

// Canlı olaylarını 2 saniyelik tek pencereye indirger; art arda gelen
// STOMP mesajları tek kokpit isteği üretir.
let canliZamanlayici = null
const canliTazele = () => {
  if (!canli.aktif.value) return
  if (canliZamanlayici !== null) clearTimeout(canliZamanlayici)
  canliZamanlayici = setTimeout(() => {
    canliZamanlayici = null
    if (canli.aktif.value) {
      kokpitStore.getKokpit()
      kokpitStore.akisYukle(20)
      kokpitStore.gorevlerYukle()
    }
  }, CANLI_YENILEME_GECIKME_MS)
}

watch(
  () => canli.sonOlay.value,
  (olay) => {
    if (olay) canliTazele()
  }
)

onBeforeUnmount(() => {
  if (canliZamanlayici !== null) {
    clearTimeout(canliZamanlayici)
    canliZamanlayici = null
  }
})

const canliDegistir = () => {
  canli.ayarla(!canli.aktif.value)
}

// --- Dönem / para birimi -----------------------------------------------------
const DONEM_ANAHTAR = 'raspel_dashboard_donem'
const PARA_ANAHTAR = 'raspel_dashboard_para_birimi'
const DONEMLER = ['BU_AY', 'GECEN_AY', 'BU_CEYREK', 'BU_YIL']
const PARALAR = ['TRY', 'USD', 'EUR']

const kayitliDeger = (anahtar, gecerli, varsayilan) => {
  try {
    const kayit = localStorage.getItem(anahtar)
    return gecerli.includes(kayit) ? kayit : varsayilan
  } catch {
    return varsayilan
  }
}

const kaydet = (anahtar, deger) => {
  try {
    localStorage.setItem(anahtar, deger)
  } catch {
    /* depolama kapalıysa oturum boyunca bellekte kalır */
  }
}

const donemDegistir = async (kod) => {
  if (!DONEMLER.includes(kod)) return
  kaydet(DONEM_ANAHTAR, kod)
  await kokpitStore.donemDegistir(kod)
}

const paraBirimiDegistir = (kod) => {
  if (!PARALAR.includes(kod)) return
  kaydet(PARA_ANAHTAR, kod)
  kokpitStore.paraBirimiDegistir(kod)
  dovizStore.aktifParaBirimi = kod
}

const yenile = async () => {
  if (kokpitStore.yukleniyor) return
  if (canliZamanlayici !== null) {
    clearTimeout(canliZamanlayici)
    canliZamanlayici = null
  }
  await Promise.all([
    kokpitStore.getKokpit(),
    kokpitStore.akisYukle(20),
    kokpitStore.gorevlerYukle()
  ])
  if (!kokpitStore.hata) {
    duyuru.value = t('dashboard.canliGuncellendi')
  }
}

const akisYenile = () => {
  kokpitStore.akisYukle(20)
}

/** `route` stringini ({path, query}) biçimine çevirir. */
const rotaCoz = (rota) => {
  if (!rota) return null
  const [path, sorgu] = String(rota).split('?')
  const query = {}
  if (sorgu) {
    new URLSearchParams(sorgu).forEach((deger, anahtar) => {
      query[anahtar] = deger
    })
  }
  return { path: path || '/', query }
}

const gorevSec = (oge) => {
  const hedef = rotaCoz(oge?.route)
  if (hedef) {
    router.push(hedef)
    return
  }
  // Route yoksa eski davranış: tahsilat/onay diyaloğu veya türe göre ekran.
  kritikOgeSec(oge)
}

/** Akış satırı: backend'in verdiği hedefe git; yoksa türe göre makul ekran. */
const akisSec = (olay) => {
  if (olay?.route) {
    router.push({ path: olay.route, query: {} })
    return
  }
  const TUR_ROTA = {
    FATURA: '/faturalar',
    TAHSILAT: '/hareketler',
    ODEME: '/hareketler',
    SIPARIS: '/siparisler',
    STOK: '/stoklar',
    ONAY: '/onaylar'
  }
  const rota = TUR_ROTA[olay?.tur]
  if (rota) router.push({ path: rota })
}

const detayaGit = (hedef) => {
  if (!hedef?.route) return
  router.push({ path: hedef.route, query: hedef.query || {} })
}

// --- Hızlı işlemler ----------------------------------------------------------
const hizliTahsilatAc = (tur = 'TAHSILAT', cariId = null) => {
  hizliTahsilatTur.value = tur === 'ODEME' ? 'ODEME' : 'TAHSILAT'
  const sayi = Number(cariId)
  hizliTahsilatCariId.value = cariId != null && Number.isFinite(sayi) ? sayi : null
  hizliTahsilatAcik.value = true
}

const hizliIslem = (tip, cariId = null) => {
  if (tip === 'tahsilat') {
    hizliTahsilatAc('TAHSILAT', cariId)
  } else if (tip === 'odeme') {
    hizliTahsilatAc('ODEME', cariId)
  }
}

const kritikOgeSec = (oge) => {
  switch (oge?.anahtar) {
    case 'vadesiGecen':
      hizliTahsilatAc('TAHSILAT')
      break
    case 'bekleyenOnay':
      if (onayGorebilir.value) onayDialogAcik.value = true
      break
    case 'kritikStok':
      router.push({ path: '/satinalma', query: { yeni: '1' } })
      break
    case 'likiditeTrendi':
      router.push({ path: '/kasa' })
      break
    default:
      if (oge?.hedef?.route) detayaGit(oge.hedef)
  }
}

const hizliIslemKaydedildi = () => {
  yenile()
}

// --- Özelleştirme ------------------------------------------------------------
const bolumGorunurlukDegistir = (anahtar, gorunur) => {
  kokpitStore.bolumGorunurlukDegistir(anahtar, gorunur)
}

const bolumSirala = (sira) => {
  kokpitStore.bolumSirala(sira)
}

const bolumleriSifirla = async () => {
  try {
    await kokpitStore.tercihiSifirla()
  } catch {
    toastBildirim.hata(t('dashboard.kokpitOzellestirmeKaydedilemedi'))
  }
}

const tercihiKaydet = async () => {
  try {
    await kokpitStore.tercihKaydet()
    ozellestirmeAcik.value = false
  } catch {
    toastBildirim.hata(t('dashboard.kokpitOzellestirmeKaydedilemedi'))
  }
}

// --- Excel / PDF -------------------------------------------------------------
const excelIndiriliyor = ref(false)

const excelDosyaAdi = () => {
  const tarih = new Date()
  const yil = tarih.getFullYear()
  const ay = String(tarih.getMonth() + 1).padStart(2, '0')
  const gun = String(tarih.getDate()).padStart(2, '0')
  return `dashboard-${yil}${ay}${gun}.xlsx`
}

const blobIndir = (blob, dosyaAdi) => {
  if (typeof document === 'undefined' || typeof URL === 'undefined') return
  if (typeof URL.createObjectURL !== 'function') return
  const url = URL.createObjectURL(blob)
  try {
    const baglanti = document.createElement('a')
    baglanti.href = url
    baglanti.download = dosyaAdi
    baglanti.rel = 'noopener'
    document.body.appendChild(baglanti)
    baglanti.click()
    baglanti.remove()
  } finally {
    if (typeof URL.revokeObjectURL === 'function') URL.revokeObjectURL(url)
  }
}

const excelIndir = async () => {
  if (excelIndiriliyor.value) return
  excelIndiriliyor.value = true
  try {
    const cevap = await excelAPI.dashboard({ donem: kokpitStore.donem })
    const veri = cevap?.data
    const blob = typeof Blob !== 'undefined' && veri instanceof Blob ? veri : new Blob([veri ?? ''])
    blobIndir(blob, excelDosyaAdi())
    toastBildirim.basarili(t('dashboard.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('dashboard.excelHata'))
  } finally {
    excelIndiriliyor.value = false
  }
}

const pdfIndir = () => {
  if (typeof window !== 'undefined' && typeof window.print === 'function') window.print()
}

watch(
  () => kokpitStore.sonGuncelleme,
  (deger) => {
    if (deger) duyuru.value = t('dashboard.sonGuncelleme', { zaman: formatDateTime(deger) })
  }
)

onMounted(async () => {
  kokpitStore.donem = kayitliDeger(DONEM_ANAHTAR, DONEMLER, 'BU_AY')
  yogunluk.value = kayitliDeger(YOGUNLUK_ANAHTAR, YOGUNLUKLAR, 'kompakt')
  const baslangicPara = kayitliDeger(PARA_ANAHTAR, PARALAR, 'TRY')
  kokpitStore.paraBirimiDegistir(baslangicPara)
  dovizStore.aktifParaBirimi = baslangicPara
  const kurlariYukle = Array.isArray(dovizStore.kurlar) && dovizStore.kurlar.length > 0
    ? Promise.resolve()
    : dovizStore.kurlariYukle()
  await Promise.all([
    kokpitStore.getKokpit(),
    kokpitStore.akisYukle(20),
    kokpitStore.gorevlerYukle(),
    kokpitStore.tercihYukle(),
    kurlariYukle
  ])

  // Alt bölgeleri görünür olunca (300px önce) mount et. Eleman zaten
  // görünürdeyse (kısa sayfa / test ortamı) beklemeden aç.
  if (typeof IntersectionObserver !== 'function' || !altBolge.value) {
    altGorunur.value = true
    return
  }
  const konum = altBolge.value.getBoundingClientRect()
  if (konum.height === 0 || konum.top < window.innerHeight) {
    altGorunur.value = true
    return
  }
  altGozlemci = new IntersectionObserver((girdiler) => {
    if (girdiler.some((girdi) => girdi.isIntersecting)) {
      altGorunur.value = true
      altGozlemci?.disconnect()
      altGozlemci = null
    }
  }, { rootMargin: '300px 0px' })
  altGozlemci.observe(altBolge.value)
})

onBeforeUnmount(() => {
  altGozlemci?.disconnect()
  altGozlemci = null
})
</script>

<style scoped>
/* Yapışkan üst bar: atalardaki `overflow-x: hidden` sticky için scroll
   konteyneri üretiyordu. `clip` aynı yatay kırpmayı korur, scroll konteyneri
   üretmez; kural yalnız kokpit sayfası varken uygulanır. */
:global(.main-content:has(.kokpit-sayfa)),
:global(.app-container:has(.kokpit-sayfa)),
:global(#app:has(.kokpit-sayfa)) {
  overflow-x: clip;
}
.kokpit-sayfa {
  --kokpit-bosluk: 14px;
  /* İçerik tavanı. 2200px'te 2 sütunlu detay kartları ~1075 CSS px kalır;
     daha geniş ekranlarda tavan kademeli büyür (kart üst sınırı ~1275 CSS px).
     Değerler CSS px; `html { zoom: .9 }` olduğu için ekranda 0.9 ile ölçeklenir.
     Kırılımlar `@media` olduğundan FİZİKSEL viewport ile değerlendirilir
     (zoom media sorgusunu etkilemez). */
  --kokpit-icerik-max: 2200px;
  display: flex;
  flex-direction: column;
  gap: var(--kokpit-bosluk);
  min-width: 0;
  max-width: var(--kokpit-icerik-max);
  margin: 0 auto;
  padding: 16px 18px 28px;
}
/* Geniş ekran: tavan kademeli büyür. Sonuç: 2K'da yan boşluk ~4%,
   ultrawide'da ~13%. Tavan altındaki viewport'larda kural bağlamaz. */
@media (min-width: 2300px) {
  .kokpit-sayfa {
    --kokpit-icerik-max: 2400px;
  }
}
@media (min-width: 3100px) {
  .kokpit-sayfa {
    --kokpit-icerik-max: 2600px;
  }
}
/* Kullanıcı tercihi: ferah görünüm. */
.kokpit-sayfa--ferah {
  --kokpit-bosluk: 24px;
}
.kokpit-sayfa--ferah :deep(.akis-satir),
.kokpit-sayfa--ferah :deep(.bekleyen-satir) {
  padding-top: 11px;
  padding-bottom: 11px;
}
.kokpit-sayfa--ferah :deep(.hizli-islem) {
  min-height: 48px;
}
/* Birincil karo ferah modda da destek karolardan belirgin yüksek kalır. */
.kokpit-sayfa--ferah :deep(.hizli-islem--birincil) {
  min-height: 58px;
}
/* Geniş ekranda 14px kart aralığı ince kalıyor: yalnız kompakt yoğunlukta
   ritmi 16px'e çıkar; kullanıcının "ferah" tercihi (24px) korunur. */
@media (min-width: 2100px) {
  .kokpit-sayfa:not(.kokpit-sayfa--ferah) {
    --kokpit-bosluk: 16px;
  }
}
/* Hero: sol (KPI + kritik), sağ (hızlı işlemler). */
.kokpit-hero {
  display: grid;
  grid-template-columns: minmax(0, 2.4fr) minmax(220px, 1fr);
  gap: var(--kokpit-bosluk);
  align-items: stretch;
  min-width: 0;
}
.kokpit-hero-sol {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}
.kokpit-hero-sag {
  min-width: 0;
}
/* Orta: son işlemler + bekleyen işler. */
.kokpit-orta {
  display: grid;
  grid-template-columns: minmax(0, 2.2fr) minmax(280px, 1fr);
  gap: var(--kokpit-bosluk);
  align-items: stretch;
  min-width: 0;
}
.kokpit-orta-akis,
.kokpit-orta-bekleyen {
  min-width: 0;
}
.kokpit-bolum-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 4px 0 -6px;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--text-secondary);
  text-transform: uppercase;
}
.kokpit-bolum-baslik::before {
  content: '';
  width: 16px;
  height: 2px;
  border-radius: 2px;
  background: var(--accent);
}
.kokpit-alt {
  display: flex;
  flex-direction: column;
  gap: var(--kokpit-bosluk);
  min-width: 0;
}
.kokpit-takvim {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}
.kokpit-takvim-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  text-align: left;
}
.kokpit-takvim-baslik:hover,
.kokpit-takvim-baslik:focus-visible {
  border-color: var(--accent-border);
  color: var(--accent);
}
.kokpit-takvim-baslik:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.kokpit-takvim-ok {
  margin-left: auto;
  font-size: 11px;
  transition: transform var(--dur-fast) var(--ease-standard);
}
.kokpit-takvim-ok--acik {
  transform: rotate(180deg);
}
@media (max-width: 1100px) {
  .kokpit-hero,
  .kokpit-orta {
    grid-template-columns: minmax(0, 1fr);
  }
}
.kokpit-bolumler {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--kokpit-bosluk);
  min-width: 0;
}
.kokpit-bolum {
  min-width: 0;
}
.kokpit-bolum > * {
  height: 100%;
}
.kokpit-hata {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 11px;
  border: 1px solid var(--danger-border);
  border-radius: var(--radius-md);
  background: var(--danger-soft);
  color: var(--danger-text);
  font-size: 12px;
}
.kokpit-hata-buton {
  margin-left: auto;
  padding: 3px 8px;
  border: 1px solid currentColor;
  border-radius: var(--radius-sm);
  background: transparent;
  color: inherit;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
}
.kokpit-hata-buton:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
@media (min-width: 1101px) {
  /* Tek sayida detay karti kalirsa (USER rolu / ozellestirme) yarim satir
     boslugu birakma; son karti iki kolona yay. */
  .kokpit-bolum:last-child:nth-child(odd) {
    grid-column: 1 / -1;
  }
}
@media (max-width: 1100px) {
  .kokpit-bolumler {
    grid-template-columns: minmax(0, 1fr);
  }
}
@media (max-width: 600px) {
  .kokpit-sayfa {
    padding: 10px 12px 22px;
  }
}
</style>
