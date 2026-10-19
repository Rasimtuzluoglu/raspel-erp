<template>
  <div class="stoksayim-container">
    <PageHeader
      :title="t('stokSayim.title')"
      :subtitle="t('stokSayim.aciklama')"
      icon="pi pi-clipboard"
    >
      <template #actions>
        <div class="tarama-adet">
          <label>{{ t('stokSayim.taramaAdet') }}</label>
          <InputNumber
            v-model="taramaAdet"
            :min="1"
            class="adet-input"
          />
        </div>
        <Button
          :label="t('stokSayim.hizliSayim')"
          icon="pi pi-camera"
          class="p-button-outlined"
          @click="taramaModu = true"
        />
        <Button
          :label="t('stokSayim.yeniSayim')"
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
        :baslik="t('stokSayim.kpiToplam')"
        :deger="ozet.toplam"
        ikon="pi pi-clipboard"
        renk="#3b82f6"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('stokSayim.kpiTaslak')"
        :deger="ozet.taslak"
        ikon="pi pi-file-edit"
        renk="#64748b"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('stokSayim.kpiTamamlanan')"
        :deger="ozet.tamamlanan"
        ikon="pi pi-check-circle"
        renk="#10b981"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('stokSayim.kpiFarkli')"
        :deger="ozet.farkli"
        ikon="pi pi-exclamation-triangle"
        renk="#ef4444"
        :para-birimi="false"
      />
      <KpiKart
        :baslik="t('stokSayim.kpiBugun')"
        :deger="ozet.bugun"
        ikon="pi pi-calendar"
        renk="#8b5cf6"
        :para-birimi="false"
      />
    </div>

    <div
      v-if="sonTaramalar.length"
      class="tarama-paneli"
      role="status"
      aria-live="polite"
    >
      <div class="tarama-paneli-baslik">
        <i class="pi pi-check-circle" />
        <span>{{ t('stokSayim.tarananEklendi', { stok: sonTaramalar[0].ad, adet: sonTaramalar[0].adet, toplam: sonTaramalar[0].toplam }) }}</span>
      </div>
      <div class="tarama-chips">
        <Tag
          v-for="(tarama, i) in sonTaramalar"
          :key="i"
          :value="`${tarama.ad}: ${tarama.toplam}`"
          severity="success"
          class="tarama-chip"
        />
      </div>
    </div>

    <div class="sayim-arac">
      <IconField class="filtre-arama">
        <InputIcon class="pi pi-search" />
        <InputText
          v-model="filtreQ"
          :placeholder="t('stokSayim.aramaPlaceholder')"
          @input="filtreDegisti"
        />
      </IconField>
      <DatePicker
        v-model="filtreBaslangic"
        date-format="dd.mm.yy"
        show-icon
        :manual-input="false"
        class="filtre-tarih"
        :placeholder="t('stokSayim.baslangic')"
        @date-select="filtreDegisti"
      />
      <DatePicker
        v-model="filtreBitis"
        date-format="dd.mm.yy"
        show-icon
        :manual-input="false"
        class="filtre-tarih"
        :placeholder="t('stokSayim.bitis')"
        @date-select="filtreDegisti"
      />
      <Button
        v-if="filtreVar"
        :label="t('stokSayim.temizle')"
        icon="pi pi-filter-slash"
        class="p-button-text p-button-sm"
        @click="filtreTemizle"
      />
      <span class="arac-bosluk" />
      <Button
        :label="t('stokSayim.excel')"
        icon="pi pi-file-excel"
        outlined
        size="small"
        :loading="excelYukleniyor"
        @click="excelIndir"
      />
    </div>

    <div
      class="sayim-cipler"
      role="group"
      :aria-label="t('common.status')"
    >
      <button
        v-for="c in durumCipleri"
        :key="c.deger"
        type="button"
        class="sayim-cip"
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
      class="sayim-masa"
      striped-rows
      :loading="yukleniyor"
      :empty-message="filtreVar ? t('stokSayim.filtreBos') : t('stokSayim.veriYok')"
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
        field="stokAdi"
        :header="t('stokSayim.urun')"
        sortable
      />
      <Column
        field="beklenenMiktar"
        :header="t('stokSayim.beklenen')"
      >
        <template #body="{ data }">
          {{ formatNumber(data.beklenenMiktar) }}
        </template>
      </Column>
      <Column
        field="sayilanMiktar"
        :header="t('stokSayim.sayilan')"
      >
        <template #body="{ data }">
          {{ formatNumber(data.sayilanMiktar) }}
        </template>
      </Column>
      <Column
        field="fark"
        :header="t('stokSayim.fark')"
      >
        <template #body="{ data }">
          <span :class="(data.fark ?? data.sayilanMiktar - data.beklenenMiktar) >= 0 ? 'positive' : 'negative'">
            {{ formatNumber(data.fark ?? data.sayilanMiktar - data.beklenenMiktar) }}
          </span>
        </template>
      </Column>
      <Column
        field="durum"
        :header="t('common.status')"
      >
        <template #body="{ data }">
          <Tag
            :value="durumLabel(data.durum, t)"
            :severity="durumSeverity(data.durum)"
          />
        </template>
      </Column>
      <Column
        :header="t('stokSayim.islem')"
        style="width: 80px"
      >
        <template #body="{ data }">
          <SatirEylemleri
            :gorunur="{ duzenle: false, cogalt: false, sil: false }"
            :items="sayimAksiyonlari(data)"
          />
        </template>
      </Column>
    </AppDataTable>

    <!-- Mobil kart görünümü -->
    <div class="sayim-mobil">
      <div
        v-if="yukleniyor && !list.length"
        class="sayim-skeleton"
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
        :message="filtreVar ? t('stokSayim.filtreBos') : t('stokSayim.veriYok')"
        icon="pi pi-clipboard"
      />
      <div
        v-for="s in list"
        :key="s.id"
        class="sayim-mobil-kart"
        @click="detayAc(s)"
      >
        <div class="smk-ust">
          <strong>{{ s.stokAdi }}</strong>
          <SatirEylemleri
            :gorunur="{ duzenle: false, cogalt: false, sil: false }"
            :items="sayimAksiyonlari(s)"
            @click.stop
          />
        </div>
        <div class="smk-taglar">
          <Tag
            :value="durumLabel(s.durum, t)"
            :severity="durumSeverity(s.durum)"
          />
          <span class="smk-tarih">{{ formatDate(s.tarih) }}</span>
        </div>
        <div class="smk-satir">
          <span>{{ t('stokSayim.beklenen') }}: <strong>{{ formatNumber(s.beklenenMiktar) }}</strong></span>
          <span>{{ t('stokSayim.sayilan') }}: <strong>{{ formatNumber(s.sayilanMiktar) }}</strong></span>
        </div>
        <div class="smk-satir">
          <span>{{ t('stokSayim.fark') }}:</span>
          <strong :class="Number(s.fark) >= 0 ? 'positive' : 'negative'">{{ formatNumber(s.fark) }}</strong>
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
          <i class="pi pi-clipboard" />
          <span>{{ dialogHeader }}</span>
        </div>
      </template>
      <div class="form-grid">
        <div class="field">
          <label>{{ t('stokSayim.tarihZorunlu') }}</label><DatePicker
            v-model="form.tarih"
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('stokSayim.urunZorunlu') }}</label>
          <Dropdown
            v-model="form.stokId"
            :options="stokOnerileri"
            option-label="ad"
            option-value="id"
            :placeholder="t('stokSayim.urunSec')"
            class="w-full"
            filter
            filter-by="ad,stokKodu,barkod"
            @filter="stokAra"
            @change="onStokSec"
          />
        </div>
        <div class="field">
          <label>{{ t('stokSayim.beklenenMiktar') }}</label><InputNumber
            v-model="form.beklenenMiktar"
            class="w-full"
            :min="0"
            disabled
          />
        </div>
        <div class="field">
          <label>{{ t('stokSayim.sayilanMiktarZorunlu') }}</label><InputNumber
            v-model="form.sayilanMiktar"
            class="w-full"
            :min="0"
          />
        </div>
        <div class="field">
          <label>{{ t('stokSayim.fark') }}</label>
          <span
            :class="{
              positive: form.sayilanMiktar - form.beklenenMiktar >= 0,
              negative: form.sayilanMiktar - form.beklenenMiktar < 0
            }"
            style="font-weight: 700; font-size: 18px"
          >
            {{ formatNumber(form.sayilanMiktar - form.beklenenMiktar) }}
          </span>
        </div>
        <div class="field">
          <label>{{ t('stokSayim.aciklamaAlani') }}</label>
          <Textarea
            v-model="form.aciklama"
            rows="2"
            auto-resize
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
    <BarcodeScannerModal
      v-model:visible="taramaModu"
      @scan="barkodOkundu"
    />

    <StokSayimDetayDrawer
      v-model:visible="detayDrawer"
      :sayim="seciliSayim"
      @duzenle="drawerDuzenle"
      @tamamla="drawerTamamla"
      @iptal="drawerIptal"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { stokSayimAPI, excelAPI } from '../api/index.js'
