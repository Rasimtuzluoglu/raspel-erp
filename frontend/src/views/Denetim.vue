<template>
  <div class="denetim-page">
    <PageHeader title="Denetim Log" subtitle="Sistemdeki tüm işlem kayıtlarını görüntüleyin ve filtreleyin.">
      <template #actions>
        <Button label="Excel" icon="pi pi-file-excel" class="p-button-sm p-button-outlined" @click="excelIndir" :loading="excelYukleniyor" />
      </template>
    </PageHeader>

    <Card class="filtre-karti">
      <template #content>
        <div class="filtre-grid">
          <div class="filtre-alan">
            <label>İşlem Türü</label>
            <Select v-model="filtre.islem" :options="islemTipleri" placeholder="Tümü" class="w-full" allowClear clearIcon="pi pi-times" @change="filtrele" />
          </div>
          <div class="filtre-alan">
            <label>Entity</label>
            <Select v-model="filtre.entityAdi" :options="entityListesi" placeholder="Tümü" class="w-full" allowClear clearIcon="pi pi-times" @change="filtrele" />
          </div>
          <div class="filtre-alan">
            <label>Tarih Aralığı</label>
            <TarihHizliSecim v-model="filtre.tarihAraligi" />
          </div>
          <div class="filtre-alan" v-if="filtre.tarihAraligi?.length === 2">
            <label>Özel Tarih Aralığı</label>
            <DatePicker v-model="filtre.tarihAraligi" selectionMode="range" dateFormat="dd/mm/yy" placeholder="Başlangıç - Bitiş" class="w-full" @date-select="filtrele" />
          </div>
          <div class="filtre-aksiyon">
            <Button label="Filtre Kaydet" icon="pi pi-bookmark" class="p-button-sm p-button-text" @click="kayitliFiltreDialog = true" />
            <Dropdown v-model="seciliKayitliFiltre" :options="kayitliFiltreler" optionLabel="ad" placeholder="Kayıtlı Filtreler" class="kayitli-filtre" @change="kayitliFiltreYukle" />
            <Button label="Temizle" icon="pi pi-filter-slash" class="p-button-sm p-button-text" @click="filtreTemizle" />
          </div>
        </div>
      </template>
    </Card>

    <Dialog v-model:visible="kayitliFiltreDialog" header="Filtreyi Kaydet" :modal="true" style="width: 380px">
      <FormField label="Filtre Adı" :required="true">
        <InputText v-model="yeniFiltreAdi" placeholder="Örn: Son 3 ay KESILDI faturalar" class="w-full" />
      </FormField>
      <template #footer>
        <Button label="İptal" icon="pi pi-times" @click="kayitliFiltreDialog = false" class="p-button-text" />
        <Button label="Kaydet" icon="pi pi-check" @click="filtreKaydet" :disabled="!yeniFiltreAdi?.trim()" />
      </template>
    </Dialog>

    <Card>
      <template #content>
        <DataTable :value="logs" :loading="yukleniyor" stripedRows :rows="20" :paginator="true" :totalRecords="toplamKayit" lazy :first="sayfa * 20" @page="sayfaDegisti" size="small" sortField="tarih" :sortOrder="-1">
          <Column field="tarih" header="Tarih" style="width: 150px">
            <template #body="s">{{ formatDate(s.data.tarih) }}</template>
          </Column>
          <Column field="kullaniciId" header="Kullanıcı ID" style="width: 100px" />
          <Column field="islem" header="İşlem" style="width: 100px">
            <template #body="s">
              <Tag :value="s.data.islem" :severity="islemSeverity(s.data.islem)" />
            </template>
          </Column>
          <Column field="entityAdi" header="Entity" style="width: 110px" />
          <Column field="entityId" header="Entity ID" style="width: 90px" />
          <Column field="aciklama" header="Açıklama" />
          <Column field="ipAdresi" header="IP" style="width: 120px" />
        </DataTable>
        <div v-if="!logs.length && !yukleniyor" class="empty-state">Henüz denetim kaydı bulunamadı.</div>
      </template>
    </Card>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useToast } from 'primevue/usetoast'
import { auditLogAPI, excelAPI } from '../api/index.js'
import axios from 'axios'
import TarihHizliSecim from '../components/TarihHizliSecim.vue'
import FormField from '../components/FormField.vue'

const toast = useToast()
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
  try { kayitliFiltreler.value = JSON.parse(localStorage.getItem(KAYITLI_ANAHTAR) || '[]') } catch { kayitliFiltreler.value = [] }
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
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || err?.message || 'Denetim kayıtları yüklenirken hata oluştu', life: 5000 })
  } finally {
    yukleniyor.value = false
  }
}

const filtrele = () => { sayfa.value = 0; yukle(0) }

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
    const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `denetim-log-${new Date().toISOString().split('T')[0]}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch {
    toast.add({ severity: 'error', summary: 'Hata', detail: 'Excel indirilemedi', life: 5000 })
  } finally {
    excelYukleniyor.value = false
  }
}
const filtreTemizle = () => {
  filtre.value = { islem: null, entityAdi: null, tarihAraligi: [] }
  filtrele()
}
const sayfaDegisti = (e) => { sayfa.value = e.page; yukle(e.page) }

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

const filtreSecenekleriniYukle = async () => {
  try {
    const [islemRes, entityRes] = await Promise.all([
      auditLogAPI.getIslemTipleri(),
      auditLogAPI.getEntityListesi()
    ])
    islemTipleri.value = islemRes.data || []
    entityListesi.value = entityRes.data || []
  } catch {}
}

onMounted(() => {
  filtreSecenekleriniYukle()
  kayitliFiltreleriYukle()
  yukle()
})
</script>

<style scoped>
.denetim-page { padding: 1.5rem; }
.filtre-karti { margin-bottom: 1rem; }
.filtre-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; align-items: end; }
.filtre-alan label { display: block; margin-bottom: 4px; font-size: 0.8rem; font-weight: 600; color: var(--text-secondary); }
.filtre-aksiyon { display: flex; align-items: flex-end; }
.w-full { width: 100% !important; }
.kayitli-filtre { min-width: 170px !important; }
.empty-state { text-align: center; padding: 2rem; color: var(--text-muted); }
</style>
