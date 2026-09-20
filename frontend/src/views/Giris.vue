<template>
  <div
    class="giris-sayfasi"
    :style="parallax"
  >
    <!-- Sinematik sahne: CSS mesh katmanlari (canvas / agir blur yok) -->
    <div
      class="sahne-arka"
      aria-hidden="true"
    >
      <div class="sahne-mesh" />
      <div class="sahne-grid" />
      <div class="sahne-parilti p1" />
      <div class="sahne-parilti p2" />
    </div>

    <div class="giris-split-wrapper">
      <!-- SOL BÖLÜM: Kurumsal Hero & Özellik Vitrini (Masaüstü) -->
      <div class="giris-hero-alani">
        <div class="hero-brand">
          <div class="hero-logo-box">
            <i class="pi pi-bolt hero-logo-icon" />
          </div>
          <div class="hero-brand-text">
            <span class="hero-brand-name">RasPel ERP</span>
            <span class="hero-badge">v1.28.0 Enterprise</span>
          </div>
        </div>

        <span class="hero-eyebrow"><i class="pi pi-bolt" /> {{ $t('giris.heroEyebrow') }}</span>
        <h1 class="hero-title">
          {{ $t('giris.heroTitle') }}
        </h1>
        <div class="hero-rotate">
          <transition
            name="rotate-fade"
            mode="out-in"
          >
            <span
              :key="rotateIndex"
              class="hero-rotate-item"
            >{{ rotateItems[rotateIndex] }}</span>
          </transition>
        </div>
        <p class="hero-desc">
          {{ $t('giris.heroSubtitle') }}
        </p>

        <!-- Canli urun vitrini -->
        <div class="hero-preview-wrap">
          <LoginPreview />
        </div>

        <!-- Ozellik listesi -->
        <div class="hero-features">
          <span class="hero-features-title">{{ $t('giris.featuresTitle') }}</span>
          <div class="hero-features-grid">
            <div class="feature-item">
              <span class="feature-ikon"><i class="pi pi-sparkles" /></span>
              <div class="feature-metin">
                <strong>{{ $t('giris.featureAiTitle') }}</strong>
                <span>{{ $t('giris.featureAiDesc') }}</span>
              </div>
            </div>
            <div class="feature-item">
              <span class="feature-ikon"><i class="pi pi-file-check" /></span>
              <div class="feature-metin">
                <strong>{{ $t('giris.featureEInvoiceTitle') }}</strong>
                <span>{{ $t('giris.featureEInvoiceDesc') }}</span>
              </div>
            </div>
            <div class="feature-item">
              <span class="feature-ikon"><i class="pi pi-box" /></span>
              <div class="feature-metin">
                <strong>{{ $t('giris.featureStockTitle') }}</strong>
                <span>{{ $t('giris.featureStockDesc') }}</span>
              </div>
            </div>
            <div class="feature-item">
              <span class="feature-ikon"><i class="pi pi-chart-bar" /></span>
              <div class="feature-metin">
                <strong>{{ $t('giris.featureProfitTitle') }}</strong>
                <span>{{ $t('giris.featureProfitDesc') }}</span>
              </div>
            </div>
            <div class="feature-item">
              <span class="feature-ikon"><i class="pi pi-compass" /></span>
              <div class="feature-metin">
                <strong>{{ $t('giris.featureFieldTitle') }}</strong>
                <span>{{ $t('giris.featureFieldDesc') }}</span>
              </div>
            </div>
            <div class="feature-item">
              <span class="feature-ikon"><i class="pi pi-wallet" /></span>
              <div class="feature-metin">
                <strong>{{ $t('giris.featureCollectionTitle') }}</strong>
                <span>{{ $t('giris.featureCollectionDesc') }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Guven rozetleri (notr) -->
        <div class="hero-trust">
          <span class="trust-badge"><i class="pi pi-shield" /> {{ $t('giris.trustKvkk') }}</span>
          <span class="trust-badge"><i class="pi pi-lock" /> {{ $t('giris.trustSsl') }}</span>
          <span class="trust-badge"><i class="pi pi-key" /> {{ $t('giris.trust2fa') }}</span>
          <span class="trust-badge"><i class="pi pi-cloud" /> {{ $t('giris.trustCloud') }}</span>
        </div>
      </div>

      <!-- SAĞ BÖLÜM: Odaklanmış Cam Efektli Giriş Formu -->
      <div class="giris-form-alani">
        <div class="giris-kutu">
          <!-- Mobil kompakt hero (<960px) -->
          <div class="giris-mobil-hero">
            <div class="mobil-mark">
              <i class="pi pi-bolt" />
            </div>
            <div class="mobil-metin">
              <h1 class="mobil-title">
                RasPel ERP
              </h1>
              <p class="mobil-tagline">
                {{ $t('giris.mobileTagline') }}
              </p>
            </div>
            <div class="mobil-chips">
              <span><i class="pi pi-sparkles" /> {{ $t('giris.previewTabAi') }}</span>
              <span><i class="pi pi-file-check" /> {{ $t('giris.previewTabInvoice') }}</span>
              <span><i class="pi pi-box" /> {{ $t('giris.previewTabStock') }}</span>
              <span><i class="pi pi-chart-bar" /> {{ $t('giris.previewTabProfit') }}</span>
            </div>
            <div class="mobil-chips mobil-trust">
              <span><i class="pi pi-shield" /> {{ $t('giris.trustKvkk') }}</span>
              <span><i class="pi pi-lock" /> {{ $t('giris.trustSsl') }}</span>
              <span><i class="pi pi-key" /> {{ $t('giris.trust2fa') }}</span>
            </div>
          </div>

          <div class="giris-logo">
            <div class="logo-icon">
              <img
                v-if="sirketLogo"
                :src="sirketLogo"
                class="sirket-logo"
                alt="logo"
              >
              <i
                v-else
                class="pi pi-calculator"
              />
            </div>
            <h2>RasPel ERP</h2>
            <p class="alt-baslik">
              {{ $t('giris.subtitle') }}
            </p>
          </div>

          <p class="giris-selam">
            {{ selamlama }}<span
              v-if="sonSirketAd"
              class="son-sirket"
            > · {{ $t('giris.lastCompany') }}: <strong>{{ sonSirketAd }}</strong></span>
          </p>

          <div class="giris-form">
            <transition name="shake">
              <div
                v-if="hata"
                class="hata-kutu"
              >
                <i class="pi pi-exclamation-circle" /> {{ hata }}
              </div>
            </transition>

            <!-- Adim 0: Ilk Kurulum -->
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

            <!-- Adim 2: 2FA -->
            <div v-else-if="ikiFaktorAdimi">
              <div class="iki-fa-ikon">
                <i class="pi pi-shield" />
              </div>
              <h2 class="iki-fa-baslik">
                {{ $t('giris.twoFactorTitle') }}
              </h2>
              <p class="iki-fa-alt">
                {{ $t('giris.twoFactorHint') }}
              </p>
              <div class="form-grup">
                <div class="input-wrapper kod-wrapper">
                  <i class="pi pi-key" />
                  <InputText
                    v-model="ikiFaktorKod"
                    placeholder="••••••"
                    inputmode="numeric"
                    maxlength="6"
                    class="kod-input"
                    @keyup="e => e.key === 'Enter' && ikiFaktorDogrula()"
                  />
                </div>
              </div>
              <Button
                :label="$t('giris.verify')"
                icon="pi pi-shield"
                :loading="authStore?.loading || false"
                class="giris-buton"
                @click="ikiFaktorDogrula"
              />
              <div class="geri-satir">
                <a @click="geriDon">&larr; {{ $t('giris.back') }}</a>
              </div>
            </div>

            <!-- Adim 3: Sirket Secimi -->
            <div v-else-if="sirketSecimAdimi">
              <div class="sirket-secim-ikon">
                <i class="pi pi-building" />
              </div>
              <h2 class="iki-fa-baslik">
                {{ $t('giris.selectCompany') }}
              </h2>
              <p class="iki-fa-alt">
                {{ $t('giris.selectCompanyHint') }}
              </p>
              <div
                v-if="sirketler && sirketler.length > 0"
                class="sirket-listesi"
              >
                <template
                  v-for="(grup, grupAdi) in gruplanmisSirketler"
                  :key="grupAdi"
                >
                  <div
                    v-if="Object.keys(gruplanmisSirketler).length > 1"
                    class="sirket-grup-baslik"
                  >
                    <i class="pi pi-folder" />
                    <span>{{ grupAdi }}</span>
                  </div>
                  <button
                    v-for="s in grup"
                    :key="s.id"
                    class="sirket-secim-kart"
                    :class="{ 'son-secilen': s.id === sonSecilenSirketId }"
                    @click="sirketSecVeGirisYap(s)"
                  >
                    <div class="sirket-kart-sol">
                      <i class="pi pi-building" />
                      <div class="sirket-kart-bilgi">
                        <span class="sirket-kart-ad">{{ s.ad }}</span>
                        <span class="sirket-kart-vkn">VKN: {{ s.vergiNo || '-' }}</span>
                      </div>
                    </div>
                    <div class="sirket-kart-sag">
                      <span
                        v-if="s.tur && s.tur !== 'DIGER'"
                        class="sirket-tur-badge"
                        :class="'tur-' + (s.tur || '').toLowerCase()"
                      >
                        {{ s.tur === 'RESMI' ? 'Resmi' : s.tur === 'GAYRIRESMI' ? 'Gayriresmi' : s.tur }}
                      </span>
                      <span
                        v-if="s.yil"
                        class="sirket-yil-badge"
                      >{{ s.yil }}</span>
                      <i
                        v-if="s.id === sonSecilenSirketId"
                        class="pi pi-star-fill son-secilen-yildiz"
                      />
                      <i class="pi pi-arrow-right" />
                    </div>
                  </button>
                </template>
              </div>
              <p
                v-else
                class="sirket-yok"
              >
                {{ $t('giris.noCompany') }}
              </p>
              <div class="geri-satir">
                <a @click="tumAdimlariSifirla">&larr; {{ $t('giris.loginAgain') }}</a>
              </div>
            </div>

            <!-- Adim 1: Normal Kullanici Girişi -->
            <div v-else>
              <div class="form-grup">
                <label>{{ $t('auth.username') }}</label>
                <div class="input-wrapper">
                  <i class="pi pi-user" />
                  <InputText
                    ref="kullaniciInput"
                    v-model="username"
                    :placeholder="$t('auth.username')"
                    @keyup="onUsernameKeyup"
                    @keydown="klavyeKontrol"
                  />
                </div>
              </div>

              <div class="form-grup">
                <div class="label-row">
                  <label>{{ $t('auth.password') }}</label>
                  <span
                    v-if="capsLockAcik"
                    class="caps-lock-uyari"
                  >
                    <i class="pi pi-arrow-circle-up" /> {{ $t('giris.capsLockOn') }}
                  </span>
                </div>
                <div class="input-wrapper">
                  <i class="pi pi-lock" />
                  <InputText
                    ref="sifreInput"
                    v-model="password"
                    :type="sifreGorunur ? 'text' : 'password'"
                    placeholder="••••••"
                    @keyup="onPasswordKeyup"
                    @keydown="klavyeKontrol"
                  />
                  <button
                    type="button"
                    class="sifre-toggle"
                    tabindex="-1"
                    :aria-label="sifreGorunur ? $t('giris.sifreyiGizle') : $t('giris.sifreyiGoster')"
                    @click="sifreGorunur = !sifreGorunur"
                  >
                    <i :class="sifreGorunur ? 'pi pi-eye-slash' : 'pi pi-eye'" />
                  </button>
                </div>
              </div>

              <div class="beni-hatirla">
                <Checkbox
                  v-model="beniHatirla"
                  :binary="true"
                  input-id="beniHatirla"
                />
                <label for="beniHatirla">{{ $t('giris.rememberMe') }}</label>
              </div>

              <Button
                :label="$t('auth.login')"
                icon="pi pi-sign-in"
                :loading="authStore?.loading || false"
                class="giris-buton"
                @click="girisYap"
              />

              <!-- Hızlı Demo Rolleri -->
              <div class="hizli-roller">
                <span class="hizli-rol-etiket">{{ $t('giris.quickFill') }}:</span>
                <button
                  type="button"
                  class="rol-chip"
                  @click="hizliRolDoldur('admin')"
                >
                  <i class="pi pi-user-plus" /> {{ $t('giris.quickRoleAdmin') }}
                </button>
                <button
                  type="button"
                  class="rol-chip"
                  @click="hizliRolDoldur('muhasebe')"
                >
                  <i class="pi pi-calculator" /> {{ $t('giris.quickRoleAccountant') }}
                </button>
              </div>

              <div class="giris-alt-linkler">
                <a @click="sifremiUnuttumAdimi = true">{{ $t('giris.forgotPassword') }}</a>
              </div>
            </div>
          </div>

          <!-- Sifremi Unuttum -->
          <div
            v-if="sifremiUnuttumAdimi && !ikiFaktorAdimi && !sirketSecimAdimi && !kurulumAdimi"
            class="sifre-sifirla-panel"
          >
            <div class="sifirla-ust">
              <i class="pi pi-envelope" />
              <h3>{{ $t('giris.forgotTitle') }}</h3>
              <a @click="sifremiUnuttumAdimi = false">&larr; {{ $t('giris.backToLogin') }}</a>
            </div>
            <p>{{ $t('giris.forgotHint') }}</p>
            <div class="sifirla-form">
              <InputText
                v-model="sifirlaUsername"
                :placeholder="$t('giris.usernamePlaceholder')"
                class="w-full"
                :disabled="sifirlaGonderiliyor"
                @keyup.enter="sifreSifirlamaTalepEt"
              />
              <Button
                :label="$t('giris.forgotSendLink')"
                icon="pi pi-send"
                :loading="sifirlaGonderiliyor"
                @click="sifreSifirlamaTalepEt"
              />
            </div>
            <Message
              v-if="sifirlaMesaj"
              severity="success"
              :closable="false"
            >
              {{ sifirlaMesaj }}
            </Message>
            <Message
              v-if="sifirlaHata"
              severity="error"
              :closable="false"
            >
              {{ sifirlaHata }}
            </Message>
          </div>

          <!-- Kart Altı Güvenlik Mikro Rozetleri (Mobil & Genel) -->
          <div class="form-guvenlik-rozetleri">
            <span><i class="pi pi-shield" /> {{ $t('giris.miniSecuritySsl') }}</span>
            <span><i class="pi pi-lock" /> {{ $t('giris.miniSecurity2fa') }}</span>
            <span><i class="pi pi-database" /> {{ $t('giris.miniSecurityCloud') }}</span>
          </div>

          <div class="giris-footer">
            <ThemeSwitcher />
            <span>{{ $t('giris.copyright') }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watchEffect } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useMouse, usePreferredReducedMotion, useIntervalFn, useThrottleFn } from '@vueuse/core'
