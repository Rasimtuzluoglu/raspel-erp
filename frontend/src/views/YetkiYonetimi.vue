<template>
  <div class="yetki-page">
    <PageHeader title="Rol & Yetki Matrisi (RBAC)" subtitle="Sistemdeki rollerin modül bazlı okuma, yazma, silme ve dışa aktarım izinleri">
      <template #actions>
        <Button label="Değişiklikleri Kaydet" icon="pi pi-check" class="p-button-success" @click="kaydet" :loading="kaydediliyor" />
      </template>
    </PageHeader>

    <div v-if="yukleniyor" class="p-4">
      <SkeletonLoader :count="4" />
    </div>

    <div v-else class="matrix-container">
      <div class="roles-tabs">
        <button
          v-for="r in roller"
          :key="r.id"
          class="role-tab"
          :class="{ active: seciliRol?.id === r.id }"
          @click="seciliRol = r"
        >
          <i class="pi pi-shield"></i> {{ r.ad }}
        </button>
      </div>

      <div v-if="seciliRol" class="role-details-card">
        <div class="role-info">
          <h3>{{ seciliRol.ad }} Yetkileri</h3>
          <p>{{ seciliRol.aciklama }}</p>
        </div>

        <table class="yetki-table">
          <thead>
            <tr>
              <th>Modül</th>
              <th>Yetki Kodu</th>
              <th>Açıklama</th>
              <th style="width: 100px; text-align: center;">Erişim İzni</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="y in yetkiler" :key="y.id">
              <td><span class="modul-tag">{{ y.modul }}</span></td>
              <td><code>{{ y.kod }}</code></td>
              <td>{{ y.aciklama }}</td>
              <td style="text-align: center;">
                <input
                  type="checkbox"
                  class="yetki-checkbox"
                  :checked="yetkiVarmis(y.id)"
                  @change="yetkiToggle(y.id)"
                />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { apiClient } from '../api/index.js'
const toast = useToast()
const yukleniyor = ref(false)
const kaydediliyor = ref(false)

const roller = ref([])
const yetkiler = ref([])
const seciliRol = ref(null)

const verileriYukle = async () => {
  yukleniyor.value = true
  try {
    const [rRes, yRes] = await Promise.all([
      apiClient.get('/yetkiler/roller'),
      apiClient.get('/yetkiler')
    ])
    roller.value = rRes.data || []
    yetkiler.value = yRes.data || []
    if (roller.value.length > 0) {
      seciliRol.value = roller.value[0]
    }
  } catch (e) {
    toast.add({ severity: 'error', summary: 'Hata', detail: 'Yetki matrisi yüklenemedi.', life: 5000 })
  } finally {
    yukleniyor.value = false
  }
}

const yetkiVarmis = (yetkiId) => {
  if (!seciliRol.value || !seciliRol.value.yetkiler) return false
  return seciliRol.value.yetkiler.some(y => y.id === yetkiId)
}

const yetkiToggle = (yetkiId) => {
  if (!seciliRol.value) return
  if (!seciliRol.value.yetkiler) seciliRol.value.yetkiler = []

  const idx = seciliRol.value.yetkiler.findIndex(y => y.id === yetkiId)
  if (idx > -1) {
    seciliRol.value.yetkiler.splice(idx, 1)
  } else {
    const yObj = yetkiler.value.find(y => y.id === yetkiId)
    if (yObj) seciliRol.value.yetkiler.push(yObj)
  }
}

const kaydet = async () => {
  if (!seciliRol.value) return
  kaydediliyor.value = true
  try {
    const yetkiIds = seciliRol.value.yetkiler.map(y => y.id)
    await apiClient.put(`/yetkiler/roller/${seciliRol.value.id}`, yetkiIds)
    toast.add({ severity: 'success', summary: 'Başarılı', detail: `${seciliRol.value.ad} yetkileri kaydedildi.`, life: 4000 })
  } catch (e) {
    toast.add({ severity: 'error', summary: 'Hata', detail: 'Rol yetkileri güncellenemedi.', life: 5000 })
  } finally {
    kaydediliyor.value = false
  }
}

onMounted(() => {
  verileriYukle()
})
</script>

<style scoped>
.yetki-page { padding: 1.5rem; }
.matrix-container { display: flex; gap: 1.5rem; margin-top: 1.5rem; }
.roles-tabs { display: flex; flex-direction: column; gap: 0.5rem; width: 220px; flex-shrink: 0; }
.role-tab {
  display: flex; align-items: center; gap: 0.75rem;
  padding: 0.8rem 1rem; border-radius: 8px;
  background: var(--surface-card, #1e293b);
  border: 1px solid var(--surface-border, rgba(255,255,255,0.1));
  color: var(--text-primary, #f1f5f9);
  font-weight: 600; cursor: pointer; text-align: left;
  transition: all 0.2s;
}
.role-tab:hover { background: rgba(59,130,246,0.1); }
.role-tab.active { background: #3b82f6; color: #ffffff; border-color: #3b82f6; }

.role-details-card {
  flex: 1;
  background: var(--surface-card, #1e293b);
  border: 1px solid var(--surface-border, rgba(255,255,255,0.1));
  border-radius: 12px; padding: 1.5rem;
}
.role-info h3 { margin: 0 0 0.25rem 0; font-size: 1.3rem; }
.role-info p { color: var(--text-secondary, #94a3b8); margin-bottom: 1.5rem; }

.yetki-table { width: 100%; border-collapse: collapse; }
.yetki-table th, .yetki-table td { padding: 0.75rem 1rem; border-bottom: 1px solid rgba(255,255,255,0.08); text-align: left; }
.yetki-table th { background: rgba(255,255,255,0.03); color: var(--text-secondary, #94a3b8); font-size: 0.85rem; text-transform: uppercase; }
.modul-tag { background: rgba(59,130,246,0.2); color: #38bdf8; padding: 0.2rem 0.5rem; border-radius: 4px; font-weight: 600; font-size: 0.8rem; }
.yetki-checkbox { width: 18px; height: 18px; cursor: pointer; accent-color: #3b82f6; }
</style>
