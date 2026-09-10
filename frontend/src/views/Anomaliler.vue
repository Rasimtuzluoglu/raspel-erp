<template>
  <div class="anomaliler-page">
    <PageHeader
      :title="t('anomaliler.title')"
      :subtitle="t('anomaliler.subtitle')"
    >
      <template #actions>
        <Button
          v-if="aktifSekme === 'anomali'"
          :label="t('anomaliler.yenidenTara')"
          icon="pi pi-refresh"
          class="p-button-primary"
          :loading="yukleniyor"
          @click="anomalileriYukle"
        />
        <Button
          v-else
          :label="t('anomaliler.yeniIpEkle')"
          icon="pi pi-plus"
          class="p-button-success"
          @click="ipModalAcik = true"
        />
      </template>
    </PageHeader>

    <TabView @tab-change="sekmeDegisti">
      <TabPanel>
        <template #header>
          <span class="flex items-center gap-1.5">
            <i class="pi pi-shield" />
            {{ t('anomaliler.anomaliRisk') }}
          </span>
        </template>

        <div
          v-if="yukleniyor"
          class="p-4"
        >
          <SkeletonLoader :count="3" />
        </div>

        <div
          v-else-if="anomaliler.length === 0"
          class="empty-box"
        >
          <i class="pi pi-check-circle success-icon" />
          <h3>{{ t('anomaliler.bosBaslik') }}</h3>
          <p>{{ t('anomaliler.bosAciklama') }}</p>
        </div>

        <div
          v-else
          class="anomali-grid"
        >
          <div
            v-for="item in anomaliler"
            :key="item.id"
            class="anomali-card"
            :class="(item.seviye || '').toLowerCase()"
          >
            <div class="card-header">
              <div class="header-left">
                <span
                  class="badge"
                  :class="(item.seviye || '').toLowerCase()"
                >{{ item.seviye }} {{ t('anomaliler.oncelik') }}</span>
                <span class="tur-label">{{ item.tur }}</span>
              </div>
              <span class="tarih">{{ formatTarih(item.tespitTarihi) }}</span>
            </div>
            <h4 class="card-title">
              <i class="pi pi-exclamation-triangle" /> {{ item.baslik }}
            </h4>
            <p class="card-desc">
              {{ item.aciklama }}
            </p>
            <div class="oneri-box">
              <strong><i class="pi pi-lightbulb" /> {{ t('anomaliler.oneri') }}:</strong> {{ item.oneri }}
            </div>
          </div>
        </div>
      </TabPanel>

      <TabPanel>
        <template #header>
          <span class="flex items-center gap-1.5">
            <i class="pi pi-lock" />
            {{ t('anomaliler.ipBeyazListesi') }}
          </span>
        </template>

        <div class="ip-info-box">
          <i class="pi pi-shield" />
          <div>
            <strong>{{ t('anomaliler.ipPolitika') }}</strong>
            <p>{{ t('anomaliler.ipPolitikaAciklama') }}</p>
          </div>
        </div>

        <DataTable
          :value="ipListesi"
          striped-rows
          :loading="ipYukleniyor"
        >
          <Column
            field="ipAdresi"
            :header="t('anomaliler.izinVerilenIp')"
            style="width: 220px"
          >
            <template #body="{ data }">
              <code>{{ data.ipAdresi }}</code>
            </template>
          </Column>
          <Column
            field="aciklama"
            :header="t('anomaliler.aciklamaLokasyon')"
          />
          <Column
            field="eklemeTarihi"
            :header="t('anomaliler.tanimlamaTarihi')"
            style="width: 160px"
          />
          <Column
            field="durum"
            :header="t('common.status')"
            style="width: 120px"
          >
            <template #body="{ data }">
              <Tag
                :value="data.durum || 'AKTIF'"
                severity="success"
              />
            </template>
          </Column>
          <Column
            :header="t('anomaliler.islem')"
            style="width: 100px"
          >
            <template #body="{ data }">
              <Button
                icon="pi pi-trash"
                class="p-button-rounded p-button-text p-button-danger"
                :title="t('anomaliler.ipSil')"
                @click="ipSil(data.id)"
              />
            </template>
          </Column>
        </DataTable>

        <!-- IP Ekleme Modalı -->
        <Dialog
          v-model:visible="ipModalAcik"
          :header="t('anomaliler.yeniIpBaslik')"
          :modal="true"
          :style="{ width: '450px' }"
        >
          <div class="p-fluid">
            <div class="field mb-3">
              <label for="ipAdresi">{{ t('anomaliler.ipAdresi') }}</label>
              <InputText
                id="ipAdresi"
                v-model="yeniIp.ipAdresi"
                :placeholder="t('anomaliler.ipAdresiPlaceholder')"
              />
            </div>
            <div class="field mb-3">
              <label for="ipAciklama">{{ t('anomaliler.aciklamaOfis') }}</label>
              <InputText
                id="ipAciklama"
                v-model="yeniIp.aciklama"
                :placeholder="t('anomaliler.ipAciklamaPlaceholder')"
              />
            </div>
          </div>
          <template #footer>
            <Button
              :label="t('common.cancel')"
              icon="pi pi-times"
              class="p-button-text"
              @click="ipModalAcik = false"
            />
            <Button
              :label="t('common.save')"
              icon="pi pi-check"
              class="p-button-primary"
              :loading="ipKaydediliyor"
              @click="ipKaydet"
            />
          </template>
        </Dialog>
      </TabPanel>
    </TabView>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { anomaliAPI } from '../api/index.js'
