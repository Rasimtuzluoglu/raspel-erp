<template>
  <div class="izinler-sayfasi">
    <PageHeader
      :title="t('izinler.title')"
      :subtitle="t('izinler.aciklama')"
      icon="pi pi-calendar"
    >
      <template #actions>
        <Button
          icon="pi pi-refresh"
          :aria-label="t('common.refresh')"
          class="p-button-text p-button-sm"
          :loading="yukleniyor"
          @click="yukle"
        />
      </template>
    </PageHeader>

    <div class="kpi-satiri">
      <KpiKart
        :baslik="t('izinler.kpiBekleyen')"
        :deger="ozet.bekleyen"
        ikon="pi pi-hourglass"
        renk="#f59e0b"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('izinler.kpiOnaylanan')"
        :deger="ozet.onaylandi"
        ikon="pi pi-check-circle"
        renk="#10b981"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('izinler.kpiReddedilen')"
        :deger="ozet.reddedildi"
        ikon="pi pi-times-circle"
        renk="#ef4444"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('izinler.kpiBugunIzinli')"
        :deger="ozet.bugunIzinli"
        ikon="pi pi-user-minus"
        renk="#3b82f6"
        :para-birimi="false"
      />
    </div>

    <div class="filtre-grup">
      <SelectButton
        v-model="durumFiltre"
        :options="filtreSecenekleri"
        option-label="label"
        option-value="value"
        @change="filtreDegisti"
      />
    </div>

    <div class="izin-arac">
      <span class="arac-arama">
        <i class="pi pi-search" />
        <InputText
          v-model="arama"
          :placeholder="t('izinler.aramaPlaceholder')"
          class="arama-input"
          @input="aramaGirdi"
        />
      </span>
      <Dropdown
        v-model="personelFiltre"
        :options="personelListesi"
        option-label="displayName"
        option-value="id"
        :placeholder="t('izinler.personel')"
        show-clear
        filter
        class="arac-secim"
        @change="filtreDegisti"
      />
      <DatePicker
        v-model="tarihAraligi"
        selection-mode="range"
        :placeholder="t('common.date')"
        date-format="dd.mm.yy"
        show-button-bar
        class="arac-tarih"
        @update:model-value="filtreDegisti"
      />
      <span class="arac-bosluk" />
      <Button
        :label="t('izinler.excel')"
        icon="pi pi-file-excel"
        outlined
        size="small"
        :loading="excelYukleniyor"
        @click="excelIndir"
      />
    </div>

    <div
      class="izin-masa"
      :aria-busy="yukleniyor"
    >
      <AppDataTable
        :value="tumIzinler"
        lazy
        :total-records="toplam"
        :rows="sayfaBoyutu"
        striped-rows
        :loading="yukleniyor"
        :empty-message="filtreVar ? t('izinler.filtreBos') : t('common.noData')"
        gorunum-anahtari="izinler"
        @page="sayfaDegisti"
        @sort="siralaDegisti"
      >
        <Column
          field="personelAdi"
          :header="t('izinler.personel')"
          sortable
        />
        <Column
          field="izinTuru"
          :header="t('izinler.izinTuru')"
          sortable
        >
          <template #body="{ data }">
            {{ izinTuruLabel(data.izinTuru) }}
          </template>
        </Column>
        <Column
          field="baslangic"
          :header="t('izinler.baslangic')"
          sortable
        >
          <template #body="{ data }">
            {{ formatDate(data.baslangic) }}
          </template>
        </Column>
        <Column
          field="bitis"
          :header="t('izinler.bitis')"
          sortable
        >
          <template #body="{ data }">
            {{ formatDate(data.bitis) }}
          </template>
        </Column>
        <Column
          field="gunSayisi"
          :header="t('izinler.gun')"
          sortable
        />
        <Column
          field="durum"
          :header="t('common.status')"
          sortable
        >
          <template #body="{ data }">
            <Tag
              :value="durumLabel(data.durum)"
              :severity="data.durum === 'ONAYLANDI' ? 'success' : data.durum === 'REDDEDILDI' ? 'danger' : 'warn'"
            />
          </template>
        </Column>
        <Column
          field="aciklama"
          :header="t('common.description')"
          class="izin-aciklama-kolon"
        >
          <template #body="{ data }">
            {{ data.aciklama || '-' }}
          </template>
        </Column>
        <Column
          :header="t('common.actions')"
          style="width: 72px"
        >
          <template #body="{ data }">
            <SatirEylemleri
              :gorunur="{ duzenle: false, cogalt: false, sil: false }"
              :items="izinAksiyonlari(data)"
              :baslik="data.personelAdi"
            />
          </template>
        </Column>
        <template #empty>
          <EmptyState
            icon="pi pi-calendar"
            :message="filtreVar ? t('izinler.filtreBos') : t('common.noData')"
          />
        </template>
      </AppDataTable>
    </div>

    <!-- Mobil kart görünümü -->
    <div
      class="izin-mobil"
      :aria-busy="yukleniyor"
    >
      <span
        class="sr-only"
        role="status"
        aria-live="polite"
      >{{ t('izinler.sonucSayisi', { sayi: toplam }) }}</span>
      <div
        v-if="yukleniyor && !tumIzinler.length"
        class="izin-skeleton"
        role="status"
        :aria-label="t('common.loading')"
      >
        <SkeletonLoader
          :count="4"
          height="104px"
        />
      </div>
      <EmptyState
        v-else-if="!tumIzinler.length"
        :message="filtreVar ? t('izinler.filtreBos') : t('common.noData')"
        icon="pi pi-calendar"
      />
      <article
        v-for="i in tumIzinler"
        :key="i.id"
        class="izin-mobil-kart"
      >
        <div class="imk-ust">
          <button
            type="button"
            class="imk-ad"
            :title="t('izinler.detay')"
            @click="detayAc(i)"
          >
            <i class="pi pi-user" />
            <span>{{ i.personelAdi }}</span>
          </button>
          <SatirEylemleri
            :gorunur="{ duzenle: false, cogalt: false, sil: false }"
            :items="izinAksiyonlari(i)"
            :baslik="i.personelAdi"
          />
        </div>
        <div class="imk-satir">
          <Tag
            :value="durumLabel(i.durum)"
            :severity="i.durum === 'ONAYLANDI' ? 'success' : i.durum === 'REDDEDILDI' ? 'danger' : 'warn'"
          />
          <span class="imk-alt">{{ izinTuruLabel(i.izinTuru) }}</span>
        </div>
        <div class="imk-satir imk-tarih">
          <span><i class="pi pi-calendar" /> {{ formatDate(i.baslangic) }} – {{ formatDate(i.bitis) }}</span>
          <span class="imk-alt">{{ t('izinler.gun') }}: {{ i.gunSayisi }}</span>
        </div>
        <p
          v-if="i.aciklama"
          class="imk-aciklama"
        >
          {{ i.aciklama }}
        </p>
      </article>
    </div>

    <IzinDetayDrawer
      v-model:visible="detayDrawer"
      :izin="detay"
      :yukleniyor="detayYukleniyor"
      :onaylanabilir="canApprove && detay?.durum === 'BEKLEMEDE'"
      :silinebilir="authStore.isAdmin"
      @durum="drawerDurum"
      @sil="drawerSil"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useAuthStore } from '../stores/authStore.js'
