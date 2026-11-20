<template>
  <div
    v-if="faturalar && faturalar.length > 0"
    class="cari-faturalar"
  >
    <div class="cari-not-baslik">
      <h4>
        <i
          class="pi pi-file"
          style="margin-right: 6px"
        />{{ t('cariHesaplar.gecmisFaturalar', { n: faturalar.length }) }}
      </h4>
    </div>
    <AppDataTable
      :value="faturalar"
      size="small"
      striped-rows
      :rows="5"
      :paginator="faturalar.length > 5"
    >
      <Column :header="t('cariHesaplar.faturaNo')">
        <template #body="{ data }">
          <a
            class="fatura-link"
            @click="emit('fatura-detay', data)"
          >{{ data.faturaNumarasi }}</a>
        </template>
      </Column>
      <Column :header="t('common.date')">
        <template #body="{ data }">
          {{ formatDate(data.tarih) }}
        </template>
      </Column>
      <Column :header="t('cariHesaplar.tur')">
        <template #body="{ data }">
          {{ data.tur === 'SATIS' ? t('cariHesaplar.satis') : t('cariHesaplar.alis') }}
        </template>
      </Column>
      <Column :header="t('common.amount')">
        <template #body="{ data }">
          <span class="positive">{{ formatCurrency(data.genelToplam) }}</span>
        </template>
      </Column>
      <Column :header="t('common.status')">
        <template #body="{ data }">
          <span :class="['badge', data.odemeDurumu === 'ODENDI' ? 'tahsilat' : 'odeme']">
            {{ data.odemeDurumu === 'ODENDI' ? t('cariHesaplar.odendi') : data.odemeDurumu === 'KISMI_ODENDI' ? t('cariHesaplar.kismi') : t('cariHesaplar.odenmedi') }}
          </span>
        </template>
      </Column>
    </AppDataTable>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatCurrency, formatDate } from '../../utils/format.js'

/** Cari 360 kartı > Geçmiş Faturalar sekmesi. Veri parent'tan gelir. */
defineProps({
  faturalar: { type: Array, default: () => [] }
})

const emit = defineEmits(['fatura-detay'])
const { t } = useI18n()
</script>

<style scoped>
.cari-faturalar {
  margin-top: 20px;
  border-top: 1px solid var(--border);
  padding-top: 14px;
}

.cari-not-baslik h4 {
  margin: 0 0 10px;
  font-size: 14px;
}

.fatura-link {
  color: var(--accent);
  cursor: pointer;
  font-weight: 600;
  text-decoration: none;
}

.positive {
  color: var(--success);
  font-weight: bold;
}

.badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
}

.badge.tahsilat {
  background-color: var(--success-soft);
  color: var(--success);
}

.badge.odeme {
  background-color: var(--danger-soft);
  color: var(--danger);
}
</style>
