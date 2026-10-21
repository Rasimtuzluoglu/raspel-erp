<template>
  <div class="puantaj-container">
    <PageHeader
      :title="t('puantaj.title')"
      :subtitle="t('puantaj.aciklama')"
      icon="pi pi-clock"
    >
      <template #actions>
        <Button
          :label="t('puantaj.yeniKayit')"
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
        :baslik="t('puantaj.kpiGeldi')"
        :deger="ozet.geldi"
        ikon="pi pi-check-circle"
        renk="#10b981"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('puantaj.kpiGelmedi')"
        :deger="ozet.gelmedi"
        ikon="pi pi-times-circle"
        renk="#ef4444"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('puantaj.kpiIzinli')"
        :deger="ozet.izinli"
        ikon="pi pi-calendar"
        renk="#f59e0b"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('puantaj.kpiMazeretli')"
        :deger="ozet.mazeretli"
        ikon="pi pi-file"
        renk="#3b82f6"
        :para-birimi="false"
      />
    </div>

    <div class="puantaj-arac">
      <span class="arac-arama">
        <i class="pi pi-search" />
        <InputText
          v-model="arama"
          :placeholder="t('puantaj.aramaPlaceholder')"
          class="arama-input"
          @input="aramaGirdi"
        />
      </span>
      <Dropdown
        v-model="personelFiltre"
        :options="personelList"
        option-label="displayName"
        option-value="id"
        :placeholder="t('puantaj.personel')"
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
        v-model="durumFiltre"
        :options="durumSecenekleri"
        option-label="label"
        option-value="value"
        class="arac-secim"
        @change="filtreDegisti"
      />
      <span class="arac-bosluk" />
      <Button
        :label="t('puantaj.excel')"
        icon="pi pi-file-excel"
        outlined
        size="small"
        :loading="excelYukleniyor"
        @click="excelIndir"
      />
    </div>

    <div
      class="puantaj-masa"
      :aria-busy="yukleniyor"
    >
      <AppDataTable
        :value="list"
        lazy
        :total-records="toplam"
        :rows="sayfaBoyutu"
        striped-rows
        :loading="yukleniyor"
        :empty-message="filtreVar ? t('puantaj.filtreBos') : t('common.noData')"
        gorunum-anahtari="puantaj"
        @page="sayfaDegisti"
        @sort="siralaDegisti"
      >
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
          field="personelAdi"
          :header="t('puantaj.personel')"
          sortable
        />
        <Column
          field="durum"
          :header="t('common.status')"
          sortable
        >
          <template #body="{ data }">
            <Tag
              :value="durumEtiket(data.durum)"
              :severity="data.durum === 'GELDI' ? 'success' : data.durum === 'IZINLI' ? 'warn' : 'danger'"
            />
          </template>
        </Column>
        <Column
          field="aciklama"
          :header="t('common.description')"
        />
        <Column
          :header="t('puantaj.islem')"
          style="width: 72px"
        >
          <template #body="{ data }">
            <SatirEylemleri
              :gorunur="{ duzenle: false, cogalt: false, sil: false }"
              :items="puantajAksiyonlari(data)"
              :baslik="data.personelAdi"
            />
          </template>
        </Column>
        <template #empty>
          <EmptyState
            :message="filtreVar ? t('puantaj.filtreBos') : t('puantaj.kayitBulunamadi')"
          />
        </template>
      </AppDataTable>
    </div>

    <!-- Mobil kart görünümü -->
    <div
      class="puantaj-mobil"
      :aria-busy="yukleniyor"
    >
      <span
        class="sr-only"
        role="status"
        aria-live="polite"
      >{{ t('puantaj.sonucSayisi', { sayi: toplam }) }}</span>
      <div
        v-if="yukleniyor && !list.length"
        class="puantaj-skeleton"
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
        :message="filtreVar ? t('puantaj.filtreBos') : t('puantaj.kayitBulunamadi')"
      />
      <article
        v-for="k in list"
        :key="k.id"
        class="puantaj-mobil-kart"
      >
        <div class="pmk-ust">
          <button
            type="button"
            class="pmk-ad"
            :title="t('puantaj.detay')"
            @click="detayAc(k)"
          >
            <i class="pi pi-user" />
            <span>{{ k.personelAdi }}</span>
          </button>
          <SatirEylemleri
            :gorunur="{ duzenle: false, cogalt: false, sil: false }"
            :items="puantajAksiyonlari(k)"
            :baslik="k.personelAdi"
          />
        </div>
        <div class="pmk-satir">
          <Tag
            :value="durumEtiket(k.durum)"
            :severity="k.durum === 'GELDI' ? 'success' : k.durum === 'IZINLI' ? 'warn' : 'danger'"
          />
          <span class="pmk-alt"><i class="pi pi-calendar" /> {{ formatDate(k.tarih) }}</span>
        </div>
        <p
          v-if="k.aciklama"
          class="pmk-aciklama"
        >
          {{ k.aciklama }}
        </p>
      </article>
    </div>
    <Dialog
      v-model:visible="dialog"
      :header="duzenleme ? t('puantaj.duzenle') : t('puantaj.yeni')"
      modal
      :style="{ width: '520px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label>{{ t('puantaj.personelZorunlu') }}</label><Dropdown
            v-model="form.personelId"
            :options="personelList"
            option-label="displayName"
            option-value="id"
            :placeholder="t('faturalar.seciniz')"
            class="w-full"
            filter
          />
        </div>
        <div class="field">
          <label>{{ t('puantaj.tarihZorunlu') }}</label><DatePicker
            v-model="form.tarih"
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('common.status') }}</label><Dropdown
            v-model="form.durum"
            :options="durumList"
            option-label="label"
            option-value="value"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('common.description') }}</label><Textarea
            v-model="form.aciklama"
            rows="2"
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

    <PuantajDetayDrawer
      v-model:visible="detayDrawer"
      :kayit="detay"
      :duzenlenebilir="true"
      :silinebilir="authStore.isAdmin"
      @duzenle="drawerDuzenle"
      @sil="drawerSil"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { puantajAPI, personelAPI, excelAPI } from '../api/index.js'
