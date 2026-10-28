<template>
  <div
    class="dashboard-container"
    :class="{ 'icerik-hazir': hazir }"
  >
    <DashboardHero
      :ozet="dashboardStore.ozet"
      :kullanici-ad="kullaniciAd"
      :sirket-ad="sirketAd"
      :yukleniyor="ozetYukleniyor && !dashboardStore.ozet"
      :yenileniyor="yenileniyor"
      :son-guncelleme="sonGuncelleme"
      :duzen-modu="duzenModu"
      @yenile="yenile"
      @ara="aramaAc"
      @duzenle="duzenModuDegistir"
    />

    <p
      class="sr-only"
      role="status"
      aria-live="polite"
    >
      {{ duyuru }}
    </p>

    <DashboardDuzenPaneli
      v-if="duzenModu"
      :widgetlar="panelWidgetlari"
      :kaydediliyor="kaydediliyor"
      @degistir="panelDegistir"
      @tasi="panelTasi"
      @sifirla="duzeniSifirla"
      @kaydet="duzeniKaydet"
      @iptal="duzeniIptal"
    />

    <div
      v-if="bosSistem && onboardingGoster && hazir"
      class="dashboard-onboarding"
    >
      <Onboarding @atla="onboardingAtla" />
    </div>

    <template v-else>
      <div class="dashboard-grid">
        <DashboardWidget
          v-for="(w, i) in gorunurWidgetlar"
          :key="w.key"
          :anahtar="w.key"
          :etiket="widgetEtiketi(w.key)"
          :boyut="w.boyut"
          :duzen-modu="duzenModu"
          :sira="i"
          :uzunluk="gorunurWidgetlar.length"
          @yukari="widgetTasiTikla(w.key, -1)"
          @asagi="widgetTasiTikla(w.key, 1)"
          @boyut="widgetBoyutTikla(w.key)"
          @gizle="widgetGizle(w.key)"
          @surukle-baslat="surukleBaslat"
          @birak="surukleBirak"
        >
          <DashboardKpiGrid
            v-if="w.key === 'kpi'"
            :kpi="dashboardStore.ozet?.kpi"
            :yukleniyor="ozetYukleniyor"
          />

          <DashboardGorevMerkezi
            v-else-if="w.key === 'gorevler'"
            :gorevler="dashboardStore.gorevler"
            :yukleniyor="gorevlerYukleniyor"
            :hata="gorevlerHata"
          />

          <DashboardIcgoruKarti
            v-else-if="w.key === 'icgoruler'"
            :icgoruler="dashboardStore.icgoruler"
            :yukleniyor="icgorulerYukleniyor"
          />

          <DashboardHedefler
            v-else-if="w.key === 'hedefler'"
            :hedef="grafikler?.hedef"
            :kpi="dashboardStore.ozet?.kpi"
          />

          <DashboardGrafikKarti
            v-else-if="w.key === 'nakitAkisi'"
            :baslik="t('dashboard.grafikNakitAkisi')"
            ikon="pi pi-chart-line"
            link-to="/kasa"
            tip="line"
            :veri="gunlukNakitAkisi"
            :secenekler="gunlukNakitAkisiOpsiyonlari"
            :yukleniyor="grafiklerYukleniyor"
            :bos-mesaj="t('dashboard.nakitAkisiVerisiYok')"
          />

          <DashboardGrafikKarti
            v-else-if="w.key === 'aylikGelirGider'"
            :baslik="t('dashboard.grafikAylikGelirGider')"
            ikon="pi pi-chart-bar"
            link-to="/raporlar"
            tip="bar"
            :veri="aylikGelirGider"
            :secenekler="aylikGelirGiderOpsiyonlari"
            :yukleniyor="grafiklerYukleniyor"
            :bos-mesaj="t('dashboard.gelirGiderVerisiYok')"
          />

          <DashboardGrafikKarti
            v-else-if="w.key === 'kategoriSatislari'"
            :baslik="t('dashboard.grafikKategori')"
            ikon="pi pi-chart-pie"
            link-to="/raporlar"
            tip="donut"
            :veri="kategoriSatislari"
            :secenekler="donutOpsiyonlar"
            :yukleniyor="grafiklerYukleniyor"
            :yukseklik="230"
            :bos-mesaj="t('dashboard.kategoriVerisiYok')"
          />

          <DashboardGrafikKarti
            v-else-if="w.key === 'enCokSatanlar'"
            :baslik="t('dashboard.grafikEnCokSatan')"
            ikon="pi pi-trophy"
            link-to="/stoklar"
            tip="bar"
            :veri="enCokSatanlar"
            :secenekler="enCokSatanlarOpsiyonlari"
            :yukleniyor="grafiklerYukleniyor"
            :bos-mesaj="t('dashboard.satisVerisiYok')"
          />

          <DashboardAkis
            v-else-if="w.key === 'akis'"
            :akis="dashboardStore.akis"
            :yukleniyor="akisYukleniyor"
            :hata="akisHata"
            @yenile="sessizAkisYenile"
          />

          <DashboardVade
            v-else-if="w.key === 'vade'"
            :gorevler="dashboardStore.gorevler"
            :yukleniyor="gorevlerYukleniyor"
          />

          <DashboardKritikStok
            v-else-if="w.key === 'kritikStok'"
            :stok="dashboardStore.ozet?.stok"
            :yukleniyor="ozetYukleniyor"
          />

          <DashboardHizliIslemler
            v-else-if="w.key === 'hizliIslemler'"
            :is-admin="authStore.isAdmin"
          />
        </DashboardWidget>
      </div>

      <section
        v-if="analizlerGorunur"
        class="dashboard-analizler"
      >
        <button
          type="button"
          class="analiz-baslik"
          :aria-expanded="analizlerAcik"
          aria-controls="dashboard-analiz-icerik"
          @click="analizlerDegistir"
        >
          <i
            class="pi pi-chart-pie"
            aria-hidden="true"
          />
          <span>{{ t('dashboard.tumAnalizler') }}</span>
          <i
            class="pi analiz-ok"
            :class="analizlerAcik ? 'pi-chevron-up' : 'pi-chevron-down'"
            aria-hidden="true"
          />
        </button>

        <div
          v-if="analizlerAcik"
          id="dashboard-analiz-icerik"
          class="dashboard-analiz-grid"
        >
          <DashboardGrafikKarti
            :baslik="t('dashboard.grafikYaslandirma')"
            ikon="pi pi-history"
            link-to="/tahsilat"
            tip="bar"
            :veri="alacakYaslandirma"
            :secenekler="alacakYaslandirmaOpsiyonlari"
            :yukleniyor="grafiklerYukleniyor"
            :bos-mesaj="t('dashboard.alacakVerisiYok')"
          />
          <DashboardGrafikKarti
            :baslik="t('dashboard.grafikEnBorclu')"
            ikon="pi pi-user-minus"
            link-to="/cari-hesaplar"
            tip="bar"
            :veri="enCokBorcCariler"
            :secenekler="cariOpsiyonlari"
            :yukleniyor="grafiklerYukleniyor"
            :bos-mesaj="t('dashboard.borcluCariYok')"
          />
          <DashboardGrafikKarti
            :baslik="t('dashboard.grafikEnAlacakli')"
            ikon="pi pi-user-plus"
            link-to="/cari-hesaplar"
            tip="bar"
            :veri="enCokAlacakCariler"
            :secenekler="cariOpsiyonlari"
            :yukleniyor="grafiklerYukleniyor"
            :bos-mesaj="t('dashboard.alacakliCariYok')"
          />
          <DashboardGrafikKarti
            :baslik="t('dashboard.grafikKasaBanka')"
            ikon="pi pi-wallet"
            tip="donut"
            :veri="kasaBanka"
            :secenekler="donutOpsiyonlar"
            :yukleniyor="grafiklerYukleniyor"
            :yukseklik="230"
            :bos-mesaj="t('dashboard.grafikBos')"
          />
          <DashboardGrafikKarti
            :baslik="t('dashboard.grafikPareto')"
            ikon="pi pi-chart-bar"
            link-to="/raporlar"
            tip="bar"
            :veri="kategoriPareto"
            :secenekler="kategoriParetoOpsiyonlari"
            :yukleniyor="grafiklerYukleniyor"
            :bos-mesaj="t('dashboard.kategoriVerisiYok')"
          />
          <DashboardGrafikKarti
            :baslik="t('dashboard.grafikCiroHedef')"
            ikon="pi pi-flag"
            link-to="/raporlar"
            tip="bar"
            :veri="ciroHedef"
            :secenekler="ciroHedefOpsiyonlari"
            :yukleniyor="grafiklerYukleniyor"
            :bos-mesaj="t('dashboard.gelirGiderVerisiYok')"
          />
        </div>
      </section>

      <div
        v-if="authStore.isAdmin && yedekUyarisiGoster"
        class="backup-reminder"
      >
        <i
          class="pi pi-save"
          aria-hidden="true"
        />
        <span>{{ t('dashboard.yedekUyari') }}
          <router-link to="/yedekler">{{ t('dashboard.yedekAlin') }}</router-link></span>
        <button
          type="button"
          class="reminder-close"
          :aria-label="t('common.close')"
          @click="yedekUyarisiGoster = false"
        >
          &times;
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useDashboardStore } from '../stores/dashboardStore.js'
import { useDovizStore } from '../stores/dovizStore.js'
import { useAuthStore } from '../stores/authStore.js'
import Onboarding from '../components/Onboarding.vue'
import DashboardHero from '../components/dashboard/DashboardHero.vue'
import DashboardKpiGrid from '../components/dashboard/DashboardKpiGrid.vue'
import DashboardGorevMerkezi from '../components/dashboard/DashboardGorevMerkezi.vue'
import DashboardIcgoruKarti from '../components/dashboard/DashboardIcgoruKarti.vue'
import DashboardHedefler from '../components/dashboard/DashboardHedefler.vue'
import DashboardGrafikKarti from '../components/dashboard/DashboardGrafikKarti.vue'
import DashboardAkis from '../components/dashboard/DashboardAkis.vue'
import DashboardVade from '../components/dashboard/DashboardVade.vue'
import DashboardKritikStok from '../components/dashboard/DashboardKritikStok.vue'
import DashboardHizliIslemler from '../components/dashboard/DashboardHizliIslemler.vue'
import DashboardWidget from '../components/dashboard/DashboardWidget.vue'
import DashboardDuzenPaneli from '../components/dashboard/DashboardDuzenPaneli.vue'
import { useDashboardGrafikleri } from '../composables/useDashboardGrafikleri.js'
import {
  DUZEN_VERSIYON,
  WIDGET_ETIKET_ANAHTARLARI,
  duzenGecerliMi,
  duzenNormallestir,
  rolDuzeni,
  widgetBirak,
  widgetBoyutDegistir,
  widgetGorunurlukDegistir,
  widgetTasi,
  yerelDuzenOku,
  yerelDuzenYaz
} from '../utils/dashboardDuzen.js'

