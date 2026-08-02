<template>
  <div class="raporlar-container">
    <div class="raporlar-header-bar">
      <h1>Raporlar</h1>
      <div class="rapor-doviz-secim">
        <label><i class="pi pi-dollar"></i> Rapor Para Birimi:</label>
        <Dropdown v-model="dovizStore.aktifParaBirimi" :options="['TRY','USD','EUR','GBP','SAR','GAU']" class="rapor-doviz-dropdown" />
      </div>
    </div>

    <div v-if="favoriRaporlar.length" class="favori-raporlar">
      <span class="favori-baslik"><i class="pi pi-star-fill" style="color:#fbbf24"></i> Sık Kullanılanlar:</span>
      <Button v-for="r in favoriRaporlar" :key="r.key" :label="r.ad" size="small" class="p-button-sm p-button-outlined" @click="aktifSekme = r.index" />
    </div>

    <TabView v-model:activeIndex="aktifSekme">
      <TabPanel>
        <template #header>
          <div class="rapor-sekme-baslik">
            <i class="pi pi-star" :class="{ favori: raporFavori('cariEkstre') }" @click.stop="raporFavoriDegistir('cariEkstre', 0, 'Cari Ekstre')"></i>
            Cari Ekstre
          </div>
        </template>
        <div class="rapor-filtre">
          <div class="form-group">
            <label>Cari Hesap</label>
            <Dropdown v-model="ekstreCariId" :options="cariHesapStore.cariHesaplar"
              option-label="ad" option-value="id" placeholder="Seçiniz" class="w-full" />
          </div>
          <div class="form-group">
            <label>Başlangıç</label>
            <DatePicker v-model="ekstreBas" date-format="dd.mm.yy" class="w-full" />
          </div>
          <div class="form-group">
            <label>Bitiş</label>
            <DatePicker v-model="ekstreBit" date-format="dd.mm.yy" class="w-full" />
          </div>
          <div class="form-group filtre-btn">
            <label>&nbsp;</label>
            <Button label="Rapor Getir" icon="pi pi-search" @click="getCariEkstre" :loading="ekstreLoading" />
          </div>
        </div>

        <div v-if="ekstreData" class="rapor-sonuc" ref="ekstreKart">
          <div class="rapor-bilgi">
            <h3>{{ ekstreData.cariAd }}</h3>
            <p>Dönem Başı Bakiye: <strong :class="ekstreData.donemBasBakiye >= 0 ? 'positive' : 'negative'">{{ formatCurrency(ekstreData.donemBasBakiye) }}</strong></p>
            <p>Dönem Sonu Bakiye: <strong :class="ekstreData.donemSonBakiye >= 0 ? 'positive' : 'negative'">{{ formatCurrency(ekstreData.donemSonBakiye) }}</strong></p>
            <div class="rapor-aksiyonlar">
              <Button icon="pi pi-print" label="PDF" class="p-button-sm p-button-outlined" @click="yazdir(ekstreKart)" />
              <Button icon="pi pi-envelope" label="E-posta Gönder" class="p-button-sm p-button-outlined" @click="epostaGonder(ekstreData.cariAd, 'Cari Ekstre Raporu')" />
            </div>
          </div>
          <DataTable :value="ekstreData.hareketler" striped-rows :rows="10" :paginator="true"
            paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport">
            <Column field="hareketTarihi" header="Tarih" style="width:100px">
              <template #body="s">{{ formatDate(s.data.hareketTarihi) }}</template>
            </Column>
            <Column field="tur" header="Tür" style="width:90px">
              <template #body="s">
                <span :class="['badge', s.data.tur === 'TAHSILAT' ? 'tahsilat' : 'odeme']">
                  {{ s.data.tur === 'TAHSILAT' ? 'Tahsilat' : 'Ödeme' }}
                </span>
              </template>
            </Column>
            <Column field="tutar" header="Tutar" style="width:120px">
              <template #body="s">
                <span :class="s.data.tur === 'TAHSILAT' ? 'positive' : 'negative'">{{ formatCurrency(s.data.tutar) }}</span>
              </template>
            </Column>
            <Column field="aciklama" header="Açıklama"></Column>
          </DataTable>
          <Message v-if="ekstreData.hareketler.length === 0" severity="info" text="Bu dönemde hareket bulunmamaktadır." />
        </div>
      </TabPanel>

      <TabPanel>
        <template #header>
          <div class="rapor-sekme-baslik">
            <i class="pi pi-star" :class="{ favori: raporFavori('gelirGider') }" @click.stop="raporFavoriDegistir('gelirGider', 1, 'Gelir/Gider Özeti')"></i>
            Gelir/Gider Özeti
          </div>
        </template>
        <div class="rapor-filtre">
          <div class="form-group">
            <label>Başlangıç</label>
            <DatePicker v-model="ggBas" date-format="dd.mm.yy" class="w-full" />
          </div>
          <div class="form-group">
            <label>Bitiş</label>
            <DatePicker v-model="ggBit" date-format="dd.mm.yy" class="w-full" />
          </div>
          <div class="form-group filtre-btn">
            <label>&nbsp;</label>
            <Button label="Rapor Getir" icon="pi pi-search" @click="getGelirGider" :loading="ggLoading" />
          </div>
        </div>

        <div v-if="ggData" class="rapor-sonuc" ref="ggKart">
          <div class="ozet-kartlar">
            <div class="ozet-kart gelir"><span>Toplam Gelir</span><strong>{{ formatCurrency(ggData.toplamGelir) }}</strong></div>
            <div class="ozet-kart gider"><span>Toplam Gider</span><strong>{{ formatCurrency(ggData.toplamGider) }}</strong></div>
            <div class="ozet-kart" :class="ggData.netKarZarar >= 0 ? 'kar' : 'zarar'">
              <span>Net Kar/Zarar</span><strong>{{ formatCurrency(ggData.netKarZarar) }}</strong>
            </div>
            <div class="rapor-aksiyonlar">
              <Button icon="pi pi-print" label="PDF" class="p-button-sm p-button-outlined" @click="yazdir(ggKart)" />
              <Button icon="pi pi-envelope" label="E-posta Gönder" class="p-button-sm p-button-outlined" @click="epostaGonder('Gelir/Gider Özeti', 'Gelir/Gider Raporu')" />
            </div>
          </div>

          <h3 style="margin-top:25px">Aylık Dağılım</h3>
          <DataTable :value="ggData.aylikDagilim" striped-rows>
            <Column field="ay" header="Ay"></Column>
            <Column field="net" header="Net Tutar">
              <template #body="s">
                <span :class="s.data.net >= 0 ? 'positive' : 'negative'">{{ formatCurrency(s.data.net) }}</span>
              </template>
            </Column>
          </DataTable>
        </div>
      </TabPanel>

      <TabPanel>
        <template #header>
          <div class="rapor-sekme-baslik">
            <i class="pi pi-star" :class="{ favori: raporFavori('kdv') }" @click.stop="raporFavoriDegistir('kdv', 2, 'KDV Raporu')"></i>
            KDV Raporu
          </div>
        </template>
        <div class="rapor-filtre">
          <div class="form-group">
            <label>Başlangıç</label>
            <DatePicker v-model="kdvBas" date-format="dd.mm.yy" class="w-full" />
          </div>
          <div class="form-group">
            <label>Bitiş</label>
            <DatePicker v-model="kdvBit" date-format="dd.mm.yy" class="w-full" />
          </div>
          <div class="form-group filtre-btn">
            <label>&nbsp;</label>
            <Button label="Rapor Getir" icon="pi pi-search" @click="getKdv" :loading="kdvLoading" />
          </div>
        </div>

        <div v-if="kdvData" class="rapor-sonuc">
          <div class="ozet-kartlar">
            <div class="ozet-kart gelir"><span>Çıkış KDV (Satış)</span><strong>{{ formatCurrency(kdvData.toplamKdvCikis) }}</strong></div>
            <div class="ozet-kart gider"><span>Giriş KDV (Alış)</span><strong>{{ formatCurrency(kdvData.toplamKdvGiris) }}</strong></div>
            <div class="ozet-kart" :class="kdvData.kdvFarki >= 0 ? 'kar' : 'zarar'">
              <span>KDV Farkı</span><strong>{{ formatCurrency(kdvData.kdvFarki) }}</strong>
            </div>
          </div>
        </div>
      </TabPanel>

      <TabPanel>
        <template #header>
          <div class="rapor-sekme-baslik">
            <i class="pi pi-star" :class="{ favori: raporFavori('yaslandirma') }" @click.stop="raporFavoriDegistir('yaslandirma', 3, 'Yaşlandırma')"></i>
            Yaşlandırma
          </div>
        </template>
        <div class="rapor-filtre">
          <Button label="Rapor Getir" icon="pi pi-search" @click="getYaslandirma" :loading="yasLoading" />
        </div>
        <div v-if="yasData" class="rapor-sonuc">
          <DataTable :value="yasData" striped-rows :rows="10" :paginator="true"
            paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport">
            <Column field="cariAd" header="Cari Hesap"></Column>
            <Column field="bakiye" header="Borç Bakiyesi" style="width:140px">
              <template #body="s"><span class="negative">{{ formatCurrency(s.data.bakiye) }}</span></template>
            </Column>
            <Column field="gun" header="Gün" style="width:80px"></Column>
            <Column field="aralik" header="Vade Aralığı" style="width:130px">
              <template #body="s">
                <span :class="['vade-badge', vadeClass(s.data.aralik)]">{{ s.data.aralik }}</span>
              </template>
            </Column>
          </DataTable>
          <Message v-if="yasData.length === 0" severity="info" text="Borçlu cari hesap bulunmamaktadır." />
        </div>
      </TabPanel>
    </TabView>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { useDovizStore } from '../stores/dovizStore.js'
