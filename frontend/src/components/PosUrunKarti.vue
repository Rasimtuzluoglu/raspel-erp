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
      <i
        v-else
        class="pi pi-box"
      />
      <span
        v-if="sepetteAdet > 0"
        class="sepette-rozet"
      >{{ sepetteAdet }}</span>
    </div>
    <div class="product-icerik">
      <span class="product-kod">{{ urun.barkod || urun.stokKodu || '-' }}</span>
      <span class="product-name">{{ urun.ad }}</span>
      <span
        v-if="urun.stokGrubu"
        class="product-grup"
      >{{ urun.stokGrubu }}</span>
      <div
        class="product-stok-satir"
        :class="stokRenkSinifi(urun)"
      >
        <i class="pi pi-warehouse" />
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
  min-width: 100px;
}
.pos-buyuk .product-name {
  font-size: 17px;
}
.pos-buyuk .product-price {
  font-size: 18px;
}
.pos-buyuk .product-cari-fiyat {
  font-size: 15px;
}
.pos-buyuk .product-cari-fiyat i {
  font-size: 14px;
}
.pos-buyuk .product-card {
  padding: 14px 14px;
}
.product-card {
  position: relative;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 0;
  cursor: pointer;
  transition: transform var(--dur-fast, 0.15s) var(--ease-standard, ease),
    box-shadow var(--dur-fast, 0.15s) var(--ease-standard, ease),
    border-color var(--dur-fast, 0.15s) var(--ease-standard, ease);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.product-card:hover {
  border-color: var(--accent-border);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.22);
  transform: translateY(-3px);
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
  height: 96px;
  background: var(--bg-muted, rgba(148, 163, 184, 0.08));
  display: flex;
  align-items: center;
  justify-content: center;
}
.product-gorsel img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.product-gorsel > i {
  font-size: 30px;
  color: var(--text-muted);
  opacity: 0.5;
}
.sepette-rozet {
  position: absolute;
  top: 8px;
  right: 8px;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 999px;
  background: var(--accent);
  color: var(--accent-contrast, #04211d);
  font-size: 12px;
  font-weight: 800;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.25);
}
.product-icerik {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 12px 12px;
  flex: 1;
}
.product-kod {
  font-size: 10.5px;
  font-weight: 600;
  color: var(--text-muted);
  font-family: monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.product-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 34px;
}
.product-stok-satir {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 18px;
  font-size: 11px;
}
.product-stok-satir i {
  font-size: 11px;
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
.product-grup {
  font-size: 10.5px;
  color: var(--accent);
  background: var(--accent-soft);
  border: 1px solid var(--accent-border);
  border-radius: 999px;
  padding: 1px 8px;
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  font-size: 15px;
  font-weight: 800;
  color: var(--accent);
  white-space: nowrap;
}
.kdv-not {
  font-size: 10px;
  color: var(--text-muted);
}
.product-cari-fiyat {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11.5px;
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