import { useAuthStore } from '../stores/authStore.js'
import { kurulumAPI, kullaniciAPI } from '../api/index.js'
import ThemeSwitcher from '../components/ThemeSwitcher.vue'
import KurulumAdimi from '../components/KurulumAdimi.vue'
import LoginPreview from '../components/LoginPreview.vue'

const router = useRouter()
const authStore = useAuthStore()
const { t } = useI18n()

const kullaniciInput = ref(null)
const sifreInput = ref(null)

const username = ref('')
const password = ref('')
const hata = ref('')
const sirketLogo = ref('')
const sirketler = ref([])
const sifreGorunur = ref(false)
const beniHatirla = ref(false)
const capsLockAcik = ref(false)

const ikiFaktorAdimi = ref(false)
const sirketSecimAdimi = ref(false)
const ikiFaktorKod = ref('')
const girisToken = ref('')

const sifirlaUsername = ref('')
const sifirlaGonderiliyor = ref(false)
const sifirlaMesaj = ref('')
const sifirlaHata = ref('')

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

const sonSecilenSirketId = ref(Number(localStorage.getItem('raspel_erp_son_sirket')) || null)
const sonSirketAd = ref(localStorage.getItem('raspel_erp_son_sirket_ad') || '')

// Hareket azaltma tercihi ve fare parallax (yalnizca transform/opacity, throttle'li)
const azHareket = usePreferredReducedMotion()
const hareketAzalt = computed(() => azHareket.value === 'reduce')
const { x: hamX, y: hamY } = useMouse()
const fareX = ref(0)
const fareY = ref(0)
// Mousemove'u 40ms'te bir isleyerek gereksiz stil hesabini onler (performans).
const fareGuncelle = useThrottleFn((x, y) => {
  fareX.value = x
  fareY.value = y
}, 40)
watchEffect(() => fareGuncelle(hamX.value, hamY.value))

