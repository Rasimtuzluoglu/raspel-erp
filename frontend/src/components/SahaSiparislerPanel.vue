<template>
  <div class="fade-in-section">
    <div class="section-title-row">
      <h3><i class="pi pi-truck text-primary mr-2" />Aktif Saha Siparişleri</h3>
      <span class="count-pill">{{ siparisler ? siparisler.length : 0 }} Sipariş</span>
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
            :value="s.durum || 'BEKLIYOR'"
            :severity="siparisDurumSeverity(s.durum)"
            rounded
          />
        </div>

        <h4 class="customer-title">
          {{ s.cariHesapAdi || s.musteriAdi || 'Müşteri' }}
        </h4>

        <div
          v-if="s.teslimatAdresi"
          class="address-box"
        >
          <i class="pi pi-map-marker text-red-500" />
          <span>{{ s.teslimatAdresi }}</span>
        </div>

        <div class="amount-box">
          <div class="date-col">
            <small>Tarih</small>
            <strong>{{ formatTarih(s.tarih) }}</strong>
          </div>
          <div class="price-col text-right">
            <small>Tutar</small>
            <span class="price-val">{{ formatCurrency(s.toplamTutar || s.genelToplam || 0) }}</span>
          </div>
        </div>

        <div class="card-bottom-actions">
          <a
            v-if="s.telefon"
            :href="'tel:' + s.telefon"
            class="call-btn"
          >
            <i class="pi pi-phone" /> Ara
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
            <i class="pi pi-map" /> Yol Tarifi
          </a>
        </div>

        <div
          v-if="s.durum !== 'TESLIM_EDILDI'"
          class="delivery-actions"
        >
          <Button
            label="Durum"
            icon="pi pi-sync"
            class="p-button-outlined p-button-sm flex-1"
            @click="$emit('durum-sec', s)"
          />
          <Button
            label="İmza & Teslim Et"
            icon="pi pi-check"
            class="p-button-success p-button-sm flex-1 font-bold"
            @click="$emit('imza-ac', s)"
          />
        </div>
      </div>
    </div>

    <div
      v-else
      class="empty-box"
    >
      <i class="pi pi-inbox empty-icon" />
      <p>Henüz atanmış aktif bir saha siparişi bulunmuyor.</p>
    </div>
  </div>
</template>

<script setup>
import { formatCurrency } from '../utils/format.js'

defineProps({
  siparisler: { type: Array, default: () => [] }
})

defineEmits(['durum-sec', 'imza-ac', 'whatsapp'])

const formatTarih = (d) => {
  if (!d) return '-'
  return new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(d))
}

const siparisDurumSeverity = (durum) => {
  const map = { BEKLIYOR: 'warning', HAZIRLANIYOR: 'info', YOLDA: 'help', TESLIM_EDILDI: 'success', IPTAL: 'danger' }
  return map[durum] || 'info'
}
</script>

<style scoped>
.fade-in-section {
  animation: fadeIn 0.25s ease-in-out;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

.section-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}
.section-title-row h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--text-primary);
  display: flex;
  align-items: center;
}
.count-pill {
  font-size: 0.75rem;
  color: var(--text-secondary);
  background: var(--bg-muted, rgba(0, 0, 0, 0.05));
  padding: 3px 8px;
  border-radius: 6px;
  font-weight: 600;
}
.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
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
  border-color: var(--primary-color, #3b82f6);
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.65rem;
}
.order-code {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--primary-color, #3b82f6);
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
.price-val {
  font-size: 1.05rem;
  font-weight: 800;
  color: #10b981;
}
.card-bottom-actions {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.65rem;
}
.call-btn, .map-btn, .whatsapp-btn {
  flex: 1;
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
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
  border: 1px solid rgba(59, 130, 246, 0.3);
}
.map-btn:hover {
  background: rgba(59, 130, 246, 0.2);
}
.delivery-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: auto;
  padding-top: 0.5rem;
  border-top: 1px dashed var(--border);
}
.empty-box {
  text-align: center;
  padding: 3.5rem 1rem;
  background: var(--bg-card);
  border: 1px dashed var(--border);
  border-radius: 1rem;
  color: var(--text-secondary);
}
.empty-icon {
  font-size: 3rem;
  margin-bottom: 0.75rem;
  opacity: 0.4;
}
</style>

