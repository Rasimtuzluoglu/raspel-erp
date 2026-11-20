<template>
  <div
    v-if="yukleniyor"
    class="loading"
  >
    <p><i class="pi pi-spin pi-spinner" /> {{ t('cariHesaplar.hareketlerYukleniyor') }}</p>
  </div>

  <div
    v-else
    class="table-container"
  >
    <AppDataTable
      :value="bakiye"
      striped-rows
      :rows="10"
      :paginator="true"
      size="small"
    >
      <Column
        field="hareketTarihi"
        :header="t('common.date')"
        style="width: 120px"
      >
        <template #body="slotProps">
          {{ formatDate(slotProps.data.hareketTarihi) }}
        </template>
      </Column>
      <Column
        field="tur"
        :header="t('cariHesaplar.tur')"
        style="width: 100px"
      >
        <template #body="slotProps">
          <span :class="['badge', hareketBadgeSinifi(slotProps.data.tur)]">
            {{ hareketTuruEtiketi(slotProps.data.tur) }}
          </span>
        </template>
      </Column>
      <Column
        field="tutar"
        :header="t('common.amount')"
        style="width: 120px"
      >
        <template #body="slotProps">
          <span :class="slotProps.data.tur === 'TAHSILAT' ? 'positive' : 'negative'">
            {{ slotProps.data.tur === 'TAHSILAT' ? '+' : '-' }}{{ formatCurrency(slotProps.data.tutar) }}
          </span>
        </template>
      </Column>
      <Column
        field="aciklama"
        :header="t('common.description')"
      />
      <Column
        field="bakiye"
        :header="t('cariHesaplar.bakiye')"
        style="width: 130px"
      >
        <template #body="slotProps">
          <span :class="slotProps.data.bakiye >= 0 ? 'positive' : 'negative'">
            {{ formatCurrency(slotProps.data.bakiye) }}
          </span>
        </template>
      </Column>
      <template #empty>
        <EmptyState
          v-if="hareketler && hareketler.length === 0"
          :message="t('cariHesaplar.hareketYok')"
          icon="pi pi-list"
        />
      </template>
    </AppDataTable>

    <!-- Sekmeler (faturalar, özel fiyatlar, ürün geçmişi, notlar) parent'tan gelir. -->
    <slot />
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatCurrency, formatDate } from '../../utils/format.js'

/**
 * Cari hareket tablosu (bakiye yürüyen sütunu dâhil) + yükleme durumu.
 * Sekmeler `slot` ile parent'tan verilir; bileşen veri çekmez.
 */
defineProps({
  hareketler: { type: Array, default: () => [] },
  bakiye: { type: Array, default: () => [] },
  yukleniyor: { type: Boolean, default: false }
})

const { t } = useI18n()

const hareketTuruEtiketi = (tur) =>
  ({ TAHSILAT: t('cariHesaplar.tahsilat'), ODEME: t('cariHesaplar.odeme'), BORC: t('cariHesaplar.borclandirma') })[tur] || tur

const hareketBadgeSinifi = (tur) =>
  ({ TAHSILAT: 'tahsilat', ODEME: 'odeme', BORC: 'borclandirma' })[tur] || 'odeme'
</script>

<style scoped>
.loading {
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
  font-size: 16px;
}

.table-container {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
}

.badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
}

.positive {
  color: var(--success);
  font-weight: bold;
}

.negative {
  color: var(--danger);
  font-weight: bold;
}
</style>
