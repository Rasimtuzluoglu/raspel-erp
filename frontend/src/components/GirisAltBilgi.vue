<template>
  <footer
    class="giris-alt-bilgi"
    role="contentinfo"
  >
    <div class="gab-sol">
      <div
        class="gab-dil"
        data-test="giris-dil"
        role="group"
        :aria-label="t('giris.dil')"
      >
        <button
          type="button"
          class="gab-dil-btn"
          :class="{ aktif: aktifDil === 'tr' }"
          :aria-pressed="aktifDil === 'tr' ? 'true' : 'false'"
          data-test="giris-dil-tr"
          @click="dilDegistir('tr')"
        >
          TR
        </button>
        <span
          class="gab-ayrac"
          aria-hidden="true"
        >/</span>
        <button
          type="button"
          class="gab-dil-btn"
          :class="{ aktif: aktifDil === 'en' }"
          :aria-pressed="aktifDil === 'en' ? 'true' : 'false'"
          data-test="giris-dil-en"
          @click="dilDegistir('en')"
        >
          EN
        </button>
      </div>
    </div>

    <div class="gab-sag">
      <a
        v-if="durum"
        class="gab-baglanti gab-durum"
        :class="'gab-durum--' + durum.seviye"
        href="/durum"
        data-test="giris-durum"
      >
        <span class="gab-gizli">{{ t('giris.sistemDurumu') }}</span>
        <span
          class="gab-nokta"
          aria-hidden="true"
        />
        <span>{{ durum.seviye === 'up' ? t('giris.durumUp') : t('giris.durumKismi') }}</span>
      </a>

      <a
        class="gab-baglanti"
        href="mailto:destek@raspel.com.tr"
        data-test="giris-destek"
      >
        {{ t('giris.destek') }}
      </a>

      <a
        class="gab-baglanti"
        href="/kullanim-sartlari"
        data-test="giris-kvkk"
      >
        {{ t('giris.kvkk') }}
      </a>

      <span
        v-if="surum"
        class="gab-surum"
      >
        {{ t('giris.surum') }} {{ surum }}
      </span>
    </div>

    <p class="gab-telif">
      {{ t('giris.copyright') }}
    </p>
  </footer>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useLocale } from '../composables/useLocale.js'
import { durumAPI } from '../api/index.js'

const { t } = useI18n()
const { aktifDil, dilDegistir } = useLocale()

const surum = typeof __APP_VERSION__ !== 'undefined' ? __APP_VERSION__ : ''
const durum = ref(null)

const seviyeHesapla = (deger) => {
  const normal = String(deger || '').trim().toUpperCase()
  if (['UP', 'OK', 'CALISIYOR', 'SAGLIKLI', 'HEALTHY'].includes(normal)) return 'up'
  return 'amber'
}

onMounted(async () => {
  try {
    const res = await durumAPI.durum()
    const veri = res?.data || {}
    if (!veri.durum) return
    durum.value = {
      seviye: seviyeHesapla(veri.durum),
      surum: veri.surum || '',
      zaman: veri.zaman || ''
    }
  } catch {
    // Ulaşılamazsa durum bağlantısı sessizce gizlenir.
    durum.value = null
  }
})
</script>

<style scoped>
.giris-alt-bilgi {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 4px 14px;
  width: 100%;
  max-width: 420px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--giris-kenar);
  font-size: 11.5px;
  color: var(--giris-metin-3);
}

.gab-sol,
.gab-sag {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px 8px;
  min-width: 0;
}

.gab-gizli {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.gab-dil {
  display: inline-flex;
  align-items: center;
  gap: 1px;
}

.gab-dil-btn {
  min-height: 44px;
  min-width: 40px;
  padding: 0 6px;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: var(--giris-metin-3);
  font-family: inherit;
  font-size: 11.5px;
  font-weight: 700;
  letter-spacing: 0.04em;
  cursor: pointer;
  transition: color 160ms ease-out, background 160ms ease-out;
}

.gab-dil-btn:hover {
  color: var(--giris-metin);
}

.gab-dil-btn.aktif {
  color: var(--giris-aksan-parlak);
  background: var(--giris-aksan-yumusak);
}

.gab-dil-btn:focus-visible,
.gab-baglanti:focus-visible {
  outline: 2px solid var(--giris-aksan);
  outline-offset: 2px;
}

.gab-ayrac {
  color: var(--giris-kenar-3);
}

.gab-baglanti {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 44px;
  padding: 0 4px;
  border-radius: 6px;
  color: var(--giris-metin-2);
  text-decoration: none;
  font-weight: 600;
  transition: color 160ms ease-out;
}

.gab-baglanti:hover {
  color: var(--giris-aksan-parlak);
}

.gab-nokta {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--giris-uyari);
  box-shadow: 0 0 0 3px rgba(251, 191, 36, 0.16);
}

.gab-durum--up .gab-nokta {
  background: var(--giris-basari);
  box-shadow: 0 0 0 3px rgba(52, 211, 153, 0.16);
}

.gab-surum {
  padding: 0 2px;
  color: var(--giris-metin-3);
  white-space: nowrap;
}

.gab-telif {
  width: 100%;
  margin: 2px 0 0;
  font-size: 10.5px;
  color: var(--giris-metin-3);
}

@media (max-width: 480px) {
  .giris-alt-bilgi {
    justify-content: center;
    gap: 2px 10px;
  }
  .gab-sag {
    justify-content: center;
  }
}

@media (prefers-reduced-motion: reduce) {
  .gab-dil-btn,
  .gab-baglanti {
    transition: none;
  }
}
</style>
