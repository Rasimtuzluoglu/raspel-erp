<template>
  <div class="yasal-page">
    <PageHeader
      :title="t('yasal.title')"
      :subtitle="t('yasal.subtitle')"
    >
      <template #actions>
        <Button
          :label="t('kullanimSartlari.yazdir')"
          icon="pi pi-print"
          class="p-button-outlined p-button-secondary"
          @click="yazdir"
        />
      </template>
    </PageHeader>

    <TabView v-model:active-index="aktifSekme">
      <TabPanel :header="t('kullanimSartlari.title')">
        <YasalIcerik
          kod="kullanim"
          :maddeler="kullanimMaddeler"
          :hero-baslik="t('kullanimSartlari.heroBaslik')"
          :son-guncelleme="t('kullanimSartlari.sonGuncelleme')"
          :arama-placeholder="t('kullanimSartlari.aramaPlaceholder')"
          :icindekiler="t('kullanimSartlari.icindekiler')"
          :bulunamadi="t('kullanimSartlari.bulunamadi')"
          :filtre-temizle="t('kullanimSartlari.filtreTemizle')"
          hero-icon="pi pi-file-check"
        />
      </TabPanel>
      <TabPanel :header="t('gizlilikPolitikasi.title')">
        <YasalIcerik
          kod="gizlilik"
          :maddeler="gizlilikMaddeler"
          :hero-baslik="t('gizlilikPolitikasi.heroBaslik')"
          :son-guncelleme="t('gizlilikPolitikasi.sonGuncelleme')"
          :arama-placeholder="t('gizlilikPolitikasi.aramaPlaceholder')"
          :icindekiler="t('gizlilikPolitikasi.icindekiler')"
          :bulunamadi="t('gizlilikPolitikasi.bulunamadi')"
          :filtre-temizle="t('gizlilikPolitikasi.filtreTemizle')"
          hero-icon="pi pi-shield"
        />
      </TabPanel>
    </TabView>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import YasalIcerik from '../components/YasalIcerik.vue'

const { t } = useI18n()
const route = useRoute()

const aktifSekme = ref(0)

// Eski ayri rotalar (`/kullanim-sartlari`, `/gizlilik-politikasi`) bu sayfaya
// `?sekme=` ile yonlendirilir; hangi sekmenin acilacagini belirler.
const sekmeAyarla = () => {
  aktifSekme.value = route.query.sekme === 'gizlilik' ? 1 : 0
}
onMounted(sekmeAyarla)

const kullanimMaddeler = computed(() => [
  { id: 1, baslik: t('kullanimSartlari.madde1Baslik'), kategori: t('kullanimSartlari.madde1Kategori'), icon: 'pi pi-server', iconBg: 'rgba(59, 130, 246, 0.15)', iconColor: '#3b82f6', icerik: t('kullanimSartlari.madde1Icerik'), ipucu: t('kullanimSartlari.madde1Ipucu') },
  { id: 2, baslik: t('kullanimSartlari.madde2Baslik'), kategori: t('kullanimSartlari.madde2Kategori'), icon: 'pi pi-user-edit', iconBg: 'rgba(16, 185, 129, 0.15)', iconColor: '#10b981', icerik: t('kullanimSartlari.madde2Icerik'), ipucu: t('kullanimSartlari.madde2Ipucu') },
  { id: 3, baslik: t('kullanimSartlari.madde3Baslik'), kategori: t('kullanimSartlari.madde3Kategori'), icon: 'pi pi-database', iconBg: 'rgba(245, 158, 11, 0.15)', iconColor: '#f59e0b', icerik: t('kullanimSartlari.madde3Icerik'), ipucu: t('kullanimSartlari.madde3Ipucu') },
  { id: 4, baslik: t('kullanimSartlari.madde4Baslik'), kategori: t('kullanimSartlari.madde4Kategori'), icon: 'pi pi-exclamation-triangle', iconBg: 'rgba(239, 68, 68, 0.15)', iconColor: '#ef4444', icerik: t('kullanimSartlari.madde4Icerik'), ipucu: t('kullanimSartlari.madde4Ipucu') },
  { id: 5, baslik: t('kullanimSartlari.madde5Baslik'), kategori: t('kullanimSartlari.madde5Kategori'), icon: 'pi pi-cloud-download', iconBg: 'rgba(139, 92, 246, 0.15)', iconColor: '#8b5cf6', icerik: t('kullanimSartlari.madde5Icerik'), ipucu: t('kullanimSartlari.madde5Ipucu') },
  { id: 6, baslik: t('kullanimSartlari.madde6Baslik'), kategori: t('kullanimSartlari.madde6Kategori'), icon: 'pi pi-lock', iconBg: 'rgba(236, 72, 153, 0.15)', iconColor: '#ec4899', icerik: t('kullanimSartlari.madde6Icerik'), ipucu: t('kullanimSartlari.madde6Ipucu') },
  { id: 7, baslik: t('kullanimSartlari.madde7Baslik'), kategori: t('kullanimSartlari.madde7Kategori'), icon: 'pi pi-headphones', iconBg: 'rgba(6, 182, 212, 0.15)', iconColor: '#06b6d4', icerik: t('kullanimSartlari.madde7Icerik'), ipucu: t('kullanimSartlari.madde7Ipucu') }
])

