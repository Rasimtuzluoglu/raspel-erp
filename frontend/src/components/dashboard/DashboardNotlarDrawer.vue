<template>
  <div
    v-if="visible"
    class="db-drawer-katman"
    data-test="notlar-katman"
    @click.self="kapat"
  >
    <aside
      ref="panel"
      class="db-drawer db-notlar-drawer"
      role="dialog"
      aria-modal="true"
      aria-labelledby="db-notlar-baslik"
      tabindex="-1"
      data-test="notlar-drawer"
      @keydown="tusYonet"
    >
      <header class="db-drawer-ust">
        <div class="db-drawer-baslik-alan">
          <h2
            id="db-notlar-baslik"
            class="db-drawer-baslik"
          >
            {{ t('dashboard.notlarBaslik') }}
          </h2>
          <p class="db-drawer-alt">
            {{ t('dashboard.notlarAlt') }}
          </p>
        </div>
        <button
          type="button"
          class="db-drawer-kapat"
          :aria-label="t('common.close')"
          :title="t('common.close')"
          data-test="notlar-kapat"
          @click="kapat"
        >
          <i
            class="pi pi-times"
            aria-hidden="true"
          />
        </button>
      </header>

      <form
        class="notlar-ekle"
        @submit.prevent="ekle"
      >
        <label
          class="sr-only"
          for="db-not-baslik"
        >{{ t('dashboard.notlarBaslikAlan') }}</label>
        <input
          id="db-not-baslik"
          ref="baslikGirdi"
          v-model="baslik"
          type="text"
          class="notlar-girdi"
          :maxlength="200"
          :placeholder="t('dashboard.notlarBaslikIpucu')"
          :aria-invalid="hata ? 'true' : 'false'"
          :aria-describedby="hata ? 'db-not-hata' : undefined"
          data-test="not-baslik"
          @keydown.enter.prevent="icerikGirdi?.focus?.()"
        >
        <label
          class="sr-only"
          for="db-not-icerik"
        >{{ t('dashboard.notlarIcerikAlan') }}</label>
        <textarea
          id="db-not-icerik"
          ref="icerikGirdi"
          v-model="icerik"
          class="notlar-girdi notlar-girdi--cok"
          rows="2"
          :maxlength="2000"
          :placeholder="t('dashboard.notlarIcerikIpucu')"
          data-test="not-icerik"
          @keydown.ctrl.enter.prevent="ekle"
          @keydown.meta.enter.prevent="ekle"
        />
        <button
          type="submit"
          class="notlar-ekle-btn"
          :disabled="kaydediliyor"
          :aria-label="t('dashboard.notlarEkle')"
          :title="t('dashboard.notlarEkle')"
          data-test="not-ekle"
        >
          <i
            :class="kaydediliyor ? 'pi pi-spin pi-spinner' : 'pi pi-plus'"
            aria-hidden="true"
          />
        </button>
      </form>

      <p
        v-if="hata"
        id="db-not-hata"
        class="notlar-hata"
        role="alert"
        data-test="not-hata"
      >
        {{ hata }}
      </p>

      <div class="notlar-ust">
        <span class="notlar-sayac">{{ t('dashboard.notlarSon', { n: notlar.length }) }}</span>
        <div class="notlar-ust-eylem">
          <button
            type="button"
            class="notlar-yenile"
            :disabled="yukleniyor"
            :aria-label="t('dashboard.yenile')"
            :title="t('dashboard.yenile')"
            data-test="notlar-yenile"
            @click="yukle"
          >
            <i
              :class="yukleniyor ? 'pi pi-spin pi-spinner' : 'pi pi-refresh'"
              aria-hidden="true"
            />
          </button>
          <router-link
            to="/notlar"
            class="notlar-tumu"
            data-test="notlar-tumu"
            @click="kapat"
          >
            {{ t('dashboard.tumunuGor') }}
            <i
              class="pi pi-arrow-right"
              aria-hidden="true"
            />
          </router-link>
        </div>
      </div>

      <div
        v-if="yukleniyor && !notlar.length"
        class="notlar-iskelet"
        aria-hidden="true"
        data-test="notlar-iskelet"
      >
        <span
          v-for="i in 3"
          :key="`not-iskelet-${i}`"
          class="iskelet"
        />
      </div>

      <div
        v-else-if="yuklemeHatasi"
        class="notlar-durum notlar-durum--hata"
        data-test="notlar-hata-durum"
      >
        <span>{{ t('dashboard.notlarYuklenemedi') }}</span>
        <button
          type="button"
          class="notlar-dene"
          @click="yukle"
        >
          {{ t('dashboard.yenidenDene') }}
        </button>
      </div>

      <ul
        v-else-if="notlar.length"
        class="notlar-liste"
        data-test="not-liste"
      >
        <li
          v-for="not in notlar"
          :key="not.id"
          class="notlar-oge"
          :data-test="`not-oge-${not.id}`"
        >
          <div class="notlar-icerik-alan">
            <span class="notlar-baslik">{{ not.baslik }}</span>
            <p
              v-if="not.icerik"
              class="notlar-metin"
            >
              {{ not.icerik }}
            </p>
            <div class="notlar-alt">
              <span class="notlar-tarih">{{ tarihMetni(not.olusturmaTarihi) }}</span>
              <span
                v-if="not.onemDerecesi && not.onemDerecesi !== 'NORMAL'"
                class="notlar-onem"
                :class="`notlar-onem--${not.onemDerecesi.toLowerCase()}`"
              >{{ onemAdi(not.onemDerecesi) }}</span>
            </div>
          </div>
        </li>
      </ul>

      <p
        v-else
        class="notlar-bos"
        data-test="notlar-bos"
      >
        <i
          class="pi pi-clipboard"
          aria-hidden="true"
        />
        {{ t('dashboard.notlarBos') }}
      </p>
    </aside>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatDate } from '../../utils/format.js'
