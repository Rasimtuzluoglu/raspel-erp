<template>
  <div class="faturalar-container">
    <h1>Fatura Yönetimi</h1>

    <Toolbar class="toolbar">
      <template #start>
        <Button label="Yeni Fatura" icon="pi pi-plus" @click="openCreateDialog" class="p-button-success" />
      </template>
      <template #end>
        <Button label="Excel" icon="pi pi-file-excel" class="p-button-sm p-button-outlined" @click="excelIndir" />
      </template>
    </Toolbar>

    <div class="loading" v-if="loading"><p><i class="pi pi-spin pi-spinner"></i> Yükleniyor...</p></div>

    <div class="table-container" v-if="!loading">
      <DataTable
        :value="faturaStore.faturalar"
        responsive-layout="scroll" striped-rows
        :rows="10" :paginator="true"
        paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown"
        :rows-per-page-options="[10,20,50]"
        current-page-report-template="{first} - {last} ({totalRecords} kayıt)"
      >
        <Column field="faturaNumarasi" header="Fatura No" style="width:160px"></Column>
        <Column field="tarih" header="Tarih" style="width:110px">
          <template #body="s">{{ formatDate(s.data.tarih) }}</template>
        </Column>
        <Column field="tur" header="Tür" style="width:90px">
          <template #body="s">
            <span :class="['badge', s.data.tur === 'SATIS' ? 'satis' : 'alis']">
              {{ s.data.tur === 'SATIS' ? 'Satış' : 'Alış' }}
            </span>
          </template>
        </Column>
        <Column field="cariHesapAd" header="Cari Hesap" style="width:180px">
          <template #body="s">{{ s.data.cariHesapAd || '-' }}</template>
        </Column>
        <Column field="genelToplam" header="Toplam" style="width:130px">
          <template #body="s">{{ formatCurrency(s.data.genelToplam) }}</template>
        </Column>
        <Column field="durum" header="Durum" style="width:110px">
          <template #body="s">
            <span :class="['durum-badge', (s.data.durum || '').toLowerCase()]">
              {{ durumLabel(s.data.durum) }}
            </span>
          </template>
        </Column>
        <Column header="İşlemler" style="width:310px">
          <template #body="s">
            <Button icon="pi pi-eye" class="p-button-rounded p-button-sm p-button-info"
              @click="viewFatura(s.data.id)" title="Görüntüle" />
            <Button icon="pi pi-download" class="p-button-rounded p-button-sm p-button-help"
              @click="pdfIndir(s.data)" title="PDF İndir" />
            <Button icon="pi pi-whatsapp" class="p-button-rounded p-button-sm p-button-success"
              @click="whatsappGonder(s.data)" title="WhatsApp İle Gönder" style="background:#25D366;border-color:#25D366" />
            <Button icon="pi pi-pencil" class="p-button-rounded p-button-sm p-button-warning"
              @click="editFatura(s.data)" v-if="s.data.durum === 'TASLAK'" title="Düzenle" />
            <Button v-if="s.data.durum === 'TASLAK'" icon="pi pi-check" class="p-button-rounded p-button-sm p-button-success"
              @click="confirmKes(s.data.id)" title="Kes" />
            <Button v-if="s.data.durum !== 'IPTAL'" icon="pi pi-ban" class="p-button-rounded p-button-sm p-button-danger"
              @click="confirmIptal(s.data.id)" title="İptal" />
          </template>
        </Column>
      </DataTable>
      <EmptyState v-if="faturaStore.faturalar.length === 0" message="Henüz fatura yok" sub-message="İlk faturanızı oluşturarak satış sürecinizi başlatın." icon="pi pi-file" action-label="İlk Faturayı Oluştur" action-icon="pi pi-plus" @action="openCreateDialog" />    </div>

    <Dialog v-model:visible="showDialog" :header="dialogBaslik" :modal="true" style="width:750px" :closable="false">
      <div class="form-grid">
        <div class="form-group">
          <label>Cari Hesap</label>
          <Dropdown v-model="form.cariHesapId" :options="cariHesapStore.cariHesaplar"
            option-label="ad" option-value="id" placeholder="Seçiniz (isteğe bağlı)" class="w-full" />
        </div>
        <div class="form-group">
          <label>Fatura Türü *</label>
          <Dropdown v-model="form.tur" :options="turSecenekler" optionLabel="label" optionValue="value" placeholder="Seçiniz" class="w-full" />
        </div>
        <div class="form-group">
          <label>Tarih *</label>
          <DatePicker v-model="form.tarih" date-format="dd.mm.yy" class="w-full" />
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>Para Birimi</label>
            <Dropdown v-model="form.paraBirimi" :options="['TRY','USD','EUR','GBP','SAR','GAU']" class="w-full" placeholder="TRY (₺)" />
          </div>
          <div class="form-group" v-if="form.paraBirimi && form.paraBirimi !== 'TRY'">
            <label>Kur Bilgisi (TL Karşılığı)</label>
            <div class="kur-bilgi-box">
              1 {{ form.paraBirimi }} = {{ dovizStore.formatPara(dovizStore.getKur(form.paraBirimi).satisFiyati || dovizStore.getKur(form.paraBirimi).satisKuru, 'TRY') }}
            </div>
          </div>
        </div>
        <div class="form-group">
          <label>Açıklama</label>
          <Textarea v-model="form.aciklama" rows="2" placeholder="İsteğe bağlı" class="w-full" />
        </div>
      </div>

      <div class="urun-ekleme">
        <div class="form-row" style="display:flex;gap:10px;align-items:flex-end">
          <div class="form-group" style="flex:3;margin:0">
            <label>Ürün Seç (Stoktan Otomatik Ekle)</label>
            <Dropdown v-model="urunSecimi" :options="stokStore.stoklar" filter option-label="ad" option-value="id"
              placeholder="Ürün ara ve seç..." class="w-full" @change="urunSecildi">
              <template #option="s">
                <div style="display:flex;align-items:center;gap:10px">
                  <span style="flex:1;color:#f1f5f9">{{ s.option.ad }}</span>
                  <span style="color:#4ade80;font-size:12px;font-weight:600">{{ s.option.miktar }} {{ s.option.birim || 'Adet' }}</span>
                  <span style="color:#94a3b8;font-size:12px">{{ formatCurrency(s.option.fiyat) }}</span>
                </div>
              </template>
            </Dropdown>
          </div>
          <div class="form-group" style="flex:1;margin:0">
            <label>Miktar</label>
            <InputNumber v-model="urunAdet" :min="1" class="w-full" />
          </div>
          <Button icon="pi pi-plus" class="p-button-success" style="margin-bottom:2px" @click="urunEkleKalem" :disabled="!urunSecimi || !urunAdet" />
        </div>
      </div>

      <h3 style="margin:20px 0 10px">Fatura Kalemleri</h3>
      <DataTable :value="form.kalemler" striped-rows>
        <Column header="#" style="width:40px">
          <template #body="s">{{ s.index + 1 }}</template>
        </Column>
        <Column header="Açıklama *">
          <template #body="s">
            <InputText v-model="s.data.aciklama" placeholder="Kalem açıklaması" class="w-full" />
          </template>
        </Column>
        <Column header="Adet *" style="width:90px">
          <template #body="s">
            <InputNumber v-model="s.data.adet" :min="1" class="w-full" />
          </template>
        </Column>
        <Column header="Birim Fiyat *" style="width:130px">
          <template #body="s">
            <InputNumber v-model="s.data.birimFiyat" :min="0" :min-fraction-digits="2" :max-fraction-digits="2" class="w-full" />
          </template>
        </Column>
        <Column header="İskonto %" style="width:100px">
          <template #body="s">
            <InputNumber v-model="s.data.iskontoOrani" :min="0" :max="100" :min-fraction-digits="0" class="w-full" />
          </template>
        </Column>
        <Column header="KDV %" style="width:80px">
          <template #body="s">
            <Dropdown v-model="s.data.kdvOrani" :options="[0,10,20]" class="w-full" />
          </template>
        </Column>
        <Column header="Tutar" style="width:120px">
          <template #body="s">{{ formatCurrency(kalemTutar(s.data)) }}</template>
        </Column>
        <Column header="" style="width:50px">
          <template #body="s">
            <Button icon="pi pi-trash" class="p-button-rounded p-button-danger p-button-sm"
              @click="removeKalem(s.index)" />
          </template>
        </Column>
      </DataTable>
      <div style="margin-top:10px">
        <Button label="+ Kalem Ekle" icon="pi pi-plus" @click="addKalem" class="p-button-sm p-button-outlined" />
      </div>

      <div class="summary-box">
        <div class="summary-row"><span>Ara Toplam:</span><span>{{ formatCurrency(araToplam) }}</span></div>
        <div class="summary-row"><span>KDV:</span><span>{{ formatCurrency(kdvToplam) }}</span></div>
        <div class="summary-row total"><span>Genel Toplam:</span><span>{{ formatCurrency(genelToplam) }}</span></div>
      </div>

      <template #footer>
        <Button label="İptal" icon="pi pi-times" @click="closeDialog" class="p-button-text" />
        <Button :label="editingId ? 'Faturayı Güncelle' : 'Faturayı Oluştur'" icon="pi pi-check" @click="saveFatura" :loading="saving" />
      </template>
    </Dialog>

    <Message v-if="faturaStore.error" severity="error" :text="faturaStore.error" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import { useFaturaStore } from '../stores/faturaStore.js'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { useStokStore } from '../stores/stokStore.js'
