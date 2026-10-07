<template>
  <div class="denetim-page">
    <PageHeader
      :title="t('denetim.title')"
      :subtitle="t('denetim.subtitle')"
    >
      <template #actions>
        <Button
          :label="t('denetim.excel')"
          icon="pi pi-file-excel"
          class="p-button-sm p-button-outlined"
          :loading="excelYukleniyor"
          @click="excelIndir"
        />
      </template>
    </PageHeader>

    <Card class="filtre-karti">
      <template #content>
        <div class="filtre-grid">
          <div class="filtre-alan">
            <label>{{ t('denetim.islemTuru') }}</label>
            <Select
              v-model="filtre.islem"
              :options="islemTipleri"
              :placeholder="t('denetim.tumu')"
              class="w-full"
              show-clear
              clear-icon="pi pi-times"
              @change="filtrele"
            />
          </div>
          <div class="filtre-alan">
            <label>{{ t('denetim.entity') }}</label>
            <Select
              v-model="filtre.entityAdi"
              :options="entityListesi"
              :placeholder="t('denetim.tumu')"
              class="w-full"
              show-clear
              clear-icon="pi pi-times"
              @change="filtrele"
            />
          </div>
          <div class="filtre-alan">
            <label>{{ t('denetim.kullanici') }}</label>
            <Select
              v-model="filtre.kullaniciId"
              :options="kullanicilar"
              option-label="ad"
              option-value="id"
              :placeholder="t('denetim.tumu')"
              class="w-full"
              filter
              show-clear
              clear-icon="pi pi-times"
              @change="filtrele"
            />
          </div>
          <div class="filtre-alan filtre-tarih">
            <label>{{ t('denetim.tarihAraligi') }}</label>
            <TarihHizliSecim v-model="filtre.tarihAraligi" />
          </div>
          <div class="filtre-alan">
            <label>{{ t('denetim.ozelTarihAraligi') }}</label>
            <DatePicker
              v-model="filtre.tarihAraligi"
              selection-mode="range"
              date-format="dd.mm.yy"
              :placeholder="t('denetim.baslangicBitis')"
              class="w-full"
            />
          </div>
          <div class="filtre-aksiyon">
            <Button
              :label="t('denetim.filtreKaydet')"
              icon="pi pi-bookmark"
              class="p-button-sm p-button-text"
              @click="kaydetDialogAc"
            />
            <Dropdown
              v-model="seciliKayitliFiltre"
              :options="kayitliFiltreler"
              option-label="ad"
              :placeholder="t('denetim.kayitliFiltreler')"
              class="kayitli-filtre"
              @change="kayitliFiltreYukle"
            />
            <Button
              icon="pi pi-trash"
              class="p-button-sm p-button-text p-button-danger"
              :title="t('denetim.filtreSil')"
              :aria-label="t('denetim.filtreSil')"
              :disabled="!seciliKayitliFiltre"
              @click="kayitliFiltreSil"
            />
            <Button
              :label="t('denetim.temizle')"
              icon="pi pi-filter-slash"
              class="p-button-sm p-button-text"
              @click="filtreTemizle"
            />
          </div>
        </div>
      </template>
    </Card>

    <Dialog
      v-model:visible="kayitliFiltreDialog"
      :header="t('denetim.filtreyiKaydet')"
      :modal="true"
      style="width: 380px"
    >
      <FormField
        :label="t('denetim.filtreAdi')"
        :required="true"
      >
        <InputText
          v-model="yeniFiltreAdi"
          :placeholder="t('denetim.filtreAdiPlaceholder')"
          class="w-full"
        />
      </FormField>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="kayitliFiltreDialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :disabled="!yeniFiltreAdi?.trim()"
          @click="filtreKaydet"
        />
      </template>
    </Dialog>

    <Card>
      <template #content>
        <DataTable
          :value="logSatirlari"
          :loading="yukleniyor"
          striped-rows
          :rows="PAGE_SIZE"
          :paginator="true"
          :total-records="toplamKayit"
          lazy
          :first="sayfa * PAGE_SIZE"
          size="small"
          sort-field="tarih"
          :sort-order="-1"
          @page="sayfaDegisti"
        >
          <template #empty>
            <EmptyState :message="t('denetim.empty')" />
          </template>
          <Column
            field="tarih"
            :header="t('common.date')"
            style="width: 150px"
          >
            <template #body="s">
              {{ formatDate(s.data.tarih) }}
            </template>
          </Column>
          <Column
            field="kullaniciAdi"
            :header="t('denetim.kullanici')"
            style="width: 150px"
          >
            <template #body="s">
              {{ s.data.kullaniciAdi || (s.data.kullaniciId ? '#' + s.data.kullaniciId : '-') }}
            </template>
          </Column>
          <Column
            field="islem"
            :header="t('denetim.islem')"
            style="width: 110px"
          >
            <template #body="s">
              <Tag
                :value="islemEtiket(s.data.islem)"
                :severity="islemSeverity(s.data.islem)"
              />
            </template>
          </Column>
          <Column
            field="entityAdi"
            :header="t('denetim.entity')"
            style="width: 110px"
          />
          <Column
            field="entityId"
            :header="t('denetim.entityId')"
            style="width: 90px"
          />
          <Column
            field="aciklama"
            :header="t('common.description')"
          />
          <Column
            field="detay"
            :header="t('denetim.detay')"
          >
            <template #body="s">
              <span
                v-if="s.data.detay"
                class="detay-metin"
                @click="detayGoster(s.data)"
              >
                {{ s.data.kisaDetay }}
              </span>
              <span v-else>-</span>
            </template>
          </Column>
          <Column
            field="ipAdresi"
            :header="t('denetim.ip')"
            style="width: 120px"
          />
        </DataTable>
      </template>
    </Card>

    <Dialog
      v-model:visible="detayDialogAcik"
      :header="t('denetim.islemDetayi')"
      :modal="true"
      style="width: 560px"
    >
      <div class="detay-dialog-icerik">
        <div class="detay-dialog-satir">
          <span class="detay-dialog-etiket">{{ t('denetim.islem') }}</span>
          <strong>{{ islemEtiket(seciliDetay?.islem) }}</strong>
        </div>
        <div class="detay-dialog-satir">
          <span class="detay-dialog-etiket">{{ t('denetim.entity') }}</span>
          <span>{{ seciliDetay?.entityAdi }} #{{ seciliDetay?.entityId }}</span>
        </div>
        <div class="detay-dialog-satir">
          <span class="detay-dialog-etiket">{{ t('denetim.kullanici') }}</span>
          <span>{{ seciliDetay?.kullaniciAdi || (seciliDetay?.kullaniciId ? '#' + seciliDetay.kullaniciId : '-') }}</span>
        </div>
        <div class="detay-dialog-satir">
          <span class="detay-dialog-etiket">{{ t('denetim.ipAdresi') }}</span>
          <span>{{ seciliDetay?.ipAdresi || '-' }}</span>
        </div>
        <div class="detay-dialog-satir">
          <span class="detay-dialog-etiket">{{ t('common.date') }}</span>
          <span>{{ seciliDetay?.tarih ? formatDate(seciliDetay.tarih) : '-' }}</span>
        </div>
        <div
          v-if="seciliDetay?.aciklama"
          class="detay-dialog-satir"
        >
          <span class="detay-dialog-etiket">{{ t('denetim.aciklama') }}</span>
          <span>{{ seciliDetay.aciklama }}</span>
        </div>
        <pre class="detay-json">{{ detayFormatli }}</pre>
      </div>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { auditLogAPI, excelAPI, kullaniciAPI } from '../api/index.js'
