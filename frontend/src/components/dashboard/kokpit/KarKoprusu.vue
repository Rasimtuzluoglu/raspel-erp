<template>
  <section
    class="kar-koprusu"
    data-test="kar-koprusu"
    :aria-label="t('dashboard.kokpitKarKoprusuBaslik')"
  >
    <header class="kopru-ust">
      <div>
        <h3 class="kopru-baslik">
          {{ t('dashboard.kokpitKarKoprusuBaslik') }}
        </h3>
        <p class="kopru-alt">
          {{ t('dashboard.kokpitKarKoprusuAlt') }}
        </p>
      </div>
      <span class="kopru-formul">{{ t('dashboard.kokpitKarKoprusuFormul') }}</span>
    </header>

    <div class="kopru-satirlar">
      <div
        v-for="adim in adimlar"
        :key="adim.anahtar"
        class="kopru-satir"
        data-test="kar-kopru-satir"
      >
        <span class="kopru-etiket">{{ adim.etiket }}</span>
        <div class="kopru-iz">
          <span
            class="kopru-bar"
            :class="[adim.sinif, { 'kopru-bar--negatif': adim.bitis < 0 }]"
            :style="stil(adim)"
          />
        </div>
        <strong
          class="kopru-deger"
          :class="{ negatif: adim.bitis < 0 }"
        >
          <span v-if="adim.isaret">{{ adim.isaret }}</span>{{ formatPara(adim.deger, paraBirimi) }}
        </strong>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatPara } from '../../../utils/format.js'

const props = defineProps({
  ciro: { type: Number, default: 0 },
  maliyet: { type: Number, default: 0 },
  brutKar: { type: Number, default: 0 },
  netKar: { type: Number, default: null },
  paraBirimi: { type: String, default: 'TRY' }
})

const { t } = useI18n()

const netKarVar = computed(() =>
  props.netKar !== null &&
  props.netKar !== undefined &&
  Number.isFinite(Number(props.netKar))
)
const digerGider = computed(() =>
  netKarVar.value ? Math.max(0, Number(props.brutKar) - Number(props.netKar)) : 0
)

const adimlar = computed(() => {
  const liste = [
    {
      anahtar: 'ciro',
      etiket: t('dashboard.kokpitKarKoprusuCiro'),
      deger: Number(props.ciro) || 0,
      baslangic: 0,
      bitis: Number(props.ciro) || 0,
      sinif: 'kopru-bar--ciro',
      isaret: ''
    },
    {
      anahtar: 'maliyet',
      etiket: t('dashboard.kokpitKarKoprusuMaliyet'),
      deger: Number(props.maliyet) || 0,
      baslangic: Number(props.ciro) || 0,
      bitis: (Number(props.ciro) || 0) - (Number(props.maliyet) || 0),
      sinif: 'kopru-bar--maliyet',
      isaret: '−'
    },
    {
      anahtar: 'brut',
      etiket: t('dashboard.kokpitKarKoprusuBrut'),
      deger: Number(props.brutKar) || 0,
      baslangic: 0,
      bitis: Number(props.brutKar) || 0,
      sinif: 'kopru-bar--brut',
      isaret: ''
    }
  ]
  if (netKarVar.value) {
    if (digerGider.value > 0) {
      liste.push({
        anahtar: 'diger',
        etiket: t('dashboard.kokpitKarKoprusuDiger'),
        deger: digerGider.value,
        baslangic: Number(props.brutKar) || 0,
        bitis: Number(props.netKar),
        sinif: 'kopru-bar--diger',
        isaret: '−'
      })
    }
    liste.push({
      anahtar: 'net',
      etiket: t('dashboard.kokpitKarKoprusuNet'),
      deger: Number(props.netKar),
      baslangic: 0,
      bitis: Number(props.netKar),
      sinif: 'kopru-bar--net',
      isaret: ''
    })
  }
  return liste
})

const maxDeger = computed(() =>
  Math.max(
    1,
    Number(props.ciro) || 0,
    Number(props.maliyet) || 0,
    Math.abs(Number(props.brutKar) || 0),
    Math.abs(Number(props.netKar) || 0)
  )
)

const stil = (adim) => {
  const sol = Math.min(adim.baslangic, adim.bitis)
  const genislik = Math.abs(adim.bitis - adim.baslangic)
  return {
    left: `${Math.max(0, (sol / maxDeger.value) * 100)}%`,
    width: `${Math.max(1.5, (genislik / maxDeger.value) * 100)}%`
  }
}
</script>

<style scoped>
.kar-koprusu {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 9px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.02), transparent 45%),
    var(--bg-muted);
}
.kopru-ust {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.kopru-baslik {
  margin: 0;
  font-size: 11px;
  font-weight: 700;
  color: var(--text-primary);
}
.kopru-alt {
  margin: 1px 0 0;
  font-size: 9.5px;
  color: var(--text-muted);
}
.kopru-formul {
  margin-left: auto;
  padding: 2px 5px;
  border: 1px solid var(--border);
  border-radius: var(--radius-xs);
  color: var(--text-muted);
  font-size: 8.5px;
  white-space: nowrap;
}
.kopru-satirlar {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.kopru-satir {
  display: grid;
  grid-template-columns: 74px minmax(0, 1fr) 84px;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.kopru-etiket {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 10px;
  color: var(--text-muted);
}
.kopru-iz {
  position: relative;
  width: 100%;
  height: 12px;
  overflow: hidden;
  border-radius: var(--radius-pill);
  background: var(--bg-secondary);
}
.kopru-bar {
  position: absolute;
  top: 2px;
  bottom: 2px;
  min-width: 2px;
  border-radius: var(--radius-pill);
  background: var(--accent);
  transition:
    left var(--dur-base) var(--ease-standard),
    width var(--dur-base) var(--ease-standard);
}
.kopru-bar--ciro {
  background: var(--accent);
}
.kopru-bar--maliyet,
.kopru-bar--diger {
  background: var(--danger);
  opacity: 0.75;
}
.kopru-bar--brut {
  background: var(--warning);
}
.kopru-bar--net {
  background: var(--success);
}
.kopru-bar--net.kopru-bar--negatif {
  background: var(--danger);
}
.kopru-deger {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: right;
  font-size: 10.5px;
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
}
.kopru-deger.negatif {
  color: var(--danger);
}
@media (max-width: 520px) {
  .kopru-satir {
    grid-template-columns: 62px minmax(0, 1fr) 76px;
  }
  .kopru-formul {
    display: none;
  }
}
@media (prefers-reduced-motion: reduce) {
  .kopru-bar {
    transition: none;
  }
}
</style>
