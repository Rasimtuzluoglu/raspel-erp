<template>
  <aside
    class="sidebar"
    :class="{ 'mobil-acik': mobilMenuAcik }"
  >
    <div class="sidebar-ust">
      <router-link
        to="/"
        class="brand"
      >
        <img
          v-if="sirketLogo"
          :src="sirketLogo"
          class="brand-logo"
          alt="logo"
          loading="lazy"
        >
        <div
          v-else
          class="brand-icon"
        >
          <i class="pi pi-calculator" />
        </div>
        <div class="brand-text">
          <span class="brand-title">RasPel</span>
          <span
            v-if="authStore.sirketAdi"
            class="brand-company"
          >{{ authStore.sirketAdi }}</span>
        </div>
      </router-link>
      <div class="sidebar-top-actions">
        <button
          class="hamburger-btn"
          :title="mobilMenuAcik ? $t('nav.menuKapat') : $t('nav.menuAc')"
          @click="mobilMenuAcik = !mobilMenuAcik"
        >
          <i :class="mobilMenuAcik ? 'pi pi-times' : 'pi pi-bars'" />
        </button>
      </div>
    </div>

    <div class="sidebar-menu">
      <div
        v-if="favoriMenuler && favoriMenuler.length"
        class="menu-grup"
      >
        {{ $t('common.favorites') }}
      </div>
      <router-link
        v-for="m in favoriMenuler"
        :key="m.path"
        :to="m.path"
        :class="{ active: menuAktif(m.path) }"
        :title="$t(m.labelKey)"
      >
        <i :class="m.icon" /><span>{{ $t(m.labelKey) }}</span>
        <i
          class="pi pi-star"
          :class="{ favori: isFav(m.path) }"
          @click.prevent.stop="toggleFav(m.path)"
        />
      </router-link>

      <router-link
        v-if="!authStore.isDriver"
        to="/"
        :class="{ active: menuAktif('/') }"
        :title="$t('nav.dashboard')"
      >
        <i class="pi pi-home" /><span>{{ $t('nav.dashboard') }}</span>
      </router-link>

      <div
        class="menu-grup gelismis-mod-btn"
        @click="toggleGelismisMod"
      >
        <template v-if="gelismisMod">
          {{ $t('nav.temelMod') }} <i class="pi pi-chevron-up" />
        </template>
        <template v-else>
          {{ $t('nav.gelismisMod') }} <i class="pi pi-chevron-down" />
        </template>
      </div>

      <template
        v-for="(m, i) in gorunenMenuler"
        :key="m.path"
      >
        <div
          v-if="!i || m.grupKey !== gorunenMenuler[i - 1].grupKey"
          class="menu-grup"
        >
          {{ $t(m.grupKey) }}
        </div>
        <router-link
          :to="m.path"
          :class="{ active: menuAktif(m.path) }"
          :title="$t(m.labelKey)"
        >
          <i :class="m.icon" /><span>{{ $t(m.labelKey) }}</span>
          <span
            v-if="m.path === '/onaylar' && onaySayisi"
            class="menu-sayac"
          >{{ onaySayisi }}</span>
          <i
            class="pi pi-star"
            :class="{ favori: isFav(m.path) }"
            @click.prevent.stop="toggleFav(m.path)"
          />
        </router-link>
      </template>
    </div>

    <div class="sidebar-alt">
      <div
        class="sidebar-arama"
        :title="$t('common.search') + ' (Ctrl+K)'"
        @click="$emit('open-search')"
      >
        <i class="pi pi-search" />
        <span>{{ $t('common.search') }}</span>
        <kbd>Ctrl+K</kbd>
      </div>

      <div class="admin-card">
        <button
          v-if="authStore.sirketAdi || authStore.companyName"
          class="firma-secici"
          :title="$t('nav.sirketDegistirBaslik')"
          @click="sirketDegistirAc"
        >
          <i class="pi pi-building" />
          <span class="firma-secici-ad">{{ authStore.sirketAdi || authStore.companyName }}</span>
          <i class="pi pi-chevron-down firma-secici-ok" />
        </button>

        <div class="admin-profile">
          <div class="admin-avatar">
            <img
              v-if="authStore?.kullanici?.avatarUrl"
              :src="authStore?.kullanici?.avatarUrl"
              :alt="authStore?.kullanici?.displayName"
              loading="lazy"
            >
            <span
              v-else
              class="avatar-fallback"
            >{{ authStore?.kullanici?.displayName?.charAt(0) || 'U' }}</span>
          </div>
          <div class="admin-info">
            <span class="admin-name">{{
              authStore?.kullanici?.displayName || authStore?.kullanici?.username || $t('nav.user')
            }}</span>
            <span class="admin-role">{{ authStore?.kullanici?.role || 'USER' }}</span>
          </div>
          <button
            class="icon-action-btn logout-icon-btn"
            :title="$t('auth.logout')"
            :aria-label="$t('auth.logout')"
            @click="cikis"
          >
            <i class="pi pi-sign-out" />
          </button>
        </div>

        <div class="admin-actions">
          <BildirimZili class="bildirim-zili-kapsayici" />
          <ThemeSwitcher />
          <button
            class="icon-action-btn"
            :class="{ 'sunum-acik': sunumAktif }"
            :title="sunumAktif ? $t('sunumModu.kapat') : $t('sunumModu.ac')"
            :aria-label="sunumAktif ? $t('sunumModu.kapat') : $t('sunumModu.ac')"
            :aria-pressed="sunumAktif"
            @click="sunumDegistir()"
          >
            <i :class="sunumAktif ? 'pi pi-eye-slash' : 'pi pi-eye'" />
          </button>
          <span
            v-if="sunumAktif"
            class="sunum-rozet"
            :title="$t('sunumModu.ipucu')"
          >
            <i class="pi pi-eye-slash" /> {{ $t('sunumModu.aktif') }}
          </span>
          <button
            class="icon-action-btn"
            :title="$t('nav.sifreDegistir')"
            :aria-label="$t('nav.sifreDegistir')"
            @click="$emit('open-password-modal')"
          >
            <i class="pi pi-lock" />
          </button>
          <button
            class="icon-action-btn"
            :title="$t('nav.hesapMakinesi')"
            :aria-label="$t('nav.hesapMakinesi')"
            @click="$emit('open-calculator')"
          >
            <i class="pi pi-calculator" />
          </button>
          <button
            class="icon-action-btn"
            :title="$t('nav.doviz')"
            :aria-label="$t('nav.doviz')"
            @click="$emit('open-currency')"
          >
            <i class="pi pi-money-bill" />
          </button>
          <button
            class="icon-action-btn"
            :title="$t('nav.araclar')"
            :aria-label="$t('nav.araclar')"
            :aria-expanded="aracAcik"
            @click="aracAcik = !aracAcik"
          >
            <i class="pi pi-ellipsis-h" />
          </button>
        </div>
        <div
          v-if="aracAcik"
          class="admin-tools"
        >
          <button
            class="icon-action-btn"
            :title="$t('nav.kdv')"
            @click="$emit('open-kdv')"
          >
            <i class="pi pi-percentage" />
          </button>
          <button
            class="icon-action-btn"
            :title="$t('nav.taksit')"
            @click="$emit('open-taksit')"
          >
            <i class="pi pi-calendar" />
          </button>
          <button
            class="icon-action-btn"
            :title="$t('nav.karMarji')"
            @click="$emit('open-marj')"
          >
            <i class="pi pi-chart-line" />
          </button>
        </div>
      </div>

      <div class="sidebar-credit">
        RasPel Co.
      </div>
    </div>

    <Dialog
      v-model:visible="sirketDialogAcik"
      :header="$t('nav.sirketDegistir')"
      :modal="true"
      :closable="!sirketDegistiriliyor"
      :style="{ width: '440px' }"
    >
      <div
        v-if="sirketSecenekleri.length === 0"
        class="sirket-degistir-bos"
      >
        <i class="pi pi-building" />
        <p>{{ $t('nav.sirketListesiYuklenemedi') }}</p>
      </div>
      <div
        v-else-if="sirketDegistiriliyor"
        class="sirket-degistir-bos"
      >
        <i class="pi pi-spin pi-spinner" />
        <p>{{ $t('nav.sirketDegistiriliyor') }}</p>
      </div>
      <div class="sirket-degistir-liste">
        <button
          v-for="s in sirketSecenekleri"
          :key="s.id"
          class="sirket-degistir-kart"
          :class="{ aktif: s.id === authStore.sirketId }"
          :disabled="sirketDegistiriliyor"
          @click="sirketDegistir(s)"
        >
          <div class="sirket-degistir-bilgi">
            <i class="pi pi-building" />
            <div>
              <span class="sirket-degistir-ad">{{ s.ad }}</span>
              <span
                v-if="s.vergiNo"
                class="sirket-degistir-vkn"
              >{{ $t('nav.vkn') }}: {{ s.vergiNo }}</span>
            </div>
          </div>
          <i
            v-if="s.id === authStore.sirketId"
            class="pi pi-check-circle sirket-degistir-aktif"
          />
          <i
            v-else
            class="pi pi-arrow-right"
          />
        </button>
      </div>
    </Dialog>
  </aside>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore.js'
