<template>
  <div class="vardiya-container">
    <PageHeader
      :title="t('vardiyalar.title')"
      :subtitle="t('vardiyalar.aciklama')"
      icon="pi pi-calendar-plus"
    >
      <template #actions>
        <Button
          :label="t('vardiyalar.yeniVardiya')"
          icon="pi pi-plus"
          @click="dialogAc()"
        />
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
        :baslik="t('vardiyalar.kpiToplam')"
        :deger="ozet.toplam"
        ikon="pi pi-calendar"
        renk="#3b82f6"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('vardiyalar.kpiBugun')"
        :deger="ozet.bugun"
        ikon="pi pi-clock"
        renk="#10b981"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('vardiyalar.kpiBuHafta')"
        :deger="ozet.buHafta"
        ikon="pi pi-calendar-clock"
        renk="#f59e0b"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('vardiyalar.kpiPersonel')"
        :deger="ozet.personelSayisi"
        ikon="pi pi-users"
        renk="#8b5cf6"
        :para-birimi="false"
      />
    </div>

    <div class="vardiya-arac">
      <span class="arac-arama">
        <i class="pi pi-search" />
        <InputText
          v-model="arama"
          :placeholder="t('vardiyalar.aramaPlaceholder')"
          class="arama-input"
          @input="aramaGirdi"
        />
      </span>
      <Dropdown
        v-model="personelFiltre"
        :options="personelListesi"
        option-label="displayName"
        option-value="id"
        :placeholder="t('vardiyalar.personel')"
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
      <Dropdown
        v-model="turFiltre"
        :options="turSecenekleriTumu"
        option-label="label"
        option-value="value"
        class="arac-secim"
        @change="filtreDegisti"
      />
      <span class="arac-bosluk" />
      <Button
        :label="t('vardiyalar.excel')"
        icon="pi pi-file-excel"
        outlined
        size="small"
        :loading="excelYukleniyor"
        @click="excelIndir"
      />
    </div>

    <div
      class="vardiya-masa"
      :aria-busy="yukleniyor"
    >
      <AppDataTable
        :value="list"
        lazy
        :total-records="toplam"
        :rows="sayfaBoyutu"
        striped-rows
        :loading="yukleniyor"
        :empty-message="filtreVar ? t('vardiyalar.filtreBos') : t('common.noData')"
        gorunum-anahtari="vardiyalar"
        @page="sayfaDegisti"
        @sort="siralaDegisti"
      >
        <Column
          field="personelAdi"
          :header="t('vardiyalar.personel')"
          sortable
        />
        <Column
          field="tarih"
          :header="t('common.date')"
          sortable
        >
          <template #body="{ data }">
            {{ formatDate(data.tarih) }}
          </template>
        </Column>
        <Column
          field="baslangic"
          :header="t('vardiyalar.baslangic')"
        >
          <template #body="{ data }">
            {{ data.baslangic }}
          </template>
        </Column>
        <Column
          field="bitis"
          :header="t('vardiyalar.bitis')"
        >
          <template #body="{ data }">
            {{ data.bitis }}
          </template>
        </Column>
        <Column
          field="tur"
          :header="t('vardiyalar.tur')"
        >
          <template #body="{ data }">
            <Tag
              :value="turEtiket(data.tur)"
              :severity="data.tur === 'SABAH' ? 'info' : data.tur === 'AKSAM' ? 'warn' : 'contrast'"
            />
          </template>
        </Column>
        <Column
          :header="t('vardiyalar.islem')"
          style="width: 72px"
        >
          <template #body="{ data }">
            <SatirEylemleri
              :gorunur="{ duzenle: false, cogalt: false, sil: false }"
              :items="vardiyaAksiyonlari(data)"
              :baslik="data.personelAdi"
            />
          </template>
        </Column>
        <template #empty>
          <EmptyState
            :message="filtreVar ? t('vardiyalar.filtreBos') : t('common.noData')"
          />
        </template>
      </AppDataTable>
    </div>

    <!-- Mobil kart görünümü -->
    <div
      class="vardiya-mobil"
      :aria-busy="yukleniyor"
    >
      <span
        class="sr-only"
        role="status"
        aria-live="polite"
      >{{ t('vardiyalar.sonucSayisi', { sayi: toplam }) }}</span>
      <div
        v-if="yukleniyor && !list.length"
        class="vardiya-skeleton"
        role="status"
        :aria-label="t('common.loading')"
      >
        <SkeletonLoader
          :count="4"
          height="96px"
        />
      </div>
      <EmptyState
        v-else-if="!list.length"
        :message="filtreVar ? t('vardiyalar.filtreBos') : t('common.noData')"
      />
      <article
        v-for="v in list"
        :key="v.id"
        class="vardiya-mobil-kart"
      >
        <div class="vmk-ust">
          <button
            type="button"
            class="vmk-ad"
            :title="t('vardiyalar.detay')"
            @click="detayAc(v)"
          >
            <i class="pi pi-user" />
            <span>{{ v.personelAdi }}</span>
          </button>
          <SatirEylemleri
            :gorunur="{ duzenle: false, cogalt: false, sil: false }"
            :items="vardiyaAksiyonlari(v)"
            :baslik="v.personelAdi"
          />
        </div>
        <div class="vmk-satir">
          <Tag
            :value="turEtiket(v.tur)"
            :severity="v.tur === 'SABAH' ? 'info' : v.tur === 'AKSAM' ? 'warn' : 'contrast'"
          />
          <span class="vmk-alt"><i class="pi pi-calendar" /> {{ formatDate(v.tarih) }}</span>
        </div>
        <div class="vmk-satir vmk-alt">
          <span><i class="pi pi-clock" /> {{ v.baslangic }} – {{ v.bitis }}</span>
        </div>
      </article>
    </div>

    <Dialog
      v-model:visible="dialog"
      :header="dialogHeader"
      modal
      :style="{ width: '520px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('vardiyalar.personelZorunlu') }}</label>
          <Dropdown
            v-model="form.personelId"
            :options="personelListesi"
            option-label="displayName"
            option-value="id"
            :placeholder="t('vardiyalar.personelSec')"
            class="w-full"
            filter
          />
        </div>
        <div class="field">
          <label>{{ t('vardiyalar.tarihZorunlu') }}</label><DatePicker
            v-model="form.tarih"
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
        <div class="field-row">
          <div class="field">
            <label>{{ t('vardiyalar.baslangicZorunlu') }}</label><InputText
              v-model="form.baslangic"
              placeholder="08:00"
              class="w-full"
            />
          </div>
          <div class="field">
            <label>{{ t('vardiyalar.bitisZorunlu') }}</label><InputText
              v-model="form.bitis"
              placeholder="16:00"
              class="w-full"
            />
          </div>
        </div>
        <div class="field">
          <label>{{ t('vardiyalar.turZorunlu') }}</label>
          <Dropdown
            v-model="form.tur"
            :options="turSecenekleri"
            option-label="label"
            option-value="value"
            :placeholder="t('common.select')"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="dialog = false"
        />
        <Button
          :label="t('common.save')"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="kaydet"
        />
      </template>
    </Dialog>

    <VardiyaDetayDrawer
      v-model:visible="detayDrawer"
      :kayit="detay"
      :silinebilir="authStore.isAdmin"
      @duzenle="drawerDuzenle"
      @sil="drawerSil"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { vardiyaAPI, personelAPI, excelAPI } from '../api/index.js'
