<template>
  <div
    class="hizli-islemler"
    data-test="hizli-islemler"
  >
    <router-link
      v-for="islem in islemler"
      :key="islem.to"
      :to="islem.to"
      class="islem-karti"
      :class="`islem-karti--${islem.sinif}`"
      :data-islem="islem.sinif"
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

const { t } = useI18n()

/**
 * Sag paneldeki aksiyon izgarasi: 6 kisayol (ADMIN). Yonetici kokpiti ve
 * yedekler yalnizca ADMIN'e gorunur; diger rollerde 4 kalir. Yalnizca mevcut
 * rotalar kullanilir.
 */
const islemler = computed(() => {
  const liste = [
    { to: '/satinalma', anahtar: 'dashboard.satinalma', ikon: 'pi pi-shopping-cart', sinif: 'satinalma' },
    { to: '/onaylar', anahtar: 'dashboard.onaylar', ikon: 'pi pi-check-square', sinif: 'onaylar' },
    { to: '/raporlar', anahtar: 'dashboard.raporlar', ikon: 'pi pi-chart-bar', sinif: 'raporlar' }
  ]
  if (props.isAdmin) {
    liste.push({
      to: '/yonetici-kokpiti',
      anahtar: 'dashboard.yoneticiKokpiti',
      ikon: 'pi pi-bolt',
      sinif: 'kokpit'
    })
  }
  liste.push({ to: '/ajanda', anahtar: 'dashboard.ajanda', ikon: 'pi pi-calendar', sinif: 'ajanda' })
  if (props.isAdmin) {
    liste.push({ to: '/yedekler', anahtar: 'dashboard.yedekler', ikon: 'pi pi-save', sinif: 'yedekler' })
  }
  return liste
})
</script>

<style scoped>
/* 3 kolonlu aksiyon izgarasi; her hucre en az 44px dokunma hedefi. */
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
  min-height: 58px;
  min-width: 0;
  padding: 6px 4px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 11px;
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
  width: 34px;
  height: 34px;
  border-radius: 10px;
  font-size: 13.5px;
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
.islem-karti--satinalma {
  color: #f59e0b;
}
.islem-karti--onaylar {
  color: #10b981;
}
.islem-karti--raporlar {
  color: #3b82f6;
}
.islem-karti--kokpit {
  color: #f59e0b;
}
.islem-karti--ajanda {
  color: #8b5cf6;
}
.islem-karti--yedekler {
  color: #0ea5e9;
}
@media (max-width: 460px) {
  .hizli-islemler {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (prefers-reduced-motion: reduce) {
  .islem-karti {
    transition: none;
  }
}
</style>
