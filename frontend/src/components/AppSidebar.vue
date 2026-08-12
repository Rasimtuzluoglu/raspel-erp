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
          :title="mobilMenuAcik ? 'Menüyü Kapat' : 'Menüyü Aç'"
          @click="mobilMenuAcik = !mobilMenuAcik"
        >
          <i :class="mobilMenuAcik ? 'pi pi-times' : 'pi pi-bars'" />
        </button>
      </div>
    </div>

    <div class="sidebar-menu">
      <div
        v-if="favoriMenuler.length"
        class="menu-grup"
      >
        {{ $t('common.favorites') }}
      </div>
      <router-link
        v-for="m in favoriMenuler"
        :key="m.path"
        :to="m.path"
        :class="{ active: $route.path === m.path || ($route.path.startsWith(m.path) && m.path !== '/') }"
        :title="m.label"
      >
        <i :class="m.icon" /><span>{{ m.label }}</span>
        <i
          class="pi pi-star"
          :class="{ favori: isFav(m.path) }"
          @click.prevent.stop="toggleFav(m.path)"
        />
      </router-link>

      <router-link
        to="/"
        :class="{ active: $route.path === '/' }"
        :title="$t('nav.dashboard')"
      >
        <i class="pi pi-home" /><span>{{ $t('nav.dashboard') }}</span>
      </router-link>

      <div
        class="menu-grup gelismis-mod-btn"
        @click="toggleGelismisMod"
      >
        <template v-if="gelismisMod">
          Temel Mod <i class="pi pi-chevron-up" />
        </template>
        <template v-else>
          Gelişmiş Mod <i class="pi pi-chevron-down" />
        </template>
      </div>

      <template
        v-for="(m, i) in gorunenMenuler"
        :key="m.path"
      >
        <div
          v-if="!i || m.grup !== gorunenMenuler[i - 1].grup"
          class="menu-grup"
        >
          {{ m.grup }}
        </div>
        <router-link
          :to="m.path"
          :class="{ active: $route.path === m.path || ($route.path.startsWith(m.path) && m.path !== '/') }"
          :title="m.label"
        >
          <i :class="m.icon" /><span>{{ m.label }}</span>
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
        <div class="admin-profile">
          <div class="admin-avatar">
            <img
              v-if="authStore.kullanici?.avatarUrl"
              :src="authStore.kullanici.avatarUrl"
              :alt="authStore.kullanici?.displayName"
              loading="lazy"
            >
            <span
              v-else
              class="avatar-fallback"
            >{{ authStore.kullanici?.displayName?.charAt(0) || 'U' }}</span>
          </div>
          <div class="admin-info">
            <span class="admin-name">{{ authStore.kullanici?.displayName || authStore.kullanici?.username || 'Kullanıcı' }}</span>
            <span class="admin-role">{{ authStore.kullanici?.role || 'USER' }}</span>
          </div>
          <button
            class="icon-action-btn logout-icon-btn"
            :title="$t('auth.logout')"
            @click="cikis"
          >
            <i class="pi pi-sign-out" />
          </button>
        </div>

        <div class="admin-actions">
          <BildirimZili class="bildirim-zili-kapsayici" />
          <ThemeSwitcher />
          <button class="icon-action-btn" title="Sifre Degistir" @click="$emit('open-password-modal')"><i class="pi pi-lock" /></button>
          <button class="icon-action-btn" title="Hesap Makinesi" @click="$emit('open-calculator')"><i class="pi pi-calculator" /></button>
          <button class="icon-action-btn" title="Doviz" @click="$emit('open-currency')"><i class="pi pi-money-bill" /></button>
          <button class="icon-action-btn" title="Araclar" @click="aracAcik = !aracAcik"><i class="pi pi-ellipsis-h" /></button>
        </div>
        <div v-if="aracAcik" class="admin-tools">
          <button class="icon-action-btn" title="KDV" @click="$emit('open-kdv')"><i class="pi pi-percentage" /></button>
          <button class="icon-action-btn" title="Taksit" @click="$emit('open-taksit')"><i class="pi pi-calendar" /></button>
          <button class="icon-action-btn" title="Kar Marj" @click="$emit('open-marj')"><i class="pi pi-chart-line" /></button>
        </div>
      </div>

      <div class="sidebar-credit">
        RasPel Co.
      </div>
    </div>

    <KisayolRehberi v-model:goster="rehberGoster" />
  </aside>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore.js'
