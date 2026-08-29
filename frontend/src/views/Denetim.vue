<template>
  <div class="denetim-page">
    <PageHeader
      title="Denetim Log"
      subtitle="Sistemdeki tüm işlem kayıtlarını görüntüleyin ve filtreleyin."
    >
      <template #actions>
        <Button
          label="Excel"
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
            <label>İşlem Türü</label>
            <Select
              v-model="filtre.islem"
              :options="islemTipleri"
              placeholder="Tümü"
              class="w-full"
              allow-clear
              clear-icon="pi pi-times"
              @change="filtrele"
            />
          </div>
          <div class="filtre-alan">
            <label>Entity</label>
            <Select
              v-model="filtre.entityAdi"
              :options="entityListesi"
              placeholder="Tümü"
              class="w-full"
              allow-clear
              clear-icon="pi pi-times"
              @change="filtrele"
            />
          </div>
          <div class="filtre-alan">
            <label>Tarih Aralığı</label>
            <TarihHizliSecim v-model="filtre.tarihAraligi" />
          </div>
          <div
            v-if="filtre.tarihAraligi?.length === 2"
            class="filtre-alan"
          >
            <label>Özel Tarih Aralığı</label>
            <DatePicker
              v-model="filtre.tarihAraligi"
              selection-mode="range"
              date-format="dd/mm/yy"
              placeholder="Başlangıç - Bitiş"
              class="w-full"
              @date-select="filtrele"
            />
          </div>
          <div class="filtre-aksiyon">
            <Button
              label="Filtre Kaydet"
              icon="pi pi-bookmark"
              class="p-button-sm p-button-text"
              @click="kayitliFiltreDialog = true"
            />
            <Dropdown
              v-model="seciliKayitliFiltre"
              :options="kayitliFiltreler"
              option-label="ad"
              placeholder="Kayıtlı Filtreler"
              class="kayitli-filtre"
              @change="kayitliFiltreYukle"
            />
            <Button
              label="Temizle"
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
      header="Filtreyi Kaydet"
      :modal="true"
      style="width: 380px"
    >
      <FormField
        label="Filtre Adı"
        :required="true"
      >
        <InputText
          v-model="yeniFiltreAdi"
          placeholder="Örn: Son 3 ay KESILDI faturalar"
          class="w-full"
        />
      </FormField>
      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="kayitliFiltreDialog = false"
        />
        <Button
          label="Kaydet"
          icon="pi pi-check"
          :disabled="!yeniFiltreAdi?.trim()"
          @click="filtreKaydet"
        />
      </template>
    </Dialog>

    <Card>
      <template #content>
        <DataTable
          state-storage="session"
          state-key="denetim-table-state"
          :value="logs"
          :loading="yukleniyor"
          striped-rows
          :rows="20"
          :paginator="true"
          :total-records="toplamKayit"
          lazy
          :first="sayfa * 20"
          size="small"
          sort-field="tarih"
          :sort-order="-1"
          @page="sayfaDegisti"
        >
          <Column
            field="tarih"
            header="Tarih"
            style="width: 150px"
          >
            <template #body="s">
              {{ formatDate(s.data.tarih) }}
            </template>
          </Column>
          <Column
            field="kullaniciId"
            header="Kullanıcı ID"
            style="width: 100px"
          />
          <Column
            field="islem"
            header="İşlem"
            style="width: 100px"
          >
            <template #body="s">
              <Tag
                :value="s.data.islem"
                :severity="islemSeverity(s.data.islem)"
              />
            </template>
          </Column>
          <Column
            field="entityAdi"
            header="Entity"
            style="width: 110px"
          />
          <Column
            field="entityId"
            header="Entity ID"
            style="width: 90px"
          />
          <Column
            field="aciklama"
            header="Açıklama"
          />
          <Column
            field="detay"
            header="Detay"
          >
            <template #body="s">
              <span
                v-if="s.data.detay"
                class="detay-metin"
                @click="detayGoster(s.data)"
              >
                {{ kisaDetay(s.data.detay) }}
              </span>
              <span v-else>-</span>
            </template>
          </Column>
          <Column
            field="ipAdresi"
            header="IP"
            style="width: 120px"
          />
        </DataTable>
        <div
          v-if="(!logs || !logs.length) && !yukleniyor"
          class="empty-state"
        >
          Henüz denetim kaydı bulunamadı.
        </div>
      </template>
    </Card>

    <Dialog
      v-model:visible="detayDialogAcik"
      header="İşlem Detayı"
      :modal="true"
      style="width: 560px"
    >
      <div class="detay-dialog-icerik">
        <div class="detay-dialog-satir">
          <span class="detay-dialog-etiket">İşlem</span>
          <strong>{{ seciliDetay?.islem }}</strong>
        </div>
        <div class="detay-dialog-satir">
          <span class="detay-dialog-etiket">Entity</span>
          <span>{{ seciliDetay?.entityAdi }} #{{ seciliDetay?.entityId }}</span>
        </div>
        <div class="detay-dialog-satir">
          <span class="detay-dialog-etiket">Tarih</span>
          <span>{{ seciliDetay?.tarih ? formatDate(seciliDetay.tarih) : '-' }}</span>
        </div>
        <pre class="detay-json">{{ detayFormatli }}</pre>
      </div>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { auditLogAPI, excelAPI } from '../api/index.js'
