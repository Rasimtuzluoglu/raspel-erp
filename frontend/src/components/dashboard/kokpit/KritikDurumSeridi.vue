<template>
  <section
    class="kritik-serit"
    data-test="kritik-durum-seridi"
    :aria-label="t('dashboard.icgoruKritik')"
  >
    <template v-if="yukleniyor && ogeler.length === 0">
      <span
        v-for="n in 4"
        :key="`kritik-iskelet-${n}`"
        class="kritik-serit-iskelet"
        aria-hidden="true"
      />
    </template>

    <template v-else-if="ogeler.length > 0">
      <component
        :is="hedef(oge) ? 'router-link' : 'div'"
        v-for="oge in ogeler"
        :key="oge.anahtar"
        :to="hedef(oge) || undefined"
        class="kritik-serit-oge"
        :class="[seviyeSinifi(oge.seviye), { 'kritik-serit-oge--link': !!hedef(oge) }]"
        :data-test="`kritik-oge-${oge.anahtar}`"
      >
        <i
          class="kritik-serit-ikon"
          :class="seviyeIkonu(oge.seviye)"
          aria-hidden="true"
        />
        <span class="kritik-serit-metin">
          <span class="kritik-serit-etiket">{{ etiket(oge) }}</span>
          <strong class="kritik-serit-deger">{{ degerMetni(oge) }}</strong>
        </span>
        <span class="kritik-serit-rozet">{{ seviyeEtiketi(oge.seviye) }}</span>
      </component>
    </template>

    <p
      v-else
      class="kritik-serit-bos"
      data-test="kritik-serit-bos"
    >
      <i
        class="pi pi-check-circle"
        aria-hidden="true"
      />
      {{ t('dashboard.kokpitKritikSeritBos') }}
    </p>
  </section>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatPara } from '../../../utils/format.js'

const props = defineProps({
  ogeler: { type: Array, default: () => [] },
  yukleniyor: { type: Boolean, default: false },
  paraBirimi: { type: String, default: 'TRY' }
})

const { t, te } = useI18n()

const ANAHTARLAR = {
  vadesiGecen: 'dashboard.kokpitKritikSeritVadesiGecen',
  kritikStok: 'dashboard.kokpitKritikSeritKritikStok',
  bekleyenOnay: 'dashboard.kokpitKritikSeritBekleyenOnay',
  likiditeTrendi: 'dashboard.kokpitKritikSeritLikiditeTrendi'
}

const etiket = (oge) => {
  const anahtar = ANAHTARLAR[oge?.anahtar]
  return anahtar && te(anahtar) ? t(anahtar) : oge?.etiket || ''
}

const hedef = (oge) => {
  const route = oge?.hedef?.route
  if (!route) return null
  return { path: route, query: oge.hedef.query || {} }
}

const seviyeSinifi = (seviye) => {
  switch (seviye) {
    case 'KRITIK':
      return 'kritik-serit-oge--kritik'
    case 'WARN':
      return 'kritik-serit-oge--uyari'
    default:
      return 'kritik-serit-oge--bilgi'
  }
}

const seviyeIkonu = (seviye) => {
  switch (seviye) {
    case 'KRITIK':
      return 'pi pi-exclamation-circle'
    case 'WARN':
      return 'pi pi-exclamation-triangle'
    default:
      return 'pi pi-info-circle'
  }
}

const seviyeEtiketi = (seviye) => {
  if (seviye === 'KRITIK') return t('dashboard.kokpitKritik')
  if (seviye === 'WARN') return t('dashboard.kokpitUyari')
  return t('dashboard.kokpitBilgi')
}

const degerMetni = (oge) => {
  const parcalar = []
  if (oge?.tutar !== null && oge?.tutar !== undefined) {
    const sayi = Number(oge.tutar)
    if (Number.isFinite(sayi)) {
      if (oge.anahtar === 'likiditeTrendi') {
        parcalar.push(`${sayi.toLocaleString('tr-TR', { maximumFractionDigits: 1 })}%`)
      } else {
        parcalar.push(formatPara(sayi, props.paraBirimi))
      }
    }
  }
  if (oge?.adet !== null && oge?.adet !== undefined && Number(oge.adet) > 0) {
    const adetAnahtari = oge.anahtar === 'vadesiGecen'
      ? 'dashboard.kokpitFaturaAdet'
      : 'dashboard.kokpitAdet'
    parcalar.push(`${oge.adet} ${t(adetAnahtari)}`)
  }
  return parcalar.length > 0 ? parcalar.join(' · ') : '—'
}
</script>

<style scoped>
.kritik-serit {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 6px;
  min-width: 0;
}
.kritik-serit-oge {
  position: relative;
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  padding: 7px 9px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  color: inherit;
  text-decoration: none;
}
.kritik-serit-oge--link:hover,
.kritik-serit-oge--link:focus-visible {
  border-color: var(--accent-border);
}
.kritik-serit-oge--link:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}
.kritik-serit-oge--kritik {
  border-left: 3px solid var(--danger);
}
.kritik-serit-oge--uyari {
  border-left: 3px solid var(--warning);
}
.kritik-serit-oge--bilgi {
  border-left: 3px solid var(--info);
}
.kritik-serit-ikon {
  flex-shrink: 0;
  font-size: 13px;
}
.kritik-serit-oge--kritik .kritik-serit-ikon {
  color: var(--danger);
}
.kritik-serit-oge--uyari .kritik-serit-ikon {
  color: var(--warning);
}
.kritik-serit-oge--bilgi .kritik-serit-ikon {
  color: var(--info);
}
.kritik-serit-metin {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}
.kritik-serit-etiket {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 10.5px;
  color: var(--text-muted);
}
.kritik-serit-deger {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.kritik-serit-rozet {
  flex-shrink: 0;
  margin-left: auto;
  padding: 1px 5px;
  border-radius: var(--radius-pill);
  font-size: 9px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.kritik-serit-oge--kritik .kritik-serit-rozet {
  background: var(--danger-soft);
  color: var(--danger);
}
.kritik-serit-oge--uyari .kritik-serit-rozet {
  background: var(--warning-soft);
  color: var(--warning);
}
.kritik-serit-oge--bilgi .kritik-serit-rozet {
  background: var(--info-soft);
  color: var(--info);
}
.kritik-serit-bos {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 0;
  padding: 10px;
  border: 1px dashed var(--border);
  border-radius: var(--radius-sm);
  font-size: 11.5px;
  color: var(--text-muted);
}
.kritik-serit-iskelet {
  display: block;
  height: 38px;
  border-radius: var(--radius-sm);
  background: var(--bg-muted);
  animation: kritik-pulse 1.4s ease-in-out infinite;
}
@keyframes kritik-pulse {
  0%,
  100% {
    opacity: 0.45;
  }
  50% {
    opacity: 0.9;
  }
}
@media (prefers-reduced-motion: reduce) {
  .kritik-serit-iskelet {
    animation: none;
  }
}
</style>
