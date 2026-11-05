<template>
  <transition name="pwa-yukari">
    <div
      v-if="gorunur"
      class="pwa-kurulum"
      role="region"
      :aria-label="t('pwa.kurulumBaslik')"
      aria-live="polite"
      data-test="pwa-kurulum"
    >
      <span
        class="pwa-kurulum-ikon"
        aria-hidden="true"
      ><i
        :class="iosYonlendirme ? 'pi pi-share-alt' : 'pi pi-download'"
      /></span>

      <div class="pwa-kurulum-metin">
        <strong class="pwa-kurulum-baslik">
          {{ iosYonlendirme ? t('pwa.iosYonlendirme') : t('pwa.kurulumBaslik') }}
        </strong>
        <span class="pwa-kurulum-alt">{{ t('pwa.kurulumAlt') }}</span>
      </div>

      <div class="pwa-kurulum-eylemler">
        <button
          v-if="!iosYonlendirme"
          type="button"
          class="pwa-ekle"
          data-test="pwa-ekle"
          @click="ekle"
        >
          {{ t('pwa.ekle') }}
        </button>
        <button
          type="button"
          class="pwa-kapat"
          :aria-label="t('common.close')"
          :title="t('common.close')"
          data-test="pwa-kapat"
          @click="kapat"
        >
          <i
            class="pi pi-times"
            aria-hidden="true"
          />
        </button>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'

/** "Kapat" tercihi: 30 gun boyunca kurulum bandi tekrar gosterilmez. */
const PWA_KAPATMA_ANAHTAR = 'raspel_pwa_kapatma'
/** iOS yonlendirmesi tek seferliktir. */
const PWA_IOS_ANAHTAR = 'raspel_pwa_ios'
const PWA_KAPATMA_SURESI_MS = 30 * 24 * 60 * 60 * 1000

const { t } = useI18n()

const depoOku = (anahtar) => {
  try {
    return typeof localStorage === 'undefined' ? null : localStorage.getItem(anahtar)
  } catch {
    return null
  }
}

const depoYaz = (anahtar, deger) => {
  try {
    if (typeof localStorage !== 'undefined') localStorage.setItem(anahtar, deger)
  } catch {
    /* depolama kapaliysa oturum boyunca gizli kalir */
  }
}

/** Uygulama zaten kurulu/tam ekran modunda mi? */
const standaloneMi = () => {
  if (typeof window === 'undefined') return true
  try {
    if (typeof window.matchMedia === 'function' && window.matchMedia('(display-mode: standalone)').matches) {
      return true
    }
  } catch {
    /* matchMedia desteklenmiyorsa navigator.standalone'a bakilir */
  }
  return window.navigator?.standalone === true
}

/** iOS Safari (Chrome/Firefox/Edge iOS dahil degil: onlarda kurulum akisi yok). */
const iosSafariMi = () => {
  if (typeof navigator === 'undefined') return false
  const ua = navigator.userAgent || ''
  const ios = /iPad|iPhone|iPod/.test(ua) || (/Macintosh/.test(ua) && navigator.maxTouchPoints > 1)
  if (!ios) return false
  const digerTarayici = /CriOS|FxiOS|EdgiOS|OPiOS|mercury/i.test(ua)
  if (digerTarayici) return false
  // iOS 13+ iPadOS masaustu UA'sinda "Safari" gecmeyebilir; WebKit tabanli
  // tarayicilarin tamami "Ana Ekrana Ekle" akisini paylasir.
  return true
}

const kapatmaAktifMi = () => {
  const ham = depoOku(PWA_KAPATMA_ANAHTAR)
  if (!ham) return false
  const zaman = Number(ham)
  if (!Number.isFinite(zaman)) return Boolean(ham)
  return Date.now() - zaman < PWA_KAPATMA_SURESI_MS
}

const kurulabilir = ref(false)
const iosYonlendirme = ref(false)
const kapatildi = ref(kapatmaAktifMi())
const tamamlandi = ref(false)
const standalone = standaloneMi()

// iOS yonlendirmesi yalnizca bir kez gosterilir; bu yuzden senkron okunur.
if (!standalone && !kapatildi.value && iosSafariMi() && !depoOku(PWA_IOS_ANAHTAR)) {
  iosYonlendirme.value = true
  depoYaz(PWA_IOS_ANAHTAR, String(Date.now()))
}

