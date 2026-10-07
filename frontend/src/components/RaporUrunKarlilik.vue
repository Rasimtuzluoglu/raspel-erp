<template>
  <div>
    <div class="rapor-filtre">
      <Button
        :label="t('raporlar.yenile')"
        icon="pi pi-refresh"
        size="small"
        class="p-button-outlined"
        :loading="ukLoading"
        @click="getUrunKarlilik"
      />
    </div>
    <DataTable
      :value="ukData"
      size="small"
      striped-rows
      :loading="ukLoading"
      :paginator="true"
      :rows="15"
      :rows-per-page-options="[10, 15, 25, 50]"
    >
      <template #empty>
        <EmptyState />
      </template>
      <Column
        field="stokKodu"
        :header="t('raporlar.stokKodu')"
      />
      <Column
        field="stokAd"
        :header="t('raporlar.urun')"
      />
      <Column :header="t('raporlar.alisMaliyeti')">
        <template #body="s">
          <span class="gizli-veri">{{ formatCurrency(s.data.alisFiyat) }}</span>
        </template>
      </Column>
      <Column :header="t('raporlar.satisFiyati')">
        <template #body="s">
          {{ formatCurrency(s.data.satisFiyati) }}
        </template>
      </Column>
      <Column :header="t('raporlar.kar')">
        <template #body="s">
          <span
            class="gizli-veri"
            :class="s.data.kar >= 0 ? 'positive' : 'negative'"
          >{{ formatCurrency(s.data.kar) }}</span>
        </template>
      </Column>
      <Column :header="t('raporlar.karMarji')">
        <template #body="s">
          <span
            class="gizli-veri"
            :class="s.data.karMarji >= 0 ? 'positive' : 'negative'"
          >%{{ s.data.karMarji }}</span>
        </template>
      </Column>
    </DataTable>
    <Message
      v-if="!ukData || !ukData.length"
      severity="info"
      :text="t('raporlar.henuzUrunYok')"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { raporAPI } from '../api/index.js'
import { formatCurrency } from '../utils/format.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'

const { t } = useI18n()
const toastBildirim = useToastBildirim()

const ukData = ref([])
const ukLoading = ref(false)

const getUrunKarlilik = async () => {
  ukLoading.value = true
  try {
    const r = await raporAPI.urunKarlilik()
    ukData.value = r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || err?.message || t('raporlar.urunKarlilikHata'))
  }
  ukLoading.value = false
}

onMounted(getUrunKarlilik)
defineExpose({ getUrunKarlilik })
</script>
