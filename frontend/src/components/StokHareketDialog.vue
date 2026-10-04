<template>
  <Dialog
    v-model:visible="visible"
    :header="baslik"
    :modal="true"
    style="width: 500px"
  >
    <div class="form-grup">
      <label>{{ $t('common.quantity') }} *</label>
      <InputNumber
        v-model="miktar"
        :min="0.01"
        :min-fraction-digits="1"
        class="w-full"
      />
    </div>
    <div class="form-grup">
      <label>{{ $t('common.date') }} *</label>
      <DatePicker
        v-model="hareketTarihi"
        date-format="dd.mm.yy"
        class="w-full"
      />
    </div>
    <div class="form-grup">
      <label>{{ $t('nav.cari') }}</label>
      <Dropdown
        v-model="cariHesapId"
        :options="cariOnerileri"
        option-label="ad"
        option-value="id"
        :placeholder="$t('common.optional')"
        class="w-full"
        show-clear
        filter
        filter-by="ad,vergiNumarasi,telefon"
        :loading="cariOnerileriYukleniyor"
        @filter="cariAra"
      />
    </div>
    <div class="form-grup">
      <label>{{ $t('stoklar.hareketDepo') }}</label>
      <Dropdown
        v-model="depoId"
        :options="depolar"
        option-label="ad"
        option-value="id"
        :placeholder="$t('stoklar.depoSecin')"
        show-clear
        class="w-full"
      />
    </div>
    <div class="form-grup">
      <label>{{ $t('common.description') }}</label>
      <Textarea
        v-model="aciklama"
        rows="2"
        class="w-full"
      />
    </div>
    <template #footer>
      <Button
        :label="$t('common.cancel')"
        icon="pi pi-times"
        class="p-button-text"
        @click="visible = false"
      />
      <Button
        :label="$t('common.save')"
        icon="pi pi-check"
        :loading="loading"
        @click="$emit('kaydet')"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { watch } from 'vue'
import { useCariOnerileri } from '../composables/useCariOnerileri.js'

defineProps({
  baslik: { type: String, default: '' },
  depolar: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

defineEmits(['kaydet'])

// Cari secici sunucu aramali. Once `Stoklar.vue` 50 kayitlik listeyi prop ile
// geciyordu; 50. kayittan sonraki cari stok hareketine atanamadan kayboluyordu.
const { oneriler: cariOnerileri, ara: cariAra, hemenAra: cariOnerileriYukle, yukleniyor: cariOnerileriYukleniyor } =
  useCariOnerileri()

const visible = defineModel('visible', { type: Boolean, default: false })
const miktar = defineModel('miktar', { type: Number, default: null })
const hareketTarihi = defineModel('hareketTarihi', { type: [Date, String], default: null })
const cariHesapId = defineModel('cariHesapId', { type: [Number, String], default: null })
const depoId = defineModel('depoId', { type: [Number, String], default: null })
const aciklama = defineModel('aciklama', { type: String, default: '' })

// Dialog her acildiginda ilk sayfa yuklenir; dropdown filtresiz birakilirsa
// ilk kullanimda bos gorunur.
watch(
  visible,
  (acik) => {
    if (acik) cariOnerileriYukle()
  },
  { immediate: true }
)
</script>

<style scoped>
.form-grup {
  margin-bottom: 18px;
}
.form-grup label {
  display: block;
  margin-bottom: 6px;
  font-weight: 600;
  color: var(--text-secondary);
  font-size: 12px;
}
</style>
