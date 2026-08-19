<template>
  <div class="yedekler-container">
    <div class="page-header">
      <h1><i class="pi pi-save" /> Yedekleme</h1>
      <div class="header-islem">
        <Select
          v-model="yedekTipi"
          :options="tipler"
          option-label="label"
          option-value="value"
          class="tip-select"
        />
        <Button
          label="Yedek Al"
          icon="pi pi-plus-circle"
          class="p-button-success"
          :loading="yedekAliniyor"
          @click="manuelYedek"
        />
      </div>
    </div>

    <ConfirmDialog />

    <Message
      v-if="hata"
      severity="error"
      :closable="true"
      @close="hata = ''"
    >
      {{ hata }}
    </Message>
    <Message
      v-if="basari"
      severity="success"
      :closable="true"
      @close="basari = ''"
    >
      {{ basari }}
    </Message>

    <div class="ozet-grid">
      <Card class="ozet-kart">
        <template #title>
          <i
            class="pi pi-calendar"
            style="margin-right: 8px"
          />Günlük
        </template>
        <template #content>
          <div class="ozet-satir">
            <span>Adet</span><strong>{{ schedule.counts?.DAILY || 0 }}</strong>
          </div>
          <div class="ozet-satir">
            <span>Saklama</span><strong>30 gün</strong>
          </div>
          <div class="ozet-saat">
            <i class="pi pi-clock" /> Her gün 03:00
          </div>
        </template>
      </Card>
      <Card class="ozet-kart">
        <template #title>
          <i
            class="pi pi-calendar-week"
            style="margin-right: 8px"
          />Haftalık
        </template>
        <template #content>
          <div class="ozet-satir">
            <span>Adet</span><strong>{{ schedule.counts?.WEEKLY || 0 }}</strong>
          </div>
          <div class="ozet-satir">
            <span>Saklama</span><strong>180 gün</strong>
          </div>
          <div class="ozet-saat">
            <i class="pi pi-clock" /> Pazar 03:00
          </div>
        </template>
      </Card>
      <Card class="ozet-kart">
        <template #title>
          <i
            class="pi pi-calendar-plus"
            style="margin-right: 8px"
          />Aylık
        </template>
        <template #content>
          <div class="ozet-satir">
            <span>Adet</span><strong>{{ schedule.counts?.MONTHLY || 0 }}</strong>
          </div>
          <div class="ozet-satir">
            <span>Saklama</span><strong>365 gün</strong>
          </div>
          <div class="ozet-saat">
            <i class="pi pi-clock" /> Ayın 1'i 03:00
          </div>
        </template>
      </Card>
      <Card class="ozet-kart">
        <template #title>
          <i
            class="pi pi-calendar-star"
            style="margin-right: 8px"
          />Yıllık
        </template>
        <template #content>
          <div class="ozet-satir">
            <span>Adet</span><strong>{{ schedule.counts?.YEARLY || 0 }}</strong>
          </div>
          <div class="ozet-satir">
            <span>Saklama</span><strong>Sınırsız</strong>
          </div>
          <!-- Bulut Yedekleme (Cloud Storage) Kartı -->
          <Card class="bulut-kart">
            <template #title>
              <div class="bulut-baslik">
                <span><i
                  class="pi pi-cloud"
                  style="margin-right: 8px; color: #3b82f6"
                />Bulut Yedekleme & Şifreleme</span>
                <Tag
                  :value="cloudConfig.encryptionEnabled ? 'AES-256 ŞİFRELİ' : 'ŞİFRESİZ'"
                  :severity="cloudConfig.encryptionEnabled ? 'success' : 'warn'"
                />
              </div>
            </template>
            <template #content>
              <div class="bulut-grid">
                <div class="field">
                  <label>Bulut Sağlayıcı</label>
                  <Select
                    v-model="cloudConfig.provider"
                    :options="[
                      { label: 'Amazon AWS S3', value: 'AWS_S3' },
                      { label: 'Google Drive', value: 'GOOGLE_DRIVE' },
                      { label: 'Dropbox Business', value: 'DROPBOX' }
                    ]"
                    option-label="label"
                    option-value="value"
                    class="w-full"
                  />
                </div>
                <div class="field">
                  <label>Hedef Dizin / Bucket</label>
                  <InputText
                    v-model="cloudConfig.bucketName"
                    placeholder="Örn: s3://raspel-erp-backups"
                    class="w-full"
                  />
                </div>
                <div class="field">
                  <label>Bölge (Region)</label>
                  <InputText
                    v-model="cloudConfig.region"
                    placeholder="Örn: eu-central-1"
                    class="w-full"
                  />
                </div>
                <div class="bulut-aksiyonlar">
                  <Button
                    label="Bulut Ayarlarını Kaydet"
                    icon="pi pi-save"
                    size="small"
                    class="p-button-outlined"
                    :loading="cloudKaydediliyor"
                    @click="bulutAyarlariKaydet"
                  />
                  <Button
                    label="Buluta Senkronize Et"
                    icon="pi pi-cloud-upload"
                    size="small"
                    class="p-button-primary"
                    :loading="cloudSenkronizeEdiliyor"
                    @click="bulutaEsitle(null)"
                  />
                </div>
              </div>
            </template>
          </Card>

          <Card class="yedek-listesi">
            <template #title>
              <i
                class="pi pi-list"
                style="margin-right: 8px"
              />Yedek Dosyaları
            </template>
            <template #content>
              <DataTable
                state-storage="session"
                state-key="yedekler-table-state"
                :value="yedekler"
                :loading="yedeklerYukleniyor"
                striped-rows
                size="small"
                :rows="10"
                :paginator="yedekler.length > 10"
                sort-field="lastModified"
                :sort-order="-1"
              >
                <Column
                  field="filename"
                  header="Dosya Adı"
                  sortable
                >
                  <template #body="s">
                    <i
                      class="pi pi-file-archive"
                      style="margin-right: 8px; color: #3b82f6"
                    />{{ s.data.filename }}
                  </template>
                </Column>
                <Column
                  field="type"
                  header="Tür"
                  sortable
                  style="width: 100px"
                >
                  <template #body="s">
                    <Tag
                      :value="typeLabel(s.data.type)"
                      :severity="typeSeverity(s.data.type)"
                    />
                  </template>
                </Column>
                <Column
                  field="size"
                  header="Boyut"
                  sortable
                  style="width: 100px"
                >
                  <template #body="s">
                    {{ formatSize(s.data.size) }}
                  </template>
                </Column>
                <Column
                  field="lastModified"
                  header="Tarih"
                  sortable
                  style="width: 170px"
                >
                  <template #body="s">
                    {{ formatDate(s.data.lastModified) }}
                  </template>
                </Column>
                <Column
                  header="İşlem"
                  style="width: 100px"
                >
                  <template #body="s">
                    <Button
                      icon="pi pi-download"
                      class="p-button-sm p-button-text"
                      title="İndir"
                      @click="indir(s.data.filename)"
                    />
                    <Button
                      icon="pi pi-upload"
                      class="p-button-sm p-button-text p-button-warning"
                      title="Geri Yükle"
                      @click="geriYukle(s.data.filename)"
                    />
                    <Button
                      icon="pi pi-trash"
                      class="p-button-sm p-button-text p-button-danger"
                      title="Sil"
                      @click="sil(s.data.filename)"
                    />
                  </template>
                </Column>
              </DataTable>
              <div
                v-if="!yedekler.length && !yedeklerYukleniyor"
                class="empty-state"
              >
                Henüz yedek alınmamış
              </div>
            </template>
          </Card>
        </template>
      </card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import { backupAPI } from '../api/index.js'

