<template>
  <div
    class="buyuk-grafik"
    :data-test="dataTest"
    :style="{ '--grafik-yukseklik': `${yukseklik}px` }"
    role="img"
    :aria-label="ariaLabel"
  >
    <div
      v-if="seriler.length > 1"
      class="buyuk-grafik-legend"
      aria-hidden="true"
    >
      <span
        v-for="seri in seriler"
        :key="seri.ad"
        class="buyuk-grafik-legend-oge"
      >
        <i :style="{ background: seri.renk }" /> {{ seri.ad }}
      </span>
    </div>

    <div class="buyuk-grafik-govde">
      <div
        class="buyuk-grafik-y"
        aria-hidden="true"
      >
        <span
          v-for="(tik, index) in yTikleri"
          :key="`y-${index}`"
          class="buyuk-grafik-y-etiket"
        >{{ yEtiketi(tik) }}</span>
      </div>

      <div
        class="buyuk-grafik-cizim"
        @mouseleave="aktifIndex = null"
      >
        <svg
          viewBox="0 0 100 100"
          preserveAspectRatio="none"
          aria-hidden="true"
        >
          <line
            v-for="(tik, index) in yTikleri"
            :key="`grid-${index}`"
            x1="0"
            x2="100"
            :y1="yOrani(tik)"
            :y2="yOrani(tik)"
            class="buyuk-grafik-grid"
            vector-effect="non-scaling-stroke"
          />

          <rect
            v-if="hedefBand"
            x="0"
            width="100"
            :y="yOrani(hedefBand.ust)"
            :height="Math.abs(yOrani(hedefBand.alt) - yOrani(hedefBand.ust))"
            class="buyuk-grafik-bant"
          />

          <line
            v-if="sifirCizgisi && sifirGorunur"
            x1="0"
            x2="100"
            :y1="yOrani(0)"
            :y2="yOrani(0)"
            class="buyuk-grafik-sifir"
            vector-effect="non-scaling-stroke"
          />

          <g
            v-for="(seri, seriIndex) in seriler"
            :key="`seri-${seri.ad}`"
            :style="{ color: seri.renk }"
          >
            <template v-if="tip === 'bar'">
              <rect
                v-for="(deger, index) in seri.degerler"
                :key="`bar-${seriIndex}-${index}`"
                :x="barX(index, seriIndex)"
                :y="Math.min(yOrani(deger), yOrani(0))"
                :width="barGenislik"
                :height="Math.abs(yOrani(deger) - yOrani(0))"
                rx="0.7"
                fill="currentColor"
              />
            </template>
            <template v-else>
              <polygon
                v-if="seri.dolgu"
                :points="alanNoktalari(seri.degerler)"
                fill="currentColor"
                opacity="0.10"
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

          <rect
            v-for="(_, index) in etiketler"
            :key="`hit-${index}`"
            :x="(index * 100) / Math.max(1, etiketler.length)"
            :width="100 / Math.max(1, etiketler.length)"
            y="0"
            height="100"
            fill="transparent"
            @mouseenter="aktifIndex = index"
          />
        </svg>

        <div
          v-if="aktifIndex !== null"
          class="buyuk-grafik-ipucu"
          :style="{ left: ipucuKonum }"
          aria-hidden="true"
        >
          <strong>{{ etiketler[aktifIndex] }}</strong>
          <span
            v-for="seri in seriler"
            :key="`ipucu-${seri.ad}`"
          >
            <i :style="{ background: seri.renk }" />
            {{ seri.ad }}: {{ formatDeger(seri.degerler[aktifIndex]) }}
          </span>
        </div>
      </div>
    </div>

    <div class="buyuk-grafik-x">
      <span
        v-for="(etiket, index) in etiketler"
        :key="`x-${index}`"
        :style="{ left: xKonumu(index) }"
        :class="{
          'buyuk-grafik-x--gizli': !etiketGoster(index),
          'buyuk-grafik-x--ilk': index === 0,
          'buyuk-grafik-x--son': index === etiketler.length - 1
        }"
      >{{ etiket }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { formatPara } from '../../../utils/format.js'

const props = defineProps({
  etiketler: { type: Array, default: () => [] },
  seriler: { type: Array, default: () => [] },
  tip: { type: String, default: 'line', validator: (d) => ['line', 'bar'].includes(d) },
  yukseklik: { type: Number, default: 190 },
  paraBirimi: { type: String, default: 'TRY' },
  hedefBand: { type: Object, default: null },
  sifirCizgisi: { type: Boolean, default: false },
  ariaLabel: { type: String, default: '' },
  dataTest: { type: String, default: 'buyuk-grafik' }
})

const aktifIndex = ref(null)

const serilerDuz = computed(() => (Array.isArray(props.seriler) ? props.seriler.filter((s) => s && Array.isArray(s.degerler)) : []))

const tumDegerler = computed(() => {
  const degerler = serilerDuz.value.flatMap((seri) => seri.degerler.map(Number).filter(Number.isFinite))
  if (props.hedefBand) {
    degerler.push(Number(props.hedefBand.alt) || 0, Number(props.hedefBand.ust) || 0)
  }
  if (props.sifirCizgisi || props.tip === 'bar') degerler.push(0)
  return degerler.length > 0 ? degerler : [0, 1]
})

const altSinir = computed(() => Math.min(...tumDegerler.value))
const ustSinir = computed(() => Math.max(...tumDegerler.value))
const aralik = computed(() => (ustSinir.value === altSinir.value ? 1 : ustSinir.value - altSinir.value))

const yOrani = (deger) => {
  const sayi = Number(deger)
  if (!Number.isFinite(sayi)) return 50
  return Math.max(0, Math.min(100, 100 - ((sayi - altSinir.value) / aralik.value) * 100))
}

const yTikleri = computed(() => {
  const adet = 5
  return Array.from({ length: adet }, (_, i) => ustSinir.value - (aralik.value * i) / (adet - 1))
})

const sifirGorunur = computed(() => altSinir.value < 0 && ustSinir.value > 0)

const noktaX = (index, adet) => {
  if (adet <= 1) return 50
  return (index / (adet - 1)) * 100
}

const cizgiNoktalari = (degerler) => {
  const adet = degerler.length
  return degerler
    .map((deger, index) => `${noktaX(index, adet).toFixed(2)},${yOrani(deger).toFixed(2)}`)
    .join(' ')
}

const alanNoktalari = (degerler) => {
  const adet = degerler.length
  if (adet < 2) return ''
  const taban = yOrani(0)
  return `0,${taban.toFixed(2)} ${cizgiNoktalari(degerler)} 100,${taban.toFixed(2)}`
}

const barGenislik = computed(() => {
  const adet = Math.max(1, props.etiketler.length)
  const seriSayisi = Math.max(1, serilerDuz.value.length)
  return (100 / adet) * 0.72 / seriSayisi
})

const barX = (index, seriIndex) => {
  const adet = Math.max(1, props.etiketler.length)
  const grup = 100 / adet
  return index * grup + grup * 0.14 + seriIndex * barGenislik.value
}

const xKonumu = (index) => {
  const adet = Math.max(1, props.etiketler.length)
  if (props.tip === 'bar') {
    return `${((index + 0.5) / adet) * 100}%`
  }
  return adet <= 1 ? '50%' : `${(index / (adet - 1)) * 100}%`
}

const etiketGoster = (index) => {
  const adet = props.etiketler.length
  if (adet <= 12) return true
  const adim = Math.ceil(adet / 6)
  return index % adim === 0 || index === adet - 1
}

const ipucuKonum = computed(() => {
  if (aktifIndex.value === null) return '0%'
  const adet = Math.max(1, props.etiketler.length)
  const oran = props.tip === 'bar'
    ? (aktifIndex.value + 0.5) / adet
    : (adet <= 1 ? 0.5 : aktifIndex.value / (adet - 1))
  return `${Math.max(6, Math.min(94, oran * 100))}%`
})

const formatDeger = (deger) => {
  const sayi = Number(deger)
  return Number.isFinite(sayi) ? formatPara(sayi, props.paraBirimi) : '—'
}

const yEtiketi = (deger) => {
  const sayi = Number(deger)
  if (!Number.isFinite(sayi)) return ''
  return new Intl.NumberFormat('tr-TR', {
    notation: 'compact',
    maximumFractionDigits: 1
  }).format(sayi)
}
</script>

<style scoped>
.buyuk-grafik {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
  height: var(--grafik-yukseklik);
  min-width: 0;
}
.buyuk-grafik-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 10.5px;
  color: var(--text-muted);
}
.buyuk-grafik-legend-oge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.buyuk-grafik-legend-oge i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}
.buyuk-grafik-govde {
  display: flex;
  flex: 1;
  min-height: 0;
  gap: 6px;
}
.buyuk-grafik-y {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  width: 44px;
  flex-shrink: 0;
  padding: 1px 0 1px;
  text-align: right;
  font-size: 9.5px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}
