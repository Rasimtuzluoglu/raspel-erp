<template>
  <div class="satis-container">
    <h1 class="page-title">
      {{ t('satis.title') }}
    </h1>

    <Toolbar class="toolbar">
      <template #start>
        <Button
          :label="t('satis.yeniSatis')"
          icon="pi pi-plus"
          class="p-button-success"
          @click="openSatis"
        />
      </template>
      <template #end>
        <TarihHizliSecim
          v-model="tarihAraligi"
          style="margin-right: 8px"
        />
        <span class="p-input-icon-left">
          <i class="pi pi-search" />
          <InputText
            v-model="filtre"
            :placeholder="t('satis.aramaPlaceholder')"
          />
        </span>
      </template>
    </Toolbar>

    <div class="table-container">
      <DataTable
        :value="satislar"
        :lazy="true"
        :paginator="true"
        :rows="sayfaBoyutu"
        :first="sayfa * sayfaBoyutu"
        :total-records="toplamKayit"
        :loading="loading"
        paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport"
        :current-page-report-template="'{totalRecords} ' + $t('common.recordsWord') + ' · {first}-{last}'"
        striped-rows
        @page="sayfaDegisti"
      >
        <template #empty>
          <EmptyState />
        </template>
        <Column
          field="faturaNumarasi"
          :header="t('satis.colFaturaNo')"
          style="width: 150px"
        />
        <Column
          field="tarih"
          :header="t('common.date')"
          style="width: 105px"
        >
          <template #body="s">
            {{ formatDate(s.data.tarih) }}
          </template>
        </Column>
        <Column
          field="cariHesapAd"
          :header="t('satis.colMusteri')"
          style="width: 180px"
        >
          <template #body="s">
            {{ s.data.cariHesapAd || '-' }}
          </template>
        </Column>
        <Column
          field="kdv"
          :header="t('satis.colKdv')"
          style="width: 100px"
        >
          <template #body="s">
            {{ formatPara(s.data.kdv, s.data.paraBirimi) }}
          </template>
        </Column>
        <Column
          field="genelToplam"
          :header="t('common.amount')"
          style="width: 120px"
        >
          <template #body="s">
            {{ formatPara(s.data.genelToplam, s.data.paraBirimi) }}
          </template>
        </Column>
        <Column
          field="kalanTutar"
          :header="t('satis.colKalan')"
          style="width: 110px"
        >
          <template #body="s">
            <span :class="{ 'kalan-var': Number(s.data.kalanTutar || 0) > 0 }">
              {{ formatPara(s.data.kalanTutar, s.data.paraBirimi) }}
            </span>
          </template>
        </Column>
        <Column
          field="odemeDurumu"
          :header="t('satis.colOdeme')"
          style="width: 120px"
        >
          <template #body="s">
            <Tag
              v-if="s.data.odemeDurumu"
              :value="odemeDurumEtiketi(s.data.odemeDurumu)"
              :severity="odemeDurumSeverity(s.data.odemeDurumu)"
            />
          </template>
        </Column>
        <Column
          field="durum"
          :header="t('common.status')"
          style="width: 150px"
        >
          <template #body="s">
            <span :class="['durum-badge', (s.data.durum || '').toLowerCase()]">{{ durumLabel(s.data.durum) }}</span>
            <Tag
              v-if="gecikmisGun(s.data) > 0"
              class="gecikme-rozet"
              :value="t('satis.gunGecikti', { n: gecikmisGun(s.data) })"
              severity="danger"
            />
          </template>
        </Column>
        <Column
          :header="t('satis.colTeslimat')"
          style="width: 130px"
        >
          <template #body="s">
            <Tag
              v-if="s.data.teslimatVar"
              :value="teslimatDurumEtiketi(s.data.teslimatDurum)"
              :severity="TESLIMAT_DURUM_SEVERITY[s.data.teslimatDurum] || 'secondary'"
              :title="s.data.driverAd || ''"
              style="cursor: pointer"
              @click="router.push({ name: 'Teslimatlar' })"
            />
            <span
              v-else
              class="teslimat-yok"
            >—</span>
          </template>
        </Column>
        <Column
          :header="t('common.actions')"
          style="width: 120px"
        >
          <template #body="s">
            <SatirEylemleri
              :gorunur="{ duzenle: false, cogalt: false, sil: false }"
              :items="satisEylemleri(s.data)"
            />
          </template>
        </Column>
      </DataTable>
    </div>

    <Dialog
      v-model:visible="showSatisDialog"
      :header="dialogBaslik"
      :modal="true"
      style="width: 800px"
      :closable="false"
    >
      <div class="satis-modu">
        <label style="color: #94a3b8; font-weight: 600; font-size: 12px; text-transform: uppercase; margin-right: 12px">{{ t('satis.islemModu') }}</label>
        <div class="modu-radio-group">
          <div
            :class="['modu-option', { active: satisModu === 'SATIS' }]"
            @click="satisModu = 'SATIS'"
          >
            <i class="pi pi-shopping-cart" /> {{ t('satis.satisYap') }}
          </div>
          <div
            :class="['modu-option', { active: satisModu === 'TEKLIF' }]"
            @click="satisModu = 'TEKLIF'"
          >
            <i class="pi pi-file" /> {{ t('satis.teklifOlustur') }}
          </div>
        </div>
      </div>
      <div class="form-row">
        <div
          class="form-group"
          style="flex: 2"
        >
          <label>{{ t('satis.musteri') }} <span v-if="satisModu === 'SATIS'">*</span></label>
          <Dropdown
            v-model="satisForm.cariHesapId"
            :options="cariHesapStore?.cariHesaplar || []"
            option-label="ad"
            option-value="id"
            :placeholder="t('satis.musteriSeciniz')"
            class="w-full"
          />
        </div>
        <div
          class="form-group"
          style="flex: 1"
        >
          <label>{{ t('satis.tarihZorunlu') }}</label>
          <DatePicker
            v-model="satisForm.tarih"
            date-format="dd.mm.yy"
            class="w-full"
          />
        </div>
      </div>

      <div class="urun-ekleme">
        <div class="form-row">
          <div
            class="form-group"
            style="flex: 3"
          >
            <label>{{ t('satis.urunSec') }}</label>
            <Dropdown
              v-model="seciliUrun"
              :options="stokStore.stoklar"
              filter
              option-label="ad"
              option-value="id"
              :placeholder="t('satis.urunAra')"
              class="w-full"
              @change="urunSecildi"
            >
              <template #value="slotProps">
                <span v-if="slotProps.value">{{ stokAdi(slotProps.value) }}</span>
                <span v-else>{{ slotProps.placeholder }}</span>
              </template>
              <template #option="slotProps">
                <div class="urun-opsiyon">
                  <span class="urun-ad">{{ slotProps.option.ad }}</span>
                  <span class="urun-stok">{{ slotProps.option.miktar }} {{ slotProps.option.birim || t('satis.adetBirim') }}</span>
                  <span class="urun-fiyat">{{ formatCurrency(slotProps.option.fiyat) }}</span>
                </div>
              </template>
            </Dropdown>
          </div>
          <div
            class="form-group"
            style="flex: 1"
          >
            <label>{{ t('satis.miktar') }}</label>
            <InputNumber
              v-model="yeniUrunAdet"
              :min="1"
              class="w-full"
            />
          </div>
          <div
            class="form-group"
            style="flex: 1"
          >
            <label>{{ t('satis.birimFiyat') }}</label>
            <InputNumber
              v-model="yeniUrunFiyat"
              :min="0"
              :min-fraction-digits="2"
              class="w-full"
            />
          </div>
          <div
            class="form-group"
            style="flex: 0 0 auto; display: flex; align-items: flex-end"
          >
            <Button
              icon="pi pi-plus"
              :aria-label="$t('common.add')"
              class="p-button-success"
              :disabled="!seciliUrun || !yeniUrunAdet"
              @click="urunEkle"
            />
          </div>
        </div>
        <small style="color: #64748b">{{ t('satis.fiyatOtomatik') }}</small>
        <CariUrunFiyatPaneli
          v-if="seciliUrun && (fiyatSecenekleri.length || (cariUrunFiyati && cariUrunFiyati.sonFiyat != null))"
          :fiyat-gecmisi="cariUrunFiyati"
          :secenekler="fiyatSecenekleri"
          class="mt-2"
          @uygula="satisCariFiyatUygula"
        />
      </div>

      <h3 style="margin: 18px 0 10px; color: #f1f5f9; font-size: 15px">
        {{ t('satis.satisKalemleri') }}
      </h3>
      <FaturaKalemleri
        :kalemler="satisForm.kalemler"
        :ara-toplam="araToplam"
        :kdv-toplam="kdvToplam"
        :genel-toplam="genelToplam"
        :kdv-secenekleri="kdvOranlari"
        @add="satisForm.kalemler.push({ aciklama: '', adet: 1, birimFiyat: 0, iskontoOrani: 0, kdvOrani: 20 })"
        @remove="(i) => satisForm.kalemler.splice(i, 1)"
      />

      <div class="form-group">
        <label>{{ t('common.description') }}</label>
        <Textarea
          v-model="satisForm.aciklama"
          rows="2"
          class="w-full"
        />
      </div>

      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="showSatisDialog = false"
        />
        <Button
          :label="satisModu === 'TEKLIF' ? t('satis.teklifiKaydet') : t('satis.satisiTamamla')"
          icon="pi pi-check"
          :loading="saving"
          :disabled="satisForm.kalemler.length === 0 || (satisModu === 'SATIS' && !satisForm.cariHesapId)"
          @click="satisiTamamla"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { faturaAPI, teklifAPI } from '../api/index.js'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { useStokStore } from '../stores/stokStore.js'
