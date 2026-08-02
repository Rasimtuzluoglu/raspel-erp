<template>
  <div class="muhasebe-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">Genel Muhasebe</h1>
      <Button
        v-if="aktifSekme === 0"
        label="Yeni Hesap" icon="pi pi-plus" @click="hesapDialogAc()" />
      <Button
        v-else-if="aktifSekme === 1"
        label="Yeni Yevmiye Fişi" icon="pi pi-plus" @click="fisDialogAc()" />
    </div>

    <IlkZiyaretIpuclari
      anahtar="muhasebe"
      baslik="Genel Muhasebe"
      metin="Hesap planını düzenleyin, dengeli yevmiye fişleri (borç = alacak) kaydedin ve Mizan ile Defter-i Kebir raporlarını görüntüleyin."
    />

    <TabView v-model:activeIndex="aktifSekme" class="muhasebe-tabs">
      <!-- HESAP PLANI -->
      <TabPanel header="Hesap Planı">
        <AppDataTable
          :value="hesaplar"
          :loading="yukleniyor"
          aramaAktif
          aramaPlaceholder="Hesap ara..."
          gorunumAnahtari="muhasebe_hesap_plani"
        >
          <Column field="kod" header="Kod" sortable style="width:110px" />
          <Column field="ad" header="Hesap Adı" sortable />
          <Column field="tip" header="Tip">
            <template #body="{ data }"><Tag :value="tipEtiketi(data.tip)" :severity="tipSeverity(data.tip)" /></template>
          </Column>
          <Column field="grup" header="Grup" />
          <Column header="İşlem" style="width:60px">
            <template #body="{ data }">
              <SatirEylemleri
                :gorunur="{ duzenle: true, cogalt: true, sil: true }"
                @duzenle="hesapDialogAc(data)"
                @cogalt="hesapCogalt(data)"
                @sil="hesapSil(data)"
              />
            </template>
          </Column>
        </AppDataTable>
      </TabPanel>

      <!-- YEVMIYE FİŞLERİ -->
      <TabPanel header="Yevmiye Fişleri">
        <div class="filtre-bar">
          <DatePicker v-model="filtreBaslangic" dateFormat="dd/mm/yy" placeholder="Başlangıç" />
          <DatePicker v-model="filtreBitis" dateFormat="dd/mm/yy" placeholder="Bitiş" />
          <Button icon="pi pi-refresh" label="Yenile" class="p-button-sm p-button-text" @click="fisleriYukle" />
        </div>
        <DataTable :value="fisler" stripedRows :loading="fisYukleniyor">
          <Column field="fisNo" header="Fiş No" sortable style="width:150px" />
          <Column field="tarih" header="Tarih" sortable>
            <template #body="{ data }">{{ formatDate(data.tarih) }}</template>
          </Column>
          <Column field="aciklama" header="Açıklama" />
          <Column field="toplamBorc" header="Borç">
            <template #body="{ data }">{{ formatCurrency(data.toplamBorc) }}</template>
          </Column>
          <Column field="toplamAlacak" header="Alacak">
            <template #body="{ data }">{{ formatCurrency(data.toplamAlacak) }}</template>
          </Column>
          <Column field="durum" header="Durum">
            <template #body="{ data }"><Tag :value="data.durum" :severity="data.durum === 'ONAYLANDI' ? 'success' : data.durum === 'IPTAL' ? 'danger' : 'info'" /></template>
          </Column>
          <Column header="İşlem" style="width:140px">
            <template #body="{ data }">
              <Button icon="pi pi-eye" class="p-button-rounded p-button-text" @click="fisDetayAc(data)" title="Görüntüle" />
              <Button v-if="data.durum !== 'IPTAL'" icon="pi pi-ban" class="p-button-rounded p-button-text p-button-danger" @click="fisIptal(data)" title="İptal Et" />
            </template>
          </Column>
        </DataTable>
      </TabPanel>

      <!-- MİZAN -->
      <TabPanel header="Mizan">
        <div class="filtre-bar">
          <DatePicker v-model="mizanBaslangic" dateFormat="dd/mm/yy" placeholder="Başlangıç" />
          <DatePicker v-model="mizanBitis" dateFormat="dd/mm/yy" placeholder="Bitiş" />
          <Button icon="pi pi-refresh" label="Hesapla" class="p-button-sm" @click="mizanYukle" />
        </div>
        <DataTable :value="mizan" stripedRows :loading="mizanYukleniyor">
          <Column field="hesapKodu" header="Hesap Kodu" sortable style="width:110px" />
          <Column field="hesapAdi" header="Hesap Adı" />
          <Column field="borc" header="Borç">
            <template #body="{ data }">{{ formatCurrency(data.borc) }}</template>
          </Column>
          <Column field="alacak" header="Alacak">
            <template #body="{ data }">{{ formatCurrency(data.alacak) }}</template>
          </Column>
          <Column field="borcBakiye" header="Borç Bakiye">
            <template #body="{ data }"><span class="pozitif">{{ formatCurrency(data.borcBakiye) }}</span></template>
          </Column>
          <Column field="alacakBakiye" header="Alacak Bakiye">
            <template #body="{ data }"><span class="negatif">{{ formatCurrency(data.alacakBakiye) }}</span></template>
          </Column>
        </DataTable>
      </TabPanel>

      <!-- DEFTER-İ KEBİR -->
      <TabPanel header="Defter-i Kebir">
        <div class="filtre-bar">
          <Select v-model="kebirHesap" :options="hesapSecenekleri" optionLabel="ad" optionValue="kod" placeholder="Hesap seçin" class="kebir-select" showClear />
          <DatePicker v-model="kebirBaslangic" dateFormat="dd/mm/yy" placeholder="Başlangıç" />
          <DatePicker v-model="kebirBitis" dateFormat="dd/mm/yy" placeholder="Bitiş" />
          <Button icon="pi pi-refresh" label="Listele" class="p-button-sm" @click="kebirYukle" />
        </div>
        <DataTable :value="kebir" stripedRows :loading="kebirYukleniyor">
          <Column field="tarih" header="Tarih">
            <template #body="{ data }">{{ formatDate(data.tarih) }}</template>
          </Column>
          <Column field="fisNo" header="Fiş No" />
          <Column field="aciklama" header="Açıklama" />
          <Column field="borc" header="Borç">
            <template #body="{ data }">{{ formatCurrency(data.borc) }}</template>
          </Column>
          <Column field="alacak" header="Alacak">
            <template #body="{ data }">{{ formatCurrency(data.alacak) }}</template>
          </Column>
          <Column field="bakiye" header="Bakiye">
            <template #body="{ data }">{{ formatCurrency(data.bakiye) }}</template>
          </Column>
        </DataTable>
      </TabPanel>
    </TabView>

    <!-- HESAP DIALOG -->
    <Dialog v-model:visible="hesapDialog" :header="hesapDialogBaslik" modal :style="{ width: '480px' }">
      <div class="form-grid">
        <div class="field">
          <label class="zorunlu">Hesap Kodu</label>
          <InputText v-model="hesapForm.kod" class="w-full" placeholder="Ör: 100, 120, 320" :class="{ 'p-invalid': hesapFormHatali.kod }" />
          <small v-if="hesapFormHatali.kod" class="hata-mesaj">Hesap kodu zorunludur</small>
        </div>
        <div class="field">
          <label class="zorunlu">Hesap Adı</label>
          <InputText v-model="hesapForm.ad" class="w-full" :class="{ 'p-invalid': hesapFormHatali.ad }" />
          <small v-if="hesapFormHatali.ad" class="hata-mesaj">Hesap adı zorunludur</small>
        </div>
        <div class="field">
          <label>Tip *</label>
          <Select v-model="hesapForm.tip" :options="['AKTIF','PASIF','GELIR','GIDER']" class="w-full" />
        </div>
        <div class="field"><label>Grup</label><InputText v-model="hesapForm.grup" class="w-full" /></div>
      </div>
      <template #footer>
        <Button label="İptal" icon="pi pi-times" class="p-button-text" @click="hesapDialog = false" />
        <Button label="Kaydet" icon="pi pi-check" @click="hesapKaydet" :loading="kaydediliyor" />
      </template>
    </Dialog>

    <!-- YEVMIYE FİŞİ DIALOG -->
    <Dialog v-model:visible="fisDialog" header="Yeni Yevmiye Fişi" modal :style="{ width: '640px' }">
      <div class="form-grid">
        <div class="field"><label>Tarih *</label><DatePicker v-model="fisForm.tarih" dateFormat="dd/mm/yy" class="w-full" /></div>
        <div class="field"><label>Açıklama</label><InputText v-model="fisForm.aciklama" class="w-full" /></div>
      </div>
      <div class="fis-kalemler">
        <div class="fis-kalem-baslik">
          <span>Hesap</span><span>Borç</span><span>Alacak</span><span></span>
        </div>
        <div v-for="(k, i) in fisForm.kalemler" :key="i" class="fis-kalem">
          <Select v-model="k.hesapKodu" :options="hesapSecenekleri" optionLabel="ad" optionValue="kod" placeholder="Hesap seçin" class="kalem-hesap" />
          <InputNumber v-model="k.borc" mode="currency" currency="TRY" class="kalem-tutar" />
          <InputNumber v-model="k.alacak" mode="currency" currency="TRY" class="kalem-tutar" />
          <Button icon="pi pi-times" class="p-button-rounded p-button-text p-button-danger" @click="fisForm.kalemler.splice(i, 1)" />
        </div>
        <Button label="Kalem Ekle" icon="pi pi-plus" class="p-button-sm p-button-text" @click="kalemEkle" />
      </div>
      <div class="fis-toplam">
        Toplam Borç: <strong>{{ formatCurrency(fisToplamBorc) }}</strong>
        &nbsp;|&nbsp; Toplam Alacak: <strong>{{ formatCurrency(fisToplamAlacak) }}</strong>
      </div>
      <template #footer>
        <Button label="İptal" icon="pi pi-times" class="p-button-text" @click="fisDialog = false" />
        <Button label="Fişi Kaydet" icon="pi pi-check" @click="fisKaydet" :loading="kaydediliyor" />
      </template>
    </Dialog>

    <!-- FİŞ DETAY DIALOG -->
    <Dialog v-model:visible="fisDetayDialog" header="Fiş Detayı" modal :style="{ width: '560px' }">
      <div class="fis-detay-baslik">
        <strong>{{ fisDetay?.fisNo }}</strong> — {{ formatDate(fisDetay?.tarih) }}
        <Tag :value="fisDetay?.durum" :severity="fisDetay?.durum === 'ONAYLANDI' ? 'success' : fisDetay?.durum === 'IPTAL' ? 'danger' : 'info'" />
      </div>
      <p v-if="fisDetay?.aciklama" class="fis-detay-aciklama">{{ fisDetay.aciklama }}</p>
      <DataTable :value="fisDetay?.kalemler || []" stripedRows>
        <Column field="hesapKodu" header="Hesap" style="width:90px" />
        <Column field="hesapAdi" header="Hesap Adı" />
        <Column field="borc" header="Borç"><template #body="{ data }">{{ formatCurrency(data.borc) }}</template></Column>
        <Column field="alacak" header="Alacak"><template #body="{ data }">{{ formatCurrency(data.alacak) }}</template></Column>
      </DataTable>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import { muhasebeAPI } from '../api/index.js'