import { sirketAPI } from '../api/index.js'
import { personelIzinAPI, satinalmaTalepAPI, siparisAPI } from '../api/index.js'
import BildirimZili from './BildirimZili.vue'
import ThemeSwitcher from './ThemeSwitcher.vue'
import { safeGet, safeSet } from '../utils/safeStorage.js'
import { useTheme } from '../composables/useTheme.js'
import { useSunumModu } from '../composables/useSunumModu.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useI18n } from 'vue-i18n'

defineEmits([
  'open-search',
  'open-password-modal',
  'open-calculator',
  'open-currency',
  'open-kdv',
  'open-taksit',
  'open-marj',
  'open-iban',
  'open-tc'
])

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const { aktif: sunumAktif, degistir: sunumDegistir } = useSunumModu()
const toastBildirim = useToastBildirim()
const { t } = useI18n()

const mobilMenuAcik = ref(false)
const aracAcik = ref(false)
const gelismisMod = ref(safeGet('raspel_erp_gelismis_mod', false))
const sirketDialogAcik = ref(false)
const sirketSecenekleri = ref([])
const sirketDegistiriliyor = ref(false)

const sirketDegistirAc = async () => {
  sirketSecenekleri.value = await authStore.sirketlerim()
  sirketDialogAcik.value = true
}