import TarihHizliSecim from '../components/TarihHizliSecim.vue'
import FormField from '../components/FormField.vue'
import EmptyState from '../components/EmptyState.vue'
import { useI18n } from 'vue-i18n'
import { formatTarihSaat as formatDate, getLocalDateString } from '../utils/format.js'

const toast = useToast()
const toastBildirim = useToastBildirim()
const { t } = useI18n()

const PAGE_SIZE = 20
const MAKS_KAYITLI_FILTRE = 20

const logs = ref([])
const yukleniyor = ref(false)
const sayfa = ref(0)
const toplamKayit = ref(0)
const islemTipleri = ref([])
const entityListesi = ref([])
const kullanicilar = ref([])
const excelYukleniyor = ref(false)

const filtre = ref({
  islem: null,
  entityAdi: null,
  kullaniciId: null,
  tarihAraligi: []
})

const KAYITLI_ANAHTAR = 'raspel_kayitli_filtreler_denetim'
const kayitliFiltreler = ref([])
const seciliKayitliFiltre = ref(null)
const kayitliFiltreDialog = ref(false)
const yeniFiltreAdi = ref('')

// Tarih araligi degistiginde (hizli secim, ozel aralik veya kayitli filtre
// yuklemesi) listeyi otomatik yenile. ONCEDEN yalnizca iki Select ve
// DatePicker'in `@date-select`'i yeniliyordu; "Bu Ay" gibi hizli secimler
// tabloyu GUNCELLEMIYORDU.
watch(
  () => filtre.value.tarihAraligi,
  () => filtrele()
)