const parallax = computed(() => {
  if (hareketAzalt.value || typeof window === 'undefined') return {}
  const w = window.innerWidth || 1
  const h = window.innerHeight || 1
  const dx = (fareX.value - w / 2) / (w / 2)
  const dy = (fareY.value - h / 2) / (h / 2)
  return {
    '--px': `${(dx * 6).toFixed(2)}px`,
    '--py': `${(dy * 6).toFixed(2)}px`,
    '--pxs': `${(dx * 14).toFixed(2)}px`,
    '--pys': `${(dy * 14).toFixed(2)}px`
  }
})

// Saate gore selamlama
const selamlama = computed(() => {
  const saat = new Date().getHours()
  if (saat < 12) return t('giris.greetingMorning')
  if (saat < 18) return t('giris.greetingAfternoon')
  return t('giris.greetingEvening')
})

// Hero basligi altinda donen deger onerileri
const rotateIndex = ref(0)
const rotateItems = computed(() => [
  t('giris.heroRotate1'),
  t('giris.heroRotate2'),
  t('giris.heroRotate3'),
  t('giris.heroRotate4')
])
useIntervalFn(() => {
  if (hareketAzalt.value) return
  rotateIndex.value = (rotateIndex.value + 1) % rotateItems.value.length
}, 2800)

