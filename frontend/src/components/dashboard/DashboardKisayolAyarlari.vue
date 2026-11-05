<template>
  <div
    v-if="visible"
    class="db-modal-katman"
    data-test="kisayol-ayar-katman"
    @click.self="kapat"
  >
    <div
      ref="panel"
      class="db-modal db-kisayol-ayar"
      role="dialog"
      aria-modal="true"
      aria-labelledby="db-kisayol-ayar-baslik"
      tabindex="-1"
      data-test="kisayol-ayarlari"
      @keydown="tusYonet"
    >
      <header class="db-modal-ust">
        <div class="db-modal-baslik-alan">
          <h2
            id="db-kisayol-ayar-baslik"
            class="db-modal-baslik"
          >
            {{ t('dashboard.kisayolAyarBaslik') }}
          </h2>
          <p class="db-modal-alt">
            {{ t('dashboard.kisayolAyarAlt') }}
          </p>
        </div>
        <button
          type="button"
          class="db-modal-kapat"
          :aria-label="t('common.close')"
          :title="t('common.close')"
          data-test="kisayol-ayar-kapat"
          @click="kapat"
        >
          <i
            class="pi pi-times"
            aria-hidden="true"
          />
        </button>
      </header>

      <p
        class="kisayol-ayar-sayac"
        :class="{ 'kisayol-ayar-sayac--dolu': taslak.length >= MAX_KISAYOL }"
        data-test="kisayol-ayar-sayac"
      >
        {{ t('dashboard.kisayolAyarSayac', { secili: taslak.length, maks: MAX_KISAYOL }) }}
      </p>

      <p
        v-if="hata"
        class="kisayol-ayar-hata"
        role="alert"
        data-test="kisayol-ayar-hata"
      >
        {{ hata }}
      </p>

      <ul
        class="kisayol-ayar-liste"
        role="group"
        :aria-label="t('dashboard.kisayolAyarBaslik')"
      >
        <li
          v-for="k in katalog"
          :key="k.key"
          class="kisayol-ayar-satir"
        >
          <label
            class="kisayol-ayar-oge"
            :class="{ 'kisayol-ayar-oge--secili': seciliMi(k.key), 'kisayol-ayar-oge--pasif': !seciliMi(k.key) && doluMu }"
          >
            <input
              type="checkbox"
              class="kisayol-ayar-onay"
              :value="k.key"
              :checked="seciliMi(k.key)"
              :disabled="!seciliMi(k.key) && doluMu"
              :data-test="`kisayol-secim-${k.key}`"
              @change="degistir(k.key, $event.target.checked)"
            >
            <span class="kisayol-ayar-ikon"><i
              :class="k.ikon"
              aria-hidden="true"
            /></span>
            <span class="kisayol-ayar-etiket">{{ t(k.anahtar) }}</span>
            <span
              v-if="k.admin"
              class="kisayol-ayar-rozet"
            >{{ t('dashboard.kisayolAdmin') }}</span>
          </label>
        </li>
      </ul>

      <div class="db-modal-eylemler">
        <button
          type="button"
          class="db-modal-btn"
          data-test="kisayol-sifirla"
          @click="sifirla"
        >
          <i
            class="pi pi-replay"
            aria-hidden="true"
          />
          {{ t('dashboard.kisayolSifirla') }}
        </button>
        <span class="db-modal-esnek" />
        <button
          type="button"
          class="db-modal-btn"
          data-test="kisayol-iptal"
          @click="kapat"
        >
          {{ t('common.cancel') }}
        </button>
        <button
          type="button"
          class="db-modal-btn db-modal-btn--birincil"
          data-test="kisayol-kaydet"
          @click="kaydet"
        >
          {{ t('dashboard.kisayolKaydet') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  MAX_KISAYOL,
  VARSAYILAN_KISAYOLLAR,
  kisayolKatalogu,
  kisayollariNormalize,
  kisayollariYaz
} from '../../utils/dashboardKisayollar.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  secim: { type: Array, default: () => [] },
  isAdmin: { type: Boolean, default: false }
})

const emit = defineEmits(['update:visible', 'kaydet', 'sifirla'])

const { t } = useI18n()

const panel = ref(null)
const taslak = ref([])
const hata = ref('')

let oncekiOdak = null

const katalog = computed(() => kisayolKatalogu(props.isAdmin))
const doluMu = computed(() => taslak.value.length >= MAX_KISAYOL)

const seciliMi = (key) => taslak.value.includes(key)

const taslakAyarla = (keys) => {
  taslak.value = kisayollariNormalize(keys, props.isAdmin)
}

