<template>
  <div
    v-if="visible"
    class="onay-katman"
    data-test="kokpit-onay-katman"
    @click.self="kapat"
  >
    <section
      ref="panel"
      class="onay-panel"
      role="dialog"
      aria-modal="true"
      aria-labelledby="kokpit-onay-baslik"
      tabindex="-1"
      @keydown.esc.stop.prevent="kapat"
    >
      <header class="onay-ust">
        <div>
          <h2
            id="kokpit-onay-baslik"
            class="onay-baslik"
          >
            {{ t('dashboard.kokpitOnayBaslik') }}
          </h2>
          <p class="onay-alt">
            {{ t('dashboard.kokpitOnayAlt') }}
          </p>
        </div>
        <button
          type="button"
          class="onay-kapat"
          :aria-label="t('dashboard.kokpitOnayKapat')"
          data-test="kokpit-onay-kapat"
          @click="kapat"
        >
          <i
            class="pi pi-times"
            aria-hidden="true"
          />
        </button>
      </header>

      <div
        v-if="yukleniyor && kayitlar.length === 0"
        class="onay-iskelet"
        aria-hidden="true"
      >
        <span
          v-for="i in 3"
          :key="`onay-iskelet-${i}`"
          class="onay-iskelet-satir"
        />
      </div>

      <div
        v-else-if="hata"
        class="onay-hata"
        role="alert"
      >
        <span>{{ hata }}</span>
        <button
          type="button"
          class="onay-buton"
          @click="yukle"
        >
          {{ t('dashboard.gorevYenidenDene') }}
        </button>
      </div>

      <p
        v-else-if="kayitlar.length === 0"
        class="onay-bos"
        data-test="kokpit-onay-bos"
      >
        <i
          class="pi pi-check-circle"
          aria-hidden="true"
        />
        {{ t('dashboard.kokpitOnayYok') }}
      </p>

      <ul
        v-else
        class="onay-liste"
      >
        <li
          v-for="kayit in kayitlar"
          :key="kayit.anahtar"
          class="onay-satir"
          :data-test="`kokpit-onay-satir-${kayit.anahtar}`"
        >
          <div class="onay-icerik">
            <span class="onay-tur">{{ turEtiketi(kayit.tur) }}</span>
            <strong class="onay-satir-baslik">{{ kayit.baslik || kayit.tur }}</strong>
            <small
              v-if="kayit.altBaslik"
              class="onay-satir-alt"
            >{{ kayit.altBaslik }}</small>
            <span class="onay-meta">
              <span v-if="kayit.tarih">{{ formatDate(kayit.tarih) }}</span>
              <span v-if="kayit.kaynakId != null">#{{ kayit.kaynakId }}</span>
            </span>
          </div>
          <strong
            v-if="kayit.tutar !== null && kayit.tutar !== undefined"
            class="onay-tutar gizli-veri"
          >{{ formatPara(kayit.tutar, 'TRY') }}</strong>
          <div class="onay-eylemler">
            <button
              type="button"
              class="onay-buton onay-buton--onay"
              :disabled="islemde"
              :data-test="`kokpit-onayla-${kayit.anahtar}`"
              @click="onayla(kayit)"
            >
              <i
                class="pi pi-check"
                aria-hidden="true"
              />
              {{ t('dashboard.onayla') }}
            </button>
            <button
              type="button"
              class="onay-buton onay-buton--red"
              :disabled="islemde"
              :data-test="`kokpit-reddet-${kayit.anahtar}`"
              @click="redAc(kayit)"
            >
              <i
                class="pi pi-times"
                aria-hidden="true"
              />
              {{ t('dashboard.reddet') }}
            </button>
          </div>
        </li>
      </ul>

      <div
        v-if="redHedef"
        class="onay-red"
        data-test="kokpit-onay-red"
      >
        <label
          class="onay-red-etiket"
          for="kokpit-onay-gerekce"
        >{{ t('dashboard.kokpitOnayRedGerekce') }}</label>
        <textarea
          id="kokpit-onay-gerekce"
          ref="gerekceAlan"
          v-model="gerekce"
          rows="2"
          maxlength="500"
          class="onay-red-alan"
          data-test="kokpit-onay-gerekce"
        />
        <p
          v-if="redHata"
          class="onay-red-hata"
          role="alert"
        >
          {{ redHata }}
        </p>
        <div class="onay-red-eylemler">
          <button
            type="button"
            class="onay-buton"
            @click="redKapat"
          >
            {{ t('common.vazgec') }}
          </button>
          <button
            type="button"
            class="onay-buton onay-buton--red"
            :disabled="islemde || gerekce.trim().length < 3"
            data-test="kokpit-onay-red-gonder"
            @click="reddet"
          >
            {{ t('dashboard.reddet') }}
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { onaylarAPI } from '../../../api/index.js'
import { useToastBildirim } from '../../../composables/useToastBildirim.js'
import { formatDate, formatPara } from '../../../utils/format.js'

