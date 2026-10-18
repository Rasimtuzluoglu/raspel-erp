<template>
  <div class="saha-fade">
    <div class="saha-baslik-satir">
      <h3><i class="pi pi-truck saha-baslik-ikon" />{{ $t('sahaPortali.aktifSahaSiparisleri') }}</h3>
      <span class="saha-count-pill">{{ siparisler ? siparisler.length : 0 }} {{ $t('sahaPortali.siparisKisa') }}</span>
    </div>

    <div
      v-if="siparisler && siparisler.length > 0"
      class="cards-grid"
    >
      <div
        v-for="s in siparisler"
        :key="s.id"
        class="saha-card"
      >
        <div class="card-top">
          <span class="order-code">#{{ s.siparisNo || s.id }}</span>
          <Tag
            :value="durumEtiket(s.durum)"
            :severity="siparisDurumSeverity(s.durum)"
            rounded
          />
        </div>

        <h4 class="customer-title">
          {{ s.cariHesapAdi || s.musteriAdi || $t('sahaPortali.musteri') }}
        </h4>

        <div
          v-if="s.teslimatAdresi"
          class="address-box"
        >
          <i class="pi pi-map-marker saha-ikon-kirmizi" />
          <span>{{ s.teslimatAdresi }}</span>
        </div>

        <div class="amount-box">
          <div class="date-col">
            <small>{{ $t('sahaPortali.tarih') }}</small>
            <strong>{{ formatTarih(s.tarih) }}</strong>
          </div>
          <div class="price-col">
            <small>{{ $t('sahaPortali.tutar') }}</small>
            <span class="price-val">{{ formatCurrency(s.toplamTutar || s.genelToplam || 0) }}</span>
          </div>
        </div>

        <div class="card-bottom-actions">
          <a
            v-if="s.telefon"
            :href="'tel:' + s.telefon"
            class="call-btn"
          >
            <i class="pi pi-phone" /> {{ $t('sahaPortali.ara') }}
          </a>
          <button
            v-if="s.telefon || s.cariHesapAdi"
            type="button"
            class="whatsapp-btn"
            @click="$emit('whatsapp', s)"
          >
            <i class="pi pi-whatsapp" /> WhatsApp
          </button>
          <a
            v-if="s.teslimatAdresi"
            :href="'https://maps.google.com/?q=' + encodeURIComponent(s.teslimatAdresi)"
            target="_blank"
            class="map-btn"
          >
            <i class="pi pi-map" /> {{ $t('sahaPortali.yolTarifi') }}
          </a>
        </div>

        <div
          v-if="s.durum !== 'TESLIM_EDILDI'"
          class="delivery-actions"
        >
          <Button
            :label="$t('common.status')"
            icon="pi pi-sync"
            class="p-button-outlined p-button-sm flex-1"
            @click="$emit('durum-sec', s)"
          />
          <Button
            :label="$t('sahaPortali.imzaTeslimEt')"
            icon="pi pi-check"
            class="p-button-success p-button-sm flex-1 font-bold"
            @click="$emit('imza-ac', s)"
          />
        </div>
      </div>
    </div>

    <div
      v-else
      class="saha-bos"
    >
      <i class="pi pi-inbox saha-bos-ikon" />
      <p>{{ $t('sahaPortali.bosSaha') }}</p>
    </div>
  </div>
</template>

<script setup>
import { formatCurrency, formatTarih } from '../utils/format.js'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

defineProps({
  siparisler: { type: Array, default: () => [] }
})

defineEmits(['durum-sec', 'imza-ac', 'whatsapp'])

const siparisDurumSeverity = (durum) => {
  const map = { BEKLIYOR: 'warning', HAZIRLANIYOR: 'info', YOLDA: 'help', TESLIM_EDILDI: 'success', IPTAL: 'danger' }
  return map[durum] || 'info'
}

// Durum kodlarini cevir (siparisTakip.durum.<KOD>; yoksa kodun kendisi).
const durumEtiket = (kod) => {
  if (!kod) return t('siparisTakip.durum.BEKLIYOR')
  const anahtar = `siparisTakip.durum.${kod}`
  const ceviri = t(anahtar)
  return ceviri === anahtar ? kod : ceviri
}
</script>

<style scoped>
.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(300px, 100%), 1fr));
  gap: 1rem;
}
.saha-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 1rem;
  padding: 1.15rem;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  transition: transform 0.2s, box-shadow 0.2s;
}
.saha-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
  border-color: var(--primary-color, var(--accent));
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.65rem;
}
.saha-card :deep(.p-tag) {
  white-space: nowrap;
}
.order-code {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--primary-color, var(--accent));
}
.customer-title {
  font-size: 1.05rem;
  font-weight: 700;
  margin: 0 0 0.5rem 0;
  color: var(--text-primary);
}
.address-box {
  display: flex;
  align-items: flex-start;
  gap: 0.4rem;
  font-size: 0.8rem;
  color: var(--text-secondary);
  margin-bottom: 0.85rem;
}
.address-box i {
  margin-top: 2px;
}
.amount-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--bg-muted, rgba(0,0,0,0.03));
  border-radius: 0.625rem;
  padding: 0.65rem 0.85rem;
  margin-bottom: 0.85rem;
}
.amount-box small {
  display: block;
  font-size: 0.7rem;
  color: var(--text-secondary);
}
.price-col {
  text-align: right;
}
.price-val {
  font-size: 1.05rem;
  font-weight: 800;
  color: #10b981;
}
.card-bottom-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 0.65rem;
}
.call-btn, .map-btn, .whatsapp-btn {
  flex: 1;
  min-height: 40px;
  text-align: center;
  padding: 0.5rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 600;
  border-radius: 0.5rem;
  text-decoration: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  cursor: pointer;
  transition: all 0.2s;
}
.call-btn {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
  border: 1px solid rgba(16, 185, 129, 0.3);
}
.call-btn:hover {
  background: rgba(16, 185, 129, 0.2);
}
.whatsapp-btn {
  background: rgba(37, 211, 102, 0.1);
  color: #25d366;
  border: 1px solid rgba(37, 211, 102, 0.3);
}
.whatsapp-btn:hover {
  background: rgba(37, 211, 102, 0.2);
}
.map-btn {
  background: var(--accent-soft);
  color: var(--accent);
  border: 1px solid var(--accent-border);
}
.map-btn:hover {
  background: var(--accent-soft-strong);
}
.delivery-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: auto;
  padding-top: 0.5rem;
  border-top: 1px dashed var(--border);
}
.delivery-actions :deep(.p-button) {
  min-height: 40px;
}
</style>