import { raporAPI } from '../api/index.js'

const dovizStore = useDovizStore()
import { safeGet, safeSet } from '../utils/safeStorage.js'
const toast = useToast()

const cariHesapStore = useCariHesapStore()

const FAVORI_ANAHTAR = 'raspel_favori_raporlar'
const aktifSekme = ref(0)
const favoriRaporlar = ref(safeGet(FAVORI_ANAHTAR, []))

const raporFavori = (key) => favoriRaporlar.value.some(r => r.key === key)

const raporFavoriDegistir = (key, index, ad) => {
  const mevcut = favoriRaporlar.value.findIndex(r => r.key === key)
  if (mevcut > -1) {
    favoriRaporlar.value.splice(mevcut, 1)
  } else {
    favoriRaporlar.value.push({ key, index, ad })
  }
  safeSet(FAVORI_ANAHTAR, favoriRaporlar.value)
}

const ekstreKart = ref(null)
const ggKart = ref(null)

const yazdir = (hedef) => {
  const icerik = hedef.value?.outerHTML || ''
  const win = window.open('', '_blank', 'width=900,height=700')
  if (!win) { toast.add({ severity: 'error', summary: 'Hata', detail: 'Pencere engellendi', life: 4000 }); return }
  win.document.write(`<html><head><title>Rapor</title><style>
    body { font-family: Arial, sans-serif; padding: 24px; color: #1e293b; }
    table { width: 100%; border-collapse: collapse; margin: 12px 0; }
    th { background: #1976d2; color: white; padding: 8px; text-align: left; font-size: 12px; }
    td { padding: 8px; border-bottom: 1px solid #e2e8f0; font-size: 12px; }
    h3 { margin: 0 0 8px; }
    .rapor-aksiyonlar, .p-button, .p-paginator, .p-dropdown, .p-inputtext { display: none !important; }
    .positive { color: #16a34a; } .negative { color: #dc2626; }
    .ozet-kartlar { display: flex; gap: 12px; margin: 12px 0; flex-wrap: wrap; }
    .ozet-kart { border: 1px solid #e2e8f0; padding: 12px; border-radius: 8px; }
    .ozet-kart span { display: block; font-size: 11px; color: #64748b; }
    .ozet-kart strong { font-size: 16px; }
  </style></head><body>${icerik}</body></html>`)
  win.document.close()
  setTimeout(() => { win.focus(); win.print() }, 200)
}

