<template>
  <div class="kart-kutu">
    <DataTable
      :value="filtrelenmisTeklifler"
      :loading="yukleniyor"
      paginator
      :rows="10"
      :rows-per-page-options="[10, 25, 50]"
      striped-rows
      responsive-layout="scroll"
      class="p-datatable-sm"
    >
      <template #header>
        <div class="tablo-toolbar">
          <div class="durum-filtre-chips flex gap-2">
            <button
              type="button"
              :class="['chip-btn', { aktif: seciliDurumFiltre === 'HEPSI' }]"
              @click="durumFiltrele('HEPSI')"
            >
              {{ t('teklifler.tumu') }} ({{ teklifler ? teklifler.length : 0 }})
            </button>
            <button
              type="button"
              :class="['chip-btn', { aktif: seciliDurumFiltre === 'TASLAK' }]"
              @click="durumFiltrele('TASLAK')"
            >
              {{ t('teklifler.durumTaslak') }}
            </button>
            <button
              type="button"
              :class="['chip-btn', { aktif: seciliDurumFiltre === 'GONDERILDI' }]"
              @click="durumFiltrele('GONDERILDI')"
            >
              {{ t('teklifler.durumGonderildi') }}
            </button>
            <button
              type="button"
              :class="['chip-btn', { aktif: seciliDurumFiltre === 'ONAYLANDI' }]"
              @click="durumFiltrele('ONAYLANDI')"
            >
              {{ t('teklifler.durumOnaylandi') }}
            </button>
            <button
              type="button"
              :class="['chip-btn', { aktif: seciliDurumFiltre === 'SIPARISE_DONUSTU' }]"
              @click="durumFiltrele('SIPARISE_DONUSTU')"
            >
              {{ t('teklifler.durumSipariseDonustu') }}
            </button>
          </div>
          <!-- REDTEAM/Faz3.3: `p-input-icon-left` PrimeVue 4'te KALDIRILDI; o sinif
               artik uretilmiyor, yani ikon yerlestirme kurali hic uygulanmiyor
               ve ikon input'un uzerine biniyordu. PrimeVue 4 API'si:
               IconField + InputIcon. -->
          <IconField>
            <InputIcon class="pi pi-search" />
            <InputText
              v-model="aramaMetni"
              :placeholder="t('teklifler.aramaPlaceholder')"
              class="p-inputtext-sm"
            />
          </IconField>
        </div>
      </template>

      <Column
        field="teklifNo"
        :header="t('teklifler.teklifNo')"
        sortable
      >
        <template #body="{ data }">
          <div class="flex items-center gap-2">
            <span class="font-bold text-primary">{{ data.teklifNo }}</span>
            <span
              v-if="data.revizyonNo > 0"
              class="badge-rev"
            >{{ t('teklifler.rev') }}{{ data.revizyonNo }}</span>
          </div>
        </template>
      </Column>

      <Column
        field="tarih"
        :header="t('common.date')"
        sortable
      >
        <template #body="{ data }">
          {{ formatDate(data.tarih) }}
        </template>
      </Column>

      <Column
        field="gecerlilikTarihi"
        :header="t('teklifler.gecerlilik')"
        sortable
      >
        <template #body="{ data }">
          <div v-if="data.gecerlilikTarihi">
            <span>{{ formatDate(data.gecerlilikTarihi) }}</span>
            <small
              v-if="isGecmis(data.gecerlilikTarihi) && data.durum !== 'SIPARISE_DONUSTU' && data.durum !== 'FATURALASTI'"
              class="text-red-500 block"
            >{{ t('teklifler.suresiDoldu') }}</small>
          </div>
          <span
            v-else
            class="text-muted"
          >-</span>
        </template>
      </Column>

      <Column
        field="cariHesapAdi"
        :header="t('teklifler.musteriCari')"
        sortable
      >
        <template #body="{ data }">
          <div class="font-medium">
            {{ data.cariHesapAdi || t('teklifler.genelMusteri') }}
          </div>
          <small
            v-if="data.cariVergiNo"
            class="text-muted"
          >{{ t('teklifler.vknTc') }} {{ data.cariVergiNo }}</small>
        </template>
      </Column>

      <Column
        field="kalemler"
        :header="t('teklifler.kalem')"
      >
        <template #body="{ data }">
          <span class="badge-kalem">{{ t('teklifler.nKalem', { n: data.kalemler?.length || 0 }) }}</span>
        </template>
      </Column>

      <Column
        field="genelToplam"
        :header="t('teklifler.genelToplam')"
        sortable
      >
        <template #body="{ data }">
          <span class="font-bold text-base text-primary dark:text-gray-100">
            {{ formatPara(data.genelToplam, data.paraBirimi) }}
          </span>
        </template>
      </Column>

      <Column
        field="durum"
        :header="t('common.status')"
        sortable
      >
        <template #body="{ data }">
          <Tag
            :value="durumLabel(data.durum)"
            :severity="durumSeverity(data.durum)"
          />
        </template>
      </Column>

      <Column
        :header="t('common.actions')"
        class="text-right"
        style="min-width: 220px;"
      >
        <template #body="{ data }">
          <div class="flex justify-end gap-1">
            <!-- Önizle & Mektup / Proforma PDF -->
            <Button
              icon="pi pi-print"
              class="p-button-text p-button-sm p-button-secondary"
              :title="t('teklifler.onizleYazdir')"
              @click="emit('onizle', data)"
            />

            <!-- Revizyon Oluştur -->
            <Button
              icon="pi pi-copy"
              class="p-button-text p-button-sm p-button-info"
              :title="t('teklifler.yeniRevizyon')"
              @click="emit('revizyon', data)"
            />

            <!-- Siparişe Dönüştür -->
            <Button
              v-if="data.durum !== 'SIPARISE_DONUSTU' && data.durum !== 'FATURALASTI'"
              icon="pi pi-shopping-cart"
              class="p-button-text p-button-sm p-button-success"
              :title="t('teklifler.sipariseDonustur')"
              @click="emit('siparis', data)"
            />

            <!-- Faturaya Dönüştür -->
            <Button
              v-if="data.durum !== 'SIPARISE_DONUSTU' && data.durum !== 'FATURALASTI'"
              icon="pi pi-file"
              class="p-button-text p-button-sm p-button-warning"
              :title="t('teklifler.faturayaDonustur')"
              @click="emit('fatura', data)"
            />

            <!-- Düzenle -->
            <Button
              v-if="data.durum !== 'SIPARISE_DONUSTU' && data.durum !== 'FATURALASTI'"
              icon="pi pi-pencil"
              class="p-button-text p-button-sm"
              :title="t('common.edit')"
              @click="emit('duzenle', data)"
            />

            <!-- Sil -->
            <Button
              v-if="data.durum !== 'SIPARISE_DONUSTU' && data.durum !== 'FATURALASTI'"
              icon="pi pi-trash"
              class="p-button-text p-button-sm p-button-danger"
              :title="t('common.delete')"
              @click="emit('sil', data)"
            />
          </div>
        </template>
      </Column>

      <template #empty>
        <div class="text-center py-6 text-muted">
          <i class="pi pi-inbox text-4xl mb-2 block text-gray-400" />
          {{ t('teklifler.empty') }}
        </div>
      </template>
    </DataTable>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatDate, formatPara } from '../utils/format.js'

