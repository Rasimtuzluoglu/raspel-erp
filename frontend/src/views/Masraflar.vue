<template>
  <div class="masraf-container">
    <PageHeader
      :title="t('masraflar.title')"
      :subtitle="t('masraflar.aciklama')"
      icon="pi pi-receipt"
    >
      <template #actions>
        <Button
          v-if="canWrite"
          :label="t('masraflar.yeniMasraf')"
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
        :baslik="t('masraflar.kpiBuAy')"
        :deger="ozet.buAyToplam"
        ikon="pi pi-calendar"
        renk="#f59e0b"
      />
      <KpiKart
        :baslik="t('masraflar.kpiBuYil')"
        :deger="ozet.buYilToplam"
        ikon="pi pi-chart-line"
        renk="#3b82f6"
      />
      <KpiKart
        :baslik="t('masraflar.kpiKdv')"
        :deger="ozet.buYilKdv"
        ikon="pi pi-percentage"
        renk="#8b5cf6"
      />
      <KpiKart
        :baslik="t('masraflar.kpiKayitSayisi')"
        :deger="ozet.toplamKayit"
        ikon="pi pi-list"
        renk="#10b981"
        :para-birimi="false"
      />
    </div>

    <div class="masraf-arac">
      <span class="arac-arama">
        <i class="pi pi-search" />
        <InputText
          v-model="arama"
          :placeholder="t('masraflar.aramaPlaceholder')"
          class="arama-input"
          @input="aramaGirdi"
        />
      </span>
      <Dropdown
        v-model="kategoriFiltre"
        :options="kategoriSecenekleri"
        option-label="label"
        option-value="value"
        :placeholder="t('masraflar.kategori')"
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
        :label="t('masraflar.excel')"
        icon="pi pi-file-excel"
        outlined
        size="small"
        :loading="excelYukleniyor"
        @click="excelIndir"
      />
    </div>

    <div
      class="masraf-masa"
      :aria-busy="yukleniyor"
    >
      <AppDataTable
        :value="list"
        lazy
        :total-records="toplam"
        :rows="sayfaBoyutu"
        striped-rows
        :loading="yukleniyor"
        :empty-message="filtreVar ? t('masraflar.filtreBos') : t('masraflar.empty')"
        gorunum-anahtari="masraflar"
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
          field="kategori"
          :header="t('masraflar.kategori')"
          sortable
        >
          <template #body="{ data }">
            {{ kategoriEtiket(data.kategori) }}
          </template>
        </Column>
        <Column
          field="aciklama"
          :header="t('common.description')"
        />
        <Column
          field="tutar"
          :header="t('common.amount')"
        >
          <template #body="{ data }">
            <span class="gizli-veri">{{ formatCurrency(data.tutar) }}</span>
          </template>
        </Column>
        <Column
          field="belgeNo"
          :header="t('masraflar.belgeNo')"
        />
        <Column
          :header="t('common.actions')"
          style="width: 72px"
        >
          <template #body="{ data }">
            <SatirEylemleri
              :gorunur="{ duzenle: false, cogalt: false, sil: false }"
              :items="masrafAksiyonlari(data)"
              :baslik="data.aciklama || data.kategori"
            />
          </template>
        </Column>
        <template #empty>
          <EmptyState
            v-if="!yukleniyor && list.length === 0"
            :message="filtreVar ? t('masraflar.filtreBos') : t('masraflar.empty')"
            :sub-message="filtreVar ? '' : t('masraflar.emptyHint')"
            icon="pi pi-receipt"
            :action-label="filtreVar || !canWrite ? '' : t('masraflar.yeniMasraf')"
            action-icon="pi pi-plus"
            @action="dialogAc()"
          />
        </template>
      </AppDataTable>
    </div>

    <!-- Mobil kart görünümü -->
    <div
      class="masraf-mobil"
      :aria-busy="yukleniyor"
    >
      <span
        class="sr-only"
        role="status"
        aria-live="polite"
      >{{ t('masraflar.sonucSayisi', { sayi: toplam }) }}</span>
      <div
        v-if="yukleniyor && !list.length"
        class="masraf-skeleton"
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
        :message="filtreVar ? t('masraflar.filtreBos') : t('masraflar.empty')"
        icon="pi pi-receipt"
      />
      <article
        v-for="m in list"
        :key="m.id"
        class="masraf-mobil-kart"
      >
        <div class="mmk-ust">
          <button
            type="button"
            class="mmk-ad"
            :title="t('masraflar.detay')"
            @click="detayAc(m)"
          >
            <i class="pi pi-receipt" />
            <span>{{ kategoriEtiket(m.kategori) }}</span>
          </button>
          <SatirEylemleri
            :gorunur="{ duzenle: false, cogalt: false, sil: false }"
            :items="masrafAksiyonlari(m)"
            :baslik="m.aciklama || m.kategori"
          />
        </div>
        <div class="mmk-satir">
          <strong class="mmk-tutar">{{ formatCurrency(m.tutar) }}</strong>
          <span class="mmk-alt"><i class="pi pi-calendar" /> {{ formatDate(m.tarih) }}</span>
        </div>
        <p
          v-if="m.aciklama"
          class="mmk-aciklama"
        >
          {{ m.aciklama }}
        </p>
        <div
          v-if="m.belgeNo"
          class="mmk-satir mmk-alt"
        >
          <span><i class="pi pi-file" /> {{ m.belgeNo }}</span>
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
          <label for="masraf-tarih">{{ t('masraflar.tarihZorunlu') }}</label><DatePicker
            id="masraf-tarih"
            v-model="form.tarih"
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label for="masraf-kategori">{{ t('masraflar.kategoriZorunlu') }}</label>
          <Dropdown
            id="masraf-kategori"
            v-model="form.kategori"
            :options="kategoriFormSecenekleri"
            option-label="label"
            option-value="value"
            :placeholder="t('masraflar.kategoriSecin')"
            class="w-full"
          />
        </div>
        <div class="field">
          <label for="masraf-cari">{{ t('masraflar.cariHesap') }}</label>
          <Dropdown
            id="masraf-cari"
            v-model="form.cariHesapId"
            :options="cariHesaplar"
            option-label="ad"
            option-value="id"
            :placeholder="t('masraflar.opsiyonel')"
            show-clear
            filter
            class="w-full"
          />
        </div>
        <div class="field">
          <label for="masraf-aciklama">{{ t('common.description') }}</label><Textarea
            id="masraf-aciklama"
            v-model="form.aciklama"
            rows="2"
            class="w-full"
            maxlength="500"
          />
        </div>
        <div class="field">
          <label for="masraf-tutar">{{ t('masraflar.tutar') }}</label><InputNumber
            id="masraf-tutar"
            v-model="form.tutar"
            mode="currency"
            currency="TRY"
            :min="0"
            class="w-full"
          />
        </div>
        <div class="field">
          <label for="masraf-kdv">{{ t('masraflar.kdvOrani') }}</label><InputNumber
            id="masraf-kdv"
            v-model="form.kdvOrani"
            :min="0"
            :max="100"
            :suffix="' %'"
            class="w-full"
          />
        </div>
        <div class="field">
          <label for="masraf-belge">{{ t('masraflar.belgeNo') }}</label><InputText
            id="masraf-belge"
            v-model="form.belgeNo"
            class="w-full"
            maxlength="50"
          />
        </div>
        <div class="field">
          <label for="masraf-odeme-yontemi">{{ t('masraflar.odemeYontemi') }}</label>
          <Dropdown
            id="masraf-odeme-yontemi"
            v-model="form.odemeYontemi"
            :options="odemeYontemleri"
            option-label="label"
            option-value="value"
            :placeholder="t('masraflar.odemeYok')"
            show-clear
            class="w-full"
          />
        </div>
        <div class="field">
          <label for="masraf-kasa">{{ t('masraflar.odemeKasa') }}</label><Dropdown
            id="masraf-kasa"
            v-model="form.kasaId"
            :options="kasalar"
            option-label="ad"
            option-value="id"
            :placeholder="t('masraflar.opsiyonel')"
            show-clear
            class="w-full"
          />
        </div>
        <div class="field">
          <label for="masraf-banka">{{ t('masraflar.odemeBanka') }}</label><Dropdown
            id="masraf-banka"
            v-model="form.bankaId"
            :options="bankalar"
            option-label="ad"
            option-value="id"
            :placeholder="t('masraflar.opsiyonel')"
            show-clear
            class="w-full"
          />
        </div>
        <div
          v-if="form.tutar > 0"
          class="field full-width kdv-ozet"
        >
          <span>{{ t('masraflar.matrah') }}: <strong>{{ formatCurrency(matrah) }}</strong></span>
          <span>{{ t('masraflar.kdvTutar') }}: <strong>{{ formatCurrency(kdvTutar) }}</strong></span>
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

    <MasrafDetayDrawer
      v-model:visible="detayDrawer"
      :masraf="detay"
      :cari-adi="detay ? cariAdi(detay.cariHesapId) : ''"
      :hesap-adi="detay ? hesapAdi(detay) : ''"
      :duzenlenebilir="canWrite"
      :silinebilir="isAdmin"
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
import { useAuthStore } from '../stores/authStore.js'
import { masrafAPI, kasaAPI, bankaAPI, cariHesapAPI, excelAPI } from '../api/index.js'
import EmptyState from '../components/EmptyState.vue'
import PageHeader from '../components/PageHeader.vue'
import KpiKart from '../components/KpiKart.vue'
import SatirEylemleri from '../components/SatirEylemleri.vue'
import MasrafDetayDrawer from '../components/MasrafDetayDrawer.vue'
import SkeletonLoader from '../components/SkeletonLoader.vue'
import { formatCurrency } from '../utils/format.js'
import { useI18n } from 'vue-i18n'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const authStore = useAuthStore()
const { t } = useI18n()

