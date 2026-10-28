<template>
  <div
    class="kritik-stok"
    data-test="kritik-stok"
  >
    <div
      v-if="yukleniyor && !stok"
      class="kritik-iskelet"
      aria-hidden="true"
    >
      <span class="iskelet iskelet--buyuk" />
      <span class="iskelet iskelet--kucuk" />
    </div>

    <template v-else>
      <router-link
        to="/stoklar?kritik=1"
        class="kritik-kart"
        :class="{ 'kritik-kart--uyari': kritikAdet > 0 }"
      >
        <span class="kritik-ikon">
          <i
            class="pi pi-exclamation-triangle"
            aria-hidden="true"
          />
        </span>
        <span class="kritik-icerik">
          <span class="kritik-baslik">{{ t('dashboard.kritikStokUyarilari') }}</span>
          <strong class="kritik-deger">{{ kritikAdet }}</strong>
          <span class="kritik-alt">{{ t('dashboard.kritikStokSayisi', { n: kritikAdet }) }}</span>
        </span>
        <i
          class="pi pi-arrow-right kritik-git"
          aria-hidden="true"
        />
      </router-link>

      <div class="kritik-ozet">
        <div class="kritik-chip">
          <span class="kritik-chip-etiket">{{ t('dashboard.kritikStokCesit') }}</span>
          <strong>{{ cesit }}</strong>
        </div>
        <div class="kritik-chip">
          <span class="kritik-chip-etiket">{{ t('dashboard.kritikStokDegeri') }}</span>
          <strong class="gizli-veri">{{ formatCurrency(deger) }}</strong>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatCurrency } from '../../utils/format.js'

const props = defineProps({
  stok: { type: Object, default: null },
  yukleniyor: { type: Boolean, default: false }
})

const { t } = useI18n()

const cesit = computed(() => Number(props.stok?.cesit) || 0)
const deger = computed(() => Number(props.stok?.deger) || 0)
const kritikAdet = computed(() => Number(props.stok?.kritikAdet) || 0)
</script>

<style scoped>
.kritik-stok {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.kritik-kart {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: inherit;
  text-decoration: none;
  transition: border-color var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.kritik-kart--uyari {
  border-color: var(--danger-border);
  background: linear-gradient(180deg, var(--danger-soft), transparent 70%), var(--bg-card);
}
.kritik-kart:hover {
  border-color: var(--accent-border);
}
.kritik-kart:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.kritik-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--danger-soft);
  color: var(--danger);
  font-size: 16px;
  flex-shrink: 0;
}
.kritik-icerik {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
  flex: 1;
}
.kritik-baslik {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
}
.kritik-deger {
  font-size: 20px;
  font-weight: 800;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
  color: var(--danger);
}
.kritik-alt {
  font-size: 11px;
  color: var(--text-muted);
}
.kritik-git {
  font-size: 11px;
  color: var(--text-muted);
}
.kritik-ozet {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}
.kritik-chip {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 10px;
  border-radius: 10px;
  background: var(--bg-muted);
  min-width: 0;
}
.kritik-chip-etiket {
  font-size: 11px;
  color: var(--text-secondary);
}
.kritik-chip strong {
  font-size: 13px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.kritik-iskelet {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.iskelet {
  display: block;
  border-radius: 12px;
  background: var(--bg-muted);
  animation: iskeletParlama 1.4s ease infinite;
}
.iskelet--buyuk {
  height: 64px;
}
.iskelet--kucuk {
  height: 40px;
}
@keyframes iskeletParlama {
  0% {
    opacity: 0.45;
  }
  50% {
    opacity: 0.85;
  }
  100% {
    opacity: 0.45;
  }
}
@media (prefers-reduced-motion: reduce) {
  .iskelet {
    animation: none;
  }
}
</style>
