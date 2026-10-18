<template>
  <div class="takip-sayfasi">
    <PageHeader
      :title="t('siparisTakip.title')"
      :subtitle="t('siparisTakip.subtitle')"
    >
      <template #actions>
        <Button
          :label="t('siparisTakip.excel')"
          icon="pi pi-file-excel"
          class="p-button-outlined"
          :loading="excelYukleniyor"
          @click="excelIndir"
        />
        <Button
          icon="pi pi-refresh"
          :aria-label="t('common.refresh')"
          class="p-button-text p-button-sm"
          @click="yukle"
        />
      </template>
    </PageHeader>

    <!-- KPI ozeti -->
    <div class="kpi-grid">
      <KpiKart
        :baslik="t('siparisTakip.kpiToplam')"
        :deger="ozet.toplam"
        ikon="pi pi-shopping-cart"
        :para-birimi="false"
        renk="#3b82f6"
      />
      <KpiKart
        :baslik="t('siparisTakip.kpiBekleyen')"
        :deger="ozet.bekleyen"
        ikon="pi pi-clock"
        :para-birimi="false"
        renk="#f59e0b"
      />
      <KpiKart
        :baslik="t('siparisTakip.kpiUretimde')"
        :deger="ozet.uretimde"
        ikon="pi pi-cog"
        :para-birimi="false"
        renk="#8b5cf6"
      />
      <KpiKart
        :baslik="t('siparisTakip.kpiSevkte')"
        :deger="ozet.sevkte"
        ikon="pi pi-truck"
        :para-birimi="false"
        renk="#0ea5e9"
      />
      <KpiKart
        :baslik="t('siparisTakip.kpiTeslim')"
        :deger="ozet.teslimEdildi"
        ikon="pi pi-check-circle"
        :para-birimi="false"
        renk="#10b981"
      />
      <KpiKart
        :baslik="t('siparisTakip.kpiGeciken')"
        :deger="ozet.geciken"
        ikon="pi pi-exclamation-triangle"
        :para-birimi="false"
        renk="#ef4444"
      />
    </div>

    <!-- Filtre çubuğu: sipariş no, durum, şoför -->
    <div class="takip-filtre">
      <IconField class="filtre-arama">
        <InputIcon class="pi pi-search" />
        <InputText
          v-model="filtreQ"
          :placeholder="t('siparisTakip.aramaPlaceholder')"
          @input="filtreDegisti"
        />
      </IconField>
      <Dropdown
        v-model="filtreDurum"
        :options="durumSecenekleri"
        option-label="ad"
        option-value="deger"
        :placeholder="t('siparisTakip.durumFiltre')"
        show-clear
        class="filtre-select"
        @change="filtreCipSenkron"
      />
      <Dropdown
        v-model="filtreSofor"
        :options="soforSecenekleri"
        option-label="ad"
        option-value="deger"
        :placeholder="t('siparisTakip.tumSoforler')"
        show-clear
        filter
        class="filtre-select"
        @change="filtreDegisti"
      />
      <Dropdown
        v-model="sirala"
        :options="siralamaSecenekleri"
        option-label="ad"
        option-value="deger"
        class="filtre-select"
        @change="filtreDegisti"
      />
      <Button
        :label="t('denetim.temizle')"
        icon="pi pi-filter-slash"
        class="p-button-text p-button-sm"
        @click="filtreTemizle"
      />
    </div>

    <!-- Hizli filtre cipleri -->
    <div class="takip-cipler">
      <button
        v-for="c in hizliCiplar"
        :key="c.deger"
        type="button"
        class="takip-cip"
        :class="{ aktif: hizliFiltre === c.deger }"
        @click="hizliFiltreUygula(c.deger)"
      >
        <i :class="c.ikon" /> {{ c.ad }}
      </button>
    </div>

    <div
      v-if="yukleniyor"
      class="bos"
    >
      {{ t('common.loading') }}
    </div>
    <EmptyState
      v-else-if="!satirlar.length"
      icon="pi pi-shopping-cart"
      :message="filtreVar ? t('siparisTakip.filtreBos') : t('siparisTakip.bos')"
    />

    <div
      v-for="s in satirlar"
      :key="s.siparisId"
      class="takip-kart tiklanabilir"
      @click="satirAc(s)"
    >
      <div class="kart-ust">
        <strong>{{ s.siparisNo }}</strong>
        <span
          v-if="s.cariAd"
          class="muted"
        >{{ s.cariAd }}</span>
        <Tag
          v-if="s.driverAd"
          :value="t('siparisTakip.soforEtiket', { ad: s.driverAd })"
          severity="info"
        />
        <Button
          v-if="!s.uretimDurum && s.siparisDurum !== 'IPTAL'"
          :label="t('uretim.siparistenEmir')"
          icon="pi pi-cog"
          class="p-button-sm p-button-outlined uretim-buton"
          :loading="emirOlusturuluyor === s.siparisId"
          @click.stop="uretimEmriSor(s)"
        />
      </div>

      <div class="adimlar">
        <!-- 1. Sipariş -->
        <div
          class="adim"
          :class="{ tamam: siparisTamam(s) }"
        >
          <i :class="siparisTamam(s) ? 'pi pi-check' : 'pi pi-file'" />
          <span>{{ t('siparisTakip.siparis') }}</span>
          <Tag
            :value="durumEtiket(s.siparisDurum)"
            :severity="durumSeverity(s.siparisDurum)"
          />
        </div>
        <div class="ok">
          <i class="pi pi-arrow-right" />
        </div>

        <!-- 2. Üretim -->
        <div
          class="adim"
          :class="{ tamam: uretimTamam(s) }"
        >
          <i :class="uretimTamam(s) ? 'pi pi-check' : 'pi pi-cog'" />
          <span>{{ t('siparisTakip.uretim') }}</span>
          <Tag
            v-if="s.uretimDurum"
            :value="durumEtiket(s.uretimDurum)"
            :severity="durumSeverity(s.uretimDurum)"
          />
          <Tag
            v-else
            :value="t('siparisTakip.yok')"
            severity="secondary"
          />
          <span
            v-if="s.uretimSayisi > 1"
            class="adet-rozet"
          >×{{ s.uretimSayisi }}</span>
        </div>
        <div class="ok">
          <i class="pi pi-arrow-right" />
        </div>

        <!-- 3. Sevk -->
        <div
          class="adim"
          :class="{ tamam: sevkTamam(s) }"
        >
          <i :class="sevkTamam(s) ? 'pi pi-check' : 'pi pi-truck'" />
          <span>{{ t('siparisTakip.sevk') }}</span>
          <Tag
            v-if="s.sevkDurum"
            :value="durumEtiket(s.sevkDurum)"
            :severity="durumSeverity(s.sevkDurum)"
          />
          <Tag
            v-else
            :value="t('siparisTakip.yok')"
            severity="secondary"
          />
          <span
            v-if="s.sevkSayisi > 1"
            class="adet-rozet"
          >×{{ s.sevkSayisi }}</span>
        </div>
        <div class="ok">
          <i class="pi pi-arrow-right" />
        </div>

        <!-- 4. Teslimat -->
        <div
          class="adim"
          :class="{ tamam: s.teslimatDurum === 'TESLIM_EDILDI' }"
        >
          <i :class="s.teslimatDurum === 'TESLIM_EDILDI' ? 'pi pi-check' : 'pi pi-map-marker'" />
          <span>{{ t('siparisTakip.teslimat') }}</span>
          <Tag
            v-if="s.teslimatDurum"
            :value="durumEtiket(s.teslimatDurum)"
            :severity="durumSeverity(s.teslimatDurum)"
          />
          <Tag
            v-else
            :value="t('siparisTakip.yok')"
            severity="secondary"
          />
          <Tag
            v-if="s.teslimatGecikti"
            :value="t('siparisTakip.gecikti')"
            severity="danger"
          />
          <span
            v-if="s.beklenenTeslimTarihi"
            class="beklenen-tarih"
          >{{ formatTarih(s.beklenenTeslimTarihi) }}</span>
        </div>
      </div>
    </div>

    <Paginator
      v-if="toplam > sayfaBoyutu"
      :rows="sayfaBoyutu"
      :total-records="toplam"
      :first="sayfa * sayfaBoyutu"
      :rows-per-page-options="[10, 25, 50]"
      @page="sayfaDegisti"
    />

    <SiparisTakipDetayDrawer
      v-model:visible="detayDrawer"
      :satir="seciliSatir"
      @uretim-emri="uretimEmriSor"
      @git="detayaGit"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { siparisTakipAPI, uretimAPI, excelAPI } from '../api/index.js'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import KpiKart from '../components/KpiKart.vue'
