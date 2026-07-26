<template>
  <div class="anomaliler-page">
    <PageHeader title="Akıllı Anomali & Mükerrer Tespiti" subtitle="Sistemdeki mükerrer faturalar, çift ödemeler ve finansal risklerin yapay zeka tespiti">
      <template #actions>
        <Button label="Yeniden Tara" icon="pi pi-refresh" class="p-button-primary" @click="anomalileriYukle" :loading="yukleniyor" />
      </template>
    </PageHeader>

    <div v-if="yukleniyor" class="p-4">
      <SkeletonLoader :count="3" />
    </div>

    <div v-else-if="anomaliler.length === 0" class="empty-box">
      <i class="pi pi-check-circle success-icon"></i>
      <h3>Harika! Hiçbir Şüpheli Durum Veya Mükerrer Kayıt Bulunamadı.</h3>
      <p>Sistemdeki tüm faturalar, hareketler ve bakiyeler tutarlı görünmektedir.</p>
    </div>

    <div v-else class="anomali-grid">
      <div v-for="item in anomaliler" :key="item.id" class="anomali-card" :class="item.seviye.toLowerCase()">
        <div class="card-header">
          <div class="header-left">
            <span class="badge" :class="item.seviye.toLowerCase()">{{ item.seviye }} ÖNCELİK</span>
            <span class="tur-label">{{ item.tur }}</span>
          </div>
          <span class="tarih">{{ formatTarih(item.tespitTarihi) }}</span>
        </div>
        <h4 class="card-title"><i class="pi pi-exclamation-triangle"></i> {{ item.baslik }}</h4>
        <p class="card-desc">{{ item.aciklama }}</p>
        <div class="oneri-box">
          <strong><i class="pi pi-lightbulb"></i> Öneri:</strong> {{ item.oneri }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import axios from 'axios'

const toast = useToast()
const yukleniyor = ref(false)
const anomaliler = ref([])

const anomalileriYukle = async () => {
  yukleniyor.value = true
  try {
    const res = await axios.get('/api/v1/anomaliler')
    anomaliler.value = res.data || []
    if (anomaliler.value.length > 0) {
      toast.add({ severity: 'warn', summary: 'Anomali Tespiti', detail: `${anomaliler.value.length} adet şüpheli durum tespit edildi.`, life: 5000 })
    } else {
      toast.add({ severity: 'success', summary: 'Temiz', detail: 'Hiçbir mükerrer kayıt veya anomaliye rastlanmadı.', life: 3000 })
    }
  } catch (e) {
    toast.add({ severity: 'error', summary: 'Hata', detail: 'Anomaliler taranırken hata oluştu.', life: 5000 })
  } finally {
    yukleniyor.value = false
  }
}

const formatTarih = (t) => {
  if (!t) return ''
  return new Date(t).toLocaleString('tr-TR')
}

onMounted(() => {
  anomalileriYukle()
})
</script>

<style scoped>
.anomaliler-page {
  padding: 1.5rem;
}
.empty-box {
  background: var(--surface-card, #1e293b);
  border: 1px solid var(--surface-border, rgba(255,255,255,0.1));
  border-radius: 12px;
  padding: 3rem;
  text-align: center;
  margin-top: 1.5rem;
}
.success-icon {
  font-size: 3.5rem;
  color: #22c55e;
  margin-bottom: 1rem;
}
.anomali-grid {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-top: 1.5rem;
}
.anomali-card {
  background: var(--surface-card, #1e293b);
  border: 1px solid var(--surface-border, rgba(255,255,255,0.1));
  border-left: 6px solid #3b82f6;
  border-radius: 10px;
  padding: 1.25rem;
}
.anomali-card.yuksek { border-left-color: #ef4444; }
.anomali-card.orta { border-left-color: #f59e0b; }
.anomali-card.dusuk { border-left-color: #3b82f6; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}
.badge {
  padding: 0.25rem 0.6rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
}
.badge.yuksek { background: rgba(239,68,68,0.2); color: #ef4444; }
.badge.orta { background: rgba(245,158,11,0.2); color: #f59e0b; }
.badge.dusuk { background: rgba(59,130,246,0.2); color: #3b82f6; }

.tur-label {
  margin-left: 0.5rem;
  font-size: 0.8rem;
  color: var(--text-secondary, #94a3b8);
  font-weight: 600;
}
.tarih {
  font-size: 0.8rem;
  color: var(--text-muted, #64748b);
}
.card-title {
  margin: 0 0 0.5rem 0;
  font-size: 1.1rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.card-desc {
  color: var(--text-secondary, #cbd5e1);
  margin-bottom: 0.75rem;
  font-size: 0.95rem;
}
.oneri-box {
  background: rgba(255,255,255,0.05);
  border-radius: 6px;
  padding: 0.6rem 0.8rem;
  font-size: 0.875rem;
  color: #38bdf8;
}
</style>
