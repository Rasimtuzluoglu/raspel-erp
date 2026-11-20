<template>
  <div
    v-if="sonUrunler && sonUrunler.length > 0"
    class="cari-urunler"
  >
    <div class="cari-not-baslik">
      <h4>
        <i
          class="pi pi-box"
          style="margin-right: 6px"
        />{{ t('cariHesaplar.gecmisteAldigiUrunler') }}
      </h4>
    </div>
    <div class="cari-urunler-grid">
      <button
        v-for="u in sonUrunler"
        :key="u.stokId"
        type="button"
        class="cari-urun-chip"
        @click="emit('goster', u)"
      >
        <span class="cari-urun-ad">{{ u.stokAd }}</span>
        <span class="cari-urun-bilgi">{{ u.sonAlisTarihi }} · {{ t('cariHesaplar.nAdet', { n: u.adet }) }}</span>
        <span class="cari-urun-fiyat">{{ formatCurrency(u.sonBirimFiyat) }}</span>
      </button>
    </div>
    <div
      v-if="secili"
      class="cari-urun-fiyat-gecmisi"
    >
      <div class="fg-baslik">
        <span><i class="pi pi-chart-line" /> {{ secili.urunAd }} — {{ t('cariHesaplar.fiyatGecmisi') }}</span>
        <button
          type="button"
          class="fg-kapat"
          :aria-label="t('common.close')"
          @click="emit('kapat')"
        >
          <i class="pi pi-times" />
        </button>
      </div>
      <div
        v-for="(k, i) in secili.gecmis"
        :key="i"
        class="fg-satir"
      >
        <span class="fg-tarih">{{ formatDate(k.tarih) }}</span>
        <span class="fg-fatura">{{ k.faturaNumarasi }}</span>
        <span class="fg-adet">{{ t('cariHesaplar.nAdet', { n: k.adet }) }}</span>
        <span class="fg-fiyat">{{ formatCurrency(k.birimFiyat) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatCurrency, formatDate } from '../../utils/format.js'

/** Cari 360 kartı > Geçmişte Aldığı Ürünler sekmesi. */
defineProps({
  sonUrunler: { type: Array, default: () => [] },
  secili: { type: Object, default: null }
})

const emit = defineEmits(['goster', 'kapat'])
const { t } = useI18n()
</script>

<style scoped>
.cari-urunler {
  margin-top: 20px;
  border-top: 1px solid var(--border);
  padding-top: 14px;
}

.cari-not-baslik h4 {
  margin: 0 0 10px;
  font-size: 14px;
}

.cari-urunler-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.cari-urun-chip {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 8px 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  text-align: left;
}

.cari-urun-ad {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.cari-urun-bilgi {
  font-size: 11px;
  color: var(--text-muted);
}

.cari-urun-fiyat {
  font-size: 12px;
  font-weight: 700;
  color: var(--accent);
}

.cari-urun-fiyat-gecmisi {
  margin-top: 12px;
  padding: 12px;
  background: var(--purple-soft);
  border: 1px solid var(--purple-border);
  border-radius: 8px;
}

.fg-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  font-weight: 600;
  color: var(--purple);
  margin-bottom: 8px;
}

.fg-kapat {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
}

.fg-satir {
  display: flex;
  gap: 12px;
  align-items: center;
  font-size: 12px;
  color: var(--text-secondary);
  padding: 3px 0;
}

.fg-tarih {
  width: 80px;
  flex-shrink: 0;
}

.fg-fatura {
  flex: 1;
}

.fg-adet {
  width: 60px;
  text-align: right;
}

.fg-fiyat {
  font-weight: 600;
  color: var(--text-primary);
  min-width: 80px;
  text-align: right;
}
</style>
