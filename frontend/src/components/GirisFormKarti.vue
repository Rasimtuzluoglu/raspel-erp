<template>
  <section
    class="giris-kart"
    :class="{ 'giris-kart--kurulum': kurulumAdimi }"
  >
    <header
      v-if="!kurulumAdimi"
      class="gk-ust"
    >
      <div class="gk-marka">
        <img
          :src="logoGoster"
          alt=""
          class="gk-logo"
          width="36"
          height="36"
        >
        <div class="gk-marka-metin">
          <span class="gk-ad">RasPel ERP</span>
          <span
            v-if="sonSirketAd"
            class="gk-son"
          >{{ t('giris.lastCompany') }}: <strong>{{ sonSirketAd }}</strong></span>
        </div>
      </div>
      <h2 class="gk-baslik">
        {{ baslik }}
      </h2>
      <p class="gk-alt">
        {{ altBaslik }}
      </p>
    </header>

    <Transition name="gk-hata">
      <div
        v-if="hata"
        id="giris-hata"
        class="gk-hata"
        role="alert"
      >
        <i
          class="pi pi-exclamation-circle"
          aria-hidden="true"
        />
        <span>{{ hata }}</span>
      </div>
    </Transition>

    <!-- Adım 0: İlk kurulum sihirbazı -->
    <KurulumAdimi
      v-if="kurulumAdimi"
      v-model:ad="kurulumForm.ad"
      v-model:vergi-no="kurulumForm.vergiNo"
      v-model:vergi-dairesi="kurulumForm.vergiDairesi"
      v-model:telefon="kurulumForm.telefon"
      v-model:email="kurulumForm.email"
      v-model:admin-username="kurulumForm.adminUsername"
      v-model:admin-display-name="kurulumForm.adminDisplayName"
      v-model:admin-password="kurulumForm.adminPassword"
      :yukleniyor="kurulumYukleniyor"
      @baslat="kurulumBaslat"
    />

    <!-- Adım 2: 2FA -->
    <form
      v-else-if="ikiFaktorAdimi"
      class="giris-form"
      data-test="iki-faktor-form"
      novalidate
      @submit.prevent="ikiFaktorDogrula"
    >
      <div class="gk-adim-ikon">
        <i
          class="pi pi-shield"
          aria-hidden="true"
        />
      </div>

      <template v-if="!qrPanelAcik">
        <div class="gk-alan">
          <label for="iki-faktor-kod">{{ t('giris.dogrulamaKodu') }}</label>
          <input
            id="iki-faktor-kod"
            ref="ikiFaktorInput"
            v-model="ikiFaktorKod"
            data-test="iki-faktor-kod"
            type="text"
            inputmode="numeric"
            autocomplete="one-time-code"
            maxlength="6"
            class="gk-input gk-input--kod"
            :aria-invalid="kodGecersiz ? 'true' : 'false'"
            :aria-describedby="kodGecersiz ? 'giris-hata' : 'iki-faktor-ipucu'"
            @input="kodGecersiz = false"
          >
          <small
            id="iki-faktor-ipucu"
            class="gk-ipucu"
          >{{ t('giris.twoFactorHint') }}</small>
        </div>

        <button
          type="submit"
          class="gk-buton"
          data-test="iki-faktor-dogrula"
          :disabled="yukleniyor"
        >
          <i
            v-if="yukleniyor"
            class="pi pi-spin pi-spinner"
            aria-hidden="true"
          />
          <span>{{ t('giris.verify') }}</span>
        </button>

        <button
          type="button"
          class="gk-buton gk-buton--ikincil"
          data-test="qr-ile-onayla"
          :disabled="yukleniyor"
          @click="qrPanelAcik = true"
        >
          <i
            class="pi pi-qrcode"
            aria-hidden="true"
          />
          <span>{{ t('qrOnay.baslik') }}</span>
        </button>
      </template>

      <QrOnayPaneli
        v-else
        :giris-token="girisToken"
        :remember-me="beniHatirla"
        @basarili="qrGirisBasarili"
        @kapat="qrPanelAcik = false"
      />

      <button
        v-if="!qrPanelAcik"
        type="button"
        class="gk-baglanti"
        @click="geriDon"
      >
        &larr; {{ t('giris.back') }}
      </button>
    </form>

    <!-- Adım 3: Şirket seçimi -->
    <div
      v-else-if="sirketSecimAdimi"
      class="giris-form"
      data-test="sirket-secim"
    >
      <div class="sirket-listesi">
        <template
          v-for="(grup, grupAdi) in gruplanmisSirketler"
          :key="grupAdi"
        >
          <div
            v-if="Object.keys(gruplanmisSirketler).length > 1"
            class="sirket-grup-baslik"
          >
            <i
              class="pi pi-folder"
              aria-hidden="true"
            />
            <span>{{ grupAdi }}</span>
          </div>
          <button
            v-for="s in grup"
            :key="s.id"
            type="button"
            class="sirket-kart"
            :class="{ 'son-secilen': s.id === sonSecilenSirketId }"
            @click="sirketSecVeGirisYap(s)"
          >
            <span class="sirket-kart-avatar">
              <i
                class="pi pi-building"
                aria-hidden="true"
              />
            </span>
            <span class="sirket-kart-bilgi">
              <span class="sirket-kart-ad">{{ s.ad }}</span>
              <span class="sirket-kart-vkn">VKN: {{ s.vergiNo || '-' }}</span>
            </span>
            <span class="sirket-kart-sag">
              <span
                v-if="s.tur && s.tur !== 'DIGER'"
                class="sirket-tur-badge"
                :class="'tur-' + (s.tur || '').toLowerCase()"
              >
                {{ s.tur === 'RESMI' ? t('sirketler.resmi') : s.tur === 'GAYRIRESMI' ? t('sirketler.gayriresmi') : s.tur }}
              </span>
              <span
                v-if="s.yil"
                class="sirket-yil-badge"
              >{{ s.yil }}</span>
              <i
                v-if="s.id === sonSecilenSirketId"
                class="pi pi-star-fill son-secilen-yildiz"
                aria-hidden="true"
              />
              <i
                class="pi pi-arrow-right sirket-kart-ok"
                aria-hidden="true"
              />
            </span>
          </button>
        </template>
      </div>

      <p
        v-if="sirketler.length === 0"
        class="sirket-yok"
      >
        {{ t('giris.noCompany') }}
      </p>

      <button
        type="button"
        class="gk-baglanti"
        @click="tumAdimlariSifirla"
      >
        &larr; {{ t('giris.loginAgain') }}
      </button>
    </div>

    <!-- Adım 1: Kullanıcı adı / parola -->
    <form
      v-else
      class="giris-form"
      data-test="giris-form"
      novalidate
      @submit.prevent="girisYap"
    >
      <!-- İsteğe bağlı, katlanabilir şirket kodu -->
      <div class="gk-kod">
        <button
          type="button"
          class="gk-kod-ac"
          data-test="sirket-kodu-ac"
          :aria-expanded="sirketKoduAcik ? 'true' : 'false'"
          aria-controls="sirket-kodu-alan"
          @click="sirketKoduAcik = !sirketKoduAcik"
        >
          <i
            class="pi pi-building"
            aria-hidden="true"
          />
          <span>{{ sirketKoduAcik ? t('giris.sirketKoduKaldir') : t('giris.sirketKoduEkle') }}</span>
          <i
            class="pi"
            :class="sirketKoduAcik ? 'pi-chevron-up' : 'pi-chevron-down'"
            aria-hidden="true"
          />
        </button>

        <Transition name="gk-ac">
          <div
            v-if="sirketKoduAcik"
            id="sirket-kodu-alan"
            class="gk-alan gk-alan--kod"
          >
            <label for="sirket-kodu">{{ t('giris.sirketKodu') }}</label>
            <input
              id="sirket-kodu"
              v-model.trim="sirketKodu"
              data-test="sirket-kodu"
              type="text"
              autocomplete="organization"
              spellcheck="false"
              class="gk-input"
              :placeholder="t('giris.sirketKoduPlaceholder')"
              aria-describedby="sirket-kodu-ipucu"
            >
            <small
              id="sirket-kodu-ipucu"
              class="gk-ipucu"
            >{{ t('giris.sirketKoduIpucu') }}</small>
          </div>
        </Transition>
      </div>

      <div class="gk-alan">
        <label for="giris-username">{{ t('auth.username') }}</label>
        <div class="gk-input-sar">
          <i
            class="pi pi-user"
            aria-hidden="true"
          />
          <input
            id="giris-username"
            ref="kullaniciInput"
            v-model="username"
            data-test="giris-username"
            type="text"
            autocomplete="username"
            class="gk-input"
            :aria-invalid="alanHatasi.username ? 'true' : 'false'"
            :aria-describedby="alanHatasi.username ? 'giris-hata' : undefined"
            @keydown="klavyeKontrol"
            @input="alanHatasi.username = false"
          >
        </div>
      </div>

      <div class="gk-alan">
        <div class="gk-etiket-satir">
          <label for="giris-password">{{ t('auth.password') }}</label>
          <span
            v-if="capsLockAcik"
            id="giris-caps"
            class="gk-caps"
            role="status"
          >
            <i
              class="pi pi-arrow-circle-up"
              aria-hidden="true"
            />
            {{ t('giris.capsLockOn') }}
          </span>
        </div>
        <div class="gk-input-sar">
          <i
            class="pi pi-lock"
            aria-hidden="true"
          />
          <input
            id="giris-password"
            ref="sifreInput"
            v-model="password"
            data-test="giris-password"
            :type="sifreGorunur ? 'text' : 'password'"
            autocomplete="current-password"
            class="gk-input gk-input--sifre"
            :aria-invalid="alanHatasi.password ? 'true' : 'false'"
            :aria-describedby="passwordDescribedby"
            @keydown="klavyeKontrol"
            @input="alanHatasi.password = false"
          >
          <button
            type="button"
            class="gk-goz"
            :aria-label="sifreGorunur ? t('giris.sifreyiGizle') : t('giris.sifreyiGoster')"
            :aria-pressed="sifreGorunur ? 'true' : 'false'"
            @click="sifreGorunur = !sifreGorunur"
          >
            <i
              :class="sifreGorunur ? 'pi pi-eye-slash' : 'pi pi-eye'"
              aria-hidden="true"
            />
          </button>
        </div>
      </div>

      <label class="gk-hatirla">
        <input
          v-model="beniHatirla"
          type="checkbox"
          class="gk-checkbox"
        >
        <span>{{ t('giris.rememberMe') }}</span>
      </label>

      <button
        type="submit"
        class="gk-buton"
        data-test="giris-buton"
        :disabled="yukleniyor"
      >
        <i
          v-if="yukleniyor"
          class="pi pi-spin pi-spinner"
          aria-hidden="true"
        />
        <span>{{ yukleniyor ? t('giris.girisYapiliyor') : t('auth.login') }}</span>
      </button>

      <button
        v-if="passkeyVar"
        type="button"
        class="gk-buton gk-buton--ikincil"
        data-test="passkey-giris"
        :disabled="yukleniyor"
        @click="passkeyGirisYap"
      >
        <i
          class="pi pi-key"
          aria-hidden="true"
        />
        <span>{{ t('giris.passkeyIleGiris') }}</span>
      </button>

      <div class="gk-alt">
        <button
          type="button"
          class="gk-baglanti"
          data-test="sifremi-unuttum"
          :aria-expanded="sifremiUnuttumAdimi ? 'true' : 'false'"
          aria-controls="sifremi-unuttum-panel"
          @click="sifremiUnuttumAdimi = !sifremiUnuttumAdimi"
        >
          {{ t('giris.forgotPassword') }}
        </button>
      </div>

      <div
        v-if="sifremiUnuttumAdimi"
        id="sifremi-unuttum-panel"
        class="gk-sifirla"
      >
        <h3 class="gk-sifirla-baslik">
          {{ t('giris.forgotTitle') }}
        </h3>
        <p class="gk-sifirla-alt">
          {{ t('giris.forgotHint') }}
        </p>

        <div class="gk-alan">
          <label for="sifirla-username">{{ t('auth.username') }}</label>
          <input
            id="sifirla-username"
            v-model.trim="sifirlaUsername"
            type="text"
            autocomplete="username"
            class="gk-input"
            :placeholder="t('giris.usernamePlaceholder')"
            :disabled="sifirlaGonderiliyor"
            @keydown.enter.prevent.stop="sifreSifirlamaTalepEt"
          >
        </div>

        <button
          type="button"
          class="gk-buton gk-buton--ikincil"
          :disabled="sifirlaGonderiliyor"
          @click="sifreSifirlamaTalepEt"
        >
          <span>{{ t('giris.forgotSendLink') }}</span>
        </button>

        <p
          v-if="sifirlaMesaj"
          class="gk-basarili"
          role="status"
        >
          {{ sifirlaMesaj }}
        </p>
        <p
          v-if="sifirlaHata"
          class="gk-hata-metin"
          role="alert"
        >
          {{ sifirlaHata }}
        </p>
      </div>
    </form>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '../stores/authStore.js'
