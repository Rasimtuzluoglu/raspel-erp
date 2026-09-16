<template>
  <nav
    v-if="items && items.length > 1"
    class="breadcrumb"
    aria-label="Breadcrumb"
  >
    <ol class="breadcrumb-list">
      <li
        v-for="(item, i) in items"
        :key="item.path"
        class="breadcrumb-item"
      >
        <template v-if="i < items.length - 1">
          <router-link
            :to="item.path"
            class="breadcrumb-link"
          >
            <i
              v-if="i === 0"
              class="pi pi-home breadcrumb-home"
            />
            <span>{{ item.label }}</span>
          </router-link>
          <i class="pi pi-chevron-right breadcrumb-sep" />
        </template>
        <span
          v-else
          class="breadcrumb-current"
        >{{ item.label }}</span>
      </li>
    </ol>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'

const route = useRoute()
const { t } = useI18n()

const labelKeyMap = {
  '/': 'nav.dashboard',
  '/cari-hesaplar': 'nav.cari',
  '/faturalar': 'nav.faturalar',
  '/bankalar': 'nav.banka',
  '/kasa': 'nav.kasa',
  '/muhasebe': 'nav.muhasebe',
  '/stoklar': 'nav.stok',
  '/personel': 'nav.personel',
  '/hizli-satis': 'nav.hizliSatis',
  '/siparisler': 'nav.siparis',
  '/raporlar': 'nav.rapor',
  '/raporlar/karlilik-analizi': 'nav.karlilikAnalizi',
  '/raporlar/fatura-gecmis': 'nav.faturaGecmisRaporu',
  '/hareketler': 'nav.hareket',
  '/notlar': 'nav.notlar',
  '/satislar': 'nav.satis',
  '/satinalma': 'nav.satinalma',
  '/cek-senet': 'nav.ceksenet',
  '/irsaliyeler': 'nav.irsaliye',
  '/projeler': 'nav.proje',
  '/denetim': 'nav.denetim',
  '/yedekler': 'nav.yedek',
  '/subeler': 'nav.sube',
  '/depolar': 'nav.depo',
  '/butceler': 'nav.butce',
  '/masraflar': 'nav.masraf',
  '/fiyat-listesi': 'nav.fiyatListesi',
  '/iadeler': 'nav.iade',
  '/stok-seriler': 'nav.serilot',
  '/stok-sayim': 'nav.stokSayim',
  '/maas-bordro': 'nav.maasBordro',
  '/vardiyalar': 'nav.vardiya',
  '/anomaliler': 'nav.anomaliler',
  '/kategoriler': 'nav.kategori',
  '/kullanicilar': 'nav.kullanici',
  '/toplu-stok': 'nav.topluStok',
  '/sirketler': 'nav.sirket',
  '/donemler': 'nav.donem',
  '/izinler': 'nav.izin',
  '/puantaj': 'nav.puantaj',
  '/crm': 'nav.crm',
  '/e-fatura': 'nav.eFatura',
  '/kritik-stok': 'nav.kritikStok',
  '/hesap-ayarlari': 'nav.hesapAyarlari',
  '/banka-mutabakat': 'nav.bankaMutabakat',
  '/vergi-raporlari': 'nav.vergiRaporlari',
  '/veri-aktar': 'nav.veriAktar',
  '/kullanim-sartlari': 'nav.kullanimSartlari',
  '/gizlilik-politikasi': 'nav.gizlilik',
  '/yetki-yonetimi': 'nav.yetkiler'
}

function pathLabel(path) {
  const key = labelKeyMap[path]
  if (key) return t(key)
  return path
    .replace(/^\//, '')
    .replace(/-/g, ' ')
    .replace(/\b\w/g, (c) => c.toLocaleUpperCase('tr-TR'))
}

const items = computed(() => {
  const segments = route.path.split('/').filter(Boolean)
  if (segments.length === 0) {
    return [{ path: '/', label: t('nav.dashboard') }]
  }
  const crumbs = [{ path: '/', label: t('nav.dashboard') }]
  let current = ''
  for (const seg of segments) {
    current += '/' + seg
    crumbs.push({ path: current, label: pathLabel(current) })
  }
  return crumbs
})
</script>

<style scoped>
.breadcrumb {
  padding: 0 0 12px;
  margin: 0 0 8px;
  border-bottom: 1px solid var(--border, rgba(148, 163, 184, 0.18));
  background: transparent;
  max-width: 100%;
  overflow: hidden;
}
.breadcrumb-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
  min-width: 0;
}
.breadcrumb-item {
  display: flex;
  align-items: center;
  font-size: 13px;
}
.breadcrumb-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--text-secondary, #64748b);
  text-decoration: none;
  border-radius: 4px;
  padding: 2px 6px;
  transition:
    color 0.15s,
    background 0.15s;
}
.breadcrumb-link:hover {
  color: var(--primary-color, #3b82f6);
  background: rgba(148, 163, 184, 0.14);
}
.breadcrumb-home {
  font-size: 13px;
}
.breadcrumb-sep {
  font-size: 10px;
  color: var(--text-muted, #94a3b8);
  margin: 0 4px;
}
.breadcrumb-current {
  color: var(--text-primary);
  font-weight: 500;
  padding: 2px 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 240px;
}
</style>