import { useStokOnerileri } from '../composables/useStokOnerileri.js'
import { useI18n } from 'vue-i18n'
import { formatTarih as formatDate, getLocalDateString, durumLabel } from '../utils/format.js'
import BarcodeScannerModal from '../components/BarcodeScannerModal.vue'
import KpiKart from '../components/KpiKart.vue'
import SatirEylemleri from '../components/SatirEylemleri.vue'
import StokSayimDetayDrawer from '../components/StokSayimDetayDrawer.vue'
import SkeletonLoader from '../components/SkeletonLoader.vue'
import EmptyState from '../components/EmptyState.vue'

const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { t } = useI18n()
const list = ref([])
const toplam = ref(0)
const sayfa = ref(0)
const SAYFA_BOYUTU_ANAHTAR = 'raspel_stok_sayim_sayfa_boyutu'
const sayfaBoyutu = ref(Number(localStorage.getItem(SAYFA_BOYUTU_ANAHTAR)) || 25)
const sirala = ref('tarih,desc')
const filtreQ = ref('')
const filtreDurum = ref('TUMU')
const filtreBaslangic = ref(null)
const filtreBitis = ref(null)
const excelYukleniyor = ref(false)
let aramaZaman = null
// Sayım ürün seçici sunucu aramalı. Önceden `stokAPI.getAll({ size: 5000 })`
// ile 5000 stok her sayfa açılışında çekiliyordu (uygulamadaki en büyük tek
// istek) ve 5000. stoktan sonrası seçilemiyordu.
const { oneriler: stokOnerileri, ara: stokAra, hemenAra: stokOnerileriYukle } = useStokOnerileri()
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const dialog = ref(false)
const duzenleme = ref(false)
const taramaModu = ref(false)
const taramaAdet = ref(1)
const sonTaramalar = ref([])
const detayDrawer = ref(false)
const seciliSayim = ref(null)
const form = ref({ tarih: new Date(), stokId: null, beklenenMiktar: 0, sayilanMiktar: 0, aciklama: '' })