const gruplanmisSirketler = computed(() => {
  const gruplar = {}
  sirketler.value.forEach(s => {
    let grupAdi = t('giris.bagimsizFirmalar')
    if (s.parentId) {
      const parent = sirketler.value.find(p => p.id === s.parentId)
      grupAdi = parent ? parent.ad : `Grup ${s.parentId}`
    } else {
      const hasChildren = sirketler.value.some(c => c.parentId === s.id)
      grupAdi = hasChildren ? s.ad : t('giris.bagimsizFirmalar')
    }
    if (!gruplar[grupAdi]) {
      gruplar[grupAdi] = []
    }
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

const sifremiUnuttumAdimi = ref(false)

onMounted(async () => {
  if (authStore.isLoggedIn) {
    router.push('/')
    return
  }
  beniHatirla.value = localStorage.getItem('raspel_erp_beni_hatirla') === 'true'
  await kurulumDurumKontrol()
})

const klavyeKontrol = (event) => {
  if (event && typeof event.getModifierState === 'function') {
    capsLockAcik.value = event.getModifierState('CapsLock')
  }
}

const onUsernameKeyup = (event) => {
  klavyeKontrol(event)
  if (event.key === 'Enter') {
    odaklanSifre()
  }
}

const onPasswordKeyup = (event) => {
  klavyeKontrol(event)
  if (event.key === 'Enter') {
    girisYap()
  }
}

const hizliRolDoldur = (rol) => {
  username.value = rol
  odaklanSifre()
}

const kurulumDurumKontrol = async () => {
  try {
    const res = await kurulumAPI.durum()
    kurulumAdimi.value = !!res.data?.kurulumGerekli
  } catch {
    /* backend erisilemiyorsa giris formu gosterilir */
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

const odaklanSifre = () => sifreInput.value?.$el?.querySelector('input')?.focus()

const girisYap = async () => {
  hata.value = ''
  if (!username.value.trim() || !password.value.trim()) {
    hata.value = t('giris.emptyCredentials')
    return
  }
  try {
    localStorage.setItem('raspel_erp_beni_hatirla', beniHatirla.value ? 'true' : 'false')
    const sonuc = await authStore.girisYap(username.value, password.value, beniHatirla.value)
    if (sonuc?.twoFactorGerekli) {
      girisToken.value = sonuc.girisToken
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

const sirketSecVeGirisYap = async (sirket) => {
  hata.value = ''
  sirketLogo.value = sirket.logoUrl || ''
  try {
    await authStore.girisSirket(girisToken.value, sirket.id, beniHatirla.value)
    localStorage.setItem('raspel_erp_son_sirket', sirket.id)
    if (sirket.ad) localStorage.setItem('raspel_erp_son_sirket_ad', sirket.ad)
    router.push('/')
  } catch (err) {
    hata.value = err.response?.data?.message || t('giris.companySelectFailed')
  }
}

const ikiFaktorDogrula = async () => {
  hata.value = ''
  if (!ikiFaktorKod.value.trim()) {
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
  ikiFaktorKod.value = ''
  girisToken.value = ''
}
const tumAdimlariSifirla = () => {
  ikiFaktorAdimi.value = false
  sirketSecimAdimi.value = false
  ikiFaktorKod.value = ''
  girisToken.value = ''
  sirketler.value = []
}
</script>

<style scoped>
.giris-sayfasi {
  --giris-aksan: var(--accent, #10b981);
  --giris-aksan-koyu: #059669;
  --giris-aksan-parlak: #34d399;
  --giris-tint-05: rgba(16, 185, 129, 0.05);
  --giris-tint-08: rgba(16, 185, 129, 0.08);
  --giris-tint-14: rgba(16, 185, 129, 0.14);
  --giris-tint-15: rgba(16, 185, 129, 0.15);
  --giris-tint-16: rgba(16, 185, 129, 0.16);
  --giris-tint-18: rgba(16, 185, 129, 0.18);
  --giris-tint-20: rgba(16, 185, 129, 0.20);
  --giris-tint-22: rgba(16, 185, 129, 0.22);
  --giris-tint-25: rgba(16, 185, 129, 0.25);
  --giris-tint-26: rgba(16, 185, 129, 0.26);
  --giris-tint-28: rgba(16, 185, 129, 0.28);
  --giris-tint-30: rgba(16, 185, 129, 0.30);
  --giris-tint-35: rgba(16, 185, 129, 0.35);
  --giris-tint-40: rgba(16, 185, 129, 0.40);
  --giris-tint-50: rgba(16, 185, 129, 0.50);
  --giris-tint-55: rgba(16, 185, 129, 0.55);
  min-height: 100vh;
  min-height: 100dvh;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at 50% 0%, #0a1620 0%, #0b0f14 62%);
  overflow-x: hidden;
  overflow-y: auto;
  padding:
    calc(30px + env(safe-area-inset-top))
    calc(20px + env(safe-area-inset-right))
    calc(30px + env(safe-area-inset-bottom))
    calc(20px + env(safe-area-inset-left));
  width: 100%;
  max-width: 100%;
  overscroll-behavior: none;
  -webkit-tap-highlight-color: transparent;
}

/* Aksan rengi dinamik (kullanici/sirket secimi) ise renk tonlarini color-mix ile turet. */
@supports (color: color-mix(in srgb, red, blue)) {
  .giris-sayfasi {
    --giris-aksan-koyu: color-mix(in srgb, var(--giris-aksan) 82%, #000000);
    --giris-aksan-parlak: color-mix(in srgb, var(--giris-aksan) 70%, #ffffff);
    --giris-tint-05: color-mix(in srgb, var(--giris-aksan) 5%, transparent);
    --giris-tint-08: color-mix(in srgb, var(--giris-aksan) 8%, transparent);
    --giris-tint-14: color-mix(in srgb, var(--giris-aksan) 14%, transparent);
    --giris-tint-15: color-mix(in srgb, var(--giris-aksan) 15%, transparent);
    --giris-tint-16: color-mix(in srgb, var(--giris-aksan) 16%, transparent);
    --giris-tint-18: color-mix(in srgb, var(--giris-aksan) 18%, transparent);
    --giris-tint-20: color-mix(in srgb, var(--giris-aksan) 20%, transparent);
    --giris-tint-22: color-mix(in srgb, var(--giris-aksan) 22%, transparent);
    --giris-tint-25: color-mix(in srgb, var(--giris-aksan) 25%, transparent);
    --giris-tint-26: color-mix(in srgb, var(--giris-aksan) 26%, transparent);
    --giris-tint-28: color-mix(in srgb, var(--giris-aksan) 28%, transparent);
    --giris-tint-30: color-mix(in srgb, var(--giris-aksan) 30%, transparent);
    --giris-tint-35: color-mix(in srgb, var(--giris-aksan) 35%, transparent);
    --giris-tint-40: color-mix(in srgb, var(--giris-aksan) 40%, transparent);
    --giris-tint-50: color-mix(in srgb, var(--giris-aksan) 50%, transparent);
    --giris-tint-55: color-mix(in srgb, var(--giris-aksan) 55%, transparent);
    background: radial-gradient(circle at 50% 0%, color-mix(in srgb, var(--giris-aksan) 10%, #0b0f14) 0%, #0b0f14 62%);
  }
}

/* Company Selection Enhance */
.sirket-grup-baslik {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-secondary);
  margin: 16px 0 8px 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.sirket-kart-sol {
  display: flex;
  align-items: center;
  gap: 12px;
}
.sirket-kart-sag {
  display: flex;
  align-items: center;
  gap: 10px;
}
.sirket-tur-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 12px;
  background: rgba(255,255,255,0.1);
  color: var(--text-secondary);
}
.tur-resmi {
  background: var(--giris-tint-15);
  color: var(--giris-aksan);
  border: 1px solid var(--giris-tint-30);
}
.tur-gayriresmi {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
  border: 1px solid rgba(245, 158, 11, 0.3);
}
.sirket-yil-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 12px;
  background: var(--giris-tint-15);
  color: var(--giris-aksan);
  border: 1px solid var(--giris-tint-30);
}
.son-secilen-yildiz {
  color: #f59e0b;
  font-size: 14px;
}
.sirket-secim-kart.son-secilen {
  border-color: var(--giris-tint-50);
  background: var(--giris-tint-05);
}

[data-theme='light'] .giris-sayfasi {
  background: radial-gradient(circle at 50% 0%, #ffffff 0%, #eef2f6 100%);
}

/* Sinematik sahne katmanlari (tamamen composited; canvas / agir blur yok) */
.sahne-arka {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}
.sahne-mesh {
  position: absolute;
  inset: -30%;
  background:
    radial-gradient(38% 45% at 22% 20%, var(--giris-tint-30), transparent 70%),
    radial-gradient(42% 48% at 82% 26%, var(--giris-tint-26), transparent 72%),
    radial-gradient(46% 50% at 68% 82%, var(--giris-tint-16), transparent 74%),
    radial-gradient(40% 42% at 12% 78%, var(--giris-tint-16), transparent 72%);
  transform: rotate(0deg);
  animation: sahneDon 90s linear infinite;
  will-change: transform;
}
.sahne-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.06) 1px, transparent 1px);
  background-size: 46px 46px;
  -webkit-mask-image: radial-gradient(circle at 50% 40%, #000 0%, transparent 78%);
  mask-image: radial-gradient(circle at 50% 40%, #000 0%, transparent 78%);
  opacity: 0.6;
}
.sahne-parilti {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}
.sahne-parilti.p1 {
  width: 46vw;
  height: 46vw;
  max-width: 720px;
  max-height: 720px;
  top: -12%;
  left: -8%;
  background: radial-gradient(circle, var(--giris-tint-22), transparent 62%);
  animation: parildama 16s ease-in-out infinite alternate;
}
.sahne-parilti.p2 {
  width: 40vw;
  height: 40vw;
  max-width: 620px;
  max-height: 620px;
  bottom: -14%;
  right: -6%;
  background: radial-gradient(circle, var(--giris-tint-20), transparent 62%);
  animation: parildama 20s ease-in-out infinite alternate -6s;
}
@keyframes sahneDon {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
@keyframes parildama {
  from { transform: scale(1); opacity: 0.9; }
  to { transform: scale(1.12); opacity: 0.6; }
}

/* Split Screen Wrapper */
.giris-split-wrapper {
  position: relative;
  z-index: 1;
  display: flex;
  width: 100%;
  max-width: min(1180px, 100%);
  min-width: 0;
  max-height: calc(100dvh - 40px);
  background: rgba(15, 23, 42, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 28px;
  box-shadow: 0 25px 60px -15px rgba(0, 0, 0, 0.65);
  overflow: hidden;
  animation: sahneGiris 0.7s cubic-bezier(0.16, 1, 0.3, 1) both;
}
@keyframes sahneGiris {
  from { opacity: 0; transform: translateY(20px) scale(0.985); }
  to { opacity: 1; transform: none; }
}

[data-theme='light'] .giris-split-wrapper {
  background: rgba(255, 255, 255, 0.85);
  border-color: rgba(226, 232, 240, 0.8);
  box-shadow: 0 20px 50px -10px rgba(0, 0, 0, 0.08);
}

/* SOL: Hero & Vitrin */
.giris-hero-alani {
  flex: 1.15;
  padding: 26px 34px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  justify-content: safe center;
  overflow-y: auto;
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.7) 0%, rgba(15, 23, 42, 0.85) 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  scrollbar-width: thin;
}

[data-theme='light'] .giris-hero-alani {
  background: linear-gradient(135deg, rgba(245, 247, 250, 0.9) 0%, rgba(238, 242, 246, 0.95) 100%);
  border-right-color: rgba(226, 232, 240, 0.9);
}

/* Acik temada aksan vurgulari koyu tonla (okunabilirlik) */
[data-theme='light'] .hero-eyebrow {
  color: var(--giris-aksan-koyu);
}
[data-theme='light'] .hero-rotate-item {
  background: linear-gradient(90deg, var(--giris-aksan), var(--giris-aksan-koyu));
}
[data-theme='light'] .feature-ikon {
  color: var(--giris-aksan-koyu);
}
[data-theme='light'] .trust-badge i {
  color: var(--giris-aksan-koyu);
}
[data-theme='light'] .feature-item {
  background: rgba(255, 255, 255, 0.72);
  border-color: rgba(203, 213, 225, 0.7);
}
[data-theme='light'] .hero-features-title {
  color: var(--text-secondary);
}

.hero-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.hero-logo-box {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, var(--giris-aksan), var(--giris-aksan-koyu));
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px var(--giris-tint-40);
}
.hero-logo-icon {
  font-size: 22px;
  color: var(--accent-contrast, #ffffff);
}
.hero-brand-name {
  font-size: 22px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.5px;
}
.hero-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  background: var(--giris-tint-15);
  color: var(--giris-aksan-parlak);
  border: 1px solid var(--giris-tint-30);
  border-radius: 20px;
  margin-left: 8px;
}

.hero-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1.22;
  margin: 0 0 8px;
  letter-spacing: -0.5px;
}
.hero-desc {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.45;
  margin: 0 0 4px;
}
.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  font-weight: 700;
  letter-spacing: 0.6px;
  text-transform: uppercase;
  color: var(--giris-aksan-parlak);
  margin-bottom: 4px;
}
.hero-eyebrow i {
  font-size: 11px;
}

/* Ozellik listesi */
.hero-features {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.hero-features-title {
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.6px;
  text-transform: uppercase;
  color: var(--text-muted);
}
.hero-features-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}
.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 7px 9px;
  border-radius: 11px;
  background: rgba(148, 163, 184, 0.06);
  border: 1px solid rgba(148, 163, 184, 0.12);
  transition: transform 0.18s ease, border-color 0.18s ease;
}
.feature-item:hover {
  transform: translateY(-2px);
  border-color: var(--giris-tint-35);
}
.feature-ikon {
  width: 26px;
  height: 26px;
  flex-shrink: 0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--giris-tint-14);
  color: var(--giris-aksan-parlak);
  font-size: 12px;
}
.feature-metin {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.feature-metin strong {
  font-size: 12px;
  color: var(--text-primary);
  font-weight: 700;
}
.feature-metin span {
  font-size: 10.5px;
  color: var(--text-muted);
  line-height: 1.35;
}

/* Guven rozetleri */
.hero-trust {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  padding-top: 10px;
  border-top: 1px solid rgba(148, 163, 184, 0.12);
}
.trust-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 10px;
  border-radius: 20px;
  font-size: 10.5px;
  font-weight: 600;
  color: var(--text-secondary);
  background: rgba(148, 163, 184, 0.08);
  border: 1px solid rgba(148, 163, 184, 0.16);
}
.trust-badge i {
  color: var(--giris-aksan);
  font-size: 11px;
}

/* Hero donen baslik */
.hero-rotate {
  min-height: 22px;
  margin: 0 0 8px;
}
.hero-rotate-item {
  font-size: 15.5px;
  font-weight: 700;
  background: linear-gradient(90deg, var(--giris-aksan-parlak), var(--giris-aksan-parlak));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.rotate-fade-enter-active,
.rotate-fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}
.rotate-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.rotate-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* Canli urun vitrini */
.hero-preview-wrap {
  margin: 2px 0;
  transform: translate3d(var(--pxs, 0), var(--pys, 0), 0);
  transition: transform 0.25s ease-out;
  will-change: transform;
}

/* SAĞ: Form Alanı */
.giris-form-alani {
  flex: 0.95;
  padding: 44px 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.giris-kutu {
  width: 100%;
  max-width: 400px;
}

.giris-logo {
  text-align: center;
  margin-bottom: 24px;
}
.logo-icon {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, var(--giris-aksan), var(--giris-aksan-koyu));
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 14px;
  box-shadow: 0 8px 24px var(--giris-tint-35);
  overflow: hidden;
}
.sirket-logo {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.logo-icon i {
  font-size: 28px;
  color: var(--accent-contrast, #ffffff);
}
.giris-logo h2 {
  color: var(--text-primary);
  font-size: 22px;
  margin: 0 0 4px;
  font-weight: 700;
}
.alt-baslik {
  color: var(--text-secondary);
  font-size: 13px;
  margin: 0;
}

.giris-form {
  background: rgba(30, 41, 59, 0.45);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}
[data-theme='light'] .giris-form {
  background: #ffffff;
  border-color: rgba(226, 232, 240, 0.9);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.04);
}

.form-grup {
  margin-bottom: 15px;
}
.label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.form-grup label {
  display: block;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 4px;
}

.caps-lock-uyari {
  font-size: 11px;
  font-weight: 600;
  color: #f59e0b;
  display: flex;
  align-items: center;
  gap: 4px;
  animation: pulse-slow 1.5s infinite;
}

@keyframes pulse-slow {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.input-wrapper {
  position: relative;
}
.input-wrapper > i {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-muted);
  font-size: 16px;
  z-index: 2;
  pointer-events: none;
  transition: color 0.2s ease;
}
.input-wrapper :deep(.p-inputtext) {
  width: 100%;
  padding: 12px 14px 12px 40px !important;
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: 12px;
  color: var(--text-primary);
  font-size: 14.5px;
  min-height: 46px;
  transition: all 0.2s ease;
}
.input-wrapper :deep(.p-inputtext:focus) {
  border-color: var(--giris-aksan) !important;
  box-shadow: 0 0 0 4px var(--giris-tint-22) !important;
  outline: none;
}
.input-wrapper:focus-within > i {
  color: var(--giris-aksan);
}
.kod-input {
  text-align: center !important;
  letter-spacing: 2px;
  font-size: 18px;
  font-family: monospace;
  padding-right: 14px !important;
}

.sifre-toggle {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 16px;
  z-index: 2;
  padding: 4px;
  border-radius: 6px;
  transition: color 0.2s;
}
.sifre-toggle:hover {
  color: var(--text-primary);
}

.beni-hatirla {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  font-size: 13px;
  color: var(--text-secondary);
}

.giris-buton {
  width: 100%;
  padding: 12px;
  margin-top: 4px;
  background: linear-gradient(135deg, var(--giris-aksan), var(--giris-aksan-koyu));
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  color: var(--accent-contrast, #ffffff);
  transition: all 0.2s ease;
}
.giris-buton:hover {
  background: linear-gradient(135deg, var(--giris-aksan-koyu), var(--giris-aksan-koyu));
  transform: translateY(-1px);
  box-shadow: 0 6px 22px var(--giris-tint-40);
}

/* Dokunmatik: 300ms gecikme ve cift-dokunma zoom'unu engelle */
.giris-buton,
.rol-chip,
.sirket-secim-kart,
.sifre-toggle,
.geri-satir a,
.giris-alt-linkler a {
  touch-action: manipulation;
}

/* Hızlı Roller */
.hizli-roller {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed rgba(148, 163, 184, 0.15);
}
.hizli-rol-etiket {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 600;
}
.rol-chip {
  padding: 4px 10px;
  background: var(--giris-tint-08);
  border: 1px solid var(--giris-tint-20);
  border-radius: 14px;
  color: var(--text-primary);
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: all 0.15s;
}
.rol-chip:hover {
  background: var(--giris-tint-20);
  border-color: var(--giris-aksan);
  transform: translateY(-1px);
}
.rol-chip i {
  font-size: 10px;
  color: var(--giris-aksan);
}

.giris-alt-linkler {
  text-align: center;
  margin-top: 12px;
}
.giris-alt-linkler a {
  color: var(--text-muted);
  font-size: 13px;
  cursor: pointer;
  transition: color 0.15s;
}
.giris-alt-linkler a:hover {
  color: var(--giris-aksan);
}

/* Hata Kutusu */
.hata-kutu {
  margin-bottom: 16px;
  padding: 12px 14px;
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid rgba(239, 68, 68, 0.35);
  border-radius: 12px;
  color: #fca5a5;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 500;
}
.hata-kutu i {
  font-size: 16px;
  flex-shrink: 0;
}

/* Shake Animation */
.shake-enter-active {
  animation: shake 0.4s cubic-bezier(0.36, 0.07, 0.19, 0.97) both;
}
@keyframes shake {
  10%, 90% { transform: translate3d(-1px, 0, 0); }
  20%, 80% { transform: translate3d(2px, 0, 0); }
  30%, 50%, 70% { transform: translate3d(-3px, 0, 0); }
  40%, 60% { transform: translate3d(3px, 0, 0); }
}

/* Form Altı Mikro Güvenlik Rozetleri */
.form-guvenlik-rozetleri {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 14px;
  margin-top: 18px;
  font-size: 11px;
  color: var(--text-muted);
}
.form-guvenlik-rozetleri span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.form-guvenlik-rozetleri i {
  font-size: 10px;
  color: var(--giris-aksan);
}

.giris-footer {
  text-align: center;
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid rgba(148, 163, 184, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}
.giris-footer span {
  font-size: 11px;
  color: var(--text-muted);
}

/* 2FA & Kurulum */
.iki-fa-ikon,
.sirket-secim-ikon {
  width: 54px;
  height: 54px;
  margin: 0 auto 12px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.iki-fa-ikon {
  background: linear-gradient(135deg, var(--giris-aksan), var(--giris-aksan-koyu));
  box-shadow: 0 6px 20px var(--giris-tint-30);
}
.sirket-secim-ikon {
  background: linear-gradient(135deg, var(--giris-aksan), var(--giris-aksan-koyu));
  box-shadow: 0 6px 20px var(--giris-tint-30);
}
.iki-fa-ikon i,
.sirket-secim-ikon i {
  font-size: 24px;
  color: var(--accent-contrast, #ffffff);
}
.iki-fa-baslik {
  text-align: center;
  color: var(--text-primary);
  font-size: 17px;
  margin: 0 0 4px;
}
.iki-fa-alt {
  text-align: center;
  color: var(--text-secondary);
  font-size: 12.5px;
  margin: 0 0 16px;
}
.geri-satir {
  text-align: center;
  margin-top: 14px;
}
.geri-satir a {
  color: var(--text-muted);
  font-size: 13px;
  cursor: pointer;
}
.geri-satir a:hover {
  color: var(--text-primary);
}

.sirket-listesi {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.sirket-secim-buton {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px 14px;
  background: var(--bg-primary);
  border: 1px solid var(--border);
  border-radius: 12px;
  color: var(--text-primary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.sirket-secim-buton:hover {
  border-color: var(--giris-aksan);
  background: var(--giris-tint-08);
  transform: translateY(-1px);
}
.sirket-mini-logo {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
}
.sirket-mini-icon {
  font-size: 18px;
  color: var(--text-muted);
  flex-shrink: 0;
}
.sirket-ad {
  flex: 1;
  text-align: left;
  font-weight: 500;
}
.sirket-ok {
  color: var(--text-muted);
  font-size: 13px;
}
.sirket-yok {
  text-align: center;
  color: var(--text-secondary);
  font-size: 13px;
  padding: 16px 0;
}

.sifre-sifirla-panel {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 20px;
  margin-top: 14px;
}
.sifirla-ust {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.sifirla-ust i {
  font-size: 20px;
  color: var(--primary-color);
}
.sifirla-ust h3 {
  margin: 0;
  font-size: 15px;
  color: var(--text-primary);
  flex: 1;
}
.sifirla-ust a {
  color: var(--text-muted);
  font-size: 12.5px;
  cursor: pointer;
}
.sifirla-ust a:hover {
  color: var(--text-primary);
}
.sifre-sifirla-panel p {
  font-size: 12.5px;
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.4;
}

/* Mobil kompakt hero */
.giris-mobil-hero {
  display: none;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}
.mobil-mark {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--giris-aksan), var(--giris-aksan-koyu));
  color: var(--accent-contrast, #ffffff);
  font-size: 20px;
  flex-shrink: 0;
  box-shadow: 0 4px 16px var(--giris-tint-40);
}
.mobil-metin {
  flex: 1;
  min-width: 0;
}
.mobil-title {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: var(--text-primary);
}
.mobil-tagline {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--text-secondary);
}
.mobil-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  width: 100%;
}
.mobil-chips span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 10.5px;
  font-weight: 600;
  color: var(--text-secondary);
  background: rgba(148, 163, 184, 0.08);
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 14px;
  padding: 4px 9px;
}
.mobil-chips i {
  color: var(--giris-aksan);
  font-size: 10px;
}

/* Selamlama + son firma */
.giris-selam {
  text-align: center;
  font-size: 12.5px;
  color: var(--text-muted);
  margin: -8px 0 14px;
}
.son-sirket {
  color: var(--text-secondary);
}
.son-sirket strong {
  color: var(--text-primary);
  font-weight: 600;
}

/* Hareket azaltma tercihi (erisilebilirlik) */
@media (prefers-reduced-motion: reduce) {
  .sahne-mesh,
  .sahne-parilti {
    animation: none;
  }
  .giris-split-wrapper {
    animation: none;
  }
  .caps-lock-uyari {
    animation: none;
  }
  .hero-preview-wrap {
    transform: none;
    transition: none;
  }
  .rotate-fade-enter-active,
  .rotate-fade-leave-active {
    transition: none;
  }
}

/* Responsive Düzen */
@media (max-width: 1180px) {
  .hero-features-grid {
    grid-template-columns: 1fr;
  }
}

/* Kisa ekranlar: hero'yu sikistir, tum icerik kaydirmadan gorunsun */
@media (max-height: 900px) {
  .giris-hero-alani {
    padding: 20px 28px;
    gap: 8px;
  }
  .hero-brand {
    margin-bottom: 10px;
  }
  .hero-title {
    font-size: 21px;
  }
  .hero-desc {
    font-size: 12.5px;
  }
  .feature-metin span {
    display: none;
  }
  .feature-item {
    padding: 6px 9px;
  }
  .trust-badge {
    font-size: 10px;
    padding: 4px 9px;
  }
}

@media (max-height: 760px) {
  .giris-hero-alani {
    padding: 16px 24px;
    gap: 6px;
  }
  .hero-brand,
  .hero-rotate,
  .hero-eyebrow {
    display: none;
  }
  .hero-title {
    font-size: 19px;
    margin-bottom: 6px;
  }
  .hero-desc {
    display: none;
  }
  .hero-features {
    gap: 7px;
  }
  .hero-trust {
    padding-top: 8px;
  }
}

@media (max-width: 960px) {
  .giris-hero-alani {
    display: none;
  }
  .giris-mobil-hero {
    display: flex;
  }
  .giris-split-wrapper {
    max-width: 480px;
    border-radius: 20px;
    max-height: none;
  }
  .giris-form-alani {
    padding: 32px 24px;
  }
  /* iOS: odaklaninca otomatik zoom'u engelle (16px alti zoom tetikler) */
  .input-wrapper :deep(.p-inputtext) {
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .giris-sayfasi {
    padding:
      calc(16px + env(safe-area-inset-top))
      calc(12px + env(safe-area-inset-right))
      calc(16px + env(safe-area-inset-bottom))
      calc(12px + env(safe-area-inset-left));
  }
  .giris-form {
    padding: 18px;
  }
  .kurulum-iki-kolon {
    flex-direction: column;
    gap: 0;
  }
}
</style>
