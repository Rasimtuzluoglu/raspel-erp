<template>
  <div class="kpi-kart">
    <div class="kpi-ust">
      <span
        class="kpi-ikon"
        :style="{ background: `${renk}22`, color: renk }"
      >
        <i :class="ikon" />
      </span>
      <span class="kpi-baslik">{{ baslik }}</span>
      <span
        v-if="trend !== null && trend !== undefined"
        class="kpi-trend"
        :class="trend >= 0 ? 'yukselis' : 'dusus'"
        :title="$t('dashboard.gecenAyaGore')"
      >
        <i :class="trend >= 0 ? 'pi pi-arrow-up-right' : 'pi pi-arrow-down-right'" />
        {{ Math.abs(trend).toFixed(1) }}%
      </span>
    </div>

    <div class="kpi-deger">
      {{ bicimliDeger }}
    </div>

    <SparkLine
      v-if="sparkline && sparkline.length > 1"
      :veriler="sparkline"
      :renk="renk"
      class="kpi-sparkline"
    />

    <div class="kpi-alt">
      <slot name="alt" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import SparkLine from './SparkLine.vue'
import { useSayac } from '../composables/useSayac.js'
import { formatCurrency } from '../utils/format.js'

const props = defineProps({
  baslik: { type: String, default: '' },
  deger: { type: [Number, String], default: 0 },
  ikon: { type: String, default: 'pi pi-chart-line' },
  renk: { type: String, default: '#3b82f6' },
  trend: { type: Number, default: null },
  sparkline: { type: Array, default: () => [] },
  paraBirimi: { type: Boolean, default: true }
})

const sayisalDeger = computed(() => Number(props.deger) || 0)
const { gosterilen } = useSayac(sayisalDeger)

const bicimliDeger = computed(() =>
  props.paraBirimi
    ? formatCurrency(gosterilen.value)
    : Math.round(gosterilen.value).toLocaleString('tr-TR')
)
</script>

<style scoped>
.kpi-kart {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px 18px;
  border-radius: 16px;
  background:
    linear-gradient(180deg, rgba(148, 163, 184, 0.06), rgba(148, 163, 184, 0) 60%),
    var(--bg-card);
  border: 1px solid var(--border);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
  overflow: hidden;
}
.kpi-kart:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.16);
  border-color: rgba(59, 130, 246, 0.35);
}
.kpi-ust {
  display: flex;
  align-items: center;
  gap: 8px;
}
.kpi-ikon {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  flex-shrink: 0;
}
.kpi-baslik {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-secondary);
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.kpi-trend {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11.5px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 999px;
  flex-shrink: 0;
}
.kpi-trend.yukselis {
  color: #10b981;
  background: rgba(16, 185, 129, 0.14);
}
.kpi-trend.dusus {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.14);
}
.kpi-trend i {
  font-size: 10px;
}
.kpi-deger {
  font-size: 1.55rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.kpi-sparkline {
  margin-top: 2px;
}
.kpi-alt {
  font-size: 12px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
@media (prefers-reduced-motion: reduce) {
  .kpi-kart,
  .kpi-kart:hover {
    transition: none;
    transform: none;
  }
}
</style>