import PageHeader from '../components/PageHeader.vue'
import SkeletonLoader from '../components/SkeletonLoader.vue'
import { useI18n } from 'vue-i18n'
import { formatTarihSaat as formatTarih } from '../utils/format.js'

const { t } = useI18n()
const toast = useToast()
const toastBildirim = useToastBildirim()

const aktifSekme = ref('anomali')
const sekmeDegisti = (e) => {
  aktifSekme.value = e.index === 0 ? 'anomali' : 'ip'
}

const yukleniyor = ref(false)
const anomaliler = ref([])

const ipListesi = ref([])
const ipYukleniyor = ref(false)
const ipModalAcik = ref(false)
const ipKaydediliyor = ref(false)
const yeniIp = ref({ ipAdresi: '', aciklama: '' })

const anomalileriYukle = async () => {
  yukleniyor.value = true
  try {
    const res = await anomaliAPI.tara()
    anomaliler.value = res.data || []
    if (anomaliler.value.length > 0) {
      toast.add({
        severity: 'warn',
        summary: t('anomaliler.anomaliTespiti'),
        detail: t('anomaliler.supheliDurum', { sayi: anomaliler.value.length }),
        life: 5000
      })
    }
  } catch {
    toastBildirim.hata(t('anomaliler.taramaHatasi'))
  } finally {
    yukleniyor.value = false
  }
}

const ipListesiYukle = async () => {
  ipYukleniyor.value = true
  try {
    const res = await anomaliAPI.getIpWhitelist()
    ipListesi.value = res.data || []
  } catch {
    /* empty */
  } finally {
    ipYukleniyor.value = false
  }
}

const ipKaydet = async () => {
  if (!yeniIp.value.ipAdresi) {
    toastBildirim.uyari(t('anomaliler.ipGiriniz'))
    return
  }
  ipKaydediliyor.value = true
  try {
    const res = await anomaliAPI.addIpWhitelist(yeniIp.value)
    ipListesi.value = res.data || []
    ipModalAcik.value = false
    yeniIp.value = { ipAdresi: '', aciklama: '' }
    toastBildirim.basarili(t('anomaliler.ipTanimlandi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('anomaliler.ipEklenemedi'))
  } finally {
    ipKaydediliyor.value = false
  }
}

const ipSil = async (id) => {
  try {
    const res = await anomaliAPI.deleteIpWhitelist(id)
    ipListesi.value = res.data || []
    toastBildirim.basarili(t('anomaliler.ipKaldirildi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('anomaliler.ipSilinemedi'))
  }
}

onMounted(() => {
  anomalileriYukle()
  ipListesiYukle()
})
</script>

<style scoped>
.anomaliler-page {
  padding: 0;
}
.ip-info-box {
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.3);
  border-radius: 10px;
  padding: 14px 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}
.ip-info-box i {
  font-size: 28px;
  color: #3b82f6;
}
.ip-info-box strong {
  display: block;
  font-size: 14px;
  color: var(--text-primary);
  margin-bottom: 2px;
}
.ip-info-box p {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
}
.empty-box {
  background: var(--bg-card, #1e293b);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 3rem;
  text-align: center;
  margin-top: 1.5rem;
}
.success-icon {
  font-size: 3.5rem;
  color: #22c55e;
  margin-bottom: 1rem;
}
.anomali-grid {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-top: 1.5rem;
}
.anomali-card {
  background: var(--bg-card, #1e293b);
  border: 1px solid var(--border);
  border-left: 6px solid #3b82f6;
  border-radius: 10px;
  padding: 1.25rem;
}
.anomali-card.kritik, .anomali-card.yuksek {
  border-left-color: #ef4444;
}
.anomali-card.orta {
  border-left-color: #f59e0b;
}
.anomali-card.dusuk {
  border-left-color: #3b82f6;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}
.badge {
  padding: 0.25rem 0.6rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
}
.badge.kritik, .badge.yuksek {
  background: rgba(239, 68, 68, 0.25);
  color: #f87171;
}
.badge.orta {
  background: rgba(245, 158, 11, 0.25);
  color: #fbbf24;
}
.badge.dusuk {
  background: rgba(59, 130, 246, 0.25);
  color: #60a5fa;
}

.tur-label {
  margin-left: 0.5rem;
  font-size: 0.8rem;
  color: var(--text-secondary, #94a3b8);
  font-weight: 600;
}
.tarih {
  font-size: 0.8rem;
  color: var(--text-muted, #64748b);
}
.card-title {
  margin: 0 0 0.5rem 0;
  font-size: 1.1rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.card-desc {
  color: var(--text-secondary, #cbd5e1);
  margin-bottom: 0.75rem;
  font-size: 0.95rem;
}
.oneri-box {
  background: rgba(56, 189, 248, 0.08);
  border: 1px solid rgba(56, 189, 248, 0.15);
  border-radius: 6px;
  padding: 0.6rem 0.8rem;
  font-size: 0.875rem;
  color: #38bdf8;
}
</style>
