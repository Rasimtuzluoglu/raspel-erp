<template>
  <div class="app-container">
    <template v-if="authStore.isLoggedIn">
      <aside class="sidebar">
        <div class="sidebar-ust">
          <router-link to="/" class="brand">
            <img v-if="sirketLogo" :src="sirketLogo" class="brand-logo" alt="logo" />
            <div v-else class="brand-icon"><i class="pi pi-calculator"></i></div>
            <div class="brand-text">
              <span class="brand-title">RasPel</span>
              <span class="brand-company" v-if="authStore.sirketAdi">{{ authStore.sirketAdi }}</span>
            </div>
          </router-link>
        </div>

        <div class="sidebar-menu">
          <div v-if="favoriMenuler.length" class="menu-grup">Sık Kullanılanlar</div>
          <router-link v-for="m in favoriMenuler" :key="m.path" :to="m.path" :class="{ active: $route.path === m.path || ($route.path.startsWith(m.path) && m.path !== '/') }" :title="m.label">
            <i :class="m.icon"></i><span>{{ m.label }}</span>
            <i class="pi pi-star-fav" :class="{ favori: isFav(m.path) }" @click.prevent.stop="toggleFav(m.path)"></i>
          </router-link>

          <router-link to="/" :class="{ active: $route.path === '/' }" title="Ana Sayfa"><i class="pi pi-home"></i><span>Ana Sayfa</span></router-link>
          <div class="menu-grup">Finans</div>
          <router-link to="/cari-hesaplar" :class="{ active: $route.path === '/cari-hesaplar' }" title="Cari Hesaplar"><i class="pi pi-users"></i><span>Cari</span><i class="pi pi-star-fav" :class="{ favori: isFav('/cari-hesaplar') }" @click.prevent.stop="toggleFav('/cari-hesaplar')"></i></router-link>
          <router-link to="/faturalar" :class="{ active: $route.path.startsWith('/faturalar') }" title="Faturalar"><i class="pi pi-file"></i><span>Faturalar</span><i class="pi pi-star-fav" :class="{ favori: isFav('/faturalar') }" @click.prevent.stop="toggleFav('/faturalar')"></i></router-link>
          <router-link to="/bankalar" :class="{ active: $route.path === '/bankalar' }" title="Bankalar"><i class="pi pi-building"></i><span>Banka</span><i class="pi pi-star-fav" :class="{ favori: isFav('/bankalar') }" @click.prevent.stop="toggleFav('/bankalar')"></i></router-link>
          <router-link to="/kasa" :class="{ active: $route.path === '/kasa' }" title="Kasa"><i class="pi pi-wallet"></i><span>Kasa</span><i class="pi pi-star-fav" :class="{ favori: isFav('/kasa') }" @click.prevent.stop="toggleFav('/kasa')"></i></router-link>
          <router-link to="/cek-senet" :class="{ active: $route.path === '/cek-senet' }" title="Çek/Senet"><i class="pi pi-money-bill"></i><span>Çek/Senet</span><i class="pi pi-star-fav" :class="{ favori: isFav('/cek-senet') }" @click.prevent.stop="toggleFav('/cek-senet')"></i></router-link>
          <router-link v-if="authStore.kullanici?.role === 'ADMIN'" to="/butceler" :class="{ active: $route.path === '/butceler' }" title="Bütçe"><i class="pi pi-chart-bar"></i><span>Bütçe</span><i class="pi pi-star-fav" :class="{ favori: isFav('/butceler') }" @click.prevent.stop="toggleFav('/butceler')"></i></router-link>
          <router-link to="/masraflar" :class="{ active: $route.path === '/masraflar' }" title="Masraf"><i class="pi pi-money-bill"></i><span>Masraf</span><i class="pi pi-star-fav" :class="{ favori: isFav('/masraflar') }" @click.prevent.stop="toggleFav('/masraflar')"></i></router-link>
          <div class="menu-grup">Ticaret</div>
          <router-link to="/satislar" :class="{ active: $route.path === '/satislar' }" title="Satış"><i class="pi pi-shopping-cart"></i><span>Satış</span><i class="pi pi-star-fav" :class="{ favori: isFav('/satislar') }" @click.prevent.stop="toggleFav('/satislar')"></i></router-link>
          <router-link to="/hizli-satis" :class="{ active: $route.path === '/hizli-satis' }" title="Hızlı Satış"><i class="pi pi-bolt"></i><span>Hızlı Satış</span><i class="pi pi-star-fav" :class="{ favori: isFav('/hizli-satis') }" @click.prevent.stop="toggleFav('/hizli-satis')"></i></router-link>
          <router-link to="/satinalma" :class="{ active: $route.path === '/satinalma' }" title="Satın Alma"><i class="pi pi-shopping-bag"></i><span>Satın Alma</span><i class="pi pi-star-fav" :class="{ favori: isFav('/satinalma') }" @click.prevent.stop="toggleFav('/satinalma')"></i></router-link>
          <router-link to="/siparisler" :class="{ active: $route.path === '/siparisler' }" title="Siparişler"><i class="pi pi-receipt"></i><span>Sipariş</span><i class="pi pi-star-fav" :class="{ favori: isFav('/siparisler') }" @click.prevent.stop="toggleFav('/siparisler')"></i></router-link>
          <router-link to="/irsaliyeler" :class="{ active: $route.path === '/irsaliyeler' }" title="İrsaliyeler"><i class="pi pi-truck"></i><span>İrsaliye</span><i class="pi pi-star-fav" :class="{ favori: isFav('/irsaliyeler') }" @click.prevent.stop="toggleFav('/irsaliyeler')"></i></router-link>
          <router-link to="/fiyat-listesi" :class="{ active: $route.path === '/fiyat-listesi' }" title="Fiyat Listesi"><i class="pi pi-tag"></i><span>Fiyat Listesi</span><i class="pi pi-star-fav" :class="{ favori: isFav('/fiyat-listesi') }" @click.prevent.stop="toggleFav('/fiyat-listesi')"></i></router-link>
          <router-link to="/iadeler" :class="{ active: $route.path === '/iadeler' }" title="İadeler"><i class="pi pi-replay"></i><span>İade</span><i class="pi pi-star-fav" :class="{ favori: isFav('/iadeler') }" @click.prevent.stop="toggleFav('/iadeler')"></i></router-link>
          <div class="menu-grup">Envanter</div>
          <router-link to="/stoklar" :class="{ active: $route.path === '/stoklar' }" title="Stoklar"><i class="pi pi-box"></i><span>Stok</span><i class="pi pi-star-fav" :class="{ favori: isFav('/stoklar') }" @click.prevent.stop="toggleFav('/stoklar')"></i></router-link>
          <router-link to="/toplu-stok" :class="{ active: $route.path === '/toplu-stok' }" title="Toplu Stok"><i class="pi pi-database"></i><span>Toplu Stok</span><i class="pi pi-star-fav" :class="{ favori: isFav('/toplu-stok') }" @click.prevent.stop="toggleFav('/toplu-stok')"></i></router-link>
          <router-link to="/depolar" :class="{ active: $route.path === '/depolar' }" title="Depolar"><i class="pi pi-warehouse"></i><span>Depo</span><i class="pi pi-star-fav" :class="{ favori: isFav('/depolar') }" @click.prevent.stop="toggleFav('/depolar')"></i></router-link>
          <router-link to="/stok-seriler" :class="{ active: $route.path === '/stok-seriler' }" title="Seri/Lot Takibi"><i class="pi pi-qrcode"></i><span>Seri/Lot</span><i class="pi pi-star-fav" :class="{ favori: isFav('/stok-seriler') }" @click.prevent.stop="toggleFav('/stok-seriler')"></i></router-link>
          <router-link to="/stok-sayim" :class="{ active: $route.path === '/stok-sayim' }" title="Stok Sayımı"><i class="pi pi-sort-alt"></i><span>Stok Sayım</span><i class="pi pi-star-fav" :class="{ favori: isFav('/stok-sayim') }" @click.prevent.stop="toggleFav('/stok-sayim')"></i></router-link>
          <div class="menu-grup">Yönetim</div>
          <router-link to="/subeler" :class="{ active: $route.path === '/subeler' }" title="Şubeler"><i class="pi pi-sitemap"></i><span>Şube</span><i class="pi pi-star-fav" :class="{ favori: isFav('/subeler') }" @click.prevent.stop="toggleFav('/subeler')"></i></router-link>
          <router-link to="/personel" :class="{ active: $route.path === '/personel' }" title="Personel"><i class="pi pi-id-card"></i><span>Personel</span><i class="pi pi-star-fav" :class="{ favori: isFav('/personel') }" @click.prevent.stop="toggleFav('/personel')"></i></router-link>
          <router-link to="/izinler" :class="{ active: $route.path === '/izinler' }" title="İzin Talepleri"><i class="pi pi-calendar"></i><span>İzin</span><i class="pi pi-star-fav" :class="{ favori: isFav('/izinler') }" @click.prevent.stop="toggleFav('/izinler')"></i></router-link>
          <router-link to="/projeler" :class="{ active: $route.path === '/projeler' }" title="Projeler"><i class="pi pi-folder"></i><span>Proje</span><i class="pi pi-star-fav" :class="{ favori: isFav('/projeler') }" @click.prevent.stop="toggleFav('/projeler')"></i></router-link>
          <router-link v-if="authStore.kullanici?.role === 'ADMIN'" to="/maas-bordro" :class="{ active: $route.path === '/maas-bordro' }" title="Maaş Bordro"><i class="pi pi-credit-card"></i><span>Maaş Bordro</span><i class="pi pi-star-fav" :class="{ favori: isFav('/maas-bordro') }" @click.prevent.stop="toggleFav('/maas-bordro')"></i></router-link>
          <router-link to="/vardiyalar" :class="{ active: $route.path === '/vardiyalar' }" title="Vardiyalar"><i class="pi pi-clock"></i><span>Vardiya</span><i class="pi pi-star-fav" :class="{ favori: isFav('/vardiyalar') }" @click.prevent.stop="toggleFav('/vardiyalar')"></i></router-link>
          <div class="menu-grup">Sistem</div>
          <router-link v-if="authStore.kullanici?.role === 'ADMIN'" to="/sirketler" :class="{ active: $route.path === '/sirketler' }" title="Şirketler"><i class="pi pi-building"></i><span>Şirket</span><i class="pi pi-star-fav" :class="{ favori: isFav('/sirketler') }" @click.prevent.stop="toggleFav('/sirketler')"></i></router-link>
          <router-link to="/donemler" :class="{ active: $route.path === '/donemler' }" title="Dönemler"><i class="pi pi-calendar"></i><span>Dönem</span><i class="pi pi-star-fav" :class="{ favori: isFav('/donemler') }" @click.prevent.stop="toggleFav('/donemler')"></i></router-link>
          <router-link to="/kullanicilar" v-if="authStore.kullanici?.role === 'ADMIN'" :class="{ active: $route.path === '/kullanicilar' }" title="Kullanıcılar"><i class="pi pi-user"></i><span>Kullanıcı</span><i class="pi pi-star-fav" :class="{ favori: isFav('/kullanicilar') }" @click.prevent.stop="toggleFav('/kullanicilar')"></i></router-link>
          <router-link to="/kategoriler" :class="{ active: $route.path === '/kategoriler' }" title="Kategoriler"><i class="pi pi-tags"></i><span>Kategori</span><i class="pi pi-star-fav" :class="{ favori: isFav('/kategoriler') }" @click.prevent.stop="toggleFav('/kategoriler')"></i></router-link>
          <router-link v-if="authStore.kullanici?.role === 'ADMIN'" to="/yedekler" :class="{ active: $route.path === '/yedekler' }" title="Yedekleme"><i class="pi pi-save"></i><span>Yedek</span><i class="pi pi-star-fav" :class="{ favori: isFav('/yedekler') }" @click.prevent.stop="toggleFav('/yedekler')"></i></router-link>
          <div class="menu-grup">Rapor</div>
          <router-link to="/raporlar" :class="{ active: $route.path === '/raporlar' }" title="Raporlar"><i class="pi pi-chart-bar"></i><span>Rapor</span><i class="pi pi-star-fav" :class="{ favori: isFav('/raporlar') }" @click.prevent.stop="toggleFav('/raporlar')"></i></router-link>
          <router-link to="/hareketler" :class="{ active: $route.path === '/hareketler' }" title="Hareketler"><i class="pi pi-chart-line"></i><span>Hareket</span><i class="pi pi-star-fav" :class="{ favori: isFav('/hareketler') }" @click.prevent.stop="toggleFav('/hareketler')"></i></router-link>
          <router-link v-if="authStore.kullanici?.role === 'ADMIN'" to="/denetim" :class="{ active: $route.path === '/denetim' }" title="Denetim"><i class="pi pi-shield"></i><span>Denetim</span><i class="pi pi-star-fav" :class="{ favori: isFav('/denetim') }" @click.prevent.stop="toggleFav('/denetim')"></i></router-link>
        </div>

        <div class="sidebar-alt">
          <div class="sidebar-alt-ust">
            <div class="sidebar-arama" @click="quickSearchVisible = true" title="Hızlı Ara (Ctrl+K)">
              <i class="pi pi-search"></i>
              <span>Hızlı Ara...</span>
              <kbd>Ctrl+K</kbd>
            </div>
            <div class="kullanici-kart">
              <div class="kullanici-avatar">
                <img v-if="authStore.kullanici?.avatarUrl" :src="authStore.kullanici.avatarUrl" :alt="authStore.kullanici.displayName" />
                <span v-else class="avatar-yedek">{{ authStore.kullanici?.displayName?.charAt(0) }}</span>
              </div>
              <div class="kullanici-bilgi">
                <span class="kullanici-ad">{{ authStore.kullanici?.displayName }}</span>
                <span class="kullanici-rol">{{ authStore.kullanici?.role }}</span>
              </div>
              <Button icon="pi pi-lock" class="p-button-rounded p-button-text sifre-btn" @click="sifreDialog = true" title="Şifre Değiştir" />
              <Button icon="pi pi-sign-out" class="p-button-rounded p-button-text cikis-btn" @click="cikis" title="Çıkış Yap" />
            </div>
          </div>
          <button class="theme-btn" @click="toggleTheme" :title="isDark ? 'Açık Tema' : 'Koyu Tema'">
            <i :class="isDark ? 'pi pi-sun' : 'pi pi-moon'"></i>
          </button>
          <div class="sidebar-credit">Rasim Tuzluoğlu</div>
        </div>
      </aside>
      <div class="sidebar-spacer"></div>
    </template>

    <main class="main-content" :class="{ 'giris-sayfasi': !authStore.isLoggedIn }">
      <router-view />
    </main>

    <QuickSearch :visible="quickSearchVisible" @update:visible="quickSearchVisible = $event" />
    <Toast position="top-right" :life="5000" />
    <ConfirmDialog />

    <Dialog v-model:visible="sifreDialog" header="Şifre Değiştir" modal :style="{ width: '400px' }">
      <div class="form-grid">
        <div class="field">
          <label>Mevcut Şifre</label>
          <InputText v-model="sifreForm.mevcutSifre" type="password" class="w-full" />
        </div>
        <div class="field">
          <label>Yeni Şifre</label>
          <InputText v-model="sifreForm.yeniSifre" type="password" class="w-full" />
        </div>
        <div class="field">
          <label>Yeni Şifre Tekrar</label>
          <InputText v-model="sifreForm.yeniSifreTekrar" type="password" class="w-full" />
        </div>
      </div>
      <template #footer>
        <Button label="İptal" icon="pi pi-times" class="p-button-text" @click="sifreDialog = false" />
        <Button label="Değiştir" icon="pi pi-check" @click="sifreDegistir" :loading="sifreDegistiriliyor" />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useAuthStore } from './stores/authStore.js'
