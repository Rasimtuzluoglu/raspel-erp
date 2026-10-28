<template>
  <div
    class="pano-kpi"
    data-test="kpi-grid"
    data-widget="kpi"
  >
    <template v-if="yukleniyor && !kpi">
      <div
        v-for="i in 4"
        :key="`kpi-iskelet-${i}`"
        class="kpi-hucre kpi-hucre--iskelet"
        aria-hidden="true"
      >
        <span class="kpi-iskelet kpi-iskelet--kisa" />
        <span class="kpi-iskelet kpi-iskelet--buyuk" />
        <span class="kpi-iskelet kpi-iskelet--uzun" />
      </div>
    </template>

    <template v-else>
      <!-- Likidite -->
      <article
        class="kpi-hucre kpi-hucre--likidite"
        data-test="kpi-likidite"
      >
        <header class="kpi-ust">
          <span class="kpi-ikon"><i
            class="pi pi-wallet"
            aria-hidden="true"
          /></span>
          <span class="kpi-etiket">{{ t('dashboard.kpiLikidite') }}</span>
          <span
            v-if="trendVar"
            class="delta"
            :class="trendYuzde >= 0 ? 'delta--artis' : 'delta--azalis'"
          >
            <i
              :class="trendYuzde >= 0 ? 'pi pi-arrow-up-right' : 'pi pi-arrow-down-right'"
              aria-hidden="true"
            />
            {{ Math.abs(trendYuzde).toFixed(1) }}%
          </span>
        </header>
        <p class="kpi-deger gizli-veri">
          {{ formatCurrency(likidite.deger || 0) }}
        </p>
        <SparkLine
          v-if="sparkline.length > 1"
          :veriler="sparkline"
          renk="#10b981"
          :yukseklik="30"
        />
        <footer class="kpi-alt">
          <span>{{ t('dashboard.kpiKasa') }} <strong>{{ formatCurrency(likidite.kasa || 0) }}</strong></span>
          <span>{{ t('dashboard.kpiBanka') }} <strong>{{ formatCurrency(likidite.banka || 0) }}</strong></span>
        </footer>
      </article>

      <!-- Net cari -->
      <router-link
        to="/cari-hesaplar"
        class="kpi-hucre kpi-hucre--cari"
        data-test="kpi-net-cari"
      >
        <header class="kpi-ust">
          <span class="kpi-ikon"><i
            class="pi pi-users"
            aria-hidden="true"
          /></span>
          <span class="kpi-etiket">{{ t('dashboard.kpiNetCari') }}</span>
          <i
            class="pi pi-arrow-right kpi-git"
            aria-hidden="true"
          />
        </header>
        <p class="kpi-deger gizli-veri">
          {{ formatCurrency(netCari.deger || 0) }}
        </p>
        <footer class="kpi-alt kpi-alt--cift">
          <span class="kpi-alacak">{{ t('dashboard.kpiAlacak') }} <strong>{{ formatCurrency(netCari.alacak || 0) }}</strong></span>
          <span class="kpi-borc">{{ t('dashboard.kpiBorc') }} <strong>{{ formatCurrency(netCari.borc || 0) }}</strong></span>
        </footer>
      </router-link>

      <!-- Aylık ciro + hedef halkası -->
      <router-link
        to="/raporlar"
        class="kpi-hucre kpi-hucre--ciro"
        data-test="kpi-ciro"
      >
        <header class="kpi-ust">
          <span class="kpi-ikon"><i
            class="pi pi-chart-bar"
            aria-hidden="true"
          /></span>
          <span class="kpi-etiket">{{ t('dashboard.kpiCiro') }}</span>
          <i
            class="pi pi-arrow-right kpi-git"
            aria-hidden="true"
          />
        </header>
        <div class="kpi-govde">
          <p class="kpi-deger gizli-veri">
            {{ formatCurrency(ciro.deger || 0) }}
          </p>
          <svg
            class="halka"
            viewBox="0 0 44 44"
            role="img"
            :aria-label="`${t('dashboard.kpiCiro')} %${ciroYuzde}`"
          >
            <circle
              class="halka-zemin"
              cx="22"
              cy="22"
              r="18"
            />
            <circle
              class="halka-dolu"
              cx="22"
              cy="22"
              r="18"
              :stroke-dasharray="`${(ciroYuzde / 100) * 113.1} 113.1`"
            />
          </svg>
        </div>
        <footer class="kpi-alt">
          <span>{{ t('dashboard.kpiHedef') }} <strong>{{ formatCurrency(ciro.hedef || 0) }}</strong></span>
          <span>%{{ ciroYuzde }}</span>
        </footer>
      </router-link>

      <!-- Aylık kâr + kâr hedefi -->
      <router-link
        to="/raporlar"
        class="kpi-hucre kpi-hucre--kar"
        data-test="kpi-kar"
      >
        <header class="kpi-ust">
          <span class="kpi-ikon"><i
            class="pi pi-chart-line"
            aria-hidden="true"
          /></span>
          <span class="kpi-etiket">{{ t('dashboard.kpiKar') }}</span>
          <span class="delta delta--marj">{{ t('dashboard.kpiMarj') }} %{{ marj }}</span>
        </header>
        <p
          class="kpi-deger gizli-veri"
          :class="(kar.deger || 0) >= 0 ? 'kpi-deger--artis' : 'kpi-deger--azalis'"
        >
          {{ formatCurrency(kar.deger || 0) }}
        </p>
        <div class="kar-cubuk">
          <div
            class="kar-cubuk-dolu"
            :style="{ width: `${karYuzde}%` }"
          />
        </div>
        <footer class="kpi-alt">
          <span>{{ t('dashboard.kpiHedef') }} <strong>{{ formatCurrency(kar.hedef || 0) }}</strong></span>
          <span>%{{ karYuzde }}</span>
        </footer>
      </router-link>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import SparkLine from '../SparkLine.vue'
