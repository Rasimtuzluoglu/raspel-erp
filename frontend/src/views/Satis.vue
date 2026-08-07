<template>
  <div class="satis-container">
    <h1>Satış İşlemleri</h1>

    <Toolbar class="toolbar">
      <template #start>
        <Button
          label="Yeni Satış"
          icon="pi pi-plus"
          class="p-button-success"
          @click="openSatis"
        />
      </template>
      <template #end>
        <TarihHizliSecim v-model="tarihAraligi" style="margin-right:8px" />
        <span class="p-input-icon-left">
          <i class="pi pi-search" />
          <InputText
            v-model="filtre"
            placeholder="Fatura no veya cari ara..."
          />
        </span>
      </template>
    </Toolbar>

    <div class="table-container">
      <DataTable
        :value="filtrelenmisSatislar"
        :paginator="true"
        :rows="15"
        paginator-template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport"
        current-page-report-template="{totalRecords} kayıttan {first}-{last}"
        striped-rows
        sort-field="tarih"
        :sort-order="-1"
      >
        <Column
          field="faturaNumarasi"
          header="Fatura No"
          style="width:160px"
        />
        <Column
          field="tarih"
          header="Tarih"
          style="width:110px"
        >
          <template #body="s">
            {{ formatDate(s.data.tarih) }}
          </template>
        </Column>
        <Column
          field="cariHesapAd"
          header="Müşteri"
          style="width:200px"
        >
          <template #body="s">
            {{ s.data.cariHesapAd || '-' }}
          </template>
        </Column>
        <Column
          field="genelToplam"
          header="Tutar"
          style="width:130px"
        >
          <template #body="s">
            {{ formatCurrency(s.data.genelToplam) }}
          </template>
        </Column>
        <Column
          field="durum"
          header="Durum"
          style="width:100px"
        >
          <template #body="s">
            <span :class="['durum-badge', (s.data.durum || '').toLowerCase()]">{{ durumLabel(s.data.durum) }}</span>
          </template>
        </Column>
        <Column
          header="İşlemler"
          style="width:180px"
        >
          <template #body="s">
            <Button
              icon="pi pi-eye"
              class="p-button-rounded p-button-sm p-button-info"
              title="Görüntüle"
              @click="$router.push(`/faturalar/${s.data.id}`)"
            />
            <Button
              icon="pi pi-print"
              class="p-button-rounded p-button-sm p-button-help"
              title="A4 Fatura Yazdır"
              @click="printFatura(s.data.id)"
            />
            <Button
              icon="pi pi-receipt"
              class="p-button-rounded p-button-sm p-button-warning"
              title="Termal Fiş Yazdır (80mm)"
              @click="printTermalFis(s.data)"
            />
          </template>
        </Column>
      </DataTable>
      <Message
        v-if="filtrelenmisSatislar.length === 0"
        severity="info"
        text="Satış bulunamadı."
      />
    </div>

    <Dialog
      v-model:visible="showSatisDialog"
      :header="dialogBaslik"
      :modal="true"
      style="width:800px"
      :closable="false"
    >
      <div class="satis-modu">
        <label style="color:#94a3b8;font-weight:600;font-size:12px;text-transform:uppercase;margin-right:12px">İşlem Modu</label>
        <div class="modu-radio-group">
          <div
            :class="['modu-option', { active: satisModu === 'SATIS' }]"
            @click="satisModu = 'SATIS'"
          >
            <i class="pi pi-shopping-cart" /> Satış Yap
          </div>
          <div
            :class="['modu-option', { active: satisModu === 'TEKLIF' }]"
            @click="satisModu = 'TEKLIF'"
          >
            <i class="pi pi-file" /> Teklif Oluştur
          </div>
        </div>
      </div>
      <div class="form-row">
        <div
          class="form-group"
          style="flex:2"
        >
          <label>Müşteri <span v-if="satisModu === 'SATIS'">*</span></label>
          <Dropdown
            v-model="satisForm.cariHesapId"
            :options="cariHesapStore.cariHesaplar"
            option-label="ad"
            option-value="id"
            placeholder="Müşteri seçiniz"
            class="w-full"
          />
        </div>
        <div
          class="form-group"
          style="flex:1"
        >
          <label>Tarih *</label>
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
            style="flex:3"
          >
            <label>Ürün Seç</label>
            <Dropdown
              v-model="seciliUrun"
              :options="stokStore.stoklar"
              filter
              option-label="ad"
              option-value="id"
              placeholder="Ürün ara ve seç..."
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
                  <span class="urun-stok">{{ slotProps.option.miktar }} {{ slotProps.option.birim || 'Adet' }}</span>
                  <span class="urun-fiyat">{{ formatCurrency(slotProps.option.fiyat) }}</span>
                </div>
              </template>
            </Dropdown>
          </div>
          <div
            class="form-group"
            style="flex:1"
          >
            <label>Miktar *</label>
            <InputNumber
              v-model="yeniUrunAdet"
              :min="1"
              class="w-full"
            />
          </div>
          <div
            class="form-group"
            style="flex:1"
          >
            <label>Birim Fiyat</label>
            <InputNumber
              v-model="yeniUrunFiyat"
              :min="0"
              :min-fraction-digits="2"
              class="w-full"
            />
          </div>
          <div
            class="form-group"
            style="flex:0 0 auto;display:flex;align-items:flex-end"
          >
            <Button
              icon="pi pi-plus"
              class="p-button-success"
              :disabled="!seciliUrun || !yeniUrunAdet"
              @click="urunEkle"
            />
          </div>
        </div>
        <small style="color:#64748b">Ürün seçince fiyat otomatik gelir, değiştirebilirsiniz</small>
      </div>

      <h3 style="margin:18px 0 10px;color:#f1f5f9;font-size:15px">
        Satış Kalemleri
      </h3>
      <DataTable
        :value="satisForm.kalemler"
        striped-rows
      >
        <Column
          header="#"
          style="width:40px"
        >
          <template #body="s">
            {{ s.index + 1 }}
          </template>
        </Column>
        <Column header="Ürün">
          <template #body="s">
            {{ s.data.aciklama }}
          </template>
        </Column>
        <Column
          header="Adet"
          style="width:80px"
        >
          <template #body="s">
            {{ s.data.adet }}
          </template>
        </Column>
        <Column
          header="Birim Fiyat"
          style="width:120px"
        >
          <template #body="s">
            {{ formatCurrency(s.data.birimFiyat) }}
          </template>
        </Column>
        <Column
          header="KDV"
          style="width:60px"
        >
          <template #body="s">
            %{{ s.data.kdvOrani }}
          </template>
        </Column>
        <Column
          header="Tutar"
          style="width:120px"
        >
          <template #body="s">
            {{ formatCurrency(s.data.tutar || (s.data.birimFiyat * s.data.adet)) }}
          </template>
        </Column>
        <Column
          header=""
          style="width:50px"
        >
          <template #body="s">
            <Button
              icon="pi pi-trash"
              class="p-button-rounded p-button-danger p-button-sm"
              @click="satisForm.kalemler.splice(s.index,1)"
            />
          </template>
        </Column>
      </DataTable>

      <div class="summary-box">
        <div class="summary-row">
          <span>Ara Toplam:</span><span>{{ formatCurrency(araToplam) }}</span>
        </div>
        <div class="summary-row">
          <span>KDV:</span><span>{{ formatCurrency(kdvToplam) }}</span>
        </div>
        <div class="summary-row total">
          <span>Genel Toplam:</span><span>{{ formatCurrency(genelToplam) }}</span>
        </div>
      </div>

      <div class="form-group">
        <label>Açıklama</label>
        <Textarea
          v-model="satisForm.aciklama"
          rows="2"
          class="w-full"
        />
      </div>

      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="showSatisDialog = false"
        />
        <Button
          :label="satisModu === 'TEKLIF' ? 'Teklifi Kaydet' : 'Satışı Tamamla'"
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
import { ref, computed, onMounted } from 'vue'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { faturaAPI } from '../api/index.js'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { useStokStore } from '../stores/stokStore.js'
import TarihHizliSecim from '../components/TarihHizliSecim.vue'