import EmptyState from '../components/EmptyState.vue'
import SiparisTakipDetayDrawer from '../components/SiparisTakipDetayDrawer.vue'
import { useI18n } from 'vue-i18n'
import { formatTarih } from '../utils/format.js'

const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const router = useRouter()
const { t } = useI18n()

const satirlar = ref([])
const toplam = ref(0)
const sayfa = ref(0)
const SAYFA_BOYUTU_ANAHTAR = 'raspel_siparis_takip_sayfa_boyutu'
const sayfaBoyutu = ref(Number(localStorage.getItem(SAYFA_BOYUTU_ANAHTAR)) || 25)
const yukleniyor = ref(false)
const excelYukleniyor = ref(false)
const emirOlusturuluyor = ref(null)
const detayDrawer = ref(false)
const seciliSatir = ref(null)

const satirAc = (s) => {
  seciliSatir.value = s
  detayDrawer.value = true
}
const detayaGit = (name) => {
  detayDrawer.value = false
  router.push({ name })
}

const filtreQ = ref('')
const filtreDurum = ref(null)
const filtreSofor = ref(null)
const hizliFiltre = ref('TUMU')
const sirala = ref('tarih,desc')
let aramaZaman = null

const soforListesi = ref([])

// KPI ozeti.
const ozet = ref({ toplam: 0, bekleyen: 0, uretimde: 0, sevkte: 0, teslimEdildi: 0, geciken: 0, iptal: 0 })
const ozetYukle = async () => {
  try {
    const r = await siparisTakipAPI.ozet()
    ozet.value = { ...ozet.value, ...(r.data || {}) }
  } catch {
    /* ozet alinamazsa KPI sifirda kalir */
  }
}

