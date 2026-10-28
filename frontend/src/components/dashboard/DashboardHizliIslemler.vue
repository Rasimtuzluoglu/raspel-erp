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
      <i
        class="pi pi-search"
        aria-hidden="true"
      />
      <span>{{ t('dashboard.heroAra') }}</span>
    </button>
    <router-link
      v-for="islem in islemler"
      :key="islem.to"
      :to="islem.to"
      class="islem-karti"
      :class="`islem-karti--${islem.sinif}`"
    >
      <i
        :class="islem.ikon"
        aria-hidden="true"
      />
      <span>{{ t(islem.anahtar) }}</span>
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

const islemler = computed(() => {
  const liste = [
    { to: '/teklifler', anahtar: 'dashboard.satisTeklifleri', ikon: 'pi pi-file-edit', sinif: 'teklif' },
    { to: '/saha-portali', anahtar: 'dashboard.sahaPortali', ikon: 'pi pi-compass', sinif: 'saha' },
    { to: '/faturalar', anahtar: 'dashboard.yeniFatura', ikon: 'pi pi-file', sinif: 'fatura' },
    { to: '/hizli-satis', anahtar: 'dashboard.hizliSatis', ikon: 'pi pi-shopping-bag', sinif: 'satis' },
    { to: '/cari-hesaplar', anahtar: 'dashboard.yeniCari', ikon: 'pi pi-user-plus', sinif: 'cari' },
    { to: '/hareketler', anahtar: 'dashboard.tahsilatOdeme', ikon: 'pi pi-money-bill', sinif: 'tahsilat' },
    { to: '/kasa', anahtar: 'dashboard.kasa', ikon: 'pi pi-wallet', sinif: 'kasa' },
    { to: '/stoklar', anahtar: 'dashboard.stoklar', ikon: 'pi pi-box', sinif: 'stok' }
  ]
  if (props.isAdmin) {
    liste.unshift({
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
/*
 * Sag rayda 2 kolonlu kompakt ikon izgarasi: kart cercevesi yok,
 * her kisayol ikon cipi + etiketten olusur.
 */
.hizli-islemler {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px;
}
.islem-karti {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 38px;
  padding: 6px 8px;
  margin: 0 -8px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 600;
  text-align: left;
  text-decoration: none;
  cursor: pointer;
  min-width: 0;
  transition: background var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.islem-karti:hover {
  background: var(--bg-card);
}
.islem-karti:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: -2px;
}
.islem-karti span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.islem-karti i {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 10px;
  font-size: 13px;
  flex-shrink: 0;
  background: color-mix(in srgb, currentcolor 14%, transparent);
}
.islem-karti--ara {
  color: var(--accent);
}
.islem-karti--kokpit i {
  color: #f59e0b;
}
.islem-karti--teklif i {
  color: var(--accent);
}
.islem-karti--saha i {
  color: #a78bfa;
}
.islem-karti--fatura i {
  color: #6366f1;
}
.islem-karti--satis i {
  color: #10b981;
}
.islem-karti--cari i {
  color: #f59e0b;
}
.islem-karti--tahsilat i {
  color: #ef4444;
}
.islem-karti--kasa i {
  color: #0ea5e9;
}
.islem-karti--stok i {
  color: #d97706;
}
@media (prefers-reduced-motion: reduce) {
  .islem-karti {
    transition: none;
  }
}
</style>
