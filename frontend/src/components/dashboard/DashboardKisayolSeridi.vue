<template>
  <nav
    class="kisayol-serit"
    :aria-label="t('dashboard.kisayolSeridi')"
    data-test="kisayol-seridi"
  >
    <router-link
      v-for="k in seciliKisayollar"
      :key="k.key"
      :to="k.to"
      class="kisayol"
      :class="`kisayol--${k.renk}`"
      :data-kisayol="k.key"
    >
      <span class="kisayol-ikon"><i
        :class="k.ikon"
        aria-hidden="true"
      /></span>
      <span class="kisayol-etiket">{{ t(k.anahtar) }}</span>
    </router-link>

    <button
      type="button"
      class="kisayol-duzenle"
      :aria-label="t('dashboard.kisayolDuzenle')"
      :title="t('dashboard.kisayolDuzenle')"
      data-test="kisayol-duzenle"
      @click="ayarlariAc"
    >
      <span class="kisayol-ikon"><i
        class="pi pi-pencil"
        aria-hidden="true"
      /></span>
      <span class="kisayol-etiket">{{ t('dashboard.kisayolDuzenle') }}</span>
    </button>

    <span
      class="sr-only"
      role="status"
      aria-live="polite"
    >{{ duyuru }}</span>

    <DashboardKisayolAyarlari
      :visible="ayarlarAcik"
      :secim="secim"
      :is-admin="isAdmin"
      @update:visible="ayarlarAcik = $event"
      @kaydet="kaydet"
    />
  </nav>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import DashboardKisayolAyarlari from './DashboardKisayolAyarlari.vue'
import {
  kisayollariCoz,
  kisayollariOku,
  kisayollariYaz
} from '../../utils/dashboardKisayollar.js'

const props = defineProps({
  /** ADMIN olmayan kullanici kokpit/yedekler kisayollarini gormez. */
  isAdmin: { type: Boolean, default: false }
})

const { t } = useI18n()

/**
 * Finans masasinin ust kisayol seridi. Secim cihazda `raspel_dashboard_kisayollar`
 * anahtarinda tutulur; siralama her zaman katalog sirasidir.
 */
const secim = ref(kisayollariOku(props.isAdmin))
const ayarlarAcik = ref(false)
const duyuru = ref('')

const seciliKisayollar = computed(() => kisayollariCoz(secim.value, props.isAdmin))

const ayarlariAc = () => {
  duyuru.value = ''
  ayarlarAcik.value = true
}

const kaydet = (keys) => {
  secim.value = kisayollariYaz(keys, props.isAdmin)
  ayarlarAcik.value = false
  duyuru.value = t('dashboard.kisayolKaydedildi')
}

// Rol degisirse (oturum yenilenmesi) secim yeniden okunur; yetkisiz kayitlar duser.
watch(
  () => props.isAdmin,
  (adminMi) => {
    secim.value = kisayollariOku(adminMi)
  }
)
</script>

<style scoped>
/* Tek satir; dar ekranda yatay kaydirilir, tasma yapmaz. */
.kisayol-serit {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 2px 0;
  overflow-x: auto;
  overscroll-behavior-x: contain;
  scrollbar-width: thin;
}
.kisayol,
.kisayol-duzenle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  text-decoration: none;
  white-space: nowrap;
  transition: border-color var(--dur-base, 0.2s) var(--ease-standard, ease),
    color var(--dur-base, 0.2s) var(--ease-standard, ease),
    box-shadow var(--dur-base, 0.2s) var(--ease-standard, ease),
    transform var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.kisayol:hover,
.kisayol-duzenle:hover {
  border-color: var(--accent-border);
  color: var(--text-primary);
  box-shadow: var(--elev-1);
  transform: translateY(-1px);
}
.kisayol:focus-visible,
.kisayol-duzenle:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.kisayol-duzenle {
  border-style: dashed;
  cursor: pointer;
  font-family: inherit;
}
.kisayol-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 7px;
  font-size: 11.5px;
  flex-shrink: 0;
  background: color-mix(in srgb, currentcolor 14%, transparent);
}
.kisayol-etiket {
  overflow: hidden;
  text-overflow: ellipsis;
}
.kisayol--yeniFatura {
  color: #6366f1;
}
.kisayol--tahsilat {
  color: #10b981;
}
.kisayol--odeme {
  color: #ef4444;
}
.kisayol--hizliSatis {
  color: #10b981;
}
.kisayol--yeniCari {
  color: #f59e0b;
}
.kisayol--kasa {
  color: #0ea5e9;
}
.kisayol--stoklar {
  color: #d97706;
}
.kisayol--teklifler {
  color: var(--accent);
}
.kisayol--satinalma {
  color: #8b5cf6;
}
.kisayol--onaylar {
  color: #14b8a6;
}
.kisayol--raporlar {
  color: #3b82f6;
}
.kisayol--kokpit {
  color: #0d9488;
}
.kisayol--ajanda {
  color: #ec4899;
}
.kisayol--yedekler {
  color: #64748b;
}
@media (prefers-reduced-motion: reduce) {
  .kisayol,
  .kisayol-duzenle {
    transition: none;
  }
}
</style>
