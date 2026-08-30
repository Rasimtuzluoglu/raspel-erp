<template>
  <div class="yetki-page">
    <PageHeader
      title="Rol & Yetki Matrisi (RBAC)"
      subtitle="Sistemdeki rollerin modül bazlı okuma, yazma, silme ve dışa aktarım izinleri"
    >
      <template #actions>
        <div class="flex items-center gap-2">
          <Button
            label="Tümünü Seç"
            icon="pi pi-check-square"
            class="p-button-outlined p-button-sm"
            @click="tumunuSec"
          />
          <Button
            label="Tümünü Temizle"
            icon="pi pi-times-circle"
            class="p-button-outlined p-button-secondary p-button-sm"
            @click="tumunuTemizle"
          />
          <Button
            label="Değişiklikleri Kaydet"
            icon="pi pi-check"
            class="p-button-primary p-button-sm"
            :loading="kaydediliyor"
            @click="kaydet"
          />
        </div>
      </template>
    </PageHeader>

    <div
      v-if="yukleniyor"
      class="p-4"
    >
      <SkeletonLoader :count="5" />
    </div>

    <div
      v-else
      class="matrix-container"
    >
      <!-- Sol Rol Seçim Menüsü -->
      <div class="roles-sidebar">
        <h4 class="text-xs font-bold uppercase text-muted mb-2 px-2">
          Sistem Rolleri
        </h4>
        <button
          v-for="r in roller"
          :key="r.id"
          type="button"
          class="role-tab-btn"
          :class="{ aktif: seciliRol?.id === r.id }"
          @click="rolSec(r)"
        >
          <div class="flex items-center gap-2">
            <i class="pi pi-shield text-base" />
            <div class="text-left">
              <div class="font-bold text-sm leading-tight">
                {{ r.ad }}
              </div>
              <div class="text-xs text-muted leading-tight mt-0.5">
                {{ r.yetkiler?.length || 0 }} İzin Aktif
              </div>
            </div>
          </div>
          <i
            v-if="seciliRol?.id === r.id"
            class="pi pi-chevron-right text-xs"
          />
        </button>
      </div>

      <!-- Sağ Matris Kartı -->
      <div
        v-if="seciliRol"
        class="matrix-card"
      >
        <div class="matrix-card-header flex justify-between items-center pb-4 border-b mb-4">
          <div>
            <h3 class="text-lg font-bold text-primary dark:text-gray-100 flex items-center gap-2">
              <i class="pi pi-shield text-primary" /> {{ seciliRol.ad }}
            </h3>
            <p class="text-xs text-muted mt-0.5">
              {{ seciliRol.aciklama || 'Bu role atanmış yetkileri aşağıdan yönetebilirsiniz.' }}
            </p>
          </div>
          <Tag
            :value="`${seciliRol.yetkiler?.length || 0} / ${yetkiler.length} Yetki Tanımlı`"
            severity="info"
          />
        </div>

        <div class="overflow-x-auto">
          <table class="rbac-table w-full">
            <thead>
              <tr>
                <th class="text-left">
                  Modül Adı
                </th>
                <th class="text-center">
                  <i class="pi pi-eye mr-1" /> Okuma
                </th>
                <th class="text-center">
                  <i class="pi pi-pencil mr-1" /> Yazma / Ekleme
                </th>
                <th class="text-center">
                  <i class="pi pi-trash mr-1" /> Silme
                </th>
                <th class="text-center">
                  <i class="pi pi-download mr-1" /> Dışa Aktar
                </th>
                <th class="text-right">
                  Hızlı İşlem
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="m in moduller"
                :key="m.kod"
                class="hover:bg-secondary/50 dark:hover:bg-gray-800/50"
              >
                <td class="font-semibold text-sm">
                  <div class="flex items-center gap-2">
                    <span class="modul-badge">{{ m.ad }}</span>
                  </div>
                </td>
                <!-- Okuma -->
                <td class="text-center">
                  <input
                    v-if="m.yetkiler.read"
                    type="checkbox"
                    class="yetki-cb"
                    :checked="yetkiVarmis(m.yetkiler.read.id)"
                    @change="yetkiToggle(m.yetkiler.read.id)"
                  >
                  <span
                    v-else
                    class="text-muted text-xs"
                  >-</span>
                </td>
                <!-- Yazma -->
                <td class="text-center">
                  <input
                    v-if="m.yetkiler.write"
                    type="checkbox"
                    class="yetki-cb"
                    :checked="yetkiVarmis(m.yetkiler.write.id)"
                    @change="yetkiToggle(m.yetkiler.write.id)"
                  >
                  <span
                    v-else
                    class="text-muted text-xs"
                  >-</span>
                </td>
                <!-- Silme -->
                <td class="text-center">
                  <input
                    v-if="m.yetkiler.delete"
                    type="checkbox"
                    class="yetki-cb danger"
                    :checked="yetkiVarmis(m.yetkiler.delete.id)"
                    @change="yetkiToggle(m.yetkiler.delete.id)"
                  >
                  <span
                    v-else
                    class="text-muted text-xs"
                  >-</span>
                </td>
                <!-- Dışa Aktarma -->
                <td class="text-center">
                  <input
                    v-if="m.yetkiler.export"
                    type="checkbox"
                    class="yetki-cb success"
                    :checked="yetkiVarmis(m.yetkiler.export.id)"
                    @change="yetkiToggle(m.yetkiler.export.id)"
                  >
                  <span
                    v-else
                    class="text-muted text-xs"
                  >-</span>
                </td>
                <!-- Satır Bazlı Hızlı Toggle -->
                <td class="text-right">
                  <Button
                    :label="modulTumYetkilerVarMi(m) ? 'Kaldır' : 'Tümü'"
                    :class="modulTumYetkilerVarMi(m) ? 'p-button-text p-button-danger p-button-xs' : 'p-button-text p-button-primary p-button-xs'"
                    @click="modulToggle(m)"
                  />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { apiClient } from '../api/index.js'