import SatirEylemleri from '../components/SatirEylemleri.vue'
import IlkZiyaretIpuclari from '../components/IlkZiyaretIpuclari.vue'
import { useGeriAl } from '../composables/useGeriAl.js'

const toast = useToast()
const confirm = useConfirm()
const { silVeGeriAl } = useGeriAl()

const aktifSekme = ref(0)
const yukleniyor = ref(false)
const kaydediliyor = ref(false)

// Hesap planı
const hesaplar = ref([])
const hesapDialog = ref(false)
const hesapDuzenleme = ref(false)
const hesapForm = ref({ kod: '', ad: '', tip: 'AKTIF', grup: '' })
const hesapFormHatali = ref({ kod: false, ad: false })
const hesapDialogBaslik = computed(() => hesapDuzenleme.value ? 'Hesap Düzenle' : 'Yeni Hesap')

// Fişler
const fisler = ref([])
const fisYukleniyor = ref(false)
const fisDialog = ref(false)
const fisDetayDialog = ref(false)
const fisDetay = ref(null)
const filtreBaslangic = ref(null)
const filtreBitis = ref(null)
const fisForm = ref({ tarih: new Date(), aciklama: '', kalemler: [{ hesapKodu: null, borc: 0, alacak: 0 }] })

// Mizan
const mizan = ref([])
const mizanYukleniyor = ref(false)
const mizanBaslangic = ref(null)
const mizanBitis = ref(null)