const router = useRouter()
const { t } = useI18n()
const authStore = useAuthStore()
const dashboardStore = useDashboardStore()
const dovizStore = useDovizStore()

const rol = computed(() => authStore.kullanici?.role || 'ADMIN')

// --- Kisisellestirme -------------------------------------------------------
const duzen = ref(rolDuzeni(rol.value))
const taslak = ref(kopyalaDuzen(duzen.value))
const duzenModu = ref(false)
const kaydediliyor = ref(false)
const suruklenenWidget = ref(null)

function kopyalaDuzen(kaynak) {
  return {
    versiyon: DUZEN_VERSIYON,
    sira: (kaynak?.sira || []).map((w) => ({ ...w }))
  }
}

const panelWidgetlari = computed(() =>
  taslak.value.sira.map((w) => ({
    key: w.key,
    etiket: widgetEtiketi(w.key),
    gorunur: w.gorunur,
    boyut: w.boyut
  }))
)

const aktifDuzen = computed(() => (duzenModu.value ? taslak.value : duzen.value))
const gorunurWidgetlar = computed(() => aktifDuzen.value.sira.filter((w) => w.gorunur))

const widgetEtiketi = (key) => {
  const anahtar = WIDGET_ETIKET_ANAHTARLARI[key]
  return anahtar ? t(anahtar) : key
}

