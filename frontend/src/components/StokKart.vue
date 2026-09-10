<template>
  <div
    class="stok-kart"
    :class="{ 'dusuk-stok': stok.minMiktar && stok.miktar <= stok.minMiktar }"
    @click="$emit('sec', stok)"
  >
    <div class="kart-ust">
      <div
        v-if="stok.stokKodu"
        class="stok-kod"
      >
        {{ stok.stokKodu }}
      </div>
      <span
        v-if="stok.minMiktar && stok.miktar <= stok.minMiktar"
        class="uyari-eti"
      ><i class="pi pi-exclamation-triangle" /> {{ t('stoklar.colKritik') }}</span>
    </div>
    <h3>{{ stok.ad }}</h3>
    <div class="kart-bilgi">
      <div class="bilgi-item">
        <span class="bilgi-label">{{ t('stoklar.colMiktar') }}</span>
        <span
          class="bilgi-deger"
          :class="stok.miktar <= (stok.minMiktar || 0) ? 'kritik' : 'normal'"
        >
          {{ stok.miktar }} {{ stok.birim || '' }}
        </span>
      </div>
      <div class="bilgi-item">
        <span class="bilgi-label">{{ t('stoklar.colBirimFiyat') }}</span>
        <span class="bilgi-deger">{{ formatCurrency(stok.fiyat) }}</span>
      </div>
    </div>
    <div class="kart-islem">
      <Button
        icon="pi pi-pencil"
        class="p-button-rounded p-button-info p-button-sm"
        @click.stop="$emit('duzenle', stok)"
      />
      <Button
        icon="pi pi-trash"
        class="p-button-rounded p-button-danger p-button-sm"
        @click.stop="$emit('sil', stok.id)"
      />
    </div>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatCurrency } from '../utils/format.js'

defineProps({
  stok: { type: Object, required: true }
})
defineEmits(['sec', 'duzenle', 'sil'])
const { t } = useI18n()
</script>

<style scoped>
.stok-kart {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 18px;
  cursor: pointer;
  transition: all 0.3s ease;
}
.stok-kart:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  border-color: rgba(59, 130, 246, 0.25);
}
.stok-kart.dusuk-stok {
  border-color: rgba(239, 68, 68, 0.3);
}
.stok-kart.dusuk-stok:hover {
  border-color: rgba(239, 68, 68, 0.5);
}
.kart-ust {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.stok-kod {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.uyari-eti {
  font-size: 11px;
  color: #f87171;
  background: rgba(239, 68, 68, 0.15);
  padding: 2px 8px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 3px;
}
.stok-kart h3 {
  margin: 0 0 12px;
  font-size: 15px;
  color: var(--text-primary);
}
.kart-bilgi {
  display: flex;
  gap: 15px;
  margin-bottom: 14px;
}
.bilgi-item {
  flex: 1;
}
.bilgi-label {
  display: block;
  font-size: 11px;
  color: var(--text-muted);
  margin-bottom: 3px;
  text-transform: uppercase;
}
.bilgi-deger {
  font-size: 16px;
  font-weight: 700;
}
.bilgi-deger.normal {
  color: #4ade80;
}
.bilgi-deger.kritik {
  color: #f87171;
}
.kart-islem {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}
</style>