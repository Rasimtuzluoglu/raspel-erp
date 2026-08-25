<template>
  <Dialog
    :visible="visible"
    :header="stok?.ad || 'Ürün Detayı'"
    :modal="true"
    style="width: 700px"
    @update:visible="$emit('update:visible', $event)"
  >
    <div
      v-if="stok"
      class="detail-grid"
    >
      <div class="detail-item">
        <span class="detail-label">Stok Kodu</span>
        <span class="detail-value">{{ stok.stokKodu || '-' }}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">Barkod</span>
        <span class="detail-value">{{ stok.barkod || '-' }}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">Birim</span>
        <span class="detail-value">{{ stok.birim || '-' }}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">Miktar</span>
        <span
          class="detail-value"
          :class="stok.minMiktar && stok.miktar <= stok.minMiktar ? 'kritik' : 'normal'"
        >
          {{ stok.miktar }} {{ stok.birim || '' }}
        </span>
      </div>
      <div class="detail-item">
        <span class="detail-label">Alış Fiyatı</span>
        <span class="detail-value">{{ formatCurrency(stok.fiyat) }}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">Satış Fiyatı</span>
        <span class="detail-value">{{ formatCurrency(stok.satisFiyati) }}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">Kategori</span>
        <span class="detail-value">{{ stok.kategori || '-' }}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">Marka</span>
        <span class="detail-value">{{ stok.marka || '-' }}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">Min. Stok</span>
        <span class="detail-value">{{ stok.minMiktar || '-' }}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">Raf No</span>
        <span class="detail-value">{{ stok.rafNo || '-' }}</span>
      </div>
    </div>
    <div class="form-section-title">
      Stok Hareketleri
    </div>
    <div
      v-if="hareketlerYukleniyor"
      class="loading"
    >
      <p><i class="pi pi-spin pi-spinner" /> Yükleniyor...</p>
    </div>
    <EmptyState
      v-else-if="hareketler.length === 0"
      message="Hareket bulunamadı"
      sub-message="Bu ürüne ait stok hareketi bulunmamaktadır."
      icon="pi pi-list"
    />
    <DataTable
      v-else
      state-storage="session"
      state-key="stok-hareketler-table-state"
      :value="hareketler"
      size="small"
      striped-rows
      :paginator="hareketler.length > 10"
      :rows="10"
    >
      <Column
        header="Tarih"
        style="width: 110px"
      >
        <template #body="s">
          {{ formatDate(s.data.hareketTarihi || s.data.tarih) }}
        </template>
      </Column>
      <Column
        header="Tür"
        style="width: 90px"
      >
        <template #body="s">
          <span :class="['badge', s.data.tur === 'GIRIS' ? 'giris' : 'cikis']">
            {{ s.data.tur === 'GIRIS' ? 'Giriş' : 'Çıkış' }}
          </span>
        </template>
      </Column>
      <Column
        header="Miktar"
        style="width: 90px"
      >
        <template #body="s">
          <span :class="s.data.tur === 'GIRIS' ? 'positive' : 'negative'">{{ s.data.miktar }}</span>
        </template>
      </Column>
      <Column header="Açıklama">
        <template #body="s">
          {{ s.data.aciklama || '-' }}
        </template>
      </Column>
    </DataTable>
  </Dialog>
</template>

<script setup>
import { formatCurrency } from '../utils/format.js'

defineProps({
  visible: { type: Boolean, default: false },
  stok: { type: Object, default: null },
  hareketler: { type: Array, default: () => [] },
  hareketlerYukleniyor: { type: Boolean, default: false }
})

defineEmits(['update:visible'])

const formatDate = (d) =>
  d ? new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(d)) : '-'
</script>

<style scoped>
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 8px;
}
.detail-item {
  padding: 6px 0;
}
.detail-label {
  display: block;
  font-size: 11px;
  color: var(--text-muted);
  text-transform: uppercase;
  margin-bottom: 3px;
}
.detail-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}
.detail-value.normal {
  color: #4ade80;
}
.detail-value.kritik {
  color: #f87171;
}
.form-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}
.loading {
  text-align: center;
  padding: 40px;
  color: var(--text-secondary);
}
.badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 700;
}
.badge.giris {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}
.badge.cikis {
  background: rgba(239, 68, 68, 0.15);
  color: #f87171;
}
.positive {
  color: #4ade80;
  font-weight: 700;
}
.negative {
  color: #f87171;
  font-weight: 700;
}
</style>