const taslakSirasi = (yeniSira) => {
  taslak.value = { versiyon: DUZEN_VERSIYON, sira: yeniSira }
}

const widgetTasiTikla = (key, yon) => {
  taslakSirasi(widgetTasi(taslak.value.sira, key, yon))
}

const widgetBoyutTikla = (key) => {
  taslakSirasi(widgetBoyutDegistir(taslak.value.sira, key))
}

const widgetGizle = (key) => {
  taslakSirasi(widgetGorunurlukDegistir(taslak.value.sira, key, false))
}

const panelDegistir = ({ key, alan, deger }) => {
  if (alan === 'gorunur') {
    taslakSirasi(widgetGorunurlukDegistir(taslak.value.sira, key, deger))
    return
  }
  if (alan === 'boyut') {
    taslakSirasi(taslak.value.sira.map((w) => (w.key === key ? { ...w, boyut: Number(deger) === 2 ? 2 : 1 } : w)))
  }
}

const panelTasi = ({ key, yon }) => {
  taslakSirasi(widgetTasi(taslak.value.sira, key, yon))
}

const surukleBaslat = (key) => {
  suruklenenWidget.value = key
}

const surukleBirak = (hedefKey) => {
  if (suruklenenWidget.value) {
    taslakSirasi(widgetBirak(taslak.value.sira, suruklenenWidget.value, hedefKey))
  }
  suruklenenWidget.value = null
}