import { useAuthStore } from '../stores/authStore.js'
import PageHeader from '../components/PageHeader.vue'
import KpiKart from '../components/KpiKart.vue'
import SatirEylemleri from '../components/SatirEylemleri.vue'
import VardiyaDetayDrawer from '../components/VardiyaDetayDrawer.vue'
import SkeletonLoader from '../components/SkeletonLoader.vue'
import { useI18n } from 'vue-i18n'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const authStore = useAuthStore()
const { t } = useI18n()

const SAYFA_ANAHTAR = 'raspel_vardiyalar_rows'

const list = ref([])
const personelListesi = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const excelYukleniyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const form = ref({ personelId: null, tarih: new Date(), baslangic: '08:00', bitis: '16:00', tur: 'SABAH' })
const ozet = ref({ toplam: 0, bugun: 0, buHafta: 0, personelSayisi: 0 })
const detayDrawer = ref(false)
const detay = ref(null)

const arama = ref('')
const personelFiltre = ref(null)
const turFiltre = ref('TUMU')
const tarihAraligi = ref([new Date(new Date().getFullYear(), new Date().getMonth(), 1), new Date()])
const toplam = ref(0)
const sayfa = ref(0)
const sayfaBoyutu = ref(Number(localStorage.getItem(SAYFA_ANAHTAR)) || 25)
const sirala = ref('tarih,desc')
let aramaZamanlayici = null

