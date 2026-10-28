<template>
  <div
    class="dashboard-container"
    :class="{ 'icerik-hazir': hazir }"
  >
    <div class="pano">
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
        class="pano-onboarding"
      >
        <Onboarding @atla="onboardingAtla" />
      </div>

      <template v-else>
        <!-- Ust band: 4 KPI hucreleri (sabit konum, gizlenebilir) -->
        <DashboardKpiGrid
          v-if="kpiGorunur"
          :kpi="dashboardStore.ozet?.kpi"
          :yukleniyor="ozetYukleniyor"
        />

        <!-- Govde: ana icerik (sol) + sag ray -->
        <div
          class="pano-govde"
          :class="{ 'pano-govde--tek-kolon': bolgeler.some((b) => !b.widgetlar.length) }"
        >
          <template
            v-for="bolge in bolgeler"
            :key="bolge.key"
          >
            <component
              :is="bolge.key === 'ray' ? 'aside' : 'div'"
              v-if="bolge.widgetlar.length"
              :class="bolge.key === 'ray' ? 'pano-ray' : 'pano-ana'"
              :role="bolge.key === 'ray' ? null : 'group'"
              :aria-label="bolge.etiket"
            >
              <DashboardWidget
                v-for="(w, i) in bolge.widgetlar"
                :key="w.key"
                :anahtar="w.key"
                :etiket="widgetEtiketi(w.key)"
                :duzen-modu="duzenModu"
                :sira="i"
                :uzunluk="bolge.widgetlar.length"
                @yukari="widgetTasiTikla(w.key, -1)"
                @asagi="widgetTasiTikla(w.key, 1)"
                @gizle="widgetGizle(w.key)"
                @surukle-baslat="surukleBaslat"
                @birak="surukleBirak"
              >
                <DashboardBolum
                  v-if="w.key === 'gorevler'"
                  :baslik="t('dashboard.gorevMerkezi')"
                  ikon="pi pi-list-check"
                >
                  <DashboardGorevMerkezi
                    :gorevler="dashboardStore.gorevler"
                    :yukleniyor="gorevlerYukleniyor"
                    :hata="gorevlerHata"
                  />
                </DashboardBolum>

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
                >
                  <template #alt>
                    <div
                      class="nakit-projeksiyon"
                      data-test="nakit-projeksiyon"
                    >
                      <span class="nakit-projeksiyon-baslik">{{ t('dashboard.hedefNakitProjeksiyon') }}</span>
                      <div class="nakit-projeksiyon-cipler">
                        <div
                          v-for="p in projeksiyonlar"
                          :key="p.etiket"
                          class="nakit-cip"
                          :class="{ 'nakit-cip--negatif': p.deger < 0 }"
                          data-test="nakit-projeksiyon-cip"
                        >
                          <span class="nakit-cip-etiket">{{ p.etiket }}</span>
                          <strong class="nakit-cip-deger gizli-veri">{{ formatCurrency(p.deger) }}</strong>
                        </div>
                      </div>
                    </div>
                  </template>
                </DashboardGrafikKarti>

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

                <DashboardBolum
                  v-else-if="w.key === 'akis'"
                  :baslik="t('dashboard.akisBaslik')"
                  ikon="pi pi-bolt"
                  link-to="/hareketler"
                >
                  <DashboardAkis
                    :akis="dashboardStore.akis"
                    :yukleniyor="akisYukleniyor"
                    :hata="akisHata"
                    @yenile="sessizAkisYenile"
                  />
                </DashboardBolum>

                <DashboardBolum
                  v-else-if="w.key === 'icgoruler'"
                  :baslik="t('dashboard.icgoruler')"
                  ikon="pi pi-sparkles"
                >
                  <DashboardIcgoruKarti
                    :icgoruler="dashboardStore.icgoruler"
                    :yukleniyor="icgorulerYukleniyor"
                  />
                </DashboardBolum>

                <DashboardBolum
                  v-else-if="w.key === 'hizliIslemler'"
                  :baslik="t('dashboard.hizliIslemler')"
                  ikon="pi pi-bolt"
                >
                  <DashboardHizliIslemler :is-admin="authStore.isAdmin" />
                </DashboardBolum>

                <DashboardBolum
                  v-else-if="w.key === 'kritikStok'"
                  :baslik="t('dashboard.kritikStokUyarilari')"
                  ikon="pi pi-exclamation-triangle"
                >
                  <DashboardKritikStok
                    :stok="dashboardStore.ozet?.stok"
                    :yukleniyor="ozetYukleniyor"
                  />
                </DashboardBolum>
              </DashboardWidget>
            </component>
          </template>
        </div>

        <!-- Panonun alt bandi: tum analizler (acilir) -->
        <section
          v-if="analizlerGorunur"
          class="pano-analizler"
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
      </template>
    </div>

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
import DashboardBolum from '../components/dashboard/DashboardBolum.vue'
import DashboardKpiGrid from '../components/dashboard/DashboardKpiGrid.vue'
import DashboardGorevMerkezi from '../components/dashboard/DashboardGorevMerkezi.vue'
import DashboardIcgoruKarti from '../components/dashboard/DashboardIcgoruKarti.vue'
import DashboardGrafikKarti from '../components/dashboard/DashboardGrafikKarti.vue'
import DashboardAkis from '../components/dashboard/DashboardAkis.vue'
import DashboardKritikStok from '../components/dashboard/DashboardKritikStok.vue'
import DashboardHizliIslemler from '../components/dashboard/DashboardHizliIslemler.vue'
import DashboardWidget from '../components/dashboard/DashboardWidget.vue'
import DashboardDuzenPaneli from '../components/dashboard/DashboardDuzenPaneli.vue'
import { useDashboardGrafikleri } from '../composables/useDashboardGrafikleri.js'
import { formatCurrency } from '../utils/format.js'
import {
  DUZEN_VERSIYON,
  SABIT_ANAHTARLARI,
  WIDGET_ETIKET_ANAHTARLARI,
  duzenGecerliMi,
  duzenNormallestir,
  rolDuzeni,
  widgetBirak,
  widgetBolgesi,
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
    bolge: widgetBolgesi(w.key),
    sabit: SABIT_ANAHTARLARI.includes(w.key)
  }))
)

