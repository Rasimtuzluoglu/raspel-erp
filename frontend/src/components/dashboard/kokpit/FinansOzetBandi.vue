<template>
  <section
    class="finans-ozet-bandi"
    data-test="finans-ozet-bandi"
    role="group"
    :aria-label="t('dashboard.finansalOzet')"
  >
    <template v-if="yukleniyor && kpi.length === 0">
      <div
        v-for="n in 8"
        :key="`kpi-iskelet-${n}`"
        class="metrik-iskelet"
        aria-hidden="true"
      >
        <span class="metrik-iskelet-etiket" />
        <span class="metrik-iskelet-deger" />
        <span class="metrik-iskelet-alt" />
      </div>
    </template>

    <template v-else>
      <MetrikKutu
        v-for="oge in kpi"
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
    </template>
  </section>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import MetrikKutu from './MetrikKutu.vue'

defineProps({
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
</script>

<style scoped>
.finans-ozet-bandi {
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  gap: 8px;
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
  .metrik-iskelet-alt {
    animation: none;
  }
}
@media (max-width: 1680px) {
  .finans-ozet-bandi {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
@media (max-width: 900px) {
  .finans-ozet-bandi {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 520px) {
  .finans-ozet-bandi {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