import { useNotStore } from '../../stores/notStore.js'

const props = defineProps({
  visible: { type: Boolean, default: false }
})

const emit = defineEmits(['update:visible'])

const { t } = useI18n()
const store = useNotStore()

const panel = ref(null)
const baslikGirdi = ref(null)
const icerikGirdi = ref(null)
const baslik = ref('')
const icerik = ref('')
const hata = ref('')
const kaydediliyor = ref(false)

let oncekiOdak = null

const notlar = computed(() => (Array.isArray(store.notlar) ? store.notlar.slice(0, 10) : []))
const yukleniyor = computed(() => store.loading)
const yuklemeHatasi = computed(() => store.error)

const tarihMetni = (tarih) => (tarih ? formatDate(tarih) : '')
const onemAdi = (v) =>
  ({
    DUSUK: t('notlar.dusuk'),
    NORMAL: t('notlar.normal'),
    YUKSEK: t('notlar.yuksek'),
    KRITIK: t('notlar.kritik')
  })[v] || v

const yukle = () =>
  store
    .getAllNotlar({ page: 0, size: 10, sort: 'olusturmaTarihi,desc' })
    .catch(() => {
      /* hata store.error uzerinden gosterilir */
    })

const ekle = async () => {
  const temizBaslik = baslik.value.trim()
  if (!temizBaslik) {
    hata.value = t('dashboard.notlarBaslikZorunlu')
    baslikGirdi.value?.focus?.()
    return
  }
  if (temizBaslik.length > 200) {
    hata.value = t('dashboard.notlarUzunHata', { limit: 200 })
    return
  }
  kaydediliyor.value = true
  hata.value = ''
  try {
    await store.addNot({
      baslik: temizBaslik,
      icerik: icerik.value.trim(),
      onemDerecesi: 'NORMAL',
      renk: 'MAVI'
    })
    baslik.value = ''
    icerik.value = ''
    await yukle()
    baslikGirdi.value?.focus?.()
  } catch (err) {
    hata.value = err?.response?.data?.message || t('dashboard.notlarKaydedilemedi')
  } finally {
    kaydediliyor.value = false
  }
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
      baslik.value = ''
      icerik.value = ''
      hata.value = ''
      yukle()
      nextTick(() => {
        panel.value?.focus?.()
        baslikGirdi.value?.focus?.()
      })
    } else if (oncekiOdak && typeof oncekiOdak.focus === 'function') {
      oncekiOdak.focus?.()
      oncekiOdak = null
    }
  },
  { immediate: true }
)
</script>