import { sirketAPI, kullaniciAPI } from './api/index.js'
import QuickSearch from './components/QuickSearch.vue'

const router = useRouter()
const authStore = useAuthStore()
const toast = useToast()
const quickSearchVisible = ref(false)

const sifreDialog = ref(false)
const sifreDegistiriliyor = ref(false)
const sifreForm = ref({ mevcutSifre: '', yeniSifre: '', yeniSifreTekrar: '' })

const sifreDegistir = async () => {
  if (!sifreForm.value.yeniSifre || sifreForm.value.yeniSifre.length < 3) {
    toast.add({ severity: 'warn', summary: 'Uyarı', detail: 'Yeni şifre en az 3 karakter olmalıdır', life: 5000 })
    return
  }
  if (sifreForm.value.yeniSifre !== sifreForm.value.yeniSifreTekrar) {
    toast.add({ severity: 'warn', summary: 'Uyarı', detail: 'Yeni şifreler eşleşmiyor', life: 5000 })
    return
  }
  sifreDegistiriliyor.value = true
  try {
    await kullaniciAPI.sifreDegistir({ mevcutSifre: sifreForm.value.mevcutSifre, yeniSifre: sifreForm.value.yeniSifre })
    toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Şifreniz değiştirildi', life: 5000 })
    sifreDialog.value = false
    sifreForm.value = { mevcutSifre: '', yeniSifre: '', yeniSifreTekrar: '' }
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Şifre değiştirilemedi', life: 5000 })
  }
  sifreDegistiriliyor.value = false
}

