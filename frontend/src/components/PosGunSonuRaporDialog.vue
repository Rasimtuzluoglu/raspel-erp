<template>
  <Dialog
    :visible="visible"
    :header="t('posTerminalleri.gunSonuRaporu')"
    modal
    :style="{ width: '720px' }"
    :breakpoints="{ '575px': '92vw' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div class="rapor-filtre">
      <DatePicker
        v-model="tarihAraligi"
        selection-mode="range"
        date-format="dd.mm.yy"
        :placeholder="t('posTerminalleri.tarihAraligi')"
        show-icon
        class="rapor-filtre-alan"
      />
      <span class="rapor-ozet">
        {{ t('posTerminalleri.toplam') }}
        <strong>{{ formatPara(toplamTutar) }}</strong>
        · {{ t('posTerminalleri.komisyon') }}
        <strong>{{ formatPara(toplamKomisyon) }}</strong>
      </span>
    </div>

    <DataTable
      :value="filtreliSatirlar"
      :loading="yukleniyor"
      striped-rows
      size="small"
      scrollable
      scroll-height="50vh"
      :paginator="filtreliSatirlar.length > 20"
      :rows="20"
    >
      <template #empty>
        <EmptyState
          icon="pi pi-file"
          :message="t('posTerminalleri.raporYok')"
        />
      </template>
      <Column
        field="tarih"
        :header="t('common.date')"
        style="width: 120px"
        sortable
      />
      <Column
        :header="t('posTerminalleri.title')"
        sortable
        field="posAd"
      >
        <template #body="s">
          {{ s.data.posAd || s.data.posId }}
        </template>
      </Column>
      <Column
        field="tutar"
        :header="t('posTerminalleri.toplam')"
        style="width: 140px"
        sortable
      >
        <template #body="s">
          {{ formatPara(s.data.tutar) }}
        </template>
      </Column>
      <Column
        field="komisyon"
        :header="t('posTerminalleri.komisyon')"
        style="width: 140px"
        sortable
      >
        <template #body="s">
          {{ formatPara(s.data.komisyon) }}
        </template>
      </Column>
    </DataTable>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { posAPI } from '../api/index.js'
import EmptyState from './EmptyState.vue'
import { formatPara, getLocalDateString } from '../utils/format.js'

const props = defineProps({
  visible: { type: Boolean, default: false },
  /** POS id -> ad eşlemesi için terminaller listesi. */
  terminaller: { type: Array, default: () => [] }
})

defineEmits(['update:visible'])

const { t } = useI18n()
const satirlar = ref([])
const yukleniyor = ref(false)
const tarihAraligi = ref(null)

const posAdHaritasi = computed(() => {
  const m = {}
  for (const p of props.terminaller) m[p.id] = p.ad
  return m
})

const filtreliSatirlar = computed(() =>
  satirlar.value.filter((s) => {
    if (Array.isArray(tarihAraligi.value) && tarihAraligi.value.length === 2 && tarihAraligi.value[0]) {
      const tr = s.tarih || ''
      if (tr < getLocalDateString(tarihAraligi.value[0]) || tr > getLocalDateString(tarihAraligi.value[1])) return false
    }
    return true
  })
)
const toplamTutar = computed(() => filtreliSatirlar.value.reduce((a, s) => a + (Number(s.tutar) || 0), 0))
const toplamKomisyon = computed(() => filtreliSatirlar.value.reduce((a, s) => a + (Number(s.komisyon) || 0), 0))

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await posAPI.gunSonuRapor()
    satirlar.value = (r.data || []).map((s) => ({ ...s, posAd: posAdHaritasi.value[s.posId] || null }))
  } catch {
    satirlar.value = []
  } finally {
    yukleniyor.value = false
  }
}

watch(
  () => props.visible,
  (acik) => {
    if (acik) {
      tarihAraligi.value = null
      yukle()
    }
  }
)
</script>

<style scoped>
.rapor-filtre {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.rapor-filtre-alan {
  flex: 1 1 240px;
  min-width: 0;
}
.rapor-ozet {
  font-size: 0.82rem;
  color: var(--text-secondary);
}
.rapor-ozet strong {
  color: var(--text-primary);
}
</style>