import { useDovizStore } from '../stores/dovizStore.js'

const dovizStore = useDovizStore()
import { excelAPI, pdfAPI } from '../api/index.js'
import { useKisayollar } from '../composables/useKisayollar.js'
import { useTaslakKayit } from '../composables/useTaslakKayit.js'
import { useFormKorumasi } from '../composables/useFormKorumasi.js'

const router = useRouter()
const toast = useToast()
const confirm = useConfirm()
const faturaStore = useFaturaStore()
const cariHesapStore = useCariHesapStore()
const stokStore = useStokStore()

useKisayollar({
  yeni: () => openCreateDialog(),
  iptal: () => { showDialog.value = false },
  kaydet: () => saveFatura()
})

const showDialog = ref(false)
const loading = ref(false)
const saving = ref(false)
const editingId = ref(null)

const turSecenekler = [{label:'Satış',value:'SATIS'},{label:'Alış',value:'ALIS'}]

const form = ref({
  cariHesapId: null,
  tur: '',
  tarih: new Date(),
  aciklama: '',
  kalemler: []
})

const urunSecimi = ref(null)
const urunAdet = ref(1)

const dialogBaslik = computed(() => editingId.value ? 'Fatura Düzenle' : 'Yeni Fatura Oluştur')

const { temizle: taslakTemizle } = useTaslakKayit('fatura', form, {
  onRestore: () => {
    toast.add({ severity: 'info', summary: 'Taslak Geri Yüklendi', detail: 'Kesilmemiş faturanız geri yüklendi.', life: 5000 })
  }
})

