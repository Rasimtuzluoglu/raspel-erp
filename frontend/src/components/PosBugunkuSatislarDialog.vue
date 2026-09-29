<template>
  <Dialog
    :visible="visible"
    :header="t('hizliSatis.bugunkuSatislar', { n: satislar.length })"
    :modal="true"
    style="width: 460px"
    @update:visible="emit('update:visible', $event)"
  >
    <div
      v-if="satislar.length === 0"
      class="sepet-bos"
    >
      {{ t('hizliSatis.bugunSatisYok') }}
    </div>
    <template v-else>
      <div class="gunluk-toplam">
        <span>{{ t('hizliSatis.bugunkuSatislar', { n: satislar.length }) }}</span>
        <strong>{{ formatCurrency(toplam) }}</strong>
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
        <button
          type="button"
          class="gunluk-satis-goruntule"
          :title="t('satis.goruntule')"
          :aria-label="t('satis.goruntule')"
          @click="emit('goruntule', s)"
        >
          <i class="pi pi-eye" />
        </button>
      </div>
    </template>
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
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatCurrency } from '../utils/format.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  satislar: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:visible', 'yenile', 'goruntule'])
const { t } = useI18n()

const toplam = computed(() =>
  props.satislar.reduce((acc, s) => acc + (Number(s.genelToplam) || 0), 0)
)
</script>

<style scoped>
.sepet-bos {
  text-align: center;
  padding: 20px;
  color: var(--text-muted);
  font-size: 13px;
}
.gunluk-toplam {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 12px;
  margin-bottom: 6px;
  border-radius: 10px;
  background: var(--bg-muted, rgba(148, 163, 184, 0.08));
  border: 1px solid var(--border);
  font-size: 13px;
  color: var(--text-secondary);
}
.gunluk-toplam strong {
  color: var(--accent);
  font-variant-numeric: tabular-nums;
}
.gunluk-satis-satir {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
}
.gunluk-satis-bilgi {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
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
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.gunluk-satis-goruntule {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-secondary);
  cursor: pointer;
}
.gunluk-satis-goruntule:hover {
  color: var(--accent);
  border-color: var(--accent-border);
}
</style>
