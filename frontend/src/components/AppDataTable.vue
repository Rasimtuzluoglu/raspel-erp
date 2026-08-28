<template>
  <div class="app-datatable-wrapper">
    <transition name="fade">
      <div
        v-if="selection && selection.length > 0"
        class="batch-action-bar"
      >
        <div class="batch-info">
          <i class="pi pi-check-square" />
          <span><strong>{{ selection ? selection.length : 0 }}</strong> {{ $t('common.selectedRecords', { n: selection.length }) }}</span>
        </div>
        <div class="batch-buttons">
          <slot name="batch-actions" />
          <Button
            :label="$t('common.clearSelection')"
            icon="pi pi-times"
            class="p-button-text p-button-sm"
            @click="$emit('update:selection', [])"
          />
        </div>
      </div>
    </transition>

    <div
      v-if="aramaAktif"
      class="tablo-arama"
    >
      <i class="pi pi-search" />
      <InputText
        v-model="aramaTerimi"
        :placeholder="aramaPlaceholder || $t('common.searchPlaceholder')"
        class="arama-input"
      />
      <Button
        v-if="aramaTerimi"
        icon="pi pi-times"
        class="p-button-rounded p-button-text"
        @click="aramaTerimi = ''"
      />
    </div>

    <DataTable
      v-bind="$attrs"
      :value="filtrelenmisDeger"
      :loading="loading"
      :paginator="paginator"
      :rows="aktifRows"
      :lazy="lazy"
      :total-records="totalRecords"
      :rows-per-page-options="rowsPerPageOptions"
      responsive-layout="scroll"
      :selection="selection"
      @update:selection="$emit('update:selection', $event)"
      @page="$emit('page', $event)"
      @sort="$emit('sort', $event)"
      @update:rows="rowsDegisti"
    >
      <template #empty>
        <EmptyState
          v-if="!loading"
          :message="emptyMessage"
        />
      </template>
      <slot />
    </DataTable>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import EmptyState from './EmptyState.vue'

const props = defineProps({
  value: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  paginator: { type: Boolean, default: true },
  emptyMessage: { type: String, default: 'Kayıt bulunamadı' },
  rows: { type: Number, default: 25 },
  lazy: { type: Boolean, default: false },
  totalRecords: { type: Number, default: 0 },
  rowsPerPageOptions: { type: Array, default: () => [10, 25, 50, 100] },
  selection: { type: Array, default: () => [] },
  /** localStorage anahtarı — sayfa boyutu tercihi bu anahtar altında kalıcı saklanır */
  gorunumAnahtari: { type: String, default: '' },
  /** Arama kutusunu gösterir */
  aramaAktif: { type: Boolean, default: false },
  aramaPlaceholder: { type: String, default: 'Ara...' }
})

defineEmits(['page', 'sort', 'update:selection'])

const ROWS_KEY = 'raspel_tablo_rows'
const aktifRows = ref(props.rows)
const aramaTerimi = ref('')

const filtrelenmisDeger = computed(() => {
  const t = (aramaTerimi.value || '').trim().toLowerCase()
  if (!t) return props.value
  return props.value.filter((satir) => {
    if (!satir) return false
    return Object.values(satir).some((val) => {
      if (val == null) return false
      return String(val).toLowerCase().includes(t)
    })
  })
})

const rowsDegisti = (yeniSatirSayisi) => {
  aktifRows.value = yeniSatirSayisi
  if (props.gorunumAnahtari) {
    try {
      const kayitli = JSON.parse(localStorage.getItem(ROWS_KEY) || '{}')
      kayitli[props.gorunumAnahtari] = yeniSatirSayisi
      localStorage.setItem(ROWS_KEY, JSON.stringify(kayitli))
    } catch {
      /* empty */
    }
  }
}

onMounted(() => {
  if (props.gorunumAnahtari) {
    try {
      const kayitli = JSON.parse(localStorage.getItem(ROWS_KEY) || '{}')
      if (kayitli[props.gorunumAnahtari]) aktifRows.value = kayitli[props.gorunumAnahtari]
    } catch {
      /* empty */
    }
  }
})

watch(
  () => props.rows,
  (yeni) => {
    aktifRows.value = yeni
  }
)
</script>

<style scoped>
.app-datatable-wrapper {
  width: 100%;
  overflow-x: auto;
  position: relative;
}

.tablo-arama {
  display: flex;
  align-items: center;
  gap: 4px;
  max-width: 320px;
  margin-bottom: 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 2px 8px;
}
.tablo-arama > i {
  color: var(--text-muted);
  font-size: 14px;
  margin-left: 4px;
}
.arama-input {
  border: none;
  background: transparent;
  box-shadow: none !important;
}
.arama-input:focus {
  box-shadow: none !important;
}

.batch-action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.15) 0%, rgba(139, 92, 246, 0.15) 100%);
  border: 1px solid rgba(59, 130, 246, 0.3);
  border-radius: 10px;
  padding: 8px 16px;
  margin-bottom: 12px;
  color: var(--text-primary, #f1f5f9);
}

.batch-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.batch-info i {
  color: #3b82f6;
  font-size: 16px;
}

.batch-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}

.fade-enter-active,
.fade-leave-active {
  transition: all 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
