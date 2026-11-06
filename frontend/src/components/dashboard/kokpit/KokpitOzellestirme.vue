<template>
  <div
    class="ozellestirme-katman"
    data-test="kokpit-ozellestirme"
    @click.self="emit('kapat')"
  >
    <aside
      class="ozellestirme-panel"
      role="dialog"
      aria-modal="true"
      aria-labelledby="kokpit-ozellestirme-baslik"
    >
      <header class="ozellestirme-ust">
        <div>
          <h2
            id="kokpit-ozellestirme-baslik"
            class="ozellestirme-baslik"
          >
            {{ t('dashboard.kokpitOzellestirmeBaslik') }}
          </h2>
          <p class="ozellestirme-alt">
            {{ t('dashboard.kokpitOzellestirmeAltBaslik') }}
          </p>
        </div>
        <button
          type="button"
          class="ozellestirme-kapat"
          :aria-label="t('dashboard.kokpitOzellestirmeKapat')"
          data-test="ozellestirme-kapat"
          @click="emit('kapat')"
        >
          <i
            class="pi pi-times"
            aria-hidden="true"
          />
        </button>
      </header>

      <ul class="ozellestirme-liste">
        <li
          v-for="(anahtar, index) in siraliBolumler"
          :key="anahtar"
          class="ozellestirme-satir"
          :data-test="`ozellestirme-satir-${anahtar}`"
        >
          <label class="ozellestirme-gorunurluk">
            <input
              type="checkbox"
              :checked="gorunurMu(anahtar)"
              :disabled="gorunurMu(anahtar) && gorunurBolumler.length <= 1"
              :data-test="`ozellestirme-gorunurluk-${anahtar}`"
              @change="gorunurlukDegistir(anahtar, $event.target.checked)"
            >
            <span class="ozellestirme-etiket">{{ bolumEtiketi(anahtar) }}</span>
          </label>

          <div class="ozellestirme-sira">
            <button
              type="button"
              class="ozellestirme-ok"
              :disabled="index === 0"
              :aria-label="t('dashboard.kokpitOzellestirmeYukari')"
              :title="t('dashboard.kokpitOzellestirmeYukari')"
              :data-test="`ozellestirme-yukari-${anahtar}`"
              @click="tasi(index, -1)"
            >
              <i
                class="pi pi-arrow-up"
                aria-hidden="true"
              />
            </button>
            <button
              type="button"
              class="ozellestirme-ok"
              :disabled="index === siraliBolumler.length - 1"
              :aria-label="t('dashboard.kokpitOzellestirmeAsagi')"
              :title="t('dashboard.kokpitOzellestirmeAsagi')"
              :data-test="`ozellestirme-asagi-${anahtar}`"
              @click="tasi(index, 1)"
            >
              <i
                class="pi pi-arrow-down"
                aria-hidden="true"
              />
            </button>
          </div>
        </li>
      </ul>

      <p
        v-if="gorunurBolumler.length <= 1"
        class="ozellestirme-uyari"
        data-test="ozellestirme-en-az-bir"
      >
        <i
          class="pi pi-info-circle"
          aria-hidden="true"
        />
        {{ t('dashboard.kokpitOzellestirmeEnAzBir') }}
      </p>

      <footer class="ozellestirme-alt-bar">
        <button
          type="button"
          class="ozellestirme-buton"
          :disabled="kaydediliyor"
          data-test="ozellestirme-sifirla"
          @click="emit('sifirla')"
        >
          {{ t('dashboard.kokpitOzellestirmeSifirla') }}
        </button>
        <button
          type="button"
          class="ozellestirme-buton ozellestirme-buton--ana"
          :disabled="kaydediliyor"
          data-test="ozellestirme-kaydet"
          @click="emit('kaydet')"
        >
          <i
            v-if="kaydediliyor"
            class="pi pi-spin pi-spinner"
            aria-hidden="true"
          />
          {{ t('dashboard.kokpitOzellestirmeKaydet') }}
        </button>
      </footer>
    </aside>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  bolumler: { type: Array, default: () => [] },
  gorunurBolumler: { type: Array, default: () => [] },
  kaydediliyor: { type: Boolean, default: false }
})

const emit = defineEmits(['kapat', 'gorunurluk-degistir', 'sira-degistir', 'sifirla', 'kaydet'])
const { t } = useI18n()

const BASLIKLAR = {
  nakit: 'dashboard.kokpitNakitBaslik',
  alacakVerecek: 'dashboard.kokpitAlacakVerecekBaslik',
  karMarj: 'dashboard.kokpitKarMarjBaslik',
  faturaSiparis: 'dashboard.kokpitFaturaSiparisBaslik'
}