// Defter-i kebir
const kebir = ref([])
const kebirYukleniyor = ref(false)
const kebirHesap = ref(null)
const kebirBaslangic = ref(null)
const kebirBitis = ref(null)

const hesapSecenekleri = computed(() => hesaplar.value.map(h => ({ ad: `${h.kod} - ${h.ad}`, kod: h.kod })))
const fisToplamBorc = computed(() => (fisForm.value.kalemler || []).reduce((t, k) => t + (Number(k.borc) || 0), 0))
const fisToplamAlacak = computed(() => (fisForm.value.kalemler || []).reduce((t, k) => t + (Number(k.alacak) || 0), 0))

const formatCurrency = (v) => v == null ? '0,00 ₺' : new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v)
const formatDate = (d) => d ? new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(d)) : '-'

const tipEtiketi = (t) => ({ AKTIF: 'Aktif', PASIF: 'Pasif', GELIR: 'Gelir', GIDER: 'Gider' }[t] || t)
const tipSeverity = (t) => ({ AKTIF: 'info', PASIF: 'warning', GELIR: 'success', GIDER: 'danger' }[t] || 'secondary')

const tarihParam = (d) => d ? d.toISOString?.().split('T')[0] ?? d : null

onMounted(() => {
  hesaplariYukle()
  fisleriYukle()
})

