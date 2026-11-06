<template>
  <section
    class="kokpit-hizli-islemler"
    data-test="kokpit-hizli-islemler"
    :aria-label="t('dashboard.hizliIslemler')"
  >
    <router-link
      to="/faturalar?yeni=1"
      class="hizli-islem"
      data-test="hizli-yeni-fatura"
    >
      <i
        class="pi pi-file-plus"
        aria-hidden="true"
      />
      <span>{{ t('dashboard.yeniFatura') }}</span>
    </router-link>

    <button
      type="button"
      class="hizli-islem"
      data-test="hizli-tahsilat"
      @click="emit('tahsilat')"
    >
      <i
        class="pi pi-wallet"
        aria-hidden="true"
      />
      <span>{{ t('dashboard.kokpitTahsilat') }}</span>
    </button>

    <button
      type="button"
      class="hizli-islem"
      data-test="hizli-odeme"
      @click="emit('odeme')"
    >
      <i
        class="pi pi-credit-card"
        aria-hidden="true"
      />
      <span>{{ t('dashboard.kokpitHizliOdeme') }}</span>
    </button>

    <router-link
      to="/masraflar?yeni=1"
      class="hizli-islem"
      data-test="hizli-masraf"
    >
      <i
        class="pi pi-receipt"
        aria-hidden="true"
      />
      <span>{{ t('dashboard.kokpitHizliMasraf') }}</span>
    </router-link>

    <button
      v-if="onayGorebilir"
      type="button"
      class="hizli-islem"
      data-test="hizli-onaylar"
      @click="emit('onaylar')"
    >
      <i
        class="pi pi-check-square"
        aria-hidden="true"
      />
      <span>{{ t('dashboard.onaylar') }}</span>
      <span
        v-if="bekleyenOnay > 0"
        class="hizli-islem-rozet"
      >{{ bekleyenOnay }}</span>
    </button>
  </section>
</template>

<script setup>
import { useI18n } from 'vue-i18n'

defineProps({
  bekleyenOnay: { type: Number, default: 0 },
  onayGorebilir: { type: Boolean, default: false }
})

const emit = defineEmits(['tahsilat', 'odeme', 'onaylar'])
const { t } = useI18n()
</script>

<style scoped>
.kokpit-hizli-islemler {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6px;
  min-width: 0;
}
.hizli-islem {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 38px;
  padding: 6px 9px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 11.5px;
  font-weight: 600;
  text-decoration: none;
  white-space: nowrap;
  cursor: pointer;
  overflow: hidden;
  transition:
    border-color var(--dur-fast) var(--ease-standard),
    background var(--dur-fast) var(--ease-standard),
    color var(--dur-fast) var(--ease-standard);
}
.hizli-islem:hover,
.hizli-islem:focus-visible {
  border-color: var(--accent-border);
  background: var(--bg-secondary);
  color: var(--accent);
}
.hizli-islem:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.hizli-islem i {
  flex-shrink: 0;
  font-size: 12px;
}
.hizli-islem span {
  overflow: hidden;
  text-overflow: ellipsis;
}
.hizli-islem-rozet {
  flex-shrink: 0;
  min-width: 17px;
  padding: 1px 4px;
  border-radius: var(--radius-pill);
  background: var(--danger-soft);
  color: var(--danger);
  font-size: 9.5px;
  font-weight: 700;
  text-align: center;
}
@media (max-width: 900px) {
  .kokpit-hizli-islemler {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
@media (max-width: 520px) {
  .kokpit-hizli-islemler {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .hizli-islem {
    min-height: 42px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .hizli-islem {
    transition: none;
  }
}
</style>
