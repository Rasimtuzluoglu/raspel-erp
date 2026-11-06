<template>
  <div class="qr-onay-sayfasi">
    <main
      class="qr-onay-kart"
      data-test="qr-onay-sayfa"
    >
      <div class="qr-onay-marka">
        <img
          :src="markaLogo"
          alt="RasPel"
          width="42"
          height="42"
        >
        <div>
          <span class="qr-onay-ad">RasPel ERP</span>
          <span class="qr-onay-alt">{{ t('qrOnay.sayfaAlt') }}</span>
        </div>
      </div>

      <h1>{{ t('qrOnay.sayfaBaslik') }}</h1>

      <div
        v-if="durum === 'BEKLIYOR'"
        class="qr-onay-icerik"
      >
        <div class="qr-onay-ikon">
          <i
            class="pi pi-desktop"
            aria-hidden="true"
          />
        </div>
        <p class="qr-onay-baslik">
          {{ t('qrOnay.girisIstegi') }}
        </p>
        <p class="qr-onay-metin">
          {{ t('qrOnay.girisIstegiAlt') }}
        </p>
        <div class="qr-onay-eylemler">
          <Button
            :label="t('qrOnay.onayla')"
            icon="pi pi-check"
            :loading="islemde"
            data-test="qr-onayla"
            @click="onayla"
          />
          <Button
            :label="t('qrOnay.reddet')"
            icon="pi pi-times"
            severity="secondary"
            outlined
            :disabled="islemde"
            data-test="qr-reddet"
            @click="reddet"
          />
        </div>
      </div>

      <div
        v-else
        class="qr-onay-icerik"
        data-test="qr-onay-durum"
      >
        <div
          class="qr-onay-ikon"
          :class="basarili ? 'qr-onay-ikon--ok' : 'qr-onay-ikon--hata'"
        >
          <i
            class="pi"
            :class="basarili ? 'pi-check-circle' : 'pi-times-circle'"
            aria-hidden="true"
          />
        </div>
        <p class="qr-onay-baslik">
          {{ basarili ? t('qrOnay.onayBasarili') : mesaj }}
        </p>
        <Button
          v-if="basarili"
          :label="t('nav.dashboard')"
          icon="pi pi-arrow-right"
          data-test="qr-panele-git"
          @click="router.push('/')"
        />
      </div>

      <p
        v-if="hata"
        class="qr-onay-hata"
        role="alert"
      >
        {{ hata }}
      </p>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { qrOturumAPI } from '../api/index.js'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const markaLogo = '/logo-icon.png'

const token = computed(() => String(route.params.token || ''))
const durum = ref('YUKLENIYOR')
const islemde = ref(false)
const hata = ref('')
const mesaj = ref('')

const basarili = computed(() => durum.value === 'ONAYLANDI')

const yukle = async () => {
  try {
    const cevap = await qrOturumAPI.durum(token.value)
    const deger = cevap.data?.durum || 'SURESI_DOLDU'
    if (deger === 'SURESI_DOLDU') {
      durum.value = 'SURESI_DOLDU'
      mesaj.value = t('qrOnay.suresiDoldu')
      return
    }
    if (deger === 'REDDEDILDI') {
      durum.value = 'REDDEDILDI'
      mesaj.value = t('qrOnay.reddedildi')
      return
    }
    durum.value = deger
  } catch (err) {
    durum.value = 'HATA'
    mesaj.value = err.response?.data?.message || t('qrOnay.gecersiz')
  }
}

const onayla = async () => {
  hata.value = ''
  islemde.value = true
  try {
    await qrOturumAPI.onayla(token.value)
    durum.value = 'ONAYLANDI'
  } catch (err) {
    hata.value = err.response?.data?.message || t('qrOnay.onayHata')
  } finally {
    islemde.value = false
  }
}

const reddet = async () => {
  hata.value = ''
  islemde.value = true
  try {
    await qrOturumAPI.reddet(token.value)
    durum.value = 'REDDEDILDI'
    mesaj.value = t('qrOnay.reddedildi')
  } catch (err) {
    hata.value = err.response?.data?.message || t('qrOnay.onayHata')
  } finally {
    islemde.value = false
  }
}

onMounted(yukle)
</script>

<style scoped>
.qr-onay-sayfasi {
  min-height: 100dvh;
  display: grid;
  place-items: center;
  padding: 24px 16px calc(24px + env(safe-area-inset-bottom));
  background:
    radial-gradient(60% 50% at 50% 0%, rgba(20, 184, 166, 0.12), transparent 70%),
    linear-gradient(180deg, #070b10 0%, #0b1219 100%);
  color: #e8eef5;
}

.qr-onay-kart {
  width: 100%;
  max-width: 420px;
  padding: 24px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 18px;
  background: rgba(16, 24, 33, 0.82);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.45);
}

.qr-onay-marka {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.qr-onay-marka img {
  border-radius: 10px;
}

.qr-onay-ad {
  display: block;
  font-size: 14px;
  font-weight: 800;
}

.qr-onay-alt {
  display: block;
  margin-top: 2px;
  font-size: 11.5px;
  color: #9fb0c0;
}

h1 {
  margin: 0 0 18px;
  font-size: 24px;
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.qr-onay-icerik {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  text-align: center;
}

.qr-onay-ikon {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: rgba(20, 184, 166, 0.12);
  color: #2dd4bf;
  font-size: 24px;
}

.qr-onay-ikon--ok {
  background: rgba(34, 197, 94, 0.14);
  color: #22c55e;
}

.qr-onay-ikon--hata {
  background: rgba(239, 68, 68, 0.14);
  color: #ef4444;
}

.qr-onay-baslik {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
}

.qr-onay-metin {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: #9fb0c0;
}

.qr-onay-eylemler {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  margin-top: 8px;
}

.qr-onay-hata {
  margin: 12px 0 0;
  font-size: 12.5px;
  color: #f87171;
  text-align: center;
}
</style>