// --- Kayitli filtreler: ISO-guvenli serilestirme ---
// ONCEDEN Date nesneleri JSON.stringify ile ISO string'e donusuyor, geri
// yuklenirken DatePicker'a string geliyor ve aralik bos gorunuyordu.
const isoToDate = (s) => (typeof s === 'string' && s ? new Date(`${s}T00:00:00`) : null)

const seriFiltre = () => ({
  islem: filtre.value.islem || null,
  entityAdi: filtre.value.entityAdi || null,
  kullaniciId: filtre.value.kullaniciId || null,
  tarihAraligi:
    filtre.value.tarihAraligi?.length === 2 && filtre.value.tarihAraligi[0]
      ? [getLocalDateString(filtre.value.tarihAraligi[0]), getLocalDateString(filtre.value.tarihAraligi[1])]
      : []
})

const cozFiltre = (f = {}) => ({
  islem: f.islem || null,
  entityAdi: f.entityAdi || null,
  kullaniciId: f.kullaniciId || null,
  tarihAraligi:
    Array.isArray(f.tarihAraligi) && f.tarihAraligi.length === 2
      ? [isoToDate(f.tarihAraligi[0]), isoToDate(f.tarihAraligi[1])]
      : []
})

const kayitliFiltreleriKaydet = () => {
  try {
    localStorage.setItem(KAYITLI_ANAHTAR, JSON.stringify(kayitliFiltreler.value))
  } catch {
    /* yoksay */
  }
}

const kayitliFiltreleriYukle = () => {
  try {
    kayitliFiltreler.value = JSON.parse(localStorage.getItem(KAYITLI_ANAHTAR) || '[]')
  } catch {
    kayitliFiltreler.value = []
  }
}

const kaydetDialogAc = () => {
  yeniFiltreAdi.value = seciliKayitliFiltre.value?.ad || ''
  kayitliFiltreDialog.value = true
}

const filtreKaydet = () => {
  const ad = yeniFiltreAdi.value.trim()
  if (!ad) return
  const kayit = { ad, filtre: seriFiltre() }
  // Ayni ad varsa guncelle (mukerrer kayit olusmasin).
  const idx = kayitliFiltreler.value.findIndex((f) => f.ad.toLowerCase() === ad.toLowerCase())
  if (idx >= 0) {
    kayitliFiltreler.value[idx] = kayit
  } else {
    kayitliFiltreler.value.push(kayit)
    if (kayitliFiltreler.value.length > MAKS_KAYITLI_FILTRE) kayitliFiltreler.value.shift()
  }
  kayitliFiltreleriKaydet()
  kayitliFiltreDialog.value = false
  yeniFiltreAdi.value = ''
  toast.add({ severity: 'success', summary: t('denetim.kaydedildi'), detail: t('denetim.filtreKaydedildi'), life: 3000 })
}

const kayitliFiltreYukle = () => {
  if (!seciliKayitliFiltre.value) return
  filtre.value = cozFiltre(seciliKayitliFiltre.value.filtre)
  toast.add({ severity: 'info', summary: t('denetim.filtreUygulandi'), detail: seciliKayitliFiltre.value.ad, life: 3000 })
}

const kayitliFiltreSil = () => {
  if (!seciliKayitliFiltre.value) return
  const ad = seciliKayitliFiltre.value.ad
  kayitliFiltreler.value = kayitliFiltreler.value.filter((f) => f.ad !== ad)
  kayitliFiltreleriKaydet()
  seciliKayitliFiltre.value = null
  toast.add({ severity: 'success', summary: t('denetim.filtreSilindi'), life: 3000 })
}

const yukle = async (page = 0) => {
  yukleniyor.value = true
  try {
    const params = { page, size: PAGE_SIZE }
    if (filtre.value.islem) params.islem = filtre.value.islem
    if (filtre.value.entityAdi) params.entityAdi = filtre.value.entityAdi
    if (filtre.value.kullaniciId) params.kullaniciId = filtre.value.kullaniciId
    if (filtre.value.tarihAraligi?.length === 2 && filtre.value.tarihAraligi[0]) {
      params.baslangicTarih = formatISODate(filtre.value.tarihAraligi[0])
      params.bitisTarih = formatISODate(filtre.value.tarihAraligi[1])
    }
    const r = await auditLogAPI.getAll(params)
    logs.value = unwrapList(r)
    toplamKayit.value = r.data?.totalElements || logs.value.length
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('denetim.hataYukleme'))
  } finally {
    yukleniyor.value = false
  }
}

const filtrele = () => {
  sayfa.value = 0
  yukle(0)
}

