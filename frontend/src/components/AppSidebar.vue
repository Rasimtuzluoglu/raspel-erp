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
        v-if="favoriMenuler && favoriMenuler.length"
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
          :title="'Şirket değiştir'"
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
              authStore?.kullanici?.displayName || authStore?.kullanici?.username || 'Kullanıcı'
            }}</span>
            <span class="admin-role">{{ authStore?.kullanici?.role || 'USER' }}</span>
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
          <button
            class="icon-action-btn"
            title="Sifre Degistir"
            @click="$emit('open-password-modal')"
          >
            <i class="pi pi-lock" />
          </button>
          <button
            class="icon-action-btn"
            title="Hesap Makinesi"
            @click="$emit('open-calculator')"
          >
            <i class="pi pi-calculator" />
          </button>
          <button
            class="icon-action-btn"
            title="Doviz"
            @click="$emit('open-currency')"
          >
            <i class="pi pi-money-bill" />
          </button>
          <button
            class="icon-action-btn"
            title="Araclar"
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
            title="KDV"
            @click="$emit('open-kdv')"
          >
            <i class="pi pi-percentage" />
          </button>
          <button
            class="icon-action-btn"
            title="Taksit"
            @click="$emit('open-taksit')"
          >
            <i class="pi pi-calendar" />
          </button>
          <button
            class="icon-action-btn"
            title="Kar Marj"
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

    <KisayolRehberi v-model:goster="rehberGoster" />

    <Dialog
      v-model:visible="sirketDialogAcik"
      header="Şirket Değiştir"
      :modal="true"
      :style="{ width: '440px' }"
    >
      <div
        v-if="sirketSecenekleri.length === 0"
        class="sirket-degistir-bos"
      >
        <i class="pi pi-building" />
        <p>Şirket listesi yüklenemedi.</p>
      </div>
      <div class="sirket-degistir-liste">
        <button
          v-for="s in sirketSecenekleri"
          :key="s.id"
          class="sirket-degistir-kart"
          :class="{ aktif: s.id === authStore.sirketId }"
          @click="sirketDegistir(s)"
        >
          <div class="sirket-degistir-bilgi">
            <i class="pi pi-building" />
            <div>
              <span class="sirket-degistir-ad">{{ s.ad }}</span>
              <span
                v-if="s.vergiNo"
                class="sirket-degistir-vkn"
              >VKN: {{ s.vergiNo }}</span>
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
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore.js'
import { sirketAPI } from '../api/index.js'
import { personelIzinAPI, satinalmaTalepAPI, siparisAPI } from '../api/index.js'
import BildirimZili from './BildirimZili.vue'
import ThemeSwitcher from './ThemeSwitcher.vue'
import KisayolRehberi from './KisayolRehberi.vue'
import { safeGet, safeSet } from '../utils/safeStorage.js'
import { useTheme } from '../composables/useTheme.js'

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
const authStore = useAuthStore()