const ozet = ref({ toplam: 0, taslak: 0, tamamlanan: 0, iptal: 0, farkli: 0, bugun: 0 })
const ozetYukle = async () => {
  try {
    const r = await stokSayimAPI.ozet()
    ozet.value = { ...ozet.value, ...(r.data || {}) }
  } catch {
    /* özet alınamazsa KPI sıfır kalır */
  }
}

const dialogHeader = computed(() => (duzenleme.value ? t('stokSayim.sayimDuzenle') : t('stokSayim.yeniSayim')))

const formatNumber = (v) => {
  if (v === null || v === undefined) return '0'
  return Number(v).toLocaleString('tr-TR', { maximumFractionDigits: 4 })
}

const durumSeverity = (d) => (d === 'TAMAMLANDI' ? 'success' : d === 'IPTAL' ? 'danger' : 'warn')

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
  if (filtreBaslangic.value) p.baslangic = tarihIso(filtreBaslangic.value)
  if (filtreBitis.value) p.bitis = tarihIso(filtreBitis.value)
  return p
}

const yukle = async () => {
  yukleniyor.value = true
  try {
    const r = await stokSayimAPI.filtreli(filtreParams())
    list.value = r.data?.content || []
    toplam.value = r.data?.totalElements ?? list.value.length
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('stokSayim.hataYukleme'))
  } finally {
    yukleniyor.value = false
  }
  ozetYukle()
}

