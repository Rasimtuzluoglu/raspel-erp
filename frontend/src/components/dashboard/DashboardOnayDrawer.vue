<template>
  <div
    v-if="visible"
    class="db-drawer-katman"
    data-test="onay-drawer-katman"
    @click.self="kapat"
  >
    <aside
      ref="panel"
      class="db-drawer db-onay-drawer"
      role="dialog"
      aria-modal="true"
      :aria-labelledby="baslikId"
      tabindex="-1"
      :aria-busy="yukleniyor ? 'true' : 'false'"
      data-test="onay-drawer"
      @keydown="tusYonet"
    >
      <header class="db-drawer-ust">
        <div class="db-drawer-baslik-alan">
          <h2
            :id="baslikId"
            class="db-drawer-baslik"
          >
            {{ baslik }}
          </h2>
          <p class="db-drawer-alt">
            {{ t('dashboard.hizliOnayAlt') }}
          </p>
        </div>
        <button
          type="button"
          class="db-drawer-kapat"
          :aria-label="t('common.close')"
          :title="t('common.close')"
          @click="kapat"
        >
          <i
            class="pi pi-times"
            aria-hidden="true"
          />
        </button>
      </header>

      <div
        v-if="yukleniyor && !kayitlar.length"
        class="onay-iskelet"
        aria-hidden="true"
      >
        <span
          v-for="i in 4"
          :key="`onay-iskelet-${i}`"
          class="onay-iskelet-satir"
        />
      </div>

      <div
        v-else-if="hata"
        class="onay-durum onay-durum--hata"
        role="alert"
      >
        <span>
          <i
            class="pi pi-exclamation-circle"
            aria-hidden="true"
          />
          {{ t('dashboard.onayListeHata') }}
        </span>
        <button
          type="button"
          class="onay-btn"
          @click="yukle"
        >
          {{ t('dashboard.gorevYenidenDene') }}
        </button>
      </div>

      <p
        v-else-if="!kayitlar.length"
        class="onay-durum"
        data-test="onay-bos"
      >
        <i
          class="pi pi-check-circle"
          aria-hidden="true"
        />
        {{ t('dashboard.onayListeBos') }}
      </p>

      <ul
        v-else
        class="onay-liste"
      >
        <li
          v-for="kayit in kayitlar"
          :key="kayit.anahtar"
          class="onay-satir"
          :data-test="`onay-satir-${kayit.anahtar}`"
        >
          <div class="onay-icerik">
            <strong class="onay-baslik">{{ kayit.baslik || kayitTuruAdi }}</strong>
            <small
              v-if="kayit.altBaslik"
              class="onay-alt-baslik"
            >{{ kayit.altBaslik }}</small>
            <span class="onay-meta">
              <span v-if="kayit.tarih">{{ formatDate(kayit.tarih) }}</span>
              <span v-if="kayit.kaynakId != null">#{{ kayit.kaynakId }}</span>
            </span>
          </div>
          <strong
            v-if="kayit.tutar !== null && kayit.tutar !== undefined"
            class="onay-tutar gizli-veri"
          >{{ formatCurrency(kayit.tutar) }}</strong>
          <div class="onay-eylemler">
            <button
              type="button"
              class="onay-btn onay-btn--onayla"
              :disabled="islemde"
              :aria-label="`${kayit.baslik || kayitTuruAdi} — ${t('dashboard.onayla')}`"
              :data-test="`onayla-${kayit.anahtar}`"
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
              class="onay-btn onay-btn--reddet"
              :disabled="islemde"
              :aria-label="`${kayit.baslik || kayitTuruAdi} — ${t('dashboard.reddet')}`"
              :data-test="`reddet-${kayit.anahtar}`"
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

      <!-- Red gerekcesi: en az 3 karakter zorunlu. -->
      <div
        v-if="redHedef"
        class="onay-red-katman"
        @click.self="redKapat"
      >
        <div
          class="onay-red"
          role="dialog"
          aria-modal="true"
          aria-labelledby="onay-red-baslik"
          data-test="onay-red-modal"
        >
          <h3
            id="onay-red-baslik"
            class="onay-red-baslik"
          >
            {{ t('dashboard.redGerekceBaslik') }}
          </h3>
          <label
            class="onay-red-etiket"
            for="onay-red-gerekce"
          >{{ t('dashboard.redGerekceEtiket') }}</label>
          <textarea
            id="onay-red-gerekce"
            ref="gerekceAlan"
            v-model="gerekce"
            class="onay-red-alan"
            rows="3"
            maxlength="500"
            :placeholder="t('dashboard.redGerekceIpucu')"
            data-test="onay-red-gerekce"
            @keydown.esc.stop.prevent="redKapat"
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
              class="onay-btn"
              @click="redKapat"
            >
              {{ t('common.cancel') }}
            </button>
            <button
              type="button"
              class="onay-btn onay-btn--reddet"
              :disabled="islemde || gerekceGecerli === false"
              data-test="onay-red-gonder"
              @click="reddet"
            >
              {{ t('dashboard.reddet') }}
            </button>
          </div>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, inject, nextTick, ref, watch } from 'vue'