import { formatCurrency } from '../../utils/format.js'

const props = defineProps({
  kpi: { type: Object, default: null },
  yukleniyor: { type: Boolean, default: false }
})

const { t } = useI18n()

const likidite = computed(() => props.kpi?.likidite || {})
const netCari = computed(() => props.kpi?.netCari || {})
const ciro = computed(() => props.kpi?.ciro || {})
const kar = computed(() => props.kpi?.kar || {})

const sparkline = computed(() => (Array.isArray(likidite.value.sparkline) ? likidite.value.sparkline : []))

const trendVar = computed(
  () => props.kpi?.likidite?.trendYuzde !== null && props.kpi?.likidite?.trendYuzde !== undefined
)
const trendYuzde = computed(() => Number(likidite.value.trendYuzde) || 0)

const yuzdeSinirla = (deger) => Math.min(100, Math.max(0, Math.round(Number(deger) || 0)))

const ciroYuzde = computed(() => {
  if (ciro.value.yuzde !== null && ciro.value.yuzde !== undefined) return yuzdeSinirla(ciro.value.yuzde)
  const hedef = Number(ciro.value.hedef) || 0
  return hedef > 0 ? yuzdeSinirla((Number(ciro.value.deger) / hedef) * 100) : 0
})

const marj = computed(() => {
  if (kar.value.marj !== null && kar.value.marj !== undefined) return Number(kar.value.marj).toFixed(1)
  const ciroDegeri = Number(ciro.value.deger) || 0
  return ciroDegeri > 0 ? ((Number(kar.value.deger) / ciroDegeri) * 100).toFixed(1) : '0.0'
})

const karYuzde = computed(() => {
  const hedef = Number(kar.value.hedef) || 0
  return hedef > 0 ? yuzdeSinirla((Number(kar.value.deger) / hedef) * 100) : 0
})
</script>

