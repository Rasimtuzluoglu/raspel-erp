<template>
  <div class="cari-ozel-fiyatlar">
    <div class="cari-not-baslik">
      <h4>
        <i
          class="pi pi-tag"
          style="margin-right: 6px"
        />{{ t('cariHesaplar.ozelFiyatlar') }}
      </h4>
    </div>
    <div
      v-for="f in ozelFiyatlar"
      :key="f.id"
      class="ozel-fiyat-satir"
    >
      <span class="ozel-fiyat-urun">{{ f.stokAd }}</span>
      <span class="ozel-fiyat-tutar">{{ formatCurrency(f.fiyat) }}</span>
      <Button
        icon="pi pi-trash"
        :aria-label="t('common.delete')"
        class="p-button-rounded p-button-text p-button-danger p-button-sm"
        @click="emit('sil', f)"
      />
    </div>
    <div class="ozel-fiyat-ekle">
      <!-- Stok araması sunucu tarafında (1000 kayıt sınırı yok). -->
      <Dropdown
        v-model="stokId"
        :options="stokOnerileri"
        option-label="ad"
        option-value="id"
        :placeholder="t('cariHesaplar.urunSec')"
        filter
        filter-by="ad,stokKodu,barkod"
        class="ozel-fiyat-stok-select"
        @filter="stokAra"
      />
      <InputNumber
        v-model="tutar"
        mode="currency"
        currency="TRY"
        locale="tr-TR"
        :min-fraction-digits="2"
        :placeholder="t('cariHesaplar.fiyat')"
        class="ozel-fiyat-tutar-input"
      />
      <Button
        icon="pi pi-plus"
        :label="t('cariHesaplar.ekle')"
        size="small"
        @click="ekle"
      />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { unwrapList } from '../../api/utils/unwrap.js'
import { stokAPI } from '../../api/index.js'
import { formatCurrency } from '../../utils/format.js'

/** Cari 360 kartı > Özel Fiyatlar sekmesi. */
defineProps({
  ozelFiyatlar: { type: Array, default: () => [] }
})

const emit = defineEmits(['ekle', 'sil'])
const { t } = useI18n()

const stokId = ref(null)
const tutar = ref(null)
const stokOnerileri = ref([])
let zamanlayici = null
let sira = 0

const stokAra = (event) => {
  const q = (event?.filter ?? event?.query ?? '').toString().trim()
  const benim = ++sira
  if (zamanlayici) clearTimeout(zamanlayici)
  zamanlayici = setTimeout(async () => {
    try {
      const r = await stokAPI.ara(q)
      if (benim !== sira) return
      stokOnerileri.value = unwrapList(r)
    } catch {
      if (benim === sira) stokOnerileri.value = []
    }
  }, 250)
}

const ekle = () => {
  if (!stokId.value || !tutar.value) return
  emit('ekle', { stokId: stokId.value, tutar: tutar.value })
  stokId.value = null
  tutar.value = null
}
</script>

<style scoped>
.cari-ozel-fiyatlar {
  margin-top: 20px;
  border-top: 1px solid var(--border);
  padding-top: 14px;
}

.cari-not-baslik h4 {
  margin: 0 0 10px;
  font-size: 14px;
}

.ozel-fiyat-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
  border-bottom: 1px solid var(--border);
}

.ozel-fiyat-urun {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
}

.ozel-fiyat-tutar {
  font-size: 13px;
  font-weight: 700;
  color: var(--accent);
}

.ozel-fiyat-ekle {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  align-items: center;
}

.ozel-fiyat-stok-select {
  flex: 1;
}

.ozel-fiyat-tutar-input {
  width: 140px;
}
</style>
