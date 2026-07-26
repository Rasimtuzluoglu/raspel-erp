<template>
  <div class="yedekler-container">
    <div class="page-header">
      <h1><i class="pi pi-save"></i> Yedekleme</h1>
      <div class="header-islem">
        <Select v-model="yedekTipi" :options="tipler" optionLabel="label" optionValue="value" class="tip-select" />
        <Button label="Yedek Al" icon="pi pi-plus-circle" class="p-button-success" @click="manuelYedek" :loading="yedekAliniyor" />
      </div>
    </div>

    <Message v-if="hata" severity="error" :closable="true" @close="hata = ''">{{ hata }}</Message>
    <Message v-if="basari" severity="success" :closable="true" @close="basari = ''">{{ basari }}</Message>

    <div class="ozet-grid">
      <Card class="ozet-kart">
        <template #title><i class="pi pi-calendar" style="margin-right:8px"></i>Günlük</template>
        <template #content>
          <div class="ozet-satir"><span>Adet</span><strong>{{ schedule.counts?.DAILY || 0 }}</strong></div>
          <div class="ozet-satir"><span>Saklama</span><strong>30 gün</strong></div>
          <div class="ozet-saat"><i class="pi pi-clock"></i> Her gün 03:00</div>
        </template>
      </Card>
      <Card class="ozet-kart">
        <template #title><i class="pi pi-calendar-week" style="margin-right:8px"></i>Haftalık</template>
        <template #content>
          <div class="ozet-satir"><span>Adet</span><strong>{{ schedule.counts?.WEEKLY || 0 }}</strong></div>
          <div class="ozet-satir"><span>Saklama</span><strong>180 gün</strong></div>
          <div class="ozet-saat"><i class="pi pi-clock"></i> Pazar 03:00</div>
        </template>
      </Card>
      <Card class="ozet-kart">
        <template #title><i class="pi pi-calendar-plus" style="margin-right:8px"></i>Aylık</template>
        <template #content>
          <div class="ozet-satir"><span>Adet</span><strong>{{ schedule.counts?.MONTHLY || 0 }}</strong></div>
          <div class="ozet-satir"><span>Saklama</span><strong>365 gün</strong></div>
          <div class="ozet-saat"><i class="pi pi-clock"></i> Ayın 1'i 03:00</div>
        </template>
      </Card>
      <Card class="ozet-kart">
        <template #title><i class="pi pi-calendar-star" style="margin-right:8px"></i>Yıllık</template>
        <template #content>
          <div class="ozet-satir"><span>Adet</span><strong>{{ schedule.counts?.YEARLY || 0 }}</strong></div>
          <div class="ozet-satir"><span>Saklama</span><strong>Sınırsız</strong></div>
          <div class="ozet-saat"><i class="pi pi-clock"></i> 1 Ocak 03:00</div>
        </template>
      </Card>
    </div>

    <Card class="yedek-listesi">
      <template #title><i class="pi pi-list" style="margin-right:8px"></i>Yedek Dosyaları</template>
      <template #content>
        <DataTable :value="yedekler" :loading="yedeklerYukleniyor" stripedRows size="small" :rows="10" :paginator="yedekler.length > 10" sortField="lastModified" :sortOrder="-1">
          <Column field="filename" header="Dosya Adı" sortable>
            <template #body="s"><i class="pi pi-file-archive" style="margin-right:8px;color:#3b82f6"></i>{{ s.data.filename }}</template>
          </Column>
          <Column field="type" header="Tür" sortable style="width:100px">
            <template #body="s">
              <Tag :value="typeLabel(s.data.type)" :severity="typeSeverity(s.data.type)" />
            </template>
          </Column>
          <Column field="size" header="Boyut" sortable style="width:100px">
            <template #body="s">{{ formatSize(s.data.size) }}</template>
          </Column>
          <Column field="lastModified" header="Tarih" sortable style="width:170px">
            <template #body="s">{{ formatDate(s.data.lastModified) }}</template>
          </Column>
          <Column header="İşlem" style="width:100px">
            <template #body="s">
              <Button icon="pi pi-download" class="p-button-sm p-button-text" @click="indir(s.data.filename)" title="İndir" />
              <Button icon="pi pi-trash" class="p-button-sm p-button-text p-button-danger" @click="sil(s.data.filename)" title="Sil" />
            </template>
          </Column>
        </DataTable>
        <div v-if="!yedekler.length && !yedeklerYukleniyor" class="empty-state">Henüz yedek alınmamış</div>
      </template>
    </Card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { backupAPI } from '../api/index.js'

