<template>
  <div class="yedekler-container">
    <div class="page-header">
      <h1><i class="pi pi-save" /> {{ t('yedekler.title') }}</h1>
      <div class="header-islem">
        <Select
          v-model="yedekTipi"
          :options="tipler"
          option-label="label"
          option-value="value"
          class="tip-select"
        />
        <Button
          :label="t('yedekler.yedekAl')"
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

    <Card class="dogrulama-kart">
      <template #title>
        <div class="baslik-satir">
          <span>
            <i
              class="pi pi-shield"
            />{{ t('yedekler.yedekDogrulama') }}
          </span>
          <Button
            icon="pi pi-refresh"
            class="p-button-sm p-button-text"
            @click="dogrulamaYukle"
          />
        </div>
      </template>
      <template #content>
        <div class="dogrulama-icerik">
          <Tag
            :value="dogrulama.durum || t('yedekler.kontrolEdiliyor')"
            :severity="dogrulamaSeverity(dogrulama.durum)"
          />
          <span
            v-if="dogrulama.sonYedek"
            class="dogrulama-detay"
          >
            {{ t('yedekler.sonYedek') }}: <strong>{{ dogrulama.sonYedek }}</strong>
            <template v-if="dogrulama.yasSaat != null"> · {{ t('yedekler.saatOnce', { n: dogrulama.yasSaat }) }}</template>
            <template v-if="dogrulama.butunluk === false"> · <strong style="color: var(--red-500)">{{ t('yedekler.butunlukHatasi') }}</strong></template>
          </span>
          <span
            v-else-if="dogrulama.mesaj"
            class="dogrulama-detay"
          >
            {{ dogrulama.mesaj }}
          </span>
        </div>
      </template>
    </Card>

    <div class="ozet-grid">
      <Card class="ozet-kart">
        <template #title>
          <i
            class="pi pi-calendar"
            style="margin-right: 8px"
          />{{ t('yedekler.gunluk') }}
        </template>
        <template #content>
          <div class="ozet-satir">
            <span>{{ t('yedekler.adet') }}</span><strong>{{ schedule.counts?.DAILY || 0 }}</strong>
          </div>
          <div class="ozet-satir">
            <span>{{ t('yedekler.saklama') }}</span><strong>{{ t('yedekler.gunSayisi', { n: 30 }) }}</strong>
          </div>
          <div class="ozet-saat">
            <i class="pi pi-clock" /> {{ t('yedekler.herGun') }}
          </div>
        </template>
      </Card>
      <Card class="ozet-kart">
        <template #title>
          <i
            class="pi pi-calendar-week"
            style="margin-right: 8px"
          />{{ t('yedekler.haftalik') }}
        </template>
        <template #content>
          <div class="ozet-satir">
            <span>{{ t('yedekler.adet') }}</span><strong>{{ schedule.counts?.WEEKLY || 0 }}</strong>
          </div>
          <div class="ozet-satir">
            <span>{{ t('yedekler.saklama') }}</span><strong>{{ t('yedekler.gunSayisi', { n: 180 }) }}</strong>
          </div>
          <div class="ozet-saat">
            <i class="pi pi-clock" /> {{ t('yedekler.pazar') }}
          </div>
        </template>
      </Card>
      <Card class="ozet-kart">
        <template #title>
          <i
            class="pi pi-calendar-plus"
            style="margin-right: 8px"
          />{{ t('yedekler.aylik') }}
        </template>
        <template #content>
          <div class="ozet-satir">
            <span>{{ t('yedekler.adet') }}</span><strong>{{ schedule.counts?.MONTHLY || 0 }}</strong>
          </div>
          <div class="ozet-satir">
            <span>{{ t('yedekler.saklama') }}</span><strong>{{ t('yedekler.gunSayisi', { n: 365 }) }}</strong>
          </div>
          <div class="ozet-saat">
            <i class="pi pi-clock" /> {{ t('yedekler.ayinIlkGunu') }}
          </div>
        </template>
      </Card>
      <Card class="ozet-kart">
        <template #title>
          <i
            class="pi pi-calendar-star"
            style="margin-right: 8px"
          />{{ t('yedekler.yillik') }}
        </template>
        <template #content>
          <div class="ozet-satir">
            <span>{{ t('yedekler.adet') }}</span><strong>{{ schedule.counts?.YEARLY || 0 }}</strong>
          </div>
          <div class="ozet-satir">
            <span>{{ t('yedekler.saklama') }}</span><strong>{{ t('yedekler.sinirsiz') }}</strong>
          </div>
          <!-- Bulut Yedekleme (Cloud Storage) Kartı -->
          <Card class="bulut-kart">
            <template #title>
              <div class="bulut-baslik">
                <span><i
                  class="pi pi-cloud"
                  style="margin-right: 8px; color: #3b82f6"
                />{{ t('yedekler.bulutYedekleme') }}</span>
                <Tag
                  :value="cloudConfig.encryptionEnabled ? t('yedekler.aesSifreli') : t('yedekler.sifresiz')"
                  :severity="cloudConfig.encryptionEnabled ? 'success' : 'warn'"
                />
              </div>
            </template>
            <template #content>
              <div class="bulut-grid">
                <div class="field">
                  <label>{{ t('yedekler.bulutSaglayici') }}</label>
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
                  <label>{{ t('yedekler.hedefDizin') }}</label>
                  <InputText
                    v-model="cloudConfig.bucketName"
                    :placeholder="t('yedekler.hedefDizinPlaceholder')"
                    class="w-full"
                  />
                </div>
                <div class="field">
                  <label>{{ t('yedekler.bolge') }}</label>
                  <InputText
                    v-model="cloudConfig.region"
                    :placeholder="t('yedekler.bolgePlaceholder')"
                    class="w-full"
                  />
                </div>
                <div class="bulut-aksiyonlar">
                  <Button
                    :label="t('yedekler.bulutAyarlariKaydet')"
                    icon="pi pi-save"
                    size="small"
                    class="p-button-outlined"
                    :loading="cloudKaydediliyor"
                    @click="bulutAyarlariKaydet"
                  />
                  <Button
                    :label="t('yedekler.bulutaSenkronize')"
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
              />{{ t('yedekler.yedekDosyalari') }}
            </template>
            <template #content>
              <DataTable
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
                  :header="t('yedekler.dosyaAdi')"
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
                  :header="t('yedekler.tur')"
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
                  :header="t('yedekler.boyut')"
                  sortable
                  style="width: 100px"
                >
                  <template #body="s">
                    {{ formatSize(s.data.size) }}
                  </template>
                </Column>
                <Column
                  field="lastModified"
                  :header="t('common.date')"
                  sortable
                  style="width: 170px"
                >
                  <template #body="s">
                    {{ formatDate(s.data.lastModified) }}
                  </template>
                </Column>
                <Column
                  :header="t('yedekler.islem')"
                  style="width: 100px"
                >
                  <template #body="s">
                    <Button
                      icon="pi pi-download"
                      class="p-button-sm p-button-text"
                      :title="t('yedekler.indir')"
                      @click="indir(s.data.filename)"
                    />
                    <Button
                      icon="pi pi-upload"
                      class="p-button-sm p-button-text p-button-warning"
                      :title="t('yedekler.geriYukle')"
                      @click="geriYukle(s.data.filename)"
                    />
                    <Button
                      icon="pi pi-trash"
                      class="p-button-sm p-button-text p-button-danger"
                      :title="t('common.delete')"
                      @click="sil(s.data.filename)"
                    />
                  </template>
                </Column>
              </DataTable>
              <div
                v-if="(!yedekler || !yedekler.length) && !yedeklerYukleniyor"
                class="empty-state"
              >
                {{ t('yedekler.yedekYok') }}
              </div>
            </template>
          </Card>
        </template>
      </Card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useConfirm } from 'primevue/useconfirm'