<style scoped>
/* Kabuk (.db-drawer-katman / .db-drawer / baslik) dashboard.css icinde. */
.db-notlar-drawer {
  gap: 10px;
}

/* --- Ekleme alani -------------------------------------------------------- */
.notlar-ekle {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 38px;
  grid-template-rows: auto auto;
  gap: 6px 8px;
}
.notlar-girdi {
  grid-column: 1;
  min-width: 0;
  min-height: 38px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: 9px;
  background: var(--bg-muted);
  color: var(--text-primary);
  font-size: 12.5px;
  font-family: inherit;
}
.notlar-girdi--cok {
  resize: vertical;
  min-height: 54px;
  line-height: 1.4;
}
.notlar-girdi::placeholder {
  color: var(--text-muted);
}
.notlar-girdi:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 1px;
}
.notlar-girdi[aria-invalid='true'] {
  border-color: var(--danger);
}
.notlar-ekle-btn {
  grid-column: 2;
  grid-row: 1 / span 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  align-self: stretch;
  border: 1px solid var(--accent);
  border-radius: 9px;
  background: var(--accent);
  color: var(--accent-contrast);
  cursor: pointer;
  transition: background var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.notlar-ekle-btn:hover:not(:disabled) {
  background: var(--accent-hover);
}
.notlar-ekle-btn:disabled {
  opacity: 0.6;
  cursor: progress;
}
.notlar-ekle-btn:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.notlar-hata {
  margin: 0;
  font-size: 11.5px;
  color: var(--danger);
}

/* --- Ust satir ----------------------------------------------------------- */
.notlar-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}
.notlar-sayac {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
}
.notlar-ust-eylem {
  display: flex;
  align-items: center;
  gap: 8px;
}
.notlar-yenile {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
}
.notlar-yenile:hover:not(:disabled) {
  border-color: var(--accent-border);
  color: var(--accent);
}
.notlar-yenile:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.notlar-tumu {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 28px;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--accent);
  text-decoration: none;
}
.notlar-tumu i {
  font-size: 9px;
}
.notlar-tumu:hover {
  color: var(--accent-hover);
}
.notlar-tumu:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

/* --- Liste --------------------------------------------------------------- */
.notlar-liste {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.notlar-oge {
  padding: 9px 10px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-muted);
  min-width: 0;
}
.notlar-icerik-alan {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}
.notlar-baslik {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notlar-metin {
  margin: 0;
  font-size: 11.5px;
  line-height: 1.4;
  color: var(--text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.notlar-alt {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.notlar-tarih {
  font-size: 10px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}
.notlar-onem {
  padding: 1px 6px;
  border-radius: 999px;
  font-size: 9.5px;
  font-weight: 700;
}
.notlar-onem--dusuk {
  background: var(--success-soft);
  color: var(--success);
}
.notlar-onem--yuksek {
  background: var(--warning-soft);
  color: var(--warning);
}
.notlar-onem--kritik {
  background: var(--danger-soft);
  color: var(--danger);
}

/* --- Durumlar ------------------------------------------------------------ */
.notlar-iskelet {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.notlar-iskelet .iskelet {
  display: block;
  height: 54px;
  border-radius: 10px;
  background: var(--bg-muted);
}
.notlar-durum {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
  padding: 12px;
  border: 1px solid var(--danger-border, var(--border));
  border-radius: 10px;
  font-size: 12px;
  color: var(--danger);
}
.notlar-dene {
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  color: var(--text-primary);
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
}
.notlar-dene:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.notlar-bos {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  padding: 18px 4px;
  font-size: 12.5px;
  color: var(--text-secondary);
}
.notlar-bos i {
  font-size: 15px;
  color: var(--accent);
}

@media (prefers-reduced-motion: reduce) {
  .notlar-ekle-btn {
    transition: none;
  }
}
</style>