const epostaGonder = (baslik, raporAdi) => {
  toast.add({ severity: 'info', summary: 'Paylaşım', detail: `${raporAdi} e-posta ile paylaşılacak.`, life: 4000 })
}

const ekstreCariId = ref(null)
const ekstreBas = ref(new Date(new Date().getFullYear(), 0, 1))
const ekstreBit = ref(new Date())
const ekstreData = ref(null)
const ekstreLoading = ref(false)

const ggBas = ref(new Date(new Date().getFullYear(), 0, 1))
const ggBit = ref(new Date())
const ggData = ref(null)
const ggLoading = ref(false)

const kdvBas = ref(new Date(new Date().getFullYear(), 0, 1))
const kdvBit = ref(new Date())
const kdvData = ref(null)
const kdvLoading = ref(false)

const yasData = ref(null)
const yasLoading = ref(false)

const formatDateForApi = (d) => {
  if (!d) return ''
  if (d instanceof Date) return d.toISOString().split('T')[0]
  if (typeof d === 'string') return d.split('T')[0]
  return String(d)
}

onMounted(async () => {
  await cariHesapStore.getAllCariHesaplar()
  getGelirGider()
  getKdv()
})

const getCariEkstre = async () => {
  if (!ekstreCariId.value) {
    toast.add({ severity: 'warn', summary: 'Uyarı', detail: 'Lütfen bir Cari Hesap seçiniz.', life: 3000 })
    return
  }
  ekstreLoading.value = true
  try {
    const r = await raporAPI.cariEkstre({
      cariHesapId: ekstreCariId.value,
      baslangic: formatDateForApi(ekstreBas.value),
      bitis: formatDateForApi(ekstreBit.value)
    })
    ekstreData.value = r.data
  } catch (err) { toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || err?.message || 'Cari ekstre yüklenirken hata oluştu', life: 5000 }); ekstreData.value = null }
  finally { ekstreLoading.value = false }
}