const gorunur = computed(
  () =>
    !standalone &&
    !kapatildi.value &&
    !tamamlandi.value &&
    (kurulabilir.value || iosYonlendirme.value)
)

let ertelenenOlay = null

const kurulabilirYakala = (olay) => {
  // Kurulu/tam ekran uygulamada kurulum bandi anlamsizdir.
  if (standalone) return
  // Tarayicinin kendi mini bilgi cubugunu engelle; kurulumu biz yonetiyoruz.
  if (typeof olay?.preventDefault === 'function') olay.preventDefault()
  ertelenenOlay = olay
  kurulabilir.value = true
}

const kuruldu = () => {
  tamamlandi.value = true
  kurulabilir.value = false
  ertelenenOlay = null
  depoYaz(PWA_KAPATMA_ANAHTAR, String(Date.now()))
}

const ekle = async () => {
  const olay = ertelenenOlay
  if (!olay || typeof olay.prompt !== 'function') return
  try {
    await olay.prompt()
    if (olay.userChoice && typeof olay.userChoice.then === 'function') {
      await olay.userChoice
    }
  } catch {
    /* kullanici iptal etti veya tarayici reddetti */
  } finally {
    // Ayni olay yeniden kullanilamaz; bant bir daha gosterilmez.
    kurulabilir.value = false
    ertelenenOlay = null
    tamamlandi.value = true
  }
}

const kapat = () => {
  kapatildi.value = true
  depoYaz(PWA_KAPATMA_ANAHTAR, String(Date.now()))
}

onMounted(() => {
  if (typeof window === 'undefined' || typeof window.addEventListener !== 'function') return
  window.addEventListener('beforeinstallprompt', kurulabilirYakala)
  window.addEventListener('appinstalled', kuruldu)
})

onBeforeUnmount(() => {
  if (typeof window === 'undefined' || typeof window.removeEventListener !== 'function') return
  window.removeEventListener('beforeinstallprompt', kurulabilirYakala)
  window.removeEventListener('appinstalled', kuruldu)
})
</script>

<style scoped>
/*
 * Kurulum bandi alt menunun hemen ustunde durur. Yalnizca mobilde gorunur;
 * masaustunde PWA kurulumu tarayici menusunden yapilir.
 */
.pwa-kurulum {
  position: fixed;
  left: 12px;
  right: 12px;
  bottom: calc(56px + env(safe-area-inset-bottom, 0px) + 8px);
  z-index: 880;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--accent-border);
  border-radius: 14px;
  background: var(--bg-card);
  color: var(--text-primary);
  box-shadow: var(--elev-3, var(--elev-2));
}
@media (min-width: 901px) {
  .pwa-kurulum {
    display: none;
  }
}
.pwa-kurulum-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 14px;
}
.pwa-kurulum-metin {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}
.pwa-kurulum-baslik {
  font-size: 13px;
  font-weight: 700;
  line-height: 1.25;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pwa-kurulum-alt {
  font-size: 11.5px;
  line-height: 1.3;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pwa-kurulum-eylemler {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
.pwa-ekle {
  min-height: 44px;
  padding: 0 14px;
  border: 1px solid var(--accent);
  border-radius: 10px;
  background: var(--accent);
  color: var(--accent-contrast);
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
}
.pwa-ekle:hover {
  background: var(--accent-hover);
  border-color: var(--accent-hover);
}
.pwa-kapat {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: var(--text-muted);
  font-size: 13px;
  cursor: pointer;
}
.pwa-kapat:hover {
  background: var(--bg-muted);
  color: var(--text-primary);
}
.pwa-ekle:focus-visible,
.pwa-kapat:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.pwa-yukari-enter-active,
.pwa-yukari-leave-active {
  transition:
    transform 0.22s ease,
    opacity 0.22s ease;
}
.pwa-yukari-enter-from,
.pwa-yukari-leave-to {
  transform: translateY(12px);
  opacity: 0;
}
@media (prefers-reduced-motion: reduce) {
  .pwa-yukari-enter-active,
  .pwa-yukari-leave-active {
    transition: none;
  }
}
</style>