const mobilMenuAcik = ref(false)
const rehberGoster = ref(false)
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
  } catch {
    // Hata kullanıcıya bildirilmeden dialog açık kalır
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
  { path: '/', label: 'Ana Sayfa', icon: 'pi pi-home', grup: '' },
  { path: '/sohbet', label: 'Sohbet', icon: 'pi pi-comments', grup: '' },
  { path: '/ajanda', label: 'Ajanda', icon: 'pi pi-calendar', grup: '' },
  { path: '/onaylar', label: 'Onaylar', icon: 'pi pi-check-circle', grup: '' },
  { path: '/belgeler', label: 'Belgeler', icon: 'pi pi-folder-open', grup: '' },
  { path: '/sistem-durum', label: 'Sistem Durumu', icon: 'pi pi-server', grup: '', admin: true },
  { path: '/muhasebe', label: 'Muhasebe', icon: 'pi pi-book', grup: 'Finans', gelismis: true },
  { path: '/cari-hesaplar', label: 'Cari', icon: 'pi pi-users', grup: 'Finans' },
  { path: '/faturalar', label: 'Faturalar', icon: 'pi pi-file', grup: 'Finans' },
  { path: '/tekrarlayan-faturalar', label: 'Tekrarlayan Faturalar', icon: 'pi pi-sync', grup: 'Finans' },
  { path: '/bankalar', label: 'Banka', icon: 'pi pi-building', grup: 'Finans' },
  { path: '/kasa', label: 'Kasa', icon: 'pi pi-wallet', grup: 'Finans' },
  { path: '/banka-mutabakat', label: 'Banka Mutabakatı', icon: 'pi pi-link', grup: 'Finans', gelismis: true },
  { path: '/cek-senet', label: 'Çek/Senet', icon: 'pi pi-money-bill', grup: 'Finans', gelismis: true },
  { path: '/tahsilat', label: 'Tahsilat', icon: 'pi pi-dollar', grup: 'Finans' },
  { path: '/pos-terminalleri', label: 'POS Terminalleri', icon: 'pi pi-credit-card', grup: 'Finans', gelismis: true },
  { path: '/butceler', label: 'Bütçe', icon: 'pi pi-chart-bar', grup: 'Finans', admin: true, gelismis: true },
  { path: '/masraflar', label: 'Masraf', icon: 'pi pi-money-bill', grup: 'Finans', gelismis: true },
  { path: '/satislar', label: 'Satış', icon: 'pi pi-shopping-cart', grup: 'Ticaret', gelismis: true },
  { path: '/hizli-satis', label: 'Hızlı Satış', icon: 'pi pi-bolt', grup: 'Ticaret' },
  { path: '/saha-portali', label: 'Saha Portalı', icon: 'pi pi-compass', grup: 'Ticaret' },
  { path: '/teklifler', label: 'Teklifler', icon: 'pi pi-file-edit', grup: 'Ticaret' },
  { path: '/crm', label: 'CRM', icon: 'pi pi-bullseye', grup: 'Ticaret', gelismis: true },
  { path: '/e-fatura', label: 'E-Fatura', icon: 'pi pi-file-pdf', grup: 'Ticaret', gelismis: true },
  { path: '/satinalma', label: 'Satın Alma', icon: 'pi pi-shopping-bag', grup: 'Ticaret', gelismis: true },
  { path: '/siparisler', label: 'Sipariş', icon: 'pi pi-receipt', grup: 'Ticaret', gelismis: true },
  { path: '/siparis-takip', label: 'Sipariş Takibi', icon: 'pi pi-sitemap', grup: 'Ticaret' },
  { path: '/teslimatlar', label: 'Teslimatlar', icon: 'pi pi-truck', grup: 'Ticaret' },
  { path: '/irsaliyeler', label: 'İrsaliye', icon: 'pi pi-truck', grup: 'Ticaret', gelismis: true },
  { path: '/fiyat-listesi', label: 'Fiyat Listesi', icon: 'pi pi-tag', grup: 'Ticaret', gelismis: true },
  { path: '/iadeler', label: 'İade', icon: 'pi pi-replay', grup: 'Ticaret', gelismis: true },
  { path: '/stoklar', label: 'Stok', icon: 'pi pi-box', grup: 'Envanter' },
  { path: '/kritik-stok', label: 'Kritik Stok', icon: 'pi pi-exclamation-triangle', grup: 'Envanter', gelismis: true },
  { path: '/toplu-stok', label: 'Toplu Stok', icon: 'pi pi-database', grup: 'Envanter', gelismis: true },
  { path: '/depolar', label: 'Depo', icon: 'pi pi-warehouse', grup: 'Envanter', gelismis: true },
  { path: '/stok-seriler', label: 'Seri/Lot', icon: 'pi pi-qrcode', grup: 'Envanter', gelismis: true },
  { path: '/stok-sayim', label: 'Stok Sayım', icon: 'pi pi-sort-alt', grup: 'Envanter', gelismis: true },
  { path: '/stok-duzeltmeler', label: 'Stok Düzeltme', icon: 'pi pi-sliders-h', grup: 'Envanter', gelismis: true },
  { path: '/uretim', label: 'Üretim', icon: 'pi pi-cog', grup: 'Envanter' },
  { path: '/subeler', label: 'Şube', icon: 'pi pi-sitemap', grup: 'Yönetim', gelismis: true },
  { path: '/personel', label: 'Personel', icon: 'pi pi-id-card', grup: 'Yönetim' },
  { path: '/izinler', label: 'İzin', icon: 'pi pi-calendar', grup: 'Yönetim', gelismis: true },
  { path: '/projeler', label: 'Proje', icon: 'pi pi-folder', grup: 'Yönetim', gelismis: true },
  {
    path: '/maas-bordro',
    label: 'Maaş Bordro',
    icon: 'pi pi-credit-card',
    grup: 'Yönetim',
    admin: true,
    gelismis: true
  },
  { path: '/vardiyalar', label: 'Vardiya', icon: 'pi pi-clock', grup: 'Yönetim', gelismis: true },
  { path: '/sirketler', label: 'Şirket', icon: 'pi pi-building', grup: 'Sistem', admin: true, gelismis: true },
  { path: '/yeni-yil-sihirbazi', label: 'Yeni Yıl Aç', icon: 'pi pi-sparkles', grup: 'Sistem', admin: true, gelismis: true },
  { path: '/donemler', label: 'Dönem', icon: 'pi pi-calendar', grup: 'Sistem', gelismis: true },
  { path: '/kullanicilar', label: 'Kullanıcı', icon: 'pi pi-user', grup: 'Sistem', admin: true, gelismis: true },
  { path: '/yetki-yonetimi', label: 'Yetkiler', icon: 'pi pi-key', grup: 'Sistem', admin: true, gelismis: true },
  { path: '/kategoriler', label: 'Kategori', icon: 'pi pi-tags', grup: 'Sistem', gelismis: true },
  { path: '/notlar', label: 'Notlar', icon: 'pi pi-pen-to-square', grup: 'Sistem' },
  { path: '/veri-aktar', label: 'Veri Aktar', icon: 'pi pi-upload', grup: 'Sistem', gelismis: true },
  { path: '/kullanim-sartlari', label: 'Kullanım Şartları', icon: 'pi pi-file', grup: 'Sistem', gelismis: true },
  { path: '/gizlilik-politikasi', label: 'Gizlilik', icon: 'pi pi-shield', grup: 'Sistem', gelismis: true },
  { path: '/hesap-ayarlari', label: 'Hesap Ayarları', icon: 'pi pi-cog', grup: 'Sistem', gelismis: true },
  { path: '/yedekler', label: 'Yedek', icon: 'pi pi-save', grup: 'Sistem', admin: true, gelismis: true },
  { path: '/yonetici-kokpiti', label: 'Yönetici Kokpiti', icon: 'pi pi-bolt', grup: 'Rapor', admin: true },
  { path: '/raporlar', label: 'Rapor', icon: 'pi pi-chart-bar', grup: 'Rapor' },
  { path: '/vergi-raporlari', label: 'KDV & BA/BS', icon: 'pi pi-file-edit', grup: 'Rapor', gelismis: true },
  { path: '/anomaliler', label: 'Anomali', icon: 'pi pi-exclamation-triangle', grup: 'Rapor', gelismis: true },
  { path: '/hareketler', label: 'Hareket', icon: 'pi pi-chart-line', grup: 'Rapor' },
  { path: '/denetim', label: 'Denetim', icon: 'pi pi-shield', grup: 'Rapor', admin: true, gelismis: true }
]

const gorunenMenuler = computed(() => {
  return tumMenuler.filter((m) => {
    if (m.path === '/') return false
    
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
  window.location.replace('/giris')
}

onMounted(() => {
  initTheme()
  onaySayisiniYukle()
})

const onaySayisi = ref(0)

const onaySayisiniYukle = async () => {
  try {
    const [iRes, tRes, sRes] = await Promise.all([
      personelIzinAPI.getAll(),
      satinalmaTalepAPI.getAll(),
      siparisAPI.getAll({ size: 100 })
    ])
    const izinler = iRes.data?.content || iRes.data || []
    const talepler = tRes.data?.content || tRes.data || []
    const siparisler = sRes.data?.content || sRes.data || []
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
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 8px;
  color: var(--text-primary);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.firma-secici:hover {
  background: rgba(59, 130, 246, 0.18);
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