import { backupAPI } from '../api/index.js'
import { useI18n } from 'vue-i18n'
import { formatTarihSaat as formatDate } from '../utils/format.js'

const confirm = useConfirm()
const { t } = useI18n()

const tipler = computed(() => [
  { value: 'DAILY', label: t('yedekler.gunluk') },
  { value: 'WEEKLY', label: t('yedekler.haftalik') },
  { value: 'MONTHLY', label: t('yedekler.aylik') },
  { value: 'YEARLY', label: t('yedekler.yillik') }
])

const yedekTipi = ref('DAILY')
const yedekler = ref([])
const yedeklerYukleniyor = ref(false)
const yedekAliniyor = ref(false)
const hata = ref('')
const basari = ref('')
const schedule = ref({})
const dogrulama = ref({})

const dogrulamaYukle = async () => {
  try {
    const r = await backupAPI.dogrula()
    dogrulama.value = r.data || {}
  } catch {
    dogrulama.value = {}
  }
}

const dogrulamaSeverity = (durum) => {
  if (durum === 'OK') return 'success'
  if (durum === 'UYARI') return 'warn'
  if (durum === 'KRITIK') return 'danger'
  return 'info'
}

const typeLabel = (tip) => tipler.value.find((i) => i.value === tip)?.label || tip
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
    hata.value = t('yedekler.hataYukleme')
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
    basari.value = res.data.message || t('yedekler.yedekAlindi')
    await yukle()
  } catch (err) {
    hata.value = err.response?.data?.message || t('yedekler.yedeklemeBasarisiz')
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
      hata.value = t('yedekler.dosyaIndirilemedi')
    })
}