import { kurulumAPI, kullaniciAPI, passkeyAPI } from '../api/index.js'
import { passkeyDestekleniyor, passkeyIleGiris } from '../utils/webauthn.js'
import KurulumAdimi from './KurulumAdimi.vue'
import QrOnayPaneli from './QrOnayPaneli.vue'

const props = defineProps({
  sirketLogo: { type: String, default: '' },
  sonSirketAd: { type: String, default: '' }
})

const emit = defineEmits(['giris-basarili'])

const { t } = useI18n()
const authStore = useAuthStore()

const kullaniciInput = ref(null)
const sifreInput = ref(null)
const ikiFaktorInput = ref(null)

const username = ref('')
const password = ref('')
const sirketKodu = ref('')
const sirketKoduAcik = ref(false)
const beniHatirla = ref(false)
const sifreGorunur = ref(false)
const capsLockAcik = ref(false)
const hata = ref('')
const alanHatasi = ref({ username: false, password: false })

const ikiFaktorAdimi = ref(false)
const qrPanelAcik = ref(false)
const ikiFaktorKod = ref('')
const kodGecersiz = ref(false)
const sirketSecimAdimi = ref(false)
const girisToken = ref('')
const sirketler = ref([])
const secilenLogo = ref('')

const sifremiUnuttumAdimi = ref(false)
const sifirlaUsername = ref('')
const sifirlaGonderiliyor = ref(false)
const sifirlaMesaj = ref('')
const sifirlaHata = ref('')