const duzenModuDegistir = () => {
  if (duzenModu.value) {
    duzeniIptal()
    return
  }
  taslak.value = kopyalaDuzen(duzen.value)
  duzenModu.value = true
  duyur(t('dashboard.duzenModu'))
}

const duzeniIptal = () => {
  taslak.value = kopyalaDuzen(duzen.value)
  duzenModu.value = false
}

const duzeniSifirla = () => {
  taslak.value = rolDuzeni(rol.value)
  duyur(t('dashboard.duzenSifirlandi'))
}

const duzeniKaydet = async () => {
  if (kaydediliyor.value) return
  kaydediliyor.value = true
  const yeni = kopyalaDuzen(taslak.value)
  try {
    await dashboardStore.kaydetTercih(yeni)
    duzen.value = yeni
    yerelDuzenYaz(yeni, false)
    duzenModu.value = false
    duyur(t('dashboard.duzenKaydedildi'))
  } catch {
    // Backend'e yazilamadi: duzen cihazda tutulur, kullanici bilgilendirilir.
    duzen.value = yeni
    yerelDuzenYaz(yeni, true)
    duzenModu.value = false
    duyur(t('dashboard.duzenYerelKaydedildi'))
  } finally {
    kaydediliyor.value = false
  }
}

/**
 * Yukleme sirasi: backend tercihi -> (kaydedilememis) yerel duzen -> rol
 * varsayilani. Boylece cihazda bekleyen bir duzen, rol varsayilani tarafindan
 * ezilmez.
 */
const duzeniYukle = async () => {
  const varsayilan = rolDuzeni(rol.value)
  let aday = null
  const tercih = await dashboardStore.getTercih()
  if (duzenGecerliMi(tercih?.duzen)) {
    aday = tercih.duzen
  } else {
    const yerel = yerelDuzenOku()
    if (yerel?.kaydedilemedi && duzenGecerliMi(yerel.duzen)) aday = yerel.duzen
  }
  duzen.value = duzenNormallestir(aday || varsayilan, rol.value)
  if (!duzenModu.value) taslak.value = kopyalaDuzen(duzen.value)
}

// --- Veri yukleme ----------------------------------------------------------
const hazir = ref(false)
const yenileniyor = ref(false)
const sonGuncelleme = ref('')
const duyuru = ref('')
const onboardingGoster = ref(false)
const yedekUyarisiGoster = ref(true)
const analizlerAcik = ref(false)

const kullaniciAd = computed(
  () =>
    authStore.kullanici?.displayName ||
    authStore.kullanici?.username ||
    dashboardStore.ozet?.hero?.kullaniciAd ||
    ''
)
const sirketAd = computed(() => authStore.sirketAdi || dashboardStore.ozet?.hero?.sirketAd || '')

