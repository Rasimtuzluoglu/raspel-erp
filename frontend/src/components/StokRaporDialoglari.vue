<template>
  <Dialog
    :visible="degerlemeVisible"
    :header="t('stoklar.degerleme')"
    modal
    :style="{ width: '860px' }"
    @update:visible="emit('update:degerlemeVisible', $event)"
  >
    <div
      v-if="degerlemeVerisi"
      class="degerleme-ozet"
    >
      <span>{{ t('stoklar.ortalamaDeger') }}: <strong>{{ formatCurrency(degerlemeVerisi.toplamOrtalamaDeger) }}</strong></span>
      <span>{{ t('stoklar.fifoDeger') }}: <strong>{{ formatCurrency(degerlemeVerisi.toplamFifoDeger) }}</strong></span>
    </div>
    <DataTable
      :value="degerlemeVerisi?.satirlar || []"
      striped-rows
      :paginator="true"
      :rows="15"
      scrollable
      scroll-height="420px"
    >
      <template #empty>
        <EmptyState />
      </template>
      <Column
        field="stokKodu"
        :header="t('stoklar.kod')"
        style="width: 120px"
      />
      <Column
        field="ad"
        :header="t('common.description')"
      />
      <Column
        field="miktar"
        :header="t('stoklar.stokMiktar')"
        style="width: 100px"
      />
      <Column
        field="ortalamaBirimMaliyet"
        :header="t('stoklar.ortalamaMaliyet')"
        style="width: 140px"
      >
        <template #body="{ data }">
          {{ formatCurrency(data.ortalamaBirimMaliyet) }}
        </template>
      </Column>
      <Column
        field="ortalamaDeger"
        :header="t('stoklar.ortalamaDeger')"
        style="width: 140px"
      >
        <template #body="{ data }">
          {{ formatCurrency(data.ortalamaDeger) }}
        </template>
      </Column>
      <Column
        field="fifoBirimMaliyet"
        :header="t('stoklar.fifoMaliyet')"
        style="width: 140px"
      >
        <template #body="{ data }">
          {{ formatCurrency(data.fifoBirimMaliyet) }}
        </template>
      </Column>
      <Column
        field="fifoDeger"
        :header="t('stoklar.fifoDeger')"
        style="width: 140px"
      >
        <template #body="{ data }">
          {{ formatCurrency(data.fifoDeger) }}
        </template>
      </Column>
    </DataTable>
  </Dialog>

  <Dialog
    :visible="oneriVisible"
    :header="t('stoklar.siparisOnerisi')"
    modal
    :style="{ width: '760px' }"
    @update:visible="emit('update:oneriVisible', $event)"
  >
    <DataTable
      :value="oneriVerisi"
      striped-rows
      :paginator="true"
      :rows="15"
      scrollable
      scroll-height="420px"
    >
      <template #empty>
        <EmptyState />
      </template>
      <Column
        field="stokKodu"
        :header="t('stoklar.kod')"
        style="width: 120px"
      />
      <Column
        field="ad"
        :header="t('common.description')"
      />
      <Column
        field="mevcut"
        :header="t('stoklar.mevcut')"
        style="width: 100px"
      />
      <Column
        field="minMiktar"
        :header="t('stoklar.minMiktar')"
        style="width: 100px"
      />
      <Column
        field="oneriMiktar"
        :header="t('stoklar.oneriMiktar')"
        style="width: 110px"
      >
        <template #body="{ data }">
          <strong>{{ data.oneriMiktar }}</strong>
        </template>
      </Column>
      <Column
        field="tahminiTutar"
        :header="t('stoklar.tahminiTutar')"
        style="width: 130px"
      >
        <template #body="{ data }">
          {{ formatCurrency(data.tahminiTutar) }}
        </template>
      </Column>
    </DataTable>
  </Dialog>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatCurrency } from '../utils/format.js'
import EmptyState from './EmptyState.vue'

defineProps({
  degerlemeVisible: { type: Boolean, default: false },
  degerlemeVerisi: { type: Object, default: null },
  oneriVisible: { type: Boolean, default: false },
  oneriVerisi: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:degerlemeVisible', 'update:oneriVisible'])
const { t } = useI18n()
</script>

<style scoped>
.degerleme-ozet {
  display: flex;
  gap: 24px;
  margin-bottom: 12px;
  font-size: 14px;
  color: var(--text-secondary);
}
</style>