const sonSecilenSirketId = ref(Number(localStorage.getItem('raspel_erp_son_sirket')) || null)

const yukleniyor = computed(() => authStore.loading === true)
const passkeyVar = passkeyDestekleniyor()

const logoGoster = computed(() => secilenLogo.value || props.sirketLogo || '/logo-icon.png')

const baslik = computed(() => {
  if (ikiFaktorAdimi.value) return t('giris.twoFactorTitle')
  if (sirketSecimAdimi.value) return t('giris.selectCompany')
  return t('giris.girisBaslik')
})

const altBaslik = computed(() => {
  if (ikiFaktorAdimi.value) return t('giris.twoFactorHint')
  if (sirketSecimAdimi.value) return t('giris.selectCompanyHint')
  return t('giris.girisAlt')
})

const passwordDescribedby = computed(() => {
  const idler = []
  if (capsLockAcik.value) idler.push('giris-caps')
  if (alanHatasi.value.password) idler.push('giris-hata')
  return idler.length > 0 ? idler.join(' ') : undefined
})

const gruplanmisSirketler = computed(() => {
  const gruplar = {}
  sirketler.value.forEach((s) => {
    let grupAdi = t('giris.bagimsizFirmalar')
    if (s.parentId) {
      const parent = sirketler.value.find((p) => p.id === s.parentId)
      grupAdi = parent ? parent.ad : `Grup ${s.parentId}`
    } else {
      const hasChildren = sirketler.value.some((c) => c.parentId === s.id)
      grupAdi = hasChildren ? s.ad : t('giris.bagimsizFirmalar')
    }
    if (!gruplar[grupAdi]) gruplar[grupAdi] = []
    gruplar[grupAdi].push(s)
  })

  if (Object.keys(gruplar).length === 1 && gruplar[t('giris.bagimsizFirmalar')]) {
    gruplar[t('giris.firmalar')] = gruplar[t('giris.bagimsizFirmalar')]
    delete gruplar[t('giris.bagimsizFirmalar')]
  }
  return gruplar
})