const filtreVar = computed(
  () =>
    !!arama.value.trim() ||
    !!personelFiltre.value ||
    !!tarihAraligi.value?.[0] ||
    turFiltre.value !== 'TUMU'
)

const ozetYukle = async () => {
  try {
    const r = await vardiyaAPI.ozet()
    ozet.value = { ...ozet.value, ...(r.data || {}) }
  } catch {
    /* özet alınamazsa KPI sıfır kalır */
  }
}
const turSecenekleri = computed(() => [
  { label: t('vardiyalar.turSabah'), value: 'SABAH' },
  { label: t('vardiyalar.turAksam'), value: 'AKSAM' },
  { label: t('vardiyalar.turGece'), value: 'GECE' }
])
const turSecenekleriTumu = computed(() => [
  { label: t('vardiyalar.turTumu'), value: 'TUMU' },
  ...turSecenekleri.value
])
const turEtiket = (tur) => ({
  SABAH: t('vardiyalar.turSabah'),
  AKSAM: t('vardiyalar.turAksam'),
  GECE: t('vardiyalar.turGece')
})[tur] || tur

const dialogHeader = computed(() => (duzenleme.value ? t('vardiyalar.duzenle') : t('vardiyalar.yeniVardiya')))

import { formatTarih as formatDate, getLocalDateString } from '../utils/format.js'

const filtreParams = () => {
  const p = { page: sayfa.value, size: sayfaBoyutu.value, sort: sirala.value }
  if (personelFiltre.value) p.personelId = personelFiltre.value
  if (turFiltre.value !== 'TUMU') p.tur = turFiltre.value
  if (arama.value.trim()) p.q = arama.value.trim()
  if (tarihAraligi.value?.[0]) p.baslangic = getLocalDateString(tarihAraligi.value[0])
  if (tarihAraligi.value?.[1]) p.bitis = getLocalDateString(tarihAraligi.value[1])
  return p
}