import { personelIzinAPI, personelAPI, excelAPI } from '../api/index.js'
import PageHeader from '../components/PageHeader.vue'
import KpiKart from '../components/KpiKart.vue'
import SatirEylemleri from '../components/SatirEylemleri.vue'
import IzinDetayDrawer from '../components/IzinDetayDrawer.vue'
import SkeletonLoader from '../components/SkeletonLoader.vue'
import { useI18n } from 'vue-i18n'
import { formatTarih as formatDate, getLocalDateString } from '../utils/format.js'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const authStore = useAuthStore()
const { t } = useI18n()

const SAYFA_ANAHTAR = 'raspel_izinler_rows'

const yukleniyor = ref(false)
const excelYukleniyor = ref(false)
const tumIzinler = ref([])
const durumFiltre = ref('TUMU')
const arama = ref('')
const personelFiltre = ref(null)
const personelListesi = ref([])
const tarihAraligi = ref(null)
const toplam = ref(0)
const sayfa = ref(0)
const sayfaBoyutu = ref(Number(localStorage.getItem(SAYFA_ANAHTAR)) || 25)
const sirala = ref('baslangic,desc')
let aramaZamanlayici = null

const detayDrawer = ref(false)
const detay = ref(null)
const detayYukleniyor = ref(false)
const canApprove = computed(() => ['ADMIN', 'MUHASEBE'].includes(authStore?.kullanici?.role))

const ozet = ref({ bekleyen: 0, onaylandi: 0, reddedildi: 0, bugunIzinli: 0 })
const filtreSecenekleri = computed(() => [
  { label: t('izinler.tumu'), value: 'TUMU' },
  { label: t('izinler.beklemede'), value: 'BEKLEMEDE' },
  { label: t('izinler.onaylandi'), value: 'ONAYLANDI' },
  { label: t('izinler.reddedildi'), value: 'REDDEDILDI' }
])