const aktifDuzen = computed(() => (duzenModu.value ? taslak.value : duzen.value))
const gorunurWidgetlar = computed(() => aktifDuzen.value.sira.filter((w) => w.gorunur))

const kpiGorunur = computed(() => gorunurWidgetlar.value.some((w) => w.key === 'kpi'))

/**
 * Duz liste -> gorsel bolgeler. Bolge, anahtardan turetilir; `kpi` ust bantta
 * ayrica cizildigi icin bolgelere girmez.
 */
const bolgeler = computed(() => {
  const kpiHaric = gorunurWidgetlar.value.filter((w) => w.key !== 'kpi')
  return [
    {
      key: 'ana',
      etiket: t('dashboard.anaIcerik'),
      widgetlar: kpiHaric.filter((w) => widgetBolgesi(w.key) === 'ana')
    },
    {
      key: 'ray',
      etiket: t('dashboard.sagRay'),
      widgetlar: kpiHaric.filter((w) => widgetBolgesi(w.key) === 'ray')
    }
  ]
})

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

const widgetGizle = (key) => {
  taslakSirasi(widgetGorunurlukDegistir(taslak.value.sira, key, false))
}

const panelDegistir = ({ key, alan, deger }) => {
  if (alan === 'gorunur') {
    taslakSirasi(widgetGorunurlukDegistir(taslak.value.sira, key, deger))
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
  taslak.value = duzenNormallestir(rolDuzeni(rol.value), rol.value)
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
 * ezilmez. Her durumda `duzenNormallestir` eski anahtarlari (hedefler/vade) ve
 * `boyut` alanini temizler.
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

/**
 * 30/60/90 gunluk nakit projeksiyonu, Nakit Akisi bolumunun altinda 3 kompakt
 * cip olarak gosterilir (ayri panel yok).
 */
const projeksiyonlar = computed(() => {
  const kpi = dashboardStore.ozet?.kpi
  const likidite = Number(kpi?.likidite?.deger) || 0
  const alacak = Number(kpi?.netCari?.alacak) || 0
  const borc = Math.abs(Number(kpi?.netCari?.borc) || 0)
  const gun30 = likidite + alacak * 0.45 - borc * 0.4
  const gun60 = gun30 + alacak * 0.35 - borc * 0.35
  const gun90 = gun60 + alacak * 0.2 - borc * 0.25
  return [
    { etiket: t('dashboard.hedefGun30'), deger: Math.round(gun30) },
    { etiket: t('dashboard.hedefGun60'), deger: Math.round(gun60) },
    { etiket: t('dashboard.hedefGun90'), deger: Math.round(gun90) }
  ]
})

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

<style src="../assets/dashboard.css"></style>
