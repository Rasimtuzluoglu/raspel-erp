<template>
  <div class="fiyat-gecmisi-panel">
    <div class="fiyat-gecmisi-ust">
      <i class="pi pi-chart-line" />
      <span>Alış Fiyat Geçmişi</span>
      <span :class="['trend-rozet', (fiyatGecmisi.trend || '').toLowerCase()]">{{
        trendLabel(fiyatGecmisi.trend)
      }}</span>
    </div>
    <div class="fiyat-gecmisi-liste">
      <div
        v-for="(kayit, i) in fiyatGecmisi.gecmis"
        :key="i"
        class="fiyat-gecmisi-item"
      >
        <span class="fg-tarih">{{ formatDate(kayit.tarih) }}</span>
        <span class="fg-fatura">{{ kayit.faturaNumarasi }}</span>
        <span class="fg-fiyat">{{ formatCurrency(kayit.birimFiyat) }}</span>
      </div>
    </div>
    <div
      v-if="fiyatGecmisi.guncelFiyat"
      class="fiyat-gecmisi-guncel"
    >
      Güncel Satış Fiyatı: <strong>{{ formatCurrency(fiyatGecmisi.guncelFiyat) }}</strong>
    </div>
  </div>
</template>

<script setup>
import { formatCurrency } from '../utils/format.js'

defineProps({
  fiyatGecmisi: { type: Object, default: null }
})

const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(
    new Date(dateString)
  )
}

const trendLabel = (trend) => ({ ARTIS: 'Yükseliyor', AZALIS: 'Düşüyor', STABIL: 'Sabit' })[trend] || '-'
</script>

<style scoped>
.fiyat-gecmisi-panel {
  background: rgba(139, 92, 246, 0.06);
  border: 1px solid rgba(139, 92, 246, 0.25);
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 15px;
}
.fiyat-gecmisi-ust {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #a78bfa;
  margin-bottom: 8px;
}
.fiyat-gecmisi-liste {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.fiyat-gecmisi-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--text-secondary);
}
.fg-tarih {
  width: 80px;
  flex-shrink: 0;
}
.fg-fatura {
  flex: 1;
}
.fg-fiyat {
  font-weight: 600;
  color: var(--text-primary);
}
.fiyat-gecmisi-guncel {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(139, 92, 246, 0.2);
  font-size: 12px;
  color: var(--text-secondary);
}
.trend-rozet {
  margin-left: auto;
  padding: 1px 8px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
}
.trend-rozet.artis {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}
.trend-rozet.azalis {
  background: rgba(239, 68, 68, 0.15);
  color: #f87171;
}
.trend-rozet.stabil {
  background: rgba(148, 163, 184, 0.15);
  color: #94a3b8;
}
</style>
