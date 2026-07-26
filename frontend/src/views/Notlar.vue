<template>
  <div class="notlar-page">
    <PageHeader title="Notlar" subtitle="Hızlı not al, düzenle, yönet. Artık başka uygulamaya gerek yok.">
      <template #actions>
        <Button label="Yeni Not" icon="pi pi-plus" class="p-button-primary" @click="dialogAc" />
      </template>
    </PageHeader>

    <div v-if="store.loading" class="p-4">
      <SkeletonLoader :count="3" />
    </div>

    <div v-else-if="store.notlar.length === 0" class="empty-box">
      <i class="pi pi-sticky-note empty-icon"></i>
      <h3>Henüz Not Yok</h3>
      <p>İlk notunu eklemek için "Yeni Not" butonuna tıkla.</p>
    </div>

    <div v-else class="not-grid">
      <div v-for="item in store.notlar" :key="item.id" class="not-card" :class="item.onemDerecesi?.toLowerCase()">
        <div class="not-card-header">
          <span class="onem-badge" :class="item.onemDerecesi?.toLowerCase()">{{ item.onemDerecesi || 'NORMAL' }}</span>
          <span class="not-tarih">{{ formatTarih(item.olusturmaTarihi) }}</span>
        </div>
        <h4 class="not-baslik">{{ item.baslik }}</h4>
        <p class="not-icerik">{{ item.icerik || 'Açıklama yok' }}</p>
        <div class="not-card-actions">
          <Button icon="pi pi-pencil" class="p-button-rounded p-button-text p-button-sm" @click="dialogDuzenle(item)" title="Düzenle" />
          <Button icon="pi pi-trash" class="p-button-rounded p-button-text p-button-danger p-button-sm" @click="sil(item.id)" title="Sil" />
        </div>
      </div>
    </div>

    <Dialog v-model:visible="dialogGoster" :header="dialogBaslik" :modal="true" style="width:500px">
      <div class="form-group">
        <label>Başlık *</label>
        <InputText v-model="form.baslik" placeholder="Not başlığı" class="w-full" :class="{ 'p-invalid': !form.baslik && gonderildi }" />
      </div>
      <div class="form-group">
        <label>İçerik</label>
        <Textarea v-model="form.icerik" placeholder="Not içeriği..." rows="5" class="w-full" />
      </div>
      <div class="form-group">
        <label>Önem Derecesi</label>
        <Dropdown v-model="form.onemDerecesi" :options="onemSecenek" optionLabel="label" optionValue="value" placeholder="Seçiniz" class="w-full" />
      </div>
      <template #footer>
        <Button label="İptal" icon="pi pi-times" @click="dialogGoster = false" class="p-button-text" />
        <Button label="Kaydet" icon="pi pi-check" @click="kaydet" :loading="kaydediliyor" :disabled="!form.baslik?.trim()" />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useNotStore } from '../stores/notStore.js'

const toast = useToast()
const store = useNotStore()

const onemSecenek = [
  { label: 'Düşük', value: 'DUSUK' },
  { label: 'Normal', value: 'NORMAL' },
  { label: 'Yüksek', value: 'YUKSEK' },
  { label: 'Kritik', value: 'KRITIK' }
]

const dialogGoster = ref(false)
const duzenlemeModu = ref(false)
const kaydediliyor = ref(false)
const gonderildi = ref(false)
const form = ref({ baslik: '', icerik: '', onemDerecesi: 'NORMAL' })

const dialogBaslik = computed(() => duzenlemeModu.value ? 'Notu Düzenle' : 'Yeni Not')

onMounted(() => store.getAllNotlar())

const dialogAc = () => {
  duzenlemeModu.value = false
  form.value = { baslik: '', icerik: '', onemDerecesi: 'NORMAL' }
  gonderildi.value = false
  dialogGoster.value = true
}

const dialogDuzenle = (item) => {
  duzenlemeModu.value = true
  form.value = { ...item }
  gonderildi.value = false
  dialogGoster.value = true
}

const kaydet = async () => {
  gonderildi.value = true
  if (!form.value.baslik?.trim()) return
  kaydediliyor.value = true
  try {
    if (duzenlemeModu.value) {
      await store.updateNot(form.value.id, form.value)
      toast.add({ severity: 'success', summary: 'Güncellendi', detail: 'Not başarıyla güncellendi.', life: 3000 })
    } else {
      await store.addNot(form.value)
      toast.add({ severity: 'success', summary: 'Eklendi', detail: 'Not başarıyla eklendi.', life: 3000 })
    }
    dialogGoster.value = false
  } catch {
    toast.add({ severity: 'error', summary: 'Hata', detail: 'İşlem başarısız oldu.', life: 5000 })
  } finally {
    kaydediliyor.value = false
  }
}

const sil = async (id) => {
  try {
    await store.deleteNot(id)
    toast.add({ severity: 'success', summary: 'Silindi', detail: 'Not silindi.', life: 3000 })
  } catch {
    toast.add({ severity: 'error', summary: 'Hata', detail: 'Silme başarısız.', life: 5000 })
  }
}

const formatTarih = (t) => {
  if (!t) return ''
  return new Date(t).toLocaleString('tr-TR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.notlar-page { padding: 1.5rem; }
.empty-box {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 3rem;
  text-align: center;
  margin-top: 1.5rem;
}
.empty-icon { font-size: 3.5rem; color: #94a3b8; margin-bottom: 1rem; }
.not-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1rem;
  margin-top: 1.5rem;
}
.not-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 1.25rem;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
}
.not-card:hover { border-color: rgba(59,130,246,0.3); }
.not-card.yuksek { border-left: 4px solid #f59e0b; }
.not-card.kritik { border-left: 4px solid #ef4444; }
.not-card.dusuk { border-left: 4px solid #3b82f6; }
.not-card.normal { border-left: 4px solid #64748b; }
[data-theme="light"] .not-card { background: #ffffff; }
.not-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}
.onem-badge {
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
}
.onem-badge.dusuk { background: rgba(59,130,246,0.2); color: #60a5fa; }
.onem-badge.normal { background: rgba(100,116,139,0.2); color: #94a3b8; }
.onem-badge.yuksek { background: rgba(245,158,11,0.2); color: #fbbf24; }
.onem-badge.kritik { background: rgba(239,68,68,0.2); color: #f87171; }
.not-tarih { font-size: 0.75rem; color: var(--text-muted); }
.not-baslik {
  margin: 0 0 0.5rem;
  font-size: 1rem;
  color: var(--text-primary);
  font-weight: 600;
}
.not-icerik {
  font-size: 0.875rem;
  color: var(--text-secondary);
  flex: 1;
  margin-bottom: 0.75rem;
  white-space: pre-wrap;
  word-break: break-word;
}
.not-card-actions {
  display: flex;
  gap: 0.25rem;
  justify-content: flex-end;
  border-top: 1px solid var(--border);
  padding-top: 0.5rem;
}
.form-group { margin-bottom: 1rem; }
.form-group label { display: block; margin-bottom: 0.35rem; font-weight: 600; font-size: 0.85rem; color: var(--text-secondary); }
.w-full { width: 100% !important; }
[data-theme="light"] .onem-badge.normal { background: rgba(100,116,139,0.1); color: #475569; }
</style>
