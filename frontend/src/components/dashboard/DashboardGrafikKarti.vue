<template>
  <section
    class="grafik-karti"
    :class="{ 'grafik-karti--genis': genis }"
    data-test="grafik-karti"
  >
    <header class="grafik-ust">
      <h3 class="grafik-baslik">
        <span
          v-if="ikon"
          class="grafik-ikon"
        ><i
          :class="ikon"
          aria-hidden="true"
        /></span>
        <span class="grafik-baslik-metin">{{ baslik }}</span>
      </h3>
      <router-link
        v-if="linkTo"
        :to="linkTo"
        class="grafik-link"
        :aria-label="`${baslik} — ${t('dashboard.detayaGit')}`"
      >
        {{ t('dashboard.detayaGit') }}
        <i
          class="pi pi-arrow-right"
          aria-hidden="true"
        />
      </router-link>
    </header>

    <p
      v-if="altMetin"
      class="grafik-alt"
    >
      {{ altMetin }}
    </p>

    <div
      class="grafik-govde"
      :style="{ height: `${yukseklik}px` }"
    >
      <div
        v-if="yukleniyor"
        class="grafik-iskelet"
        aria-hidden="true"
      >
        <span class="iskelet" />
      </div>
      <p
        v-else-if="bosMu"
        class="grafik-bos"
      >
        <i
          class="pi pi-chart-bar"
          aria-hidden="true"
        />
        {{ bosMesaj || t('dashboard.grafikBos') }}
      </p>
      <component
        :is="grafikBileseni"
        v-else
        :data="veri"
        :options="secenekler"
      />
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Bar, Doughnut, Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Filler
} from 'chart.js'

ChartJS.register(ArcElement, Tooltip, Legend, CategoryScale, LinearScale, PointElement, LineElement, BarElement, Filler)

const props = defineProps({
  baslik: { type: String, required: true },
  ikon: { type: String, default: 'pi pi-chart-bar' },
  linkTo: { type: String, default: null },
  tip: { type: String, default: 'bar' },
  veri: { type: Object, default: null },
  secenekler: { type: Object, default: () => ({}) },
  yukleniyor: { type: Boolean, default: false },
  genis: { type: Boolean, default: false },
  altMetin: { type: String, default: '' },
  bosMesaj: { type: String, default: '' },
  yukseklik: { type: Number, default: 260 }
})

const { t } = useI18n()

const grafikBileseni = computed(() => {
  if (props.tip === 'donut') return Doughnut
  if (props.tip === 'line') return Line
  return Bar
})

const bosMu = computed(
  () => !props.veri || !Array.isArray(props.veri.datasets) || props.veri.datasets.length === 0
)
</script>

<style scoped>
.grafik-karti {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid var(--border);
  background:
    linear-gradient(180deg, rgba(148, 163, 184, 0.05), rgba(148, 163, 184, 0) 55%),
    var(--bg-card);
  box-shadow: var(--shadow);
  min-width: 0;
}
.grafik-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}
.grafik-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  min-width: 0;
}
.grafik-baslik-metin {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.grafik-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 12px;
  flex-shrink: 0;
}
.grafik-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--accent);
  text-decoration: none;
  padding: 4px 6px;
  border-radius: 6px;
}
.grafik-link:hover {
  color: var(--accent-hover);
  background: var(--accent-soft);
}
.grafik-link:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.grafik-link i {
  font-size: 10px;
}
.grafik-alt {
  margin: 0;
  font-size: 11.5px;
  color: var(--text-muted);
}
.grafik-govde {
  position: relative;
  width: 100%;
  min-height: 120px;
}
.grafik-iskelet {
  position: absolute;
  inset: 0;
  display: flex;
}
.iskelet {
  flex: 1;
  border-radius: 12px;
  background: var(--bg-muted);
  animation: iskeletParlama 1.4s ease infinite;
}
.grafik-bos {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 100%;
  margin: 0;
  color: var(--text-muted);
  font-size: 12.5px;
  text-align: center;
}
.grafik-bos i {
  font-size: 22px;
  opacity: 0.6;
}
@keyframes iskeletParlama {
  0% {
    opacity: 0.45;
  }
  50% {
    opacity: 0.85;
  }
  100% {
    opacity: 0.45;
  }
}
@media (prefers-reduced-motion: reduce) {
  .iskelet {
    animation: none;
  }
}
@media (max-width: 760px) {
  .grafik-govde {
    max-height: 220px;
  }
}
</style>