const tipler = [
  { value: 'DAILY', label: 'Günlük' },
  { value: 'WEEKLY', label: 'Haftalık' },
  { value: 'MONTHLY', label: 'Aylık' },
  { value: 'YEARLY', label: 'Yıllık' }
]

const yedekTipi = ref('DAILY')
const yedekler = ref([])
const yedeklerYukleniyor = ref(false)
const yedekAliniyor = ref(false)
const hata = ref('')
const basari = ref('')
const schedule = ref({})

const typeLabel = (t) => tipler.find(i => i.value === t)?.label || t
const typeSeverity = (t) => {
  if (t === 'DAILY') return 'info'
  if (t === 'WEEKLY') return 'warn'
  if (t === 'MONTHLY') return 'success'
  if (t === 'YEARLY') return 'danger'
  return 'info'
}

const yukle = async () => {
  yedeklerYukleniyor.value = true
  try {
    const [yedekRes, scheduleRes] = await Promise.all([
      backupAPI.list(),
      backupAPI.getSchedule()
    ])
    yedekler.value = yedekRes.data || []
    schedule.value = scheduleRes.data || {}
  } catch (err) {
    hata.value = 'Yedekler yüklenirken hata oluştu'
  } finally {
    yedeklerYukleniyor.value = false
  }
}

const manuelYedek = async () => {
  yedekAliniyor.value = true
  hata.value = ''
  basari.value = ''
  try {
    const res = await backupAPI.manual(yedekTipi.value)
    basari.value = res.data.message || 'Yedek başarıyla alındı'
    await yukle()
  } catch (err) {
    hata.value = err.response?.data?.message || 'Yedekleme başarısız'
  } finally {
    yedekAliniyor.value = false
  }
}

const indir = (filename) => {
  backupAPI.download(filename).then(res => {
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', filename)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  }).catch(() => {
    hata.value = 'Dosya indirilemedi'
  })
}

const sil = async (filename) => {
  if (!confirm(`"${filename}" dosyasını silmek istediğinize emin misiniz?`)) return
  try {
    await backupAPI.delete(filename)
    basari.value = `"${filename}" silindi`
    await yukle()
  } catch (err) {
    hata.value = err.response?.data?.message || 'Silme başarısız'
  }
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return size.toFixed(1) + ' ' + units[i]
}

const formatDate = (d) => {
  if (!d) return '—'
  return new Date(d).toLocaleString('tr-TR')
}

onMounted(yukle)
</script>

<style scoped>
.yedekler-container { padding: 0; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; flex-wrap: wrap; gap: 12px; }
.page-header h1 { margin: 0; font-size: 24px; display: flex; align-items: center; gap: 10px; }
.header-islem { display: flex; align-items: center; gap: 10px; }
.tip-select { width: 140px; }
.tip-select :deep(.p-dropdown) { min-height: 40px; }

.ozet-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 24px; }
.ozet-kart .p-card-title { font-size: 14px !important; }
.ozet-satir { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; border-bottom: 1px solid var(--border); font-size: 13px; }
.ozet-satir:last-of-type { border-bottom: none; }
.ozet-satir strong { font-weight: 600; }
.ozet-saat { font-size: 11px; color: var(--text-muted); margin-top: 6px; }
.ozet-saat i { margin-right: 4px; }

.empty-state { text-align: center; padding: 40px; color: var(--text-muted); font-size: 14px; }

@media (max-width: 900px) {
  .ozet-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 500px) {
  .ozet-grid { grid-template-columns: 1fr; }
}
</style>