import { useAuthStore } from '../stores/authStore.js'
import PageHeader from '../components/PageHeader.vue'
import KpiKart from '../components/KpiKart.vue'
import SatirEylemleri from '../components/SatirEylemleri.vue'
import PuantajDetayDrawer from '../components/PuantajDetayDrawer.vue'
import SkeletonLoader from '../components/SkeletonLoader.vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { useI18n } from 'vue-i18n'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const authStore = useAuthStore()
const { t } = useI18n()

const SAYFA_ANAHTAR = 'raspel_puantaj_rows'

const list = ref([])
const personelList = ref([])
const yukleniyor = ref(false)
const excelYukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const personelFiltre = ref(null)
const durumFiltre = ref('TUMU')
const arama = ref('')
const tarihAraligi = ref([new Date(new Date().getFullYear(), new Date().getMonth(), 1), new Date()])
const toplam = ref(0)
const sayfa = ref(0)
const sayfaBoyutu = ref(Number(localStorage.getItem(SAYFA_ANAHTAR)) || 25)
const sirala = ref('tarih,desc')
let aramaZamanlayici = null

const durumList = computed(() => [
  { label: t('puantaj.durumGeldi'), value: 'GELDI' },
  { label: t('puantaj.durumGelmedi'), value: 'GELMEDI' },
  { label: t('puantaj.durumIzinli'), value: 'IZINLI' },
  { label: t('puantaj.durumMazeretli'), value: 'MAZERETLI' }
])
const durumSecenekleri = computed(() => [
  { label: t('puantaj.durumTumu'), value: 'TUMU' },
  ...durumList.value
])
const filtreVar = computed(
  () =>
    !!arama.value.trim() ||
    !!personelFiltre.value ||
    !!tarihAraligi.value?.[0] ||
    durumFiltre.value !== 'TUMU'
)
const durumEtiket = (d) => ({
  GELDI: t('puantaj.durumGeldi'),
  GELMEDI: t('puantaj.durumGelmedi'),
  IZINLI: t('puantaj.durumIzinli'),
  MAZERETLI: t('puantaj.durumMazeretli')
})[d] || d
const form = ref({ personelId: null, tarih: new Date(), durum: 'GELDI', aciklama: '' })
const ozet = ref({ toplam: 0, geldi: 0, gelmedi: 0, izinli: 0, mazeretli: 0 })
const detayDrawer = ref(false)
const detay = ref(null)

const ozetYukle = async () => {
  try {
    const r = await puantajAPI.ozet()
    ozet.value = { ...ozet.value, ...(r.data || {}) }
  } catch {
    /* özet alınamazsa KPI sıfır kalır */
  }
}

const filtreParams = () => {
  const p = { page: sayfa.value, size: sayfaBoyutu.value, sort: sirala.value }
  if (personelFiltre.value) p.personelId = personelFiltre.value
  if (durumFiltre.value !== 'TUMU') p.durum = durumFiltre.value
  if (arama.value.trim()) p.q = arama.value.trim()
  if (tarihAraligi.value?.[0]) p.baslangic = getLocalDateString(tarihAraligi.value[0])
  if (tarihAraligi.value?.[1]) p.bitis = getLocalDateString(tarihAraligi.value[1])
  return p
}