const sil = (filename) => {
  confirm.require({
    message: t('yedekler.silOnayMesaj', { ad: filename }),
    header: t('yedekler.yedekSil'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('masraflar.evetSil'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        await backupAPI.delete(filename)
        basari.value = t('yedekler.silindi', { ad: filename })
        await yukle()
      } catch (err) {
        hata.value = err.response?.data?.message || t('yedekler.silmeBasarisiz')
      }
    }
  })
}

const geriYukle = (filename) => {
  confirm.require({
    message: t('yedekler.geriYuklemeOnayMesaj', { ad: filename }),
    header: t('yedekler.geriYuklemeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('yedekler.evetGeriYukle'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        const res = await backupAPI.restore(filename)
        basari.value = res.data?.message || t('yedekler.geriYuklemeTamamlandi')
      } catch (err) {
        hata.value = err.response?.data?.message || t('yedekler.geriYuklemeBasarisiz')
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
    basari.value = t('yedekler.bulutAyarlariKaydedildi')
  } catch (err) {
    hata.value = err.response?.data?.message || t('yedekler.bulutAyarlariKaydedilemedi')
  } finally {
    cloudKaydediliyor.value = false
  }
}

const bulutaEsitle = async (filename) => {
  cloudSenkronizeEdiliyor.value = true
  try {
    const res = await backupAPI.syncToCloud(filename)
    basari.value = res.data?.message || t('yedekler.bulutSenkronizasyonBasari')
  } catch (err) {
    hata.value = err.response?.data?.message || t('yedekler.bulutSenkronizasyonBasarisiz')
  } finally {
    cloudSenkronizeEdiliyor.value = false
  }
}

onMounted(() => {
  yukle()
  bulutAyarlariYukle()
  dogrulamaYukle()
})
</script>

<style scoped>
.yedekler-container {
  padding: 0;
}
.dogrulama-kart {
  margin-bottom: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
}
.baslik-satir {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  width: 100%;
}
.baslik-satir > span {
  display: inline-flex;
  align-items: center;
  min-width: 0;
}
.baslik-satir > span i {
  margin-right: 8px;
}
.dogrulama-icerik {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.dogrulama-detay {
  color: var(--text-secondary);
  font-size: 0.9rem;
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
  grid-template-columns: repeat(auto-fit, minmax(min(220px, 100%), 1fr));
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
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
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 500px) {
  .ozet-grid {
    grid-template-columns: 1fr;
  }
}
</style>