import { PrimeVueToastSymbol } from 'primevue/usetoast'
import { useI18n } from 'vue-i18n'
import { onaylarAPI } from '../../api/index.js'
import { formatCurrency, formatDate } from '../../utils/format.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  /** Gorev merkezi turu: ONAY_IZIN | ONAY_MASRAF | ONAY_SATINALMA | ONAY_SIPARIS */
  tur: { type: String, default: 'ONAY_IZIN' }
})

const emit = defineEmits(['update:visible', 'guncellendi'])

const { t } = useI18n()

// Toast servisi opsiyoneldir: saglayici yoksa (test/izole kullanim) sessiz gecer.
const toast = inject(PrimeVueToastSymbol, null)

const GOREV_TURU_ANAHTARLARI = {
  ONAY_IZIN: 'dashboard.gorevTuruOnayIzin',
  ONAY_MASRAF: 'dashboard.gorevTuruOnayMasraf',
  ONAY_SATINALMA: 'dashboard.gorevTuruOnaySatinalma',
  ONAY_SIPARIS: 'dashboard.gorevTuruOnaySiparis'
}

const API_TURLERI = {
  ONAY_IZIN: 'IZIN',
  ONAY_MASRAF: 'MASRAF',
  ONAY_SATINALMA: 'SATINALMA',
  ONAY_SIPARIS: 'SIPARIS'
}

const apiTur = computed(() => API_TURLERI[props.tur] || null)
const kayitTuruAdi = computed(() =>
  GOREV_TURU_ANAHTARLARI[props.tur] ? t(GOREV_TURU_ANAHTARLARI[props.tur]) : props.tur
)
const baslik = computed(() => `${t('dashboard.hizliOnay')} — ${kayitTuruAdi.value}`)
const baslikId = 'db-onay-drawer-baslik'

const kayitlar = ref([])
const yukleniyor = ref(false)
const hata = ref('')
const islemde = ref(false)

const panel = ref(null)
let oncekiOdak = null

const bildir = (severity, detay) => {
  if (!toast || typeof toast.add !== 'function') return
  toast.add({ severity, summary: t('dashboard.hizliOnay'), detail: detay, life: 3000 })
}

const hataMetni = (err) => err?.response?.data?.message || err?.message || t('dashboard.onayIslemHatasi')

const yukle = async () => {
  if (!apiTur.value) {
    kayitlar.value = []
    return
  }
  yukleniyor.value = true
  hata.value = ''
  try {
    const cevap = await onaylarAPI.bekleyen({ tur: apiTur.value, page: 0, size: 50 })
    const veri = cevap?.data || {}
    const icerik = Array.isArray(veri) ? veri : veri.content || []
    kayitlar.value = icerik.map((kayit, sira) => {
      const tur = kayit.tur || apiTur.value
      return { ...kayit, tur, anahtar: `${tur}:${kayit.kaynakId ?? sira}` }
    })
  } catch (err) {
    hata.value = hataMetni(err)
    kayitlar.value = []
  } finally {
    yukleniyor.value = false
  }
}

// --- Red gerekcesi ---------------------------------------------------------
const redHedef = ref(null)
const gerekce = ref('')
const redHata = ref('')
const gerekceAlan = ref(null)
const gerekceGecerli = computed(() => gerekce.value.trim().length >= 3)

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

// --- Karar -----------------------------------------------------------------
const kararGonder = async (kayit, karar, metin = '') => {
  if (islemde.value || !kayit?.tur || kayit.kaynakId == null) return
  islemde.value = true
  try {
    await onaylarAPI.karar({ tur: kayit.tur, kaynakId: kayit.kaynakId, karar, gerekce: metin })
    kayitlar.value = kayitlar.value.filter((k) => k.anahtar !== kayit.anahtar)
    redKapat()
    bildir('success', karar === 'ONAY' ? t('dashboard.onayBasarili') : t('dashboard.redBasarili'))
    emit('guncellendi', { karar, kayit })
  } catch (err) {
    bildir('error', hataMetni(err))
    if (karar === 'RED') redHata.value = hataMetni(err)
  } finally {
    islemde.value = false
  }
}

const onayla = (kayit) => kararGonder(kayit, 'ONAY', '')

