<template>
  <div class="stokseri-container">
    <PageHeader
      :title="t('stokSeriler.title')"
      :subtitle="t('stokSeriler.aciklama')"
      icon="pi pi-tags"
    >
      <template #actions>
        <Button
          :label="t('stokSeriler.yeniSeriLot')"
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
        :baslik="t('stokSeriler.kpiToplam')"
        :deger="ozet.toplam"
        ikon="pi pi-tags"
        renk="#3b82f6"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('stokSeriler.kpiStokta')"
        :deger="ozet.stokta"
        ikon="pi pi-check-circle"
        renk="#10b981"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('stokSeriler.kpiTuketildi')"
        :deger="ozet.tuketildi"
        ikon="pi pi-box"
        renk="#64748b"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('stokSeriler.kpiSktYaklasan')"
        :deger="ozet.sktYaklasan"
        ikon="pi pi-exclamation-triangle"
        renk="#f59e0b"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('stokSeriler.kpiSktGecmis')"
        :deger="ozet.sktGecmis"
        ikon="pi pi-times-circle"
        renk="#ef4444"
        :para-birimi="false"
      />
    </div>

    <div
      v-if="sonKullanma.length"
      class="skt-uyari"
      role="status"
      aria-live="polite"
    >
      <i class="pi pi-exclamation-triangle" />
      <span>{{ t('stokSeriler.sktUyari', { n: sonKullanma.length }) }}</span>
      <div
        v-for="s in sonKullanma.slice(0, 5)"
        :key="s.id"
        class="skt-satir"
      >
        {{ s.stokAdi }} — {{ s.seriNo }} (SKT: {{ formatDate(s.sonKullanmaTarihi) }})
      </div>
    </div>

    <div class="seri-arac">
      <IconField class="filtre-arama">
        <InputIcon class="pi pi-search" />
        <InputText
          v-model="filtreQ"
          :placeholder="t('stokSeriler.aramaPlaceholder')"
          @input="filtreDegisti"
        />
      </IconField>
      <Dropdown
        v-model="filtreDepo"
        :options="depoListesi"
        option-label="ad"
        option-value="id"
        show-clear
        class="filtre-select"
        :placeholder="t('stokSeriler.depo')"
        @change="filtreDegisti"
      />
      <DatePicker
        v-model="filtreSktBaslangic"
        date-format="dd.mm.yy"
        show-icon
        :manual-input="false"
        class="filtre-tarih"
        :placeholder="t('stokSeriler.sktBaslangic')"
        @date-select="filtreDegisti"
      />
      <DatePicker
        v-model="filtreSktBitis"
        date-format="dd.mm.yy"
        show-icon
        :manual-input="false"
        class="filtre-tarih"
        :placeholder="t('stokSeriler.sktBitis')"
        @date-select="filtreDegisti"
      />
      <Button
        v-if="filtreVar"
        :label="t('stokSeriler.temizle')"
        icon="pi pi-filter-slash"
        class="p-button-text p-button-sm"
        @click="filtreTemizle"
      />
      <span class="arac-bosluk" />
      <Button
        :label="t('stokSeriler.excel')"
        icon="pi pi-file-excel"
        outlined
        size="small"
        :loading="excelYukleniyor"
        @click="excelIndir"
      />
    </div>

    <div
      class="seri-cipler"
      role="group"
      :aria-label="t('common.status')"
    >
      <button
        v-for="c in durumCipleri"
        :key="c.deger"
        type="button"
        class="seri-cip"
        :class="{ aktif: filtreDurum === c.deger }"
        :aria-pressed="filtreDurum === c.deger"
        @click="cipUygula(c.deger)"
      >
        <i :class="c.ikon" /> {{ c.ad }}
      </button>
    </div>

    <AppDataTable
      :value="list"
      lazy
      :total-records="toplam"
      :rows="sayfaBoyutu"
      class="seri-masa"
      striped-rows
      :loading="yukleniyor"
      :empty-message="filtreVar ? t('stokSeriler.filtreBos') : t('stokSeriler.veriYok')"
      @page="sayfaDegisti"
      @sort="siralaDegisti"
    >
      <Column
        field="stokAdi"
        :header="t('stokSeriler.urun')"
        sortable
      />
      <Column
        field="seriNo"
        :header="t('stokSeriler.seriNo')"
        sortable
      />
      <Column
        field="lotNo"
        :header="t('stokSeriler.lotNo')"
      />
      <Column
        field="miktar"
        :header="t('stokSeriler.miktar')"
      >
        <template #body="{ data }">
          {{ formatNumber(data.miktar) }}
        </template>
      </Column>
      <Column
        field="kalanMiktar"
        :header="t('stokSeriler.kalanMiktar')"
      >
        <template #body="{ data }">
          {{ formatNumber(data.kalanMiktar) }}
        </template>
      </Column>
      <Column
        :header="t('stokSeriler.depo')"
      >
        <template #body="{ data }">
          {{ depoAdi(data.depoId) }}
        </template>
      </Column>
      <Column
        field="sonKullanmaTarihi"
        :header="t('stokSeriler.skt')"
      >
        <template #body="{ data }">
          {{ formatDate(data.sonKullanmaTarihi) }}
        </template>
      </Column>
      <Column
        :header="t('stokSeriler.durum')"
      >
        <template #body="{ data }">
          <Tag
            :value="data.durum === 'TUKETILDI' ? t('stokSeriler.durumTuketildi') : t('stokSeriler.durumStokta')"
            :severity="data.durum === 'TUKETILDI' ? 'secondary' : 'success'"
          />
        </template>
      </Column>
      <Column
        :header="t('stokSeriler.islem')"
        style="width: 80px"
      >
        <template #body="{ data }">
          <SatirEylemleri
            :gorunur="{ duzenle: false, cogalt: false, sil: false }"
            :items="seriAksiyonlari(data)"
          />
        </template>
      </Column>
    </AppDataTable>

    <!-- Mobil kart görünümü -->
    <div class="seri-mobil">
      <div
        v-if="yukleniyor && !list.length"
        class="seri-skeleton"
        role="status"
        :aria-label="t('common.loading')"
      >
        <SkeletonLoader
          :count="4"
          height="72px"
        />
      </div>
      <EmptyState
        v-else-if="!list.length"
        :message="filtreVar ? t('stokSeriler.filtreBos') : t('stokSeriler.veriYok')"
        icon="pi pi-tags"
      />
      <div
        v-for="s in list"
        :key="s.id"
        class="seri-mobil-kart"
        @click="detayAc(s)"
      >
        <div class="smk-ust">
          <strong>{{ s.stokAdi }}</strong>
          <SatirEylemleri
            :gorunur="{ duzenle: false, cogalt: false, sil: false }"
            :items="seriAksiyonlari(s)"
            @click.stop
          />
        </div>
        <div class="smk-taglar">
          <Tag
            :value="s.durum === 'TUKETILDI' ? t('stokSeriler.durumTuketildi') : t('stokSeriler.durumStokta')"
            :severity="s.durum === 'TUKETILDI' ? 'secondary' : 'success'"
          />
          <span class="smk-alt">SN: {{ s.seriNo }}</span>
          <span
            v-if="s.lotNo"
            class="smk-alt"
          >Lot: {{ s.lotNo }}</span>
        </div>
        <div class="smk-satir">
          <span>{{ t('stokSeriler.miktar') }}: <strong>{{ formatNumber(s.miktar) }}</strong></span>
          <span>{{ t('stokSeriler.kalanMiktar') }}: <strong>{{ formatNumber(s.kalanMiktar) }}</strong></span>
        </div>
        <div class="smk-satir">
          <span>{{ t('stokSeriler.skt') }}: {{ formatDate(s.sonKullanmaTarihi) }}</span>
          <span>{{ depoAdi(s.depoId) }}</span>
        </div>
      </div>
    </div>

    <Dialog
      v-model:visible="dialog"
      modal
      :style="{ width: '520px', maxWidth: '96vw' }"
    >
      <template #header>
        <div class="dialog-baslik">
          <i class="pi pi-tags" />
          <span>{{ dialogHeader }}</span>
        </div>
      </template>
      <div class="form-grid">
        <div class="field">
          <label>{{ t('stokSeriler.urunZorunlu') }}</label>
          <Dropdown
            v-model="form.stokId"
            :options="stokOnerileri"
            option-label="ad"
            option-value="id"
            :placeholder="t('stokSeriler.urunSec')"
            class="w-full"
            filter
            filter-by="ad,stokKodu,barkod"
            :disabled="duzenleme"
            @filter="stokAra"
          />
        </div>
        <div class="field">
          <label>{{ t('stokSeriler.seriNoZorunlu') }}</label><InputText
            v-model="form.seriNo"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('stokSeriler.lotNo') }}</label><InputText
            v-model="form.lotNo"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('stokSeriler.sonKullanmaTarihi') }}</label><DatePicker
            v-model="form.sonKullanmaTarihi"
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('stokSeriler.depo') }}</label>
          <Dropdown
            v-model="form.depoId"
            :options="depoListesi"
            option-label="ad"
            option-value="id"
            :placeholder="t('stokSeriler.depoSec')"
            class="w-full"
            :show-clear="true"
          />
        </div>
        <div class="field">
          <label>{{ t('stokSeriler.miktar') }}</label><InputNumber
            v-model="form.miktar"
            :min="0"
            :min-fraction-digits="0"
            :max-fraction-digits="3"
            :disabled="duzenleme"
            class="w-full"
          />
        </div>
        <p
          v-if="duzenleme"
          class="muted"
        >
          {{ t('stokSeriler.guvenliDuzenlemeNotu') }}
        </p>
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

    <StokSeriDetayDrawer
      v-model:visible="detayDrawer"
      :seri="seciliSeri"
      :depo-adi="seciliSeri ? depoAdi(seciliSeri.depoId) : ''"
      @duzenle="drawerDuzenle"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { stokSeriAPI, depoAPI, excelAPI } from '../api/index.js'