const sirketDegistir = async (sirket) => {
  if (sirket.id === authStore.sirketId) {
    sirketDialogAcik.value = false
    return
  }
  sirketDegistiriliyor.value = true
  try {
    await authStore.sirketDegistir(sirket.id)
    sirketDialogAcik.value = false
    // Aktif şirket değişti; verilerin tazelenmesi için tam yenileme
    window.location.reload()
  } catch (err) {
    const mesaj = err?.response?.data?.message || err?.message || t('nav.sirketDegistirilemedi')
    toastBildirim.hata(mesaj)
  } finally {
    sirketDegistiriliyor.value = false
  }
}

const toggleGelismisMod = () => {
  gelismisMod.value = !gelismisMod.value
  safeSet('raspel_erp_gelismis_mod', gelismisMod.value)
}

// Rota değiştiğinde mobil menüyü kapat
watch(
  () => router.currentRoute.value.path,
  () => {
    mobilMenuAcik.value = false
  }
)

const { initTheme } = useTheme()

const favoriler = ref(safeGet('raspel_erp_favorites', []))
watch(favoriler, (f) => safeSet('raspel_erp_favorites', f), { deep: true })

const toggleFav = (path) => {
  const idx = favoriler.value.indexOf(path)
  if (idx > -1) favoriler.value.splice(idx, 1)
  else favoriler.value.push(path)
}
const isFav = (path) => favoriler.value.includes(path)

