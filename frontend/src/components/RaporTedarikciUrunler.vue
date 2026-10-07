<template>
  <div>
    <div class="rapor-filtre">
      <div class="form-group">
        <label>{{ t('raporlar.tedarikciFiltresi') }}</label>
        <Dropdown
          v-model="tuFiltre"
          :options="tuTedarikciler"
          :placeholder="t('raporlar.tumTedarikciler')"
          class="w-full"
          :show-clear="true"
        />
      </div>
      <Button
        :label="t('raporlar.yenile')"
        icon="pi pi-refresh"
        size="small"
        class="p-button-outlined"
        :loading="tuLoading"
        @click="getTedarikciUrunler"
      />
    </div>
    <DataTable
      :value="tuFiltrelenmisData"
      size="small"
      striped-rows
      :loading="tuLoading"
      row-group-mode="subheader"
      group-rows-by="cariHesapAd"
      :paginator="true"
      :rows="15"
      :rows-per-page-options="[10, 15, 25, 50]"
    >
      <template #empty>
        <EmptyState />
      </template>
      <template #groupheader="{ group }">
        <span class="tedarikci-grup"><i class="pi pi-building" /> {{ group.value }} ({{ t('raporlar.tedarikci') }})</span>
      </template>
      <Column
        field="stokKodu"
        :header="t('raporlar.stokKodu')"
      />
      <Column
        field="stokAd"
        :header="t('raporlar.urun')"
      />
      <Column
        field="toplamMiktar"
        :header="t('raporlar.toplamMiktar')"
      />
      <Column :header="t('raporlar.sonBirimFiyat')">
        <template #body="s">
          {{ formatCurrency(s.data.sonBirimFiyat) }}
        </template>
      </Column>
      <Column :header="t('raporlar.sonAlisTarihi')">
        <template #body="s">
          {{ s.data.sonTarih }}
        </template>
      </Column>
    </DataTable>
    <Message
      v-if="!tuData || !tuData.length"
      severity="info"
      :text="t('raporlar.henuzAlisFaturasiYok')"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { raporAPI } from '../api/index.js'
import { formatCurrency } from '../utils/format.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'

const { t } = useI18n()
const toastBildirim = useToastBildirim()

const tuData = ref([])
const tuLoading = ref(false)
const tuFiltre = ref(null)

const tuTedarikciler = computed(() => {
  const set = new Set(tuData.value.map((d) => d.cariHesapAd).filter(Boolean))
  return [...set].sort()
})

const tuFiltrelenmisData = computed(() => {
  if (!tuFiltre.value) return tuData.value
  return tuData.value.filter((d) => d.cariHesapAd === tuFiltre.value)
})

const getTedarikciUrunler = async () => {
  tuLoading.value = true
  try {
    const r = await raporAPI.tedarikciUrunler()
    tuData.value = r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('raporlar.tedarikciRaporuHata'))
  }
  tuLoading.value = false
}

onMounted(getTedarikciUrunler)
defineExpose({ getTedarikciUrunler })
</script>