import TarihHizliSecim from '../components/TarihHizliSecim.vue'
import FormField from '../components/FormField.vue'

const toast = useToast()
const toastBildirim = useToastBildirim()
const logs = ref([])
const yukleniyor = ref(false)
const sayfa = ref(0)
const toplamKayit = ref(0)
const islemTipleri = ref([])
const entityListesi = ref([])
const excelYukleniyor = ref(false)

const filtre = ref({
  islem: null,
  entityAdi: null,
  tarihAraligi: []
})

const KAYITLI_ANAHTAR = 'raspel_kayitli_filtreler_denetim'
const kayitliFiltreler = ref([])
const seciliKayitliFiltre = ref(null)
const kayitliFiltreDialog = ref(false)
const yeniFiltreAdi = ref('')

const kayitliFiltreleriYukle = () => {
  try {
    kayitliFiltreler.value = JSON.parse(localStorage.getItem(KAYITLI_ANAHTAR) || '[]')
  } catch {
    kayitliFiltreler.value = []
  }
}

const filtreKaydet = () => {
  const kayit = { ad: yeniFiltreAdi.value.trim(), filtre: JSON.parse(JSON.stringify(filtre.value)) }
  kayitliFiltreler.value.push(kayit)
  localStorage.setItem(KAYITLI_ANAHTAR, JSON.stringify(kayitliFiltreler.value))
  kayitliFiltreDialog.value = false
  yeniFiltreAdi.value = ''
  toast.add({ severity: 'success', summary: 'Kaydedildi', detail: 'Filtre kaydedildi.', life: 3000 })
}

const kayitliFiltreYukle = () => {
  if (!seciliKayitliFiltre.value) return
  filtre.value = JSON.parse(JSON.stringify(seciliKayitliFiltre.value.filtre))
  filtrele()
  toast.add({ severity: 'info', summary: 'Filtre Uygulandı', detail: seciliKayitliFiltre.value.ad, life: 3000 })
}

const yukle = async (page = 0) => {
  yukleniyor.value = true
  try {
    const params = { page, size: 20 }
    if (filtre.value.islem) params.islem = filtre.value.islem
    if (filtre.value.entityAdi) params.entityAdi = filtre.value.entityAdi
    if (filtre.value.tarihAraligi?.length === 2 && filtre.value.tarihAraligi[0]) {
      params.baslangicTarih = formatISODate(filtre.value.tarihAraligi[0])
      params.bitisTarih = formatISODate(filtre.value.tarihAraligi[1])
    }
    const r = await auditLogAPI.getAll(params)
    logs.value = r.data?.content || r.data || []
    toplamKayit.value = r.data?.totalElements || logs.value.length
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || 'Denetim kayıtları yüklenirken hata oluştu')
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
    link.setAttribute('download', `denetim-log-${new Date().toISOString().split('T')[0]}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch {
    toastBildirim.hata('Excel indirilemedi')
  } finally {
    excelYukleniyor.value = false
  }
}
const filtreTemizle = () => {
  filtre.value = { islem: null, entityAdi: null, tarihAraligi: [] }
  filtrele()
}
const sayfaDegisti = (e) => {
  sayfa.value = e.page
  yukle(e.page)
}

const formatDate = (d) => {
  if (!d) return '-'
  return new Date(d).toLocaleString('tr-TR')
}
const formatISODate = (d) => {
  if (!d) return null
  const dt = new Date(d)
  return dt.toISOString().split('T')[0]
}
const islemSeverity = (islem) => {
  if (islem === 'SIL') return 'danger'
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
    const [islemRes, entityRes] = await Promise.all([auditLogAPI.getIslemTipleri(), auditLogAPI.getEntityListesi()])
    islemTipleri.value = islemRes.data || []
    entityListesi.value = entityRes.data || []
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
  padding: 1.5rem;
}
.filtre-karti {
  margin-bottom: 1rem;
}
.filtre-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
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
}
.w-full {
  width: 100% !important;
}
.kayitli-filtre {
  min-width: 170px !important;
}
.empty-state {
  text-align: center;
  padding: 2rem;
  color: var(--text-muted);
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
  color: var(--primary-color, #3b82f6);
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
