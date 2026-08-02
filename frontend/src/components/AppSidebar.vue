<template>
  <aside class="sidebar" :class="{ 'mobil-acik': mobilMenuAcik }">
    <div class="sidebar-ust">
      <router-link to="/" class="brand">
        <img v-if="sirketLogo" :src="sirketLogo" class="brand-logo" alt="logo" />
        <div v-else class="brand-icon"><i class="pi pi-calculator"></i></div>
        <div class="brand-text">
          <span class="brand-title">RasPel</span>
          <span class="brand-company" v-if="authStore.sirketAdi">{{ authStore.sirketAdi }}</span>
        </div>
      </router-link>
      <div class="sidebar-top-actions">
        <button class="hamburger-btn" @click="mobilMenuAcik = !mobilMenuAcik" :title="mobilMenuAcik ? 'Menüyü Kapat' : 'Menüyü Aç'">
          <i :class="mobilMenuAcik ? 'pi pi-times' : 'pi pi-bars'"></i>
        </button>
      </div>
    </div>

    <div class="sidebar-menu">
      <div v-if="favoriMenuler.length" class="menu-grup">{{ $t('common.favorites') }}</div>
      <router-link
        v-for="m in favoriMenuler"
        :key="m.path"
        :to="m.path"
        :class="{ active: $route.path === m.path || ($route.path.startsWith(m.path) && m.path !== '/') }"
        :title="m.label"
      >
        <i :class="m.icon"></i><span>{{ m.label }}</span>
        <i class="pi pi-star" :class="{ favori: isFav(m.path) }" @click.prevent.stop="toggleFav(m.path)"></i>
      </router-link>

      <router-link to="/" :class="{ active: $route.path === '/' }" :title="$t('nav.dashboard')"><i class="pi pi-home"></i><span>{{ $t('nav.dashboard') }}</span></router-link>
      
      <div class="menu-grup">{{ $t('nav.finans') }}</div>
      <router-link to="/muhasebe" :class="{ active: $route.path.startsWith('/muhasebe') }" title="Genel Muhasebe"><i class="pi pi-book"></i><span>Muhasebe</span><i class="pi pi-star" :class="{ favori: isFav('/muhasebe') }" @click.prevent.stop="toggleFav('/muhasebe')"></i></router-link>
      <router-link to="/cari-hesaplar" :class="{ active: $route.path === '/cari-hesaplar' }" :title="$t('nav.cari')"><i class="pi pi-users"></i><span>{{ $t('nav.cari') }}</span><i class="pi pi-star" :class="{ favori: isFav('/cari-hesaplar') }" @click.prevent.stop="toggleFav('/cari-hesaplar')"></i></router-link>
      <router-link to="/faturalar" :class="{ active: $route.path.startsWith('/faturalar') }" :title="$t('nav.faturalar')"><i class="pi pi-file"></i><span>{{ $t('nav.faturalar') }}</span><i class="pi pi-star" :class="{ favori: isFav('/faturalar') }" @click.prevent.stop="toggleFav('/faturalar')"></i></router-link>
      <router-link to="/bankalar" :class="{ active: $route.path === '/bankalar' }" :title="$t('nav.banka')"><i class="pi pi-building"></i><span>{{ $t('nav.banka') }}</span><i class="pi pi-star" :class="{ favori: isFav('/bankalar') }" @click.prevent.stop="toggleFav('/bankalar')"></i></router-link>
      <router-link to="/kasa" :class="{ active: $route.path === '/kasa' }" :title="$t('nav.kasa')"><i class="pi pi-wallet"></i><span>{{ $t('nav.kasa') }}</span><i class="pi pi-star" :class="{ favori: isFav('/kasa') }" @click.prevent.stop="toggleFav('/kasa')"></i></router-link>
      <router-link to="/banka-mutabakat" :class="{ active: $route.path === '/banka-mutabakat' }" title="Banka Mutabakatı"><i class="pi pi-link"></i><span>Banka Mutabakatı</span><i class="pi pi-star" :class="{ favori: isFav('/banka-mutabakat') }" @click.prevent.stop="toggleFav('/banka-mutabakat')"></i></router-link>
      <router-link to="/cek-senet" :class="{ active: $route.path === '/cek-senet' }" :title="$t('nav.ceksenet')"><i class="pi pi-money-bill"></i><span>{{ $t('nav.ceksenet') }}</span><i class="pi pi-star" :class="{ favori: isFav('/cek-senet') }" @click.prevent.stop="toggleFav('/cek-senet')"></i></router-link>
      <router-link v-if="authStore.kullanici?.role === 'ADMIN'" to="/butceler" :class="{ active: $route.path === '/butceler' }" :title="$t('nav.butce')"><i class="pi pi-chart-bar"></i><span>{{ $t('nav.butce') }}</span><i class="pi pi-star" :class="{ favori: isFav('/butceler') }" @click.prevent.stop="toggleFav('/butceler')"></i></router-link>
      <router-link to="/masraflar" :class="{ active: $route.path === '/masraflar' }" :title="$t('nav.masraf')"><i class="pi pi-money-bill"></i><span>{{ $t('nav.masraf') }}</span><i class="pi pi-star" :class="{ favori: isFav('/masraflar') }" @click.prevent.stop="toggleFav('/masraflar')"></i></router-link>

      <div class="menu-grup">{{ $t('nav.ticaret') }}</div>
      <router-link to="/satislar" :class="{ active: $route.path === '/satislar' }" :title="$t('nav.satis')"><i class="pi pi-shopping-cart"></i><span>{{ $t('nav.satis') }}</span><i class="pi pi-star" :class="{ favori: isFav('/satislar') }" @click.prevent.stop="toggleFav('/satislar')"></i></router-link>
      <router-link to="/hizli-satis" :class="{ active: $route.path === '/hizli-satis' }" :title="$t('nav.hizliSatis')"><i class="pi pi-bolt"></i><span>{{ $t('nav.hizliSatis') }}</span><i class="pi pi-star" :class="{ favori: isFav('/hizli-satis') }" @click.prevent.stop="toggleFav('/hizli-satis')"></i></router-link>
      <router-link to="/crm" :class="{ active: $route.path === '/crm' }" title="CRM"><i class="pi pi-bullseye"></i><span>CRM</span><i class="pi pi-star" :class="{ favori: isFav('/crm') }" @click.prevent.stop="toggleFav('/crm')"></i></router-link>
      <router-link to="/e-fatura" :class="{ active: $route.path === '/e-fatura' }" title="E-Fatura"><i class="pi pi-file-pdf"></i><span>E-Fatura</span><i class="pi pi-star" :class="{ favori: isFav('/e-fatura') }" @click.prevent.stop="toggleFav('/e-fatura')"></i></router-link>
      <router-link to="/satinalma" :class="{ active: $route.path === '/satinalma' }" :title="$t('nav.satinalma')"><i class="pi pi-shopping-bag"></i><span>{{ $t('nav.satinalma') }}</span><i class="pi pi-star" :class="{ favori: isFav('/satinalma') }" @click.prevent.stop="toggleFav('/satinalma')"></i></router-link>
      <router-link to="/siparisler" :class="{ active: $route.path === '/siparisler' }" :title="$t('nav.siparis')"><i class="pi pi-receipt"></i><span>{{ $t('nav.siparis') }}</span><i class="pi pi-star" :class="{ favori: isFav('/siparisler') }" @click.prevent.stop="toggleFav('/siparisler')"></i></router-link>
      <router-link to="/irsaliyeler" :class="{ active: $route.path === '/irsaliyeler' }" :title="$t('nav.irsaliye')"><i class="pi pi-truck"></i><span>{{ $t('nav.irsaliye') }}</span><i class="pi pi-star" :class="{ favori: isFav('/irsaliyeler') }" @click.prevent.stop="toggleFav('/irsaliyeler')"></i></router-link>
      <router-link to="/fiyat-listesi" :class="{ active: $route.path === '/fiyat-listesi' }" :title="$t('nav.fiyatListesi')"><i class="pi pi-tag"></i><span>{{ $t('nav.fiyatListesi') }}</span><i class="pi pi-star" :class="{ favori: isFav('/fiyat-listesi') }" @click.prevent.stop="toggleFav('/fiyat-listesi')"></i></router-link>
      <router-link to="/iadeler" :class="{ active: $route.path === '/iadeler' }" :title="$t('nav.iade')"><i class="pi pi-replay"></i><span>{{ $t('nav.iade') }}</span><i class="pi pi-star" :class="{ favori: isFav('/iadeler') }" @click.prevent.stop="toggleFav('/iadeler')"></i></router-link>

      <div class="menu-grup">{{ $t('nav.envanter') }}</div>
      <router-link to="/stoklar" :class="{ active: $route.path === '/stoklar' }" :title="$t('nav.stok')"><i class="pi pi-box"></i><span>{{ $t('nav.stok') }}</span><i class="pi pi-star" :class="{ favori: isFav('/stoklar') }" @click.prevent.stop="toggleFav('/stoklar')"></i></router-link>
      <router-link to="/kritik-stok" :class="{ active: $route.path === '/kritik-stok' }" title="Kritik Stok"><i class="pi pi-exclamation-triangle"></i><span>Kritik Stok</span><i class="pi pi-star" :class="{ favori: isFav('/kritik-stok') }" @click.prevent.stop="toggleFav('/kritik-stok')"></i></router-link>
      <router-link to="/toplu-stok" :class="{ active: $route.path === '/toplu-stok' }" :title="$t('nav.topluStok')"><i class="pi pi-database"></i><span>{{ $t('nav.topluStok') }}</span><i class="pi pi-star" :class="{ favori: isFav('/toplu-stok') }" @click.prevent.stop="toggleFav('/toplu-stok')"></i></router-link>
      <router-link to="/depolar" :class="{ active: $route.path === '/depolar' }" :title="$t('nav.depo')"><i class="pi pi-warehouse"></i><span>{{ $t('nav.depo') }}</span><i class="pi pi-star" :class="{ favori: isFav('/depolar') }" @click.prevent.stop="toggleFav('/depolar')"></i></router-link>
      <router-link to="/stok-seriler" :class="{ active: $route.path === '/stok-seriler' }" :title="$t('nav.serilot')"><i class="pi pi-qrcode"></i><span>{{ $t('nav.serilot') }}</span><i class="pi pi-star" :class="{ favori: isFav('/stok-seriler') }" @click.prevent.stop="toggleFav('/stok-seriler')"></i></router-link>
      <router-link to="/stok-sayim" :class="{ active: $route.path === '/stok-sayim' }" :title="$t('nav.stokSayim')"><i class="pi pi-sort-alt"></i><span>{{ $t('nav.stokSayim') }}</span><i class="pi pi-star" :class="{ favori: isFav('/stok-sayim') }" @click.prevent.stop="toggleFav('/stok-sayim')"></i></router-link>

      <div class="menu-grup">{{ $t('nav.yonetim') }}</div>
      <router-link to="/subeler" :class="{ active: $route.path === '/subeler' }" :title="$t('nav.sube')"><i class="pi pi-sitemap"></i><span>{{ $t('nav.sube') }}</span><i class="pi pi-star" :class="{ favori: isFav('/subeler') }" @click.prevent.stop="toggleFav('/subeler')"></i></router-link>
      <router-link to="/personel" :class="{ active: $route.path === '/personel' }" :title="$t('nav.personel')"><i class="pi pi-id-card"></i><span>{{ $t('nav.personel') }}</span><i class="pi pi-star" :class="{ favori: isFav('/personel') }" @click.prevent.stop="toggleFav('/personel')"></i></router-link>
      <router-link to="/izinler" :class="{ active: $route.path === '/izinler' }" :title="$t('nav.izin')"><i class="pi pi-calendar"></i><span>{{ $t('nav.izin') }}</span><i class="pi pi-star" :class="{ favori: isFav('/izinler') }" @click.prevent.stop="toggleFav('/izinler')"></i></router-link>
      <router-link to="/projeler" :class="{ active: $route.path === '/projeler' }" :title="$t('nav.proje')"><i class="pi pi-folder"></i><span>{{ $t('nav.proje') }}</span><i class="pi pi-star" :class="{ favori: isFav('/projeler') }" @click.prevent.stop="toggleFav('/projeler')"></i></router-link>
      <router-link v-if="authStore.kullanici?.role === 'ADMIN'" to="/maas-bordro" :class="{ active: $route.path === '/maas-bordro' }" :title="$t('nav.maasBordro')"><i class="pi pi-credit-card"></i><span>{{ $t('nav.maasBordro') }}</span><i class="pi pi-star" :class="{ favori: isFav('/maas-bordro') }" @click.prevent.stop="toggleFav('/maas-bordro')"></i></router-link>
      <router-link to="/vardiyalar" :class="{ active: $route.path === '/vardiyalar' }" :title="$t('nav.vardiya')"><i class="pi pi-clock"></i><span>{{ $t('nav.vardiya') }}</span><i class="pi pi-star" :class="{ favori: isFav('/vardiyalar') }" @click.prevent.stop="toggleFav('/vardiyalar')"></i></router-link>

      <div class="menu-grup">{{ $t('nav.sistem') }}</div>
      <router-link v-if="authStore.kullanici?.role === 'ADMIN'" to="/sirketler" :class="{ active: $route.path === '/sirketler' }" :title="$t('nav.sirket')"><i class="pi pi-building"></i><span>{{ $t('nav.sirket') }}</span><i class="pi pi-star" :class="{ favori: isFav('/sirketler') }" @click.prevent.stop="toggleFav('/sirketler')"></i></router-link>
      <router-link to="/donemler" :class="{ active: $route.path === '/donemler' }" :title="$t('nav.donem')"><i class="pi pi-calendar"></i><span>{{ $t('nav.donem') }}</span><i class="pi pi-star" :class="{ favori: isFav('/donemler') }" @click.prevent.stop="toggleFav('/donemler')"></i></router-link>
      <router-link to="/kullanicilar" v-if="authStore.kullanici?.role === 'ADMIN'" :class="{ active: $route.path === '/kullanicilar' }" :title="$t('nav.kullanici')"><i class="pi pi-user"></i><span>{{ $t('nav.kullanici') }}</span><i class="pi pi-star" :class="{ favori: isFav('/kullanicilar') }" @click.prevent.stop="toggleFav('/kullanicilar')"></i></router-link>
      <router-link to="/yetki-yonetimi" v-if="authStore.kullanici?.role === 'ADMIN'" :class="{ active: $route.path === '/yetki-yonetimi' }" title="Yetki Yönetimi"><i class="pi pi-key"></i><span>Yetkiler</span><i class="pi pi-star" :class="{ favori: isFav('/yetki-yonetimi') }" @click.prevent.stop="toggleFav('/yetki-yonetimi')"></i></router-link>
      <router-link to="/kategoriler" :class="{ active: $route.path === '/kategoriler' }" :title="$t('nav.kategori')"><i class="pi pi-tags"></i><span>{{ $t('nav.kategori') }}</span><i class="pi pi-star" :class="{ favori: isFav('/kategoriler') }" @click.prevent.stop="toggleFav('/kategoriler')"></i></router-link>
      <router-link to="/notlar" :class="{ active: $route.path === '/notlar' }" :title="$t('nav.notlar')"><i class="pi pi-pen-to-square"></i><span>{{ $t('nav.notlar') }}</span><i class="pi pi-star" :class="{ favori: isFav('/notlar') }" @click.prevent.stop="toggleFav('/notlar')"></i></router-link>
      <router-link to="/veri-aktar" :class="{ active: $route.path === '/veri-aktar' }" :title="$t('nav.veriAktar')"><i class="pi pi-upload"></i><span>{{ $t('nav.veriAktar') }}</span><i class="pi pi-star" :class="{ favori: isFav('/veri-aktar') }" @click.prevent.stop="toggleFav('/veri-aktar')"></i></router-link>
      <router-link to="/kullanim-sartlari" :class="{ active: $route.path === '/kullanim-sartlari' }" :title="$t('nav.kullanimSartlari')"><i class="pi pi-file-o"></i><span>{{ $t('nav.kullanimSartlari') }}</span><i class="pi pi-star" :class="{ favori: isFav('/kullanim-sartlari') }" @click.prevent.stop="toggleFav('/kullanim-sartlari')"></i></router-link>
      <router-link to="/hesap-ayarlari" :class="{ active: $route.path === '/hesap-ayarlari' }" title="Hesap Ayarları"><i class="pi pi-cog"></i><span>Hesap Ayarları</span><i class="pi pi-star" :class="{ favori: isFav('/hesap-ayarlari') }" @click.prevent.stop="toggleFav('/hesap-ayarlari')"></i></router-link>
      <router-link to="/gizlilik-politikasi" :class="{ active: $route.path === '/gizlilik-politikasi' }" :title="$t('nav.gizlilik')"><i class="pi pi-shield"></i><span>{{ $t('nav.gizlilik') }}</span><i class="pi pi-star" :class="{ favori: isFav('/gizlilik-politikasi') }" @click.prevent.stop="toggleFav('/gizlilik-politikasi')"></i></router-link>
      <router-link v-if="authStore.kullanici?.role === 'ADMIN'" to="/yedekler" :class="{ active: $route.path === '/yedekler' }" :title="$t('nav.yedek')"><i class="pi pi-save"></i><span>{{ $t('nav.yedek') }}</span><i class="pi pi-star" :class="{ favori: isFav('/yedekler') }" @click.prevent.stop="toggleFav('/yedekler')"></i></router-link>

      <div class="menu-grup">{{ $t('nav.rapor') }}</div>
      <router-link to="/raporlar" :class="{ active: $route.path === '/raporlar' }" :title="$t('nav.rapor')"><i class="pi pi-chart-bar"></i><span>{{ $t('nav.rapor') }}</span><i class="pi pi-star" :class="{ favori: isFav('/raporlar') }" @click.prevent.stop="toggleFav('/raporlar')"></i></router-link>
      <router-link to="/vergi-raporlari" :class="{ active: $route.path === '/vergi-raporlari' }" title="KDV & BA/BS"><i class="pi pi-file-edit"></i><span>KDV & BA/BS</span><i class="pi pi-star" :class="{ favori: isFav('/vergi-raporlari') }" @click.prevent.stop="toggleFav('/vergi-raporlari')"></i></router-link>
      <router-link to="/anomaliler" :class="{ active: $route.path === '/anomaliler' }" :title="$t('nav.anomaliler')"><i class="pi pi-exclamation-triangle"></i><span>{{ $t('nav.anomaliler') }}</span><i class="pi pi-star" :class="{ favori: isFav('/anomaliler') }" @click.prevent.stop="toggleFav('/anomaliler')"></i></router-link>
      <router-link to="/hareketler" :class="{ active: $route.path === '/hareketler' }" :title="$t('nav.hareket')"><i class="pi pi-chart-line"></i><span>{{ $t('nav.hareket') }}</span><i class="pi pi-star" :class="{ favori: isFav('/hareketler') }" @click.prevent.stop="toggleFav('/hareketler')"></i></router-link>
      <router-link v-if="authStore.kullanici?.role === 'ADMIN'" to="/denetim" :class="{ active: $route.path === '/denetim' }" :title="$t('nav.denetim')"><i class="pi pi-shield"></i><span>{{ $t('nav.denetim') }}</span><i class="pi pi-star" :class="{ favori: isFav('/denetim') }" @click.prevent.stop="toggleFav('/denetim')"></i></router-link>
    </div>

    <div class="sidebar-alt">
      <div class="sidebar-arama" @click="$emit('open-search')" :title="$t('common.search') + ' (Ctrl+K)'">
        <i class="pi pi-search"></i>
        <span>{{ $t('common.search') }}</span>
        <kbd>Ctrl+K</kbd>
      </div>

      <div class="admin-card">
        <div class="admin-profile">
          <div class="admin-avatar">
            <img v-if="authStore.kullanici?.avatarUrl" :src="authStore.kullanici.avatarUrl" :alt="authStore.kullanici?.displayName" />
            <span v-else class="avatar-fallback">{{ authStore.kullanici?.displayName?.charAt(0) || 'U' }}</span>
          </div>
          <div class="admin-info">
            <span class="admin-name">{{ authStore.kullanici?.displayName || authStore.kullanici?.username || 'Kullanıcı' }}</span>
            <span class="admin-role">{{ authStore.kullanici?.role || 'USER' }}</span>
          </div>
          <button class="icon-action-btn logout-icon-btn" @click="cikis" :title="$t('auth.logout')">
            <i class="pi pi-sign-out"></i>
          </button>
        </div>

        <div class="admin-actions">
          <BildirimZili class="bildirim-zili-kapsayici" />
          <ThemeSwitcher />
          <button class="icon-action-btn" @click="rehberGoster = true" title="Klavye Kısayolları (?)">
            <i class="pi pi-question-circle"></i>
          </button>
          <button class="icon-action-btn lang-toggle-btn" @click="toggleDil" title="Language / Dil">
            <i class="pi pi-globe"></i><span class="lang-text">{{ dilEtiketi }}</span>
          </button>
          <button class="icon-action-btn" @click="$emit('open-password-modal')" title="Şifre Değiştir">
            <i class="pi pi-lock"></i>
          </button>
        </div>
      </div>

      <div class="sidebar-credit">Rasim Tuzluoğlu</div>
    </div>

    <KisayolRehberi v-model:goster="rehberGoster" />
  </aside>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore.js'
