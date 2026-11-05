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
          for="db-not-girdi"
        >{{ t('dashboard.notlarMetin') }}</label>
        <input
          id="db-not-girdi"
          ref="girdi"
          v-model="metin"
          type="text"
          class="notlar-girdi"
          :maxlength="MAX_NOT_METIN"
          :placeholder="t('dashboard.notlarIpucu')"
          :aria-invalid="hata ? 'true' : 'false'"
          :aria-describedby="hata ? 'db-not-hata' : undefined"
          data-test="not-girdi"
          @keydown.enter.prevent="ekle"
        >
        <button
          type="submit"
          class="notlar-ekle-btn"
          :aria-label="t('dashboard.notlarEkle')"
          :title="t('dashboard.notlarEkle')"
          data-test="not-ekle"
        >
          <i
            class="pi pi-plus"
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
        <span
          class="notlar-sayac"
          data-test="not-sayac"
        >{{ t('dashboard.notlarSayac', { acik: acikSayi, toplam: notlar.length }) }}</span>
        <button
          v-if="tamamlananVar"
          type="button"
          class="notlar-temizle"
          data-test="not-temizle"
          @click="tamamlananlariTemizle"
        >
          <i
            class="pi pi-trash"
            aria-hidden="true"
          />
          {{ t('dashboard.notlarTamamlananlariTemizle') }}
        </button>
      </div>

      <ul
        v-if="notlar.length"
        class="notlar-liste"
        data-test="not-liste"
      >
        <li
          v-for="not in notlar"
          :key="not.id"
          class="notlar-oge"
          :class="{ 'notlar-oge--tamam': not.tamam }"
          :data-test="`not-oge-${not.id}`"
        >
          <label class="notlar-onay">
            <input
              type="checkbox"
              :checked="not.tamam"
              :aria-label="not.tamam ? t('dashboard.notlarGeriAl') : t('dashboard.notlarTamamla')"
              data-test="not-tamam"
              @change="tamamla(not)"
            >
            <span class="notlar-metin">{{ not.metin }}</span>
          </label>
          <small class="notlar-tarih">{{ tarihMetni(not.tarih) }}</small>
          <button
            type="button"
            class="notlar-sil"
            :aria-label="t('dashboard.notlarSil')"
            :title="t('dashboard.notlarSil')"
            data-test="not-sil"
            @click="sil(not)"
          >
            <i
              class="pi pi-times"
              aria-hidden="true"
            />
          </button>
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
import {
  MAX_NOT,
  MAX_NOT_METIN,
  notlarOku,
  notlarOlayiYay,
  notlarYaz,
  yeniNotId
} from '../../utils/dashboardNotlar.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  kullaniciId: { type: [String, Number], default: null }
})

const emit = defineEmits(['update:visible'])

const { t } = useI18n()

const panel = ref(null)
const girdi = ref(null)
const metin = ref('')
const hata = ref('')
const notlar = ref([])

let oncekiOdak = null

const acikSayi = computed(() => notlar.value.filter((not) => !not.tamam).length)
const tamamlananVar = computed(() => notlar.value.some((not) => not.tamam))

const tarihMetni = (tarih) => formatDate(tarih)

const yukle = () => {
  notlar.value = notlarOku(props.kullaniciId)
}

const kaliciYaz = () => {
  notlar.value = notlarYaz(notlar.value, props.kullaniciId)
  notlarOlayiYay({ kullaniciId: props.kullaniciId, acik: acikSayi.value })
}

const ekle = () => {
  const temiz = metin.value.trim()
  if (!temiz) {
    hata.value = t('dashboard.notlarBosHata')
    return
  }
  if (temiz.length > MAX_NOT_METIN) {
    hata.value = t('dashboard.notlarUzunHata', { limit: MAX_NOT_METIN })
    return
  }
  if (notlar.value.length >= MAX_NOT) {
    hata.value = t('dashboard.notlarLimitHata', { limit: MAX_NOT })
    return
  }
  notlar.value = [
    { id: yeniNotId(), metin: temiz, tamam: false, tarih: new Date().toISOString() },
    ...notlar.value
  ]
  metin.value = ''
  hata.value = ''
  kaliciYaz()
}

const tamamla = (not) => {
  notlar.value = notlar.value.map((kayit) =>
    kayit.id === not.id ? { ...kayit, tamam: !kayit.tamam } : kayit
  )
  hata.value = ''
  kaliciYaz()
}

const sil = (not) => {
  notlar.value = notlar.value.filter((kayit) => kayit.id !== not.id)
  hata.value = ''
  kaliciYaz()
}

const tamamlananlariTemizle = () => {
  notlar.value = notlar.value.filter((not) => !not.tamam)
  hata.value = ''
  kaliciYaz()
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
      metin.value = ''
      hata.value = ''
      yukle()
      nextTick(() => {
        panel.value?.focus?.()
        girdi.value?.focus?.()
      })
    } else if (oncekiOdak && typeof oncekiOdak.focus === 'function') {
      oncekiOdak.focus?.()
      oncekiOdak = null
    }
  },
  { immediate: true }
)

watch(
  () => props.kullaniciId,
  () => {
    if (props.visible) {
      hata.value = ''
      yukle()
    }
  }
)
</script>

<style scoped>
/* Kabuk (.db-drawer-katman / .db-drawer / baslik) dashboard.css icinde. */
.db-notlar-drawer {
  gap: 10px;
}

/* --- Ekleme alani -------------------------------------------------------- */
.notlar-ekle {
  display: flex;
  align-items: center;
  gap: 8px;
}
.notlar-girdi {
  flex: 1;
  min-width: 0;
  min-height: 38px;
  padding: 0 10px;
  border: 1px solid var(--border);
  border-radius: 9px;
  background: var(--bg-muted);
  color: var(--text-primary);
  font-size: 12.5px;
  font-family: inherit;
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
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 38px;
  height: 38px;
  border: 1px solid var(--accent);
  border-radius: 9px;
  background: var(--accent);
  color: var(--accent-contrast);
  cursor: pointer;
  transition: background var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.notlar-ekle-btn:hover {
  background: var(--accent-hover);
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

/* --- Sayac / toplu temizleme -------------------------------------------- */
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
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}
.notlar-temizle {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 28px;
  padding: 0 8px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
}
.notlar-temizle:hover {
  border-color: var(--danger-border, var(--border));
  color: var(--danger);
}
.notlar-temizle:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.notlar-temizle i {
  font-size: 10px;
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
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-muted);
  min-width: 0;
}
.notlar-onay {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
  cursor: pointer;
}
.notlar-onay input {
  flex-shrink: 0;
  width: 15px;
  height: 15px;
  accent-color: var(--accent);
  cursor: pointer;
}
.notlar-onay input:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.notlar-metin {
  min-width: 0;
  font-size: 12.5px;
  line-height: 1.4;
  color: var(--text-primary);
  overflow-wrap: anywhere;
}
.notlar-oge--tamam .notlar-metin {
  color: var(--text-muted);
  text-decoration: line-through;
}
.notlar-tarih {
  flex-shrink: 0;
  font-size: 10px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.notlar-sil {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  border: 1px solid transparent;
  border-radius: 7px;
  background: transparent;
  color: var(--text-muted);
  font-size: 10px;
  cursor: pointer;
}
.notlar-sil:hover {
  border-color: var(--danger-border, var(--border));
  color: var(--danger);
  background: var(--danger-soft);
}
.notlar-sil:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

/* --- Bos durum ----------------------------------------------------------- */
.notlar-bos {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  padding: 18px 4px;
  font-size: 12.5px;
  color: var(--text-muted);
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
