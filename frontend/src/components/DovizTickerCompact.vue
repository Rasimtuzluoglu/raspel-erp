<template>
  <div class="doviz-ticker-compact">
    <div
      v-for="k in (dovizStore?.kurlar || [])"
      :key="k.kod || k.dovizKodu"
      class="ticker-chip"
    >
      <span class="chip-kod">{{ k.kod || k.dovizKodu }}:</span>
      <span class="chip-fiyat gizli-veri">{{ dovizStore?.formatPara ? dovizStore.formatPara(k.satisFiyati || k.satisKuru, 'TRY') : '' }}</span>
    </div>
    <button
      class="chip-refresh-btn"
      :disabled="dovizStore?.loading || false"
      :title="t('dashboard.kurlariYenile')"
      @click="dovizStore?.kurlariGuncelle"
    >
      <i :class="dovizStore?.loading ? 'pi pi-spin pi-spinner' : 'pi pi-sync'" />
    </button>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { useDovizStore } from '../stores/dovizStore.js'

// Faz 4.5: Dashboard'dan ayrıştırılan döviz ticker'ı (kendi stilini taşır).
const { t } = useI18n()
const dovizStore = useDovizStore()
</script>

<style scoped>
.doviz-ticker-compact {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--bg-card, rgba(255, 255, 255, 0.05));
  border: 1px solid var(--border, rgba(255, 255, 255, 0.12));
  border-radius: 20px;
  padding: 4px 10px;
  font-size: 11px;
  flex-wrap: wrap;
}
.ticker-chip {
  display: flex;
  align-items: center;
  gap: 4px;
}
.chip-kod {
  font-weight: 700;
  color: var(--text-secondary, #94a3b8);
}
.chip-fiyat {
  font-weight: 600;
  color: #10b981;
}
.chip-refresh-btn {
  background: transparent;
  border: none;
  color: var(--text-muted, #64748b);
  cursor: pointer;
  padding: 2px 4px;
  display: flex;
  align-items: center;
  transition: color 0.15s;
}
.chip-refresh-btn:hover {
  color: var(--accent);
}
</style>
