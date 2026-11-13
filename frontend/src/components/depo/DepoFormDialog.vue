<template>
  <Dialog
    :visible="visible"
    :header="depo?.id ? t('depolar.depoDuzenle') : t('depolar.yeniDepo')"
    modal
    :style="{ width: 'min(92vw, 460px)' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div class="depo-form-grid">
      <div class="field">
        <label>{{ t('depolar.depoAdiZorunlu') }}</label>
        <InputText
          v-model="form.ad"
          class="w-full"
          maxlength="200"
        />
      </div>
      <div class="field">
        <label>{{ t('depolar.bagliSubeZorunlu') }}</label>
        <Dropdown
          v-model="form.subeId"
          :options="subeler"
          option-label="ad"
          option-value="id"
          :placeholder="t('depolar.subeSec')"
          class="w-full"
        />
      </div>
      <div class="field">
        <label>{{ t('depolar.sorumlu') }}</label>
        <InputText
          v-model="form.yetkili"
          class="w-full"
          maxlength="100"
        />
      </div>
      <div class="field">
        <label>{{ t('depolar.sorumluKullanici') }}</label>
        <Dropdown
          v-model="form.sorumluKullaniciId"
          :options="kullanicilar"
          option-label="displayName"
          option-value="id"
          show-clear
          filter
          :placeholder="t('depolar.sorumluSec')"
          class="w-full"
        />
      </div>
      <div class="field">
        <label>{{ t('depolar.kapasite') }}</label>
        <InputNumber
          v-model="form.kapasiteKalem"
          :min="0"
          :use-grouping="false"
          class="w-full"
        />
        <small>{{ t('depolar.kapasiteIpucu') }}</small>
      </div>
      <div class="field">
        <label>{{ t('depolar.adres') }}</label>
        <Textarea
          v-model="form.adres"
          rows="2"
          maxlength="500"
          class="w-full"
        />
      </div>
      <div
        v-if="depo?.id"
        class="field"
      >
        <label>{{ t('status.active') }}</label>
        <InputSwitch v-model="form.aktif" />
      </div>
    </div>
    <template #footer>
      <Button
        :label="t('common.cancel')"
        icon="pi pi-times"
        class="p-button-text"
        @click="$emit('update:visible', false)"
      />
      <Button
        :label="t('common.save')"
        icon="pi pi-check"
        :loading="kaydediliyor"
        :disabled="!form.ad?.trim() || !form.subeId"
        @click="$emit('kaydet', { ...form })"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  visible: { type: Boolean, default: false },
  depo: { type: Object, default: null },
  subeler: { type: Array, default: () => [] },
  kullanicilar: { type: Array, default: () => [] },
  kaydediliyor: { type: Boolean, default: false }
})
defineEmits(['update:visible', 'kaydet'])

const { t } = useI18n()
const bosForm = () => ({
  ad: '', subeId: null, yetkili: '', sorumluKullaniciId: null,
  kapasiteKalem: null, adres: '', aktif: true
})
const form = ref(bosForm())

watch(
  () => [props.visible, props.depo],
  () => {
    if (!props.visible) return
    form.value = props.depo ? { ...bosForm(), ...props.depo } : bosForm()
  },
  { immediate: true }
)
</script>

<style scoped>
.depo-form-grid { display: flex; flex-direction: column; gap: 10px; }
.field { display: flex; flex-direction: column; gap: 4px; }
.field label { font-size: 12px; font-weight: 600; color: var(--text-secondary); }
.field small { color: var(--text-muted); font-size: 11.5px; }
/* Diyalog formu kompakt: 48px inputlar 6 alanli formu gereksiz sisiriyordu. */
.field :deep(.p-inputtext),
.field :deep(.p-select),
.field :deep(.p-inputnumber-input),
.field :deep(.p-textarea) {
  padding: 0.5rem 0.7rem;
  font-size: 0.875rem;
}
.w-full { width: 100%; }
</style>
