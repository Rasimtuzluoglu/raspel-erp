<template>
  <div
    :id="domId"
    class="product-card"
    :class="{
      'stok-yok': stokYokMu(urun),
      sepette: sepetteAdet > 0,
      'izgara-odakli': odakli
    }"
    role="option"
    :tabindex="tabindex === undefined ? 0 : tabindex"
    :aria-selected="odakli"
    :aria-label="urun.ad"
    :aria-disabled="stokYokMu(urun)"
    @click="emit('sec')"
    @keydown.enter.prevent="emit('sec')"
    @keydown.space.prevent="emit('sec')"
    @contextmenu.prevent="emit('adet-ist', $event)"
  >
    <div class="product-gorsel">
      <img
        v-if="urun.fotoThumbUrl || urun.fotoUrl"
        :src="urun.fotoThumbUrl || urun.fotoUrl"
        :alt="urun.ad"
        loading="lazy"
        decoding="async"
      >
      <span
        v-else
        class="product-bas-harf"
      >{{ (urun.ad || '?').charAt(0).toLocaleUpperCase('tr-TR') }}</span>
    </div>
    <div class="product-icerik">
      <span class="product-name">{{ urun.ad }}</span>
      <span class="product-kod">{{ urun.barkod || urun.stokKodu || '-' }}</span>
      <div
        class="product-stok-satir"
        :class="stokRenkSinifi(urun)"
      >
        <i class="pi pi-circle-fill" />
        <span>{{ stokEtiketi(urun) }}</span>
      </div>
      <div class="product-fiyat-satir">
        <span class="product-price">{{ formatCurrency(satisFiyati(urun)) }}</span>
        <span class="kdv-not">{{ kdvNotu(urun) }}</span>
      </div>
      <span
        v-if="cariFiyat"
        class="product-cari-fiyat"
        :title="t('hizliSatis.cariOzelFiyat')"
      >
        <i class="pi pi-user" /> {{ formatCurrency(cariFiyat) }}
      </span>
    </div>
    <span
      v-if="sepetteAdet > 0"
      class="sepette-rozet"
    >{{ sepetteAdet }}</span>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatCurrency } from '../utils/format.js'

defineProps({
  urun: { type: Object, required: true },
  cariFiyat: { type: Number, default: null },
  sepetteAdet: { type: Number, default: 0 },
  // Izgara klavyeyle gezilebilirken (HizliSatis `urunIzgaraOdak`) aktif kart
  // vurgulanir ve `aria-selected` ile duyurulur. Roving tabindex: odakli kart 0,
  // digerleri -1 (Tab tek seferde ızgaraya girer, ok tuslari icerde gezer).
  odakli: { type: Boolean, default: false },
  tabindex: { type: Number, default: undefined },
  domId: { type: String, default: undefined }
})
const emit = defineEmits(['sec', 'adet-ist'])
const { t } = useI18n()

const satisFiyati = (u) => Number(u?.satisFiyati || u?.fiyat || 0)
const stokYokMu = (u) => Number(u?.miktar || 0) <= 0

// Kartta stogun KDV orani gosterilir; tanimli degilse "KDV dahil" yazilir.
const kdvNotu = (u) =>
  u?.kdvOrani != null ? t('hizliSatis.kdvOranli', { oran: Number(u.kdvOrani) }) : t('hizliSatis.kdvDahil')

const kritikStokMu = (u) => {
  if (!u?.miktar) return false
  if (u.minMiktar != null && u.miktar <= u.minMiktar) return true
  return u.miktar <= 10
}

const stokEtiketi = (u) => {
  if (stokYokMu(u)) return t('hizliSatis.stokYok')
  const birim = u?.birim || t('hizliSatis.adetBirimi')
  if (kritikStokMu(u)) return t('hizliSatis.stokSon', { n: Math.floor(u.miktar), birim })
  return t('hizliSatis.stokAdet', { n: u.miktar, birim })
}

const stokRenkSinifi = (u) => (stokYokMu(u) ? 'yok' : (kritikStokMu(u) ? 'kritik' : 'normal'))
</script>

<style scoped>
.pos-buyuk .product-kod {
  font-size: 14px;
}
.pos-buyuk .product-name {
  font-size: 17px;
}
.pos-buyuk .product-price {
  font-size: 19px;
}
.pos-buyuk .product-cari-fiyat {
  font-size: 15px;
}
.pos-buyuk .product-cari-fiyat i {
  font-size: 14px;
}
.pos-buyuk .product-card {
  padding: 14px;
}
.product-card {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  cursor: pointer;
  transition: transform var(--dur-fast, 0.15s) var(--ease-standard, ease),
    box-shadow var(--dur-fast, 0.15s) var(--ease-standard, ease),
    border-color var(--dur-fast, 0.15s) var(--ease-standard, ease);
}
.product-card:hover {
  border-color: var(--accent-border);
  box-shadow: var(--elev-1, 0 2px 10px rgba(0, 0, 0, 0.25));
  transform: translateY(-2px);
}
.product-card:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.product-card.stok-yok {
  opacity: 0.55;
  cursor: not-allowed;
}
.product-card.stok-yok .product-gorsel img {
  filter: grayscale(0.7);
}
.product-card.sepette {
  border-color: var(--accent);
  box-shadow: 0 0 0 1px var(--accent-soft-strong);
}
.product-gorsel {
  position: relative;
  flex: 0 0 auto;
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: var(--accent-soft, rgba(20, 184, 166, 0.12));
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.product-gorsel img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.product-bas-harf {
  font-size: 22px;
  font-weight: 800;
  color: var(--accent);
  line-height: 1;
}
.sepette-rozet {
  position: absolute;
  top: 8px;
  right: 8px;
  min-width: 20px;
  height: 20px;
  padding: 0 5px;
  border-radius: 999px;
  background: var(--accent);
  color: var(--accent-contrast, #04211d);
  font-size: 11px;
  font-weight: 800;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.25);
}
.product-icerik {
  display: flex;
  flex-direction: column;
  gap: 5px;
  flex: 1 1 auto;
  min-width: 0;
  align-self: stretch;
}
.product-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.product-kod {
  font-size: 11.5px;
  font-weight: 600;
  color: var(--text-muted);
  font-family: monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.product-stok-satir {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 18px;
  font-size: 12px;
}
.product-stok-satir i {
  font-size: 8px;
}
.product-stok-satir.normal {
  color: var(--success);
  font-weight: 600;
}
.product-stok-satir.kritik {
  color: var(--warning);
  font-weight: 700;
}
.product-stok-satir.yok {
  color: var(--danger);
  font-weight: 700;
}
.product-fiyat-satir {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 6px;
  margin-top: auto;
}
.product-price {
  display: inline-block;
  font-size: 17px;
  font-weight: 800;
  color: var(--accent);
  white-space: nowrap;
}
.kdv-not {
  font-size: 11px;
  color: var(--text-muted);
}
.product-cari-fiyat {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 700;
  color: var(--success);
  background: var(--success-soft);
  padding: 2px 8px;
  border-radius: 999px;
  white-space: nowrap;
  align-self: flex-start;
}
.product-cari-fiyat i {
  font-size: 10px;
}
</style>