.buyuk-grafik-cizim {
  position: relative;
  flex: 1;
  min-width: 0;
  min-height: 0;
}
.buyuk-grafik-cizim svg {
  display: block;
  width: 100%;
  height: 100%;
  overflow: visible;
}
.buyuk-grafik-grid {
  stroke: var(--border);
  stroke-opacity: 0.45;
  stroke-width: 1;
  stroke-dasharray: 2 4;
}
.buyuk-grafik-bant {
  fill: var(--success-soft);
  opacity: 0.55;
}
.buyuk-grafik-sifir {
  stroke: var(--text-muted);
  stroke-opacity: 0.7;
  stroke-width: 1;
  stroke-dasharray: 4 3;
}
.buyuk-grafik-ipucu {
  position: absolute;
  top: 4px;
  z-index: 2;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 120px;
  padding: 6px 8px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  color: var(--text-secondary);
  box-shadow: var(--elev-2);
  font-size: 10.5px;
  pointer-events: none;
  transform: translateX(-50%);
  white-space: nowrap;
}
.buyuk-grafik-ipucu strong {
  color: var(--text-primary);
  font-size: 11px;
}
.buyuk-grafik-ipucu span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.buyuk-grafik-ipucu i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}
.buyuk-grafik-x {
  position: relative;
  height: 16px;
  margin-left: 50px;
  flex-shrink: 0;
}
.buyuk-grafik-x span {
  position: absolute;
  top: 0;
  transform: translateX(-50%);
  font-size: 9.5px;
  color: var(--text-muted);
  white-space: nowrap;
}
.buyuk-grafik-x--gizli {
  opacity: 0;
}
.buyuk-grafik-x span.buyuk-grafik-x--ilk {
  transform: none;
}
.buyuk-grafik-x span.buyuk-grafik-x--son {
  transform: translateX(-100%);
}
@media (max-width: 640px) {
  .buyuk-grafik-y {
    width: 34px;
    font-size: 8.5px;
  }
  .buyuk-grafik-x {
    margin-left: 40px;
  }
}
</style>
