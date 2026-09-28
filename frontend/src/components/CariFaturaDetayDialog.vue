<template>
  <Dialog
    :visible="visible"
    :header="t('cariHesaplar.faturaDetayi')"
    :modal="true"
    style="width: 620px"
    @update:visible="emit('update:visible', $event)"
  >
    <div
      v-if="fatura"
      class="fatura-detay"
    >
      <div class="fatura-detay-ust">
        <div>
          <strong>{{ fatura.faturaNumarasi }}</strong>
          <span class="fatura-detay-tarih">{{ formatDate(fatura.tarih) }}</span>
        </div>
        <Tag
          :value="fatura.tur === 'SATIS' ? t('cariHesaplar.satis') : t('cariHesaplar.alis')"
          :severity="fatura.tur === 'SATIS' ? 'success' : 'warning'"
        />
      </div>
      <div
        v-if="fatura.cariHesapAd"
        class="fatura-detay-cari"
      >
        {{ t('cariHesaplar.cari') }}: {{ fatura.cariHesapAd }}
      </div>
      <AppDataTable
        :value="fatura.kalemler || []"
        size="small"
        striped-rows
        :paginator="false"
      >
        <Column :header="t('common.description')">
          <template #body="{ data }">
            {{ data.aciklama }}
          </template>
        </Column>
        <Column :header="t('cariHesaplar.adet')">
          <template #body="{ data }">
            {{ data.adet }}
          </template>
        </Column>
        <Column :header="t('cariHesaplar.birimFiyat')">
          <template #body="{ data }">
            {{ formatCurrency(data.birimFiyat) }}
          </template>
        </Column>
        <Column :header="t('common.amount')">
          <template #body="{ data }">
            {{ formatCurrency(data.tutar) }}
          </template>
        </Column>
      </AppDataTable>
      <div class="fatura-detay-ozet">
        <div class="ozet-satir">
          <span>{{ t('cariHesaplar.araToplam') }}</span><span>{{ formatCurrency(fatura.araToplam) }}</span>
        </div>
        <div class="ozet-satir">
          <span>{{ t('cariHesaplar.kdv') }}</span><span>{{ formatCurrency(fatura.kdv) }}</span>
        </div>
        <div class="ozet-satir ozet-genel">
          <span>{{ t('cariHesaplar.genelToplam') }}</span><strong>{{ formatCurrency(fatura.genelToplam) }}</strong>
        </div>
      </div>
    </div>
    <template #footer>
      <Button
        :label="t('stoklar.kapat')"
        icon="pi pi-times"
        class="p-button-text"
        @click="emit('update:visible', false)"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatCurrency, formatDate } from '../utils/format.js'
import AppDataTable from './AppDataTable.vue'

defineProps({
  visible: { type: Boolean, default: false },
  fatura: { type: Object, default: null }
})
const emit = defineEmits(['update:visible'])
const { t } = useI18n()
</script>

<style scoped>
.fatura-detay-ust {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.fatura-detay-tarih {
  margin-left: 12px;
  font-size: 12px;
  color: var(--text-muted);
}
.fatura-detay-cari {
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--text-secondary);
}
.fatura-detay-ozet {
  margin-top: 12px;
  padding: 10px 14px;
  border-top: 1px solid var(--border);
}
.fatura-detay-ozet .ozet-satir {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  font-size: 13px;
}
.fatura-detay-ozet .ozet-genel {
  font-weight: 700;
  font-size: 15px;
}
</style>
