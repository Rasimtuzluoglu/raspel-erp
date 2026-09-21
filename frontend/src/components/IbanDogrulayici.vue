<template>
  <Dialog
    :visible="visible"
    :header="t('ibanDogrula.baslik')"
    :modal="false"
    :style="{ width: '380px' }"
    :draggable="false"
    @update:visible="$emit('update:visible', $event)"
  >
    <div class="iban-form">
      <div class="form-satir">
        <label>{{ t('ibanDogrula.iban') }}</label>
        <div class="iban-input-group">
          <InputText
            v-model="iban"
            placeholder="TR00 0000 0000 0000 0000 0000 00"
            class="w-full"
            @input="kontrol"
          />
        </div>
      </div>
      <div
        v-if="sonuc"
        class="iban-sonuc"
        :class="sonuc.gecerli ? 'gecerli' : 'gecersiz'"
      >
        <i :class="sonuc.gecerli ? 'pi pi-check-circle' : 'pi pi-times-circle'" />
        <div>
          <strong>{{ sonuc.gecerli ? t('ibanDogrula.gecerli') : t('ibanDogrula.gecersiz') }}</strong>
          <p v-if="sonuc.gecerli">
            {{ sonuc.ulke }} • {{ t('ibanDogrula.banka') }}: {{ sonuc.banka }} • {{ t('ibanDogrula.sube') }}: {{ sonuc.sube }} • {{ t('ibanDogrula.hesap') }}: {{ sonuc.hesap }}
          </p>
        </div>
      </div>
    </div>
  </Dialog>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
defineProps({ visible: Boolean })
defineEmits(['update:visible'])

const { t } = useI18n()
const iban = ref('')
const sonuc = ref(null)

const kontrol = () => {
  const temiz = iban.value.replace(/\s/g, '').toUpperCase()
  if (temiz.length < 5) {
    sonuc.value = null
    return
  }
  if (!temiz.startsWith('TR') || temiz.length !== 26) {
    sonuc.value = { gecerli: false }
    return
  }
  const tasindi = temiz.slice(4) + temiz.slice(0, 4)
  const sayisal = tasindi.replace(/[A-Z]/g, (c) => String(c.charCodeAt(0) - 55))
  try {
    const mod = BigInt(sayisal) % 97n
    sonuc.value =
      mod === 1n
        ? {
            gecerli: true,
            ulke: 'Turkiye',
            banka: temiz.slice(4, 9),
            sube: temiz.slice(9, 14),
            hesap: temiz.slice(14)
          }
        : { gecerli: false }
  } catch {
    sonuc.value = { gecerli: false }
  }
}
</script>

<style scoped>
.iban-form {
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
.iban-sonuc {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  border-radius: 8px;
  font-size: 14px;
}
.iban-sonuc i {
  font-size: 24px;
}
.iban-sonuc p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--text-secondary);
}
.gecerli {
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
}
.gecersiz {
  background: rgba(239, 68, 68, 0.12);
  color: #ef4444;
}
</style>
