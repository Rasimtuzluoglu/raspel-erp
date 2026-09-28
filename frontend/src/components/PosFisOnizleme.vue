<template>
  <div
    id="fisOnizleme"
    class="fis-onizleme"
  >
    <div class="fis-header">
      <img
        v-if="sirketLogosu"
        :src="sirketLogosu"
        class="fis-logo"
        alt="logo"
      >
      <div class="fis-baslik">
        {{ sirketAdi || 'RASPEL ERP' }}
      </div>
      <div class="fis-tarih">
        {{ simdikiTarih }}
      </div>
      <div class="fis-fisno">
        {{ t('hizliSatis.fisNo') }} {{ fisNo || '-------' }}
      </div>
    </div>
    <div
      v-if="musteriAdi"
      class="fis-musteri"
    >
      <span>{{ t('hizliSatis.fisMusteri') }} {{ musteriAdi }}</span>
    </div>
    <div class="fis-ayrac">
      ---
    </div>
    <div class="fis-kalemler">
      <div
        v-for="i in sepet"
        :key="i.id"
        class="fis-kalem"
      >
        <div class="fis-kalem-ad">
          {{ i.ad }} x{{ i.miktar }}
        </div>
        <div
          v-if="fisFiyatli"
          class="fis-kalem-tutar"
        >
          {{ formatCurrency(i.miktar * i.fiyat) }}
        </div>
      </div>
    </div>
    <div class="fis-ayrac">
      ---
    </div>
    <template v-if="fisFiyatli">
      <div class="fis-toplam">
        <span>{{ t('hizliSatis.araToplam') }}</span>
        <span>{{ formatCurrency(toplam) }}</span>
      </div>
      <div
        v-if="indirimDegeri > 0"
        class="fis-indirim"
      >
        <span>{{ t('hizliSatis.fisIndirim', { n: indirimTipi === 'yuzde' ? indirimDegeri + '%' : '' }) }}</span>
        <span>-{{ formatCurrency(indirimTutari) }}</span>
      </div>
      <div class="fis-genel-toplam">
        <span>{{ t('hizliSatis.fisGenelToplam') }}</span>
        <span class="fis-toplam-deger">{{ formatCurrency(genelToplam) }}</span>
      </div>
      <div class="fis-ayrac">
        ---
      </div>
    </template>
    <div class="fis-odeme">
      <div
        v-if="fisFiyatli"
        class="fis-odeme-satir"
      >
        <span>{{ t('hizliSatis.fisOdenen') }}</span>
        <span>{{ formatCurrency(odenenTutar) }}</span>
      </div>
      <div
        v-if="fisFiyatli && kalanTutar > 0"
        class="fis-odeme-satir"
      >
        <span>{{ t('hizliSatis.fisKalan') }}</span>
        <span>{{ formatCurrency(kalanTutar) }}</span>
      </div>
      <div class="fis-odeme-satir fis-odeme-durum">
        <span>{{ t('hizliSatis.toplamUrun') }}</span>
        <span>{{ sepet ? sepet.length : 0 }}</span>
      </div>
      <div class="fis-odeme-satir fis-odeme-durum">
        <span>{{ t('common.status') }}</span>
        <span>{{ odemeDurumText }}</span>
      </div>
    </div>
    <div class="fis-footer">
      <div class="fis-ayrac">
        ---
      </div>
      <div class="fis-tesekkur">
        {{ fisAltNotu || t('hizliSatis.fisAltNotFallback') }}
      </div>
      <div
        v-if="authStore?.kullanici?.displayName"
        class="fis-satici"
      >
        {{ t('hizliSatis.islemYapan') }} {{ authStore?.kullanici?.displayName }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '../stores/authStore.js'
import { formatCurrency } from '../utils/format.js'

const { t } = useI18n()
const authStore = useAuthStore()

defineProps({
  sirketLogosu: { type: String, default: '' },
  sirketAdi: { type: String, default: '' },
  simdikiTarih: { type: String, default: '' },
  fisNo: { type: String, default: '' },
  musteriAdi: { type: String, default: '' },
  sepet: { type: Array, default: () => [] },
  fisFiyatli: { type: Boolean, default: true },
  toplam: { type: Number, default: 0 },
  indirimDegeri: { type: Number, default: 0 },
  indirimTipi: { type: String, default: 'tutar' },
  indirimTutari: { type: Number, default: 0 },
  genelToplam: { type: Number, default: 0 },
  odenenTutar: { type: Number, default: 0 },
  kalanTutar: { type: Number, default: 0 },
  odemeDurumText: { type: String, default: '' },
  fisAltNotu: { type: String, default: '' }
})
</script>

<style scoped>
.fis-onizleme {
  width: 80mm;
  margin: 0 auto;
  padding: 12px 8px;
  background: white;
  color: black;
  font-size: 11px;
  font-family: 'Courier New', monospace;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}
.fis-header {
  text-align: center;
  margin-bottom: 6px;
}
.fis-logo {
  display: block;
  max-height: 40px;
  max-width: 120px;
  margin: 0 auto 4px;
  object-fit: contain;
}
.fis-baslik {
  font-size: 13px;
  font-weight: 700;
}
.fis-tarih {
  font-size: 10px;
  margin-top: 2px;
}
.fis-fisno {
  font-size: 10px;
  margin-top: 1px;
  color: #555;
}
.fis-musteri {
  margin-bottom: 4px;
  font-size: 10px;
}
.fis-ayrac {
  text-align: center;
  color: #999;
  margin: 3px 0;
  letter-spacing: 2px;
}
.fis-kalem {
  display: flex;
  justify-content: space-between;
  padding: 2px 0;
}
.fis-kalem-tutar {
  white-space: nowrap;
}
.fis-toplam {
  display: flex;
  justify-content: space-between;
  padding: 3px 0;
  font-size: 12px;
}
.fis-indirim {
  display: flex;
  justify-content: space-between;
  padding: 2px 0;
  color: #c00;
  font-size: 11px;
}
.fis-genel-toplam {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  border-top: 2px solid #000;
  font-weight: 700;
  font-size: 13px;
}
.fis-odeme {
  margin-top: 4px;
}
.fis-odeme-satir {
  display: flex;
  justify-content: space-between;
  padding: 2px 0;
  font-size: 10px;
}
.fis-odeme-durum {
  font-weight: 600;
}
.fis-footer {
  text-align: center;
  margin-top: 4px;
}
.fis-tesekkur {
  font-size: 10px;
  color: #555;
}
.fis-satici {
  font-size: 9px;
  color: #888;
  margin-top: 2px;
  text-align: center;
}
</style>
