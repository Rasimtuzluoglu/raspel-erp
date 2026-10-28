<template>
  <div
    class="kpi-grid"
    data-test="kpi-grid"
  >
    <template v-if="yukleniyor && !kpi">
      <div
        v-for="i in 4"
        :key="`kpi-skeleton-${i}`"
        class="bento bento--skeleton"
        aria-hidden="true"
      >
        <span class="iskelet iskelet--kisa" />
        <span class="iskelet iskelet--buyuk" />
        <span class="iskelet iskelet--uzun" />
      </div>
    </template>

    <template v-else>
      <!-- Likidite -->
      <article
        class="bento bento--likidite"
        data-test="kpi-likidite"
      >
        <header class="bento-ust">
          <span class="bento-ikon"><i
            class="pi pi-wallet"
            aria-hidden="true"
          /></span>
          <span class="bento-etiket">{{ t('dashboard.kpiLikidite') }}</span>
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
        <p class="bento-deger gizli-veri">
          {{ formatCurrency(likidite.deger || 0) }}
        </p>
        <SparkLine
          v-if="sparkline.length > 1"
          :veriler="sparkline"
          renk="#10b981"
          :yukseklik="36"
        />
        <footer class="bento-alt">
          <span>{{ t('dashboard.kpiKasa') }} <strong>{{ formatCurrency(likidite.kasa || 0) }}</strong></span>
          <span>{{ t('dashboard.kpiBanka') }} <strong>{{ formatCurrency(likidite.banka || 0) }}</strong></span>
        </footer>
      </article>

      <!-- Net cari -->
      <router-link
        to="/cari-hesaplar"
        class="bento bento--cari"
        data-test="kpi-net-cari"
      >
        <header class="bento-ust">
          <span class="bento-ikon"><i
            class="pi pi-users"
            aria-hidden="true"
          /></span>
          <span class="bento-etiket">{{ t('dashboard.kpiNetCari') }}</span>
          <i
            class="pi pi-arrow-right bento-git"
            aria-hidden="true"
          />
        </header>
        <p class="bento-deger gizli-veri">
          {{ formatCurrency(netCari.deger || 0) }}
        </p>
        <footer class="bento-alt bento-alt--cift">
          <span class="bento-alacak">{{ t('dashboard.kpiAlacak') }} <strong>{{ formatCurrency(netCari.alacak || 0) }}</strong></span>
          <span class="bento-borc">{{ t('dashboard.kpiBorc') }} <strong>{{ formatCurrency(netCari.borc || 0) }}</strong></span>
        </footer>
      </router-link>

      <!-- Aylık ciro + hedef halkası -->
      <router-link
        to="/raporlar"
        class="bento bento--ciro"
        data-test="kpi-ciro"
      >
        <header class="bento-ust">
          <span class="bento-ikon"><i
            class="pi pi-chart-bar"
            aria-hidden="true"
          /></span>
          <span class="bento-etiket">{{ t('dashboard.kpiCiro') }}</span>
          <i
            class="pi pi-arrow-right bento-git"
            aria-hidden="true"
          />
        </header>
        <div class="bento-govde">
          <p class="bento-deger gizli-veri">
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
        <footer class="bento-alt">
          <span>{{ t('dashboard.kpiHedef') }} <strong>{{ formatCurrency(ciro.hedef || 0) }}</strong></span>
          <span>%{{ ciroYuzde }}</span>
        </footer>
      </router-link>

      <!-- Aylık kâr -->
      <router-link
        to="/raporlar"
        class="bento bento--kar"
        data-test="kpi-kar"
      >
        <header class="bento-ust">
          <span class="bento-ikon"><i
            class="pi pi-chart-line"
            aria-hidden="true"
          /></span>
          <span class="bento-etiket">{{ t('dashboard.kpiKar') }}</span>
          <span class="delta delta--marj">{{ t('dashboard.kpiMarj') }} %{{ marj }}</span>
        </header>
        <p
          class="bento-deger gizli-veri"
          :class="(kar.deger || 0) >= 0 ? 'bento-deger--artis' : 'bento-deger--azalis'"
        >
          {{ formatCurrency(kar.deger || 0) }}
        </p>
        <div class="kar-cubuk">
          <div
            class="kar-cubuk-dolu"
            :style="{ width: `${karYuzde}%` }"
          />
        </div>
        <footer class="bento-alt">
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
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
.bento {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid var(--border);
  background:
    linear-gradient(180deg, rgba(148, 163, 184, 0.06), rgba(148, 163, 184, 0) 60%),
    var(--bg-card);
  box-shadow: var(--elev-1, 0 2px 10px rgba(0, 0, 0, 0.25));
  color: inherit;
  text-decoration: none;
  min-width: 0;
  transition:
    transform var(--dur-base, 0.2s) var(--ease-standard, ease),
    border-color var(--dur-base, 0.2s) var(--ease-standard, ease),
    box-shadow var(--dur-base, 0.2s) var(--ease-standard, ease);
}
a.bento:hover {
  transform: translateY(-3px);
  border-color: var(--accent-border);
  box-shadow: var(--elev-2, 0 8px 24px rgba(0, 0, 0, 0.35));
}
a.bento:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.bento-ust {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.bento-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 9px;
  font-size: 14px;
  flex-shrink: 0;
}
.bento--likidite .bento-ikon {
  background: var(--success-soft);
  color: var(--success);
}
.bento--cari .bento-ikon {
  background: var(--accent-soft);
  color: var(--accent);
}
.bento--ciro .bento-ikon {
  background: rgba(59, 130, 246, 0.16);
  color: #60a5fa;
}
.bento--kar .bento-ikon {
  background: rgba(139, 92, 246, 0.16);
  color: #a78bfa;
}
.bento-etiket {
  flex: 1;
  min-width: 0;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bento-git {
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
.bento-deger {
  margin: 0;
  font-size: 1.45rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
}
.bento-deger--artis {
  color: var(--success);
}
.bento-deger--azalis {
  color: var(--danger);
}
.bento-alt {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 11.5px;
  color: var(--text-secondary);
  flex-wrap: wrap;
}
.bento-alt strong {
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.bento-alt--cift {
  gap: 12px;
}
.bento-alacak strong {
  color: var(--success);
}
.bento-borc strong {
  color: var(--danger);
}
.bento-govde {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.halka {
  width: 44px;
  height: 44px;
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
  height: 6px;
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
.bento--skeleton {
  min-height: 128px;
  gap: 12px;
}
.iskelet {
  display: block;
  border-radius: 8px;
  background: var(--bg-muted);
  animation: iskeletParlama 1.4s ease infinite;
}
.iskelet--kisa {
  width: 42%;
  height: 12px;
}
.iskelet--buyuk {
  width: 68%;
  height: 26px;
}
.iskelet--uzun {
  width: 100%;
  height: 12px;
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
  .bento,
  a.bento:hover,
  .halka-dolu,
  .kar-cubuk-dolu,
  .iskelet {
    transition: none;
    transform: none;
    animation: none;
  }
}
@media (max-width: 1100px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 620px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
}
</style>
