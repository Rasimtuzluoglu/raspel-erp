<template>
  <section
    class="finans-ozet-bandi"
    data-test="finans-ozet-bandi"
    role="group"
    :aria-label="t('dashboard.finansalOzet')"
  >
    <template v-if="yukleniyor && kpi.length === 0">
      <div class="finans-ust">
        <div
          v-for="n in 4"
          :key="`kpi-iskelet-${n}`"
          class="metrik-iskelet"
          aria-hidden="true"
        >
          <span class="metrik-iskelet-etiket" />
          <span class="metrik-iskelet-deger" />
          <span class="metrik-iskelet-alt" />
        </div>
      </div>
      <div
        class="finans-ikincil"
        aria-hidden="true"
      >
        <span
          v-for="n in 4"
          :key="`kpi-alt-iskelet-${n}`"
          class="ikincil-iskelet"
        />
      </div>
    </template>

    <template v-else>
      <!-- Öncelikli 4 metrik: ciro, marj, alacak, verecek -->
      <div class="finans-ust">
        <MetrikKutu
          v-for="oge in birincil"
          :key="oge.anahtar"
          :etiket="etiket(oge)"
          :deger="oge.deger"
          :degisim-yuzde="oge.degisimYuzde"
          :sparkline="oge.sparkline || []"
          :kritik="oge.kritik === true"
          :to="hedef(oge)"
          :bicim="bicim(oge)"
          :para-birimi="paraBirimi"
          :alt-etiket="t('dashboard.kokpitDegisim')"
          :kritik-etiket="t('dashboard.kokpitKritik')"
          :degisim-etiketi="t('dashboard.kokpitDegisim')"
          :data-test="`kpi-${oge.anahtar}`"
        />
      </div>

      <!-- İkincil metrikler: görsel ağırlığı düşük, hâlâ erişilebilir -->
      <div
        v-if="ikincil.length > 0"
        class="finans-ikincil"
        data-test="finans-ikincil"
      >
        <component
          :is="hedef(oge) ? 'router-link' : 'span'"
          v-for="oge in ikincil"
          :key="oge.anahtar"
          class="ikincil-cip"
          :to="hedef(oge) || undefined"
          :data-test="`kpi-alt-${oge.anahtar}`"
        >
          <span class="ikincil-etiket">{{ etiket(oge) }}</span>
          <strong class="ikincil-deger">{{ degerMetni(oge) }}</strong>
          <span
            v-if="oge.degisimYuzde !== null && oge.degisimYuzde !== undefined"
            class="ikincil-degisim"
            :class="trendSinifi(oge.degisimYuzde)"
          >
            {{ bicimliDegisim(oge.degisimYuzde) }}
          </span>
        </component>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import MetrikKutu from './MetrikKutu.vue'
import { formatPara } from '../../../utils/format.js'

const props = defineProps({
  kpi: { type: Array, default: () => [] },
  yukleniyor: { type: Boolean, default: false },
  paraBirimi: { type: String, default: 'TRY' }
})

const { t, te } = useI18n()

const ANAHTARLAR = {
  netNakit: 'dashboard.kokpitNetNakit',
  alacak: 'dashboard.kokpitAlacak',
  verecek: 'dashboard.kokpitVerecek',
  netCalismaSermayesi: 'dashboard.kokpitNetCalismaSermayesi',
  ciro: 'dashboard.kokpitCiro',
  brutKar: 'dashboard.kokpitBrutKar',
  marj: 'dashboard.kokpitMarj',
  vadesiGecen: 'dashboard.kokpitVadesiGecen'
}

/** İlk ekranda öne çıkacak metrikler (kullanıcı önceliği). */
const ONCELIK = ['ciro', 'marj', 'alacak', 'verecek']

const birincil = computed(() => {
  const harita = new Map(props.kpi.map((oge) => [oge.anahtar, oge]))
  const secili = ONCELIK.map((anahtar) => harita.get(anahtar)).filter(Boolean)
  for (const oge of props.kpi) {
    if (secili.length >= 4) break
    if (!secili.includes(oge)) secili.push(oge)
  }
  return secili.slice(0, 4)
})

const ikincil = computed(() => props.kpi.filter((oge) => !birincil.value.includes(oge)))

const etiket = (oge) => {
  const anahtar = ANAHTARLAR[oge?.anahtar]
  return anahtar && te(anahtar) ? t(anahtar) : oge?.etiket || ''
}

const hedef = (oge) => {
  const route = oge?.hedef?.route
  if (!route) return null
  return { path: route, query: oge.hedef.query || {} }
}

const bicim = (oge) => (oge?.anahtar === 'marj' ? 'yuzde' : 'para')

const degerMetni = (oge) => {
  if (oge?.anahtar === 'marj') {
    const sayi = Number(oge?.deger)
    return Number.isFinite(sayi) ? `%${sayi.toFixed(1)}` : '—'
  }
  return formatPara(oge?.deger, props.paraBirimi)
}

const bicimliDegisim = (yuzde) => {
  const sayi = Number(yuzde)
  if (!Number.isFinite(sayi)) return ''
  const isaret = sayi > 0 ? '+' : ''
  return `${isaret}${sayi.toFixed(1)}%`
}

const trendSinifi = (yuzde) => (Number(yuzde) < 0 ? 'ikincil-degisim--dusus' : 'ikincil-degisim--yukseis')
</script>

<style scoped>
.finans-ozet-bandi {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}
.finans-ust {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  min-width: 0;
}
.metrik-iskelet {
  display: flex;
  flex-direction: column;
  gap: 7px;
  min-height: 94px;
  padding: 10px 11px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-card);
}
.metrik-iskelet-etiket,
.metrik-iskelet-deger,
.metrik-iskelet-alt {
  display: block;
  height: 10px;
  border-radius: var(--radius-pill);
  background: var(--bg-muted);
  animation: metrik-pulse 1.4s ease-in-out infinite;
}
.metrik-iskelet-deger {
  width: 76%;
  height: 20px;
}
.metrik-iskelet-alt {
  width: 48%;
}
.finans-ikincil {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}
.ikincil-cip {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  padding: 5px 10px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 11.5px;
  text-decoration: none;
  white-space: nowrap;
  transition:
    border-color var(--dur-fast) var(--ease-standard),
    color var(--dur-fast) var(--ease-standard);
}
.ikincil-cip:hover,
.ikincil-cip:focus-visible {
  border-color: var(--accent-border);
  color: var(--accent);
}
.ikincil-cip:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.ikincil-etiket {
  color: var(--text-muted);
}
.ikincil-deger {
  font-size: 12px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
}
.ikincil-degisim {
  font-size: 10.5px;
  font-weight: 700;
}
.ikincil-degisim--yukseis {
  color: var(--success, #10b981);
}
.ikincil-degisim--dusus {
  color: var(--danger);
}
.ikincil-iskelet {
  width: 118px;
  height: 24px;
  border-radius: 999px;
  background: var(--bg-muted);
  animation: metrik-pulse 1.4s ease-in-out infinite;
}
@keyframes metrik-pulse {
  0%,
  100% {
    opacity: 0.45;
  }
  50% {
    opacity: 0.9;
  }
}
@media (prefers-reduced-motion: reduce) {
  .metrik-iskelet-etiket,
  .metrik-iskelet-deger,
  .metrik-iskelet-alt,
  .ikincil-iskelet {
    animation: none;
  }
}
@media (max-width: 1100px) {
  .finans-ust {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 520px) {
  .finans-ust {
    grid-template-columns: minmax(0, 1fr);
  }
  .ikincil-cip {
    flex: 1 1 100%;
    justify-content: space-between;
  }
}
</style>