const gizlilikMaddeler = computed(() => [
  { id: 1, baslik: t('gizlilikPolitikasi.madde1Baslik'), kategori: t('gizlilikPolitikasi.madde1Kategori'), icon: 'pi pi-folder-open', iconBg: 'rgba(59, 130, 246, 0.15)', iconColor: '#3b82f6', icerik: t('gizlilikPolitikasi.madde1Icerik'), ipucu: t('gizlilikPolitikasi.madde1Ipucu') },
  { id: 2, baslik: t('gizlilikPolitikasi.madde2Baslik'), kategori: t('gizlilikPolitikasi.madde2Kategori'), icon: 'pi pi-cog', iconBg: 'rgba(16, 185, 129, 0.15)', iconColor: '#10b981', icerik: t('gizlilikPolitikasi.madde2Icerik'), ipucu: t('gizlilikPolitikasi.madde2Ipucu') },
  { id: 3, baslik: t('gizlilikPolitikasi.madde3Baslik'), kategori: t('gizlilikPolitikasi.madde3Kategori'), icon: 'pi pi-lock', iconBg: 'rgba(139, 92, 246, 0.15)', iconColor: '#8b5cf6', icerik: t('gizlilikPolitikasi.madde3Icerik'), ipucu: t('gizlilikPolitikasi.madde3Ipucu') },
  { id: 4, baslik: t('gizlilikPolitikasi.madde4Baslik'), kategori: t('gizlilikPolitikasi.madde4Kategori'), icon: 'pi pi-verified', iconBg: 'rgba(245, 158, 11, 0.15)', iconColor: '#f59e0b', icerik: t('gizlilikPolitikasi.madde4Icerik'), ipucu: t('gizlilikPolitikasi.madde4Ipucu') },
  { id: 5, baslik: t('gizlilikPolitikasi.madde5Baslik'), kategori: t('gizlilikPolitikasi.madde5Kategori'), icon: 'pi pi-history', iconBg: 'rgba(6, 182, 212, 0.15)', iconColor: '#06b6d4', icerik: t('gizlilikPolitikasi.madde5Icerik'), ipucu: t('gizlilikPolitikasi.madde5Ipucu') },
  { id: 6, baslik: t('gizlilikPolitikasi.madde6Baslik'), kategori: t('gizlilikPolitikasi.madde6Kategori'), icon: 'pi pi-user-edit', iconBg: 'rgba(236, 72, 153, 0.15)', iconColor: '#ec4899', icerik: t('gizlilikPolitikasi.madde6Icerik'), ipucu: t('gizlilikPolitikasi.madde6Ipucu') },
  { id: 7, baslik: t('gizlilikPolitikasi.madde7Baslik'), kategori: t('gizlilikPolitikasi.madde7Kategori'), icon: 'pi pi-bell', iconBg: 'rgba(239, 68, 68, 0.15)', iconColor: '#ef4444', icerik: t('gizlilikPolitikasi.madde7Icerik'), ipucu: t('gizlilikPolitikasi.madde7Ipucu') }
])

const yazdir = () => {
  window.print()
}
</script>

<style scoped>
.yasal-page {
  padding: 0;
  max-width: 1300px;
  margin: 0 auto;
}
</style>