const SAYFA_ANAHTAR = 'raspel_masraflar_rows'
const KATEGORILER = ['YAKIT', 'YEMEK', 'KONAKLAMA', 'ULASIM', 'MALZEME', 'DIGER']

const list = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const excelYukleniyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const kasalar = ref([])
const bankalar = ref([])
const cariHesaplar = ref([])
const ozet = ref({ buAyToplam: 0, buYilToplam: 0, buYilKdv: 0, toplamKayit: 0 })
const detayDrawer = ref(false)
const detay = ref(null)

const arama = ref('')
const kategoriFiltre = ref('TUMU')
const tarihAraligi = ref([new Date(new Date().getFullYear(), new Date().getMonth(), 1), new Date()])
const toplam = ref(0)
const sayfa = ref(0)
const sayfaBoyutu = ref(Number(localStorage.getItem(SAYFA_ANAHTAR)) || 25)
const sirala = ref('tarih,desc')
let aramaZamanlayici = null

const canWrite = computed(() => ['ADMIN', 'MUHASEBE'].includes(authStore?.kullanici?.role))
const isAdmin = computed(() => authStore?.kullanici?.role === 'ADMIN')

const KATEGORI_ANAHTARLARI = {
  YAKIT: 'kategoriYakit',
  YEMEK: 'kategoriYemek',
  KONAKLAMA: 'kategoriKonaklama',
  ULASIM: 'kategoriUlasim',
  MALZEME: 'kategoriMalzeme',
  DIGER: 'kategoriDiger'
}