const kurulumAdimi = ref(false)
const kurulumYukleniyor = ref(false)
const kurulumForm = ref({
  ad: '',
  vergiNo: '',
  vergiDairesi: '',
  telefon: '',
  email: '',
  adminUsername: '',
  adminDisplayName: '',
  adminPassword: ''
})

const klavyeKontrol = (event) => {
  if (event && typeof event.getModifierState === 'function') {
    capsLockAcik.value = event.getModifierState('CapsLock')
  }
}

const kurulumDurumKontrol = async () => {
  try {
    const res = await kurulumAPI.durum()
    kurulumAdimi.value = !!res.data?.kurulumGerekli
  } catch {
    /* backend erişilemiyorsa normal giriş formu gösterilir */
  }
}

onMounted(async () => {
  beniHatirla.value = localStorage.getItem('raspel_erp_beni_hatirla') === 'true'
  await kurulumDurumKontrol()
})

// 2FA adımı açıldığında kodu doğrudan yazabilmek için odağı koda ver.
watch(ikiFaktorAdimi, async (aktif) => {
  if (!aktif) return
  await nextTick()
  ikiFaktorInput.value?.focus()
})

const sifreSifirlamaTalepEt = async () => {
  sifirlaMesaj.value = ''
  sifirlaHata.value = ''
  if (!sifirlaUsername.value.trim()) {
    sifirlaHata.value = t('giris.forgotUsernameRequired')
    return
  }
  sifirlaGonderiliyor.value = true
  try {
    const r = await kullaniciAPI.sifreSifirlamaTalebi({ username: sifirlaUsername.value.trim() })
    sifirlaMesaj.value = r.data?.message || t('giris.forgotLinkSent')
  } catch (err) {
    sifirlaHata.value = err.response?.data?.message || t('giris.forgotSendFailed')
  } finally {
    sifirlaGonderiliyor.value = false
  }
}

