<template>
  <div class="sifre-sifirla-sayfa">
    <Card class="sifirla-kart">
      <template #title>
        <div class="kart-baslik">
          <i class="pi pi-lock" />
          <span>{{ t('giris.resetTitle') }}</span>
        </div>
      </template>
      <template #content>
        <div
          v-if="!basarili"
          class="sifirla-icerik"
        >
          <p class="aciklama">
            {{ t('giris.resetHint') }}
          </p>
          <div class="form-grup">
            <label for="yeniSifre">{{ t('giris.newPassword') }}</label>
            <Password
              v-model="yeniSifre"
              input-id="yeniSifre"
              :feedback="true"
              toggle-mask
              class="w-full"
              :disabled="gonderiliyor"
            />
          </div>
          <div class="form-grup">
            <label for="yeniSifreTekrar">{{ t('giris.newPasswordRepeat') }}</label>
            <Password
              v-model="yeniSifreTekrar"
              input-id="yeniSifreTekrar"
              :feedback="false"
              toggle-mask
              class="w-full"
              :disabled="gonderiliyor"
              @keyup.enter="kaydet"
            />
          </div>
          <Message
            v-if="hata"
            severity="error"
            :closable="false"
          >
            {{ hata }}
          </Message>
          <Button
            :label="t('giris.resetSubmit')"
            icon="pi pi-check"
            class="w-full"
            :loading="gonderiliyor"
            @click="kaydet"
          />
        </div>
        <div
          v-else
          class="sifirla-basari"
        >
          <i class="pi pi-check-circle basari-ikon" />
          <p>{{ t('giris.resetSuccess') }}</p>
          <Button
            :label="t('giris.backToLogin')"
            icon="pi pi-sign-in"
            class="w-full"
            @click="giriseDon"
          />
        </div>
      </template>
    </Card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { kullaniciAPI } from '../api/index.js'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const yeniSifre = ref('')
const yeniSifreTekrar = ref('')
const gonderiliyor = ref(false)
const hata = ref('')
const basarili = ref(false)

const giriseDon = () => router.push('/giris')

const kaydet = async () => {
  hata.value = ''
  const token = route.query.token
  if (!token) {
    hata.value = t('giris.resetTokenMissing')
    return
  }
  if (!yeniSifre.value || yeniSifre.value.length < 8) {
    hata.value = t('giris.resetPasswordTooShort')
    return
  }
  if (yeniSifre.value !== yeniSifreTekrar.value) {
    hata.value = t('giris.resetPasswordMismatch')
    return
  }
  gonderiliyor.value = true
  try {
    await kullaniciAPI.sifreSifirlamaOnayla({ token, yeniSifre: yeniSifre.value })
    basarili.value = true
  } catch (err) {
    hata.value = err.response?.data?.message || t('giris.resetFailed')
  } finally {
    gonderiliyor.value = false
  }
}
</script>

<style scoped>
.sifre-sifirla-sayfa {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--bg-body, #0b0f14);
}
.sifirla-kart {
  width: 100%;
  max-width: 420px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
}
.kart-baslik {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  color: var(--text-primary);
}
.aciklama {
  color: var(--text-muted);
  font-size: 14px;
  margin-bottom: 20px;
}
.form-grup {
  margin-bottom: 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.form-grup label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
}
.w-full {
  width: 100%;
}
.sifirla-basari {
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.basari-ikon {
  font-size: 48px;
  color: var(--accent, #10b981);
}
</style>
