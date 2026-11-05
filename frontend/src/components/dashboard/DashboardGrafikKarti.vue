<template>
  <DashboardKart
    :baslik="baslik"
    :ikon="ikon"
    :link-to="linkTo"
    :link-metin="linkMetin || t('dashboard.detayaGit')"
    data-test="grafik-karti"
  >
    <p
      v-if="altMetin"
      class="grafik-alt"
    >
      {{ altMetin }}
    </p>

    <div
      v-if="yukleniyor"
      class="grafik-govde"
      :style="{ '--grafik-yukseklik': `${yukseklik}px` }"
      aria-hidden="true"
    >
      <span class="iskelet" />
    </div>

    <!-- Bos veri: grafik alani cizilmez; kompakt bos durum gosterilir. -->
    <div
      v-else-if="bosMu"
      class="grafik-bos"
      :style="{ minHeight: `${bosYukseklik}px` }"
    >
      <i
        class="pi pi-chart-bar"
        aria-hidden="true"
      />
      <p class="grafik-bos-metin">
        {{ bosMesaj || t('dashboard.grafikBos') }}
      </p>
      <router-link
        v-if="bosLinkTo"
        :to="bosLinkTo"
        class="grafik-bos-link"
      >
        {{ bosLinkMetin }}
        <i
          class="pi pi-arrow-right"
          aria-hidden="true"
        />
      </router-link>
    </div>

    <!-- Grafik, kart gorunur alana yaklasana kadar mount edilmez (lazy). -->
    <div
      v-else
      ref="grafikKok"
      class="grafik-govde"
      :style="{ '--grafik-yukseklik': `${yukseklik}px` }"
    >
      <component
        :is="grafikBileseni"
        v-if="gorunur"
        :data="veri"
        :options="secenekler"
      />
      <span
        v-else
        class="iskelet"
        aria-hidden="true"
        data-test="grafik-bekliyor"
      />
    </div>

    <template
      v-if="$slots.alt"
      #alt
    >
      <slot name="alt" />
    </template>
  </DashboardKart>
</template>

<script setup>
import { computed, ref } from 'vue'
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
import DashboardKart from './DashboardKart.vue'
import { useGorunurluk } from '../../composables/useGorunurluk.js'

ChartJS.register(ArcElement, Tooltip, Legend, CategoryScale, LinearScale, PointElement, LineElement, BarElement, Filler)

const props = defineProps({
  baslik: { type: String, required: true },
  ikon: { type: String, default: 'pi pi-chart-bar' },
  linkTo: { type: String, default: null },
  linkMetin: { type: String, default: '' },
  tip: { type: String, default: 'bar' },
  veri: { type: Object, default: null },
  secenekler: { type: Object, default: () => ({}) },
  yukleniyor: { type: Boolean, default: false },
  altMetin: { type: String, default: '' },
  bosMesaj: { type: String, default: '' },
  bosLinkTo: { type: String, default: null },
  bosLinkMetin: { type: String, default: '' },
  yukseklik: { type: Number, default: 240 },
  bosYukseklik: { type: Number, default: 120 }
})

const { t } = useI18n()

// Grafik alani gorunur olana kadar iskelet gosterilir; canvas bu noktada kurulur.
const grafikKok = ref(null)
const { gorunur } = useGorunurluk(grafikKok)

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
.grafik-alt {
  margin: 0 0 8px;
  font-size: 11.5px;
  color: var(--text-muted);
}
.grafik-govde {
  position: relative;
  width: 100%;
  min-width: 0;
  height: var(--grafik-yukseklik, 240px);
}
.iskelet {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 10px;
  background: var(--bg-muted);
  animation: iskeletParlama 1.4s ease infinite;
}
.grafik-bos {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  text-align: center;
  padding: 8px 12px;
}
.grafik-bos i {
  font-size: 20px;
  color: var(--text-muted);
  opacity: 0.7;
}
.grafik-bos-metin {
  margin: 0;
  font-size: 12px;
  color: var(--text-muted);
}
.grafik-bos-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 2px;
  padding: 2px 4px;
  border-radius: 6px;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--accent);
  text-decoration: none;
}
.grafik-bos-link:hover {
  color: var(--accent-hover);
}
.grafik-bos-link:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.grafik-bos-link i {
  font-size: 9px;
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
/* Mobil: grafikler 140px yukseklige iner (prop degeri CSS degiskeniyle korunur). */
@media (max-width: 760px) {
  .grafik-govde {
    height: 140px;
  }
}
</style>