import { useAuthStore } from '../stores/authStore.js'
import { useRouter } from 'vue-router'
import { useMarka } from '../composables/useMarka.js'
import { escapeHtml } from '../utils/escapeHtml.js'
import { fisPenceresiAcVeYazdir } from '../utils/fisYazdir.js'
import TarihHizliSecim from '../components/TarihHizliSecim.vue'
import CariUrunFiyatPaneli from '../components/CariUrunFiyatPaneli.vue'
import FaturaKalemleri from '../components/FaturaKalemleri.vue'
import { useUrunFiyatlari } from '../composables/useUrunFiyatlari.js'
import { formatCurrency, formatPara, getLocalDateString, durumLabel as durumLabelUtil } from '../utils/format.js'
import { kalemNetTutar, kalemKdv } from '../utils/faturaHesapla.js'
import { satisPayloadUret } from '../utils/satisPayload.js'
import { useI18n } from 'vue-i18n'

const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const router = useRouter()
const { t } = useI18n()
const cariHesapStore = useCariHesapStore()
const stokStore = useStokStore()
const authStore = useAuthStore()
const { sirketLogosu } = useMarka()

const satislar = ref([])
const showSatisDialog = ref(false)
const saving = ref(false)
const filtre = ref('')
const satisModu = ref('SATIS')
const seciliUrun = ref(null)
const yeniUrunAdet = ref(1)
const yeniUrunFiyat = ref(0)
const kdvOranlari = [0, 1, 8, 10, 18, 20]