const siralamaSecenekleri = computed(() => [
  { ad: t('siparisTakip.siralaTarih'), deger: 'tarih,desc' },
  { ad: t('siparisTakip.siralaNo'), deger: 'siparisNo,asc' }
])

// Hizli filtre cipleri (sunucu tarafi).
const hizliCiplar = computed(() => [
  { deger: 'TUMU', ad: t('siparisTakip.cipTumu'), ikon: 'pi pi-list' },
  { deger: 'GECIKEN', ad: t('siparisTakip.cipGeciken'), ikon: 'pi pi-exclamation-triangle' },
  { deger: 'URETIM', ad: t('siparisTakip.cipUretimde'), ikon: 'pi pi-cog' },
  { deger: 'SEVK', ad: t('siparisTakip.cipSevkte'), ikon: 'pi pi-truck' },
  { deger: 'TESLIM', ad: t('siparisTakip.cipTeslim'), ikon: 'pi pi-check-circle' }
])

const hizliFiltreUygula = (deger) => {
  hizliFiltre.value = deger
  filtreDurum.value = null
  sayfa.value = 0
  yukle()
}

// Durum dropdown'i elle degisirse cip "Tumu"ya doner.
const filtreCipSenkron = () => {
  hizliFiltre.value = filtreDurum.value ? 'DURUM' : 'TUMU'
  filtreDegisti()
}

// Sipariş seviyesindeki durumlar (filtre için).
const SIPARIS_DURUMLARI = ['TASLAK', 'TEKLIF', 'SIPARIS', 'BEKLIYOR', 'HAZIRLANIYOR', 'YOLDA', 'TESLIM_EDILDI', 'FATURA_KESILDI', 'IPTAL']
const durumSecenekleri = computed(() => SIPARIS_DURUMLARI.map((d) => ({ ad: durumEtiket(d), deger: d })))
const soforSecenekleri = computed(() => soforListesi.value.map((s) => ({ ad: s, deger: s })))

const filtreVar = computed(() => !!(filtreQ.value?.trim() || filtreDurum.value || filtreSofor.value))

// Ham enum kodlarını (FATURA_KESILDI) çevir; bilinmeyen kodu olduğu gibi bırak.
const durumEtiket = (kod) => {
  if (!kod) return ''
  const anahtar = `siparisTakip.durum.${kod}`
  const ceviri = t(anahtar)
  return ceviri === anahtar ? kod : ceviri
}

// "tamam" artık yalnızca GERÇEKTEN tamamlanan durumlar için (eskiden TASLAK
// bile tamam sayılıyordu).
const siparisTamam = (s) => !['TEKLIF', 'TASLAK', 'IPTAL'].includes(s.siparisDurum) && !!s.siparisDurum
const uretimTamam = (s) => s.uretimDurum === 'TAMAMLANDI'
const sevkTamam = (s) => ['KESILDI', 'TAMAMLANDI'].includes(s.sevkDurum)

const durumSeverity = (d) => {
  if (!d) return 'secondary'
  if (['IPTAL', 'REDDEDILDI', 'HATA'].includes(d)) return 'danger'
  if (['TAMAMLANDI', 'KESILDI', 'TESLIM_EDILDI', 'ONAYLANDI'].includes(d)) return 'success'
  if (['URETIMDE', 'BEKLEMEDE', 'YOLDA', 'HAZIRLANIYOR'].includes(d)) return 'info'
  return 'warn'
}

const uretimEmriSor = (s) => {
  confirm.require({
    header: t('uretim.siparistenEmir'),
    message: t('siparisTakip.uretimEmriOnay', { no: s.siparisNo }),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evet'),
    rejectLabel: t('common.cancel'),
    accept: () => uretimEmriOlustur(s)
  })
}