const kurulumBaslat = async () => {
  hata.value = ''
  const f = kurulumForm.value
  if (!f.ad.trim() || !f.vergiNo.trim()) {
    hata.value = t('kurulum.companyRequired')
    return
  }
  if (!f.adminUsername.trim() || !f.adminPassword) {
    hata.value = t('kurulum.credentialsRequired')
    return
  }
  kurulumYukleniyor.value = true
  try {
    const sonuc = await kurulumAPI.baslat({ ...f })
    girisToken.value = sonuc.data?.girisToken
    sirketler.value = sonuc.data?.sirketler || []
    if (sirketler.value.length === 1) {
      await sirketSecVeGirisYap(sirketler.value[0])
      return
    }
    kurulumAdimi.value = false
    sirketSecimAdimi.value = true
  } catch (err) {
    hata.value = err.response?.data?.message || t('kurulum.failed')
  } finally {
    kurulumYukleniyor.value = false
  }
}

const girisYap = async () => {
  hata.value = ''
  alanHatasi.value.username = !username.value.trim()
  alanHatasi.value.password = !password.value.trim()
  if (alanHatasi.value.username || alanHatasi.value.password) {
    hata.value = t('giris.emptyCredentials')
    return
  }
  try {
    localStorage.setItem('raspel_erp_beni_hatirla', beniHatirla.value ? 'true' : 'false')
    const sonuc = await authStore.girisYap(
      username.value,
      password.value,
      beniHatirla.value,
      sirketKodu.value
    )
    if (sonuc?.twoFactorGerekli) {
      girisToken.value = sonuc.girisToken
      qrPanelAcik.value = false
      ikiFaktorAdimi.value = true
      return
    }
    girisToken.value = sonuc.girisToken
    sirketler.value = sonuc.sirketler || []
    if (sirketler.value.length === 1) {
      await sirketSecVeGirisYap(sirketler.value[0])
      return
    }
    sirketSecimAdimi.value = true
  } catch (err) {
    hata.value = err.response?.data?.message || t('giris.loginFailed')
  }
}

const passkeyGirisYap = async () => {
  hata.value = ''
  try {
    const secenek = await passkeyAPI.girisSecenekleri()
    const credential = await passkeyIleGiris(secenek.data?.publicKey)
    if (!credential) return
    const sonuc = await passkeyAPI.giris({
      challengeId: secenek.data.challengeId,
      credential,
      sirketKodu: sirketKodu.value,
      rememberMe: beniHatirla.value === true
    })
    girisToken.value = sonuc.data?.girisToken
    sirketler.value = sonuc.data?.sirketler || []
    if (sirketler.value.length === 1) {
      await sirketSecVeGirisYap(sirketler.value[0])
      return
    }
    sirketSecimAdimi.value = true
  } catch (err) {
    if (err?.name === 'NotAllowedError' || err?.name === 'AbortError') return
    hata.value = err.response?.data?.message || t('giris.passkeyHata')
  }
}

