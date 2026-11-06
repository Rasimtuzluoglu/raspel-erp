<template>
  <div
    class="pipeline-kovalari"
    data-test="pipeline-kovalari"
  >
    <div
      v-if="kovalar.length === 0"
      class="pipeline-bos"
      data-test="pipeline-bos"
    >
      <i
        class="pi pi-filter"
        aria-hidden="true"
      />
      <span>{{ bosMetin }}</span>
    </div>

    <div
      v-else
      class="pipeline-liste"
    >
      <button
        v-for="kova in kovalar"
        :key="kova.anahtar"
        type="button"
        class="pipeline-kova"
        :class="[
          renkSinifi(kova.anahtar),
          {
            'pipeline-kova--sifir': adet(kova) === 0,
            'pipeline-kova--secili': secili === kova.anahtar
          }
        ]"
        :data-test="`pipeline-kova-${kova.anahtar}`"
        :aria-pressed="secili ? secili === kova.anahtar : undefined"
        @click="emit('kova-sec', kova)"
      >
        <span class="pipeline-kova-etiket">{{ kova.etiket }}</span>
        <strong class="pipeline-kova-adet">{{ adet(kova) }}</strong>
        <span
          v-if="kova.tutar !== null && kova.tutar !== undefined"
          class="pipeline-kova-tutar gizli-veri"
        >{{ formatPara(tutar(kova), paraBirimi) }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { formatPara } from '../../../utils/format.js'

defineProps({
  kovalar: { type: Array, default: () => [] },
  paraBirimi: { type: String, default: 'TRY' },
  bosMetin: { type: String, default: 'Pipeline verisi yok' },
  secili: { type: String, default: null }
})

const emit = defineEmits(['kova-sec'])

const adet = (kova) => {
  const sayi = Number(kova?.adet)
  return Number.isFinite(sayi) ? sayi : 0
}

const tutar = (kova) => {
  const sayi = Number(kova?.tutar)
  return Number.isFinite(sayi) ? sayi : 0
}

const renkSinifi = (anahtar) => {
  switch (anahtar) {
    case 'VADESI_GECEN':
      return 'pipeline-kova--kritik'
    case 'TAHSIL_EDILDI':
      return 'pipeline-kova--olumlu'
    case 'KESILDI':
      return 'pipeline-kova--ana'
    case 'TEKLIF':
      return 'pipeline-kova--bilgi'
    default:
      return 'pipeline-kova--notr'
  }
}
</script>

<style scoped>
.pipeline-kovalari {
  min-width: 0;
}
.pipeline-liste {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(92px, 1fr));
  gap: 6px;
}
.pipeline-kova {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
  padding: 7px 8px 6px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
  overflow: hidden;
  transition:
    border-color var(--dur-fast) var(--ease-standard),
    background var(--dur-fast) var(--ease-standard);
}
.pipeline-kova::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 2px;
  background: var(--text-muted);
  opacity: 0.7;
}
.pipeline-kova--notr::before {
  background: var(--text-muted);
}
.pipeline-kova--bilgi::before {
  background: var(--info);
}
.pipeline-kova--ana::before {
  background: var(--accent);
}
.pipeline-kova--kritik::before {
  background: var(--danger);
}
.pipeline-kova--olumlu::before {
  background: var(--success);
}
.pipeline-kova:hover,
.pipeline-kova:focus-visible {
  border-color: var(--accent-border);
  background: var(--bg-muted);
}
.pipeline-kova:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.pipeline-kova--secili {
  border-color: var(--accent-border);
  background: var(--accent-soft);
}
.pipeline-kova--sifir {
  opacity: 0.45;
}
.pipeline-kova-etiket {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.02em;
  text-transform: uppercase;
  color: var(--text-muted);
}
.pipeline-kova-adet {
  font-size: 17px;
  font-weight: 700;
  line-height: 1.1;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.pipeline-kova-tutar {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 10.5px;
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
}
.pipeline-bos {
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
@media (prefers-reduced-motion: reduce) {
  .pipeline-kova {
    transition: none;
  }
}
</style>