const hesaplariYukle = async () => {
  yukleniyor.value = true
  try { const r = await muhasebeAPI.getHesapPlani(); hesaplar.value = r.data || [] } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Hesap planı yüklenemedi', life: 5000 })
  }
  yukleniyor.value = false
}

const hesapDialogAc = (data) => {
  hesapDuzenleme.value = !!data
  hesapFormHatali.value = { kod: false, ad: false }
  hesapForm.value = data ? { ...data } : { kod: '', ad: '', tip: 'AKTIF', grup: '' }
  hesapDialog.value = true
}

const hesapCogalt = (data) => {
  hesapFormHatali.value = { kod: false, ad: false }
  hesapForm.value = { ...data, id: null, kod: '', ad: data.ad + ' (Kopya)' }
  hesapDuzenleme.value = false
  hesapDialog.value = true
}

const hesapKaydet = async () => {
  const hatali = { kod: !hesapForm.value.kod?.trim(), ad: !hesapForm.value.ad?.trim() }
  hesapFormHatali.value = hatali
  if (hatali.kod || hatali.ad) {
    toast.add({ severity: 'warn', summary: 'Uyarı', detail: 'Kod ve ad zorunludur', life: 3000 }); return
  }
  kaydediliyor.value = true
  try {
    if (hesapDuzenleme.value) await muhasebeAPI.hesapGuncelle(hesapForm.value.id, hesapForm.value)
    else await muhasebeAPI.hesapOlustur(hesapForm.value)
    toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Hesap kaydedildi', life: 3000 })
    hesapDialog.value = false
    hesaplariYukle()
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'İşlem başarısız', life: 5000 })
  }
  kaydediliyor.value = false
}

const hesapSil = (data) => {
  confirm.require({
    message: `"${data.kod} - ${data.ad}" hesabını silmek istediğinize emin misiniz?`,
    header: 'Silme Onayı', icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet, Sil', rejectLabel: 'İptal',
    accept: async () => {
      try {
        await muhasebeAPI.hesapSil(data.id)
        hesaplar.value = hesaplar.value.filter(h => h.id !== data.id)
        silVeGeriAl({
          veri: data,
          metin: `"${data.kod}" hesabı silindi`,
          geriYukle: async (kayit) => {
            await muhasebeAPI.hesapOlustur({ kod: kayit.kod, ad: kayit.ad, tip: kayit.tip, grup: kayit.grup })
            hesaplariYukle()
          }
        })
        toast.add({ severity: 'success', summary: 'Silindi', detail: 'Hesap silindi', life: 3000 })
      } catch (err) {
        toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Silme başarısız', life: 5000 })
      }
    }
  })
}

const fisleriYukle = async () => {
  fisYukleniyor.value = true
  try {
    const params = {}
    if (filtreBaslangic.value) params.baslangic = tarihParam(filtreBaslangic.value)
    if (filtreBitis.value) params.bitis = tarihParam(filtreBitis.value)
    const r = await muhasebeAPI.getFisler(params)
    fisler.value = r.data || []
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Fişler yüklenemedi', life: 5000 })
  }
  fisYukleniyor.value = false
}

const kalemEkle = () => fisForm.value.kalemler.push({ hesapKodu: null, borc: 0, alacak: 0 })

const fisDialogAc = () => {
  fisForm.value = { tarih: new Date(), aciklama: '', kalemler: [{ hesapKodu: null, borc: 0, alacak: 0 }] }
  fisDialog.value = true
}