const reddet = () => {
  if (!gerekceGecerli.value) {
    redHata.value = t('dashboard.redGerekceHata')
    return
  }
  if (redHedef.value) kararGonder(redHedef.value, 'RED', gerekce.value.trim())
}

// --- Acma / kapama, odak ve klavye ---------------------------------------
const kapat = () => emit('update:visible', false)

const tusYonet = (olay) => {
  if (olay.key === 'Escape') {
    if (redHedef.value) {
      redKapat()
      return
    }
    kapat()
    return
  }
  if (olay.key !== 'Tab') return
  const ogeler = Array.from(
    panel.value?.querySelectorAll(
      'button:not(:disabled), [href], input:not(:disabled), textarea:not(:disabled), select:not(:disabled), [tabindex]:not([tabindex="-1"])'
    ) || []
  )
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
      kayitlar.value = []
      redKapat()
      yukle()
      nextTick(() => panel.value?.focus?.())
    } else if (oncekiOdak && typeof oncekiOdak.focus === 'function') {
      oncekiOdak.focus?.()
      oncekiOdak = null
    }
  },
  { immediate: true }
)
</script>

<style scoped>
/* Kabuk (.db-drawer-katman / .db-drawer / baslik) dashboard.css icinde
   tanimlidir; burada yalnizca onaya ozgu stiller bulunur. */

/* --- Durumlar ------------------------------------------------------------ */
.onay-iskelet {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.onay-iskelet-satir {
  display: block;
  height: 54px;
  border-radius: 10px;
  background: var(--bg-muted);
  animation: onayParlama 1.4s ease infinite;
}
.onay-durum {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin: 0;
  padding: 14px 4px;
  font-size: 12.5px;
  color: var(--text-muted);
}
.onay-durum--hata {
  color: var(--danger);
  justify-content: space-between;
}

/* --- Liste --------------------------------------------------------------- */
.onay-liste {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.onay-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 11px;
  background: var(--bg-muted);
  min-width: 0;
}
.onay-icerik {
  display: flex;
  flex-direction: column;
  gap: 1px;
  flex: 1 1 150px;
  min-width: 0;
}
.onay-baslik {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.onay-alt-baslik {
  font-size: 11px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.onay-meta {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 10.5px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}
.onay-tutar {
  flex-shrink: 0;
  font-size: 12.5px;
  font-weight: 800;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.onay-eylemler {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.onay-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 11.5px;
  font-weight: 700;
  cursor: pointer;
  transition:
    background var(--dur-base, 0.2s) var(--ease-standard, ease),
    color var(--dur-base, 0.2s) var(--ease-standard, ease),
    border-color var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.onay-btn:hover:not(:disabled) {
  border-color: var(--accent-border);
  color: var(--text-primary);
}
.onay-btn:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.onay-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.onay-btn--onayla {
  border-color: var(--success-border, var(--border));
  background: var(--success-soft);
  color: var(--success);
}
.onay-btn--onayla:hover:not(:disabled) {
  background: var(--success);
  color: #fff;
}
.onay-btn--reddet {
  border-color: var(--danger-border, var(--border));
  background: var(--danger-soft);
  color: var(--danger);
}
.onay-btn--reddet:hover:not(:disabled) {
  background: var(--danger);
  color: #fff;
}
.onay-btn i {
  font-size: 9.5px;
}

/* --- Red gerekcesi ------------------------------------------------------- */
.onay-red-katman {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: rgba(2, 12, 14, 0.5);
}
.onay-red {
  width: 100%;
  max-width: 340px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--bg-card);
  box-shadow: var(--elev-3, var(--elev-2));
}
.onay-red-baslik {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}
.onay-red-etiket {
  font-size: 11.5px;
  font-weight: 600;
  color: var(--text-secondary);
}
.onay-red-alan {
  width: 100%;
  min-height: 74px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: 9px;
  background: var(--bg-muted);
  color: var(--text-primary);
  font-size: 12.5px;
  font-family: inherit;
  resize: vertical;
}
.onay-red-alan:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}
.onay-red-hata {
  margin: 0;
  font-size: 11.5px;
  color: var(--danger);
}
.onay-red-eylemler {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 2px;
}

@keyframes onayParlama {
  0% {
    opacity: 0.45;
  }
  50% {
    opacity: 0.85;
  }
  100% {
    opacity: 0.45;
  }
}
@media (prefers-reduced-motion: reduce) {
  .db-drawer-katman,
  .db-drawer,
  .db-drawer-kapat {
    animation: none;
    transition: none;
  }
  .onay-iskelet-satir,
  .onay-btn {
    animation: none;
    transition: none;
  }
}
@media (max-width: 600px) {
  .onay-tutar {
    font-size: 12px;
  }
}
</style>
