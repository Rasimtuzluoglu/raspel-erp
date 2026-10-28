<template>
  <section
    class="pano-bolum grafik-karti"
    data-test="grafik-karti"
  >
    <header class="pano-bolum-ust">
      <h2 class="pano-bolum-baslik">
        <span class="pano-bolum-ikon">
          <i
            :class="ikon"
            aria-hidden="true"
          />
        </span>
        <span class="pano-bolum-baslik-metin">{{ baslik }}</span>
      </h2>
      <router-link
        v-if="linkTo"
        :to="linkTo"
        class="pano-bolum-link"
        :aria-label="`${baslik} — ${t('dashboard.detayaGit')}`"
      >
        {{ t('dashboard.detayaGit') }}
        <i
          class="pi pi-arrow-right"
          aria-hidden="true"
        />
      </router-link>
    </header>

    <div class="pano-bolum-govde">
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

      <slot name="alt" />
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
/*
 * Grafik, panonun icindeki duz bir bolumdur: kart cercevesi/golgesi yoktur.
 * Baslik duzeni `dashboard.css` icindeki `.pano-bolum-*` siniflarindan gelir.
 */
.grafik-karti {
  min-width: 0;
}
.grafik-alt {
  margin: 0 0 8px;
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
  border-radius: 10px;
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