// Sunucu tarafli sayfalama (tum fatura listesini cekip istemcide filtrelemek
// ilk 50 kayitla sinirliydi; eski satislar gorunmuyordu).
const sayfa = ref(0)
const sayfaBoyutu = ref(25)
const toplamKayit = ref(0)
const loading = ref(false)
let aramaZamanlayici = null

const satisForm = ref({
  cariHesapId: null,
  tarih: new Date(),
  aciklama: '',
  kalemler: []
})

const tarihAraligi = ref(null)

const tarihParametreleri = () => {
  if (!tarihAraligi.value || tarihAraligi.value.length !== 2 || !tarihAraligi.value[0]) return {}
  return { bas: getLocalDateString(tarihAraligi.value[0]), bit: getLocalDateString(tarihAraligi.value[1]) }
}

onMounted(async () => {
  // Store'lar hata firlatir; bir hata digerlerini engellemesin.
  await Promise.allSettled([satislariYukle(), cariHesapStore.getAllCariHesaplar(), stokStore.getAll()])
})

const satislariYukle = async (yeniSayfa = sayfa.value, yeniBoyut = sayfaBoyutu.value) => {
  loading.value = true
  try {
    const params = { page: yeniSayfa, size: yeniBoyut, tur: 'SATIS', ...tarihParametreleri() }
    if (filtre.value.trim()) params.search = filtre.value.trim()
    const r = await faturaAPI.getAll(params)
    satislar.value = unwrapList(r)
    toplamKayit.value = r.data?.totalElements ?? satislar.value.length
    sayfa.value = yeniSayfa
    sayfaBoyutu.value = yeniBoyut
  } catch {
    toastBildirim.hata(t('satis.satislarYuklenemedi'))
  } finally {
    loading.value = false
  }
}