const props = defineProps({
  teklifler: { type: Array, default: () => [] },
  yukleniyor: { type: Boolean, default: false }
})
const emit = defineEmits(['onizle', 'revizyon', 'siparis', 'fatura', 'duzenle', 'sil'])
const { t } = useI18n()

const aramaMetni = ref('')
const seciliDurumFiltre = ref('HEPSI')

const filtrelenmisTeklifler = computed(() => {
  const q = (aramaMetni.value || '').trim().toLowerCase()
  return props.teklifler.filter((x) => {
    if (seciliDurumFiltre.value && seciliDurumFiltre.value !== 'HEPSI' && x.durum !== seciliDurumFiltre.value) return false
    if (!q) return true
    return [x.teklifNo, x.cariHesapAdi, x.durum].some((v) => String(v || '').toLowerCase().includes(q))
  })
})

// Filtreleme
const durumFiltrele = (durum) => {
  seciliDurumFiltre.value = durum
}

const isGecmis = (tarihStr) => {
  if (!tarihStr) return false
  return new Date(tarihStr) < new Date(new Date().setHours(0, 0, 0, 0))
}

const durumLabel = (durum) => {
  const map = {
    TASLAK: t('teklifler.durumTaslak'),
    GONDERILDI: t('teklifler.durumGonderildi'),
    ONAYLANDI: t('teklifler.durumOnaylandi'),
    REDDEDILDI: t('teklifler.durumReddedildi'),
    SIPARISE_DONUSTU: t('teklifler.durumSipariseDonustu'),
    FATURALASTI: t('teklifler.durumFaturalasti')
  }
  return map[durum] || durum
}

const durumSeverity = (durum) => {
  const map = {
    TASLAK: 'secondary',
    GONDERILDI: 'info',
    ONAYLANDI: 'success',
    REDDEDILDI: 'danger',
    SIPARISE_DONUSTU: 'warning',
    FATURALASTI: 'help'
  }
  return map[durum] || 'info'
}
</script>

<style scoped>
.kart-kutu {
  background: var(--bg-card);
  border-radius: 12px;
  border: 1px solid var(--border);
  padding: 16px;
}
.chip-btn {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  background: var(--bg-card);
  border: 1px solid var(--border);
  cursor: pointer;
  transition: all 0.2s;
}
.chip-btn.aktif {
  background: var(--primary-color, var(--accent));
  color: #fff;
  border-color: var(--primary-color, var(--accent));
}
.badge-rev {
  background: #e0e7ff;
  color: #3730a3;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 700;
}
.badge-kalem {
  background: rgba(0,0,0,0.05);
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
}
</style>