const kategoriEtiket = (kod) => {
  if (!kod) return '-'
  const anahtar = KATEGORI_ANAHTARLARI[kod]
  return anahtar ? t(`masraflar.${anahtar}`) : kod
}

const kategoriSecenekleri = computed(() => [
  { label: t('masraflar.kategoriTumu'), value: 'TUMU' },
  ...KATEGORILER.map((k) => ({ label: kategoriEtiket(k), value: k }))
])
const kategoriFormSecenekleri = computed(() =>
  KATEGORILER.map((k) => ({ label: kategoriEtiket(k), value: k }))
)
const odemeYontemleri = computed(() => [
  { label: t('masraflar.odemeNakit'), value: 'NAKIT' },
  { label: t('masraflar.odemeHavale'), value: 'HAVALE' },
  { label: t('masraflar.odemeKart'), value: 'KART' }
])
const cariAdi = (id) => {
  if (!id) return ''
  return cariHesaplar.value.find((c) => c.id === id)?.ad || ''
}
const hesapAdi = (m) => {
  if (!m) return ''
  if (m.kasaId) return kasalar.value.find((k) => k.id === m.kasaId)?.ad || ''
  if (m.bankaId) return bankalar.value.find((b) => b.id === m.bankaId)?.ad || ''
  return ''
}