const sayfaDegisti = (e) => satislariYukle(e.page, e.rows)

watch(tarihAraligi, () => satislariYukle(0, sayfaBoyutu.value))
watch(filtre, () => {
  clearTimeout(aramaZamanlayici)
  aramaZamanlayici = setTimeout(() => satislariYukle(0, sayfaBoyutu.value), 300)
})

// Vadesi gecen kesilmis satislar icin gecikme gunu (rozet).
const gecikmisGun = (s) => {
  if (s.durum !== 'KESILDI' || !s.vadeTarihi || Number(s.kalanTutar || 0) <= 0) return 0
  const vade = new Date(s.vadeTarihi)
  if (isNaN(vade.getTime())) return 0
  const bugun = new Date()
  vade.setHours(0, 0, 0, 0)
  bugun.setHours(0, 0, 0, 0)
  const gun = Math.floor((bugun - vade) / 86400000)
  return gun > 0 ? gun : 0
}

const TESLIMAT_DURUM_SEVERITY = { BEKLEMEDE: 'warn', YOLDA: 'info', TESLIM_EDILDI: 'success', IPTAL: 'danger' }
const teslimatDurumEtiketi = (d) =>
  ({ BEKLEMEDE: t('teslimatlar.durumBeklemede'), YOLDA: t('teslimatlar.durumYolda'), TESLIM_EDILDI: t('teslimatlar.durumTeslimEdildi'), IPTAL: t('teslimatlar.durumIptal') })[d] || d

const tahsilataGit = (s) => router.push({ name: 'Tahsilat', query: { cariId: s.cariHesapId } })

const satisIptal = (s) => {
  confirm.require({
    message: t('satis.iptalOnay', { no: s.faturaNumarasi }),
    header: t('satis.iptalBaslik'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.onay'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        await faturaAPI.updateDurum(s.id, 'IPTAL')
        toastBildirim.basarili(t('satis.iptalEdildi'))
        await satislariYukle()
      } catch (e) {
        toastBildirim.hata(e?.response?.data?.message || t('satis.iptalBasarisiz'))
      }
    }
  })
}

// Mevcut satisi kopyalayarak yeni satis diyalogu acar.
const satisCogalt = async (s) => {
  try {
    const r = await faturaAPI.getById(s.id)
    const f = r.data || {}
    satisModu.value = 'SATIS'
    satisForm.value = {
      cariHesapId: f.cariHesapId || null,
      tarih: new Date(),
      aciklama: f.aciklama || '',
      kalemler: (f.kalemler || []).map((k) => ({
        aciklama: k.aciklama || '',
        adet: k.adet || 1,
        birimFiyat: k.birimFiyat || 0,
        iskontoOrani: k.iskontoOrani || 0,
        kdvOrani: k.kdvOrani ?? 20,
        stokId: k.stokId || null
      }))
    }
    showSatisDialog.value = true
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || t('satis.satislarYuklenemedi'))
  }
}

const satisEylemleri = (s) => {
  const items = [
    { etiket: t('satis.goruntule'), ikon: 'pi pi-eye', islem: () => router.push(`/faturalar/${s.id}`) },
    { etiket: t('satis.a4Yazdir'), ikon: 'pi pi-print', islem: () => printFatura(s.id) },
    { etiket: t('satis.termalYazdir'), ikon: 'pi pi-receipt', islem: () => printTermalFis(s) },
    { etiket: t('satis.cogalt'), ikon: 'pi pi-copy', islem: () => satisCogalt(s) }
  ]
  if (s.durum === 'KESILDI' && Number(s.kalanTutar || 0) > 0) {
    items.push({ etiket: t('satis.tahsilatAl'), ikon: 'pi pi-money-bill', islem: () => tahsilataGit(s) })
  }
  if (s.durum === 'KESILDI') {
    items.push({ etiket: t('satis.iadeOlustur'), ikon: 'pi pi-replay', islem: () => router.push({ name: 'Iadeler', query: { faturaId: s.id } }) })
    items.push({ etiket: t('satis.efatura'), ikon: 'pi pi-send', islem: () => router.push({ name: 'EFatura', query: { faturaId: s.id } }) })
    items.push({ etiket: t('common.cancel'), ikon: 'pi pi-ban', sinif: 'eylem-sil', islem: () => satisIptal(s) })
  }
  return items
}

