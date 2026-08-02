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
            <label>Başlangıç Tarihi</label>
            <DatePicker v-model="filtre.baslangicTarih" dateFormat="dd/mm/yy" placeholder="Seçiniz" class="w-full" @date-select="filtrele" />
          </div>
          <div class="filtre-alan">
            <label>Bitiş Tarihi</label>
            <DatePicker v-model="filtre.bitisTarih" dateFormat="dd/mm/yy" placeholder="Seçiniz" class="w-full" @date-select="filtrele" />
          </div>
          <div class="filtre-aksiyon">
            <Button label="Temizle" icon="pi pi-filter-slash" class="p-button-sm p-button-text" @click="filtreTemizle" />
          </div>
        </div>
      </template>
    </Card>

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
  baslangicTarih: null,
  bitisTarih: null
})

const yukle = async (page = 0) => {
  yukleniyor.value = true
  try {
    const params = { page, size: 20 }
    if (filtre.value.islem) params.islem = filtre.value.islem
    if (filtre.value.entityAdi) params.entityAdi = filtre.value.entityAdi
    if (filtre.value.baslangicTarih) params.baslangicTarih = formatISODate(filtre.value.baslangicTarih)
    if (filtre.value.bitisTarih) params.bitisTarih = formatISODate(filtre.value.bitisTarih)
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
    if (filtre.value.baslangicTarih) params.baslangicTarih = formatISODate(filtre.value.baslangicTarih)
    if (filtre.value.bitisTarih) params.bitisTarih = formatISODate(filtre.value.bitisTarih)
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
  filtre.value = { islem: null, entityAdi: null, baslangicTarih: null, bitisTarih: null }
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
.empty-state { text-align: center; padding: 2rem; color: var(--text-muted); }
</style>
