<template>
  <Dialog
    :visible="visible"
    modal
    :header="$t('cariHesaplar.borclandirma')"
    :style="{ width: '480px', maxWidth: 'calc(100vw - 24px)' }"
    :dismissable-mask="false"
    @update:visible="$emit('update:visible', $event)"
  >
    <div class="borclandirma-form">
      <div class="form-panel">
        <div class="form-section-title">
          <i class="pi pi-user" /> {{ $t('cariHesaplar.cari') }}
        </div>
        <FormField :label="$t('cariHesaplar.cari')">
          <Select
            v-model="form.cariHesapId"
            :options="efektifCariler"
            option-label="ad"
            option-value="id"
            :filter="true"
            :disabled="!!props.cari"
            :placeholder="$t('cariHesaplar.cariSecin')"
            class="w-full"
          />
        </FormField>
      </div>

      <div class="form-panel">
        <div class="form-section-title">
          <i class="pi pi-plus-circle" /> {{ $t('cariHesaplar.borclandirmaBilgisi') }}
        </div>
        <div class="form-grid-2">
          <FormField :label="$t('cariHesaplar.borcTutari')">
            <InputNumber
              v-model="form.tutar"
              :min="0"
              :min-fraction-digits="2"
              :max-fraction-digits="2"
              :disabled="!form.cariHesapId"
              :placeholder="$t('common.amount')"
              class="w-full"
            />
          </FormField>
          <FormField :label="$t('cariHesaplar.borcTarihi')">
            <DatePicker
              v-model="form.hareketTarihi"
              show-icon
              date-format="dd/mm/yy"
              class="w-full"
            />
          </FormField>
        </div>
        <FormField :label="$t('common.description')">
          <Textarea
            v-model="form.aciklama"
            rows="2"
            :placeholder="$t('cariHesaplar.borclandirmaNotPlaceholder')"
            class="w-full"
          />
        </FormField>
        <Message
          severity="info"
          :closable="false"
          class="bilgi-notu"
        >
          {{ $t('cariHesaplar.borclandirmaAciklama') }}
        </Message>
      </div>
    </div>

    <template #footer>
      <Button
        :label="$t('common.cancel')"
        icon="pi pi-times"
        class="p-button-text"
        @click="$emit('update:visible', false)"
      />
      <Button
        :label="$t('common.save')"
        icon="pi pi-check"
        class="p-button-success"
        :loading="kaydediliyor"
        :disabled="!gecerli"
        @click="kaydet"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { hareketAPI } from '../api/index.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import FormField from './FormField.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  /** Cari detayından geçilirse tutar için kullanılır (cari seçimi sabitlenir) */
  cari: { type: Object, default: null },
  /** Cari listesi (opsiyonel) */
  cariler: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:visible', 'kaydedildi'])

const { t } = useI18n()
const toastBildirim = useToastBildirim()

const efektifCariler = computed(() => {
  const liste = props.cariler.map((c) => ({ ...c, ad: c.ad || c.cariAd }))
  if (props.cari && !liste.find((c) => c.id === props.cari.id)) {
    liste.unshift(props.cari)
  }
  return liste
})

const form = ref({
  cariHesapId: props.cari?.id || null,
  tutar: null,
  hareketTarihi: new Date(),
  aciklama: ''
})

const kaydediliyor = ref(false)

const gecerli = computed(() => !!(form.value.cariHesapId && form.value.tutar && form.value.tutar > 0))

const temizle = () => {
  form.value = {
    cariHesapId: props.cari?.id || null,
    tutar: null,
    hareketTarihi: new Date(),
    aciklama: ''
  }
}

watch(
  () => props.visible,
  (open) => {
    if (open) {
      temizle()
    }
  }
)

const kaydet = async () => {
  if (!gecerli.value) return
  kaydediliyor.value = true
  try {
    await hareketAPI.create({
      cariHesapId: form.value.cariHesapId,
      tur: 'BORC',
      tutar: form.value.tutar,
      hareketTarihi: form.value.hareketTarihi ? form.value.hareketTarihi.toISOString().slice(0, 10) : null,
      aciklama: form.value.aciklama || null
    })
    toastBildirim.basarili(t('cariHesaplar.borclandirmaKaydedildi'))
    emit('kaydedildi')
    emit('update:visible', false)
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('cariHesaplar.borclandirmaKaydedilemedi'))
  } finally {
    kaydediliyor.value = false
  }
}
</script>

<style scoped>
.borclandirma-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.form-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.form-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  color: var(--text-secondary);
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}
.form-section-title i {
  color: var(--accent);
}
.form-grid-2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.w-full {
  width: 100%;
}
.bilgi-notu {
  font-size: 12px;
}
@media (max-width: 480px) {
  .form-grid-2 {
    grid-template-columns: 1fr;
  }
}
</style>
