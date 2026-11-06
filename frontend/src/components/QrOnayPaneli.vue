<template>
  <div
    class="qr-panel"
    data-test="qr-panel"
    role="region"
    :aria-label="t('qrOnay.baslik')"
  >
    <p class="qr-baslik">
      <i
        class="pi pi-mobile"
        aria-hidden="true"
      />
      {{ t('qrOnay.baslik') }}
    </p>
    <p class="qr-aciklama">
      {{ t('qrOnay.aciklama') }}
    </p>

    <div
      v-if="gorselUrl"
      class="qr-gorsel"
    >
      <img
        :src="gorselUrl"
        :alt="t('qrOnay.baslik')"
        width="220"
        height="220"
        data-test="qr-gorsel"
      >
    </div>

    <p
      class="qr-durum"
      role="status"
      aria-live="polite"
      data-test="qr-durum"
    >
      {{ durumMetni }}
    </p>

    <p
      v-if="kalan > 0 && durum === 'BEKLIYOR'"
      class="qr-sure"
    >
      {{ t('qrOnay.kalanSure') }}: {{ kalan }} sn
    </p>

    <p
      v-if="hata"
      class="qr-hata"
      role="alert"
      data-test="qr-hata"
    >
      {{ hata }}
    </p>

    <div class="qr-eylemler">
      <button
        type="button"
        class="gk-baglanti"
        data-test="qr-elle"
        @click="kapat"
      >
        {{ t('qrOnay.elleGir') }}
      </button>
      <button
        type="button"
        class="gk-baglanti"
        data-test="qr-iptal"
        @click="kapat"
      >
        {{ t('qrOnay.iptal') }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { qrOturumAPI } from '../api/index.js'

const props = defineProps({
  girisToken: { type: String, default: '' },
  rememberMe: { type: Boolean, default: false }
})

const emit = defineEmits(['basarili', 'kapat'])
const { t } = useI18n()

const token = ref('')
const gorselUrl = ref('')
const durum = ref('BEKLIYOR')
const kalan = ref(0)
const hata = ref('')
let sayac = null
let poll = null

const durumMetni = computed(() => {
  if (durum.value === 'ONAYLANDI') return t('qrOnay.onaylandi')
  if (durum.value === 'REDDEDILDI') return t('qrOnay.reddedildi')
  if (durum.value === 'SURESI_DOLDU') return t('qrOnay.suresiDoldu')
  return t('qrOnay.bekliyor')
})

const temizle = () => {
  if (sayac) clearInterval(sayac)
  if (poll) clearInterval(poll)
  sayac = null
  poll = null
}

const kapat = () => {
  temizle()
  emit('kapat')
}

const kontrol = async () => {
  if (!token.value) return
  try {
    const cevap = await qrOturumAPI.durum(token.value)
    const deger = cevap.data?.durum
    if (deger === 'ONAYLANDI') {
      temizle()
      durum.value = 'ONAYLANDI'
      const sonuc = await qrOturumAPI.tamamla(props.girisToken, props.rememberMe)
      emit('basarili', sonuc.data)
      return
    }
    if (deger === 'REDDEDILDI') {
      temizle()
      durum.value = 'REDDEDILDI'
      hata.value = t('qrOnay.reddedildi')
      return
    }
    if (deger === 'SURESI_DOLDU') {
      temizle()
      durum.value = 'SURESI_DOLDU'
      hata.value = t('qrOnay.suresiDoldu')
    }
  } catch {
    /* gecici ag hatasinda poll devam eder */
  }
}

onMounted(async () => {
  try {
    const cevap = await qrOturumAPI.baslat(props.girisToken)
    token.value = cevap.data?.token || ''
    gorselUrl.value = token.value ? qrOturumAPI.gorselUrl(token.value) : ''
    kalan.value = Number(cevap.data?.expiresIn) || 120
    sayac = setInterval(() => {
      if (kalan.value > 0) kalan.value -= 1
    }, 1000)
    poll = setInterval(kontrol, 2000)
  } catch (err) {
    hata.value = err.response?.data?.message || t('qrOnay.onayHata')
    durum.value = 'HATA'
  }
})

onBeforeUnmount(temizle)
</script>

<style scoped>
.qr-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 14px 12px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-muted);
  text-align: center;
}

.qr-baslik {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-primary);
}

.qr-aciklama {
  margin: 0;
  max-width: 300px;
  font-size: 12px;
  line-height: 1.45;
  color: var(--text-secondary);
}

.qr-gorsel {
  padding: 8px;
  border-radius: 10px;
  background: #fff;
}

.qr-gorsel img {
  display: block;
  width: 220px;
  height: 220px;
}

.qr-durum {
  margin: 0;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--accent);
}

.qr-sure {
  margin: 0;
  font-size: 11.5px;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}

.qr-hata {
  margin: 0;
  font-size: 12px;
  color: var(--danger);
}

.qr-eylemler {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
}

@media (max-width: 430px) {
  .qr-gorsel img {
    width: 180px;
    height: 180px;
  }
}
</style>