const toastBildirim = useToastBildirim()
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
    if (roller.value.length > 0 && !seciliRol.value) {
      seciliRol.value = { ...roller.value[0], yetkiler: [...(roller.value[0].yetkiler || [])] }
    }
  } catch {
    toastBildirim.hata('Yetki matrisi yüklenemedi.')
  } finally {
    yukleniyor.value = false
  }
}

const rolSec = (r) => {
  seciliRol.value = { ...r, yetkiler: [...(r.yetkiler || [])] }
}

// Modül bazlı gruplandırma
const moduller = computed(() => {
  const modulMap = {
    Cari: { ad: 'Cari Hesaplar', kod: 'CARI' },
    Fatura: { ad: 'Faturalar & E-Fatura', kod: 'FATURA' },
    Stok: { ad: 'Stok & Depo Yönetimi', kod: 'STOK' },
    Finans: { ad: 'Finans (Banka, Kasa, Çek)', kod: 'FINANS' },
    Siparis: { ad: 'Siparişler & Teklifler', kod: 'SIPARIS' },
    Satinalma: { ad: 'Satın Alma Yönetimi', kod: 'SATINALMA' },
    Irsaliye: { ad: 'İrsaliye İşlemleri', kod: 'IRSALIYE' },
    IK: { ad: 'İnsan Kaynakları & Personel', kod: 'IK' },
    Rapor: { ad: 'Raporlar & Analizler', kod: 'RAPOR' },
    Sistem: { ad: 'Sistem & Kullanıcı Ayarları', kod: 'SISTEM' }
  }

  return Object.entries(modulMap).map(([mKey, mVal]) => {
    const mYetkiler = yetkiler.value.filter(y => y.modul === mKey || y.kod.startsWith(mVal.kod))
    return {
      kod: mVal.kod,
      ad: mVal.ad,
      yetkiler: {
        read: mYetkiler.find(y => y.kod.endsWith('_READ')),
        write: mYetkiler.find(y => y.kod.endsWith('_WRITE')),
        delete: mYetkiler.find(y => y.kod.endsWith('_DELETE')),
        export: mYetkiler.find(y => y.kod.endsWith('_EXPORT') || y.kod === 'EXPORT_DATA')
      }
    }
  })
})

