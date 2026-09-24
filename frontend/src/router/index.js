import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/authStore.js'
import i18n from '../i18n.js'
import Giris from '../views/Giris.vue'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

NProgress.configure({ showSpinner: false, minimum: 0.12, trickleSpeed: 200 })

const routes = [
  {
    path: '/giris',
    name: 'Giris',
    component: Giris,
    meta: { requiresAuth: false }
  },
  {
    path: '/sifre-sifirla',
    name: 'SifreSifirla',
    component: () => import('../views/SifreSifirla.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue'),
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
    path: '/tekrarlayan-faturalar',
    name: 'TekrarlayanFaturalar',
    component: () => import('../views/TekrarlayanFaturalar.vue'),
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
    path: '/raporlar/karlilik-analizi',
    name: 'KarlilikAnalizi',
    component: () => import('../views/KarlilikAnalizi.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/raporlar/fatura-gecmis',
    name: 'FaturaGecmisRaporu',
    component: () => import('../views/FaturaGecmisRaporu.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/raporlar/gorunumler-360',
    name: 'Gorunumler360',
    component: () => import('../views/Gorunumler360.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/kategoriler',
    name: 'Kategoriler',
    component: () => import('../views/Kategoriler.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/adres-defteri',
    name: 'AdresDefteri',
    component: () => import('../views/AdresDefteri.vue'),
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
    path: '/yeni-yil-sihirbazi',
    name: 'YeniYilSihirbazi',
    component: () => import('../views/YeniYilSihirbazi.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
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
    path: '/siparis-takip',
    name: 'SiparisTakip',
    component: () => import('../views/SiparisTakip.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/teklifler',
    name: 'Teklifler',
    component: () => import('../views/Teklifler.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/yonetici-kokpiti',
    name: 'YoneticiKokpiti',
    component: () => import('../views/YoneticiKokpiti.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/saha-portali',
    name: 'SahaPortali',
    component: () => import('../views/SahaPortali.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/cek-senet',
    name: 'CekSenet',
    component: () => import('../views/CekSenet.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/tahsilat',
    name: 'Tahsilat',
    component: () => import('../views/Tahsilat.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/taksit-takvimi',
    name: 'TaksitTakvimi',
    component: () => import('../views/TaksitTakvimi.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/pos-terminalleri',
    name: 'PosTerminalleri',
    component: () => import('../views/PosTerminalleri.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/irsaliyeler',
    name: 'Irsaliyeler',
    component: () => import('../views/Irsaliyeler.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/teslimatlar',
    name: 'Teslimatlar',
    component: () => import('../views/Teslimatlar.vue'),
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
    path: '/iskonto-kurallari',
    name: 'IskontoKurallari',
    component: () => import('../views/IskontoKurallari.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/crm-merkezi',
    name: 'CrmMerkezi',
    component: () => import('../views/CrmMerkezi.vue'),
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
    path: '/uretim',
    name: 'Uretim',
    component: () => import('../views/Uretim.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/stok-duzeltmeler',
    name: 'StokDuzeltmeler',
    component: () => import('../views/StokDuzeltmeler.vue'),
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
    path: '/profil',
    redirect: '/hesap-ayarlari'
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
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    return { top: 0 }
  }
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
  } else if (authStore.isSaha && to.path !== '/yetki-reddi') {
    const sahaIzinli = [
      '/', // Yönlendirme dashboard'a gidecek oradan da portale düşecek
      '/saha-portali',
      '/stoklar',
      '/sohbet',
      '/ajanda',
      '/belgeler',
      '/notlar',
      '/hesap-ayarlari'
    ]
    if (!sahaIzinli.includes(to.path) && !to.path.startsWith('/profil')) {
      next('/yetki-reddi')
    } else {
      next()
    }
  } else if (authStore.isDriver && to.path !== '/yetki-reddi') {
    const driverIzinli = [
      '/',
      '/teslimatlar',
      '/saha-portali',
      '/sohbet',
      '/notlar',
      '/belgeler',
      '/hesap-ayarlari'
    ]
    if (!driverIzinli.includes(to.path) && !to.path.startsWith('/profil')) {
      next('/yetki-reddi')
    } else if (to.path === '/') {
      next('/teslimatlar')
    } else {
      next()
    }
  } else {
    next()
  }
})

// Yol -> i18n baslik anahtari. Sekme basligi yerellestirilir; eslesme yoksa
// yol parcasindan turetilir.
const ROTA_BASLIK_ANAHTARLARI = {
  '/': 'nav.dashboard',
  '/sohbet': 'nav.sohbet',
  '/ajanda': 'nav.ajanda',
  '/onaylar': 'nav.onaylar',
  '/belgeler': 'nav.belgeler',
  '/sistem-durum': 'nav.sistemDurum',
  '/muhasebe': 'nav.muhasebe',
  '/cari-hesaplar': 'nav.cari',
  '/faturalar': 'nav.faturalar',
  '/tekrarlayan-faturalar': 'nav.tekrarlayanFaturalar',
  '/bankalar': 'nav.banka',
  '/kasa': 'nav.kasa',
  '/banka-mutabakat': 'nav.bankaMutabakat',
  '/cek-senet': 'nav.ceksenet',
  '/tahsilat': 'nav.tahsilat',
  '/taksit-takvimi': 'nav.taksitTakvimi',
  '/pos-terminalleri': 'nav.posTerminalleri',
  '/butceler': 'nav.butce',
  '/masraflar': 'nav.masraf',
  '/satislar': 'nav.satis',
  '/hizli-satis': 'nav.hizliSatis',
  '/saha-portali': 'nav.sahaPortali',
  '/teklifler': 'nav.teklifler',
  '/crm': 'nav.crm',
  '/crm-merkezi': 'nav.crmMerkezi',
  '/adres-defteri': 'nav.adresDefteri',
  '/e-fatura': 'nav.eFatura',
  '/satinalma': 'nav.satinalma',
  '/siparisler': 'nav.siparis',
  '/siparis-takip': 'nav.siparisTakip',
  '/teslimatlar': 'nav.teslimatlar',
  '/irsaliyeler': 'nav.irsaliye',
  '/fiyat-listesi': 'nav.fiyatListesi',
  '/iskonto-kurallari': 'nav.iskontoKurallari',
  '/iadeler': 'nav.iade',
  '/stoklar': 'nav.stok',
  '/kritik-stok': 'nav.kritikStok',
  '/toplu-stok': 'nav.topluStok',
  '/depolar': 'nav.depo',
  '/stok-seriler': 'nav.serilot',
  '/stok-sayim': 'nav.stokSayim',
  '/stok-duzeltmeler': 'nav.stokDuzeltmeler',
  '/uretim': 'nav.uretim',
  '/subeler': 'nav.sube',
  '/personel': 'nav.personel',
  '/puantaj': 'nav.puantaj',
  '/izinler': 'nav.izin',
  '/projeler': 'nav.proje',
  '/maas-bordro': 'nav.maasBordro',
  '/vardiyalar': 'nav.vardiya',
  '/sirketler': 'nav.sirket',
  '/yeni-yil-sihirbazi': 'nav.yeniYil',
  '/donemler': 'nav.donem',
  '/kullanicilar': 'nav.kullanici',
  '/yetki-yonetimi': 'nav.yetkiler',
  '/kategoriler': 'nav.kategori',
  '/notlar': 'nav.notlar',
  '/veri-aktar': 'nav.veriAktar',
  '/kullanim-sartlari': 'nav.kullanimSartlari',
  '/gizlilik-politikasi': 'nav.gizlilik',
  '/hesap-ayarlari': 'nav.hesapAyarlari',
  '/yedekler': 'nav.yedek',
  '/yonetici-kokpiti': 'nav.yoneticiKokpiti',
  '/raporlar': 'nav.rapor',
  '/raporlar/karlilik-analizi': 'nav.karlilikAnalizi',
  '/raporlar/fatura-gecmis': 'nav.faturaGecmisRaporu',
  '/vergi-raporlari': 'nav.vergiRaporlari',
  '/anomaliler': 'nav.anomaliler',
  '/hareketler': 'nav.hareket',
  '/denetim': 'nav.denetim'
}

router.afterEach((to) => {
  NProgress.done()
  // Sekme basligi: route meta basligi -> yol anahtari -> yol parcasi
  const son = to.path.split('/').filter(Boolean).pop()
  const anahtar = to.meta?.titleKey || ROTA_BASLIK_ANAHTARLARI[to.path]
  const baslik = anahtar
    ? i18n.global.t(anahtar)
    : (son ? son.replace(/-/g, ' ').replace(/\b\w/g, (c) => c.toLocaleUpperCase('tr-TR')) : i18n.global.t('nav.dashboard'))
  document.title = `${baslik} · RasPel ERP`
})

/**
 * Bos zaman on-yukleme: kullanicinin gezinmesini beklemeden, tarayici bos
 * kaldiginda en sik kullanilan route chunk'larini arka planda indirir.
 * Boylece sayfa gecisleri aninda acilir (hizli program).
 */
function routeOnYukle() {
  const oncelikli = ['/', '/faturalar', '/cari-hesaplar', '/stoklar', '/hizli-satis', '/tahsilat', '/raporlar']
  const adlar = new Set(oncelikli)
  const indirilecek = routes.filter(
    (r) => r.component && typeof r.component === 'function' && adlar.has(r.path)
  )
  let i = 0
  const siradaki = () => {
    if (i >= indirilecek.length) return
    const r = indirilecek[i++]
    Promise.resolve(r.component()).catch(() => {}).finally(siradaki)
  }
  // requestIdleCallback varsa onu kullan, yoksa kucuk bir gecikmeyle baslat.
  if (typeof window !== 'undefined' && 'requestIdleCallback' in window) {
    window.requestIdleCallback(siradaki, { timeout: 4000 })
  } else {
    setTimeout(siradaki, 1500)
  }
}

if (typeof window !== 'undefined') {
  // Ilk sayfa tamamen yuklendikten sonra baslat.
  window.addEventListener('load', () => setTimeout(routeOnYukle, 500), { once: true })
}

export default router
