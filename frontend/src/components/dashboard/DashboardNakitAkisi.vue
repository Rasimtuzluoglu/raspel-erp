<template>
  <DashboardKart
    :baslik="t('dashboard.grafikNakitAkisi')"
    ikon="pi pi-chart-line"
    link-to="/kasa"
    data-test="nakit-karti"
  >
    <template #sag>
      <div
        class="nakit-segment"
        role="group"
        :aria-label="t('dashboard.nakitAralik')"
      >
        <button
          v-for="secenek in ARALIKLAR"
          :key="secenek"
          type="button"
          class="nakit-segment-buton"
          :class="{ 'nakit-segment-buton--aktif': gun === secenek }"
          :aria-pressed="gun === secenek"
          :data-test="`nakit-segment-${secenek}`"
          @click="emit('update:gun', secenek)"
        >
          {{ t('dashboard.gunSecim', { n: secenek }) }}
        </button>
      </div>
    </template>

    <div
      v-if="yukleniyor && !veriVar"
      class="nakit-grafik nakit-grafik--iskelet"
      aria-hidden="true"
    >
      <span class="iskelet" />
    </div>

    <template v-else-if="veriVar">
      <div
        class="nakit-grafik"
        data-test="nakit-grafik"
        :style="{ height: '200px' }"
      >
        <Line
          :data="veri"
          :options="secenekler"
        />
      </div>
      <p class="nakit-gerceklesen">
        {{ t('dashboard.nakitGerceklesen') }}
      </p>
    </template>

    <div
      v-else
      class="nakit-bos"
      data-test="nakit-bos"
    >
      <i
        class="pi pi-chart-line"
        aria-hidden="true"
      />
      <p class="nakit-bos-baslik">
        {{ t('dashboard.nakitBosBaslik') }}
      </p>
      <p class="nakit-bos-alt">
        {{ t('dashboard.nakitBosAlt') }}
      </p>
      <router-link
        to="/kasa"
        class="nakit-bos-buton"
      >
        {{ t('dashboard.kasayaGit') }}
      </router-link>
    </div>

    <template #alt>
      <div
        class="nakit-projeksiyon"
        data-test="nakit-projeksiyon"
      >
        <span class="nakit-projeksiyon-baslik">{{ t('dashboard.hedefNakitProjeksiyon') }}</span>
        <div class="nakit-projeksiyon-cipler">
          <div
            v-for="p in projeksiyonlar"
            :key="p.etiket"
            class="nakit-cip"
            :class="{ 'nakit-cip--negatif': p.deger < 0 }"
            data-test="nakit-projeksiyon-cip"
          >
            <span class="nakit-cip-ust">
              <span class="nakit-cip-tahmini">{{ t('dashboard.tahmini') }}</span>
              <span class="nakit-cip-etiket">{{ p.etiket }}</span>
            </span>
            <strong class="nakit-cip-deger gizli-veri">{{ formatCurrency(p.deger) }}</strong>
          </div>
        </div>
      </div>
    </template>
  </DashboardKart>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Line } from 'vue-chartjs'
import DashboardKart from './DashboardKart.vue'
import { formatCurrency } from '../../utils/format.js'

const props = defineProps({
  // Grafik verisi (parent, secilen gun sayisina gore dilimler).
  veri: { type: Object, default: null },
  secenekler: { type: Object, default: () => ({}) },
  gun: { type: Number, default: 30 },
  yukleniyor: { type: Boolean, default: false },
  projeksiyonlar: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:gun'])

const { t } = useI18n()

const ARALIKLAR = [7, 30]

/**
 * Grafik yalnizca gercek bir hareket varsa cizilir. Aksi halde 200px'lik bos
 * grafik alani yerine 120px'lik kompakt bos durum gosterilir.
 */
const veriVar = computed(() => {
  const setler = Array.isArray(props.veri?.datasets) ? props.veri.datasets : []
  return setler.some((s) => Array.isArray(s.data) && s.data.some((v) => Number(v) > 0))
})
</script>

<style scoped>
.nakit-segment {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px;
  border-radius: 8px;
  background: var(--bg-muted);
}
.nakit-segment-buton {
  min-height: 24px;
  padding: 0 8px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
}
.nakit-segment-buton:hover {
  color: var(--text-primary);
}
.nakit-segment-buton:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}
.nakit-segment-buton--aktif {
  background: var(--bg-card);
  color: var(--text-primary);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.12);
}
.nakit-grafik {
  position: relative;
  width: 100%;
  height: 200px;
  min-width: 0;
}
.nakit-grafik--iskelet {
  display: flex;
  align-items: stretch;
}
.iskelet {
  flex: 1;
  border-radius: 10px;
  background: var(--bg-muted);
  animation: iskeletParlama 1.4s ease infinite;
}
.nakit-gerceklesen {
  margin: 6px 0 0;
  font-size: 11px;
  color: var(--text-muted);
}
.nakit-bos {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 120px;
  padding: 8px 12px;
  text-align: center;
}
.nakit-bos i {
  font-size: 20px;
  color: var(--text-muted);
  opacity: 0.7;
}
.nakit-bos-baslik {
  margin: 2px 0 0;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-secondary);
}
.nakit-bos-alt {
  margin: 0;
  font-size: 11.5px;
  color: var(--text-muted);
}
.nakit-bos-buton {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
  margin-top: 4px;
  padding: 0 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-muted);
  color: var(--text-primary);
  font-size: 11.5px;
  font-weight: 600;
  text-decoration: none;
}
.nakit-bos-buton:hover {
  border-color: var(--accent-border);
  color: var(--accent);
}
.nakit-bos-buton:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.nakit-projeksiyon {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--border);
}
.nakit-projeksiyon-baslik {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-muted);
}
.nakit-projeksiyon-cipler {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  flex: 1;
  min-width: 220px;
}
.nakit-cip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 5px 10px;
  border-radius: 9px;
  background: var(--bg-muted);
  min-width: 0;
}
.nakit-cip-ust {
  display: flex;
  align-items: baseline;
  gap: 5px;
  min-width: 0;
}
.nakit-cip-tahmini {
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.3px;
  text-transform: uppercase;
  color: var(--text-muted);
  flex-shrink: 0;
}
.nakit-cip-etiket {
  font-size: 11px;
  color: var(--text-secondary);
  white-space: nowrap;
}
.nakit-cip-deger {
  font-size: 12px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--success);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.nakit-cip--negatif .nakit-cip-deger {
  color: var(--danger);
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
@media (max-width: 600px) {
  .nakit-projeksiyon-cipler {
    grid-template-columns: minmax(0, 1fr);
    min-width: 100%;
  }
}
</style>