const tumMenuler = [
  { path: '/', labelKey: 'nav.dashboard', icon: 'pi pi-home', grupKey: '' },
  { path: '/sohbet', labelKey: 'nav.sohbet', icon: 'pi pi-comments', grupKey: '' },
  { path: '/ajanda', labelKey: 'nav.ajanda', icon: 'pi pi-calendar', grupKey: '' },
  { path: '/onaylar', labelKey: 'nav.onaylar', icon: 'pi pi-check-circle', grupKey: '' },
  { path: '/belgeler', labelKey: 'nav.belgeler', icon: 'pi pi-folder-open', grupKey: '' },
  { path: '/sistem-durum', labelKey: 'nav.sistemDurum', icon: 'pi pi-server', grupKey: '', admin: true },
  { path: '/muhasebe', labelKey: 'nav.muhasebe', icon: 'pi pi-book', grupKey: 'nav.finans', gelismis: true },
  { path: '/cari-hesaplar', labelKey: 'nav.cari', icon: 'pi pi-users', grupKey: 'nav.finans' },
  { path: '/faturalar', labelKey: 'nav.faturalar', icon: 'pi pi-file', grupKey: 'nav.finans' },
  { path: '/tekrarlayan-faturalar', labelKey: 'nav.tekrarlayanFaturalar', icon: 'pi pi-sync', grupKey: 'nav.finans' },
  { path: '/bankalar', labelKey: 'nav.banka', icon: 'pi pi-building', grupKey: 'nav.finans' },
  { path: '/kasa', labelKey: 'nav.kasa', icon: 'pi pi-wallet', grupKey: 'nav.finans' },
  { path: '/banka-mutabakat', labelKey: 'nav.bankaMutabakat', icon: 'pi pi-link', grupKey: 'nav.finans', gelismis: true },
  { path: '/cek-senet', labelKey: 'nav.ceksenet', icon: 'pi pi-money-bill', grupKey: 'nav.finans', gelismis: true },
  { path: '/tahsilat', labelKey: 'nav.tahsilat', icon: 'pi pi-dollar', grupKey: 'nav.finans' },
  { path: '/taksit-takvimi', labelKey: 'nav.taksitTakvimi', icon: 'pi pi-calendar', grupKey: 'nav.finans' },
  { path: '/pos-terminalleri', labelKey: 'nav.posTerminalleri', icon: 'pi pi-credit-card', grupKey: 'nav.finans', gelismis: true },
  { path: '/butceler', labelKey: 'nav.butce', icon: 'pi pi-chart-bar', grupKey: 'nav.finans', admin: true, gelismis: true },
  { path: '/masraflar', labelKey: 'nav.masraf', icon: 'pi pi-money-bill', grupKey: 'nav.finans', gelismis: true },
  { path: '/satislar', labelKey: 'nav.satis', icon: 'pi pi-shopping-cart', grupKey: 'nav.ticaret', gelismis: true },
  { path: '/hizli-satis', labelKey: 'nav.hizliSatis', icon: 'pi pi-bolt', grupKey: 'nav.ticaret' },
  { path: '/saha-portali', labelKey: 'nav.sahaPortali', icon: 'pi pi-compass', grupKey: 'nav.ticaret' },
  { path: '/teklifler', labelKey: 'nav.teklifler', icon: 'pi pi-file-edit', grupKey: 'nav.ticaret' },
  { path: '/crm', labelKey: 'nav.crm', icon: 'pi pi-bullseye', grupKey: 'nav.ticaret', gelismis: true },
  { path: '/crm-merkezi', labelKey: 'nav.crmMerkezi', icon: 'pi pi-users', grupKey: 'nav.ticaret', gelismis: true },
  { path: '/adres-defteri', labelKey: 'nav.adresDefteri', icon: 'pi pi-address-book', grupKey: 'nav.ticaret' },
  { path: '/e-fatura', labelKey: 'nav.eFatura', icon: 'pi pi-file-pdf', grupKey: 'nav.ticaret', gelismis: true },
  { path: '/satinalma', labelKey: 'nav.satinalma', icon: 'pi pi-shopping-bag', grupKey: 'nav.ticaret', gelismis: true },
  { path: '/siparisler', labelKey: 'nav.siparis', icon: 'pi pi-receipt', grupKey: 'nav.ticaret', gelismis: true },
  { path: '/siparis-takip', labelKey: 'nav.siparisTakip', icon: 'pi pi-sitemap', grupKey: 'nav.ticaret' },
  { path: '/teslimatlar', labelKey: 'nav.teslimatlar', icon: 'pi pi-truck', grupKey: 'nav.ticaret' },
  { path: '/irsaliyeler', labelKey: 'nav.irsaliye', icon: 'pi pi-truck', grupKey: 'nav.ticaret', gelismis: true },
  { path: '/fiyat-listesi', labelKey: 'nav.fiyatListesi', icon: 'pi pi-tag', grupKey: 'nav.ticaret', gelismis: true },
  { path: '/iskonto-kurallari', labelKey: 'nav.iskontoKurallari', icon: 'pi pi-percentage', grupKey: 'nav.ticaret', gelismis: true },
  { path: '/iadeler', labelKey: 'nav.iade', icon: 'pi pi-replay', grupKey: 'nav.ticaret', gelismis: true },
  { path: '/stoklar', labelKey: 'nav.stok', icon: 'pi pi-box', grupKey: 'nav.envanter' },
  { path: '/kritik-stok', labelKey: 'nav.kritikStok', icon: 'pi pi-exclamation-triangle', grupKey: 'nav.envanter', gelismis: true },
  { path: '/toplu-stok', labelKey: 'nav.topluStok', icon: 'pi pi-database', grupKey: 'nav.envanter', gelismis: true },
  { path: '/depolar', labelKey: 'nav.depo', icon: 'pi pi-warehouse', grupKey: 'nav.envanter', gelismis: true },
  { path: '/stok-seriler', labelKey: 'nav.serilot', icon: 'pi pi-qrcode', grupKey: 'nav.envanter', gelismis: true },
  { path: '/stok-sayim', labelKey: 'nav.stokSayim', icon: 'pi pi-sort-alt', grupKey: 'nav.envanter', gelismis: true },
  { path: '/stok-duzeltmeler', labelKey: 'nav.stokDuzeltmeler', icon: 'pi pi-sliders-h', grupKey: 'nav.envanter', gelismis: true },
  { path: '/uretim', labelKey: 'nav.uretim', icon: 'pi pi-cog', grupKey: 'nav.envanter' },
  { path: '/subeler', labelKey: 'nav.sube', icon: 'pi pi-sitemap', grupKey: 'nav.yonetim', gelismis: true },
  { path: '/personel', labelKey: 'nav.personel', icon: 'pi pi-id-card', grupKey: 'nav.yonetim' },
  { path: '/izinler', labelKey: 'nav.izin', icon: 'pi pi-calendar', grupKey: 'nav.yonetim', gelismis: true },
  { path: '/projeler', labelKey: 'nav.proje', icon: 'pi pi-folder', grupKey: 'nav.yonetim', gelismis: true },
  {
    path: '/maas-bordro',
    labelKey: 'nav.maasBordro',
    icon: 'pi pi-credit-card',
    grupKey: 'nav.yonetim',
    admin: true,
    gelismis: true
  },
  { path: '/vardiyalar', labelKey: 'nav.vardiya', icon: 'pi pi-clock', grupKey: 'nav.yonetim', gelismis: true },
  { path: '/sirketler', labelKey: 'nav.sirket', icon: 'pi pi-building', grupKey: 'nav.sistem', admin: true, gelismis: true },
  { path: '/yeni-yil-sihirbazi', labelKey: 'nav.yeniYil', icon: 'pi pi-sparkles', grupKey: 'nav.sistem', admin: true, gelismis: true },
  { path: '/donemler', labelKey: 'nav.donem', icon: 'pi pi-calendar', grupKey: 'nav.sistem', gelismis: true },
  { path: '/kullanicilar', labelKey: 'nav.kullanici', icon: 'pi pi-user', grupKey: 'nav.sistem', admin: true, gelismis: true },
  { path: '/yetki-yonetimi', labelKey: 'nav.yetkiler', icon: 'pi pi-key', grupKey: 'nav.sistem', admin: true, gelismis: true },
  { path: '/kategoriler', labelKey: 'nav.kategori', icon: 'pi pi-tags', grupKey: 'nav.sistem', gelismis: true },
  { path: '/notlar', labelKey: 'nav.notlar', icon: 'pi pi-pen-to-square', grupKey: 'nav.sistem' },
  { path: '/veri-aktar', labelKey: 'nav.veriAktar', icon: 'pi pi-upload', grupKey: 'nav.sistem', gelismis: true },
  { path: '/kullanim-sartlari', labelKey: 'nav.kullanimSartlari', icon: 'pi pi-file', grupKey: 'nav.sistem', gelismis: true },
  { path: '/gizlilik-politikasi', labelKey: 'nav.gizlilik', icon: 'pi pi-shield', grupKey: 'nav.sistem', gelismis: true },
  { path: '/hesap-ayarlari', labelKey: 'nav.hesapAyarlari', icon: 'pi pi-cog', grupKey: 'nav.sistem', gelismis: true },
  { path: '/yedekler', labelKey: 'nav.yedek', icon: 'pi pi-save', grupKey: 'nav.sistem', admin: true, gelismis: true },
  { path: '/yonetici-kokpiti', labelKey: 'nav.yoneticiKokpiti', icon: 'pi pi-bolt', grupKey: 'nav.rapor', admin: true },
    { path: '/raporlar', labelKey: 'nav.rapor', icon: 'pi pi-chart-bar', grupKey: 'nav.rapor' },
    { path: '/raporlar/karlilik-analizi', labelKey: 'nav.karlilikAnalizi', icon: 'pi pi-chart-pie', grupKey: 'nav.rapor' },
    { path: '/raporlar/fatura-gecmis', labelKey: 'nav.faturaGecmisRaporu', icon: 'pi pi-history', grupKey: 'nav.rapor' },
  { path: '/vergi-raporlari', labelKey: 'nav.vergiRaporlari', icon: 'pi pi-file-edit', grupKey: 'nav.rapor', gelismis: true },
  { path: '/anomaliler', labelKey: 'nav.anomaliler', icon: 'pi pi-exclamation-triangle', grupKey: 'nav.rapor', gelismis: true },
  { path: '/hareketler', labelKey: 'nav.hareket', icon: 'pi pi-chart-line', grupKey: 'nav.rapor' },
  { path: '/denetim', labelKey: 'nav.denetim', icon: 'pi pi-shield', grupKey: 'nav.rapor', admin: true, gelismis: true }
]

