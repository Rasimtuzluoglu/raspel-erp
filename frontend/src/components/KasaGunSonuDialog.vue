<template>
  <Dialog
    v-model:visible="gunSonuDialog"
    :header="t('kasa.gunSonuBaslik')"
    :modal="true"
    class="gun-sonu-dialog"
  >
    <div class="gun-sonu-filtre">
      <DatePicker
        v-model="gunSonuTarih"
        date-format="dd.mm.yy"
        show-icon
        :manual-input="false"
        @date-select="gunSonuYukle"
      />
      <Dropdown
        v-model="gunSonuKasaId"
        :options="kasaStore.kasalar"
        option-label="ad"
        option-value="id"
        show-clear
        :placeholder="t('kasa.tumKasalar')"
        @change="gunSonuYukle"
      />
    </div>
    <div
      v-if="gunSonuYukleniyor"
      class="gun-sonu-bos"
    >
      {{ t('common.loading') }}
    </div>
    <div
      v-else-if="!gunSonuVerisi.length"
      class="gun-sonu-bos"
    >
      {{ t('kasa.gunSonuKayitYok') }}
    </div>
    <div
      v-for="k in gunSonuVerisi"
      v-else
      :key="k.kasaId"
      class="gun-sonu-kart"
    >
      <div class="gun-sonu-alt-baslik">
        {{ k.kasaAd }}
      </div>
      <div class="gun-sonu-satir">
        <span>{{ t('kasa.acilisBakiye') }}</span>
        <strong>{{ formatCurrency(k.acilisBakiye) }}</strong>
      </div>
      <div class="gun-sonu-satir">
        <span>{{ t('kasa.gunIciGiris') }}</span>
        <strong class="pozitif">+{{ formatCurrency(k.gunIciGiris) }}</strong>
      </div>
      <div class="gun-sonu-satir">
        <span>{{ t('kasa.gunIciCikis') }}</span>
        <strong class="negatif">-{{ formatCurrency(k.gunIciCikis) }}</strong>
      </div>
      <div class="gun-sonu-satir gun-sonu-vurgu">
        <span>{{ t('kasa.beklenenNakit') }}</span>
        <strong>{{ formatCurrency(k.kapanisBakiye) }}</strong>
      </div>
      <div class="gun-sonu-satir">
        <span>{{ t('kasa.nakit') }} ({{ t('kasa.tahsilat') }})</span>
        <strong>{{ formatCurrency(k.nakitTahsilat) }}</strong>
      </div>
      <div class="gun-sonu-satir">
        <span>{{ t('kasa.kart') }} ({{ t('kasa.tahsilat') }})</span>
        <strong>{{ formatCurrency(k.kartTahsilat) }}</strong>
      </div>
      <div class="gun-sonu-satir">
        <span>{{ t('kasa.havale') }} ({{ t('kasa.tahsilat') }})</span>
        <strong>{{ formatCurrency(k.havaleTahsilat) }}</strong>
      </div>
      <div
        v-if="k.taksitTahsilat"
        class="gun-sonu-satir"
      >
        <span>{{ t('kasa.taksit') }} ({{ t('kasa.tahsilat') }})</span>
        <strong>{{ formatCurrency(k.taksitTahsilat) }}</strong>
      </div>
      <div
        v-if="k.digerTahsilat"
        class="gun-sonu-satir"
      >
        <span>{{ t('kasa.diger') }} ({{ t('kasa.tahsilat') }})</span>
        <strong>{{ formatCurrency(k.digerTahsilat) }}</strong>
      </div>
      <div class="gun-sonu-satir">
        <span>{{ t('kasa.giderToplam') }}</span>
        <strong class="negatif">-{{ formatCurrency(k.giderToplam) }}</strong>
      </div>
      <div class="gun-sonu-satir">
        <span>{{ t('kasa.satisAdedi') }} / {{ t('kasa.toplamSatis') }}</span>
        <strong>{{ k.satisAdedi }} / {{ formatCurrency(k.satisToplam) }}</strong>
      </div>
      <div class="gun-sonu-sayim">
        <FormField :label="t('kasa.sayilanNakit')">
          <InputNumber
            v-model="sayilanNakit[k.kasaId]"
            mode="currency"
            currency="TRY"
            locale="tr-TR"
            :min="0"
            class="w-full"
          />
        </FormField>
        <div
          v-if="gunSonuFark(k) !== null"
          class="gun-sonu-satir"
          :class="gunSonuFark(k) === 0 ? 'fark-sifir' : 'fark-var'"
        >
          <span>{{ t('kasa.fark') }}</span>
          <strong>{{ formatCurrency(gunSonuFark(k)) }}</strong>
        </div>
      </div>
      <details
        v-if="k.hareketler && k.hareketler.length"
        class="gun-sonu-detay"
      >
        <summary>{{ t('kasa.hareketDetayi') }} ({{ k.hareketler.length }})</summary>
        <div
          v-for="h in k.hareketler"
          :key="h.id"
          class="gun-sonu-satir gun-sonu-hareket"
        >
          <span class="gun-sonu-aciklama">
            {{ h.aciklama || h.odemeYontemi || h.kaynakTip || '-' }}
          </span>
          <strong :class="h.tur === 'GELIR' ? 'pozitif' : 'negatif'">
            {{ h.tur === 'GELIR' ? '+' : '-' }}{{ formatCurrency(h.tutar) }}
          </strong>
        </div>
      </details>
    </div>
    <template #footer>
      <Button
        :label="t('kasa.kapat')"
        icon="pi pi-times"
        class="p-button-text"
        @click="gunSonuDialog = false"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useKasaStore } from '../stores/kasaStore.js'