const durumCipleri = computed(() => [
  { deger: 'TUMU', ad: t('stokSayim.cipTumu'), ikon: 'pi pi-list' },
  { deger: 'TASLAK', ad: t('common.durumTaslak'), ikon: 'pi pi-file-edit' },
  { deger: 'TAMAMLANDI', ad: t('common.durumTamamlandi'), ikon: 'pi pi-check-circle' },
  { deger: 'IPTAL', ad: t('common.durumIptal'), ikon: 'pi pi-times-circle' }
])

const filtreVar = computed(
  () =>
    !!filtreQ.value.trim() ||
    (filtreDurum.value && filtreDurum.value !== 'TUMU') ||
    !!filtreBaslangic.value ||
    !!filtreBitis.value
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
  filtreBaslangic.value = null
  filtreBitis.value = null
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
    const res = await excelAPI.stokSayim(params)
    const url = window.URL.createObjectURL(
      new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    )
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `stok-sayim-${new Date().toISOString().slice(0, 10)}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    toastBildirim.basarili(t('stokSayim.excelIndirildi'))
  } catch {
    toastBildirim.hata(t('stokSayim.excelHata'))
  } finally {
    excelYukleniyor.value = false
  }
}

onMounted(async () => {
  // Dropdown ilk açılışta boş görünmesin diye ilk sayfa bir kez çekilir.
  stokOnerileriYukle()
  await yukle()
})

const barkodOkundu = async (kod) => {
  try {
    const res = await stokSayimAPI.tara({ barkod: kod, adet: taramaAdet.value })
    const sayim = res.data
    sonTaramalar.value.unshift({ ad: sayim.stokAdi || kod, adet: taramaAdet.value, toplam: sayim.sayilanMiktar })
    if (sonTaramalar.value.length > 6) sonTaramalar.value.length = 6
    toastBildirim.basarili(t('stokSayim.tarananEklendi', { stok: sayim.stokAdi || kod, adet: taramaAdet.value, toplam: sayim.sayilanMiktar }))
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('stokSayim.barkodBulunamadi'))
  }
}

const dialogAc = (data) => {
  duzenleme.value = !!data
  form.value = data
    ? {
        ...data,
        tarih: data.tarih ? new Date(data.tarih) : new Date(),
        aciklama: data.aciklama || ''
      }
    : {
        tarih: new Date(),
        stokId: null,
        beklenenMiktar: 0,
        sayilanMiktar: 0,
        aciklama: ''
      }
  dialog.value = true
}

const onStokSec = () => {
  const stok = stokOnerileri.value.find((s) => s.id === form.value.stokId)
  form.value.beklenenMiktar = stok?.miktar ?? 0
}

const kaydet = async () => {
  if (!form.value.tarih) {
    toastBildirim.uyari(t('stokSayim.tarihZorunlu'))
    return
  }
  if (!form.value.stokId) {
    toastBildirim.uyari(t('stokSayim.urunZorunlu'))
    return
  }
  if (form.value.sayilanMiktar == null || Number(form.value.sayilanMiktar) < 0) {
    toastBildirim.uyari(t('stokSayim.sayilanNegatifOlamaz'))
    return
  }
  kaydediliyor.value = true
  try {
    const payload = {
      ...form.value,
      tarih: getLocalDateString(form.value.tarih)
    }
    if (duzenleme.value) {
      await stokSayimAPI.update(form.value.id, payload)
      toastBildirim.basarili(t('stokSayim.guncellendi'))
    } else {
      await stokSayimAPI.create(payload)
      toastBildirim.basarili(t('stokSayim.olusturuldu'))
    }
    dialog.value = false
    await yukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('stokSayim.islemBasarisiz'))
  }
  kaydediliyor.value = false
}

// ⋮ satır menüsü: detay + duruma göre işlemler.
const sayimAksiyonlari = (d) => {
  const items = [{ etiket: t('stokSayim.detay'), ikon: 'pi pi-search', islem: () => detayAc(d) }]
  if (d.durum === 'TASLAK') {
    items.push({ etiket: t('common.edit'), ikon: 'pi pi-pencil', islem: () => dialogAc(d) })
  }
  if (d.durum !== 'TAMAMLANDI') {
    items.push({ etiket: t('stokSayim.tamamla'), ikon: 'pi pi-check-circle', islem: () => durumGuncelle(d, 'TAMAMLANDI') })
  }
  if (d.durum !== 'IPTAL') {
    items.push({ etiket: t('common.cancel'), ikon: 'pi pi-times-circle', sinif: 'eylem-sil', islem: () => durumGuncelle(d, 'IPTAL') })
  }
  if (d.durum !== 'TAMAMLANDI') {
    items.push({ etiket: t('common.delete'), ikon: 'pi pi-trash', sinif: 'eylem-sil', islem: () => sil(d) })
  }
  return items
}

const detayAc = (d) => {
  seciliSayim.value = d
  detayDrawer.value = true
}
const drawerDuzenle = (d) => {
  detayDrawer.value = false
  dialogAc(d)
}
const drawerTamamla = (d) => {
  detayDrawer.value = false
  durumGuncelle(d, 'TAMAMLANDI')
}
const drawerIptal = (d) => {
  detayDrawer.value = false
  durumGuncelle(d, 'IPTAL')
}

const durumGuncelle = async (data, durum) => {
  // Stok ve cari etkisi olan durumlar onay ister (yanlis tikla stok degismesin).
  if (durum === 'TAMAMLANDI' || (data.durum === 'TAMAMLANDI' && durum !== 'TAMAMLANDI')) {
    confirm.require({
      message: durum === 'TAMAMLANDI'
        ? t('stokSayim.tamamlaOnayMesaj')
        : t('stokSayim.geriAlOnayMesaj'),
      header: t('common.onay'),
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: t('common.evet'),
      rejectLabel: t('common.vazgec'),
      accept: () => durumUygula(data, durum)
    })
    return
  }
  await durumUygula(data, durum)
}

const durumUygula = async (data, durum) => {
  try {
    await stokSayimAPI.durumGuncelle(data.id, durum)
    await yukle()
    toastBildirim.basarili(t('stokSayim.durumGuncellendi', { durum: durumLabel(durum, t) }))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('stokSayim.durumHata'))
  }
}

const sil = (data) => {
  confirm.require({
    message: t('stokSayim.silOnayMesaj'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.evetSil'),
    rejectLabel: t('common.vazgec'),
    accept: async () => {
      try {
        await stokSayimAPI.delete(data.id)
        await yukle()
        toastBildirim.basarili(t('stokSayim.silindi'))
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('stokSayim.silmeBasarisiz'))
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
.stoksayim-container {
  padding: 0;
}
.kpi-satiri {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.sayim-arac {
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
.filtre-tarih {
  min-width: 150px;
}
.arac-bosluk {
  flex: 1 1 auto;
}
.sayim-cipler {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.sayim-cip {
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
.sayim-cip:hover {
  border-color: var(--accent-border);
  color: var(--text-primary);
}
.sayim-cip.aktif {
  background: var(--accent-soft, rgba(245, 158, 11, 0.16));
  border-color: var(--accent-border, rgba(245, 158, 11, 0.5));
  color: var(--accent);
}
.sayim-cip i {
  font-size: 12px;
}
.sayim-mobil {
  display: none;
}
.sayim-skeleton {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.sayim-mobil-kart {
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
.smk-tarih {
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
  .sayim-masa {
    display: none;
  }
  .sayim-mobil {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .sayim-arac .filtre-arama,
  .sayim-arac .filtre-tarih {
    min-width: 0;
    max-width: none;
    flex: 1 1 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .sayim-cip {
    transition: none;
  }
}
.sayfa-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.tarama-adet {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}
.adet-input {
  width: 90px;
}
.tarama-paneli {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px 14px;
  margin-bottom: 16px;
  background: rgba(74, 222, 128, 0.08);
  border: 1px solid rgba(74, 222, 128, 0.35);
  border-radius: 8px;
}
.tarama-paneli-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  color: #4ade80;
}
.tarama-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.tarama-chip {
  margin: 0;
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
.positive {
  color: #4ade80;
}
.negative {
  color: #f87171;
}
</style>