const ozetYukleniyor = computed(() => dashboardStore.ozetYukleniyor)
const gorevlerYukleniyor = computed(() => dashboardStore.gorevlerYukleniyor)
const grafiklerYukleniyor = computed(() => dashboardStore.grafiklerYukleniyor)
const akisYukleniyor = computed(() => dashboardStore.akisYukleniyor)
const icgorulerYukleniyor = computed(() => dashboardStore.icgorulerYukleniyor)
const gorevlerHata = computed(() => dashboardStore.gorevlerHata)
const akisHata = computed(() => dashboardStore.akisHata)

const grafikler = computed(() => dashboardStore.grafikler)
const {
  gunlukNakitAkisi,
  gunlukNakitAkisiOpsiyonlari,
  aylikGelirGider,
  aylikGelirGiderOpsiyonlari,
  kategoriSatislari,
  donutOpsiyonlar,
  enCokSatanlar,
  enCokSatanlarOpsiyonlari,
  alacakYaslandirma,
  alacakYaslandirmaOpsiyonlari,
  enCokBorcCariler,
  enCokAlacakCariler,
  cariOpsiyonlari,
  kasaBanka,
  kategoriPareto,
  kategoriParetoOpsiyonlari,
  ciroHedef,
  ciroHedefOpsiyonlari
} = useDashboardGrafikleri(grafikler)

const analizlerGorunur = computed(() => rol.value !== 'USER')

const bosSistem = computed(() => {
  const o = dashboardStore.ozet
  if (!o) return false
  const stok = Number(o.stok?.cesit) || 0
  const alacak = Number(o.kpi?.netCari?.alacak) || 0
  const borc = Math.abs(Number(o.kpi?.netCari?.borc) || 0)
  return stok === 0 && alacak === 0 && borc === 0
})

const duyur = (mesaj) => {
  duyuru.value = ''
  nextTick(() => {
    duyuru.value = mesaj
  })
}

const guncellemeZamaniGuncelle = () => {
  sonGuncelleme.value = new Date().toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })
}

const yenile = async () => {
  if (yenileniyor.value) return
  yenileniyor.value = true
  duyur(t('dashboard.yenileniyor'))
  try {
    // Doviz kurlari ikincil bilgidir; kritik yolu bloklamaz.
    dovizStore?.kurlariYukle?.().catch(() => {})
    // 1. dalga: hero + KPI + gorev sayaclari
    await dashboardStore.getOzet()
    // 2. dalga: gorev merkezi + icgoruler
    await Promise.all([dashboardStore.getGorevler(), dashboardStore.getIcgoruler()])
    // 3. dalga: grafikler + canli akis
    await Promise.all([dashboardStore.getGrafikler(), dashboardStore.getAkis()])
    guncellemeZamaniGuncelle()
    duyur(t('dashboard.sonGuncelleme', { zaman: sonGuncelleme.value }))
  } finally {
    yenileniyor.value = false
    hazir.value = true
  }
}

// Akis 60 sn'de bir sessizce tazelenir (yalnizca sayfa gorunurken).
const sessizAkisYenile = async () => {
  if (typeof document !== 'undefined' && document.visibilityState !== 'visible') return
  await dashboardStore.getAkis()
}

const aramaAc = () => {
  if (typeof window !== 'undefined' && typeof window.dispatchEvent === 'function') {
    window.dispatchEvent(new CustomEvent('raspel:hizli-ara'))
  }
}

const analizlerDegistir = () => {
  analizlerAcik.value = !analizlerAcik.value
}

const onboardingAtla = () => {
  localStorage.setItem('raspel_erp_onboarding_atlandi', '1')
  onboardingGoster.value = false
}

onMounted(async () => {
  if (authStore.isSaha) {
    router.replace('/saha-portali')
    return
  }
  onboardingGoster.value = !localStorage.getItem('raspel_erp_onboarding_atlandi')
  // Tercih okumasi kritik yolu bloklamaz; rol varsayilani ile hemen cizilir.
  duzeniYukle()
  await yenile()
})
</script>

<style scoped>
@import '../assets/dashboard.css';
</style>