const toastBildirim = useToastBildirim()
const cariHesapStore = useCariHesapStore()
const stokStore = useStokStore()

const satislar = ref([])
const showSatisDialog = ref(false)
const saving = ref(false)
const filtre = ref('')
const satisModu = ref('SATIS')
const seciliUrun = ref(null)
const yeniUrunAdet = ref(1)
const yeniUrunFiyat = ref(0)

const satisForm = ref({
  cariHesapId: null,
  tarih: new Date(),
  aciklama: '',
  kalemler: []
})

const tarihAraligi = ref(null)

onMounted(async () => {
  await Promise.all([satislariYukle(), cariHesapStore.getAllCariHesaplar(), stokStore.getAll()])
})

const satislariYukle = async () => {
  try {
    const r = await faturaAPI.getAll()
    satislar.value = (r.data?.content || r.data || []).filter(f => f.tur === 'SATIS')
  } catch {
    toastBildirim.hata('Satışlar yüklenemedi')
  }
}

const filtrelenmisSatislar = computed(() => {
  let list = satislar.value
  if (tarihAraligi.value && tarihAraligi.value.length === 2 && tarihAraligi.value[0]) {
    const bas = new Date(tarihAraligi.value[0])
    bas.setHours(0, 0, 0, 0)
    const bit = new Date(tarihAraligi.value[1])
    bit.setHours(23, 59, 59, 999)
    list = list.filter(f => {
      if (!f.tarih) return false
      const t = new Date(f.tarih)
      return t >= bas && t <= bit
    })
  }
  if (!filtre.value.trim()) return list
  const q = filtre.value.toLowerCase()
  return list.filter(s =>
    s.faturaNumarasi?.toLowerCase().includes(q) ||
    s.cariHesapAd?.toLowerCase().includes(q)
  )
})