import { useStokOnerileri } from '../composables/useStokOnerileri.js'
import { useI18n } from 'vue-i18n'
import { formatTarih as formatDate, getLocalDateString } from '../utils/format.js'
import KpiKart from '../components/KpiKart.vue'
import SatirEylemleri from '../components/SatirEylemleri.vue'
import StokSeriDetayDrawer from '../components/StokSeriDetayDrawer.vue'
import SkeletonLoader from '../components/SkeletonLoader.vue'
import EmptyState from '../components/EmptyState.vue'

const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()
const list = ref([])
const { oneriler: stokOnerileri, ara: stokAra, hemenAra: stokOnerileriYukle } = useStokOnerileri()
const depoListesi = ref([])
const sonKullanma = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const detayDrawer = ref(false)
const seciliSeri = ref(null)
const form = ref({ stokId: null, seriNo: '', lotNo: '', sonKullanmaTarihi: null, depoId: null, miktar: 1 })

// Sunucu taraflı liste ve filtreler.
const toplam = ref(0)
const sayfa = ref(0)
const SAYFA_BOYUTU_ANAHTAR = 'raspel_stok_seri_sayfa_boyutu'
const sayfaBoyutu = ref(Number(localStorage.getItem(SAYFA_BOYUTU_ANAHTAR)) || 25)
const sirala = ref('olusturmaTarihi,desc')
const filtreQ = ref('')
const filtreDurum = ref('TUMU')
const filtreDepo = ref(null)
const filtreSktBaslangic = ref(null)
const filtreSktBitis = ref(null)
const excelYukleniyor = ref(false)
let aramaZaman = null

