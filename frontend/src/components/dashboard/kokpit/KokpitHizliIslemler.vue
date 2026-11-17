<template>
  <section
    class="kokpit-hizli-islemler kart"
    data-test="kokpit-hizli-islemler"
    :aria-label="t('dashboard.hizliIslemler')"
  >
    <header class="hizli-baslik">
      <h2 class="hizli-baslik-metin">
        {{ t('dashboard.hizliIslemler') }}
      </h2>
    </header>

    <div class="hizli-liste">
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
        <span class="hizli-etiket">{{ t('dashboard.kokpitTahsilat') }}</span>
        <i
          class="pi pi-angle-right hizli-ok"
          aria-hidden="true"
        />
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
        <span class="hizli-etiket">{{ t('dashboard.kokpitHizliOdeme') }}</span>
        <i
          class="pi pi-angle-right hizli-ok"
          aria-hidden="true"
        />
      </button>

      <router-link
        to="/faturalar?yeni=1"
        class="hizli-islem"
        data-test="hizli-yeni-fatura"
      >
        <i
          class="pi pi-file-plus"
          aria-hidden="true"
        />
        <span class="hizli-etiket">{{ t('dashboard.yeniFatura') }}</span>
        <i
          class="pi pi-angle-right hizli-ok"
          aria-hidden="true"
        />
      </router-link>

      <router-link
        to="/masraflar?yeni=1"
        class="hizli-islem"
        data-test="hizli-masraf"
      >
        <i
          class="pi pi-receipt"
          aria-hidden="true"
        />
        <span class="hizli-etiket">{{ t('dashboard.kokpitHizliMasraf') }}</span>
        <i
          class="pi pi-angle-right hizli-ok"
          aria-hidden="true"
        />
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
        <span class="hizli-etiket">{{ t('dashboard.onaylar') }}</span>
        <span
          v-if="bekleyenOnay > 0"
          class="hizli-islem-rozet"
        >{{ bekleyenOnay }}</span>
        <i
          v-else
          class="pi pi-angle-right hizli-ok"
          aria-hidden="true"
        />
      </button>
    </div>
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
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  height: 100%;
}
.hizli-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 2px;
}
.hizli-baslik-metin {
  margin: 0;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--text-secondary);
  text-transform: uppercase;
}
.hizli-liste {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.hizli-islem {
  position: relative;
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  min-height: 40px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  text-decoration: none;
  text-align: left;
  cursor: pointer;
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
  font-size: 13px;
}
.hizli-etiket {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.hizli-ok {
  color: var(--text-muted);
  font-size: 10px;
}
.hizli-islem-rozet {
  flex-shrink: 0;
  min-width: 18px;
  padding: 1px 5px;
  border-radius: var(--radius-pill);
  background: var(--danger-soft);
  color: var(--danger);
  font-size: 10px;
  font-weight: 700;
  text-align: center;
}
@media (pointer: coarse) {
  .hizli-islem {
    min-height: 44px;
  }
}
@media (max-width: 640px) {
  .hizli-liste {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (prefers-reduced-motion: reduce) {
  .hizli-islem {
    transition: none;
  }
}
</style>