const props = defineProps({
  visible: { type: Boolean, default: false }
})

const emit = defineEmits(['update:visible', 'guncellendi'])
const { t } = useI18n()
const toastBildirim = useToastBildirim()

const panel = ref(null)
const kayitlar = ref([])
const yukleniyor = ref(false)
const hata = ref('')
const islemde = ref(false)
const redHedef = ref(null)
const gerekce = ref('')
const redHata = ref('')
const gerekceAlan = ref(null)

const TUR_ETIKETLERI = {
  IZIN: 'dashboard.gorevTuruOnayIzin',
  MASRAF: 'dashboard.gorevTuruOnayMasraf',
  SATINALMA: 'dashboard.gorevTuruOnaySatinalma',
  SIPARIS: 'dashboard.gorevTuruOnaySiparis'
}

const turEtiketi = (tur) => (TUR_ETIKETLERI[tur] ? t(TUR_ETIKETLERI[tur]) : tur)

const hataMetni = (err) =>
  err?.response?.data?.message || err?.message || t('dashboard.onayIslemHatasi')

const yukle = async () => {
  yukleniyor.value = true
  hata.value = ''
  try {
    const cevap = await onaylarAPI.bekleyen({ page: 0, size: 20 })
    const veri = cevap?.data || {}
    const icerik = Array.isArray(veri) ? veri : veri.content || []
    kayitlar.value = icerik.map((kayit, index) => ({
      ...kayit,
      anahtar: `${kayit.tur || 'ONAY'}:${kayit.kaynakId ?? index}`
    }))
  } catch (err) {
    hata.value = hataMetni(err)
    kayitlar.value = []
  } finally {
    yukleniyor.value = false
  }
}

const kapat = () => emit('update:visible', false)

const kararGonder = async (kayit, karar, metin = '') => {
  if (islemde.value || !kayit?.tur || kayit.kaynakId == null) return
  islemde.value = true
  try {
    await onaylarAPI.karar({
      tur: kayit.tur,
      kaynakId: kayit.kaynakId,
      karar,
      gerekce: metin
    })
    kayitlar.value = kayitlar.value.filter((k) => k.anahtar !== kayit.anahtar)
    redKapat()
    toastBildirim.basarili(
      karar === 'ONAY' ? t('dashboard.onayBasarili') : t('dashboard.redBasarili')
    )
    emit('guncellendi', { karar, kayit })
  } catch (err) {
    const mesaj = hataMetni(err)
    toastBildirim.hata(mesaj)
    if (karar === 'RED') redHata.value = mesaj
  } finally {
    islemde.value = false
  }
}

const onayla = (kayit) => kararGonder(kayit, 'ONAY')

const redAc = (kayit) => {
  redHedef.value = kayit
  gerekce.value = ''
  redHata.value = ''
  nextTick(() => gerekceAlan.value?.focus?.())
}

const redKapat = () => {
  redHedef.value = null
  gerekce.value = ''
  redHata.value = ''
}

const reddet = () => {
  if (gerekce.value.trim().length < 3) {
    redHata.value = t('dashboard.redGerekceHata')
    return
  }
  if (redHedef.value) kararGonder(redHedef.value, 'RED', gerekce.value.trim())
}

