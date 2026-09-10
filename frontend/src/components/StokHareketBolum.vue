<template>
  <div class="hareket-bolumu">
    <div class="hareket-header">
      <h2>
        {{ stok.ad }}
        <small style="color: #64748b; font-weight: 400">({{ stok.miktar }} {{ stok.birim || 'Adet' }})</small>
      </h2>
      <Button
        :label="t('stoklar.stokGiris')"
        icon="pi pi-plus-circle"
        class="p-button-success p-button-sm"
        @click="$emit('giris')"
      />
      <Button
        :label="t('stoklar.stokCikis')"
        icon="pi pi-minus-circle"
        class="p-button-danger p-button-sm"
        @click="$emit('cikis')"
      />
      <Button
        icon="pi pi-chevron-up"
        class="p-button-text p-button-sm"
        :title="t('stoklar.kapat')"
        @click="$emit('kapat')"
      />
    </div>
    <div class="table-container">
      <DataTable
        :value="hareketler"
        striped-rows
        :rows="8"
        :paginator="true"
        paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport"
        current-page-report-template="{first} - {last} ({totalRecords} kayıt)"
      >
        <Column
          :header="t('common.date')"
          style="width: 100px"
        >
          <template #body="s">
            {{ formatDate(s.data.hareketTarihi) }}
          </template>
        </Column>
        <Column
          :header="t('faturalar.colTur')"
          style="width: 90px"
        >
          <template #body="s">
            <span :class="['badge', s.data.tur === 'GIRIS' ? 'giris' : 'cikis']">
              {{ s.data.tur === 'GIRIS' ? t('stoklar.giris') : t('stoklar.cikis') }}
            </span>
          </template>
        </Column>
        <Column
          :header="t('stoklar.colMiktar')"
          style="width: 90px"
        >
          <template #body="s">
            <span :class="s.data.tur === 'GIRIS' ? 'positive' : 'negative'">{{ s.data.miktar }}</span>
          </template>
        </Column>
        <Column
          :header="t('stoklar.hareketAgirlik')"
          style="width: 110px"
        >
          <template #body="s">
            {{ s.data.agirlik != null ? s.data.agirlik : '-' }}
          </template>
        </Column>
        <Column
          :header="t('stoklar.hareketCari')"
          style="width: 160px"
        >
          <template #body="s">
            {{ s.data.cariHesapAd || '-' }}
          </template>
        </Column>
        <Column :header="t('stoklar.hareketAciklama')" />
        <Column
          header=""
          style="width: 60px"
        >
          <template #body="s">
            <Button
              icon="pi pi-trash"
              class="p-button-rounded p-button-danger p-button-sm"
              @click="$emit('sil', s.data.id)"
            />
          </template>
        </Column>
      </DataTable>
      <Message
        v-if="hareketler && hareketler.length === 0"
        severity="info"
        :text="t('stoklar.hareketYok')"
      />
    </div>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatTarih as formatDate } from '../utils/format.js'

defineProps({
  stok: { type: Object, required: true },
  hareketler: { type: Array, default: () => [] }
})
defineEmits(['giris', 'cikis', 'kapat', 'sil'])
const { t } = useI18n()
</script>

<style scoped>
.hareket-bolumu {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 20px;
}
.hareket-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}
.hareket-header h2 {
  color: var(--text-primary);
  font-size: 20px;
  margin: 0;
}
.table-container {
  overflow-x: auto;
}
.badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 700;
}
.badge.giris {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}
.badge.cikis {
  background: rgba(239, 68, 68, 0.15);
  color: #f87171;
}
.positive {
  color: #4ade80;
  font-weight: 700;
}
.negative {
  color: #f87171;
  font-weight: 700;
}
</style>