const stokAdi = (id) => {
  const u = stokStore.stoklar.find((s) => s.id === id)
  return u ? `${u.ad} (${u.miktar} ${u.birim || 'Adet'}) - ${formatCurrency(u.fiyat)}` : ''
}

const urunSecildi = async () => {
  if (!seciliUrun.value) return
  const u = stokStore.stoklar.find((s) => s.id === seciliUrun.value)
  if (u) yeniUrunFiyat.value = u.fiyat
  cariUrunFiyati.value = null
  const cariId = satisForm.value.cariHesapId
  if (cariId && seciliUrun.value) {
    try {
      const r = await faturaAPI.cariUrunFiyatGecmisi(cariId, seciliUrun.value)
      cariUrunFiyati.value = r.data || null
    } catch {
      cariUrunFiyati.value = null
    }
  }
  await fiyatlariYukle(cariId, seciliUrun.value, u?.fiyat)
}

// Faz 2: secilen cariye bu urunun son satis fiyati
const cariUrunFiyati = ref(null)
const { secenekler: fiyatSecenekleri, yukle: fiyatlariYukle, temizle: fiyatlariTemizle } = useUrunFiyatlari()
const satisCariFiyatUygula = (f) => {
  yeniUrunFiyat.value = f
}

const urunEkle = () => {
  if (!seciliUrun.value || !yeniUrunAdet.value) return
  const u = stokStore.stoklar.find((s) => s.id === seciliUrun.value)
  if (!u) return
  if (u.miktar < yeniUrunAdet.value) {
    toastBildirim.uyari(t('satis.yetersizStok', { miktar: u.miktar, birim: u.birim || t('satis.adetBirim') }))
    return
  }
  const brf = yeniUrunFiyat.value || u.fiyat
  const kalemKdvOrani = Number(u.kdvOrani ?? 20)
  satisForm.value.kalemler.push({
    aciklama: u.ad,
    adet: yeniUrunAdet.value,
    birimFiyat: brf,
    iskontoOrani: 0,
    kdvOrani: kalemKdvOrani,
    stokId: u.id
  })
  seciliUrun.value = null
  yeniUrunAdet.value = 1
  yeniUrunFiyat.value = 0
  fiyatlariTemizle()
}

const araToplam = computed(() =>
  satisForm.value.kalemler.reduce((t, k) => t + kalemNetTutar(k), 0)
)
const kdvToplam = computed(() =>
  satisForm.value.kalemler.reduce((t, k) => t + kalemKdv(k), 0)
)
const genelToplam = computed(() => araToplam.value + kdvToplam.value)

const dialogBaslik = computed(() => (satisModu.value === 'TEKLIF' ? t('satis.yeniTeklif') : t('satis.yeniSatisDialog')))

const openSatis = () => {
  satisForm.value = { cariHesapId: null, tarih: new Date(), aciklama: '', kalemler: [] }
  seciliUrun.value = null
  yeniUrunAdet.value = 1
  yeniUrunFiyat.value = 0
  satisModu.value = 'SATIS'
  showSatisDialog.value = true
}

const satisiTamamla = async () => {
  if (satisModu.value === 'SATIS' && !satisForm.value.cariHesapId) {
    toastBildirim.uyari(t('satis.musteriSecinizUyari'))
    return
  }
  if (satisForm.value.kalemler.length === 0) {
    toastBildirim.uyari(t('satis.enAzBirUrun'))
    return
  }
  saving.value = true
  try {
    // Teklif modu: fatura yerine GERÇEK Teklif kaydı oluşturulur; böylece teklif
    // Teklifler modülünde görünür ve siparişe/faturaya dönüştürülebilir (eski
    // "durum=TEKLIF" faturası çıkmaz sokaktı).
    if (satisModu.value === 'TEKLIF') {
      await teklifAPI.create({
        cariHesapId: satisForm.value.cariHesapId,
        tarih: getLocalDateString(satisForm.value.tarih),
        tur: 'SATIS',
        durum: 'TASLAK',
        aciklama: satisForm.value.aciklama,
        kalemler: satisForm.value.kalemler.map((k) => ({
          stokId: k.stokId,
          aciklama: k.ad || k.aciklama || '',
          miktar: k.miktar,
          birimFiyat: k.birimFiyat,
          kdvOrani: k.kdvOrani
        }))
      })
      toastBildirim.basarili(t('satis.teklifKaydedildiTekliflerde'))
      showSatisDialog.value = false
      return
    }
    const durum = 'KESILDI'
    const payload = satisPayloadUret({
      cariHesapId: satisForm.value.cariHesapId,
      tur: 'SATIS',
      durum,
      tarih: getLocalDateString(satisForm.value.tarih),
      aciklama: satisForm.value.aciklama,
      kalemler: satisForm.value.kalemler
    })
    await faturaAPI.create(payload)
    toastBildirim.basarili(t('satis.satisTamamlandi'))
    showSatisDialog.value = false
    await satislariYukle()
  } catch (err) {
    const msg = err.response?.data?.message || t('satis.satisBasarisiz')
    toastBildirim.hata(msg)
  } finally {
    saving.value = false
  }
}