const { temizle: formTemizle } = useFormKorumasi(form)

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([
      faturaStore.getAllFaturalar(),
      cariHesapStore.getAllCariHesaplar(),
      stokStore.getAll()
    ])
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: 'Veriler yüklenirken hata oluştu', life: 5000 })
  } finally {
    loading.value = false
  }
})

const addKalem = () => {
  form.value.kalemler.push({ aciklama: '', adet: 1, birimFiyat: 0, iskontoOrani: 0, kdvOrani: 20 })
}

const urunSecildi = () => {
  if (!urunSecimi.value) return
  const u = stokStore.stoklar.find(s => s.id === urunSecimi.value)
  if (u) urunAdet.value = 1
}

const urunEkleKalem = () => {
  if (!urunSecimi.value || !urunAdet.value) return
  const u = stokStore.stoklar.find(s => s.id === urunSecimi.value)
  if (!u) return
  form.value.kalemler.push({
    aciklama: u.ad,
    adet: urunAdet.value,
    birimFiyat: u.fiyat,
    iskontoOrani: 0,
    kdvOrani: 20,
    stokId: u.id
  })
  urunSecimi.value = null
  urunAdet.value = 1
}

const removeKalem = (index) => {
  form.value.kalemler.splice(index, 1)
}

const kalemTutar = (kalem) => {
  const brf = kalem.birimFiyat || 0
  const adt = kalem.adet || 0
  const iskontoOran = (kalem.iskontoOrani || 0) / 100
  const net = (brf * adt) * (1 - iskontoOran)
  const kdvOran = (kalem.kdvOrani || 0) / 100
  return net + (net * kdvOran)
}