const filtreVar = computed(
  () =>
    !!arama.value.trim() ||
    !!tarihAraligi.value?.[0] ||
    kategoriFiltre.value !== 'TUMU'
)

const bosForm = () => ({
  tarih: new Date(), kategori: '', cariHesapId: null, aciklama: '', tutar: 0, belgeNo: '',
  kdvOrani: 20, odemeYontemi: null, kasaId: null, bankaId: null
})
const form = ref(bosForm())

// Tutar KDV DAHİL kabul edilir; matrah ve KDV kullanıcıya gösterilir.
const kdvTutar = computed(() => {
  const tutar = Number(form.value.tutar || 0)
  const oran = Number(form.value.kdvOrani || 0)
  if (!tutar || !oran) return 0
  const net = tutar / (1 + oran / 100)
  return Math.round((tutar - net) * 100) / 100
})
const matrah = computed(() => Math.round((Number(form.value.tutar || 0) - kdvTutar.value) * 100) / 100)

const dialogHeader = computed(() => (duzenleme.value ? t('masraflar.duzenle') : t('masraflar.yeniMasraf')))

import { formatTarih as formatDate, getLocalDateString } from '../utils/format.js'

const ozetYukle = async () => {
  try {
    const r = await masrafAPI.ozet()
    ozet.value = { ...ozet.value, ...(r.data || {}) }
  } catch {
    /* özet alınamazsa KPI sıfır kalır */
  }
}

const filtreParams = () => {
  const p = { page: sayfa.value, size: sayfaBoyutu.value, sort: sirala.value }
  if (kategoriFiltre.value !== 'TUMU') p.kategori = kategoriFiltre.value
  if (arama.value.trim()) p.q = arama.value.trim()
  if (tarihAraligi.value?.[0]) p.baslangic = getLocalDateString(tarihAraligi.value[0])
  if (tarihAraligi.value?.[1]) p.bitis = getLocalDateString(tarihAraligi.value[1])
  return p
}

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await masrafAPI.filtreli(filtreParams())
    list.value = unwrapList(r)
    toplam.value = r.data?.totalElements ?? list.value.length
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('masraflar.hataYukleme'))
  }
  // Kasa/banka/cari listeleri hata verse bile masraf listesi gösterilir.
  try {
    const [kasaRes, bankaRes, cariRes] = await Promise.all([
      kasaAPI.getAllKasalar(),
      bankaAPI.getAll(),
      cariHesapAPI.getAll({ size: 500 })
    ])
    kasalar.value = unwrapList(kasaRes)
    bankalar.value = unwrapList(bankaRes)
    cariHesaplar.value = unwrapList(cariRes)
  } catch {
    kasalar.value = []
    bankalar.value = []
    cariHesaplar.value = []
  }
  yukleniyor.value = false
  ozetYukle()
}

