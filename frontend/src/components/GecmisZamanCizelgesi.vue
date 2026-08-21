<template>
  <div class="gecmis-timeline-container">
    <div
      v-if="yukleniyor"
      class="timeline-loading"
    >
      <i class="pi pi-spin pi-spinner" /> Geçmiş yükleniyor...
    </div>
    <div
      v-else-if="!loglar.length"
      class="timeline-bos"
    >
      <i class="pi pi-history" />
      <p>Henüz işlem geçmişi kaydı bulunmuyor.</p>
    </div>
    <Timeline
      v-else
      :value="loglar"
      align="alternate"
      class="custom-timeline"
    >
      <template #marker="slotProps">
        <span
          class="timeline-marker"
          :class="getMarkerClass(slotProps.item.islemTipi || slotProps.item.islem)"
        >
          <i :class="getMarkerIcon(slotProps.item.islemTipi || slotProps.item.islem)" />
        </span>
      </template>
      <template #content="slotProps">
        <Card class="timeline-card">
          <template #title>
            <span class="timeline-islem">{{ slotProps.item.baslik || slotProps.item.islem }}</span>
          </template>
          <template #subtitle>
            <div class="timeline-meta">
              <span><i class="pi pi-user" />
                {{ slotProps.item?.kullaniciAdi || slotProps.item?.kullanici || 'Sistem' }}</span>
              <span><i class="pi pi-clock" />
                {{ formatTarih(slotProps.item?.tarih || slotProps.item?.olusturmaTarihi) }}</span>
            </div>
          </template>
          <template #content>
            <p
              v-if="slotProps.item.detay || slotProps.item.aciklama"
              class="timeline-detay"
            >
              {{ slotProps.item.detay || slotProps.item.aciklama }}
            </p>
          </template>
        </Card>
      </template>
    </Timeline>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { auditLogAPI } from '../api/index.js'

const props = defineProps({
  entityTipi: {
    type: String,
    default: ''
  },
  entityId: {
    type: [Number, String],
    default: null
  },
  mockData: {
    type: Array,
    default: () => []
  }
})

const loglar = ref(props.mockData && props.mockData.length > 0 ? [...props.mockData] : [])
const yukleniyor = ref(false)

const yukle = async () => {
  if (props.mockData && props.mockData.length > 0) {
    loglar.value = props.mockData
    return
  }
  if (!props.entityTipi && !props.entityId) {
    loglar.value = []
    return
  }
  yukleniyor.value = true
  try {
    if (auditLogAPI && typeof auditLogAPI.getAll === 'function') {
      const res = await auditLogAPI.getAll({
        entityAdi: props.entityTipi,
        entityId: props.entityId
      })
      loglar.value = res.data?.content || res.data || []
    }
  } catch {
    loglar.value = []
  } finally {
    yukleniyor.value = false
  }
}

watch(() => [props.entityTipi, props.entityId], yukle)
onMounted(yukle)

const formatTarih = (d) => {
  if (!d) return '-'
  return new Date(d).toLocaleString('tr-TR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getMarkerIcon = (islem) => {
  const islemStr = (islem || '').toUpperCase()
  if (islemStr.includes('OLUSTUR') || islemStr.includes('EKLE') || islemStr.includes('CREATE')) return 'pi pi-plus'
  if (islemStr.includes('GUNCELLE') || islemStr.includes('DUZENLE') || islemStr.includes('UPDATE'))
    return 'pi pi-pencil'
  if (islemStr.includes('SIL') || islemStr.includes('DELETE')) return 'pi pi-trash'
  if (islemStr.includes('ONAY') || islemStr.includes('APPROVE')) return 'pi pi-check'
  return 'pi pi-info-circle'
}

const getMarkerClass = (islem) => {
  const islemStr = (islem || '').toUpperCase()
  if (islemStr.includes('OLUSTUR') || islemStr.includes('EKLE') || islemStr.includes('CREATE')) return 'marker-success'
  if (islemStr.includes('GUNCELLE') || islemStr.includes('DUZENLE') || islemStr.includes('UPDATE')) return 'marker-info'
  if (islemStr.includes('SIL') || islemStr.includes('DELETE')) return 'marker-danger'
  if (islemStr.includes('ONAY') || islemStr.includes('APPROVE')) return 'marker-warning'
  return 'marker-secondary'
}
</script>

<style scoped>
.gecmis-timeline-container {
  padding: 1rem 0;
}
.timeline-loading,
.timeline-bos {
  text-align: center;
  padding: 2rem;
  color: var(--text-muted);
}
.timeline-bos i {
  font-size: 2rem;
  margin-bottom: 0.5rem;
  display: block;
}
.timeline-marker {
  display: flex;
  width: 2rem;
  height: 2rem;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  border-radius: 50%;
  z-index: 1;
}
.marker-success {
  background-color: #10b981;
}
.marker-info {
  background-color: #3b82f6;
}
.marker-danger {
  background-color: #ef4444;
}
.marker-warning {
  background-color: #f59e0b;
}
.marker-secondary {
  background-color: #6b7280;
}
.timeline-card {
  margin-bottom: 1rem;
  background: var(--bg-card);
  border: 1px solid var(--border);
}
.timeline-islem {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text-primary);
}
.timeline-meta {
  display: flex;
  gap: 1rem;
  font-size: 0.8rem;
  color: var(--text-muted);
  margin-top: 0.25rem;
}
.timeline-meta span {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}
.timeline-detay {
  margin: 0;
  font-size: 0.85rem;
  color: var(--text-secondary);
}
</style>