const gorunenMenuler = computed(() => {
  return tumMenuler.filter((m) => {
    if (m.path === '/') return false
    
    if (authStore.isDriver) {
      const driverErisilebilir = ['/teslimatlar', '/saha-portali', '/sohbet', '/notlar', '/belgeler', '/hesap-ayarlari']
      if (!driverErisilebilir.includes(m.path)) return false
    }

    if (authStore.isSaha) {
      const sahaErisilebilir = [
        '/saha-portali',
        '/stoklar',
        '/sohbet',
        '/ajanda',
        '/belgeler',
        '/notlar',
        '/hesap-ayarlari'
      ]
      if (!sahaErisilebilir.includes(m.path)) return false
    }

    if (!gelismisMod.value && m.gelismis) return false
    if (m.admin && !authStore.isAdmin) return false
    return true
  })
})

const favoriMenuler = computed(() =>
  tumMenuler.filter((m) => favoriler.value.includes(m.path) && (!m.admin || authStore?.kullanici?.role === 'ADMIN'))
)

/**
 * Aktif menüyü en uzun yol eşleşmesine göre belirler. Böylece
 * /raporlar/karlilik-analizi açıkken /raporlar da aktif görünmez.
 */
const aktifYol = computed(() => {
  const yol = route.path
  let enIyi = yol === '/' ? '/' : ''
  for (const m of tumMenuler) {
    if (m.path !== '/' && (yol === m.path || yol.startsWith(m.path + '/')) && m.path.length > enIyi.length) {
      enIyi = m.path
    }
  }
  return enIyi
})

