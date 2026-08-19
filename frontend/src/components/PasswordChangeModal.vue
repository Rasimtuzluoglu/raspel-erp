<template>
  <Dialog
    :visible="visible"
    header="Şifre Değiştir"
    modal
    :style="{ width: '400px' }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div class="form-grid">
      <div class="field">
        <label>Mevcut Şifre</label>
        <InputText
          v-model="sifreForm.mevcutSifre"
          type="password"
          class="w-full"
        />
      </div>
      <div class="field">
        <label>Yeni Şifre</label>
        <InputText
          v-model="sifreForm.yeniSifre"
          type="password"
          class="w-full"
        />
        <div
          v-if="sifreForm.yeniSifre"
          class="sifre-guc"
        >
          <div class="guc-cubuk">
            <div
              class="guc-dolgu"
              :class="gucSinif"
              :style="{ width: gucYuzde + '%' }"
            />
          </div>
          <span
            class="guc-etiket"
            :class="gucSinif"
          >{{ gucEtiket }}</span>
        </div>
      </div>
      <div class="field">
        <label>Yeni Şifre Tekrar</label>
        <InputText
          v-model="sifreForm.yeniSifreTekrar"
          type="password"
          class="w-full"
        />
      </div>
    </div>
    <template #footer>
      <Button
        label="İptal"
        icon="pi pi-times"
        class="p-button-text"
        @click="$emit('update:visible', false)"
      />
      <Button
        label="Değiştir"
        icon="pi pi-check"
        :loading="sifreDegistiriliyor"
        @click="sifreDegistir"
      />
    </template>
  </Dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { kullaniciAPI } from '../api/index.js'

defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible'])

const toastBildirim = useToastBildirim()
const sifreDegistiriliyor = ref(false)
const sifreForm = ref({ mevcutSifre: '', yeniSifre: '', yeniSifreTekrar: '' })

const gucHesapla = (sifre) => {
  let puan = 0
  if (!sifre) return 0
  if (sifre.length >= 8) puan += 25
  else if (sifre.length >= 5) puan += 15
  if (/[a-z]/.test(sifre) && /[A-Z]/.test(sifre)) puan += 25
  else if (/[a-zA-Z]/.test(sifre)) puan += 10
  if (/\d/.test(sifre)) puan += 25
  if (/[^a-zA-Z0-9]/.test(sifre)) puan += 25
  return Math.min(100, puan)
}

const gucYuzde = computed(() => gucHesapla(sifreForm.value.yeniSifre))

const gucSinif = computed(() => {
  const p = gucYuzde.value
  if (p < 25) return 'zayif'
  if (p < 50) return 'orta'
  if (p < 80) return 'iyi'
  return 'guclu'
})

const gucEtiket = computed(() => {
  const p = gucYuzde.value
  if (!sifreForm.value.yeniSifre) return ''
  if (p < 25) return 'Zayıf'
  if (p < 50) return 'Orta'
  if (p < 80) return 'İyi'
  return 'Güçlü'
})

const sifreDegistir = async () => {
  if (!sifreForm.value.yeniSifre || sifreForm.value.yeniSifre.length < 3) {
    toastBildirim.uyari('Yeni şifre en az 3 karakter olmalıdır')
    return
  }
  if (sifreForm.value.yeniSifre !== sifreForm.value.yeniSifreTekrar) {
    toastBildirim.uyari('Yeni şifreler eşleşmiyor')
    return
  }
  sifreDegistiriliyor.value = true
  try {
    await kullaniciAPI.sifreDegistir(sifreForm.value)
    toastBildirim.basarili('Şifreniz değiştirildi')
    sifreForm.value = { mevcutSifre: '', yeniSifre: '', yeniSifreTekrar: '' }
    emit('update:visible', false)
  } catch (e) {
    toastBildirim.hata(e.response?.data?.message || 'Şifre değiştirilemedi')
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
.sifre-guc {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.guc-cubuk {
  flex: 1;
  height: 6px;
  background: rgba(148, 163, 184, 0.15);
  border-radius: 3px;
  overflow: hidden;
}
.guc-dolgu {
  height: 100%;
  border-radius: 3px;
  transition:
    width 0.3s ease,
    background 0.3s ease;
}
.guc-dolgu.zayif {
  background: #ef4444;
}
.guc-dolgu.orta {
  background: #f59e0b;
}
.guc-dolgu.iyi {
  background: #3b82f6;
}
.guc-dolgu.guclu {
  background: #22c55e;
}
.guc-etiket {
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}
.guc-etiket.zayif {
  color: #ef4444;
}
.guc-etiket.orta {
  color: #f59e0b;
}
.guc-etiket.iyi {
  color: #60a5fa;
}
.guc-etiket.guclu {
  color: #4ade80;
}
</style>