const filtreVar = computed(
  () =>
    !!arama.value.trim() ||
    !!personelFiltre.value ||
    !!tarihAraligi.value?.[0] ||
    durumFiltre.value !== 'TUMU'
)

const izinTuruLabel = (tip) =>
  ({
    YILLIK_IZIN: t('izinler.yillikIzin'),
    HASTA_IZNI: t('izinler.hastaIzni'),
    MAZERET_IZNI: t('izinler.mazeretIzni'),
    DOGUM_IZNI: t('izinler.dogumIzni'),
    BABALIK_IZNI: t('izinler.babalikIzni'),
    EVLILIK_IZNI: t('izinler.evlilikIzni'),
    UCRETSIZ_IZIN: t('izinler.ucretsizIzin')
  })[tip] || tip

const durumLabel = (d) => ({ BEKLEMEDE: t('izinler.beklemede'), ONAYLANDI: t('izinler.onaylandi'), REDDEDILDI: t('izinler.reddedildi') })[d] || d

const filtreParams = () => {
  const p = { page: sayfa.value, size: sayfaBoyutu.value, sort: sirala.value }
  if (durumFiltre.value !== 'TUMU') p.durum = durumFiltre.value
  if (arama.value.trim()) p.q = arama.value.trim()
  if (personelFiltre.value) p.personelId = personelFiltre.value
  if (tarihAraligi.value?.[0]) p.baslangic = getLocalDateString(tarihAraligi.value[0])
  if (tarihAraligi.value?.[1]) p.bitis = getLocalDateString(tarihAraligi.value[1])
  return p
}

const ozetYukle = async () => {
  try {
    const r = await personelIzinAPI.ozet()
    ozet.value = { ...ozet.value, ...(r.data || {}) }
  } catch {
    /* özet alınamazsa KPI sıfır kalır */
  }
}

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await personelIzinAPI.filtreli(filtreParams())
    tumIzinler.value = unwrapList(r)
    toplam.value = r.data?.totalElements ?? tumIzinler.value.length
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('izinler.hataYukleme'))
  }
  yukleniyor.value = false
  ozetYukle()
}

const personelleriYukle = async () => {
  try {
    const r = await personelAPI.getAll({ size: 500 })
    personelListesi.value = unwrapList(r).map((p) => ({
      ...p,
      displayName: p.ad && p.soyad ? `${p.ad} ${p.soyad}` : p.ad || String(p.id)
    }))
  } catch {
    personelListesi.value = []
  }
}

onMounted(async () => {
  await personelleriYukle()
  await yukle()
})

const filtreDegisti = () => {
  sayfa.value = 0
  yukle()
}

const aramaGirdi = () => {
  clearTimeout(aramaZamanlayici)
  aramaZamanlayici = setTimeout(filtreDegisti, 300)
}

const sayfaDegisti = (e) => {
  sayfa.value = e.page
  sayfaBoyutu.value = e.rows
  localStorage.setItem(SAYFA_ANAHTAR, String(e.rows))
  yukle()
}

const siralaDegisti = (e) => {
  sirala.value = `${e.sortField},${e.sortOrder === 1 ? 'asc' : 'desc'}`
  sayfa.value = 0
  yukle()
}