const ozet = ref({ toplam: 0, stokta: 0, tuketildi: 0, sktYaklasan: 0, sktGecmis: 0 })
const ozetYukle = async () => {
  try {
    const r = await stokSeriAPI.ozet(30)
    ozet.value = { ...ozet.value, ...(r.data || {}) }
  } catch {
    /* özet alınamazsa KPI sıfır kalır */
  }
}

const dialogHeader = computed(() => (duzenleme.value ? t('stokSeriler.seriLotDuzenle') : t('stokSeriler.yeniSeriLot')))

const formatNumber = (v) => {
  if (v === null || v === undefined) return '0'
  return Number(v).toLocaleString('tr-TR', { maximumFractionDigits: 4 })
}

function depoAdi(id) {
  return depoListesi.value.find((d) => d.id === id)?.ad || '-'
}

const sktYukle = async () => {
  try {
    const sk = await stokSeriAPI.sonKullanma(30)
    sonKullanma.value = (sk.data || []).filter((x) => x.durum !== 'TUKETILDI')
  } catch {
    sonKullanma.value = []
  }
}

const tarihIso = (d) => {
  if (!d) return null
  const date = d instanceof Date ? d : new Date(d)
  if (Number.isNaN(date.getTime())) return null
  const ay = String(date.getMonth() + 1).padStart(2, '0')
  const gun = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${ay}-${gun}`
}

// Liste ve Excel'in paylaştığı filtre parametreleri.
const filtreParams = () => {
  const p = { page: sayfa.value, size: sayfaBoyutu.value, sort: sirala.value }
  if (filtreQ.value.trim()) p.q = filtreQ.value.trim()
  if (filtreDurum.value && filtreDurum.value !== 'TUMU') p.durum = filtreDurum.value
  if (filtreDepo.value) p.depoId = filtreDepo.value
  if (filtreSktBaslangic.value) p.sktBaslangic = tarihIso(filtreSktBaslangic.value)
  if (filtreSktBitis.value) p.sktBitis = tarihIso(filtreSktBitis.value)
  return p
}

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await stokSeriAPI.filtreli(filtreParams())
    list.value = r.data?.content || []
    toplam.value = r.data?.totalElements ?? list.value.length
    await sktYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('stokSeriler.hataYukleme'))
  } finally {
    yukleniyor.value = false
  }
  ozetYukle()
}

const durumCipleri = computed(() => [
  { deger: 'TUMU', ad: t('stokSeriler.cipTumu'), ikon: 'pi pi-list' },
  { deger: 'STOKTA', ad: t('stokSeriler.durumStokta'), ikon: 'pi pi-check-circle' },
  { deger: 'TUKETILDI', ad: t('stokSeriler.durumTuketildi'), ikon: 'pi pi-box' }
])

const filtreVar = computed(
  () =>
    !!filtreQ.value.trim() ||
    (filtreDurum.value && filtreDurum.value !== 'TUMU') ||
    !!filtreDepo.value ||
    !!filtreSktBaslangic.value ||
    !!filtreSktBitis.value
)

const filtreDegisti = () => {
  if (aramaZaman) clearTimeout(aramaZaman)
  aramaZaman = setTimeout(() => {
    sayfa.value = 0
    yukle()
  }, 300)
}

const cipUygula = (deger) => {
  filtreDurum.value = deger
  sayfa.value = 0
  yukle()
}

const filtreTemizle = () => {
  filtreQ.value = ''
  filtreDurum.value = 'TUMU'
  filtreDepo.value = null
  filtreSktBaslangic.value = null
  filtreSktBitis.value = null
  sayfa.value = 0
  yukle()
}

const sayfaDegisti = (e) => {
  sayfa.value = e.page
  sayfaBoyutu.value = e.rows
  localStorage.setItem(SAYFA_BOYUTU_ANAHTAR, String(e.rows))
  yukle()
}

const siralaDegisti = (e) => {
  const yon = e.sortOrder === 1 ? 'asc' : 'desc'
  sirala.value = `${e.sortField},${yon}`
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
    const res = await excelAPI.stokSeri(params)
    const url = window.URL.createObjectURL(
      new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    )
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `stok-seri-lot-${new Date().toISOString().slice(0, 10)}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    toastBildirim.basarili(t('stokSeriler.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('stokSeriler.excelHata'))
  } finally {
    excelYukleniyor.value = false
  }
}

onMounted(async () => {
  // Ürün seçici sunucu aramalı; tüm stokları çekmez.
  stokOnerileriYukle()
  try {
    const dR = await depoAPI.getAll()
    depoListesi.value = dR.data.content || dR.data || []
  } catch {
    depoListesi.value = []
  }
  await yukle()
})

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data
    ? { ...data, sonKullanmaTarihi: data.sonKullanmaTarihi ? new Date(data.sonKullanmaTarihi) : null }
    : { stokId: null, seriNo: '', lotNo: '', sonKullanmaTarihi: null, depoId: null, miktar: 1 }
  dialog.value = true
}

