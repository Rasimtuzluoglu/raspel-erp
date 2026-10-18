<template>
  <Dialog
    v-model:visible="visible"
    :modal="true"
    :style="{ width: '560px', maxWidth: '96vw' }"
  >
    <template #header>
      <div
        class="hareket-baslik"
        :class="giris ? 'giris' : 'cikis'"
      >
        <i :class="giris ? 'pi pi-plus-circle' : 'pi pi-minus-circle'" />
        <span>{{ baslik }}</span>
      </div>
    </template>

    <!-- Yon bandi: giris stogu artirir, cikis azaltir. -->
    <div
      class="hareket-bandi"
      :class="giris ? 'pozitif' : 'negatif'"
    >
      <i :class="giris ? 'pi pi-arrow-up-right' : 'pi pi-arrow-down-right'" />
      <span>{{ giris ? $t('stoklar.girisEtki') : $t('stoklar.cikisEtki') }}</span>
    </div>

    <div class="form-grid-2">
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
        :severity="giris ? 'success' : 'danger'"
        :loading="loading"
        @click="$emit('kaydet')"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useCariOnerileri } from '../composables/useCariOnerileri.js'

const props = defineProps({
  baslik: { type: String, default: '' },
  /** 'GIRIS' | 'CIKIS' — yon bandi ve renk icin. */
  tur: { type: String, default: 'GIRIS' },
  depolar: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

defineEmits(['kaydet'])

const giris = computed(() => props.tur !== 'CIKIS')

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
.hareket-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  font-size: 15px;
}
.hareket-baslik.giris i {
  color: #22c55e;
}
.hareket-baslik.cikis i {
  color: #ef4444;
}
.hareket-bandi {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 16px;
}
.hareket-bandi.pozitif {
  background: var(--success-soft, rgba(34, 197, 94, 0.12));
  border: 1px solid var(--success-border, rgba(34, 197, 94, 0.4));
  color: var(--success, #22c55e);
}
.hareket-bandi.negatif {
  background: var(--danger-soft, rgba(239, 68, 68, 0.12));
  border: 1px solid var(--danger-border, rgba(239, 68, 68, 0.4));
  color: var(--danger, #ef4444);
}
.form-grid-2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
@media (max-width: 480px) {
  .form-grid-2 {
    grid-template-columns: 1fr;
  }
}
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