const getGelirGider = async () => {
  ggLoading.value = true
  try {
    const r = await raporAPI.gelirGider({
      baslangic: formatDateForApi(ggBas.value),
      bitis: formatDateForApi(ggBit.value)
    })
    ggData.value = r.data
  } catch (err) { toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || err?.message || 'Gelir/gider raporu yüklenirken hata oluştu', life: 5000 }); ggData.value = null }
  finally { ggLoading.value = false }
}

const getKdv = async () => {
  kdvLoading.value = true
  try {
    const r = await raporAPI.kdv({
      baslangic: formatDateForApi(kdvBas.value),
      bitis: formatDateForApi(kdvBit.value)
    })
    kdvData.value = r.data
  } catch (err) { toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || err?.message || 'KDV raporu yüklenirken hata oluştu', life: 5000 }); kdvData.value = null }
  finally { kdvLoading.value = false }
}

const getYaslandirma = async () => {
  yasLoading.value = true
  try { const r = await raporAPI.yaslandirma(); yasData.value = r.data }
  catch (err) { toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || err?.message || 'Yaşlandırma raporu yüklenirken hata oluştu', life: 5000 }); yasData.value = null }
  finally { yasLoading.value = false }
}

const vadeClass = (aralik) => {
  if (aralik.startsWith('0')) return 'risk-yok'
  if (aralik.startsWith('31')) return 'risk-az'
  if (aralik.startsWith('61')) return 'risk-orta'
  return 'risk-yuksek'
}