import { kasaAPI } from '../api/index.js'
import { formatCurrency, getLocalDateString } from '../utils/format.js'
import FormField from './FormField.vue'

const { t } = useI18n()
const kasaStore = useKasaStore()

const gunSonuDialog = ref(false)
const gunSonuVerisi = ref([])
const gunSonuTarih = ref(new Date())
const gunSonuKasaId = ref(null)
const gunSonuYukleniyor = ref(false)
const sayilanNakit = ref({})

const open = async () => {
  gunSonuTarih.value = new Date()
  gunSonuKasaId.value = null
  sayilanNakit.value = {}
  gunSonuDialog.value = true
  await gunSonuYukle()
}

const gunSonuYukle = async () => {
  gunSonuYukleniyor.value = true
  try {
    const params = { tarih: getLocalDateString(gunSonuTarih.value) }
    if (gunSonuKasaId.value) params.kasaId = gunSonuKasaId.value
    const r = await kasaAPI.gunSonu(params)
    gunSonuVerisi.value = Array.isArray(r.data) ? r.data : []
  } catch {
    gunSonuVerisi.value = []
  } finally {
    gunSonuYukleniyor.value = false
  }
}

const gunSonuFark = (k) => {
  const ham = sayilanNakit.value[k.kasaId]
  if (ham === null || ham === undefined || ham === '') return null
  const sayilan = Number(ham)
  if (Number.isNaN(sayilan)) return null
  return sayilan - (k.kapanisBakiye || 0)
}

defineExpose({ open })
</script>

<style scoped>
.w-full {
  width: 100% !important;
}
.gun-sonu-dialog {
  width: 560px;
  max-width: 94vw;
}
.gun-sonu-filtre {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}
.gun-sonu-filtre > * {
  flex: 1;
}
.gun-sonu-kart {
  display: flex;
  flex-direction: column;
  gap: 2px;
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 12px;
}
.gun-sonu-kart .gun-sonu-alt-baslik {
  font-size: 15px;
  margin-bottom: 6px;
}
.gun-sonu-bos {
  text-align: center;
  color: var(--text-muted);
  padding: 24px 0;
}
.gun-sonu-vurgu strong {
  font-size: 16px;
}
.gun-sonu-sayim {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed var(--border);
}
.gun-sonu-satir.pozitif strong,
.gun-sonu-satir strong.pozitif {
  color: var(--yesil, #16a34a);
}
.gun-sonu-satir.negatif strong,
.gun-sonu-satir strong.negatif {
  color: var(--kirmizi, #dc2626);
}
.gun-sonu-satir.fark-sifir strong {
  color: var(--yesil, #16a34a);
}
.gun-sonu-satir.fark-var strong {
  color: var(--turuncu, #ea580c);
}
.gun-sonu-detay {
  margin-top: 10px;
  font-size: 13px;
}
.gun-sonu-detay summary {
  cursor: pointer;
  color: var(--text-secondary);
  padding: 4px 0;
}
.gun-sonu-hareket {
  padding: 5px 0;
  font-size: 13px;
}
.gun-sonu-aciklama {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 70%;
}
.gun-sonu-satir {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
  font-size: 14px;
}
.gun-sonu-satir span {
  color: var(--text-secondary);
}
.gun-sonu-alt-baslik {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}
</style>
