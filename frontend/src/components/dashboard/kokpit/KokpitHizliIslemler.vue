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
        class="hizli-islem hizli-islem--birincil"
        data-test="hizli-tahsilat"
        @click="emit('tahsilat')"
      >
        <span
          class="hizli-ikon"
          aria-hidden="true"
        >
          <i class="pi pi-wallet" />
        </span>
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
        <span
          class="hizli-ikon"
          aria-hidden="true"
        >
          <i class="pi pi-credit-card" />
        </span>
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
        <span
          class="hizli-ikon"
          aria-hidden="true"
        >
          <i class="pi pi-file-plus" />
        </span>
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
        <span
          class="hizli-ikon"
          aria-hidden="true"
        >
          <i class="pi pi-receipt" />
        </span>
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
        <span
          class="hizli-ikon"
          aria-hidden="true"
        >
          <i class="pi pi-check-square" />
        </span>
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
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 7px;
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
.hizli-islem--birincil {
  grid-column: span 2;
  min-height: 58px;
}
.hizli-islem:last-child:nth-child(4) {
  grid-column: span 2;
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
.hizli-ikon {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: var(--bg-muted);
  color: var(--text-secondary);
}
.hizli-islem--birincil .hizli-ikon {
  background: var(--accent-soft);
  color: var(--accent);
}
.hizli-islem--birincil .hizli-etiket {
  font-size: 13px;
  font-weight: 700;
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
  color: var(--danger-text);
  font-size: 10px;
  font-weight: 700;
  text-align: center;
}
@media (pointer: coarse) {
  .hizli-islem {
    min-height: 48px;
  }
  .hizli-islem--birincil {
    min-height: 58px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .hizli-islem {
    transition: none;
  }
}
</style>
