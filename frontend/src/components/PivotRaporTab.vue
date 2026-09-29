<template>
  <div class="rapor-icerik">
    <div class="pivot-kontroller">
      <div class="pivot-alan">
        <label>{{ t('raporlar.satir') }}</label>
        <Dropdown
          v-model="pivotSatir"
          :options="pivotBoyutlar"
          option-label="label"
          option-value="value"
        />
      </div>
      <div class="pivot-alan">
        <label>{{ t('raporlar.sutun') }}</label>
        <Dropdown
          v-model="pivotSutun"
          :options="pivotBoyutlar"
          option-label="label"
          option-value="value"
        />
      </div>
      <div class="pivot-alan">
        <label>{{ t('raporlar.deger') }}</label>
        <Dropdown
          v-model="pivotDeger"
          :options="pivotMetrikler"
          option-label="label"
          option-value="value"
        />
      </div>
      <div class="pivot-alan">
        <label>{{ t('raporlar.tarihAraligi') }}</label>
        <TarihHizliSecim v-model="pivotTarih" />
      </div>
      <Button
        :label="t('raporlar.uygula')"
        icon="pi pi-search"
        class="p-button-sm"
        @click="pivotYukle"
      />
    </div>

    <div
      v-if="pivotYukleniyor"
      class="loading"
    >
      <i class="pi pi-spin pi-spinner" /> {{ t('raporlar.hesaplaniyor') }}
    </div>

    <div
      v-else-if="pivotVerisi"
      class="pivot-sonuc"
    >
      <DataTable
        :value="pivotSatirlar"
        size="small"
        striped-rows
        scrollable
      >
        <template #empty>
          <EmptyState />
        </template>
        <Column
          header=""
          frozen
          style="min-width: 160px"
        >
          <template #body="{ data }">
            <strong>{{ data }}</strong>
          </template>
        </Column>
        <Column
          v-for="s in pivotVerisi.sutunlar"
          :key="s"
          :header="s"
          style="min-width: 110px; text-align: right"
        >
          <template #body="{ data }">
            {{ pivotDeger === 'adet' ? pivotHucre(data, s) : formatCurrency(pivotHucre(data, s)) }}
          </template>
        </Column>
        <Column
          :header="t('raporlar.toplam')"
          style="min-width: 110px; text-align: right"
        >
          <template #body="{ data }">
            <strong>{{ pivotDeger === 'adet' ? pivotSatirToplami(data) : formatCurrency(pivotSatirToplami(data)) }}</strong>
          </template>
        </Column>
      </DataTable>
      <div class="pivot-genel">
        {{ t('raporlar.genelToplamEtiketi') }}
        <strong>{{ pivotDeger === 'adet' ? pivotVerisi.genelToplam : formatCurrency(pivotVerisi.genelToplam) }}</strong>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { raporAPI } from '../api/index.js'
import TarihHizliSecim from './TarihHizliSecim.vue'
import EmptyState from './EmptyState.vue'

const props = defineProps({
  formatCurrency: { type: Function, required: true },
  formatDateForApi: { type: Function, required: true }
})
const { t } = useI18n()

// Pivot tablo state
const pivotSatir = ref('cari')
const pivotSutun = ref('ay')
const pivotDeger = ref('tutar')
const pivotTarih = ref(null)
const pivotVerisi = ref(null)
const pivotYukleniyor = ref(false)
const pivotBoyutlar = computed(() => [
  { label: t('raporlar.pivotCari'), value: 'cari' },
  { label: t('raporlar.pivotUrun'), value: 'stok' },
  { label: t('raporlar.pivotKategori'), value: 'kategori' },
  { label: t('raporlar.pivotTur'), value: 'tur' },
  { label: t('raporlar.pivotOdemeDurumu'), value: 'odeme' },
  { label: t('raporlar.ay'), value: 'ay' }
])
const pivotMetrikler = computed(() => [
  { label: t('raporlar.metrikTutar'), value: 'tutar' },
  { label: t('raporlar.metrikAdet'), value: 'adet' }
])

const pivotSatirlar = computed(() => pivotVerisi.value?.satirlar || [])
const pivotHucre = (satir, sutun) => pivotVerisi.value?.hucreler?.[satir]?.[sutun] ?? 0
const pivotSatirToplami = (satir) => pivotVerisi.value?.satirToplamlari?.[satir] ?? 0

const pivotYukle = async () => {
  pivotYukleniyor.value = true
  try {
    const params = { satir: pivotSatir.value, sutun: pivotSutun.value, deger: pivotDeger.value }
    if (pivotTarih.value && pivotTarih.value.length === 2) {
      params.baslangic = props.formatDateForApi(pivotTarih.value[0])
      params.bitis = props.formatDateForApi(pivotTarih.value[1])
    }
    const r = await raporAPI.pivot(params)
    pivotVerisi.value = r.data
  } catch {
    pivotVerisi.value = null
  } finally {
    pivotYukleniyor.value = false
  }
}
</script>

<style scoped>
.pivot-kontroller {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.pivot-alan {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.pivot-alan label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}
.pivot-sonuc {
  margin-top: 8px;
}
.pivot-genel {
  margin-top: 12px;
  text-align: right;
  font-size: 15px;
}
.pivot-genel strong {
  color: var(--accent);
}
</style>