const personelleriYukle = async () => {
  try {
    // Parametresiz cagri backend varsayilan size=50 doner ve dropdown kirpilir.
    const pR = await personelAPI.getAll({ size: 200 })
    personelListesi.value = unwrapList(pR).map((p) => ({
      ...p,
      displayName: p.ad && p.soyad ? `${p.ad} ${p.soyad}` : p.ad || p.id
    }))
  } catch {
    personelListesi.value = []
  }
}

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await vardiyaAPI.filtreli(filtreParams())
    list.value = unwrapList(r)
    toplam.value = r.data?.totalElements ?? list.value.length
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('vardiyalar.hataYukleme'))
  } finally {
    yukleniyor.value = false
  }
  ozetYukle()
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
    const res = await excelAPI.vardiyalar(params)
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `Vardiyalar-${new Date().toISOString().slice(0, 10)}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    toastBildirim.basarili(t('vardiyalar.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('vardiyalar.excelHata'))
  } finally {
    excelYukleniyor.value = false
  }
}

const vardiyaAksiyonlari = (v) => {
  const items = [{ etiket: t('vardiyalar.detay'), ikon: 'pi pi-search', islem: () => detayAc(v) }]
  items.push({ etiket: t('common.edit'), ikon: 'pi pi-pencil', islem: () => dialogAc(v) })
  if (authStore.isAdmin) {
    items.push({ etiket: t('common.delete'), ikon: 'pi pi-trash', sinif: 'eylem-sil', islem: () => sil(v) })
  }
  return items
}

const detayAc = (v) => {
  detay.value = v
  detayDrawer.value = true
}

const drawerDuzenle = (v) => {
  detayDrawer.value = false
  dialogAc(v)
}

const drawerSil = (v) => {
  detayDrawer.value = false
  sil(v)
}

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data
    ? { ...data, tarih: data.tarih ? new Date(data.tarih) : new Date() }
    : { personelId: null, tarih: new Date(), baslangic: '08:00', bitis: '16:00', tur: 'SABAH' }
  dialog.value = true
}

const kaydet = async () => {
  if (!form.value.personelId) {
    toastBildirim.uyari(t('vardiyalar.personelSeciniz'))
    return
  }
  if (!form.value.tarih) {
    toastBildirim.uyari(t('vardiyalar.tarihZorunluUyari'))
    return
  }
  const saatDeseni = /^\d{2}:\d{2}$/
  if (!saatDeseni.test(form.value.baslangic || '') || !saatDeseni.test(form.value.bitis || '')) {
    toastBildirim.uyari(t('vardiyalar.saatGecersiz'))
    return
  }
  if (form.value.baslangic === form.value.bitis) {
    toastBildirim.uyari(t('vardiyalar.ayniSaat'))
    return
  }
  if (!form.value.tur) {
    toastBildirim.uyari(t('vardiyalar.turZorunluUyari'))
    return
  }
  kaydediliyor.value = true
  try {
    const payload = {
      ...form.value,
      tarih: form.value.tarih ? getLocalDateString(form.value.tarih) : null
    }
    if (duzenleme.value) {
      await vardiyaAPI.update(form.value.id, payload)
      toastBildirim.basarili(t('vardiyalar.guncellendi'))
    } else {
      await vardiyaAPI.create(payload)
      toastBildirim.basarili(t('vardiyalar.olusturuldu'))
    }
    dialog.value = false
    const r = await vardiyaAPI.getAll()
    list.value = unwrapList(r)
    ozetYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('vardiyalar.islemBasarisiz'))
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  const personelAd = data.personelAdi || data.id
  confirm.require({
    message: t('vardiyalar.silOnayMesaj', { ad: personelAd }),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await vardiyaAPI.delete(data.id)
        list.value = list.value.filter((x) => x.id !== data.id)
        ozetYukle()
        toast.add({ severity: 'success', summary: t('vardiyalar.silindi'), detail: t('vardiyalar.vardiyaSilindi'), life: 3000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('vardiyalar.silmeBasarisiz'))
      }
    }
  })
}
</script>

<style scoped>
.vardiya-container {
  padding: 0;
}
.kpi-satiri {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.vardiya-arac {
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
  min-width: 170px;
}
.arac-tarih {
  min-width: 220px;
}
.vardiya-mobil {
  display: none;
}
.vardiya-skeleton {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.vardiya-mobil-kart {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
}
.vmk-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.vmk-ad {
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
.vmk-ad:hover span {
  color: var(--accent);
}
.vmk-ad i {
  color: var(--accent);
  flex-shrink: 0;
}
.vmk-ad span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.vmk-satir {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12.5px;
  color: var(--text-secondary);
}
.vmk-satir i {
  font-size: 11.5px;
  color: var(--text-muted);
}
.vmk-alt {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--text-muted);
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
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.field-row {
  display: flex;
  gap: 12px;
}
.field-row .field {
  flex: 1;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}
.w-full {
  width: 100%;
}
@media (max-width: 768px) {
  .vardiya-masa {
    display: none;
  }
  .vardiya-mobil {
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
}
@media (prefers-reduced-motion: reduce) {
  .vmk-ad span {
    transition: none;
  }
}
</style>