const confirm = useConfirm()

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

const typeLabel = (t) => tipler.find((i) => i.value === t)?.label || t
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
    const [yedekRes, scheduleRes] = await Promise.all([backupAPI.list(), backupAPI.getSchedule()])
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
  backupAPI
    .download(filename)
    .then((res) => {
      const url = window.URL.createObjectURL(new Blob([res.data]))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', filename)
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
    })
    .catch(() => {
      hata.value = 'Dosya indirilemedi'
    })
}

const sil = (filename) => {
  confirm.require({
    message: `"${filename}" dosyasını silmek istediğinize emin misiniz?`,
    header: 'Yedek Sil',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet, Sil',
    rejectLabel: 'İptal',
    accept: async () => {
      try {
        await backupAPI.delete(filename)
        basari.value = `"${filename}" silindi`
        await yukle()
      } catch (err) {
        hata.value = err.response?.data?.message || 'Silme başarısız'
      }
    }
  })
}

const geriYukle = (filename) => {
  confirm.require({
    message: `"${filename}" yedeğinden geri yükleme yapılacak. Mevcut veriler bu yedekteki verilerle DEĞİŞTİRİLECEK. Emin misiniz?`,
    header: 'Geri Yükleme Onayı',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet, Geri Yükle',
    rejectLabel: 'İptal',
    accept: async () => {
      try {
        const res = await backupAPI.restore(filename)
        basari.value = res.data?.message || 'Geri yükleme tamamlandı'
      } catch (err) {
        hata.value = err.response?.data?.message || 'Geri yükleme başarısız'
      }
    }
  })
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return size.toFixed(1) + ' ' + units[i]
}