<style scoped>
/*
 * KPI hucreleri kart degildir: cerceve/arka plan/golge/yaricap yoktur.
 * Ayiricilar `dashboard.css` icindeki `.pano-kpi` kurallarindan gelir.
 */
.kpi-hucre {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px 24px;
  min-width: 0;
  color: inherit;
  text-decoration: none;
  transition: background var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.kpi-hucre:hover {
  background: var(--bg-muted);
}
a.kpi-hucre:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: -2px;
}
.kpi-ust {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.kpi-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 10px;
  font-size: 12.5px;
  flex-shrink: 0;
}
.kpi-hucre--likidite .kpi-ikon {
  background: var(--success-soft);
  color: var(--success);
}
.kpi-hucre--cari .kpi-ikon {
  background: var(--accent-soft);
  color: var(--accent);
}
.kpi-hucre--ciro .kpi-ikon {
  background: rgba(59, 130, 246, 0.16);
  color: #60a5fa;
}
.kpi-hucre--kar .kpi-ikon {
  background: rgba(139, 92, 246, 0.16);
  color: #a78bfa;
}
.kpi-etiket {
  flex: 1;
  min-width: 0;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.kpi-git {
  font-size: 11px;
  color: var(--text-muted);
}
.delta {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 2px 7px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
}
.delta--artis {
  color: var(--success);
  background: var(--success-soft);
}
.delta--azalis {
  color: var(--danger);
  background: var(--danger-soft);
}
.delta--marj {
  color: var(--text-secondary);
  background: var(--bg-muted);
}
.kpi-deger {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.kpi-deger--artis {
  color: var(--success);
}
.kpi-deger--azalis {
  color: var(--danger);
}
.kpi-alt {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: auto;
  font-size: 11.5px;
  color: var(--text-secondary);
  flex-wrap: wrap;
}
.kpi-alt strong {
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.kpi-alt--cift {
  gap: 12px;
}
.kpi-alacak strong {
  color: var(--success);
}
.kpi-borc strong {
  color: var(--danger);
}
.kpi-govde {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.halka {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  transform: rotate(-90deg);
}
.halka-zemin {
  fill: none;
  stroke: var(--bg-muted);
  stroke-width: 5;
}
.halka-dolu {
  fill: none;
  stroke: var(--accent);
  stroke-width: 5;
  stroke-linecap: round;
  transition: stroke-dasharray 0.4s ease;
}
.kar-cubuk {
  height: 5px;
  border-radius: 999px;
  background: var(--bg-muted);
  overflow: hidden;
}
.kar-cubuk-dolu {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #7c3aed, #a78bfa);
  transition: width 0.4s ease;
}
.kpi-hucre--iskelet {
  min-height: 118px;
  gap: 10px;
  justify-content: center;
}
.kpi-iskelet {
  display: block;
  border-radius: 7px;
  background: var(--bg-muted);
  animation: iskeletParlama 1.4s ease infinite;
}
.kpi-iskelet--kisa {
  width: 42%;
  height: 11px;
}
.kpi-iskelet--buyuk {
  width: 68%;
  height: 24px;
}
.kpi-iskelet--uzun {
  width: 100%;
  height: 11px;
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
  .kpi-hucre,
  .halka-dolu,
  .kar-cubuk-dolu,
  .kpi-iskelet {
    transition: none;
    animation: none;
  }
}
@media (max-width: 1080px) {
  .kpi-hucre {
    padding: 14px 18px;
  }
  .kpi-deger {
    font-size: 1.25rem;
  }
}
@media (max-width: 600px) {
  .kpi-hucre {
    padding: 12px 16px;
    gap: 5px;
  }
  .kpi-etiket {
    font-size: 11.5px;
  }
  .kpi-deger {
    font-size: 1.05rem;
  }
  .delta--marj {
    display: none;
  }
}
</style>
