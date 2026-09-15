<template>
  <div class="cari-fiyat-panel">
    <div class="cf-ust">
      <i class="pi pi-user-edit" />
      <span>{{ $t('cariFiyat.baslik') }}</span>
    </div>
    <div
      v-if="fiyatGecmisi && fiyatGecmisi.sonFiyat != null"
      class="cf-icerik"
    >
      <div class="cf-satir">
        <span class="cf-etiket">{{ $t('cariFiyat.sonFiyat') }}</span>
        <strong class="cf-fiyat">{{ formatCurrency(fiyatGecmisi.sonFiyat) }}</strong>
        <span
          v-if="sonKayit && sonKayit.tarih"
          class="cf-tarih"
        >{{ formatDate(sonKayit.tarih) }}</span>
        <span
          v-if="sonKayit && sonKayit.faturaNumarasi"
          class="cf-fatura"
        >#{{ sonKayit.faturaNumarasi }}</span>
      </div>
      <Button
        :label="$t('cariFiyat.uygula')"
        icon="pi pi-check"
        class="p-button-sm p-button-outlined"
        @click="$emit('uygula', fiyatGecmisi.sonFiyat)"
      />
    </div>
    <div
      v-else
      class="cf-bos"
    >
      {{ $t('cariFiyat.kayitYok') }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { formatCurrency, formatTarih as formatDate } from '../utils/format.js'

const props = defineProps({
  /** CariUrunFiyatDTO: { sonFiyat, gecmis: [{ birimFiyat, tarih, faturaNumarasi, adet }] } */
  fiyatGecmisi: { type: Object, default: null }
})

defineEmits(['uygula'])

const sonKayit = computed(() => (props.fiyatGecmisi?.gecmis || [])[0] || null)
</script>

<style scoped>
.cari-fiyat-panel {
  background: rgba(16, 185, 129, 0.06);
  border: 1px solid rgba(16, 185, 129, 0.25);
  border-radius: 10px;
  padding: 10px 14px;
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}
.cf-ust {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #34d399;
}
.cf-icerik {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.cf-satir {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
.cf-etiket {
  color: var(--text-secondary);
}
.cf-fiyat {
  color: var(--text-primary);
}
.cf-tarih,
.cf-fatura {
  font-size: 12px;
  color: var(--text-muted);
}
.cf-bos {
  font-size: 12px;
  color: var(--text-muted);
}
</style>
