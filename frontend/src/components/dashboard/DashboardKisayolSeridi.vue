<template>
  <nav
    class="kisayol-serit"
    :aria-label="t('dashboard.kisayolSeridi')"
    data-test="kisayol-seridi"
  >
    <router-link
      v-for="k in kisayollar"
      :key="k.key"
      :to="k.to"
      class="kisayol"
      :class="`kisayol--${k.key}`"
      :data-kisayol="k.key"
    >
      <span class="kisayol-ikon"><i
        :class="k.ikon"
        aria-hidden="true"
      /></span>
      <span class="kisayol-etiket">{{ t(k.anahtar) }}</span>
    </router-link>
  </nav>
</template>

<script setup>
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

/**
 * Finans masasinin ust kisayol seridi. Yalnizca mevcut rotalar kullanilir;
 * tek satir, yatay kaydirilabilir, her hedef en az 40px yuksekligindedir.
 */
const kisayollar = [
  { key: 'yeniFatura', to: '/faturalar', anahtar: 'dashboard.kisayolYeniFatura', ikon: 'pi pi-file' },
  { key: 'tahsilat', to: '/hareketler', anahtar: 'dashboard.kisayolTahsilat', ikon: 'pi pi-arrow-down-left' },
  { key: 'odeme', to: '/hareketler', anahtar: 'dashboard.kisayolOdeme', ikon: 'pi pi-arrow-up-right' },
  { key: 'hizliSatis', to: '/hizli-satis', anahtar: 'dashboard.kisayolHizliSatis', ikon: 'pi pi-shopping-bag' },
  { key: 'yeniCari', to: '/cari-hesaplar', anahtar: 'dashboard.kisayolYeniCari', ikon: 'pi pi-users' },
  { key: 'kasa', to: '/kasa', anahtar: 'dashboard.kisayolKasa', ikon: 'pi pi-wallet' },
  { key: 'stoklar', to: '/stoklar', anahtar: 'dashboard.kisayolStoklar', ikon: 'pi pi-box' },
  { key: 'teklifler', to: '/teklifler', anahtar: 'dashboard.kisayolTeklifler', ikon: 'pi pi-file-edit' }
]
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
.kisayol {
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
.kisayol:hover {
  border-color: var(--accent-border);
  color: var(--text-primary);
  box-shadow: var(--elev-1);
  transform: translateY(-1px);
}
.kisayol:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
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
@media (prefers-reduced-motion: reduce) {
  .kisayol {
    transition: none;
  }
}
</style>