const stokAdi = (id) => {
  const u = stokStore.stoklar.find(s => s.id === id)
  return u ? `${u.ad} (${u.miktar} ${u.birim || 'Adet'}) - ${formatCurrency(u.fiyat)}` : ''
}

const urunSecildi = () => {
  if (!seciliUrun.value) return
  const u = stokStore.stoklar.find(s => s.id === seciliUrun.value)
  if (u) yeniUrunFiyat.value = u.fiyat
}

const urunEkle = () => {
  if (!seciliUrun.value || !yeniUrunAdet.value) return
  const u = stokStore.stoklar.find(s => s.id === seciliUrun.value)
  if (!u) return
  if (u.miktar < yeniUrunAdet.value) {
    toastBildirim.uyari(`Yetersiz stok! Mevcut: ${u.miktar} ${u.birim || 'Adet'}`)
    return
  }
  const brf = yeniUrunFiyat.value || u.fiyat
  satisForm.value.kalemler.push({
    aciklama: u.ad,
    adet: yeniUrunAdet.value,
    birimFiyat: brf,
    kdvOrani: 20,
    stokId: u.id,
    tutar: brf * yeniUrunAdet.value * (1 + 20 / 100)
  })
  seciliUrun.value = null
  yeniUrunAdet.value = 1
  yeniUrunFiyat.value = 0
}

const araToplam = computed(() => satisForm.value.kalemler.reduce((t, k) => t + (Number(k.birimFiyat) * Number(k.adet)), 0))
const kdvToplam = computed(() => satisForm.value.kalemler.reduce((t, k) => t + (Number(k.birimFiyat) * Number(k.adet) * (Number(k.kdvOrani) || 20) / 100), 0))
const genelToplam = computed(() => araToplam.value + kdvToplam.value)

const dialogBaslik = computed(() => satisModu.value === 'TEKLIF' ? 'Yeni Teklif' : 'Yeni Satış')

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
    toastBildirim.uyari('Müşteri seçiniz')
    return
  }
  if (satisForm.value.kalemler.length === 0) {
    toastBildirim.uyari('En az bir ürün ekleyin')
    return
  }
  saving.value = true
  try {
    const durum = satisModu.value === 'TEKLIF' ? 'TEKLIF' : 'KESILDI'
    const payload = {
      cariHesapId: satisForm.value.cariHesapId,
      tur: 'SATIS',
      durum,
      tarih: satisForm.value.tarih?.toISOString().split('T')[0],
      aciklama: satisForm.value.aciklama,
      kalemler: satisForm.value.kalemler.map(k => ({
        aciklama: k.aciklama,
        adet: k.adet,
        birimFiyat: k.birimFiyat,
        kdvOrani: k.kdvOrani || 20,
        stokId: k.stokId
      }))
    }
    await faturaAPI.create(payload)
    const msg = durum === 'TEKLIF' ? 'Teklif kaydedildi' : 'Satış tamamlandı ve stok düşüldü'
    toastBildirim.basarili(msg)
    showSatisDialog.value = false
    await satislariYukle()
  } catch (err) {
    const msg = err.response?.data?.message || 'Satış başarısız'
    toastBildirim.hata(msg)
  } finally {
    saving.value = false
  }
}