const fisKaydet = async () => {
  if (!fisForm.value.kalemler?.length) {
    toast.add({ severity: 'warn', summary: 'Uyarı', detail: 'En az bir kalem ekleyin', life: 3000 }); return
  }
  if (fisToplamBorc.value !== fisToplamAlacak.value) {
    toast.add({ severity: 'error', summary: 'Fiş denk değil', detail: 'Toplam borç ile alacak eşit olmalı', life: 5000 }); return
  }
  kaydediliyor.value = true
  try {
    const payload = {
      tarih: tarihParam(fisForm.value.tarih),
      aciklama: fisForm.value.aciklama,
      kalemler: fisForm.value.kalemler.map(k => ({
        hesapKodu: k.hesapKodu,
        hesapAdi: hesaplar.value.find(h => h.kod === k.hesapKodu)?.ad,
        borc: Number(k.borc) || 0,
        alacak: Number(k.alacak) || 0
      }))
    }
    await muhasebeAPI.fisOlustur(payload)
    toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Yevmiye fişi oluşturuldu', life: 3000 })
    fisDialog.value = false
    fisleriYukle()
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Fiş kaydedilemedi', life: 5000 })
  }
  kaydediliyor.value = false
}

const fisDetayAc = async (data) => {
  try {
    const r = await muhasebeAPI.getFis(data.id)
    fisDetay.value = r.data
    fisDetayDialog.value = true
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Fiş detayı alınamadı', life: 5000 })
  }
}

const fisIptal = (data) => {
  confirm.require({
    message: `"${data.fisNo}" fişini iptal etmek istediğinize emin misiniz?`,
    header: 'İptal Onayı', icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet, İptal Et', rejectLabel: 'Vazgeç',
    accept: async () => {
      try {
        await muhasebeAPI.fisIptal(data.id)
        toast.add({ severity: 'success', summary: 'İptal Edildi', detail: 'Fiş iptal edildi', life: 3000 })
        fisleriYukle()
      } catch (err) {
        toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'İptal başarısız', life: 5000 })
      }
    }
  })
}

const mizanYukle = async () => {
  mizanYukleniyor.value = true
  try {
    const params = {}
    if (mizanBaslangic.value) params.baslangic = tarihParam(mizanBaslangic.value)
    if (mizanBitis.value) params.bitis = tarihParam(mizanBitis.value)
    const r = await muhasebeAPI.getMizan(params)
    mizan.value = r.data || []
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Mizan alınamadı', life: 5000 })
  }
  mizanYukleniyor.value = false
}

const kebirYukle = async () => {
  kebirYukleniyor.value = true
  try {
    const params = { hesapKodu: kebirHesap.value || undefined }
    if (kebirBaslangic.value) params.baslangic = tarihParam(kebirBaslangic.value)
    if (kebirBitis.value) params.bitis = tarihParam(kebirBitis.value)
    const r = await muhasebeAPI.getDefteriKebir(params)
    kebir.value = r.data || []
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err?.response?.data?.message || 'Defter-i kebir alınamadı', life: 5000 })
  }
  kebirYukleniyor.value = false
}
</script>

<style scoped>
.muhasebe-container { padding: 0; }
.sayfa-baslik { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.filtre-bar { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; }
.form-grid { display: flex; flex-direction: column; gap: 14px; }
.field { display: flex; flex-direction: column; gap: 6px; }
.field label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.w-full { width: 100%; }
.zorunlu::after { content: ' *'; color: #ef4444; }
.hata-mesaj { color: #ef4444; font-size: 12px; }
:deep(.p-invalid) { border-color: #ef4444 !important; }
.fis-kalemler { margin-top: 16px; display: flex; flex-direction: column; gap: 8px; }
.fis-kalem-baslik, .fis-kalem { display: grid; grid-template-columns: 1fr 140px 140px 40px; gap: 8px; align-items: center; }
.fis-kalem-baslik span { font-size: 12px; font-weight: 600; color: var(--text-muted); }
.fis-toplam { margin-top: 14px; padding: 10px 14px; background: rgba(59,130,246,0.08); border-radius: 8px; font-size: 13px; }
.fis-detay-baslik { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; font-size: 14px; }
.fis-detay-aciklama { color: var(--text-secondary); margin-bottom: 12px; }
.kebir-select { min-width: 240px; }
.pozitif { color: #10b981; font-weight: 600; }
.negatif { color: #ef4444; font-weight: 600; }
</style>
