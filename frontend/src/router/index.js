import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/authStore.js'
import Giris from '../views/Giris.vue'
import Dashboard from '../views/Dashboard.vue'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

NProgress.configure({ showSpinner: false })

const routes = [
  {
    path: '/giris',
    name: 'Giris',
    component: Giris,
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    name: 'Dashboard',
    component: Dashboard,
    meta: { requiresAuth: true }
  },
  {
    path: '/cari-hesaplar',
    name: 'CariHesaplar',
    component: () => import('../views/CariHesaplar.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/hareketler',
    name: 'Hareketler',
    component: () => import('../views/Hareketler.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/faturalar',
    name: 'Faturalar',
    component: () => import('../views/Faturalar.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/faturalar/:id',
    name: 'FaturaDetay',
    component: () => import('../views/FaturaDetay.vue'),
    props: true,
    meta: { requiresAuth: true }
  },
  {
    path: '/bankalar',
    name: 'Bankalar',
    component: () => import('../views/Bankalar.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/kasa',
    name: 'Kasa',
    component: () => import('../views/Kasa.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/raporlar',
    name: 'Raporlar',
    component: () => import('../views/Raporlar.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/kategoriler',
    name: 'Kategoriler',
    component: () => import('../views/Kategoriler.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/kullanicilar',
    name: 'Kullanicilar',
    component: () => import('../views/Kullanicilar.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/stoklar',
    name: 'Stoklar',
    component: () => import('../views/Stoklar.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/toplu-stok',
    name: 'TopluStok',
    component: () => import('../views/TopluStok.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/satislar',
    name: 'Satislar',
    component: () => import('../views/Satis.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/hizli-satis',
    name: 'HizliSatis',
    component: () => import('../views/HizliSatis.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/sirketler',
    name: 'Sirketler',
    component: () => import('../views/Sirketler.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/donemler',
    name: 'Donemler',
    component: () => import('../views/Donemler.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/satinalma',
    name: 'Satinalma',
    component: () => import('../views/Satinalma.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/personel',
    name: 'Personel',
    component: () => import('../views/Personel.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/izinler',
    name: 'Izinler',
    component: () => import('../views/Izinler.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/puantaj',
    name: 'Puantaj',
    component: () => import('../views/Puantaj.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/siparisler',
    name: 'Siparisler',
    component: () => import('../views/Siparisler.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/cek-senet',
    name: 'CekSenet',
    component: () => import('../views/CekSenet.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/irsaliyeler',
    name: 'Irsaliyeler',
    component: () => import('../views/Irsaliyeler.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/projeler',
    name: 'Projeler',
    component: () => import('../views/Projeler.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/denetim',
    name: 'Denetim',
    component: () => import('../views/Denetim.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/yedekler',
    name: 'Yedekler',
    component: () => import('../views/Yedekler.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/subeler',
    name: 'Subeler',
    component: () => import('../views/Subeler.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/depolar',
    name: 'Depolar',
    component: () => import('../views/Depolar.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/butceler',
    name: 'Butceler',
    component: () => import('../views/Butceler.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/masraflar',
    name: 'Masraflar',
    component: () => import('../views/Masraflar.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/fiyat-listesi',
    name: 'FiyatListesi',
    component: () => import('../views/FiyatListesi.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/iadeler',
    name: 'Iadeler',
    component: () => import('../views/Iadeler.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/stok-seriler',
    name: 'StokSeriler',
    component: () => import('../views/StokSeriler.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/stok-sayim',
    name: 'StokSayim',
    component: () => import('../views/StokSayim.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/maas-bordro',
    name: 'MaasBordro',
    component: () => import('../views/MaasBordro.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/vardiyalar',
    name: 'Vardiyalar',
    component: () => import('../views/Vardiyalar.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/anomaliler',
    name: 'Anomaliler',
    component: () => import('../views/Anomaliler.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/notlar',
    name: 'Notlar',
    component: () => import('../views/Notlar.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/veri-aktar',
    name: 'VeriImport',
    component: () => import('../views/VeriImport.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/kullanim-sartlari',
    name: 'KullanimSartlari',
    component: () => import('../views/KullanimSartlari.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/gizlilik-politikasi',
    name: 'GizlilikPolitikasi',
    component: () => import('../views/GizlilikPolitikasi.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/yetki-yonetimi',
    name: 'YetkiYonetimi',
    component: () => import('../views/YetkiYonetimi.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/yetki-reddi',
    name: 'YetkiReddi',
    component: () => import('../views/YetkiReddi.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/muhasebe',
    name: 'Muhasebe',
    component: () => import('../views/Muhasebe.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/crm',
    name: 'Crm',
    component: () => import('../views/Crm.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/e-fatura',
    name: 'EFatura',
    component: () => import('../views/EFatura.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/kritik-stok',
    name: 'KritikStok',
    component: () => import('../views/KritikStok.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/hesap-ayarlari',
    name: 'HesapAyarlari',
    component: () => import('../views/HesapAyarlari.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/banka-mutabakat',
    name: 'BankaMutabakat',
    component: () => import('../views/BankaMutabakat.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/vergi-raporlari',
    name: 'VergiRaporlari',
    component: () => import('../views/VergiRaporlari.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/sohbet',
    name: 'Sohbet',
    component: () => import('../views/Sohbet.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/ajanda',
    name: 'Ajanda',
    component: () => import('../views/Ajanda.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/onaylar',
    name: 'Onaylar',
    component: () => import('../views/Onaylar.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/belgeler',
    name: 'Belgeler',
    component: () => import('../views/Belgeler.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/sistem-durum',
    name: 'SistemDurum',
    component: () => import('../views/SistemDurum.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  NProgress.start()
  const authStore = useAuthStore()
  if (to.meta.requiresAuth !== false && !authStore.isLoggedIn) {
    next({ name: 'Giris', query: { redirect: to.fullPath } })
  } else if (to.path === '/giris' && authStore.isLoggedIn && to.name === 'Giris') {
    next('/')
  } else if (to.meta.requiresAdmin && !authStore.isAdmin) {
    next('/yetki-reddi')
  } else if (to.meta.permission && !authStore.hasPermission(to.meta.permission)) {
    next('/yetki-reddi')
  } else {
    next()
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router