const seriAksiyonlari = (d) => {
  const items = [{ etiket: t('stokSeriler.detay'), ikon: 'pi pi-search', islem: () => detayAc(d) }]
  if (d.durum === 'STOKTA') {
    items.push({ etiket: t('common.edit'), ikon: 'pi pi-pencil', islem: () => dialogAc(d) })
  }
  if (d.durum !== 'TUKETILDI') {
    items.push({ etiket: t('common.delete'), ikon: 'pi pi-trash', sinif: 'eylem-sil', islem: () => sil(d) })
  }
  return items
}

const detayAc = (d) => {
  seciliSeri.value = d
  detayDrawer.value = true
}

const drawerDuzenle = (d) => {
  detayDrawer.value = false
  dialogAc(d)
}

const kaydet = async () => {
  if (!form.value.stokId) {
    toastBildirim.uyari(t('stokSeriler.urunZorunlu'))
    return
  }
  if (!form.value.seriNo?.trim()) {
    toastBildirim.uyari(t('stokSeriler.seriNoZorunlu'))
    return
  }
  if (!duzenleme.value && Number(form.value.miktar) <= 0) {
    toastBildirim.uyari(t('stokSeriler.miktarPozitifOlmali'))
    return
  }
  kaydediliyor.value = true
  try {
    const s = form.value.sonKullanmaTarihi
    const payload = {
      ...form.value,
      seriNo: form.value.seriNo.trim(),
      sonKullanmaTarihi: s ? getLocalDateString(s) : null,
      miktar: form.value.miktar ?? 1
    }
    if (duzenleme.value) {
      await stokSeriAPI.update(form.value.id, payload)
      toastBildirim.basarili(t('stokSeriler.guncellendi'))
    } else {
      await stokSeriAPI.create(payload)
      toastBildirim.basarili(t('stokSeriler.olusturuldu'))
    }
    dialog.value = false
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('stokSeriler.islemBasarisiz'))
  }
  kaydediliyor.value = false
}