const excelIndir = async () => {
  excelYukleniyor.value = true
  try {
    const params = filtreParams()
    delete params.page
    delete params.size
    delete params.sort
    const res = await excelAPI.izinler(params)
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `Izinler-${new Date().toISOString().slice(0, 10)}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    toastBildirim.basarili(t('izinler.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('izinler.excelHata'))
  } finally {
    excelYukleniyor.value = false
  }
}

const izinAksiyonlari = (i) => {
  const items = [{ etiket: t('izinler.detay'), ikon: 'pi pi-search', islem: () => detayAc(i) }]
  if (canApprove.value && i.durum === 'BEKLEMEDE') {
    items.push({ etiket: t('izinler.onayla'), ikon: 'pi pi-check', sinif: 'eylem-pozitif', islem: () => onayla(i) })
    items.push({ etiket: t('izinler.reddet'), ikon: 'pi pi-times', sinif: 'eylem-uyari', islem: () => reddet(i) })
  }
  if (authStore.isAdmin) {
    items.push({ etiket: t('common.delete'), ikon: 'pi pi-trash', sinif: 'eylem-sil', islem: () => sil(i) })
  }
  return items
}

const detayAc = async (i) => {
  detay.value = null
  detayDrawer.value = true
  detayYukleniyor.value = true
  try {
    const r = await personelIzinAPI.getById(i.id)
    detay.value = r.data || null
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('izinler.detayYuklenemedi'))
  } finally {
    detayYukleniyor.value = false
  }
}

const drawerDurum = (durum) => {
  const hedef = detay.value
  detayDrawer.value = false
  if (!hedef) return
  if (durum === 'ONAYLANDI') onayla(hedef)
  else reddet(hedef)
}

const drawerSil = () => {
  const hedef = detay.value
  detayDrawer.value = false
  if (hedef) sil(hedef)
}

const onayla = (data) => {
  confirm.require({
    message: t('izinler.onayOnayMesaj', { personel: data.personelAdi, tur: izinTuruLabel(data.izinTuru) }),
    header: t('izinler.izinOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('izinler.onayla'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await personelIzinAPI.durumGuncelle(data.id, 'ONAYLANDI', kullaniciAdi.value)
        await yukle()
        toastBildirim.basarili(t('izinler.izinOnaylandi'))
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('izinler.islemBasarisiz'))
      }
    }
  })
}

const reddet = (data) => {
  confirm.require({
    message: t('izinler.redOnayMesaj', { personel: data.personelAdi, tur: izinTuruLabel(data.izinTuru) }),
    header: t('izinler.izinReddi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('izinler.reddet'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await personelIzinAPI.durumGuncelle(data.id, 'REDDEDILDI', kullaniciAdi.value)
        await yukle()
        toastBildirim.basarili(t('izinler.izinReddedildi'))
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('izinler.islemBasarisiz'))
      }
    }
  })
}

const kullaniciAdi = computed(() => authStore?.kullanici?.displayName || authStore?.kullanici?.username || 'Admin')

const sil = (data) => {
  confirm.require({
    message: t('izinler.silOnayMesaj'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await personelIzinAPI.delete(data.id)
        await yukle()
        toast.add({ severity: 'success', summary: t('izinler.silindi'), detail: t('izinler.izinSilindi'), life: 5000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('izinler.silmeBasarisiz'))
      }
    }
  })
}
</script>

<style scoped>
.izinler-sayfasi {
  padding: 0;
}
.kpi-satiri {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.filtre-grup {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  max-width: 100%;
  margin-bottom: 12px;
}
.izin-arac {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.arac-bosluk {
  flex: 1 1 auto;
}
.arac-arama {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md, 10px);
  background: var(--bg-card);
  min-width: 220px;
  flex: 1 1 240px;
  max-width: 360px;
}
.arac-arama i {
  color: var(--text-muted);
  font-size: 13px;
}
.arac-arama :deep(.arama-input) {
  border: none;
  background: transparent;
  box-shadow: none;
  width: 100%;
  padding-left: 0;
}
.arac-secim {
  min-width: 200px;
}
.arac-tarih {
  min-width: 220px;
}
.izin-mobil {
  display: none;
}
.izin-skeleton {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.izin-mobil-kart {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
}
.imk-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.imk-ad {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--text-primary);
  font-size: 13.5px;
  font-weight: 700;
  text-align: left;
  cursor: pointer;
}
.imk-ad:hover span {
  color: var(--accent);
}
.imk-ad i {
  color: var(--accent);
  flex-shrink: 0;
}
.imk-ad span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.imk-satir {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12.5px;
  color: var(--text-secondary);
}
.imk-tarih span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.imk-tarih i {
  font-size: 11.5px;
  color: var(--text-muted);
}
.imk-alt {
  font-size: 12px;
  color: var(--text-muted);
}
.imk-aciklama {
  margin: 0;
  font-size: 12px;
  color: var(--text-secondary);
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
/* Uzun aciklama kolonu tabloyu genisletip gereksiz yatay scroll uretmesin. */
.izin-aciklama-kolon {
  max-width: 260px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
@media (max-width: 1200px) {
  .izin-aciklama-kolon {
    display: none;
  }
}
@media (max-width: 768px) {
  .izin-masa {
    display: none;
  }
  .izin-mobil {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .arac-bosluk {
    display: none;
  }
  .arac-arama {
    flex: 1 1 100%;
    max-width: none;
  }
  .arac-secim,
  .arac-tarih {
    flex: 1 1 45%;
    min-width: 0;
  }
  .filtre-grup {
    overflow-x: auto;
    flex-wrap: nowrap;
    padding-bottom: 2px;
  }
}
@media (prefers-reduced-motion: reduce) {
  .imk-ad span {
    transition: none;
  }
}
</style>
