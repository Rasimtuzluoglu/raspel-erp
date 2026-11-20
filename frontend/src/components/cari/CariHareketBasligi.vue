<template>
  <div class="hareket-info">
    <h3>{{ cari?.ad }}</h3>
    <div class="bakiye-ozet">
      <div class="ozet-satir">
        <span class="ozet-etiket">{{ t('cariHesaplar.toplamTahsilat') }}</span>
        <span class="positive">{{ formatCurrency(ozet.tahsilat) }}</span>
      </div>
      <div class="ozet-satir">
        <span class="ozet-etiket">{{ t('cariHesaplar.toplamOdeme') }}</span>
        <span class="negative">{{ formatCurrency(ozet.odeme) }}</span>
      </div>
      <div class="ozet-satir">
        <span class="ozet-etiket">{{ t('cariHesaplar.toplamBorclandirma') }}</span>
        <span class="negative">{{ formatCurrency(ozet.borclandirma) }}</span>
      </div>
      <div class="ozet-satir ozet-bakiye">
        <span class="ozet-etiket">{{ t('cariHesaplar.guncelBakiye') }}</span>
        <strong :class="ozet.bakiye >= 0 ? 'positive' : 'negative'">{{ formatCurrency(ozet.bakiye) }}</strong>
      </div>
    </div>
  </div>

  <div class="ekstre-arac">
    <div class="ekstre-hizli">
      <Button
        :label="t('cariHesaplar.buAy')"
        text
        class="p-button-sm"
        @click="hizliAralik('buAy')"
      />
      <Button
        :label="t('cariHesaplar.gecenAy')"
        text
        class="p-button-sm"
        @click="hizliAralik('gecenAy')"
      />
      <Button
        :label="t('cariHesaplar.buYil')"
        text
        class="p-button-sm"
        @click="hizliAralik('buYil')"
      />
      <Button
        :label="t('cariHesaplar.tumu')"
        text
        class="p-button-sm"
        @click="hizliAralik('tum')"
      />
    </div>
    <DatePicker
      v-model="ekstreBaslangic"
      :placeholder="t('cariHesaplar.ekstreBaslangic')"
      date-format="dd.mm.yy"
      show-icon
      class="ekstre-tarih"
    />
    <DatePicker
      v-model="ekstreBitis"
      :placeholder="t('cariHesaplar.ekstreBitis')"
      date-format="dd.mm.yy"
      show-icon
      class="ekstre-tarih"
    />
    <Button
      :label="t('cariHesaplar.ekstreYazdir')"
      icon="pi pi-print"
      class="p-button-sm p-button-outlined"
      @click="ekstreYazdir"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatCurrency } from '../../utils/format.js'

/**
 * Cari 360 kartı başlığı: cari adı, bakiye özeti ve ekstre tarih aralığı.
 * Veriyi parent'tan alır; yazdırma isteğini `ekstre` olayıyla bildirir.
 */
defineProps({
  cari: { type: Object, default: null },
  ozet: {
    type: Object,
    default: () => ({ tahsilat: 0, odeme: 0, borclandirma: 0, bakiye: 0 })
  }
})

const emit = defineEmits(['ekstre'])
const { t } = useI18n()

const ekstreBaslangic = ref(null)
const ekstreBitis = ref(null)

const hizliAralik = (tip) => {
  const b = new Date()
  if (tip === 'tum') {
    ekstreBaslangic.value = null
    ekstreBitis.value = null
  } else if (tip === 'buAy') {
    ekstreBaslangic.value = new Date(b.getFullYear(), b.getMonth(), 1)
    ekstreBitis.value = new Date(b.getFullYear(), b.getMonth() + 1, 0)
  } else if (tip === 'gecenAy') {
    ekstreBaslangic.value = new Date(b.getFullYear(), b.getMonth() - 1, 1)
    ekstreBitis.value = new Date(b.getFullYear(), b.getMonth(), 0)
  } else if (tip === 'buYil') {
    ekstreBaslangic.value = new Date(b.getFullYear(), 0, 1)
    ekstreBitis.value = new Date(b.getFullYear(), 11, 31)
  }
}

const ekstreYazdir = () => {
  emit('ekstre', { baslangic: ekstreBaslangic.value, bitis: ekstreBitis.value })
}
</script>

<style scoped>
.hareket-info {
  background: var(--bg-muted);
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 15px;
}

.hareket-info p {
  margin: 5px 0 0 0;
}

.bakiye-ozet {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--border);
}

.ozet-satir {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ozet-etiket {
  font-size: 12px;
  color: var(--text-secondary);
}

.ozet-bakiye {
  margin-left: auto;
}

.ekstre-arac {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}
.ekstre-hizli {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-secondary);
}
.ekstre-hizli :deep(.p-button) {
  padding: 4px 10px;
  font-size: 12px;
}
.ekstre-tarih {
  width: 150px;
}

.positive {
  color: var(--success);
  font-weight: bold;
}

.negative {
  color: var(--danger);
  font-weight: bold;
}
</style>
