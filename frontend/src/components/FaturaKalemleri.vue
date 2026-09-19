<template>
  <div>
    <DataTable
      :value="kalemler"
      striped-rows
    >
      <Column
        header="#"
        style="width: 40px"
      >
        <template #body="s">
          {{ s.index + 1 }}
        </template>
      </Column>
      <Column :header="$t('faturaKalemleri.aciklamaZorunlu')">
        <template #body="s">
          <InputText
            v-model="s.data.aciklama"
            :placeholder="$t('faturaKalemleri.aciklamaPlaceholder')"
            class="w-full"
          />
        </template>
      </Column>
      <Column
        :header="$t('faturaKalemleri.adetZorunlu')"
        style="width: 110px"
      >
        <template #body="s">
          <InputNumber
            v-model="s.data.adet"
            :min="1"
            class="w-full"
          />
        </template>
      </Column>
      <Column
        :header="$t('faturaKalemleri.birimFiyatZorunlu')"
        style="width: 140px"
      >
        <template #body="s">
          <InputNumber
            v-model="s.data.birimFiyat"
            :min="0"
            :min-fraction-digits="2"
            :max-fraction-digits="2"
            class="w-full"
          />
        </template>
      </Column>
      <Column
        :header="$t('faturaKalemleri.iskonto')"
        style="width: 100px"
      >
        <template #body="s">
          <InputNumber
            v-model="s.data.iskontoOrani"
            :min="0"
            :max="100"
            :min-fraction-digits="0"
            class="w-full"
          />
        </template>
      </Column>
      <Column
        :header="$t('faturaKalemleri.kdv')"
        style="width: 80px"
      >
        <template #body="s">
          <Dropdown
            v-model="s.data.kdvOrani"
            :options="[0, 10, 20]"
            class="w-full"
          />
        </template>
      </Column>
      <Column
        :header="$t('faturaKalemleri.tutar')"
        style="width: 130px"
      >
        <template #body="s">
          {{ formatCurrency(kalemTutar(s.data)) }}
        </template>
      </Column>
      <Column
        header=""
        style="width: 50px"
      >
        <template #body="s">
          <Button
            icon="pi pi-trash"
            class="p-button-rounded p-button-danger p-button-sm"
            :aria-label="$t('common.delete')"
            :title="$t('common.delete')"
            @click="$emit('remove', s.index)"
          />
        </template>
      </Column>
    </DataTable>
    <div style="margin-top: 10px">
      <Button
        :label="'+ ' + $t('faturaKalemleri.kalemEkle')"
        icon="pi pi-plus"
        class="p-button-sm p-button-outlined"
        @click="$emit('add')"
      />
    </div>

    <div class="summary-box">
      <div class="summary-row">
        <span>{{ $t('faturaKalemleri.araToplam') }}</span><span>{{ formatCurrency(araToplam) }}</span>
      </div>
      <div class="summary-row">
        <span>{{ $t('faturaKalemleri.kdvLabel') }}</span><span>{{ formatCurrency(kdvToplam) }}</span>
      </div>
      <div class="summary-row total">
        <span>{{ $t('faturaKalemleri.genelToplam') }}</span><span>{{ formatCurrency(genelToplam) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { formatCurrency } from '../utils/format.js'
import { kalemTutar } from '../utils/faturaHesapla.js'

defineProps({
  kalemler: { type: Array, required: true },
  araToplam: { type: Number, default: 0 },
  kdvToplam: { type: Number, default: 0 },
  genelToplam: { type: Number, default: 0 }
})

defineEmits(['add', 'remove'])
</script>

<style scoped>
.summary-box {
  background: var(--border);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 15px;
  margin-top: 15px;
}
.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 5px 0;
  font-size: 14px;
  color: var(--text-secondary);
}
.summary-row.total {
  font-weight: 700;
  font-size: 18px;
  border-top: 2px solid var(--accent);
  margin-top: 5px;
  padding-top: 10px;
  color: var(--text-primary);
}
</style>