const menuAktif = (path) => aktifYol.value === path

const sirketLogo = ref(null)

watch(
  () => authStore.sirketId,
  async (id) => {
    if (id) {
      try {
        const r = await sirketAPI.getById(id)
        sirketLogo.value = r.data?.logoUrl || null
      } catch {
        sirketLogo.value = null
      }
    } else {
      sirketLogo.value = null
    }
  },
  { immediate: true }
)

const cikis = () => {
  authStore.cikisYap()
  router.push({ name: 'Giris' })
}

onMounted(() => {
  initTheme()
  // Onay sayaçları yalnızca ofis kullanıcıları için anlamlıdır; şoför/saha
  // kullanıcılarında bu uçlar 403 döndürdüğü için hiç çağrılmaz.
  if (!authStore.isDriver && !authStore.isSaha) {
    onaySayisiniYukle()
  }
})

const onaySayisi = ref(0)

const onaySayisiniYukle = async () => {
  try {
    const [iRes, tRes, sRes] = await Promise.all([
      personelIzinAPI.getAll(),
      satinalmaTalepAPI.getAll(),
      siparisAPI.getAll({ size: 100 })
    ])
    const izinler = unwrapList(iRes)
    const talepler = unwrapList(tRes)
    const siparisler = unwrapList(sRes)
    onaySayisi.value =
      izinler.filter((i) => i.durum === 'BEKLEMEDE').length +
      talepler.filter((t) => t.durum === 'TASLAK').length +
      siparisler.filter((s) => s.durum === 'BEKLIYOR').length
  } catch {
    onaySayisi.value = 0
  }
}
</script>

