<template>
  <div class="durum-sayfasi">
    <div
      class="durum-sahne"
      aria-hidden="true"
    >
      <span class="durum-mesh" />
      <span class="durum-grid" />
    </div>

    <main
      class="durum-kart"
      data-test="durum-kart"
      aria-labelledby="durum-baslik"
    >
      <header class="durum-ust">
        <img
          :src="markaLogo"
          alt=""
          class="durum-logo"
          width="40"
          height="40"
        >
        <div class="durum-ust-metin">
          <h1
            id="durum-baslik"
            class="durum-baslik"
          >
            {{ t('durum.baslik') }}
          </h1>
          <p class="durum-alt">
            {{ t('durum.altBaslik') }}
          </p>
        </div>
      </header>

      <div
        v-if="yukleniyor"
        class="durum-genel durum-genel--bekliyor"
        role="status"
        aria-live="polite"
        data-test="durum-yukleniyor"
      >
        <span
          class="durum-nokta"
          aria-hidden="true"
        />
        <span>{{ t('durum.yukleniyor') }}</span>
      </div>

      <div
        v-else
        class="durum-genel"
        :class="'durum-genel--' + seviye"
        role="status"
        aria-live="polite"
        data-test="durum-genel"
      >
        <span
          class="durum-nokta"
          aria-hidden="true"
        />
        <strong>{{ durumMetni }}</strong>
      </div>

      <p
        v-if="!yukleniyor && hata"
        class="durum-hata-metin"
        data-test="durum-hata"
      >
        {{ t('durum.ulasilamiyorHint') }}
      </p>

      <dl
        v-if="!yukleniyor && !hata"
        class="durum-detay"
      >
        <div class="durum-detay-satir">
          <dt>{{ t('durum.surum') }}</dt>
          <dd data-test="durum-surum">
            {{ veri?.surum || '—' }}
          </dd>
        </div>
        <div class="durum-detay-satir">
          <dt>{{ t('durum.sonKontrol') }}</dt>
          <dd data-test="durum-zaman">
            {{ zamanMetni }}
          </dd>
        </div>
      </dl>

      <div class="durum-eylemler">
        <button
          type="button"
          class="durum-buton"
          data-test="durum-yenile"
          :disabled="yukleniyor"
          @click="yukle"
        >
          <i
            class="pi pi-refresh"
            aria-hidden="true"
          />
          <span>{{ t('durum.yenile') }}</span>
        </button>
        <a
          class="durum-baglanti"
          href="/giris"
          data-test="durum-giris"
        >
          {{ t('durum.girisDon') }}
        </a>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { durumAPI } from '../api/index.js'

const { t, locale } = useI18n()

const markaLogo = '/logo-icon.png'

const yukleniyor = ref(true)
const hata = ref(false)
const veri = ref(null)
const seviye = ref('amber')

const YUKARI = ['UP', 'OK', 'CALISIYOR', 'SAGLIKLI', 'HEALTHY']
const ASAGI = ['DOWN', 'HATA', 'KESINTI', 'OFFLINE', 'KAPALI']

const durumMetni = computed(() => {
  if (seviye.value === 'up') return t('durum.calisiyor')
  if (seviye.value === 'down') return t('durum.kesinti')
  if (seviye.value === 'hata') return t('durum.ulasilamiyor')
  return t('durum.kismi')
})

const zamanMetni = computed(() => {
  const ham = veri.value?.zaman
  if (!ham) return '—'
  const tarih = new Date(ham)
  if (Number.isNaN(tarih.getTime())) return '—'
  try {
    return new Intl.DateTimeFormat(locale.value === 'en' ? 'en-GB' : 'tr-TR', {
      dateStyle: 'medium',
      timeStyle: 'short'
    }).format(tarih)
  } catch {
    return tarih.toLocaleString()
  }
})

const yukle = async () => {
  yukleniyor.value = true
  hata.value = false
  try {
    const res = await durumAPI.durum()
    veri.value = res?.data || {}
    const normal = String(veri.value.durum || '').trim().toUpperCase()
    if (YUKARI.includes(normal)) seviye.value = 'up'
    else if (ASAGI.includes(normal)) seviye.value = 'down'
    else seviye.value = 'amber'
  } catch {
    veri.value = null
    hata.value = true
    seviye.value = 'hata'
  } finally {
    yukleniyor.value = false
  }
}

onMounted(yukle)
</script>

