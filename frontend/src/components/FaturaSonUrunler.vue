<template>
  <div class="son-urunler-panel">
    <div class="son-urunler-ust">
      <i class="pi pi-history" />
      <span>Bu cari son olarak şunları aldı</span>
      <button
        type="button"
        class="son-urunler-kapat"
        title="Gizle"
        @click="$emit('gizle')"
      >
        <i class="pi pi-times" />
      </button>
    </div>
    <div class="son-urunler-liste">
      <button
        v-for="(u, idx) in (urunler || [])"
        :key="u?.stokId || idx"
        type="button"
        class="son-urun-item"
        @click="$emit('ekle', u)"
      >
        <span class="son-urun-ad">{{ u?.stokAd || (u?.stokId ? 'Ürün #' + u.stokId : 'Ürün') }}</span>
        <span class="son-urun-bilgi">{{ u?.sonAlisTarihi || '' }} · {{ u?.adet || 1 }} adet</span>
        <span class="son-urun-fiyat">{{ formatCurrency(u?.sonBirimFiyat || 0) }}</span>
        <i class="pi pi-plus son-urun-ekle" />
      </button>
    </div>
  </div>
</template>

<script setup>
import { formatCurrency } from '../utils/format.js'

defineProps({
  urunler: { type: Array, default: () => [] }
})

defineEmits(['ekle', 'gizle'])
</script>

<style scoped>
.son-urunler-panel {
  background: rgba(16, 185, 129, 0.06);
  border: 1px solid rgba(16, 185, 129, 0.25);
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 15px;
}
.son-urunler-ust {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #34d399;
  margin-bottom: 10px;
}
.son-urunler-kapat {
  margin-left: auto;
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 13px;
  padding: 2px 4px;
}
.son-urunler-kapat:hover {
  color: #f87171;
}
.son-urunler-liste {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.son-urun-item {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--bg-card);
  border: 1px solid rgba(16, 185, 129, 0.2);
  border-radius: 8px;
  padding: 8px 12px;
  cursor: pointer;
  transition: all 0.2s;
  color: var(--text-primary);
}
.son-urun-item:hover {
  border-color: #10b981;
  background: rgba(16, 185, 129, 0.1);
  transform: translateY(-1px);
}
.son-urun-ad {
  font-size: 13px;
  font-weight: 600;
}
.son-urun-bilgi {
  font-size: 11px;
  color: var(--text-muted);
}
.son-urun-fiyat {
  font-size: 12px;
  color: #34d399;
  font-weight: 600;
}
.son-urun-ekle {
  font-size: 12px;
  color: #10b981;
}
</style>