const siraliBolumler = computed(() => {
  const gorunur = props.gorunurBolumler.filter((anahtar) => props.bolumler.includes(anahtar))
  const gizli = props.bolumler.filter((anahtar) => !gorunur.includes(anahtar))
  return [...gorunur, ...gizli]
})

const bolumEtiketi = (anahtar) => t(BASLIKLAR[anahtar] || anahtar)
const gorunurMu = (anahtar) => props.gorunurBolumler.includes(anahtar)

const gorunurlukDegistir = (anahtar, gorunur) => {
  if (!gorunur && props.gorunurBolumler.length <= 1) return
  emit('gorunurluk-degistir', anahtar, gorunur)
}

const tasi = (index, yon) => {
  const hedef = index + yon
  if (hedef < 0 || hedef >= siraliBolumler.value.length) return
  const sira = [...siraliBolumler.value]
  const [tasinan] = sira.splice(index, 1)
  sira.splice(hedef, 0, tasinan)
  emit('sira-degistir', sira.filter((anahtar) => gorunurMu(anahtar)))
}

const tusKacisi = (olay) => {
  if (olay?.key === 'Escape') emit('kapat')
}

onMounted(() => {
  if (typeof window !== 'undefined') window.addEventListener('keydown', tusKacisi)
})

onBeforeUnmount(() => {
  if (typeof window !== 'undefined') window.removeEventListener('keydown', tusKacisi)
})
</script>

<style scoped>
.ozellestirme-katman {
  position: fixed;
  inset: 0;
  z-index: var(--z-modal);
  display: flex;
  justify-content: flex-end;
  background: rgba(2, 6, 12, 0.55);
  backdrop-filter: blur(2px);
}
.ozellestirme-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: min(380px, 92vw);
  height: 100%;
  padding: 16px;
  overflow-y: auto;
  border-left: 1px solid var(--border);
  background: var(--bg-card);
  box-shadow: var(--elev-2);
}
.ozellestirme-ust {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border);
}
.ozellestirme-baslik {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}
.ozellestirme-alt {
  margin: 2px 0 0;
  font-size: 11px;
  color: var(--text-muted);
}
.ozellestirme-kapat {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  margin-left: auto;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
}
.ozellestirme-kapat:hover,
.ozellestirme-kapat:focus-visible {
  border-color: var(--accent-border);
  color: var(--accent);
}
.ozellestirme-kapat:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}
.ozellestirme-liste {
  display: flex;
  flex-direction: column;
  gap: 5px;
  margin: 0;
  padding: 0;
  list-style: none;
}
.ozellestirme-satir {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-muted);
}
.ozellestirme-gorunurluk {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  cursor: pointer;
}
.ozellestirme-gorunurluk input {
  width: 15px;
  height: 15px;
  accent-color: var(--accent);
  cursor: pointer;
}
.ozellestirme-etiket {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--text-secondary);
}
.ozellestirme-sira {
  display: inline-flex;
  gap: 2px;
  margin-left: auto;
}
.ozellestirme-ok {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: 1px solid var(--border);
  border-radius: var(--radius-xs);
  background: transparent;
  color: var(--text-muted);
  font-size: 9px;
  cursor: pointer;
}
.ozellestirme-ok:hover:not(:disabled),
.ozellestirme-ok:focus-visible:not(:disabled) {
  border-color: var(--accent-border);
  color: var(--accent);
}
.ozellestirme-ok:disabled {
  opacity: 0.35;
  cursor: default;
}
.ozellestirme-ok:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}
.ozellestirme-uyari {
  display: flex;
  align-items: center;
  gap: 5px;
  margin: 0;
  padding: 7px 8px;
  border: 1px solid var(--warning-border);
  border-radius: var(--radius-sm);
  background: var(--warning-soft);
  color: var(--warning);
  font-size: 11px;
}
.ozellestirme-alt-bar {
  display: flex;
  justify-content: flex-end;
  gap: 7px;
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px solid var(--border);
}
.ozellestirme-buton {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 11px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
}
.ozellestirme-buton:hover:not(:disabled),
.ozellestirme-buton:focus-visible:not(:disabled) {
  border-color: var(--accent-border);
  color: var(--accent);
}
.ozellestirme-buton:disabled {
  opacity: 0.6;
  cursor: default;
}
.ozellestirme-buton--ana {
  border-color: var(--accent-border);
  background: var(--accent);
  color: var(--accent-contrast);
}
.ozellestirme-buton--ana:hover:not(:disabled),
.ozellestirme-buton--ana:focus-visible:not(:disabled) {
  background: var(--accent-hover);
  color: var(--accent-contrast);
}
.ozellestirme-buton:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
</style>