const printFatura = (id) => {
  if (id) faturaAPI.yazdirmaKaydet(id, { format: 'A4' }).catch(() => {})
  window.open(`/faturalar/${id}?print=true`, '_blank')
}
const durumLabel = (d) => durumLabelUtil(d, t)

const odemeDurumEtiketi = (d) => ({
  ODENDI: t('faturaDetay.odendi'),
  KISMI_ODENDI: t('faturaDetay.kismiOdedi'),
  ODENMEDI: t('faturaDetay.odenmedi')
})[d] || '-'

const odemeDurumSeverity = (d) =>
  ({ ODENDI: 'success', KISMI_ODENDI: 'warn', ODENMEDI: 'danger' })[d] || 'secondary'
import { formatTarih as formatDate } from '../utils/format.js'
const printTermalFis = (satisData) => {
  if (satisData?.id) faturaAPI.yazdirmaKaydet(satisData.id, { format: 'TERMAL80' }).catch(() => {})

  const kalemlerHtml = (satisData.kalemler || [])
    .map(
      (k) => `
    <tr>
      <td style="text-align:left;">${escapeHtml(k.stokAd || k.ad || t('faturalar.urun'))} x${k.miktar || k.adet || 1}</td>
      <td style="text-align:right;">${formatCurrency(k.toplamTutar || k.miktar * k.birimFiyat || k.adet * k.birimFiyat)}</td>
    </tr>
  `
    )
    .join('')

  const content = `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <title>${t('satis.fisBaslik')} - ${escapeHtml(satisData.faturaNumarasi || '')}</title>
      <style>
        @page { size: 80mm auto; margin: 0; }
        body { font-family: 'Courier New', Courier, monospace; width: 72mm; margin: 0 auto; padding: 10px 0; font-size: 12px; color: #000; }
        .text-center { text-align: center; }
        .text-right { text-align: right; }
        .bold { font-weight: bold; }
        .line { border-top: 1px dashed #000; margin: 6px 0; }
        table { width: 100%; border-collapse: collapse; margin: 6px 0; }
        td, th { padding: 3px 0; vertical-align: top; font-size: 11px; }
        .header { margin-bottom: 8px; }
        .header h2 { margin: 0; font-size: 16px; font-weight: bold; }
        .logo { display: block; max-height: 48px; max-width: 140px; margin: 0 auto 4px; object-fit: contain; }
        .raspel-mini { font-size: 9px; color: #666; margin: 0; }
        .header p { margin: 2px 0; font-size: 10px; }
        .footer { margin-top: 10px; text-align: center; font-size: 10px; }
        .no-print { text-align: center; margin-bottom: 12px; }
        .no-print button { padding: 6px 16px; background: var(--accent-hover); color: #fff; border: none; border-radius: 4px; cursor: pointer; font-weight: bold; }
        @media print { .no-print { display: none !important; } }
      </style>
    </head>
    <body>
      <div class="no-print">
        <button onclick="window.print()">Yazdır (Termal 80mm)</button>
        <button onclick="window.close()" style="background:#64748b; margin-left:6px;">${t('common.close')}</button>
      </div>
      <div class="header text-center">
        ${sirketLogosu.value ? `<img class="logo" src="${escapeHtml(sirketLogosu.value)}" alt="logo" />` : ''}
        <h2>${escapeHtml(authStore?.sirketAdi || 'RASPEL ERP')}</h2>
        <p class="raspel-mini">RasPel ERP</p>
        <p>SATIŞ FİŞİ</p>
        <p>${t('satis.fisNo')} ${escapeHtml(satisData.faturaNumarasi || 'FIS-' + (satisData.id || Date.now()))}</p>
        <p>${t('common.date')}: ${formatDate(satisData.tarih || new Date())}</p>
        <p>${t('satis.musteriLabel')} ${escapeHtml(satisData.cariHesapAd || t('satis.perakendeMusteri'))}</p>
      </div>
      <div class="line"></div>
      <table>
        <thead>
          <tr>
            <th style="text-align:left;">Ürün / Miktar</th>
            <th style="text-align:right;">Tutar</th>
          </tr>
        </thead>
        <tbody>
          ${kalemlerHtml.length ? kalemlerHtml : `<tr><td colspan="2">${t('satis.fisKalemFallback')}</td></tr>`}
        </tbody>
      </table>
      <div class="line"></div>
      <table>
        <tr>
          <td>ARA TOPLAM:</td>
          <td class="text-right bold">${formatCurrency(satisData.araToplam || satisData.genelToplam || 0)}</td>
        </tr>
        <tr>
          <td>KDV:</td>
          <td class="text-right">${formatCurrency(satisData.kdvToplam || 0)}</td>
        </tr>
        <tr style="font-size:13px;">
          <td class="bold">GENEL TOPLAM:</td>
          <td class="text-right bold">${formatCurrency(satisData.genelToplam || 0)}</td>
        </tr>
      </table>
      <div class="line"></div>
      <div class="footer">
        <p>Bizi tercih ettiğiniz için teşekkür ederiz!</p>
        <p>Yazilim: RasPel ERP</p>
        <p>İşlem Yapan: ${escapeHtml(authStore?.kullanici?.displayName || '-')}</p>
      </div>
    </body>
    </html>
  `
  const pencere = fisPenceresiAcVeYazdir(content)
  if (!pencere) {
    toastBildirim.hata(t('satis.pencereAcılmadi'))
  }
}
</script>

