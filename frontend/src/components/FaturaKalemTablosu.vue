<template>
  <table class="fatura-kalem-tablosu">
    <thead>
      <tr>
        <th
          v-if="ayarlar.kolonSiraNo"
          style="width: 35px"
        >
          #
        </th>
        <th
          v-if="ayarlar.kolonStokKodu"
          style="width: 100px"
        >
          {{ t('faturaTasarim.urunKodu') }}
        </th>
        <th>{{ t('faturaTasarim.malHizmetAciklamasi') }}</th>
        <th style="width: 65px; text-align: center">
          {{ t('faturaTasarim.miktar') }}
        </th>
        <th style="width: 55px; text-align: center">
          {{ t('faturaTasarim.birim') }}
        </th>
        <th
          v-if="ayarlar.fiyatGoster"
          style="width: 95px; text-align: right"
        >
          {{ t('faturaTasarim.birimFiyat') }}
        </th>
        <th
          v-if="ayarlar.kolonIskonto && ayarlar.fiyatGoster"
          style="width: 65px; text-align: center"
        >
          {{ t('faturaTasarim.iskontoKisa') }}
        </th>
        <th
          v-if="ayarlar.kolonKdvOrani && ayarlar.fiyatGoster"
          style="width: 60px; text-align: center"
        >
          {{ t('faturaTasarim.kdvKisa') }}
        </th>
        <th
          v-if="ayarlar.fiyatGoster"
          style="width: 110px; text-align: right"
        >
          {{ t('faturaTasarim.tutar') }}
        </th>
      </tr>
    </thead>
    <tbody>
      <tr
        v-for="(k, idx) in kalemler"
        :key="idx"
      >
        <td
          v-if="ayarlar.kolonSiraNo"
          class="text-center"
        >
          {{ idx + 1 }}
        </td>
        <td
          v-if="ayarlar.kolonStokKodu"
          class="kod-td"
        >
          {{ k.stokKodu || 'STK-' + (100 + idx) }}
        </td>
        <td>
          <span class="kalem-ad">{{ k.aciklama }}</span>
        </td>
        <td class="text-center font-bold">
          {{ k.adet }}
        </td>
        <td class="text-center">
          {{ k.birim || t('faturaTasarim.adet') }}
        </td>
        <td
          v-if="ayarlar.fiyatGoster"
          class="text-right"
        >
          {{ formatCurrency(k.birimFiyat) }}
        </td>
        <td
          v-if="ayarlar.kolonIskonto && ayarlar.fiyatGoster"
          class="text-center"
        >
          {{ k.iskontoOrani ? `%${k.iskontoOrani}` : '-' }}
        </td>
        <td
          v-if="ayarlar.kolonKdvOrani && ayarlar.fiyatGoster"
          class="text-center"
        >
          %{{ kdvOrani(k) }}
        </td>
        <td
          v-if="ayarlar.fiyatGoster"
          class="text-right font-bold"
        >
          {{ formatCurrency(k.tutar) }}
        </td>
      </tr>
    </tbody>
  </table>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatCurrency } from '../utils/format.js'
import { kdvOrani } from '../utils/faturaHesapla.js'

defineProps({
  ayarlar: { type: Object, required: true },
  kalemler: { type: Array, default: () => [] }
})
const { t } = useI18n()
</script>

<style scoped>
.fatura-kalem-tablosu {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
  font-size: 11px;
}
.fatura-kalem-tablosu th {
  background: var(--vurgu-renk, #1e40af);
  color: #ffffff;
  font-weight: 700;
  padding: 7px 8px;
  text-align: left;
  font-size: 10.5px;
}
.fatura-kalem-tablosu td {
  padding: 6px 8px;
  border-bottom: 1px solid #e2e8f0;
  color: #1e293b;
}
.fatura-kalem-tablosu tr:nth-child(even) td {
  background: #f8fafc;
}
.kod-td {
  font-family: monospace;
  font-size: 10px;
  color: #475569;
}
.kalem-ad {
  font-weight: 600;
}
</style>