const yetkiVarmis = (yetkiId) => {
  if (!seciliRol.value || !seciliRol.value.yetkiler) return false
  return seciliRol.value.yetkiler.some(y => (y.id || y) === yetkiId)
}

const yetkiToggle = (yetkiId) => {
  if (!seciliRol.value) return
  if (!seciliRol.value.yetkiler) seciliRol.value.yetkiler = []

  const idx = seciliRol.value.yetkiler.findIndex(y => (y.id || y) === yetkiId)
  if (idx > -1) {
    seciliRol.value.yetkiler.splice(idx, 1)
  } else {
    const yObj = yetkiler.value.find(y => y.id === yetkiId)
    if (yObj) seciliRol.value.yetkiler.push(yObj)
  }
}

const modulTumYetkilerVarMi = (modul) => {
  const mList = Object.values(modul.yetkiler).filter(Boolean)
  if (!mList.length) return false
  return mList.every(y => yetkiVarmis(y.id))
}

const modulToggle = (modul) => {
  const mList = Object.values(modul.yetkiler).filter(Boolean)
  const hepsiVar = modulTumYetkilerVarMi(modul)

  mList.forEach(y => {
    const varMi = yetkiVarmis(y.id)
    if (hepsiVar && varMi) {
      yetkiToggle(y.id)
    } else if (!hepsiVar && !varMi) {
      yetkiToggle(y.id)
    }
  })
}

const tumunuSec = () => {
  if (!seciliRol.value) return
  seciliRol.value.yetkiler = [...yetkiler.value]
}

const tumunuTemizle = () => {
  if (!seciliRol.value) return
  seciliRol.value.yetkiler = []
}

const kaydet = async () => {
  if (!seciliRol.value) return
  kaydediliyor.value = true
  try {
    const yetkiIds = seciliRol.value.yetkiler.map(y => y.id || y)
    await apiClient.put(`/yetkiler/roller/${seciliRol.value.id}`, yetkiIds)
    toastBildirim.basarili(`"${seciliRol.value.ad}" rolü izinleri başarıyla güncellendi.`)
    await verileriYukle()
  } catch {
    toastBildirim.hata('Rol yetkileri güncellenemedi.')
  } finally {
    kaydediliyor.value = false
  }
}

onMounted(() => {
  verileriYukle()
})
</script>

<style scoped>
.yetki-page {
  padding: 1rem;
}
.matrix-container {
  display: flex;
  gap: 1.25rem;
  margin-top: 1rem;
  align-items: flex-start;
}
.roles-sidebar {
  width: 240px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.role-tab-btn {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-radius: 10px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  color: var(--text-primary);
  cursor: pointer;
  transition: all 0.2s ease;
}
.role-tab-btn:hover {
  border-color: var(--primary-color, #3b82f6);
}
.role-tab-btn.aktif {
  background: var(--primary-color, #3b82f6);
  border-color: var(--primary-color, #3b82f6);
  color: #ffffff;
}
.role-tab-btn.aktif .text-muted {
  color: rgba(255, 255, 255, 0.8) !important;
}

.matrix-card {
  flex: 1;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.03);
}

.rbac-table {
  border-collapse: collapse;
}
.rbac-table th {
  padding: 10px 12px;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--text-secondary);
  border-bottom: 2px solid var(--border);
  background: rgba(0,0,0,0.02);
}
.rbac-table td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--border);
}
.modul-badge {
  font-weight: 600;
  color: var(--text-primary);
}
.yetki-cb {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: #3b82f6;
}
.yetki-cb.danger {
  accent-color: #ef4444;
}
.yetki-cb.success {
  accent-color: #10b981;
}

.p-button-xs {
  padding: 2px 8px !important;
  font-size: 11px !important;
}
</style>