<style scoped>
.durum-sayfasi {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh / var(--uyg-olcek));
  min-height: calc(100dvh / var(--uyg-olcek));
  width: 100%;
  padding:
    calc(24px + env(safe-area-inset-top))
    calc(20px + env(safe-area-inset-right))
    calc(24px + env(safe-area-inset-bottom))
    calc(20px + env(safe-area-inset-left));
  background: linear-gradient(180deg, #070b10 0%, #0b1219 100%);
  color: #e8eef5;
  overflow-x: hidden;
}

.durum-sahne {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.durum-mesh {
  position: absolute;
  inset: -18%;
  background:
    radial-gradient(38% 44% at 22% 20%, rgba(20, 184, 166, 0.14), transparent 70%),
    radial-gradient(42% 46% at 80% 26%, rgba(45, 212, 191, 0.09), transparent 72%);
}

.durum-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.05) 1px, transparent 1px);
  background-size: 52px 52px;
  -webkit-mask-image: radial-gradient(circle at 50% 42%, #000 0%, transparent 78%);
  mask-image: radial-gradient(circle at 50% 42%, #000 0%, transparent 78%);
}

.durum-kart {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 460px;
  padding: 28px;
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  background: rgba(16, 24, 33, 0.78);
  box-shadow:
    0 24px 60px rgba(0, 0, 0, 0.45),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.durum-ust {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 22px;
}

.durum-logo {
  width: 40px;
  height: 40px;
  object-fit: contain;
  flex-shrink: 0;
}

.durum-ust-metin {
  min-width: 0;
}

.durum-baslik {
  margin: 0;
  font-size: 22px;
  line-height: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #e8eef5;
}

.durum-alt {
  margin: 3px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: #9fb0c0;
}

.durum-genel {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 13px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: rgba(7, 11, 16, 0.55);
  font-size: 14px;
  line-height: 1.45;
}

.durum-nokta {
  width: 9px;
  height: 9px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #fbbf24;
  box-shadow: 0 0 0 4px rgba(251, 191, 36, 0.16);
}

.durum-genel--up .durum-nokta {
  background: #34d399;
  box-shadow: 0 0 0 4px rgba(52, 211, 153, 0.16);
}

.durum-genel--down .durum-nokta,
.durum-genel--hata .durum-nokta {
  background: #f87171;
  box-shadow: 0 0 0 4px rgba(248, 113, 113, 0.16);
}

.durum-genel--bekliyor .durum-nokta {
  background: #6f8294;
  box-shadow: 0 0 0 4px rgba(111, 130, 148, 0.16);
}

.durum-hata-metin {
  margin: 10px 2px 0;
  font-size: 12.5px;
  line-height: 1.5;
  color: #9fb0c0;
}

.durum-detay {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 18px 0 0;
  padding: 14px 16px;
  border-radius: 13px;
  border: 1px solid rgba(148, 163, 184, 0.12);
  background: rgba(148, 163, 184, 0.04);
}

.durum-detay-satir {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.durum-detay-satir dt {
  font-size: 12px;
  font-weight: 600;
  color: #6f8294;
}

.durum-detay-satir dd {
  margin: 0;
  font-size: 12.5px;
  font-weight: 600;
  color: #c8d4df;
  text-align: right;
  min-width: 0;
  overflow-wrap: anywhere;
}

.durum-eylemler {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.durum-buton {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 44px;
  padding: 0 16px;
  border: 1px solid rgba(20, 184, 166, 0.32);
  border-radius: 11px;
  background: rgba(20, 184, 166, 0.12);
  color: #2dd4bf;
  font-family: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: background 160ms ease-out;
}

.durum-buton:hover:not(:disabled) {
  background: rgba(20, 184, 166, 0.2);
}

.durum-buton:disabled {
  opacity: 0.6;
  cursor: progress;
}

.durum-buton:focus-visible,
.durum-baglanti:focus-visible {
  outline: 2px solid #14b8a6;
  outline-offset: 2px;
}

.durum-baglanti {
  display: inline-flex;
  align-items: center;
  min-height: 44px;
  padding: 0 6px;
  border-radius: 8px;
  color: #9fb0c0;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  transition: color 160ms ease-out;
}

.durum-baglanti:hover {
  color: #2dd4bf;
}

@media (max-width: 480px) {
  .durum-kart {
    padding: 22px;
  }
  .durum-eylemler {
    justify-content: center;
  }
}

@media (prefers-reduced-motion: reduce) {
  .durum-buton,
  .durum-baglanti {
    transition: none;
  }
}
</style>