const theme = ref(localStorage.getItem('raspel_erp_theme') || 'dark')
const isDark = computed(() => theme.value === 'dark')

watch(theme, (t) => {
  document.documentElement.setAttribute('data-theme', t)
  document.documentElement.classList.toggle('p-dark', t === 'dark')
  localStorage.setItem('raspel_erp_theme', t)
})

const toggleTheme = () => {
  theme.value = isDark.value ? 'light' : 'dark'
}

const favoriler = ref(JSON.parse(localStorage.getItem('raspel_erp_favorites') || '[]'))
watch(favoriler, (f) => localStorage.setItem('raspel_erp_favorites', JSON.stringify(f)), { deep: true })

const toggleFav = (path) => {
  const idx = favoriler.value.indexOf(path)
  if (idx > -1) favoriler.value.splice(idx, 1)
  else favoriler.value.push(path)
}
const isFav = (path) => favoriler.value.includes(path)

const tumMenuler = [
  { path: '/', label: 'Ana Sayfa', icon: 'pi pi-home', grup: '' },
  { path: '/cari-hesaplar', label: 'Cari', icon: 'pi pi-users', grup: 'Finans' },
  { path: '/faturalar', label: 'Faturalar', icon: 'pi pi-file', grup: 'Finans' },
  { path: '/bankalar', label: 'Banka', icon: 'pi pi-building', grup: 'Finans' },
  { path: '/kasa', label: 'Kasa', icon: 'pi pi-wallet', grup: 'Finans' },
  { path: '/cek-senet', label: 'Çek/Senet', icon: 'pi pi-money-bill', grup: 'Finans' },
  { path: '/butceler', label: 'Bütçe', icon: 'pi pi-chart-bar', grup: 'Finans', admin: true },
  { path: '/masraflar', label: 'Masraf', icon: 'pi pi-money-bill', grup: 'Finans' },
  { path: '/satislar', label: 'Satış', icon: 'pi pi-shopping-cart', grup: 'Ticaret' },
  { path: '/hizli-satis', label: 'Hızlı Satış', icon: 'pi pi-bolt', grup: 'Ticaret' },
  { path: '/satinalma', label: 'Satın Alma', icon: 'pi pi-shopping-bag', grup: 'Ticaret' },
  { path: '/siparisler', label: 'Sipariş', icon: 'pi pi-receipt', grup: 'Ticaret' },
  { path: '/irsaliyeler', label: 'İrsaliye', icon: 'pi pi-truck', grup: 'Ticaret' },
  { path: '/fiyat-listesi', label: 'Fiyat Listesi', icon: 'pi pi-tag', grup: 'Ticaret' },
  { path: '/iadeler', label: 'İade', icon: 'pi pi-replay', grup: 'Ticaret' },
  { path: '/stoklar', label: 'Stok', icon: 'pi pi-box', grup: 'Envanter' },
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
  { path: '/raporlar', label: 'Rapor', icon: 'pi pi-chart-bar', grup: 'Rapor' },
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

const kisaYolHandler = (e) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault()
    quickSearchVisible.value = !quickSearchVisible.value
  }
}

onMounted(() => {
  document.documentElement.setAttribute('data-theme', theme.value)
  document.documentElement.classList.toggle('p-dark', theme.value === 'dark')
  document.addEventListener('keydown', kisaYolHandler)
})
onUnmounted(() => document.removeEventListener('keydown', kisaYolHandler))
</script>

<style>
/* App.vue'ye özel stiller buraya */
</style>
