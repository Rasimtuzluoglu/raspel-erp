<template>
  <component
    :is="to ? 'router-link' : 'div'"
    :to="to || undefined"
    class="metrik-kutu"
    :class="{
      'metrik-kutu--kritik': kritik,
      'metrik-kutu--link': !!to
    }"
    :data-test="dataTest"
    :aria-label="ariaLabel"
  >
    <div class="metrik-kutu-ust">
      <span class="metrik-kutu-etiket">{{ etiket }}</span>
      <i
        v-if="kritik"
        class="pi pi-exclamation-triangle metrik-kutu-kritik"
        :title="kritikEtiket"
        :aria-label="kritikEtiket"
        role="img"
      />
    </div>

    <strong class="metrik-kutu-deger gizli-veri">{{ bicimliDeger }}</strong>

    <div class="metrik-kutu-alt">
      <span
        v-if="degisimVar"
        class="metrik-kutu-degisim"
        :class="degisimSinifi"
      >
        <i
          :class="degisimIkonu"
          aria-hidden="true"
        />
        {{ degisimMetni }}
      </span>
      <span
        v-else
        class="metrik-kutu-degisim metrik-kutu-degisim--notr"
        aria-hidden="true"
      >—</span>
      <span
        v-if="altEtiket"
        class="metrik-kutu-alt-etiket"
      >{{ altEtiket }}</span>
    </div>

    <MikroGrafik
      v-if="sparklineVar"
      :seriler="sparkline"
      :yukseklik="32"
      :aria-label="`${etiket} mini grafik`"
      class="metrik-kutu-sparkline"
    />
  </component>
</template>

<script setup>
import { computed } from 'vue'
import { formatPara } from '../../../utils/format.js'
import MikroGrafik from './MikroGrafik.vue'

const props = defineProps({
  etiket: { type: String, required: true },
  deger: { type: [Number, String], default: null },
  degisimYuzde: { type: [Number, String], default: null },
  sparkline: { type: Array, default: () => [] },
  kritik: { type: Boolean, default: false },
  ters: { type: Boolean, default: false },
  to: { type: [Object, String], default: null },
  bicim: { type: String, default: 'para' },
  paraBirimi: { type: String, default: 'TRY' },
  altEtiket: { type: String, default: '' },
  kritikEtiket: { type: String, default: 'Kritik' },
  degisimEtiketi: { type: String, default: '' },
  dataTest: { type: String, default: 'metrik-kutu' }
})

const sayisalDeger = computed(() => {
  if (props.deger === null || props.deger === undefined || props.deger === '') return null
  const sayi = Number(props.deger)
  return Number.isFinite(sayi) ? sayi : null
})

const bicimliDeger = computed(() => {
  if (sayisalDeger.value === null) return '—'
  if (props.bicim === 'yuzde') {
    return `${sayisalDeger.value.toLocaleString('tr-TR', { maximumFractionDigits: 1 })}%`
  }
  if (props.bicim === 'sayi') {
    return sayisalDeger.value.toLocaleString('tr-TR', { maximumFractionDigits: 0 })
  }
  return formatPara(sayisalDeger.value, props.paraBirimi)
})

const degisimSayisi = computed(() => {
  if (props.degisimYuzde === null || props.degisimYuzde === undefined) return null
  const sayi = Number(props.degisimYuzde)
  return Number.isFinite(sayi) ? sayi : null
})

const degisimVar = computed(() => degisimSayisi.value !== null)

const degisimMetni = computed(() => {
  if (degisimSayisi.value === null) return ''
  const isaret = degisimSayisi.value > 0 ? '+' : ''
  return `${isaret}${degisimSayisi.value.toLocaleString('tr-TR', { maximumFractionDigits: 1 })}%`
})

const degisimSinifi = computed(() => {
  if (degisimSayisi.value === null || degisimSayisi.value === 0) return 'metrik-kutu-degisim--notr'
  const olumlu = props.ters ? degisimSayisi.value < 0 : degisimSayisi.value > 0
  return olumlu ? 'metrik-kutu-degisim--iyi' : 'metrik-kutu-degisim--kotu'
})

const degisimIkonu = computed(() => {
  if (degisimSayisi.value === null || degisimSayisi.value === 0) return 'pi pi-minus'
  return degisimSayisi.value > 0 ? 'pi pi-arrow-up' : 'pi pi-arrow-down'
})

const sparklineVar = computed(() =>
  Array.isArray(props.sparkline) && props.sparkline.length > 1
)

const ariaLabel = computed(() => {
  const parcalar = [`${props.etiket}: ${bicimliDeger.value}`]
  const degisimEtiketi = props.altEtiket || props.degisimEtiketi
  if (degisimVar.value && degisimEtiketi) {
    parcalar.push(`${degisimEtiketi} ${degisimMetni.value}`)
  }
  return parcalar.join(', ')
})
</script>

<style scoped>
.metrik-kutu {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  padding: 10px 11px 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  color: inherit;
  text-decoration: none;
  transition:
    border-color var(--dur-fast) var(--ease-standard),
    background var(--dur-fast) var(--ease-standard);
}
.metrik-kutu--link:hover,
.metrik-kutu--link:focus-visible {
  border-color: var(--accent-border);
  background: var(--bg-secondary);
}
.metrik-kutu--link:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.metrik-kutu--kritik {
  border-color: var(--danger-border);
}
.metrik-kutu--kritik::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 2px;
  border-radius: var(--radius-md) 0 0 var(--radius-md);
  background: var(--danger);
}
.metrik-kutu-ust {
  display: flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
}
.metrik-kutu-etiket {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.01em;
  color: var(--text-muted);
}
.metrik-kutu-kritik {
  flex-shrink: 0;
  font-size: 10px;
  color: var(--danger);
}
.metrik-kutu-deger {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 21px;
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.02em;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.metrik-kutu-alt {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  min-height: 16px;
}
.metrik-kutu-degisim {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.metrik-kutu-degisim i {
  font-size: 8px;
}
.metrik-kutu-degisim--iyi {
  color: var(--success);
}
.metrik-kutu-degisim--kotu {
  color: var(--danger);
}
.metrik-kutu-degisim--notr {
  color: var(--text-muted);
}
.metrik-kutu-alt-etiket {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 10.5px;
  color: var(--text-muted);
}
.metrik-kutu-sparkline {
  margin-top: 2px;
  opacity: 0.9;
}
@media (max-width: 600px) {
  .metrik-kutu {
    padding: 9px 10px 7px;
  }
  .metrik-kutu-deger {
    font-size: 18px;
  }
}
</style>
