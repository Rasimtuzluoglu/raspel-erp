<template>
  <div
    class="stok-kart"
    :class="{ 'dusuk-stok': stok.minMiktar && stok.miktar <= stok.minMiktar }"
    @click="$emit('sec', stok)"
  >
    <div
      class="kart-gorsel"
      :class="{ 'gorsel-yok': !(stok.fotoThumbUrl || stok.fotoUrl) }"
    >
      <img
        v-if="stok.fotoThumbUrl || stok.fotoUrl"
        :src="stok.fotoThumbUrl || stok.fotoUrl"
        :alt="stok.ad"
        loading="lazy"
        decoding="async"
      >
      <i
        v-else
        class="pi pi-image"
      />
    </div>
    <div class="kart-ust">
      <div
        v-if="stok.stokKodu"
        class="stok-kod"
      >
        {{ stok.stokKodu }}
      </div>
      <span
        v-if="stok.stokGrubu"
        class="grup-eti"
      >{{ stok.stokGrubu }}</span>
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
        <span class="bilgi-label">{{ t('stoklar.alisFiyati') }}</span>
        <span class="bilgi-deger fiyat">{{ formatCurrency(stok.fiyat) }}</span>
      </div>
      <div class="bilgi-item">
        <span class="bilgi-label">{{ t('stoklar.satisFiyati') }}</span>
        <span class="bilgi-deger fiyat">{{ stok.satisFiyati ? formatCurrency(stok.satisFiyati) : '-' }}</span>
      </div>
    </div>
    <div class="kart-alt-bilgi">
      <span
        v-if="marj != null"
        class="marj-eti"
        :class="marj < 0 ? 'negatif' : 'pozitif'"
      >{{ t('stoklar.karMarji') }}: %{{ marj }}</span>
      <span
        v-if="stok.tedarikciAd"
        class="tedarikci-eti"
      ><i class="pi pi-building" /> {{ stok.tedarikciAd }}</span>
    </div>
    <div class="kart-islem">
      <Button
        icon="pi pi-pencil"
        class="p-button-rounded p-button-info p-button-sm"
        :aria-label="$t('common.edit')"
        :title="$t('common.edit')"
        @click.stop="$emit('duzenle', stok)"
      />
      <Button
        icon="pi pi-trash"
        class="p-button-rounded p-button-danger p-button-sm"
        :aria-label="$t('common.delete')"
        :title="$t('common.delete')"
        @click.stop="$emit('sil', stok.id)"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatCurrency } from '../utils/format.js'

const props = defineProps({
  stok: { type: Object, required: true }
})
defineEmits(['sec', 'duzenle', 'sil'])
const { t } = useI18n()

// Satış - alış üzerinden kâr marjı (%)
const marj = computed(() => {
  const alis = Number(props.stok?.fiyat || 0)
  const satis = Number(props.stok?.satisFiyati || 0)
  if (!alis || !satis) return null
  return Math.round(((satis - alis) / alis) * 1000) / 10
})
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
  border-color: var(--accent-soft-strong);
}
.stok-kart.dusuk-stok {
  border-color: rgba(239, 68, 68, 0.3);
}
.stok-kart.dusuk-stok:hover {
  border-color: rgba(239, 68, 68, 0.5);
}
.kart-gorsel {
  height: 120px;
  margin: -6px -4px 12px;
  border-radius: 10px;
  overflow: hidden;
  background: var(--bg-muted, rgba(148, 163, 184, 0.08));
}
.kart-gorsel img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.kart-gorsel.gorsel-yok {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  font-size: 28px;
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
.grup-eti {
  font-size: 11px;
  color: var(--accent);
  background: var(--accent-soft);
  border: 1px solid var(--accent-border);
  padding: 2px 8px;
  border-radius: 6px;
  margin-right: auto;
}
.kart-alt-bilgi {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
  font-size: 11.5px;
  color: var(--text-muted);
}
.marj-eti.negatif {
  color: #f87171;
  font-weight: 600;
}
.marj-eti.pozitif {
  color: #4ade80;
  font-weight: 600;
}
.tedarikci-eti {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 60%;
}
.bilgi-deger.fiyat {
  font-size: 14px;
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