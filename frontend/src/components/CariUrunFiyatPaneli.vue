<template>
  <div class="cari-fiyat-panel">
    <div class="cf-ust">
      <i class="pi pi-tags" />
      <span>{{ $t('cariFiyat.baslik') }}</span>
      <span
        v-if="sonKayit && sonKayit.tarih"
        class="cf-tarih"
      >{{ formatDate(sonKayit.tarih) }}</span>
      <span
        v-if="sonKayit && sonKayit.faturaNumarasi"
        class="cf-fatura"
      >#{{ sonKayit.faturaNumarasi }}</span>
    </div>

    <div
      v-if="secenekler.length"
      class="cf-secenekler"
    >
      <button
        v-for="(s, i) in secenekler"
        :key="i"
        type="button"
        class="cf-secenek"
        :title="$t('cariFiyat.uygula')"
        @click="$emit('uygula', s.fiyat)"
      >
        <span class="cf-secenek-ad">{{ s.ad }}</span>
        <strong class="cf-secenek-fiyat">{{ formatCurrency(s.fiyat) }}</strong>
        <i class="pi pi-check" />
      </button>
    </div>

    <div
      v-else-if="fiyatGecmisi && fiyatGecmisi.sonFiyat != null"
      class="cf-icerik"
    >
      <div class="cf-satir">
        <span class="cf-etiket">{{ $t('cariFiyat.sonFiyat') }}</span>
        <strong class="cf-fiyat">{{ formatCurrency(fiyatGecmisi.sonFiyat) }}</strong>
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
  fiyatGecmisi: { type: Object, default: null },
  /** Uygulanabilir fiyat seviyeleri: [{ ad, fiyat }] */
  secenekler: { type: Array, default: () => [] }
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
.cf-secenekler {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.cf-secenek {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  color: var(--text-primary);
  cursor: pointer;
  font-size: 12px;
  transition:
    border-color 0.15s,
    transform 0.15s;
}
.cf-secenek:hover {
  border-color: var(--accent);
  transform: translateY(-1px);
}
.cf-secenek-ad {
  color: var(--text-secondary);
}
.cf-secenek-fiyat {
  font-weight: 700;
}
.cf-secenek i {
  color: var(--accent);
  font-size: 11px;
}
.cf-bos {
  font-size: 12px;
  color: var(--text-muted);
}
</style>