const printFatura = (id) => window.open(`/faturalar/${id}?print=true`, '_blank')
const durumLabel = (d) => ({ TASLAK: 'Taslak', TEKLIF: 'Teklif', KESILDI: 'Kesildi', IPTAL: 'İptal' })[d] || d
const formatCurrency = (v) => v ?? 0 ? new Intl.NumberFormat('tr-TR',{style:'currency',currency:'TRY'}).format(v) : '0,00 ₺'
const formatDate = (d) => d ? new Intl.DateTimeFormat('tr-TR',{year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date(d)) : '-'
const printTermalFis = (satisData) => {
  const fisWindow = window.open('', '_blank', 'width=400,height=600')
  if (!fisWindow) {
    toastBildirim.hata('Pencere açılamadı. Pop-up engelleyicinizi kontrol edin.')
    return
  }

  const kalemlerHtml = (satisData.kalemler || []).map(k => `
    <tr>
      <td style="text-align:left;">${k.stokAd || k.ad || 'Ürün'} x${k.miktar || k.adet || 1}</td>
      <td style="text-align:right;">${formatCurrency(k.toplamTutar || (k.miktar * k.birimFiyat) || (k.adet * k.birimFiyat))}</td>
    </tr>
  `).join('')

  const content = `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <title>Termal Fiş - ${satisData.faturaNumarasi || 'SATIŞ'}</title>
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
        .header p { margin: 2px 0; font-size: 10px; }
        .footer { margin-top: 10px; text-align: center; font-size: 10px; }
        .no-print { text-align: center; margin-bottom: 12px; }
        .no-print button { padding: 6px 16px; background: #2563eb; color: #fff; border: none; border-radius: 4px; cursor: pointer; font-weight: bold; }
        @media print { .no-print { display: none !important; } }
      </style>
    </head>
    <body>
      <div class="no-print">
        <button onclick="window.print()">Yazdır (Termal 80mm)</button>
        <button onclick="window.close()" style="background:#64748b; margin-left:6px;">Kapat</button>
      </div>
      <div class="header text-center">
        <h2>RASPEL ERP</h2>
        <p>SATIŞ FİŞİ</p>
        <p>Fiş No: ${satisData.faturaNumarasi || 'FIS-' + (satisData.id || Date.now())}</p>
        <p>Tarih: ${formatDate(satisData.tarih || new Date())}</p>
        <p>Müşteri: ${satisData.cariHesapAd || 'Perakende Müşteri'}</p>
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
          ${kalemlerHtml.length ? kalemlerHtml : '<tr><td colspan="2">1 Adet Satış Kalemi</td></tr>'}
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
        <p>Islem Yapan: ${authStore.kullanici?.displayName || '-'}</p>
      </div>
    </body>
    </html>
  `
  fisWindow.document.open()
  fisWindow.document.write(content)
  fisWindow.document.close()
  setTimeout(() => {
    try {
      fisWindow.focus()
      fisWindow.print()
    } catch (e) {
      console.error('Termal yazıcı hatası:', e)
    }
  }, 300)
}
</script>

<style scoped>
.satis-container { padding: 20px; }
h1 { color: var(--text-primary); margin-bottom: 20px; font-size: 28px; font-weight: 700; letter-spacing: -0.5px; }
.toolbar { margin-bottom: 20px; background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 14px 18px; }
.table-container { background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; padding: 14px; overflow-x: auto; }
.form-row { display: flex; gap: 12px; margin-bottom: 12px; }
.form-group { margin-bottom: 15px; }
.form-group label { display: block; margin-bottom: 6px; font-weight: 600; color: var(--text-secondary); font-size: 12px; text-transform: uppercase; letter-spacing: 0.5px; }
.urun-ekleme { background: rgba(59,130,246,0.05); border: 1px solid rgba(59,130,246,0.2); border-radius: 10px; padding: 16px; margin: 15px 0; }
.urun-opsiyon { display: flex; align-items: center; gap: 10px; width: 100%; }
.urun-ad { flex: 1; color: var(--text-primary); }
.urun-stok { color: #4ade80; font-size: 12px; font-weight: 600; }
.urun-fiyat { color: var(--text-secondary); font-size: 12px; }
.summary-box { background: var(--border); border: 1px solid var(--border); border-radius: 10px; padding: 15px; margin-top: 15px; }
.summary-row { display: flex; justify-content: space-between; padding: 5px 0; font-size: 14px; color: var(--text-secondary); }
.summary-row.total { font-weight: 700; font-size: 18px; border-top: 2px solid #3b82f6; margin-top: 5px; padding-top: 10px; color: var(--text-primary); }
.durum-badge { padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 700; }
.durum-badge.taslak { background: rgba(255,152,0,0.15); color: #fb923c; }
.durum-badge.teklif { background: rgba(59,130,246,0.15); color: #60a5fa; }
.durum-badge.kesildi { background: rgba(34,197,94,0.15); color: #4ade80; }
.durum-badge.iptal { background: rgba(148,163,184,0.1); color: #94a3b8; }
.satis-modu { display: flex; align-items: center; margin-bottom: 16px; }
.modu-radio-group { display: flex; gap: 2px; background: var(--border); border-radius: 8px; padding: 3px; }
.modu-option { padding: 8px 16px; border-radius: 6px; cursor: pointer; color: var(--text-secondary); font-size: 13px; font-weight: 500; display: flex; align-items: center; gap: 6px; transition: all 0.2s; }
.modu-option:hover { color: #e2e8f0; }
.modu-option.active { background: #3b82f6; color: #fff; }
.w-full { width: 100% !important; }
</style>

