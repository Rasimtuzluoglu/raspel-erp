<template>
  <div class="mini-stats-grid">
    <div class="mini-stat">
      <div class="mini-stat-icon siparis">
        <i class="pi pi-shopping-cart" />
      </div>
      <div class="mini-stat-icerik">
        <span class="mini-stat-etiket">{{ t('dashboard.bugunkuSiparis') }}</span>
        <strong class="mini-stat-deger gizli-veri">{{ dashboardStore?.bugunkuSiparis || 0 }}</strong>
      </div>
    </div>
    <div class="mini-stat">
      <div class="mini-stat-icon teslimat">
        <i class="pi pi-truck" />
      </div>
      <div class="mini-stat-icerik">
        <span class="mini-stat-etiket">{{ t('dashboard.bekleyenTeslimat') }}</span>
        <strong class="mini-stat-deger gizli-veri">{{ dashboardStore?.bekleyenTeslimat || 0 }}</strong>
      </div>
    </div>
    <div class="mini-stat">
      <div class="mini-stat-icon iade">
        <i class="pi pi-replay" />
      </div>
      <div class="mini-stat-icerik">
        <span class="mini-stat-etiket">{{ t('dashboard.iadeOrani') }}</span>
        <strong class="mini-stat-deger gizli-veri">{{ dashboardStore?.iadeOrani || 0 }}%</strong>
      </div>
    </div>
    <div class="mini-stat">
      <div class="mini-stat-icon devir">
        <i class="pi pi-sync" />
      </div>
      <div class="mini-stat-icerik">
        <span class="mini-stat-etiket">{{ t('dashboard.stokDevirHizi') }}</span>
        <strong class="mini-stat-deger gizli-veri">{{ dashboardStore?.stokDevirHizi || 0 }}</strong>
      </div>
    </div>
    <div class="mini-stat">
      <div class="mini-stat-icon calisan">
        <i class="pi pi-id-card" />
      </div>
      <div class="mini-stat-icerik">
        <span class="mini-stat-etiket">{{ t('dashboard.aktifCalisan') }}</span>
        <strong class="mini-stat-deger gizli-veri">{{ dashboardStore?.aktifCalisan || 0 }}</strong>
      </div>
    </div>
    <div class="mini-stat">
      <div class="mini-stat-icon izinli">
        <i class="pi pi-calendar" />
      </div>
      <div class="mini-stat-icerik">
        <span class="mini-stat-etiket">{{ t('dashboard.bugunIzinli') }}</span>
        <strong class="mini-stat-deger gizli-veri">{{ dashboardStore?.bugunIzinli || 0 }}</strong>
      </div>
    </div>
    <div class="mini-stat">
      <div class="mini-stat-icon bekleyen-izin">
        <i class="pi pi-hourglass" />
      </div>
      <div class="mini-stat-icerik">
        <span class="mini-stat-etiket">{{ t('dashboard.bekleyenIzin') }}</span>
        <strong class="mini-stat-deger gizli-veri">{{ dashboardStore?.bekleyenIzinSayisi || 0 }}</strong>
      </div>
    </div>
    <div class="mini-stat">
      <div class="mini-stat-icon odeme">
        <i class="pi pi-money-bill" />
      </div>
      <div class="mini-stat-icerik">
        <span class="mini-stat-etiket">{{ t('dashboard.bugunkuOdemeKart') }}</span>
        <strong class="mini-stat-deger gizli-veri">{{ formatCurrency(dashboardStore?.bugunkuOdeme || 0) }}</strong>
      </div>
    </div>
    <div class="mini-stat">
      <div class="mini-stat-icon gecen-fatura">
        <i class="pi pi-exclamation-circle" />
      </div>
      <div class="mini-stat-icerik">
        <span class="mini-stat-etiket">{{ t('dashboard.vadesiGecenFatura') }}</span>
        <strong class="mini-stat-deger gizli-veri">{{ dashboardStore?.vadesiGecenFaturalar?.length || 0 }}</strong>
      </div>
    </div>
    <div class="mini-stat">
      <div class="mini-stat-icon yaklasan-fatura">
        <i class="pi pi-calendar-clock" />
      </div>
      <div class="mini-stat-icerik">
        <span class="mini-stat-etiket">{{ t('dashboard.vadesiYaklasanFatura') }}</span>
        <strong class="mini-stat-deger gizli-veri">{{ dashboardStore?.vadesiYaklasanFaturalar?.length || 0 }}</strong>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { useDashboardStore } from '../stores/dashboardStore.js'
import { formatCurrency } from '../utils/format.js'

const { t } = useI18n()
const dashboardStore = useDashboardStore()
</script>

<style scoped>
.mini-stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(170px, 100%), 1fr));
  gap: 12px;
  margin-bottom: 24px;
}
.mini-stat {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px 14px;
  transition: all 0.2s;
  box-shadow: var(--shadow);
}
.mini-stat:hover {
  border-color: var(--accent-border);
  transform: translateY(-2px);
}
.mini-stat-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: white;
  flex-shrink: 0;
}
.mini-stat-icon.siparis {
  background: linear-gradient(135deg, var(--accent), var(--accent-hover));
}
.mini-stat-icon.teslimat {
  background: linear-gradient(135deg, #8b5cf6, #7c3aed);
}
.mini-stat-icon.iade {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}
.mini-stat-icon.devir {
  background: linear-gradient(135deg, #06b6d4, #0891b2);
}
.mini-stat-icon.calisan {
  background: linear-gradient(135deg, #22c55e, #16a34a);
}
.mini-stat-icon.izinli {
  background: linear-gradient(135deg, #eab308, #ca8a04);
}
.mini-stat-icon.bekleyen-izin {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}
.mini-stat-icon.odeme {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}
.mini-stat-icon.gecen-fatura {
  background: linear-gradient(135deg, #dc2626, #991b1b);
}
.mini-stat-icon.yaklasan-fatura {
  background: linear-gradient(135deg, #f97316, #ea580c);
}
.mini-stat-icerik {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.mini-stat-etiket {
  font-size: 11px;
  color: var(--text-muted);
  margin-bottom: 2px;
}
.mini-stat-deger {
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