watch(
  () => props.visible,
  (acik) => {
    if (acik) {
      kayitlar.value = []
      redKapat()
      yukle()
      nextTick(() => panel.value?.focus?.())
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.onay-katman {
  position: fixed;
  inset: 0;
  z-index: var(--z-modal);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: rgba(2, 6, 12, 0.55);
  backdrop-filter: blur(2px);
}
.onay-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: min(620px, 100%);
  max-height: calc(100vh - 32px);
  padding: 16px;
  overflow-y: auto;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  box-shadow: var(--elev-2);
}
.onay-ust {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border);
}
.onay-baslik {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}
.onay-alt {
  margin: 2px 0 0;
  font-size: 11px;
  color: var(--text-muted);
}
.onay-kapat {
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
.onay-kapat:hover,
.onay-kapat:focus-visible {
  border-color: var(--accent-border);
  color: var(--accent);
}
.onay-kapat:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}
.onay-iskelet {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.onay-iskelet-satir {
  display: block;
  height: 54px;
  border-radius: var(--radius-sm);
  background: var(--bg-muted);
  animation: onay-parla 1.4s ease infinite;
}
@keyframes onay-parla {
  0%,
  100% {
    opacity: 0.45;
  }
  50% {
    opacity: 0.9;
  }
}
.onay-hata,
.onay-bos {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0;
  padding: 12px 8px;
  font-size: 12px;
  color: var(--text-muted);
}
.onay-hata {
  color: var(--danger);
  justify-content: space-between;
}
.onay-liste {
  display: flex;
  flex-direction: column;
  gap: 7px;
  margin: 0;
  padding: 0;
  list-style: none;
}
.onay-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 9px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-muted);
}
.onay-icerik {
  display: flex;
  flex-direction: column;
  gap: 1px;
  flex: 1 1 160px;
  min-width: 0;
}
.onay-tur {
  font-size: 9.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--accent);
}
.onay-satir-baslik {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12.5px;
  color: var(--text-primary);
}
.onay-satir-alt,
.onay-meta {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 10.5px;
  color: var(--text-muted);
}
.onay-meta {
  display: inline-flex;
  gap: 6px;
  font-variant-numeric: tabular-nums;
}
.onay-tutar {
  flex-shrink: 0;
  font-size: 12.5px;
  font-weight: 700;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.onay-eylemler {
  display: inline-flex;
  gap: 5px;
  margin-left: auto;
}
.onay-buton {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius-xs);
  background: transparent;
  color: var(--text-secondary);
  font-size: 10.5px;
  font-weight: 600;
  cursor: pointer;
}
.onay-buton:hover:not(:disabled),
.onay-buton:focus-visible:not(:disabled) {
  border-color: var(--accent-border);
  color: var(--accent);
}
.onay-buton:disabled {
  opacity: 0.6;
  cursor: default;
}
.onay-buton--onay {
  border-color: var(--success-border);
  background: var(--success-soft);
  color: var(--success);
}
.onay-buton--red {
  border-color: var(--danger-border);
  background: var(--danger-soft);
  color: var(--danger);
}
.onay-buton:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}
.onay-red {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 9px;
  border: 1px solid var(--danger-border);
  border-radius: var(--radius-sm);
  background: var(--danger-soft);
}
.onay-red-etiket {
  font-size: 11px;
  font-weight: 600;
  color: var(--danger);
}
.onay-red-alan {
  width: 100%;
  padding: 6px 8px;
  border: 1px solid var(--danger-border);
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  color: var(--text-primary);
  font-size: 12px;
  resize: vertical;
}
.onay-red-alan:focus-visible {
  outline: 2px solid var(--danger);
  outline-offset: 1px;
}
.onay-red-hata {
  margin: 0;
  font-size: 11px;
  color: var(--danger);
}
.onay-red-eylemler {
  display: flex;
  justify-content: flex-end;
  gap: 5px;
}
@media (prefers-reduced-motion: reduce) {
  .onay-iskelet-satir {
    animation: none;
  }
}
@media (max-width: 520px) {
  .onay-panel {
    padding: 13px;
  }
  .onay-eylemler {
    width: 100%;
    margin-left: 0;
  }
}
</style>
