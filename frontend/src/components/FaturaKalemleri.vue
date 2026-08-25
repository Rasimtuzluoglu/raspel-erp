<template>
  <div>
    <DataTable
      state-storage="session"
      state-key="fatura-kalemler-table-state"
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
      <Column header="Açıklama *">
        <template #body="s">
          <InputText
            v-model="s.data.aciklama"
            placeholder="Kalem açıklaması"
            class="w-full"
          />
        </template>
      </Column>
      <Column
        header="Adet *"
        style="width: 90px"
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
        header="Birim Fiyat *"
        style="width: 130px"
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
        header="İskonto %"
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
        header="KDV %"
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
        header="Tutar"
        style="width: 120px"
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
            @click="$emit('remove', s.index)"
          />
        </template>
      </Column>
    </DataTable>
    <div style="margin-top: 10px">
      <Button
        label="+ Kalem Ekle"
        icon="pi pi-plus"
        class="p-button-sm p-button-outlined"
        @click="$emit('add')"
      />
    </div>

    <div class="summary-box">
      <div class="summary-row">
        <span>Ara Toplam:</span><span>{{ formatCurrency(araToplam) }}</span>
      </div>
      <div class="summary-row">
        <span>KDV:</span><span>{{ formatCurrency(kdvToplam) }}</span>
      </div>
      <div class="summary-row total">
        <span>Genel Toplam:</span><span>{{ formatCurrency(genelToplam) }}</span>
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
  border-top: 2px solid #3b82f6;
  margin-top: 5px;
  padding-top: 10px;
  color: var(--text-primary);
}
</style>
