<template>
  <div
    class="mikro-grafik"
    :style="{ height: `${yukseklik}px` }"
    role="img"
    :aria-label="ariaLabel"
    :data-test="dataTest"
  >
    <svg
      v-if="cizilebilir"
      :viewBox="`0 0 100 ${yukseklik}`"
      preserveAspectRatio="none"
      aria-hidden="true"
      focusable="false"
    >
      <template
        v-for="(seri, seriIndex) in serilerDuz"
        :key="`seri-${seriIndex}`"
      >
        <g :style="{ color: seri.renk }">
          <template v-if="tip === 'bar'">
            <rect
              v-for="(deger, index) in seri.degerler"
              :key="`bar-${seriIndex}-${index}`"
              :x="barX(index, seri.degerler.length)"
              :y="barY(deger)"
              :width="barGenislik(seri.degerler.length)"
              :height="barYukseklik(deger)"
              rx="1"
              fill="currentColor"
            />
          </template>
          <template v-else>
            <polygon
              v-if="tip === 'sparkline' && seri.degerler.length > 1"
              :points="alanNoktalari(seri.degerler)"
              fill="currentColor"
              opacity="0.12"
            />
            <polyline
              :points="cizgiNoktalari(seri.degerler)"
              fill="none"
              stroke="currentColor"
              stroke-width="1.6"
              stroke-linecap="round"
              stroke-linejoin="round"
              vector-effect="non-scaling-stroke"
            />
          </template>
        </g>
      </template>
    </svg>
    <span
      v-else
      class="mikro-grafik-bos"
      aria-hidden="true"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  tip: {
    type: String,
    default: 'sparkline',
    validator: (deger) => ['sparkline', 'line', 'bar'].includes(deger)
  },
  etiketler: { type: Array, default: () => [] },
  seriler: { type: Array, default: () => [] },
  yukseklik: { type: Number, default: 48 },
  ariaLabel: { type: String, default: '' },
  renk: { type: String, default: 'var(--accent)' },
  dataTest: { type: String, default: 'mikro-grafik' }
})

const PAD = 3

/** Ham sayı dizisi veya `{ ad, degerler, renk }` serilerini tek biçime getirir. */
const serilerDuz = computed(() => {
  const girdi = Array.isArray(props.seriler) ? props.seriler : []
  if (girdi.length === 0) return []
  if (typeof girdi[0] === 'number') {
    return [{
      ad: '',
      degerler: girdi.map(Number).filter(Number.isFinite),
      renk: props.renk
    }]
  }
  return girdi
    .filter((seri) => seri && Array.isArray(seri.degerler))
    .map((seri, index) => ({
      ad: seri.ad || `seri-${index + 1}`,
      degerler: seri.degerler.map(Number).filter(Number.isFinite),
      renk: seri.renk || props.renk
    }))
    .filter((seri) => seri.degerler.length > 0)
})

const cizilebilir = computed(() => serilerDuz.value.length > 0)

const tumDegerler = computed(() => serilerDuz.value.flatMap((seri) => seri.degerler))

const altSinir = computed(() => Math.min(0, ...tumDegerler.value))
const ustSinir = computed(() => Math.max(0, ...tumDegerler.value))

const oran = (deger) => {
  const alt = altSinir.value
  const ust = ustSinir.value
  if (ust === alt) return 0.5
  return (deger - alt) / (ust - alt)
}

const yKoordinati = (deger) => {
  const kullanilabilir = Math.max(1, props.yukseklik - PAD * 2)
  return PAD + (1 - oran(deger)) * kullanilabilir
}

const cizgiNoktalari = (degerler) => {
  if (degerler.length === 1) {
    const y = yKoordinati(degerler[0])
    return `0,${y} 100,${y}`
  }
  return degerler
    .map((deger, index) => {
      const x = (index / (degerler.length - 1)) * 100
      return `${x.toFixed(2)},${yKoordinati(deger).toFixed(2)}`
    })
    .join(' ')
}

const alanNoktalari = (degerler) => {
  if (degerler.length < 2) return ''
  return `0,${props.yukseklik} ${cizgiNoktalari(degerler)} 100,${props.yukseklik}`
}

const barX = (index, adet) => {
  const genislik = 100 / Math.max(1, adet)
  return index * genislik + genislik * 0.15
}

const barGenislik = (adet) => (100 / Math.max(1, adet)) * 0.7

const barY = (deger) => {
  const sifirY = yKoordinati(0)
  const degerY = yKoordinati(deger)
  return Math.min(sifirY, degerY)
}

const barYukseklik = (deger) => {
  const sifirY = yKoordinati(0)
  return Math.max(1, Math.abs(sifirY - yKoordinati(deger)))
}
</script>

<style scoped>
.mikro-grafik {
  width: 100%;
  min-width: 0;
  color: var(--accent);
}
.mikro-grafik svg {
  display: block;
  width: 100%;
  height: 100%;
  overflow: visible;
}
.mikro-grafik-bos {
  display: block;
  width: 100%;
  height: 1px;
  margin-top: 50%;
  background: var(--border);
}
</style>