import { sirketAPI } from '../api/index.js'
import { useI18n } from 'vue-i18n'
import BildirimZili from './BildirimZili.vue'
import ThemeSwitcher from './ThemeSwitcher.vue'
import KisayolRehberi from './KisayolRehberi.vue'
import { safeGet, safeSet } from '../utils/safeStorage.js'
import { useTheme } from '../composables/useTheme.js'

const emit = defineEmits(['open-search', 'open-password-modal'])

const router = useRouter()
const authStore = useAuthStore()
const { locale } = useI18n()

const mobilMenuAcik = ref(false)
const rehberGoster = ref(false)

// Rota değiştiğinde mobil menüyü kapat
watch(() => router.currentRoute.value.path, () => {
  mobilMenuAcik.value = false
})

const dilEtiketi = computed(() => locale.value === 'tr' ? 'TR' : 'EN')
const toggleDil = () => {
  locale.value = locale.value === 'tr' ? 'en' : 'tr'
  localStorage.setItem('lang', locale.value)
}

const { isDark, initTheme } = useTheme()

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
  { path: '/muhasebe', label: 'Muhasebe', icon: 'pi pi-book', grup: 'Finans' },
  { path: '/cari-hesaplar', label: 'Cari', icon: 'pi pi-users', grup: 'Finans' },
  { path: '/faturalar', label: 'Faturalar', icon: 'pi pi-file', grup: 'Finans' },
  { path: '/bankalar', label: 'Banka', icon: 'pi pi-building', grup: 'Finans' },
  { path: '/kasa', label: 'Kasa', icon: 'pi pi-wallet', grup: 'Finans' },
  { path: '/banka-mutabakat', label: 'Banka Mutabakatı', icon: 'pi pi-link', grup: 'Finans' },
  { path: '/cek-senet', label: 'Çek/Senet', icon: 'pi pi-money-bill', grup: 'Finans' },
  { path: '/butceler', label: 'Bütçe', icon: 'pi pi-chart-bar', grup: 'Finans', admin: true },
  { path: '/masraflar', label: 'Masraf', icon: 'pi pi-money-bill', grup: 'Finans' },
  { path: '/satislar', label: 'Satış', icon: 'pi pi-shopping-cart', grup: 'Ticaret' },
  { path: '/hizli-satis', label: 'Hızlı Satış', icon: 'pi pi-bolt', grup: 'Ticaret' },
  { path: '/crm', label: 'CRM', icon: 'pi pi-bullseye', grup: 'Ticaret' },
  { path: '/e-fatura', label: 'E-Fatura', icon: 'pi pi-file-pdf', grup: 'Ticaret' },
  { path: '/satinalma', label: 'Satın Alma', icon: 'pi pi-shopping-bag', grup: 'Ticaret' },
  { path: '/siparisler', label: 'Sipariş', icon: 'pi pi-receipt', grup: 'Ticaret' },
  { path: '/irsaliyeler', label: 'İrsaliye', icon: 'pi pi-truck', grup: 'Ticaret' },
  { path: '/fiyat-listesi', label: 'Fiyat Listesi', icon: 'pi pi-tag', grup: 'Ticaret' },
  { path: '/iadeler', label: 'İade', icon: 'pi pi-replay', grup: 'Ticaret' },
  { path: '/stoklar', label: 'Stok', icon: 'pi pi-box', grup: 'Envanter' },
  { path: '/kritik-stok', label: 'Kritik Stok', icon: 'pi pi-exclamation-triangle', grup: 'Envanter' },
  { path: '/toplu-stok', label: 'Toplu Stok', icon: 'pi pi-database', grup: 'Envanter' },
  { path: '/stok-seriler', label: 'Seri/Lot', icon: 'pi pi-qrcode', grup: 'Envanter' },
  { path: '/stok-sayim', label: 'Stok Sayım', icon: 'pi pi-sort-alt', grup: 'Envanter' },
  { path: '/personel', label: 'Personel', icon: 'pi pi-id-card', grup: 'Yönetim' },
  { path: '/izinler', label: 'İzin', icon: 'pi pi-calendar', grup: 'Yönetim' },
  { path: '/projeler', label: 'Proje', icon: 'pi pi-folder', grup: 'Yönetim' },
  { path: '/maas-bordro', label: 'Maaş Bordro', icon: 'pi pi-credit-card', grup: 'Yönetim', admin: true },
  { path: '/vardiyalar', label: 'Vardiya', icon: 'pi pi-clock', grup: 'Yönetim' },
  { path: '/sirketler', label: 'Şirket', icon: 'pi pi-building', grup: 'Sistem', admin: true },
  { path: '/donemler', label: 'Dönem', icon: 'pi pi-calendar', grup: 'Sistem' },
  { path: '/kullanicilar', label: 'Kullanıcı', icon: 'pi pi-user', grup: 'Sistem', admin: true },
  { path: '/kategoriler', label: 'Kategori', icon: 'pi pi-tags', grup: 'Sistem' },
  { path: '/notlar', label: 'Notlar', icon: 'pi pi-pen-to-square', grup: 'Sistem' },
  { path: '/veri-aktar', label: 'Veri Aktar', icon: 'pi pi-upload', grup: 'Sistem' },
  { path: '/kullanim-sartlari', label: 'Kullanım Şartları', icon: 'pi pi-file-o', grup: 'Sistem' },
  { path: '/gizlilik-politikasi', label: 'Gizlilik', icon: 'pi pi-shield', grup: 'Sistem' },
  { path: '/hesap-ayarlari', label: 'Hesap Ayarları', icon: 'pi pi-cog', grup: 'Sistem' },
  { path: '/raporlar', label: 'Rapor', icon: 'pi pi-chart-bar', grup: 'Rapor' },
  { path: '/vergi-raporlari', label: 'KDV & BA/BS', icon: 'pi pi-file-edit', grup: 'Rapor' },
  { path: '/hareketler', label: 'Hareket', icon: 'pi pi-chart-line', grup: 'Rapor' },
  { path: '/denetim', label: 'Denetim', icon: 'pi pi-shield', grup: 'Rapor', admin: true },
  { path: '/yedekler', label: 'Yedek', icon: 'pi pi-save', grup: 'Sistem', admin: true }
]

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
  router.push('/giris')
}

onMounted(() => {
  initTheme()
})
</script>
