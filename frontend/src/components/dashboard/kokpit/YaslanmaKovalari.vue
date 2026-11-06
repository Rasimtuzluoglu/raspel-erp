<template>
  <div
    class="yaslanma-kovalari"
    data-test="yaslanma-kovalari"
  >
    <div
      v-if="kovalar.length === 0"
      class="kova-bos"
      data-test="kova-bos"
    >
      <i
        class="pi pi-chart-bar"
        aria-hidden="true"
      />
      <span>{{ bosMetin || '—' }}</span>
    </div>

    <template v-else>
      <div
        class="kovalar-bar"
        role="img"
        :aria-label="barEtiketi"
      >
        <span
          v-for="kova in kovalar"
          :key="`bar-${kova.anahtar}`"
          class="kovalar-bar-segment"
          :class="[kovaRengi(kova.anahtar), { 'kovalar-bar-segment--sifir': tutar(kova) === 0 }]"
          :style="{ width: `${oran(kova)}%` }"
        />
      </div>

      <div class="kovalar-liste">
        <button
          v-for="kova in kovalar"
          :key="kova.anahtar"
          type="button"
          class="kova-cip"
          :class="{ 'kova-cip--sifir': tutar(kova) === 0 }"
          :data-test="`yaslanma-kova-${kova.anahtar}`"
          @click="emit('kova-sec', kova)"
        >
          <span class="kova-cip-ust">
            <span
              class="kova-cip-nokta"
              :class="kovaRengi(kova.anahtar)"
              aria-hidden="true"
            />
            <span class="kova-cip-etiket">{{ kova.etiket }}</span>
          </span>
          <strong class="kova-cip-tutar gizli-veri">{{ formatPara(tutar(kova), paraBirimi) }}</strong>
          <span
            v-if="kova.adet !== null && kova.adet !== undefined"
            class="kova-cip-adet"
          >{{ kova.adet }} {{ adetEtiketi }}</span>
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { formatPara } from '../../../utils/format.js'

const props = defineProps({
  kovalar: { type: Array, default: () => [] },
  paraBirimi: { type: String, default: 'TRY' },
  bosMetin: { type: String, default: '' },
  adetEtiketi: { type: String, default: 'adet' }
})

const emit = defineEmits(['kova-sec'])

const tutar = (kova) => {
  const sayi = Number(kova?.tutar)
  return Number.isFinite(sayi) ? sayi : 0
}

const toplam = computed(() =>
  props.kovalar.reduce((acc, kova) => acc + Math.abs(tutar(kova)), 0)
)

const oran = (kova) => {
  if (toplam.value <= 0) return 0
  return (Math.abs(tutar(kova)) / toplam.value) * 100
}

const kovaRengi = (anahtar) => {
  switch (anahtar) {
    case 'GUN_90_PLUS':
      return 'kova-renk--kritik-koyu'
    case 'GUN_61_90':
      return 'kova-renk--kritik'
    case 'GUN_31_60':
      return 'kova-renk--uyari'
    case 'GUN_0_30':
      return 'kova-renk--bilgi'
    default:
      return 'kova-renk--olumlu'
  }
}

const barEtiketi = computed(() =>
  props.kovalar
    .map((kova) => `${kova.etiket}: ${formatPara(tutar(kova), props.paraBirimi)}`)
    .join(', ')
)
</script>

<style scoped>
.yaslanma-kovalari {
  display: flex;
  flex-direction: column;
  gap: 9px;
  min-width: 0;
}
.kovalar-bar {
  display: flex;
  width: 100%;
  height: 8px;
  overflow: hidden;
  border-radius: var(--radius-pill);
  background: var(--bg-muted);
}
.kovalar-bar-segment {
  height: 100%;
  min-width: 1px;
  transition: width var(--dur-base) var(--ease-standard);
}
.kovalar-bar-segment--sifir {
  min-width: 0;
}
.kovalar-liste {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(108px, 1fr));
  gap: 6px;
}
.kova-cip {
  display: flex;
  flex-direction: column;
  gap: 2px;
  align-items: flex-start;
  min-width: 0;
  padding: 6px 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    border-color var(--dur-fast) var(--ease-standard),
    background var(--dur-fast) var(--ease-standard);
}
.kova-cip:hover,
.kova-cip:focus-visible {
  border-color: var(--accent-border);
  background: var(--bg-muted);
}
.kova-cip:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.kova-cip--sifir {
  opacity: 0.5;
}
.kova-cip-ust {
  display: flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
  width: 100%;
}
.kova-cip-nokta {
  flex-shrink: 0;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
}
.kova-cip-etiket {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 10.5px;
  color: var(--text-muted);
}
.kova-cip-tutar {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.kova-cip-adet {
  font-size: 10px;
  color: var(--text-muted);
}
.kova-bos {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 14px 8px;
  border: 1px dashed var(--border);
  border-radius: var(--radius-sm);
  font-size: 12px;
  color: var(--text-muted);
}
.kova-renk--olumlu {
  background: var(--accent);
}
.kova-renk--bilgi {
  background: var(--info);
}
.kova-renk--uyari {
  background: var(--warning);
}
.kova-renk--kritik {
  background: var(--danger);
}
.kova-renk--kritik-koyu {
  background: color-mix(in srgb, var(--danger) 70%, var(--bg-primary));
}
@media (prefers-reduced-motion: reduce) {
  .kovalar-bar-segment {
    transition: none;
  }
}
</style>