const sil = (data) => {
  confirm.require({
    message: t('stokSeriler.silOnayMesaj', { seriNo: data.seriNo }),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await stokSeriAPI.delete(data.id)
        await yukle()
        toastBildirim.basarili(t('stokSeriler.silindi'))
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('stokSeriler.silmeBasarisiz'))
      }
    }
  })
}
</script>

<style scoped>
.dialog-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  font-size: 15px;
}
.dialog-baslik i {
  color: var(--accent);
}
.stokseri-container {
  padding: 0;
}
.kpi-satiri {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.seri-arac {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.filtre-arama {
  min-width: 220px;
  flex: 1 1 220px;
  max-width: 320px;
}
.filtre-select {
  min-width: 170px;
}
.filtre-tarih {
  min-width: 150px;
}
.arac-bosluk {
  flex: 1 1 auto;
}
.seri-cipler {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.seri-cip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--dur-base, 0.2s) var(--ease-standard, ease);
}
.seri-cip:hover {
  border-color: var(--accent-border);
  color: var(--text-primary);
}
.seri-cip.aktif {
  background: var(--accent-soft, rgba(245, 158, 11, 0.16));
  border-color: var(--accent-border, rgba(245, 158, 11, 0.5));
  color: var(--accent);
}
.seri-cip i {
  font-size: 12px;
}
.seri-mobil {
  display: none;
}
.seri-skeleton {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.seri-mobil-kart {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-card);
  cursor: pointer;
}
.smk-ust {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.smk-ust strong {
  font-size: 13.5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.smk-taglar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.smk-alt {
  font-size: 12px;
  color: var(--text-muted);
}
.smk-satir {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 12.5px;
  color: var(--text-secondary);
}

@media (max-width: 768px) {
  .seri-masa {
    display: none;
  }
  .seri-mobil {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .seri-arac .filtre-arama,
  .seri-arac .filtre-select,
  .seri-arac .filtre-tarih {
    min-width: 0;
    max-width: none;
    flex: 1 1 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .seri-cip {
    transition: none;
  }
}
.sayfa-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
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
.muted {
  color: var(--text-muted);
  font-size: 12px;
}
.skt-uyari {
  background: rgba(245, 158, 11, 0.12);
  border: 1px solid rgba(245, 158, 11, 0.35);
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 16px;
  color: var(--text-primary);
  font-size: 13px;
}
.skt-uyari > i {
  color: #f59e0b;
  margin-right: 6px;
}
.skt-satir {
  padding: 2px 0 2px 22px;
  font-size: 12px;
  color: var(--text-secondary);
}
.durum-badge {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
}
.durum-badge.stokta {
  background: rgba(34, 197, 94, 0.15);
  color: #16a34a;
}
.durum-badge.tuketildi {
  background: rgba(148, 163, 184, 0.2);
  color: var(--text-secondary);
}
</style>
