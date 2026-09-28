<template>
  <Dialog
    :visible="visible"
    :header="t('hizliSatis.bugunkuSatislar', { n: satislar.length })"
    :modal="true"
    style="width: 420px"
    @update:visible="emit('update:visible', $event)"
  >
    <div
      v-if="satislar.length === 0"
      class="sepet-bos"
    >
      {{ t('hizliSatis.bugunSatisYok') }}
    </div>
    <div
      v-for="s in satislar"
      :key="s.id"
      class="gunluk-satis-satir"
    >
      <div class="gunluk-satis-bilgi">
        <span class="gunluk-satis-no">{{ s.faturaNumarasi }}</span>
        <span class="gunluk-satis-cari">{{ s.cariHesapAd || t('hizliSatis.anlik') }}</span>
      </div>
      <span class="gunluk-satis-tutar">{{ formatCurrency(s.genelToplam) }}</span>
    </div>
    <template #footer>
      <Button
        :label="t('common.refresh')"
        icon="pi pi-refresh"
        class="p-button-text"
        @click="emit('yenile')"
      />
      <Button
        :label="t('common.close')"
        icon="pi pi-times"
        class="p-button-text"
        @click="emit('update:visible', false)"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatCurrency } from '../utils/format.js'

defineProps({
  visible: { type: Boolean, default: false },
  satislar: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:visible', 'yenile'])
const { t } = useI18n()
</script>

<style scoped>
.sepet-bos {
  text-align: center;
  padding: 20px;
  color: var(--text-muted);
  font-size: 13px;
}
.gunluk-satis-satir {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
}
.gunluk-satis-bilgi {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.gunluk-satis-no {
  font-size: 12px;
  font-weight: 600;
}
.gunluk-satis-cari {
  font-size: 11px;
  color: var(--text-muted);
}
.gunluk-satis-tutar {
  font-size: 13px;
  font-weight: 700;
  color: var(--accent);
}
</style>
