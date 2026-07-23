import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/authStore.js'
import Giris from '../views/Giris.vue'
import Dashboard from '../views/Dashboard.vue'
import CariHesaplar from '../views/CariHesaplar.vue'
import Hareketler from '../views/Hareketler.vue'
import Faturalar from '../views/Faturalar.vue'
import FaturaDetay from '../views/FaturaDetay.vue'
import Bankalar from '../views/Bankalar.vue'
import Kasa from '../views/Kasa.vue'
import Raporlar from '../views/Raporlar.vue'
import Kategoriler from '../views/Kategoriler.vue'
import Kullanicilar from '../views/Kullanicilar.vue'
import Stoklar from '../views/Stoklar.vue'
import Satis from '../views/Satis.vue'
import Sirketler from '../views/Sirketler.vue'
import Donemler from '../views/Donemler.vue'
import Satinalma from '../views/Satinalma.vue'
import Personel from '../views/Personel.vue'
import Siparisler from '../views/Siparisler.vue'
import CekSenet from '../views/CekSenet.vue'
import Irsaliyeler from '../views/Irsaliyeler.vue'
import Projeler from '../views/Projeler.vue'
import Denetim from '../views/Denetim.vue'
import Subeler from '../views/Subeler.vue'
import Depolar from '../views/Depolar.vue'
import Butceler from '../views/Butceler.vue'
import Masraflar from '../views/Masraflar.vue'
import HizliSatis from '../views/HizliSatis.vue'
import TopluStok from '../views/TopluStok.vue'
import FiyatListesi from '../views/FiyatListesi.vue'
import Iadeler from '../views/Iadeler.vue'
import StokSeriler from '../views/StokSeriler.vue'
import StokSayim from '../views/StokSayim.vue'
import MaasBordro from '../views/MaasBordro.vue'
import Vardiyalar from '../views/Vardiyalar.vue'
import NotFound from '../views/NotFound.vue'

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
    component: CariHesaplar,
    meta: { requiresAuth: true }
  },
  {
    path: '/hareketler',
    name: 'Hareketler',
    component: Hareketler,
    meta: { requiresAuth: true }
  },
  {
    path: '/faturalar',
    name: 'Faturalar',
    component: Faturalar,
    meta: { requiresAuth: true }
  },
  {
    path: '/faturalar/:id',
    name: 'FaturaDetay',
    component: FaturaDetay,
    props: true,
    meta: { requiresAuth: true }
  },
  {
    path: '/bankalar',
    name: 'Bankalar',
    component: Bankalar,
    meta: { requiresAuth: true }
  },
  {
    path: '/kasa',
    name: 'Kasa',
    component: Kasa,
    meta: { requiresAuth: true }
  },
  {
    path: '/raporlar',
    name: 'Raporlar',
    component: Raporlar,
    meta: { requiresAuth: true }
  },
  {
    path: '/kategoriler',
    name: 'Kategoriler',
    component: Kategoriler,
    meta: { requiresAuth: true }
  },
  {
    path: '/kullanicilar',
    name: 'Kullanicilar',
    component: Kullanicilar,
    meta: { requiresAuth: true }
  },
  {
    path: '/stoklar',
    name: 'Stoklar',
    component: Stoklar,
    meta: { requiresAuth: true }
  },
  {
    path: '/toplu-stok',
    name: 'TopluStok',
    component: TopluStok,
    meta: { requiresAuth: true }
  },
  {
    path: '/satislar',
    name: 'Satislar',
    component: Satis,
    meta: { requiresAuth: true }
  },
  {
    path: '/hizli-satis',
    name: 'HizliSatis',
    component: HizliSatis,
    meta: { requiresAuth: true }
  },
  {
    path: '/sirketler',
    name: 'Sirketler',
    component: Sirketler,
    meta: { requiresAuth: true }
  },
  {
    path: '/donemler',
    name: 'Donemler',
    component: Donemler,
    meta: { requiresAuth: true }
  },
  {
    path: '/satinalma',
    name: 'Satinalma',
    component: Satinalma,
    meta: { requiresAuth: true }
  },
  {
    path: '/personel',
    name: 'Personel',
    component: Personel,
    meta: { requiresAuth: true }
  },
  {
    path: '/siparisler',
    name: 'Siparisler',
    component: Siparisler,
    meta: { requiresAuth: true }
  },
  {
    path: '/cek-senet',
    name: 'CekSenet',
    component: CekSenet,
    meta: { requiresAuth: true }
  },
  {
    path: '/irsaliyeler',
    name: 'Irsaliyeler',
    component: Irsaliyeler,
    meta: { requiresAuth: true }
  },
  {
    path: '/projeler',
    name: 'Projeler',
    component: Projeler,
    meta: { requiresAuth: true }
  },
  {
    path: '/denetim',
    name: 'Denetim',
    component: Denetim,
    meta: { requiresAuth: true }
  },
  {
    path: '/subeler',
    name: 'Subeler',
    component: Subeler,
    meta: { requiresAuth: true }
  },
  {
    path: '/depolar',
    name: 'Depolar',
    component: Depolar,
    meta: { requiresAuth: true }
  },
  {
    path: '/butceler',
    name: 'Butceler',
    component: Butceler,
    meta: { requiresAuth: true }
  },
  {
    path: '/masraflar',
    name: 'Masraflar',
    component: Masraflar,
    meta: { requiresAuth: true }
  },
  {
    path: '/fiyat-listesi',
    name: 'FiyatListesi',
    component: FiyatListesi,
    meta: { requiresAuth: true }
  },
  {
    path: '/iadeler',
    name: 'Iadeler',
    component: Iadeler,
    meta: { requiresAuth: true }
  },
  {
    path: '/stok-seriler',
    name: 'StokSeriler',
    component: StokSeriler,
    meta: { requiresAuth: true }
  },
  {
    path: '/stok-sayim',
    name: 'StokSayim',
    component: StokSayim,
    meta: { requiresAuth: true }
  },
  {
    path: '/maas-bordro',
    name: 'MaasBordro',
    component: MaasBordro,
    meta: { requiresAuth: true }
  },
  {
    path: '/vardiyalar',
    name: 'Vardiyalar',
    component: Vardiyalar,
    meta: { requiresAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFound
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  if (to.meta.requiresAuth !== false && !authStore.isLoggedIn) {
    next('/giris')
  } else if (to.path === '/giris' && authStore.isLoggedIn && to.name === 'Giris') {
    next('/')
  } else if (to.path === '/kullanicilar' && authStore.kullanici?.role !== 'ADMIN') {
    next('/')
  } else {
    next()
  }
})

export default router