<style scoped>
.satis-container {
  padding: 0;
  max-width: 100%;
}
.kalem-girdi :deep(.p-inputtext),
.kalem-girdi :deep(.p-inputnumber-input),
.kalem-girdi :deep(.p-select-label),
.kalem-girdi :deep(.p-dropdown-label) {
  padding: 0.3rem 0.5rem;
  font-size: 13px;
  width: 100%;
}
h1 {
  color: var(--text-primary);
  margin-bottom: 20px;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.5px;
}
.toolbar {
  margin-bottom: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 18px;
}
.table-container {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
}
.form-row {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}
.form-group {
  margin-bottom: 15px;
}
.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 600;
  color: var(--text-secondary);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.urun-ekleme {
  background: rgba(59, 130, 246, 0.05);
  border: 1px solid var(--accent-soft-strong);
  border-radius: 10px;
  padding: 16px;
  margin: 15px 0;
}
.urun-opsiyon {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}
.urun-ad {
  flex: 1;
  color: var(--text-primary);
}
.urun-stok {
  color: #4ade80;
  font-size: 12px;
  font-weight: 600;
}
.urun-fiyat {
  color: var(--text-secondary);
  font-size: 12px;
}
.summary-box {
  background: var(--border);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 15px;
  margin-top: 15px;
}
.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 5px 0;
  font-size: 14px;
  color: var(--text-secondary);
}
.summary-row.total {
  font-weight: 700;
  font-size: 18px;
  border-top: 2px solid var(--accent);
  margin-top: 5px;
  padding-top: 10px;
  color: var(--text-primary);
}
.durum-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
}
.durum-badge.taslak {
  background: rgba(255, 152, 0, 0.15);
  color: #fb923c;
}
.durum-badge.teklif {
  background: var(--accent-soft-strong);
  color: var(--accent);
}
.durum-badge.kesildi {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}
.durum-badge.iptal {
  background: rgba(148, 163, 184, 0.1);
  color: #94a3b8;
}
.satis-modu {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}
.modu-radio-group {
  display: flex;
  gap: 2px;
  background: var(--border);
  border-radius: 8px;
  padding: 3px;
}
.modu-option {
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}
.modu-option:hover {
  color: #e2e8f0;
}
.modu-option.active {
  background: var(--accent);
  color: #fff;
}
.w-full {
  width: 100% !important;
}
/* Kalan tutar, gecikme ve teslimat rozetleri */
.kalan-var {
  color: var(--danger);
  font-weight: 700;
}
.gecikme-rozet {
  margin-left: 6px;
}
.teslimat-yok {
  color: var(--text-muted);
}
</style>