<style scoped>
.menu-sayac {
  margin-left: auto;
  background: #ef4444;
  color: #fff;
  border-radius: 10px;
  min-width: 20px;
  height: 20px;
  font-size: 11px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
}

.firma-secici {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 10px;
  margin-bottom: 8px;
  background: var(--accent-soft);
  border: 1px solid var(--accent-soft-strong);
  border-radius: 8px;
  color: var(--text-primary);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.firma-secici:hover {
  background: var(--accent-soft-strong);
}
.firma-secici i:first-child {
  color: var(--accent);
  font-size: 14px;
}
.firma-secici-ad {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: left;
}
.firma-secici-ok {
  font-size: 11px;
  color: var(--text-muted);
}

.sirket-degistir-liste {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.sirket-degistir-kart {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  padding: 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  color: var(--text-primary);
  cursor: pointer;
  transition: all 0.15s;
}
.sirket-degistir-kart:hover {
  border-color: var(--accent);
}
.sirket-degistir-kart.aktif {
  border-color: var(--accent);
  background: rgba(59, 130, 246, 0.08);
}
.sirket-degistir-bilgi {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.sirket-degistir-bilgi > i {
  color: var(--accent);
  font-size: 16px;
}
.sirket-degistir-bilgi > div {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.sirket-degistir-ad {
  font-size: 13.5px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sirket-degistir-vkn {
  font-size: 11px;
  color: var(--text-muted);
}
.sirket-degistir-aktif {
  color: #10b981;
  font-size: 16px;
}
.sirket-degistir-bos {
  text-align: center;
  padding: 24px;
  color: var(--text-muted);
}
.sirket-degistir-bos i {
  font-size: 28px;
  display: block;
  margin-bottom: 8px;
}
</style>