const sirketSecVeGirisYap = async (sirket) => {
  hata.value = ''
  secilenLogo.value = sirket.logoUrl || ''
  authStore.sirketLogosunuAyarla(sirket.logoUrl || '')
  try {
    await authStore.girisSirket(girisToken.value, sirket.id, beniHatirla.value)
    localStorage.setItem('raspel_erp_son_sirket', sirket.id)
    if (sirket.ad) localStorage.setItem('raspel_erp_son_sirket_ad', sirket.ad)
    localStorage.setItem('raspel_erp_son_sirket_logo', sirket.logoUrl || '')
    emit('giris-basarili')
  } catch (err) {
    hata.value = err.response?.data?.message || t('giris.companySelectFailed')
  }
}

const ikiFaktorDogrula = async () => {
  hata.value = ''
  if (!ikiFaktorKod.value.trim()) {
    kodGecersiz.value = true
    hata.value = t('giris.invalidCode')
    return
  }
  try {
    const sonuc = await authStore.giris2fa(girisToken.value, ikiFaktorKod.value.trim(), beniHatirla.value)
    girisToken.value = sonuc.girisToken
    sirketler.value = sonuc.sirketler || []
    ikiFaktorAdimi.value = false
    if (sirketler.value.length === 1) {
      await sirketSecVeGirisYap(sirketler.value[0])
      return
    }
    sirketSecimAdimi.value = true
  } catch (err) {
    hata.value = err.response?.data?.message || t('giris.verifyFailed')
  }
}

const geriDon = () => {
  ikiFaktorAdimi.value = false
  qrPanelAcik.value = false
  ikiFaktorKod.value = ''
  kodGecersiz.value = false
  girisToken.value = ''
}

const qrGirisBasarili = async (sonuc) => {
  ikiFaktorAdimi.value = false
  qrPanelAcik.value = false
  girisToken.value = sonuc?.girisToken
  sirketler.value = sonuc?.sirketler || []
  if (sirketler.value.length === 1) {
    await sirketSecVeGirisYap(sirketler.value[0])
    return
  }
  sirketSecimAdimi.value = true
}

const tumAdimlariSifirla = () => {
  ikiFaktorAdimi.value = false
  qrPanelAcik.value = false
  sirketSecimAdimi.value = false
  ikiFaktorKod.value = ''
  kodGecersiz.value = false
  girisToken.value = ''
  sirketler.value = []
}
</script>

<style scoped>
.giris-kart {
  width: 100%;
  max-width: 420px;
  min-width: 0;
  padding: 32px;
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  background: rgba(16, 24, 33, 0.78);
  box-shadow:
    0 24px 60px rgba(0, 0, 0, 0.45),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
  color: #e8eef5;
}

.giris-kart--kurulum {
  max-width: 460px;
}

.gk-ust {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 22px;
  min-width: 0;
}