const degistir = (key, secili) => {
  hata.value = ''
  const mevcut = new Set(taslak.value)
  if (secili) {
    if (mevcut.size >= MAX_KISAYOL) {
      hata.value = t('dashboard.kisayolAyarSinir', { maks: MAX_KISAYOL })
      return
    }
    mevcut.add(key)
  } else {
    mevcut.delete(key)
  }
  // Sira her zaman katalog sirasidir.
  taslak.value = katalog.value.map((k) => k.key).filter((k) => mevcut.has(k))
}

const sifirla = () => {
  hata.value = ''
  taslak.value = [...VARSAYILAN_KISAYOLLAR]
  emit('sifirla', [...taslak.value])
}

const kaydet = () => {
  if (!taslak.value.length) {
    hata.value = t('dashboard.kisayolEnAzBir')
    return
  }
  kisayollariYaz(taslak.value, props.isAdmin)
  emit('kaydet', [...taslak.value])
}

const kapat = () => emit('update:visible', false)

const odaklanabilirler = () =>
  Array.from(
    panel.value?.querySelectorAll?.(
      'button:not(:disabled), [href], input:not(:disabled), textarea:not(:disabled), select:not(:disabled), [tabindex]:not([tabindex="-1"])'
    ) || []
  )

const tusYonet = (olay) => {
  if (olay.key === 'Escape') {
    olay.stopPropagation()
    kapat()
    return
  }
  if (olay.key !== 'Tab') return
  const ogeler = odaklanabilirler()
  if (!ogeler.length) return
  const ilk = ogeler[0]
  const son = ogeler[ogeler.length - 1]
  if (olay.shiftKey && document.activeElement === ilk) {
    olay.preventDefault()
    son.focus()
  } else if (!olay.shiftKey && document.activeElement === son) {
    olay.preventDefault()
    ilk.focus()
  }
}

watch(
  () => props.visible,
  (acik) => {
    if (acik) {
      oncekiOdak = typeof document !== 'undefined' ? document.activeElement : null
      hata.value = ''
      taslakAyarla(props.secim)
      nextTick(() => panel.value?.focus?.())
    } else if (oncekiOdak && typeof oncekiOdak.focus === 'function') {
      oncekiOdak.focus?.()
      oncekiOdak = null
    }
  },
  { immediate: true }
)

watch(
  () => props.isAdmin,
  () => {
    if (props.visible) taslakAyarla(taslak.value)
  }
)
</script>

<style scoped>
/* Kabuk (.db-modal-katman / .db-modal / baslik / eylemler) dashboard.css icinde. */
.db-kisayol-ayar {
  gap: 10px;
}

.kisayol-ayar-sayac {
  margin: 0;
  font-size: 11.5px;
  font-weight: 700;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}
.kisayol-ayar-sayac--dolu {
  color: var(--warning);
}
.kisayol-ayar-hata {
  margin: 0;
  font-size: 11.5px;
  color: var(--danger);
}

.kisayol-ayar-liste {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}
.kisayol-ayar-satir {
  min-width: 0;
}
.kisayol-ayar-oge {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 40px;
  padding: 6px 9px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-muted);
  color: var(--text-secondary);
  cursor: pointer;
  transition:
    border-color var(--dur-base, 0.2s) var(--ease-standard, ease),
    background var(--dur-base, 0.2s) var(--ease-standard, ease),
    color var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.kisayol-ayar-oge:hover {
  border-color: var(--accent-border);
  color: var(--text-primary);
}
.kisayol-ayar-oge--secili {
  border-color: var(--accent-border);
  background: var(--accent-soft);
  color: var(--text-primary);
}
.kisayol-ayar-oge--pasif {
  opacity: 0.58;
  cursor: not-allowed;
}
.kisayol-ayar-onay {
  flex-shrink: 0;
  width: 15px;
  height: 15px;
  accent-color: var(--accent);
  cursor: inherit;
}
.kisayol-ayar-onay:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.kisayol-ayar-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border-radius: 7px;
  background: color-mix(in srgb, currentcolor 14%, transparent);
  font-size: 11px;
}
.kisayol-ayar-etiket {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  font-weight: 600;
}
.kisayol-ayar-rozet {
  margin-left: auto;
  flex-shrink: 0;
  padding: 1px 6px;
  border-radius: 999px;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 9.5px;
  font-weight: 800;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}

@media (max-width: 560px) {
  .kisayol-ayar-liste {
    grid-template-columns: minmax(0, 1fr);
  }
}
@media (prefers-reduced-motion: reduce) {
  .kisayol-ayar-oge {
    transition: none;
  }
}
</style>