onMounted(yukle)

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
    const res = await excelAPI.masraflar(params)
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `Masraflar-${new Date().toISOString().slice(0, 10)}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    toastBildirim.basarili(t('masraflar.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('masraflar.excelHata'))
  } finally {
    excelYukleniyor.value = false
  }
}

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data
    ? { ...data, tarih: data.tarih ? new Date(data.tarih) : new Date() }
    : bosForm()
  dialog.value = true
}

const kaydet = async () => {
  // Cift gonderim engeli: ayni masraf iki kez kaydedilmesin.
  if (kaydediliyor.value) return
  if (!form.value.kategori) {
    toastBildirim.uyari(t('masraflar.kategoriZorunluUyari'))
    return
  }
  if (!form.value.tutar || Number(form.value.tutar) <= 0) {
    toastBildirim.uyari(t('masraflar.tutarPozitifUyari'))
    return
  }
  if (form.value.kasaId && form.value.bankaId) {
    toastBildirim.uyari(t('masraflar.tekHesap'))
    return
  }
  kaydediliyor.value = true
  try {
    const payload = {
      ...form.value,
      tarih: form.value.tarih ? getLocalDateString(form.value.tarih) : null
    }
    if (duzenleme.value) {
      await masrafAPI.update(form.value.id, payload)
      toastBildirim.basarili(t('masraflar.guncellendi'))
    } else {
      await masrafAPI.create(payload)
      toastBildirim.basarili(t('masraflar.olusturuldu'))
    }
    dialog.value = false
    await yukle()
    ozetYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('masraflar.islemBasarisiz'))
  }
  kaydediliyor.value = false
}

const masrafAksiyonlari = (m) => {
  const items = [{ etiket: t('masraflar.detay'), ikon: 'pi pi-search', islem: () => detayAc(m) }]
  if (canWrite.value) {
    items.push({ etiket: t('common.edit'), ikon: 'pi pi-pencil', islem: () => dialogAc(m) })
  }
  if (isAdmin.value) {
    items.push({ etiket: t('common.delete'), ikon: 'pi pi-trash', sinif: 'eylem-sil', islem: () => sil(m) })
  }
  return items
}

const detayAc = (m) => {
  detay.value = m
  detayDrawer.value = true
}

const drawerDuzenle = (m) => {
  detayDrawer.value = false
  dialogAc(m)
}

const drawerSil = (m) => {
  detayDrawer.value = false
  sil(m)
}

const sil = (data) => {
  confirm.require({
    message: t('masraflar.silOnayMesaj', { n: data.kategori || data.id }),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await masrafAPI.delete(data.id)
        list.value = list.value.filter((x) => x.id !== data.id)
        ozetYukle()
        toast.add({ severity: 'success', summary: t('masraflar.silindi'), detail: t('masraflar.masrafSilindi'), life: 3000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('masraflar.silmeBasarisiz'))
      }
    }
  })
}
</script>

<style scoped>
.masraf-container {
  padding: 0;
}
.kpi-satiri {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.masraf-arac {
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
  min-width: 180px;
}
.arac-tarih {
  min-width: 220px;
}
.masraf-mobil {
  display: none;
}
.masraf-skeleton {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.masraf-mobil-kart {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
}
.mmk-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.mmk-ad {
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
.mmk-ad:hover span {
  color: var(--accent);
}
.mmk-ad i {
  color: var(--accent);
  flex-shrink: 0;
}
.mmk-ad span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mmk-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 12.5px;
  color: var(--text-secondary);
}
.mmk-satir i {
  font-size: 11.5px;
  color: var(--text-muted);
}
.mmk-tutar {
  font-size: 1.05rem;
  color: var(--accent);
}
.mmk-alt {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--text-muted);
}
.mmk-aciklama {
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
.kdv-ozet {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: var(--text-secondary);
  background: var(--surface-100, rgba(148, 163, 184, 0.08));
  border-radius: 8px;
  padding: 8px 12px;
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
  .masraf-masa {
    display: none;
  }
  .masraf-mobil {
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
  .mmk-ad span {
    transition: none;
  }
}
</style>
