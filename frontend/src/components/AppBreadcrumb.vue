<template>
  <nav
    v-if="items.length > 1"
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

const route = useRoute()

const labelMap = {
  '/': 'Ana Sayfa',
  '/cari-hesaplar': 'Cari Hesaplar',
  '/faturalar': 'Faturalar',
  '/bankalar': 'Bankalar',
  '/kasa': 'Kasa',
  '/muhasebe': 'Muhasebe',
  '/stoklar': 'Stoklar',
  '/personel': 'Personel',
  '/hizli-satis': 'Hızlı Satış',
  '/siparisler': 'Siparişler',
  '/raporlar': 'Raporlar',
  '/hareketler': 'Hareketler',
  '/notlar': 'Notlar',
  '/satislar': 'Satış',
  '/satinalma': 'Satın Alma',
  '/cek-senet': 'Çek/Senet',
  '/irsaliyeler': 'İrsaliyeler',
  '/projeler': 'Projeler',
  '/denetim': 'Denetim',
  '/yedekler': 'Yedekler',
  '/subeler': 'Şubeler',
  '/depolar': 'Depolar',
  '/butceler': 'Bütçeler',
  '/masraflar': 'Masraflar',
  '/fiyat-listesi': 'Fiyat Listesi',
  '/iadeler': 'İadeler',
  '/stok-seriler': 'Stok Seriler',
  '/stok-sayim': 'Stok Sayım',
  '/maas-bordro': 'Maaş Bordro',
  '/vardiyalar': 'Vardiyalar',
  '/anomaliler': 'Anomaliler',
  '/kategoriler': 'Kategoriler',
  '/kullanicilar': 'Kullanıcılar',
  '/toplu-stok': 'Toplu Stok',
  '/sirketler': 'Şirketler',
  '/donemler': 'Dönemler',
  '/izinler': 'İzinler',
  '/puantaj': 'Puantaj',
  '/crm': 'CRM',
  '/e-fatura': 'E-Fatura',
  '/kritik-stok': 'Kritik Stok',
  '/hesap-ayarlari': 'Hesap Ayarları',
  '/banka-mutabakat': 'Banka Mutabakatı',
  '/vergi-raporlari': 'Vergi Raporları',
  '/veri-aktar': 'Veri Aktar',
  '/kullanim-sartlari': 'Kullanım Şartları',
  '/gizlilik-politikasi': 'Gizlilik Politikası',
  '/yetki-yonetimi': 'Yetki Yönetimi'
}

function pathLabel(path) {
  return (
    labelMap[path] ||
    path
      .replace(/^\//, '')
      .replace(/-/g, ' ')
      .replace(/\b\w/g, (c) => c.toLocaleUpperCase('tr-TR'))
  )
}

const items = computed(() => {
  const segments = route.path.split('/').filter(Boolean)
  if (segments.length === 0) {
    return [{ path: '/', label: 'Ana Sayfa' }]
  }
  const crumbs = [{ path: '/', label: 'Ana Sayfa' }]
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
  padding: 8px 24px;
  border-bottom: 1px solid var(--surface-border, #e2e8f0);
  background: var(--surface-card, #ffffff);
}
.breadcrumb-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
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
  background: var(--surface-hover, #f1f5f9);
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
  color: var(--text-primary, #1e293b);
  font-weight: 500;
  padding: 2px 6px;
}
</style>
