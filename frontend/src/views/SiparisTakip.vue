<template>
  <div class="takip-sayfasi">
    <PageHeader
      :title="t('siparisTakip.title')"
      :subtitle="t('siparisTakip.subtitle')"
    >
      <template #actions>
        <Button
          icon="pi pi-refresh"
          :aria-label="t('common.refresh')"
          class="p-button-text p-button-sm"
          @click="yukle"
        />
      </template>
    </PageHeader>

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
        @change="filtreDegisti"
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
      <Button
        :label="t('denetim.temizle')"
        icon="pi pi-filter-slash"
        class="p-button-text p-button-sm"
        @click="filtreTemizle"
      />
    </div>

    <div
      v-if="yukleniyor"
      class="bos"
    >
      {{ t('common.loading') }}
    </div>
    <div
      v-else-if="!satirlar.length"
      class="bos"
    >
      {{ filtreVar ? t('siparisTakip.filtreBos') : t('siparisTakip.bos') }}
    </div>

    <div
      v-for="s in satirlar"
      :key="s.siparisId"
      class="takip-kart"
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
          @click="uretimEmriSor(s)"
        />
      </div>

      <div class="adimlar">
        <!-- 1. Sipariş -->
        <div
          class="adim"
          :class="{ tamam: siparisTamam(s) }"
        >
          <i class="pi pi-file" />
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
          <i class="pi pi-cog" />
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
          <i class="pi pi-truck" />
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
          <i class="pi pi-map-marker" />
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { siparisTakipAPI, uretimAPI } from '../api/index.js'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useI18n } from 'vue-i18n'
import { formatTarih } from '../utils/format.js'

const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()

const satirlar = ref([])
const toplam = ref(0)
const sayfa = ref(0)
const sayfaBoyutu = ref(25)
const yukleniyor = ref(false)
const emirOlusturuluyor = ref(null)

const filtreQ = ref('')
const filtreDurum = ref(null)
const filtreSofor = ref(null)
let aramaZaman = null

const soforListesi = ref([])

// Sipariş seviyesindeki durumlar (filtre için).
const SIPARIS_DURUMLARI = ['TEKLIF', 'SIPARIS', 'BEKLIYOR', 'HAZIRLANIYOR', 'YOLDA', 'TESLIM_EDILDI', 'FATURA_KESILDI', 'IPTAL']
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

const yukle = async () => {
  yukleniyor.value = true
  try {
    const params = { page: sayfa.value, size: sayfaBoyutu.value }
    if (filtreQ.value?.trim()) params.q = filtreQ.value.trim()
    if (filtreDurum.value) params.durum = filtreDurum.value
    if (filtreSofor.value) params.sofor = filtreSofor.value
    const r = await siparisTakipAPI.zincir(params)
    satirlar.value = r.data?.content || unwrapList(r)
    toplam.value = r.data?.totalElements ?? satirlar.value.length
  } catch {
    toastBildirim.hata(t('siparisTakip.hataYukleme'))
  }
  yukleniyor.value = false
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
  sayfa.value = 0
  yukle()
}

const sayfaDegisti = (e) => {
  sayfa.value = e.page
  sayfaBoyutu.value = e.rows
  yukle()
}

onMounted(() => {
  soforleriYukle()
  yukle()
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
</style>