const araToplam = computed(() => {
  return form.value.kalemler.reduce((t, k) => {
    const brf = k.birimFiyat || 0
    const adt = k.adet || 0
    const iskontoOran = (k.iskontoOrani || 0) / 100
    return t + (brf * adt * (1 - iskontoOran))
  }, 0)
})

const kdvToplam = computed(() => {
  return form.value.kalemler.reduce((t, k) => {
    const brf = k.birimFiyat || 0
    const adt = k.adet || 0
    const iskontoOran = (k.iskontoOrani || 0) / 100
    const net = brf * adt * (1 - iskontoOran)
    return t + (net * ((k.kdvOrani || 0) / 100))
  }, 0)
})

const genelToplam = computed(() => araToplam.value + kdvToplam.value)

const whatsappGonder = (fatura) => {
  const cariAd = fatura.cariHesapAd || 'Müşterimiz'
  const tutar = fatura.genelToplam ? fatura.genelToplam.toLocaleString('tr-TR', { minimumFractionDigits: 2 }) + ' TL' : ''
  const mesaj = `Sayın ${cariAd},\n${fatura.faturaNumarasi || 'Fatura'} numaralı, ${tutar} tutarındaki faturanız düzenlenmiştir. Bilginize sunarız.\nRaspel ERP`
  const url = `https://api.whatsapp.com/send?text=${encodeURIComponent(mesaj)}`
  window.open(url, '_blank')
}

const openCreateDialog = () => {
  editingId.value = null
  form.value = {
    cariHesapId: null,
    tur: '',
    tarih: new Date(),
    aciklama: '',
    kalemler: [{ aciklama: '', adet: 1, birimFiyat: 0, kdvOrani: 20 }]
  }
  formTemizle()
  showDialog.value = true
}

const editFatura = (fatura) => {
  editingId.value = fatura.id
  form.value = {
    cariHesapId: fatura.cariHesapId,
    tur: fatura.tur,
    tarih: new Date(fatura.tarih),
    aciklama: fatura.aciklama || '',
    kalemler: fatura.kalemler.map(k => ({
      id: k.id,
      aciklama: k.aciklama,
      adet: k.adet,
      birimFiyat: k.birimFiyat,
      iskontoOrani: k.iskontoOrani || 0,
      kdvOrani: k.kdvOrani,
      stokId: k.stokId || null
    }))
  }
  formTemizle()
  showDialog.value = true
}

const closeDialog = () => { showDialog.value = false }

const saveFatura = async () => {
  if (!form.value.tur) {
    toast.add({ severity: 'warn', summary: 'Uyarı', detail: 'Fatura türü seçiniz', life: 5000 })
    return
  }
  const gecersiz = form.value.kalemler.some(k => !k.aciklama.trim() || !k.adet || !k.birimFiyat)
  if (gecersiz) {
    toast.add({ severity: 'warn', summary: 'Uyarı', detail: 'Tüm kalemleri eksiksiz doldurun', life: 5000 })
    return
  }

  const payload = {
    cariHesapId: form.value.cariHesapId,
    tur: form.value.tur,
    tarih: form.value.tarih ? form.value.tarih.toISOString().split('T')[0] : null,
    aciklama: form.value.aciklama,
    genelIskontoTutari: 0,
    odenenTutar: 0,
    odemeDurumu: 'ODENMEDI',
    kalemler: form.value.kalemler.map(k => ({
      id: k.id || null,
      aciklama: k.aciklama,
      adet: k.adet,
      birimFiyat: k.birimFiyat,
      iskontoOrani: k.iskontoOrani || 0,
      kdvOrani: k.kdvOrani || 0,
      stokId: k.stokId || null
    }))
  }

  saving.value = true
  try {
    if (editingId.value) {
      await faturaStore.updateFatura(editingId.value, payload)
      toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Fatura güncellendi', life: 5000 })
    } else {
      await faturaStore.addFatura(payload)
      toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Fatura oluşturuldu', life: 5000 })
    }
    taslakTemizle()
    formTemizle()
    closeDialog()
  } catch (err) {
    const msg = err.response?.data?.message || 'İşlem başarısız'
    toast.add({ severity: 'error', summary: 'Hata', detail: msg, life: 5000 })
  } finally {
    saving.value = false
  }
}

