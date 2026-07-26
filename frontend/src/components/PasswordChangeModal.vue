<template>
  <Dialog :visible="visible" @update:visible="$emit('update:visible', $event)" header="Şifre Değiştir" modal :style="{ width: '400px' }">
    <div class="form-grid">
      <div class="field">
        <label>Mevcut Şifre</label>
        <InputText v-model="sifreForm.mevcutSifre" type="password" class="w-full" />
      </div>
      <div class="field">
        <label>Yeni Şifre</label>
        <InputText v-model="sifreForm.yeniSifre" type="password" class="w-full" />
      </div>
      <div class="field">
        <label>Yeni Şifre Tekrar</label>
        <InputText v-model="sifreForm.yeniSifreTekrar" type="password" class="w-full" />
      </div>
    </div>
    <template #footer>
      <Button label="İptal" icon="pi pi-times" class="p-button-text" @click="$emit('update:visible', false)" />
      <Button label="Değiştir" icon="pi pi-check" @click="sifreDegistir" :loading="sifreDegistiriliyor" />
    </template>
  </Dialog>
</template>

<script setup>
import { ref } from 'vue'
import { useToast } from 'primevue/usetoast'
import { kullaniciAPI } from '../api/index.js'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible'])

const toast = useToast()
const sifreDegistiriliyor = ref(false)
const sifreForm = ref({ mevcutSifre: '', yeniSifre: '', yeniSifreTekrar: '' })

const sifreDegistir = async () => {
  if (!sifreForm.value.yeniSifre || sifreForm.value.yeniSifre.length < 3) {
    toast.add({ severity: 'warn', summary: 'Uyarı', detail: 'Yeni şifre en az 3 karakter olmalıdır', life: 5000 })
    return
  }
  if (sifreForm.value.yeniSifre !== sifreForm.value.yeniSifreTekrar) {
    toast.add({ severity: 'warn', summary: 'Uyarı', detail: 'Yeni şifreler eşleşmiyor', life: 5000 })
    return
  }
  sifreDegistiriliyor.value = true
  try {
    await kullaniciAPI.sifreDegistir(sifreForm.value)
    toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Şifreniz değiştirildi', life: 5000 })
    sifreForm.value = { mevcutSifre: '', yeniSifre: '', yeniSifreTekrar: '' }
    emit('update:visible', false)
  } catch (e) {
    toast.add({ severity: 'error', summary: 'Hata', detail: e.response?.data?.message || 'Şifre değiştirilemedi', life: 5000 })
  } finally {
    sifreDegistiriliyor.value = false
  }
}
</script>

<style scoped>
.field {
  margin-bottom: 1rem;
}
.field label {
  display: block;
  font-weight: 500;
  margin-bottom: 0.35rem;
  font-size: 0.875rem;
}
.w-full {
  width: 100%;
}
</style>
