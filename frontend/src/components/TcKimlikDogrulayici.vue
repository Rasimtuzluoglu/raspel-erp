<template>
  <Dialog
    :visible="visible"
    header="TC Kimlik Dogrulayici"
    :modal="false"
    :style="{ width: '320px' }"
    :draggable="false"
    @update:visible="$emit('update:visible', $event)"
  >
    <div class="tc-form">
      <div class="form-satir">
        <label>TC Kimlik No</label>
        <InputText
          v-model="tc"
          placeholder="12345678901"
          maxlength="11"
          class="w-full"
          @input="kontrol"
        />
      </div>
      <div
        v-if="sonuc !== null"
        class="tc-sonuc"
        :class="sonuc ? 'gecerli' : 'gecersiz'"
      >
        <i :class="sonuc ? 'pi pi-check-circle' : 'pi pi-times-circle'" />
        <strong>{{ sonuc ? 'Gecerli TC Kimlik No' : 'Gecersiz TC Kimlik No' }}</strong>
      </div>
      <p class="tc-uyari">
        * Algoritmik dogrulama yapilir. Resmi dogrulama icin NVI servisleri kullanilmalidir.
      </p>
    </div>
  </Dialog>
</template>

<script setup>
import { ref } from 'vue'
defineProps({ visible: Boolean })
defineEmits(['update:visible'])

const tc = ref('')
const sonuc = ref(null)

const kontrol = () => {
  const val = tc.value.replace(/\D/g, '')
  if (val.length !== 11 || val[0] === '0') {
    sonuc.value = false
    return
  }
  const digits = val.split('').map(Number)
  const tek = digits[0] + digits[2] + digits[4] + digits[6] + digits[8]
  const cift = digits[1] + digits[3] + digits[5] + digits[7]
  const h10 = (tek * 7 - cift) % 10
  const h11 = digits.slice(0, 10).reduce((s, d) => s + d, 0) % 10
  sonuc.value = h10 === digits[9] && h11 === digits[10]
}
</script>

<style scoped>
.tc-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.form-satir label {
  display: block;
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}
.tc-sonuc {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px;
  border-radius: 8px;
  font-size: 14px;
}
.tc-sonuc i {
  font-size: 24px;
}
.gecerli {
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
}
.gecersiz {
  background: rgba(239, 68, 68, 0.12);
  color: #ef4444;
}
.tc-uyari {
  font-size: 11px;
  color: var(--text-muted);
  margin: 0;
}
</style>