const viewFatura = (id) => { router.push(`/faturalar/${id}`) }
const pdfIndir = async (fatura) => {
  try {
    const res = await pdfAPI.fatura(fatura.id)
    const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `fatura_${fatura.faturaNumarasi || fatura.id}.pdf`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch {
    toast.add({ severity: 'error', summary: 'Hata', detail: 'PDF indirilemedi', life: 5000 })
  }
}

const confirmKes = (id) => {
  confirm.require({
    message: 'Faturayı kesmek istediğinizden emin misiniz?',
    header: 'Fatura Kes',
    icon: 'pi pi-check-circle',
    accept: async () => {
      try {
        await faturaStore.updateDurum(id, 'KESILDI')
        toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Fatura kesildi', life: 5000 })
      } catch { toast.add({ severity: 'error', summary: 'Hata', detail: 'İşlem başarısız', life: 5000 }) }
    }
  })
}

const confirmIptal = (id) => {
  confirm.require({
    message: 'Faturayı iptal etmek istediğinizden emin misiniz?',
    header: 'Fatura İptal',
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      try {
        await faturaStore.updateDurum(id, 'IPTAL')
        toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Fatura iptal edildi', life: 5000 })
      } catch { toast.add({ severity: 'error', summary: 'Hata', detail: 'İşlem başarısız', life: 5000 }) }
    }
  })
}

const durumLabel = (d) => {
  const lbl = { TASLAK: 'Taslak', TEKLIF: 'Teklif', KESILDI: 'Kesildi', IPTAL: 'İptal' }
  return lbl[d] || d
}

const excelIndir = async () => {
  try {
    const res = await excelAPI.faturalar()
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'Faturalar.xlsx')
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch { /* silent */ }
}

const formatCurrency = (value) => {
  if (value === null || value === undefined) return '0,00 ₺'
  return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(value)
}

const formatDate = (dateString) => {
  if (!dateString) return '-'
  return new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(dateString))
}
</script>

<style scoped>
.faturalar-container { padding: 20px; }
h1 { color: var(--text-primary); margin-bottom: 20px; font-size: 28px; font-weight: 700; letter-spacing: -0.5px; }
.toolbar { margin-bottom: 20px; background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 14px 18px; }
.table-container { background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 14px; overflow-x: auto; }
.loading { text-align: center; padding: 40px; color: #666; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
.form-group { margin-bottom: 15px; }
.form-group label { display: block; margin-bottom: 6px; font-weight: 600; color: var(--text-secondary); font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px; }
.urun-ekleme { background: rgba(59,130,246,0.05); border: 1px solid rgba(59,130,246,0.2); border-radius: 10px; padding: 14px; margin: 15px 0; }
.summary-box { background: var(--border); border: 1px solid var(--border); border-radius: 10px; padding: 15px; margin-top: 15px; }
.summary-row { display: flex; justify-content: space-between; padding: 5px 0; font-size: 14px; color: var(--text-secondary); }
.summary-row.total { font-weight: 700; font-size: 18px; border-top: 2px solid #3b82f6; margin-top: 5px; padding-top: 10px; color: var(--text-primary); }
.badge { padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 700; }
.badge.satis { background: rgba(59,130,246,0.15); color: #60a5fa; }
.badge.alis { background: rgba(239,68,68,0.15); color: #f87171; }
.durum-badge { padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 700; }
.durum-badge.taslak { background: rgba(255,152,0,0.15); color: #fb923c; }
.durum-badge.teklif { background: rgba(96,165,250,0.15); color: #60a5fa; }
.durum-badge.kesildi { background: rgba(34,197,94,0.15); color: #4ade80; }
.durum-badge.iptal { background: rgba(148,163,184,0.1); color: #94a3b8; }
.w-full { width: 100% !important; }
</style>
