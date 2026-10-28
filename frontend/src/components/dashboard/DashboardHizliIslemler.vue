<template>
  <div
    class="hizli-islemler"
    data-test="hizli-islemler"
  >
    <button
      type="button"
      class="islem-karti islem-karti--ara"
      :aria-label="t('dashboard.heroAra')"
      @click="aramaAc"
    >
      <span class="islem-ikon"><i
        class="pi pi-search"
        aria-hidden="true"
      /></span>
      <span class="islem-etiket">{{ t('dashboard.heroAra') }}</span>
    </button>
    <router-link
      v-for="islem in islemler"
      :key="islem.to"
      :to="islem.to"
      class="islem-karti"
      :class="`islem-karti--${islem.sinif}`"
    >
      <span class="islem-ikon"><i
        :class="islem.ikon"
        aria-hidden="true"
      /></span>
      <span class="islem-etiket">{{ t(islem.anahtar) }}</span>
    </router-link>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  isAdmin: { type: Boolean, default: false }
})

const emit = defineEmits(['ara'])

const { t } = useI18n()

// `App.vue` bu olayi dinleyip mevcut QuickSearch panelini acar (Ctrl+K bozulmaz).
const aramaAc = () => {
  if (typeof window !== 'undefined' && typeof window.dispatchEvent === 'function') {
    window.dispatchEvent(new CustomEvent('raspel:hizli-ara'))
  }
  emit('ara')
}

/**
 * Yalnizca mevcut rotalar: 7 kisayol + (ADMIN ise) yonetici kokpiti.
 * "Ara" ilk hucrededir; ADMIN'de toplam 9 hucre (3x3), diger rollerde 8.
 */
const islemler = computed(() => {
  const liste = [
    { to: '/teklifler', anahtar: 'dashboard.satisTeklifleri', ikon: 'pi pi-file-edit', sinif: 'teklif' },
    { to: '/faturalar', anahtar: 'dashboard.faturalar', ikon: 'pi pi-file', sinif: 'fatura' },
    { to: '/hizli-satis', anahtar: 'dashboard.hizliSatis', ikon: 'pi pi-shopping-bag', sinif: 'satis' },
    { to: '/cari-hesaplar', anahtar: 'dashboard.cariHesaplar', ikon: 'pi pi-users', sinif: 'cari' },
    { to: '/hareketler', anahtar: 'dashboard.tahsilatOdeme', ikon: 'pi pi-money-bill', sinif: 'tahsilat' },
    { to: '/kasa', anahtar: 'dashboard.kasa', ikon: 'pi pi-wallet', sinif: 'kasa' },
    { to: '/stoklar', anahtar: 'dashboard.stoklar', ikon: 'pi pi-box', sinif: 'stok' }
  ]
  if (props.isAdmin) {
    liste.push({
      to: '/yonetici-kokpiti',
      anahtar: 'dashboard.yoneticiKokpiti',
      ikon: 'pi pi-bolt',
      sinif: 'kokpit'
    })
  }
  return liste
})
</script>

<style scoped>
/* 3x3 izgara: ikon cipi + etiket; her hucre en az 44px dokunma hedefi. */
.hizli-islemler {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 4px;
  min-width: 0;
}
.islem-karti {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 62px;
  min-width: 0;
  padding: 6px 4px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 11.5px;
  font-weight: 600;
  text-align: center;
  text-decoration: none;
  cursor: pointer;
  transition: background var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.islem-karti:hover {
  background: var(--bg-muted);
  color: var(--text-primary);
}
.islem-karti:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: -2px;
}
.islem-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 11px;
  font-size: 14px;
  flex-shrink: 0;
  background: color-mix(in srgb, currentcolor 14%, transparent);
}
.islem-etiket {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.25;
}
.islem-karti--ara {
  color: var(--accent);
}
.islem-karti--kokpit {
  color: #f59e0b;
}
.islem-karti--teklif {
  color: var(--accent);
}
.islem-karti--fatura {
  color: #6366f1;
}
.islem-karti--satis {
  color: #10b981;
}
.islem-karti--cari {
  color: #f59e0b;
}
.islem-karti--tahsilat {
  color: #ef4444;
}
.islem-karti--kasa {
  color: #0ea5e9;
}
.islem-karti--stok {
  color: #d97706;
}
@media (prefers-reduced-motion: reduce) {
  .islem-karti {
    transition: none;
  }
}
</style>