const excelIndir = async () => {
  excelYukleniyor.value = true
  try {
    const params = {}
    if (filtre.value.islem) params.islem = filtre.value.islem
    if (filtre.value.entityAdi) params.entityAdi = filtre.value.entityAdi
    if (filtre.value.kullaniciId) params.kullaniciId = filtre.value.kullaniciId
    if (filtre.value.tarihAraligi?.length === 2 && filtre.value.tarihAraligi[0]) {
      params.baslangicTarih = formatISODate(filtre.value.tarihAraligi[0])
      params.bitisTarih = formatISODate(filtre.value.tarihAraligi[1])
    }
    const res = await excelAPI.denetimLog(params)
    const url = window.URL.createObjectURL(
      new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    )
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `denetim-log-${getLocalDateString()}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch {
    toastBildirim.hata(t('denetim.hataExcel'))
  } finally {
    excelYukleniyor.value = false
  }
}

const filtreTemizle = () => {
  filtre.value = { islem: null, entityAdi: null, kullaniciId: null, tarihAraligi: [] }
  seciliKayitliFiltre.value = null
}
const sayfaDegisti = (e) => {
  sayfa.value = e.page
  yukle(e.page)
}

const formatISODate = (d) => {
  if (!d) return null
  return getLocalDateString(d)
}

// Islem kodlarini kullaniciya cevirir (OLUSTUR/SIL/... ham gorunmesin).
const islemEtiketleri = computed(() => ({
  OLUSTUR: t('denetim.islemOlustur'),
  GUNCELLE: t('denetim.islemGuncelle'),
  SIL: t('denetim.islemSil'),
  ISLEM: t('denetim.islemIslem'),
  HATA: t('denetim.islemHata')
}))
const islemEtiket = (v) => islemEtiketleri.value[v] || v || '-'

const islemSeverity = (islem) => {
  if (islem === 'SIL' || islem === 'HATA') return 'danger'
  if (islem === 'OLUSTUR') return 'success'
  if (islem === 'GUNCELLE') return 'warn'
  return 'info'
}

const kisaDetay = (detay) => {
  if (!detay) return '-'
  try {
    const obj = typeof detay === 'string' ? JSON.parse(detay) : detay
    const s = JSON.stringify(obj)
    return s.length > 60 ? s.slice(0, 60) + '…' : s
  } catch {
    return detay.length > 60 ? detay.slice(0, 60) + '…' : detay
  }
}

const logSatirlari = computed(() => logs.value.map((l) => ({ ...l, kisaDetay: kisaDetay(l.detay) })))

const detayDialogAcik = ref(false)
const seciliDetay = ref(null)
const detayFormatli = ref('')

const detayGoster = (log) => {
  seciliDetay.value = log
  try {
    const obj = typeof log.detay === 'string' ? JSON.parse(log.detay) : log.detay
    detayFormatli.value = JSON.stringify(obj, null, 2)
  } catch {
    detayFormatli.value = log.detay || '-'
  }
  detayDialogAcik.value = true
}

const filtreSecenekleriniYukle = async () => {
  try {
    const [islemRes, entityRes, kullaniciRes] = await Promise.all([
      auditLogAPI.getIslemTipleri(),
      auditLogAPI.getEntityListesi(),
      kullaniciAPI.getAll({ page: 0, size: 200 })
    ])
    islemTipleri.value = islemRes.data || []
    entityListesi.value = entityRes.data || []
    kullanicilar.value = (unwrapList(kullaniciRes) || []).map((u) => ({
      id: u.id,
      ad: u.displayName || u.username || `#${u.id}`
    }))
  } catch {
    /* empty */
  }
}

onMounted(() => {
  filtreSecenekleriniYukle()
  kayitliFiltreleriYukle()
  yukle()
})
</script>

<style scoped>
.denetim-page {
  padding: 0;
  max-width: 100%;
}
.filtre-karti {
  margin-bottom: 1rem;
}
.filtre-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(180px, 100%), 1fr));
  gap: 12px;
  align-items: end;
}
.filtre-alan label {
  display: block;
  margin-bottom: 4px;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-secondary);
}
.filtre-aksiyon {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  flex-wrap: wrap;
}
.w-full {
  width: 100% !important;
}
.kayitli-filtre {
  min-width: min(170px, 100%) !important;
}
.detay-metin {
  font-family: monospace;
  font-size: 0.75rem;
  color: var(--text-secondary);
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
  display: inline-block;
}
.detay-metin:hover {
  color: var(--primary-color, var(--accent));
  text-decoration: underline;
}
.detay-dialog-icerik {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.detay-dialog-satir {
  display: flex;
  gap: 12px;
  font-size: 0.9rem;
}
.detay-dialog-etiket {
  min-width: 90px;
  color: var(--text-muted);
  font-weight: 600;
}
.detay-json {
  background: rgba(0, 0, 0, 0.05);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 12px;
  font-size: 0.8rem;
  font-family: monospace;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 320px;
  overflow: auto;
  margin: 0;
}
</style>
