<template>
  <div
    class="hedefler"
    data-test="hedefler"
  >
    <div class="hedef-blok hedef-blok--ciro">
      <div class="hedef-halka">
        <svg
          viewBox="0 0 96 96"
          role="img"
          :aria-label="`${t('dashboard.hedefCiro')} %${ciroYuzde}`"
        >
          <circle
            class="halka-zemin"
            cx="48"
            cy="48"
            r="40"
          />
          <circle
            class="halka-dolu"
            cx="48"
            cy="48"
            r="40"
            :stroke-dasharray="`${(ciroYuzde / 100) * 251.3} 251.3`"
          />
        </svg>
        <span class="halka-metin">%{{ ciroYuzde }}</span>
      </div>
      <div class="hedef-metin">
        <span class="hedef-baslik">{{ t('dashboard.hedefCiro') }}</span>
        <strong class="hedef-deger gizli-veri">{{ formatCurrency(ciroGercek) }}</strong>
        <span class="hedef-alt">{{ t('dashboard.kpiHedef') }} {{ formatCurrency(ciroHedef) }}</span>
      </div>
    </div>

    <div class="hedef-blok hedef-blok--kar">
      <div class="hedef-metin hedef-metin--tam">
        <span class="hedef-baslik">{{ t('dashboard.karHedefi') }}</span>
        <span class="hedef-satir">
          <strong class="hedef-deger gizli-veri">{{ formatCurrency(karGercek) }}</strong>
          <span class="hedef-alt">{{ formatCurrency(karHedef) }}</span>
        </span>
      </div>
      <div class="kar-cubuk">
        <div
          class="kar-cubuk-dolu"
          :class="{ 'kar-cubuk-dolu--asildi': karYuzde >= 100 }"
          :style="{ width: `${karYuzde}%` }"
        />
      </div>
      <span class="hedef-yuzde">%{{ karYuzde }}</span>
    </div>

    <div class="hedef-blok hedef-blok--projeksiyon">
      <span class="hedef-baslik">{{ t('dashboard.hedefNakitProjeksiyon') }}</span>
      <div class="projeksiyon-grid">
        <div
          v-for="p in projeksiyonlar"
          :key="p.etiket"
          class="projeksiyon-kart"
          :class="{ 'projeksiyon-kart--negatif': p.deger < 0 }"
        >
          <span class="projeksiyon-etiket">{{ p.etiket }}</span>
          <strong class="projeksiyon-deger gizli-veri">{{ formatCurrency(p.deger) }}</strong>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatCurrency } from '../../utils/format.js'

const props = defineProps({
  hedef: { type: Object, default: null },
  kpi: { type: Object, default: null }
})

const { t } = useI18n()

const sinirla = (deger) => Math.min(100, Math.max(0, Math.round(Number(deger) || 0)))

const ciroHedef = computed(() => Number(props.hedef?.ciro ?? props.kpi?.ciro?.hedef) || 0)
const ciroGercek = computed(() => Number(props.hedef?.gerceklesen ?? props.kpi?.ciro?.deger) || 0)
const ciroYuzde = computed(() => {
  if (ciroHedef.value > 0) return sinirla((ciroGercek.value / ciroHedef.value) * 100)
  return sinirla(props.kpi?.ciro?.yuzde)
})

const karHedef = computed(() => Number(props.hedef?.hedefKar ?? props.kpi?.kar?.hedef) || 0)
const karGercek = computed(() => Number(props.hedef?.kar ?? props.kpi?.kar?.deger) || 0)
const karYuzde = computed(() => (karHedef.value > 0 ? sinirla((karGercek.value / karHedef.value) * 100) : 0))

// 30/60/90 gunluk nakit projeksiyonu (mevcut anasayfa formulu korunur).
const projeksiyonlar = computed(() => {
  const likidite = Number(props.kpi?.likidite?.deger) || 0
  const alacak = Number(props.kpi?.netCari?.alacak) || 0
  const borc = Math.abs(Number(props.kpi?.netCari?.borc) || 0)
  const gun30 = likidite + alacak * 0.45 - borc * 0.4
  const gun60 = gun30 + alacak * 0.35 - borc * 0.35
  const gun90 = gun60 + alacak * 0.2 - borc * 0.25
  return [
    { etiket: t('dashboard.hedefGun30'), deger: Math.round(gun30) },
    { etiket: t('dashboard.hedefGun60'), deger: Math.round(gun60) },
    { etiket: t('dashboard.hedefGun90'), deger: Math.round(gun90) }
  ]
})
</script>

<style scoped>
.hedefler {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.2fr) minmax(0, 1.4fr);
  gap: 12px;
}
.hedef-blok {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  min-width: 0;
}
.hedef-blok--ciro {
  flex-direction: row;
  align-items: center;
  gap: 14px;
}
.hedef-halka {
  position: relative;
  width: 76px;
  height: 76px;
  flex-shrink: 0;
}
.hedef-halka svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}
.halka-zemin {
  fill: none;
  stroke: var(--bg-muted);
  stroke-width: 8;
}
.halka-dolu {
  fill: none;
  stroke: var(--accent);
  stroke-width: 8;
  stroke-linecap: round;
  transition: stroke-dasharray 0.4s ease;
}
.halka-metin {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 800;
  color: var(--text-primary);
}
.hedef-metin {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.hedef-metin--tam {
  width: 100%;
}
.hedef-baslik {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}
.hedef-deger {
  font-size: 16px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.hedef-alt {
  font-size: 11.5px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}
.hedef-satir {
  display: flex;
  align-items: baseline;
  gap: 8px;
  flex-wrap: wrap;
}
.hedef-yuzde {
  margin-left: auto;
  font-size: 12px;
  font-weight: 700;
  color: var(--accent);
}
.kar-cubuk {
  height: 8px;
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
.kar-cubuk-dolu--asildi {
  background: linear-gradient(90deg, #059669, #34d399);
}
.projeksiyon-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}
.projeksiyon-kart {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 10px;
  border-radius: 10px;
  background: var(--bg-muted);
  min-width: 0;
}
.projeksiyon-etiket {
  font-size: 11px;
  color: var(--text-secondary);
}
.projeksiyon-deger {
  font-size: 13px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--success);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.projeksiyon-kart--negatif .projeksiyon-deger {
  color: var(--danger);
}
@media (prefers-reduced-motion: reduce) {
  .halka-dolu,
  .kar-cubuk-dolu {
    transition: none;
  }
}
@media (max-width: 900px) {
  .hedefler {
    grid-template-columns: 1fr;
  }
  .hedef-blok--ciro {
    flex-direction: row;
  }
}
@media (max-width: 480px) {
  .projeksiyon-grid {
    grid-template-columns: 1fr;
  }
}
</style>