const formatDate = (d) => {
  if (!d) return '—'
  return new Date(d).toLocaleString('tr-TR')
}

const cloudConfig = ref({
  provider: 'AWS_S3',
  bucketName: 's3://raspel-erp-backups',
  region: 'eu-central-1',
  autoSync: true,
  encryptionEnabled: true
})
const cloudKaydediliyor = ref(false)
const cloudSenkronizeEdiliyor = ref(false)

const bulutAyarlariYukle = async () => {
  try {
    const r = await backupAPI.getCloudConfig()
    if (r.data) cloudConfig.value = { ...cloudConfig.value, ...r.data }
  } catch {
    /* empty */
  }
}

const bulutAyarlariKaydet = async () => {
  cloudKaydediliyor.value = true
  try {
    await backupAPI.saveCloudConfig(cloudConfig.value)
    basari.value = 'Bulut yedekleme ayarları kaydedildi'
  } catch (err) {
    hata.value = err.response?.data?.message || 'Bulut ayarları kaydedilemedi'
  } finally {
    cloudKaydediliyor.value = false
  }
}

const bulutaEsitle = async (filename) => {
  cloudSenkronizeEdiliyor.value = true
  try {
    const res = await backupAPI.syncToCloud(filename)
    basari.value = res.data?.message || 'Buluta senkronizasyon başarılı'
  } catch (err) {
    hata.value = err.response?.data?.message || 'Buluta senkronizasyon başarısız'
  } finally {
    cloudSenkronizeEdiliyor.value = false
  }
}

onMounted(() => {
  yukle()
  bulutAyarlariYukle()
})
</script>

<style scoped>
.yedekler-container {
  padding: 0;
}
.bulut-kart {
  margin-bottom: 24px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
}
.bulut-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
}
.bulut-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  align-items: flex-end;
}
.bulut-aksiyonlar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
}
.w-full {
  width: 100%;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}
.page-header h1 {
  margin: 0;
  font-size: 24px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.header-islem {
  display: flex;
  align-items: center;
  gap: 10px;
}
.tip-select {
  width: 140px;
}
.tip-select :deep(.p-select) {
  min-height: 40px;
}

.ozet-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 24px;
}
.ozet-kart .p-card-title {
  font-size: 14px !important;
}
.ozet-satir {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px solid var(--border);
  font-size: 13px;
}
.ozet-satir:last-of-type {
  border-bottom: none;
}
.ozet-satir strong {
  font-weight: 600;
}
.ozet-saat {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 6px;
}
.ozet-saat i {
  margin-right: 4px;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
  font-size: 14px;
}

@media (max-width: 900px) {
  .ozet-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 500px) {
  .ozet-grid {
    grid-template-columns: 1fr;
  }
}
</style>