const personelleriYukle = async () => {
  try {
    const personelRes = await personelAPI.getAll({ size: 500 })
    personelList.value = unwrapList(personelRes).map((p) => ({
      ...p,
      displayName: p.ad && p.soyad ? `${p.ad} ${p.soyad}` : p.ad || String(p.id)
    }))
  } catch {
    toastBildirim.hata(t('puantaj.personelYuklenemedi'))
  }
}

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await puantajAPI.filtreli(filtreParams())
    list.value = unwrapList(r)
    toplam.value = r.data?.totalElements ?? list.value.length
  } catch {
    toastBildirim.hata(t('puantaj.veriYuklenemedi'))
  }
  yukleniyor.value = false
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
    const res = await excelAPI.puantaj(params)
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `Puantaj-${new Date().toISOString().slice(0, 10)}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    toastBildirim.basarili(t('puantaj.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('puantaj.excelHata'))
  } finally {
    excelYukleniyor.value = false
  }
}

import { formatTarih as formatDate, getLocalDateString } from '../utils/format.js'

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data
    ? {
        personelId: data.personelId,
        tarih: new Date(data.tarih),
        durum: data.durum || 'GELDI',
        aciklama: data.aciklama || ''
      }
    : { personelId: personelFiltre.value, tarih: new Date(), durum: 'GELDI', aciklama: '' }
  dialog.value = true
}

const puantajAksiyonlari = (k) => {
  const items = [{ etiket: t('puantaj.detay'), ikon: 'pi pi-search', islem: () => detayAc(k) }]
  items.push({ etiket: t('common.edit'), ikon: 'pi pi-pencil', islem: () => dialogAc(k) })
  if (authStore.isAdmin) {
    items.push({ etiket: t('common.delete'), ikon: 'pi pi-trash', sinif: 'eylem-sil', islem: () => sil(k) })
  }
  return items
}

const detayAc = (k) => {
  detay.value = k
  detayDrawer.value = true
}

const drawerDuzenle = (k) => {
  detayDrawer.value = false
  dialogAc(k)
}

const drawerSil = (k) => {
  detayDrawer.value = false
  sil(k)
}

const kaydet = async () => {
  if (!form.value.personelId) {
    toastBildirim.uyari(t('puantaj.personelSeciniz'))
    return
  }
  if (!form.value.tarih) {
    toastBildirim.uyari(t('puantaj.tarihZorunluUyari'))
    return
  }
  if (!form.value.durum) {
    toastBildirim.uyari(t('puantaj.durumZorunluUyari'))
    return
  }
  kaydediliyor.value = true
  try {
    const payload = { ...form.value, tarih: getLocalDateString(form.value.tarih) }
    if (duzenleme.value) {
      await puantajAPI.update(form.value.id, payload)
      toastBildirim.basarili(t('puantaj.guncellendi'))
    } else {
      await puantajAPI.create(payload)
      toastBildirim.basarili(t('puantaj.eklendi'))
    }
    dialog.value = false
    await yukle()
    ozetYukle()
  } catch {
    toastBildirim.hata(t('puantaj.islemBasarisiz'))
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  confirm.require({
    message: t('common.confirmDelete'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('common.evetSil'), severity: 'danger', size: 'small' },
    accept: async () => {
      try {
        await puantajAPI.delete(data.id)
        await yukle()
        ozetYukle()
        toast.add({ severity: 'success', summary: t('puantaj.silindi'), life: 3000 })
      } catch {
        toastBildirim.hata(t('puantaj.silmeBasarisiz'))
      }
    }
  })
}
</script>

<style scoped>
.puantaj-container {
  padding: 0;
  max-width: 100%;
}
.kpi-satiri {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.toolbar {
  margin-bottom: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 18px;
}
.puantaj-arac {
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
.puantaj-mobil {
  display: none;
}
.puantaj-skeleton {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.puantaj-mobil-kart {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
}
.pmk-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.pmk-ad {
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
.pmk-ad:hover span {
  color: var(--accent);
}
.pmk-ad i {
  color: var(--accent);
  flex-shrink: 0;
}
.pmk-ad span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pmk-satir {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12.5px;
  color: var(--text-secondary);
}
.pmk-satir i {
  font-size: 11.5px;
  color: var(--text-muted);
}
.pmk-alt {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--text-muted);
}
.pmk-aciklama {
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
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
  .puantaj-masa {
    display: none;
  }
  .puantaj-mobil {
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
  .pmk-ad span {
    transition: none;
  }
}
</style>