const formatCurrency = (v) => {
  const deger = v ?? 0
  const birim = dovizStore.aktifParaBirimi
  if (birim && birim !== 'TRY') {
    const cevrilen = dovizStore.convert(deger, 'TRY', birim)
    return dovizStore.formatPara(cevrilen, birim)
  }
  return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(deger)
}

const formatDate = (d) => d
  ? new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(d))
  : '-'
</script>

<style scoped>
.raporlar-container { padding: 20px; }
.raporlar-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}
.raporlar-header-bar h1 { margin: 0; font-size: 28px; font-weight: 700; letter-spacing: -0.5px; }
.rapor-doviz-secim {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  padding: 6px 14px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
}
.rapor-doviz-dropdown {
  width: 110px;
}
h1 { color: var(--text-primary); margin-bottom: 20px; font-size: 28px; font-weight: 700; letter-spacing: -0.5px; }
.rapor-filtre { display: flex; gap: 15px; align-items: flex-end; flex-wrap: wrap; margin-bottom: 20px; background: var(--bg-card); border: 1px solid var(--border); padding: 20px; border-radius: 12px; }
.form-group { min-width: 200px; }
.form-group label { display: block; margin-bottom: 6px; font-weight: bold; color: #333; font-size: 13px; }
.filtre-btn { min-width: auto; }
.rapor-sonuc { margin-top: 20px; }
.rapor-aksiyonlar { display: flex; gap: 8px; margin-top: 12px; }
.rapor-sekme-baslik { display: flex; align-items: center; gap: 6px; }
.rapor-sekme-baslik .pi-star { font-size: 13px; opacity: 0.35; cursor: pointer; }
.rapor-sekme-baslik .pi-star:hover { opacity: 0.8; color: #fbbf24; }
.rapor-sekme-baslik .pi-star.favori { opacity: 1; color: #fbbf24; }
.rapor-sekme-baslik .pi-star.favori:before { content: "\e936"; }
.favori-raporlar {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: 10px; padding: 10px 14px; margin-bottom: 16px;
}
.favori-baslik { font-size: 13px; font-weight: 600; color: var(--text-secondary); display: flex; align-items: center; gap: 6px; }
.rapor-bilgi { background: #f8f9fa; padding: 15px; border-radius: 8px; margin-bottom: 15px; }
.rapor-bilgi h3 { margin: 0 0 10px 0; color: #1976d2; }
.rapor-bilgi p { margin: 5px 0; }
.ozet-kartlar { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; }
.ozet-kart { background: var(--bg-card); padding: 20px; border-radius: 14px; border: 1px solid var(--border); text-align: center; }
.ozet-kart span { display: block; font-size: 13px; color: #666; margin-bottom: 8px; }
.ozet-kart strong { font-size: 22px; }
.ozet-kart.gelir strong { color: #4caf50; }
.ozet-kart.gider strong { color: #f44336; }
.ozet-kart.kar strong { color: #4caf50; }
.ozet-kart.zarar strong { color: #f44336; }
.badge { padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; }
.badge.tahsilat { background: #e8f5e9; color: #2e7d32; }
.badge.odeme { background: #ffebee; color: #c62828; }
.positive { color: #4caf50; font-weight: bold; }
.negative { color: #f44336; font-weight: bold; }
.vade-badge { padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; }
.risk-yok { background: #e8f5e9; color: #2e7d32; }
.risk-az { background: #fff3e0; color: #e65100; }
.risk-orta { background: #ffebee; color: #c62828; }
.risk-yuksek { background: #fce4ec; color: #880e4f; }
.w-full { width: 100% !important; }
</style>
