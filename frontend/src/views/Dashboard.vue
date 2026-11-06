<template>
  <div
    class="kokpit-sayfa"
    data-test="dashboard-kokpit"
  >
    <KokpitGlobalFiltre
      :donem="kokpitStore.donem"
      :para-birimi="kokpitStore.paraBirimi"
      :canli="canliGosterge"
      :son-guncelleme="kokpitStore.sonGuncelleme"
      :yukleniyor="kokpitStore.yukleniyor"
      @update:donem="donemDegistir"
      @update:para-birimi="paraBirimiDegistir"
      @canli-degistir="canliDegistir"
      @yenile="yenile"
      @excel="excelIndir"
      @pdf="pdfIndir"
      @ozellestir="ozellestirmeAcik = true"
    />

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
    />

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
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore.js'
import { useKokpitStore } from '../stores/kokpitStore.js'
import { useDashboardCanli } from '../composables/useDashboardCanli.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { excelAPI } from '../api/index.js'
import { formatDateTime } from '../utils/format.js'
import KokpitGlobalFiltre from '../components/dashboard/kokpit/KokpitGlobalFiltre.vue'
import FinansOzetBandi from '../components/dashboard/kokpit/FinansOzetBandi.vue'
import KritikDurumSeridi from '../components/dashboard/kokpit/KritikDurumSeridi.vue'
import NakitBolumu from '../components/dashboard/kokpit/NakitBolumu.vue'
import AlacakVerecekBolumu from '../components/dashboard/kokpit/AlacakVerecekBolumu.vue'
import KarMarjBolumu from '../components/dashboard/kokpit/KarMarjBolumu.vue'
import FaturaSiparisBolumu from '../components/dashboard/kokpit/FaturaSiparisBolumu.vue'

const { t } = useI18n()
const router = useRouter()
const authStore = useAuthStore()
const kokpitStore = useKokpitStore()
const toastBildirim = useToastBildirim()

const ozellestirmeAcik = ref(false)
const duyuru = ref('')

const kokpit = computed(() => kokpitStore.kokpit)
const kpi = computed(() => (Array.isArray(kokpit.value?.ozet) ? kokpit.value.ozet : []))
const kritikDurum = computed(() =>
  Array.isArray(kokpit.value?.kritikDurum) ? kokpit.value.kritikDurum : []
)
const ilkYukleme = computed(() => kokpitStore.yukleniyor && !kokpit.value)

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
    return liste.filter((anahtar) => anahtar !== 'karMarj')
  }
  return liste
})

const bolumBileseni = (anahtar) => BILESENLER[anahtar] || null
const bolumVerisi = (anahtar) => kokpit.value?.[anahtar] || null

// --- Canlı mod ---------------------------------------------------------------
const canli = useDashboardCanli({
  sirketId: () => authStore.sirketId,
  yedekYenile: () => yenile()
})
const canliGosterge = computed(() => canli.aktif.value && canli.bagli.value)

watch(
  () => canli.sonOlay.value,
  (olay) => {
    if (olay && canli.aktif.value) yenile()
  }
)

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
}

const yenile = async () => {
  await kokpitStore.getKokpit()
  if (!kokpitStore.hata) {
    duyuru.value = t('dashboard.canliGuncellendi')
  }
}

const detayaGit = (hedef) => {
  if (!hedef?.route) return
  router.push({ path: hedef.route, query: hedef.query || {} })
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
  kokpitStore.paraBirimiDegistir(kayitliDeger(PARA_ANAHTAR, PARALAR, 'TRY'))
  await Promise.all([
    kokpitStore.getKokpit(),
    kokpitStore.tercihYukle()
  ])
})
</script>

<style scoped>
.kokpit-sayfa {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
  padding: 12px 14px 24px;
}
.kokpit-bolumler {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  min-width: 0;
}
.kokpit-bolum {
  min-width: 0;
}
.kokpit-hata {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 11px;
  border: 1px solid var(--danger-border);
  border-radius: var(--radius-md);
  background: var(--danger-soft);
  color: var(--danger);
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
@media (max-width: 1100px) {
  .kokpit-bolumler {
    grid-template-columns: minmax(0, 1fr);
  }
}
@media (max-width: 600px) {
  .kokpit-sayfa {
    padding: 8px 9px 20px;
  }
}
</style>