import { sirketAPI } from '../api/index.js'
import BildirimZili from './BildirimZili.vue'
import ThemeSwitcher from './ThemeSwitcher.vue'
import KisayolRehberi from './KisayolRehberi.vue'
import { safeGet, safeSet } from '../utils/safeStorage.js'
import { useTheme } from '../composables/useTheme.js'

defineEmits(['open-search', 'open-password-modal', 'open-calculator', 'open-currency', 'open-kdv', 'open-taksit', 'open-marj', 'open-iban', 'open-tc'])

const router = useRouter()
const authStore = useAuthStore()

const mobilMenuAcik = ref(false)
const rehberGoster = ref(false)
const aracAcik = ref(false)
const gelismisMod = ref(safeGet('raspel_erp_gelismis_mod', false))

const toggleGelismisMod = () => {
  gelismisMod.value = !gelismisMod.value
  safeSet('raspel_erp_gelismis_mod', gelismisMod.value)
}

// Rota değiştiğinde mobil menüyü kapat
watch(() => router.currentRoute.value.path, () => {
  mobilMenuAcik.value = false
})

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
  { path: '/', label: 'Ana Sayfa', icon: 'pi pi-home', grup: '' },
  { path: '/muhasebe', label: 'Muhasebe', icon: 'pi pi-book', grup: 'Finans', gelismis: true },
  { path: '/cari-hesaplar', label: 'Cari', icon: 'pi pi-users', grup: 'Finans' },
  { path: '/faturalar', label: 'Faturalar', icon: 'pi pi-file', grup: 'Finans' },
  { path: '/bankalar', label: 'Banka', icon: 'pi pi-building', grup: 'Finans' },
  { path: '/kasa', label: 'Kasa', icon: 'pi pi-wallet', grup: 'Finans' },
  { path: '/banka-mutabakat', label: 'Banka Mutabakatı', icon: 'pi pi-link', grup: 'Finans', gelismis: true },
  { path: '/cek-senet', label: 'Çek/Senet', icon: 'pi pi-money-bill', grup: 'Finans', gelismis: true },
  { path: '/butceler', label: 'Bütçe', icon: 'pi pi-chart-bar', grup: 'Finans', admin: true, gelismis: true },
  { path: '/masraflar', label: 'Masraf', icon: 'pi pi-money-bill', grup: 'Finans', gelismis: true },
  { path: '/satislar', label: 'Satış', icon: 'pi pi-shopping-cart', grup: 'Ticaret', gelismis: true },
  { path: '/hizli-satis', label: 'Hızlı Satış', icon: 'pi pi-bolt', grup: 'Ticaret' },
  { path: '/crm', label: 'CRM', icon: 'pi pi-bullseye', grup: 'Ticaret', gelismis: true },
  { path: '/e-fatura', label: 'E-Fatura', icon: 'pi pi-file-pdf', grup: 'Ticaret', gelismis: true },
  { path: '/satinalma', label: 'Satın Alma', icon: 'pi pi-shopping-bag', grup: 'Ticaret', gelismis: true },
  { path: '/siparisler', label: 'Sipariş', icon: 'pi pi-receipt', grup: 'Ticaret', gelismis: true },
  { path: '/irsaliyeler', label: 'İrsaliye', icon: 'pi pi-truck', grup: 'Ticaret', gelismis: true },
  { path: '/fiyat-listesi', label: 'Fiyat Listesi', icon: 'pi pi-tag', grup: 'Ticaret', gelismis: true },
  { path: '/iadeler', label: 'İade', icon: 'pi pi-replay', grup: 'Ticaret', gelismis: true },
  { path: '/stoklar', label: 'Stok', icon: 'pi pi-box', grup: 'Envanter' },
  { path: '/kritik-stok', label: 'Kritik Stok', icon: 'pi pi-exclamation-triangle', grup: 'Envanter', gelismis: true },
  { path: '/toplu-stok', label: 'Toplu Stok', icon: 'pi pi-database', grup: 'Envanter', gelismis: true },
  { path: '/depolar', label: 'Depo', icon: 'pi pi-warehouse', grup: 'Envanter', gelismis: true },
  { path: '/stok-seriler', label: 'Seri/Lot', icon: 'pi pi-qrcode', grup: 'Envanter', gelismis: true },
  { path: '/stok-sayim', label: 'Stok Sayım', icon: 'pi pi-sort-alt', grup: 'Envanter', gelismis: true },
  { path: '/subeler', label: 'Şube', icon: 'pi pi-sitemap', grup: 'Yönetim', gelismis: true },
  { path: '/personel', label: 'Personel', icon: 'pi pi-id-card', grup: 'Yönetim' },
  { path: '/izinler', label: 'İzin', icon: 'pi pi-calendar', grup: 'Yönetim', gelismis: true },
  { path: '/projeler', label: 'Proje', icon: 'pi pi-folder', grup: 'Yönetim', gelismis: true },
  { path: '/maas-bordro', label: 'Maaş Bordro', icon: 'pi pi-credit-card', grup: 'Yönetim', admin: true, gelismis: true },
  { path: '/vardiyalar', label: 'Vardiya', icon: 'pi pi-clock', grup: 'Yönetim', gelismis: true },
  { path: '/sirketler', label: 'Şirket', icon: 'pi pi-building', grup: 'Sistem', admin: true, gelismis: true },
  { path: '/donemler', label: 'Dönem', icon: 'pi pi-calendar', grup: 'Sistem', gelismis: true },
  { path: '/kullanicilar', label: 'Kullanıcı', icon: 'pi pi-user', grup: 'Sistem', admin: true, gelismis: true },
  { path: '/yetki-yonetimi', label: 'Yetkiler', icon: 'pi pi-key', grup: 'Sistem', admin: true, gelismis: true },
  { path: '/kategoriler', label: 'Kategori', icon: 'pi pi-tags', grup: 'Sistem', gelismis: true },
  { path: '/notlar', label: 'Notlar', icon: 'pi pi-pen-to-square', grup: 'Sistem' },
  { path: '/veri-aktar', label: 'Veri Aktar', icon: 'pi pi-upload', grup: 'Sistem', gelismis: true },
  { path: '/kullanim-sartlari', label: 'Kullanım Şartları', icon: 'pi pi-file-o', grup: 'Sistem', gelismis: true },
  { path: '/gizlilik-politikasi', label: 'Gizlilik', icon: 'pi pi-shield', grup: 'Sistem', gelismis: true },
  { path: '/hesap-ayarlari', label: 'Hesap Ayarları', icon: 'pi pi-cog', grup: 'Sistem', gelismis: true },
  { path: '/yedekler', label: 'Yedek', icon: 'pi pi-save', grup: 'Sistem', admin: true, gelismis: true },
  { path: '/raporlar', label: 'Rapor', icon: 'pi pi-chart-bar', grup: 'Rapor' },
  { path: '/vergi-raporlari', label: 'KDV & BA/BS', icon: 'pi pi-file-edit', grup: 'Rapor', gelismis: true },
  { path: '/anomaliler', label: 'Anomali', icon: 'pi pi-exclamation-triangle', grup: 'Rapor', gelismis: true },
  { path: '/hareketler', label: 'Hareket', icon: 'pi pi-chart-line', grup: 'Rapor' },
  { path: '/denetim', label: 'Denetim', icon: 'pi pi-shield', grup: 'Rapor', admin: true, gelismis: true }
]

const gorunenMenuler = computed(() => {
  return tumMenuler.filter(m => {
    if (m.path === '/') return false
    if (!gelismisMod.value && m.gelismis) return false
    if (m.admin && !authStore.isAdmin) return false
    return true
  })
})

const favoriMenuler = computed(() => tumMenuler.filter(m => favoriler.value.includes(m.path) && (!m.admin || authStore.kullanici?.role === 'ADMIN')))

const sirketLogo = ref(null)

watch(() => authStore.sirketId, async (id) => {
  if (id) {
    try {
      const r = await sirketAPI.getById(id)
      sirketLogo.value = r.data?.logoUrl || null
    } catch { sirketLogo.value = null }
  } else {
    sirketLogo.value = null
  }
}, { immediate: true })

const cikis = () => {
  authStore.cikisYap()
  window.location.replace('/giris')
}

onMounted(() => {
  initTheme()
})
</script>