.gk-marka {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.gk-logo {
  width: 36px;
  height: 36px;
  object-fit: contain;
  border-radius: 9px;
}

.gk-marka-metin {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.gk-ad {
  font-size: 14px;
  font-weight: 800;
  letter-spacing: -0.01em;
}

.gk-son {
  font-size: 11.5px;
  color: #6f8294;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gk-son strong {
  color: #9fb0c0;
  font-weight: 600;
}

.gk-baslik {
  margin: 0;
  font-size: 30px;
  line-height: 36px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #e8eef5;
}

.gk-alt {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.55;
  color: #9fb0c0;
}

.gk-hata {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  margin-bottom: 16px;
  padding: 11px 13px;
  border-radius: 12px;
  border: 1px solid rgba(239, 68, 68, 0.36);
  background: rgba(239, 68, 68, 0.12);
  color: #fca5a5;
  font-size: 13px;
  line-height: 1.45;
}

.gk-hata i {
  flex-shrink: 0;
  margin-top: 2px;
  font-size: 14px;
}

.gk-hata-enter-active,
.gk-hata-leave-active {
  transition: opacity 180ms ease-out, transform 180ms ease-out;
}

.gk-hata-enter-from,
.gk-hata-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.giris-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
}

.gk-alan {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.gk-alan label {
  font-size: 12.5px;
  font-weight: 600;
  color: #9fb0c0;
}

.gk-etiket-satir {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.gk-caps {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 600;
  color: #fbbf24;
}

.gk-caps i {
  font-size: 11px;
}

.gk-input-sar {
  position: relative;
  min-width: 0;
}

.gk-input-sar > i {
  position: absolute;
  left: 13px;
  top: 50%;
  transform: translateY(-50%);
  color: #6f8294;
  font-size: 15px;
  pointer-events: none;
  transition: color 160ms ease-out;
}

.gk-input-sar:focus-within > i {
  color: #2dd4bf;
}

.gk-input {
  width: 100%;
  min-width: 0;
  min-height: 46px;
  padding: 11px 13px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 12px;
  background: rgba(7, 11, 16, 0.72);
  color: #e8eef5;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.4;
  transition: border-color 160ms ease-out, box-shadow 160ms ease-out;
}

.gk-input-sar .gk-input {
  padding-left: 39px;
}

.gk-input--sifre {
  padding-right: 44px;
}

.gk-input::placeholder {
  color: #6f8294;
}

.gk-input:hover {
  border-color: rgba(148, 163, 184, 0.3);
}

.gk-input:focus {
  outline: 2px solid #14b8a6;
  outline-offset: 1px;
  border-color: #14b8a6;
}

.gk-input[aria-invalid='true'] {
  border-color: rgba(239, 68, 68, 0.6);
}

.gk-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Tarayıcı otomatik doldurması koyu zemini bozmasın. */
.gk-input:-webkit-autofill,
.gk-input:-webkit-autofill:hover,
.gk-input:-webkit-autofill:focus {
  -webkit-text-fill-color: #e8eef5;
  box-shadow: 0 0 0 1000px #0b1219 inset;
  caret-color: #e8eef5;
}

.gk-input--kod {
  text-align: center;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 18px;
  letter-spacing: 0.24em;
}

.gk-ipucu {
  font-size: 11.5px;
  line-height: 1.45;
  color: #6f8294;
}

.gk-goz {
  position: absolute;
  right: 1px;
  top: 50%;
  transform: translateY(-50%);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: #6f8294;
  font-size: 15px;
  cursor: pointer;
  transition: color 160ms ease-out, background 160ms ease-out;
}

.gk-goz:hover {
  color: #e8eef5;
  background: rgba(148, 163, 184, 0.1);
}

.gk-goz:focus-visible,
.gk-baglanti:focus-visible,
.gk-kod-ac:focus-visible,
.gk-buton:focus-visible,
.sirket-kart:focus-visible,
.gk-hatirla input:focus-visible {
  outline: 2px solid #14b8a6;
  outline-offset: 2px;
}

.gk-hatirla {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  min-height: 44px;
  font-size: 13px;
  color: #9fb0c0;
  cursor: pointer;
  user-select: none;
}

.gk-checkbox {
  width: 18px;
  height: 18px;
  margin: 0;
  accent-color: #14b8a6;
  cursor: pointer;
}

.gk-buton {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  min-height: 46px;
  padding: 11px 16px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: linear-gradient(135deg, #14b8a6, #0d9488);
  color: #04211d;
  font-family: inherit;
  font-size: 14.5px;
  font-weight: 700;
  cursor: pointer;
  transition: filter 160ms ease-out, transform 160ms ease-out;
}

.gk-buton:hover:not(:disabled) {
  filter: brightness(1.06);
  transform: translateY(-1px);
}

.gk-buton:active:not(:disabled) {
  transform: none;
}

.gk-buton:disabled {
  opacity: 0.7;
  cursor: progress;
}

.gk-buton--ikincil {
  background: rgba(20, 184, 166, 0.12);
  border-color: rgba(20, 184, 166, 0.32);
  color: #2dd4bf;
}

.gk-buton--ikincil:hover:not(:disabled) {
  background: rgba(20, 184, 166, 0.2);
  filter: none;
}

.gk-alt {
  display: flex;
  justify-content: center;
}

.gk-baglanti {
  align-self: center;
  min-height: 44px;
  padding: 0 10px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #9fb0c0;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: color 160ms ease-out;
}

.gk-baglanti:hover {
  color: #2dd4bf;
}

/* Şirket kodu katlanır alanı */
.gk-kod {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-bottom: 12px;
  border-bottom: 1px dashed rgba(148, 163, 184, 0.16);
}

.gk-kod-ac {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  padding: 0 10px;
  align-self: flex-start;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 10px;
  background: rgba(148, 163, 184, 0.06);
  color: #9fb0c0;
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 160ms ease-out, color 160ms ease-out;
}

.gk-kod-ac span {
  flex: 1;
}

.gk-kod-ac:hover {
  color: #e8eef5;
  border-color: rgba(20, 184, 166, 0.4);
}

.gk-kod-ac i:first-child {
  color: #14b8a6;
  font-size: 13px;
}

.gk-ac-enter-active,
.gk-ac-leave-active {
  transition: opacity 180ms ease-out, transform 180ms ease-out;
}

.gk-ac-enter-from,
.gk-ac-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* Şifremi unuttum */
.gk-sifirla {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 4px;
  padding: 16px;
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: rgba(7, 11, 16, 0.5);
}

.gk-sifirla-baslik {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #e8eef5;
}

.gk-sifirla-alt {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.5;
  color: #9fb0c0;
}

.gk-basarili,
.gk-hata-metin {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.45;
}

.gk-basarili {
  color: #5eead4;
}

.gk-hata-metin {
  color: #fca5a5;
}

/* 2FA */
.gk-adim-ikon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  margin-bottom: 2px;
  border-radius: 12px;
  background: rgba(20, 184, 166, 0.14);
  border: 1px solid rgba(20, 184, 166, 0.26);
  color: #2dd4bf;
  font-size: 19px;
}

/* Şirket seçimi */
.sirket-listesi {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 340px;
  overflow-y: auto;
  padding: 2px;
}

.sirket-grup-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 10px 2px 0;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: #6f8294;
}

.sirket-kart {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-width: 0;
  padding: 12px 14px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 13px;
  background: rgba(7, 11, 16, 0.55);
  color: #e8eef5;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color 160ms ease-out, background 160ms ease-out, transform 160ms ease-out;
}

.sirket-kart:hover {
  border-color: rgba(20, 184, 166, 0.5);
  background: rgba(20, 184, 166, 0.08);
  transform: translateY(-1px);
}

.sirket-kart.son-secilen {
  border-color: rgba(20, 184, 166, 0.44);
  background: rgba(20, 184, 166, 0.06);
}

.sirket-kart-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border-radius: 10px;
  background: linear-gradient(135deg, #14b8a6, #0d9488);
  color: #04211d;
  font-size: 15px;
}

.sirket-kart-bilgi {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.sirket-kart-ad {
  font-size: 13.5px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sirket-kart-vkn {
  font-size: 11px;
  color: #6f8294;
}

.sirket-kart-sag {
  display: flex;
  align-items: center;
  gap: 7px;
  flex-shrink: 0;
}

.sirket-kart-ok {
  color: #6f8294;
  font-size: 11px;
}

.sirket-tur-badge,
.sirket-yil-badge {
  padding: 2px 7px;
  border-radius: 10px;
  font-size: 10.5px;
  font-weight: 600;
  background: rgba(148, 163, 184, 0.14);
  color: #9fb0c0;
}

.tur-resmi {
  background: rgba(20, 184, 166, 0.16);
  border: 1px solid rgba(20, 184, 166, 0.3);
  color: #2dd4bf;
}

.tur-gayriresmi {
  background: rgba(245, 158, 11, 0.14);
  border: 1px solid rgba(245, 158, 11, 0.3);
  color: #fbbf24;
}

.son-secilen-yildiz {
  color: #fbbf24;
  font-size: 12px;
}

.sirket-yok {
  margin: 4px 0;
  text-align: center;
  font-size: 13px;
  color: #9fb0c0;
}

/* İlk kurulum sihirbazındaki PrimeVue alanları da koyu zemine uyar. */
.giris-kart :deep(.p-inputtext) {
  background: rgba(7, 11, 16, 0.72) !important;
  border-color: rgba(148, 163, 184, 0.18) !important;
  color: #e8eef5 !important;
}

.giris-kart :deep(.p-inputtext::placeholder) {
  color: #6f8294 !important;
}

.giris-kart :deep(.p-inputtext:enabled:focus) {
  border-color: #14b8a6 !important;
  box-shadow: 0 0 0 2px rgba(20, 184, 166, 0.3) !important;
}

@media (max-width: 480px) {
  .giris-kart {
    padding: 22px;
    border-radius: 16px;
  }
  .gk-baslik {
    font-size: 26px;
    line-height: 32px;
  }
  .sirket-tur-badge {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .giris-kart *,
  .giris-kart *::before,
  .giris-kart *::after {
    animation: none !important;
    transition: none !important;
  }
}
</style>