const uretimEmriOlustur = async (s) => {
  emirOlusturuluyor.value = s.siparisId
  try {
    const r = await uretimAPI.siparistenEmir(s.siparisId)
    const adet = (r.data || []).length
    toastBildirim.basarili(t('uretim.emirOlusturulduN', { n: adet }))
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('uretim.emirOlusturulamadi'))
  } finally {
    emirOlusturuluyor.value = null
  }
}

// Liste ve Excel'in paylastigi filtre parametreleri.
const filtreParams = () => {
  const params = {}
  if (filtreQ.value?.trim()) params.q = filtreQ.value.trim()
  if (filtreDurum.value) params.durum = filtreDurum.value
  if (filtreSofor.value) params.sofor = filtreSofor.value
  if (hizliFiltre.value === 'GECIKEN') params.geciken = true
  if (hizliFiltre.value === 'URETIM') params.uretimde = true
  if (hizliFiltre.value === 'SEVK') params.durum = 'YOLDA'
  if (hizliFiltre.value === 'TESLIM') params.durum = 'TESLIM_EDILDI'
  return params
}

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await siparisTakipAPI.zincir({
      page: sayfa.value,
      size: sayfaBoyutu.value,
      sort: sirala.value,
      ...filtreParams()
    })
    satirlar.value = r.data?.content || unwrapList(r)
    toplam.value = r.data?.totalElements ?? satirlar.value.length
  } catch {
    toastBildirim.hata(t('siparisTakip.hataYukleme'))
  }
  yukleniyor.value = false
}

const excelIndir = async () => {
  excelYukleniyor.value = true
  try {
    const res = await excelAPI.siparisTakip(filtreParams())
    const url = window.URL.createObjectURL(
      new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    )
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `siparis-takip-${new Date().toISOString().slice(0, 10)}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    toastBildirim.basarili(t('siparisTakip.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('siparisTakip.excelHata'))
  } finally {
    excelYukleniyor.value = false
  }
}

const soforleriYukle = async () => {
  try {
    const r = await siparisTakipAPI.soforler()
    soforListesi.value = Array.isArray(r.data) ? r.data : unwrapList(r)
  } catch {
    soforListesi.value = []
  }
}

const filtreDegisti = () => {
  if (aramaZaman) clearTimeout(aramaZaman)
  aramaZaman = setTimeout(() => {
    sayfa.value = 0
    yukle()
  }, 300)
}

const filtreTemizle = () => {
  filtreQ.value = ''
  filtreDurum.value = null
  filtreSofor.value = null
  hizliFiltre.value = 'TUMU'
  sirala.value = 'tarih,desc'
  sayfa.value = 0
  yukle()
}

const sayfaDegisti = (e) => {
  sayfa.value = e.page
  sayfaBoyutu.value = e.rows
  localStorage.setItem(SAYFA_BOYUTU_ANAHTAR, String(e.rows))
  yukle()
}

onMounted(() => {
  soforleriYukle()
  yukle()
  ozetYukle()
})
</script>

<style scoped>
.takip-sayfasi {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.takip-filtre {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.filtre-arama {
  min-width: 240px;
  flex: 1;
}
.filtre-select {
  min-width: 180px;
}
.uretim-buton {
  margin-left: auto;
}
.beklenen-tarih {
  font-size: 11px;
  color: var(--text-muted);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.adet-rozet {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 600;
}
.bos {
  text-align: center;
  color: var(--text-muted);
  padding: 32px;
}
.takip-kart {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 16px;
}
.kart-ust {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.muted {
  font-size: 12px;
  color: var(--text-muted);
}
.adimlar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.adim {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 8px;
  background: var(--bg-primary);
  border: 1px solid var(--border);
  font-size: 13px;
  opacity: 0.75;
}
.adim.tamam {
  opacity: 1;
  border-color: rgba(16, 185, 129, 0.4);
}
.adim i {
  color: var(--accent, var(--accent));
}
.ok {
  color: var(--text-muted);
}
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(160px, 100%), 1fr));
  gap: 12px;
}
.takip-cipler {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.takip-cip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 9999px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}
.takip-cip:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.takip-cip.aktif {
  background: var(--accent-soft);
  border-color: var(--accent);
  color: var(--accent);
}
.tiklanabilir {
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.tiklanabilir:hover {
  border-color: var(--accent);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.12);
}
.adim.aktif {
  opacity: 1;
  border-color: var(--accent);
}

/* Mobil: filtreler ve kart aksiyonlari tam genislik. */
@media (max-width: 640px) {
  .takip-filtre .filtre-arama,
  .takip-filtre .filtre-select {
    min-width: 100%;
  }
  .uretim-buton {
    margin-left: 0;
    width: 100%;
  }
  .adimlar {
    gap: 6px;
  }
  .adim {
    font-size: 12px;
    padding: 5px 8px;
  }
}
</style>
