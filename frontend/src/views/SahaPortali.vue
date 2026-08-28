<template>
  <div class="saha-portali-sayfasi">
    <!-- Header Banner -->
    <div class="saha-header-card">
      <div class="saha-header-content">
        <div class="user-info">
          <div class="user-avatar">
            <i class="pi pi-user" />
          </div>
          <div>
            <h2 class="user-name">
              {{ authStore?.kullanici?.displayName || authStore?.kullanici?.username || 'Saha Personeli' }}
            </h2>
            <span class="user-badge">
              <i class="pi pi-compass" /> Saha & Personel Mobil Portalı
            </span>
          </div>
        </div>
        <div class="header-actions">
          <Button
            label="Hızlı Sipariş Al"
            icon="pi pi-plus"
            class="p-button-warning p-button-sm font-semibold"
            @click="yeniSiparisModal = true"
          />
          <Button
            icon="pi pi-refresh"
            class="p-button-rounded p-button-text text-white p-button-sm"
            :loading="yukleniyor"
            @click="tumunuYukle"
          />
        </div>
      </div>
    </div>

    <!-- PrimeVue TabView Sekmeleri -->
    <TabView>
      <TabPanel>
        <template #header>
          <span class="flex items-center gap-1.5">
            <i class="pi pi-shopping-bag" />
            Sipariş & Teslimat
            <span
              v-if="bekleyenSiparisSayisi > 0"
              class="tab-badge"
            >{{ bekleyenSiparisSayisi }}</span>
          </span>
        </template>

        <SahaSiparislerPanel
          :siparisler="siparisler"
          @durum-sec="durumSecModalAc"
          @imza-ac="imzaModalAc"
          @whatsapp="whatsappSiparisPaylas"
        />
      </TabPanel>

      <TabPanel>
        <template #header>
          <span class="flex items-center gap-1.5">
            <i class="pi pi-map-marker" />
            Ziyaret Kaydı
          </span>
        </template>

        <div class="fade-in-section">
          <div class="form-container-card">
            <div class="form-header">
              <h3><i class="pi pi-map-marker text-red-500 mr-2" />Müşteri Ziyaret & Görüşme Kaydı</h3>
              <p>Ziyaret ettiğiniz müşteriyi seçip görüşme özetini merkeze bildirin.</p>
            </div>

            <div class="form-body">
              <div class="form-field">
                <label>Ziyaret Edilen Müşteri *</label>
                <Dropdown
                  v-model="ziyaretForm.cariHesapId"
                  :options="cariHesaplar"
                  option-label="ad"
                  option-value="id"
                  placeholder="Müşteri Seçin..."
                  filter
                  class="w-full"
                />
              </div>
              <div class="form-field">
                <label>Ziyaret Amacı</label>
                <Dropdown
                  v-model="ziyaretForm.amac"
                  :options="['Satış & Tanıtım', 'Sipariş & Teklif Takibi', 'Tahsilat', 'Rutin Ziyaret', 'Destek / İade']"
                  class="w-full"
                />
              </div>
              <div class="form-field">
                <label>Görüşme Notları & Sonuç *</label>
                <Textarea
                  v-model="ziyaretForm.notlar"
                  rows="4"
                  placeholder="Görüşülen yetkili, talep edilenler, sonraki adımlar..."
                  class="w-full"
                />
              </div>
              <div class="gps-location-row flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-800 rounded-lg mb-3">
                <div class="flex items-center gap-2">
                  <i class="pi pi-map-marker text-red-500 text-lg" />
                  <span class="text-sm text-gray-700 dark:text-gray-200">
                    {{ ziyaretKonum || 'Konum alınmadı' }}
                  </span>
                </div>
                <Button
                  label="Konum Al"
                  icon="pi pi-compass"
                  class="p-button-outlined p-button-sm p-button-secondary"
                  :loading="konumAliniyor"
                  @click="gpsKonumAl"
                />
              </div>
              <Button
                label="Ziyaret Notunu Merkeze İlet"
                icon="pi pi-send"
                class="p-button-primary w-full mt-2 font-bold"
                :loading="ziyaretKaydediliyor"
                @click="ziyaretKaydet"
              />
            </div>
          </div>
        </div>
      </TabPanel>

      <TabPanel>
        <template #header>
          <span class="flex items-center gap-1.5">
            <i class="pi pi-wallet" />
            Masraf & Avans
          </span>
        </template>

        <div class="fade-in-section">
          <div class="section-title-row">
            <h3><i class="pi pi-receipt text-primary mr-2" />Masraf & Avans Taleplerim</h3>
            <Button
              label="Yeni Masraf / Avans"
              icon="pi pi-plus"
              class="p-button-primary p-button-sm"
              @click="yeniMasrafModal = true"
            />
          </div>

          <div
            v-if="masraflar && masraflar.length > 0"
            class="expense-list"
          >
            <div
              v-for="m in masraflar"
              :key="m.id"
              class="expense-card"
            >
              <div class="expense-left">
                <div class="flex items-center gap-2 mb-1">
                  <span class="expense-type-badge">{{ m.tur === 'AVANS' ? 'Avans Talebi' : m.kategori }}</span>
                  <Tag
                    :value="m.durum"
                    :severity="talepDurumSeverity(m.durum)"
                    rounded
                  />
                </div>
                <p class="expense-desc">
                  {{ m.aciklama || '-' }}
                </p>
                <small class="expense-date">{{ formatTarih(m.tarih) }}</small>
              </div>
              <div class="expense-right">
                <span class="expense-amount">{{ formatPara(m.tutar) }}</span>
              </div>
            </div>
          </div>

          <div
            v-else
            class="empty-box"
          >
            <i class="pi pi-wallet empty-icon" />
            <p>Henüz masraf veya avans talebiniz yok.</p>
          </div>
        </div>
      </TabPanel>

      <TabPanel>
        <template #header>
          <span class="flex items-center gap-1.5">
            <i class="pi pi-calendar" />
            İzin Talebi
          </span>
        </template>

        <div class="fade-in-section">
          <div class="section-title-row">
            <h3><i class="pi pi-calendar-plus text-primary mr-2" />İzin Taleplerim</h3>
            <Button
              label="Yeni İzin Talebi"
              icon="pi pi-plus"
              class="p-button-primary p-button-sm"
              @click="yeniIzinModal = true"
            />
          </div>

          <div
            v-if="izinler && izinler.length > 0"
            class="expense-list"
          >
            <div
              v-for="i in izinler"
              :key="i.id"
              class="expense-card"
            >
              <div class="expense-left">
                <div class="flex items-center gap-2 mb-1">
                  <strong class="text-base text-gray-800 dark:text-gray-100">{{ i.izinTuru }}</strong>
                  <Tag
                    :value="i.durum"
                    :severity="talepDurumSeverity(i.durum)"
                    rounded
                  />
                </div>
                <p class="expense-desc">
                  <i class="pi pi-calendar mr-1" />
                  {{ formatTarih(i.baslangic) }} → {{ formatTarih(i.bitis) }}
                  <span class="font-bold text-primary ml-1">({{ i.gunSayisi }} Gün)</span>
                </p>
                <small
                  v-if="i.aciklama"
                  class="expense-date"
                >{{ i.aciklama }}</small>
              </div>
            </div>
          </div>

          <div
            v-else
            class="empty-box"
          >
            <i class="pi pi-calendar empty-icon" />
            <p>Henüz kayıtlı izin talebiniz bulunmuyor.</p>
          </div>
        </div>
      </TabPanel>
    </TabView>

    <!-- MODAL: HIZLI SİPARİŞ AL -->
    <Dialog
      v-model:visible="yeniSiparisModal"
      :modal="true"
      header="Sahada Hızlı Sipariş Al"
      :style="{ width: '90%', maxWidth: '520px' }"
    >
      <div class="modal-form-content">
        <div class="form-field">
          <label>Müşteri *</label>
          <Dropdown
            v-model="yeniSiparisForm.cariHesapId"
            :options="cariHesaplar"
            option-label="ad"
            option-value="id"
            placeholder="Müşteri Seçin"
            filter
            class="w-full"
          />
        </div>
        <div class="form-field">
          <label>Barkod ile Hızlı Bul</label>
          <div class="p-inputgroup">
            <span class="p-inputgroup-addon"><i class="pi pi-qrcode" /></span>
            <InputText
              v-model="barkodArama"
              placeholder="Barkod okutun veya girin..."
              @keyup.enter="barkodlaUrunBul"
            />
            <Button
              icon="pi pi-search"
              class="p-button-outlined"
              @click="barkodlaUrunBul"
            />
          </div>
        </div>
        <div class="form-field">
          <label>Ürün / Stok *</label>
          <Dropdown
            v-model="yeniSiparisForm.stokId"
            :options="stoklar"
            option-label="ad"
            option-value="id"
            placeholder="Ürün Seçin"
            filter
            class="w-full"
            @change="hizliSiparisStokSecildi"
          />
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Miktar</label>
            <input
              v-model.number="yeniSiparisForm.miktar"
              type="number"
              min="1"
              class="p-inputtext w-full"
            >
          </div>
          <div class="form-field">
            <label>Birim Fiyat (₺)</label>
            <input
              v-model.number="yeniSiparisForm.birimFiyat"
              type="number"
              min="0"
              step="0.01"
              class="p-inputtext w-full"
            >
          </div>
        </div>
        <div class="form-field">
          <label>Teslimat Adresi / Notu</label>
          <InputText
            v-model="yeniSiparisForm.adres"
            placeholder="Müşteri teslim adresi"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          label="İptal"
          class="p-button-text"
          @click="yeniSiparisModal = false"
        />
        <Button
          label="Siparişi Merkeze Gönder"
          icon="pi pi-check"
          class="p-button-success font-bold"
          :loading="siparisKaydediliyor"
          @click="hizliSiparisKaydet"
        />
      </template>
    </Dialog>

    <!-- MODAL: DİJİTAL İMZA & TESLİMAT -->
    <Dialog
      v-model:visible="imzaModal"
      :modal="true"
      header="✍️ Teslimat İmzası Al"
      :style="{ width: '90%', maxWidth: '460px' }"
    >
      <div class="modal-form-content">
        <div class="form-field">
          <label>Teslim Alan Kişi (Ad Soyad) *</label>
          <InputText
            v-model="imzaForm.teslimAlan"
            placeholder="Örn: Ahmet Yılmaz"
            class="w-full"
          />
        </div>
        <div class="form-field">
          <label>Teslimat Notu</label>
          <InputText
            v-model="imzaForm.notlar"
            placeholder="Eksiksiz ve hasarsız teslim edildi"
            class="w-full"
          />
        </div>
        <div class="form-field">
          <div class="flex justify-between items-center mb-1">
            <label>Dijital İmza</label>
            <button
              type="button"
              class="text-xs text-red-500 hover:underline"
              @click="imzayiTemizle"
            >
              İmzayı Temizle
            </button>
          </div>
          <canvas
            ref="imzaCanvas"
            class="imza-canvas w-full h-36 border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-lg bg-gray-50 dark:bg-gray-800 touch-none"
            @mousedown="imzaBaslat"
            @mousemove="imzaCiz"
            @mouseup="imzaBitir"
            @touchstart="imzaBaslatTouch"
            @touchmove="imzaCizTouch"
            @touchend="imzaBitir"
          />
        </div>
      </div>
      <template #footer>
        <Button
          label="İptal"
          class="p-button-text"
          @click="imzaModal = false"
        />
        <Button
          label="Teslimatı Onayla"
          icon="pi pi-check"
          class="p-button-success font-bold"
          :loading="teslimEdiliyor"
          @click="teslimatOnayla"
        />
      </template>
    </Dialog>

    <!-- MODAL: YENİ İZİN TALEBİ -->
    <Dialog
      v-model:visible="yeniIzinModal"
      :modal="true"
      header="Yeni İzin Talebi Aç"
      :style="{ width: '90%', maxWidth: '420px' }"
    >
      <div class="modal-form-content">
        <div class="form-field">
          <label>İzin Türü</label>
          <Dropdown
            v-model="izinForm.izinTuru"
            :options="['Yıllık İzin', 'Mazeret İzni', 'Sağlık Raporu', 'Evlilik İzni', 'Ücretsiz İzin']"
            class="w-full"
          />
        </div>
        <div class="form-row-2">
          <div class="form-field">
            <label>Başlangıç</label>
            <InputText
              v-model="izinForm.baslangic"
              type="date"
              class="w-full"
            />
          </div>
          <div class="form-field">
            <label>Bitiş</label>
            <InputText
              v-model="izinForm.bitis"
              type="date"
              class="w-full"
            />
          </div>
        </div>
        <div class="form-field">
          <label>Açıklama</label>
          <Textarea
            v-model="izinForm.aciklama"
            rows="2"
            placeholder="İzin gerekçesi..."
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          label="İptal"
          class="p-button-text"
          @click="yeniIzinModal = false"
        />
        <Button
          label="Talebi Gönder"
          icon="pi pi-send"
          class="p-button-primary font-bold"
          :loading="izinGonderiliyor"
          @click="izinTalepGonder"
        />
      </template>
    </Dialog>

    <!-- MODAL: YENİ MASRAF / AVANS -->
    <Dialog
      v-model:visible="yeniMasrafModal"
      :modal="true"
      header="Yeni Masraf / Avans Talebi"
      :style="{ width: '90%', maxWidth: '420px' }"
    >
      <div class="modal-form-content">
        <div class="tab-pill-group">
          <button
            type="button"
            :class="['pill-btn', { active: masrafForm.tur === 'MASRAF' }]"
            @click="masrafForm.tur = 'MASRAF'"
          >
            Harcama / Masraf
          </button>
          <button
            type="button"
            :class="['pill-btn', { active: masrafForm.tur === 'AVANS' }]"
            @click="masrafForm.tur = 'AVANS'"
          >
            Avans Talebi
          </button>
        </div>

        <div
          v-if="masrafForm.tur === 'MASRAF'"
          class="form-field"
        >
          <label>Harcama Kategorisi</label>
          <Dropdown
            v-model="masrafForm.kategori"
            :options="['YAKIT', 'YEMEK', 'KONAKLAMA', 'ULASIM', 'MALZEME', 'DIGER']"
            class="w-full"
          />
        </div>

        <div class="form-field">
          <label>Tutar (₺) *</label>
          <input
            v-model.number="masrafForm.tutar"
            type="number"
            min="1"
            step="0.01"
            class="p-inputtext w-full"
            placeholder="0.00"
          >
        </div>

        <div class="form-field">
          <label>Açıklama *</label>
          <InputText
            v-model="masrafForm.aciklama"
            placeholder="Örn: Müşteri ziyareti yakıt fişi"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          label="İptal"
          class="p-button-text"
          @click="yeniMasrafModal = false"
        />
        <Button
          label="Talebi Gönder"
          icon="pi pi-send"
          class="p-button-primary font-bold"
          :loading="masrafGonderiliyor"
          @click="masrafTalepGonder"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useAuthStore } from '../stores/authStore.js'
import { siparisAPI, personelIzinAPI, personelMasrafTalepAPI, cariHesapAPI, stokAPI, notAPI } from '../api/index.js'
import { useToast } from 'primevue/usetoast'
import SahaSiparislerPanel from '../components/SahaSiparislerPanel.vue'

const authStore = useAuthStore()
const toast = useToast()

const yukleniyor = ref(false)

const siparisler = ref([])
const izinler = ref([])
const masraflar = ref([])
const cariHesaplar = ref([])
const stoklar = ref([])

// Modallar
const imzaModal = ref(false)
const yeniIzinModal = ref(false)
const yeniMasrafModal = ref(false)
const yeniSiparisModal = ref(false)
const seciliSiparis = ref(null)

const teslimEdiliyor = ref(false)
const izinGonderiliyor = ref(false)
const masrafGonderiliyor = ref(false)
const ziyaretKaydediliyor = ref(false)
const siparisKaydediliyor = ref(false)

// Formlar
const imzaForm = ref({ teslimAlan: '', notlar: '' })
const izinForm = ref({
  izinTuru: 'Yıllık İzin',
  baslangic: new Date().toISOString().substring(0, 10),
  bitis: new Date().toISOString().substring(0, 10),
  aciklama: ''
})
const masrafForm = ref({
  tur: 'MASRAF',
  kategori: 'YAKIT',
  tutar: null,
  aciklama: ''
})
const ziyaretForm = ref({
  cariHesapId: null,
  amac: 'Satış & Tanıtım',
  notlar: ''
})
const barkodArama = ref('')
const ziyaretKonum = ref('')
const konumAliniyor = ref(false)

const yeniSiparisForm = ref({
  cariHesapId: null,
  stokId: null,
  miktar: 1,
  birimFiyat: 0,
  adres: ''
})

// Format Helpers (Lokal ve Güvenli)
const formatPara = (v) => {
  if (v == null || isNaN(v)) return '0,00 ₺'
  return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v)
}
const formatTarih = (d) => {
  if (!d) return '-'
  return new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(d))
}

// Canvas
const imzaCanvas = ref(null)
let cizimYapiliyor = false
let ctx = null

onMounted(async () => {
  await tumunuYukle()
})

const tumunuYukle = async () => {
  yukleniyor.value = true
  try {
    const [sipRes, izinRes, masrafRes, cariRes, stokRes] = await Promise.allSettled([
      siparisAPI.getAll({ size: 50 }),
      personelIzinAPI.getAll(),
      personelMasrafTalepAPI.getKullaniciTalepleri(),
      cariHesapAPI.getAll({ size: 500 }),
      stokAPI.getAll({ size: 500 })
    ])
    if (sipRes.status === 'fulfilled') siparisler.value = sipRes.value.data?.content || sipRes.value.data || []
    if (izinRes.status === 'fulfilled') izinler.value = izinRes.value.data?.content || izinRes.value.data || []
    if (masrafRes.status === 'fulfilled') masraflar.value = masrafRes.value.data || []
    if (cariRes.status === 'fulfilled') cariHesaplar.value = cariRes.value.data?.content || cariRes.value.data || []
    if (stokRes.status === 'fulfilled') stoklar.value = stokRes.value.data?.content || stokRes.value.data || []
  } finally {
    yukleniyor.value = false
  }
}

const bekleyenSiparisSayisi = computed(() =>
  siparisler.value.filter(s => s?.durum !== 'TESLIM_EDILDI' && s?.durum !== 'IPTAL').length
)


const talepDurumSeverity = (durum) => {
  const map = { BEKLEMEDE: 'warning', ONAYLANDI: 'success', REDDEDILDI: 'danger' }
  return map[durum] || 'info'
}

const imzaModalAc = (siparis) => {
  seciliSiparis.value = siparis
  imzaForm.value = { teslimAlan: '', notlar: '' }
  imzaModal.value = true
  nextTick(() => {
    if (imzaCanvas.value) {
      imzaCanvas.value.width = imzaCanvas.value.offsetWidth
      imzaCanvas.value.height = imzaCanvas.value.offsetHeight
      ctx = imzaCanvas.value.getContext('2d')
      ctx.lineWidth = 2
      ctx.strokeStyle = '#1e293b'
    }
  })
}

const imzaBaslat = (e) => { cizimYapiliyor = true; ctx.beginPath(); ctx.moveTo(e.offsetX, e.offsetY) }
const imzaCiz = (e) => { if (cizimYapiliyor) { ctx.lineTo(e.offsetX, e.offsetY); ctx.stroke() } }
const imzaBitir = () => { cizimYapiliyor = false }
const imzaBaslatTouch = (e) => { e.preventDefault(); cizimYapiliyor = true; const r = imzaCanvas.value.getBoundingClientRect(); ctx.beginPath(); ctx.moveTo(e.touches[0].clientX - r.left, e.touches[0].clientY - r.top) }
const imzaCizTouch = (e) => { e.preventDefault(); if (cizimYapiliyor) { const r = imzaCanvas.value.getBoundingClientRect(); ctx.lineTo(e.touches[0].clientX - r.left, e.touches[0].clientY - r.top); ctx.stroke() } }
const imzayiTemizle = () => { if (ctx && imzaCanvas.value) ctx.clearRect(0, 0, imzaCanvas.value.width, imzaCanvas.value.height) }

const teslimatOnayla = async () => {
  if (!imzaForm.value.teslimAlan) {
    toast.add({ severity: 'warn', summary: 'Eksik Bilgi', detail: 'Teslim alan kişi adını giriniz.', life: 3000 })
    return
  }
  teslimEdiliyor.value = true
  try {
    if (seciliSiparis.value) seciliSiparis.value.durum = 'TESLIM_EDILDI'
    toast.add({ severity: 'success', summary: 'Teslim Edildi', detail: 'Sipariş başarıyla teslim edildi.', life: 3000 })
    imzaModal.value = false
    await tumunuYukle()
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err.message, life: 3000 })
  } finally {
    teslimEdiliyor.value = false
  }
}

const durumSecModalAc = (siparis) => {
  const durum = prompt('Yeni durum: (HAZIRLANIYOR, YOLDA, TESLIM_EDILDI)', siparis.durum || 'YOLDA')
  if (durum) {
    siparis.durum = durum.toUpperCase()
    toast.add({ severity: 'success', summary: 'Güncellendi', detail: 'Sipariş durumu güncellendi.', life: 2500 })
  }
}

const izinTalepGonder = async () => {
  izinGonderiliyor.value = true
  try {
    const bas = new Date(izinForm.value.baslangic)
    const bit = new Date(izinForm.value.bitis)
    const gunSayisi = Math.max(1, Math.round((bit - bas) / (1000 * 60 * 60 * 24)) + 1)
    await personelIzinAPI.create({
      personelId: authStore?.kullanici?.personelId || 1,
      izinTuru: izinForm.value.izinTuru,
      baslangic: izinForm.value.baslangic,
      bitis: izinForm.value.bitis,
      gunSayisi,
      aciklama: izinForm.value.aciklama
    })
    toast.add({ severity: 'success', summary: 'Başarılı', detail: 'İzin talebiniz iletildi.', life: 3000 })
    yeniIzinModal.value = false
    await tumunuYukle()
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err.message, life: 3000 })
  } finally {
    izinGonderiliyor.value = false
  }
}

const masrafTalepGonder = async () => {
  if (!masrafForm.value.tutar || masrafForm.value.tutar <= 0) {
    toast.add({ severity: 'warn', summary: 'Eksik', detail: 'Geçerli bir tutar girin.', life: 3000 })
    return
  }
  masrafGonderiliyor.value = true
  try {
    await personelMasrafTalepAPI.create({
      tur: masrafForm.value.tur,
      kategori: masrafForm.value.kategori,
      tutar: masrafForm.value.tutar,
      aciklama: masrafForm.value.aciklama,
      tarih: new Date().toISOString().substring(0, 10)
    })
    toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Talebiniz iletildi.', life: 3000 })
    yeniMasrafModal.value = false
    await tumunuYukle()
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err.message, life: 3000 })
  } finally {
    masrafGonderiliyor.value = false
  }
}

const gpsKonumAl = () => {
  if (!navigator.geolocation) {
    toast.add({ severity: 'warn', summary: 'Desteklenmiyor', detail: 'Tarayıcınız konum servisini desteklemiyor.', life: 3000 })
    return
  }
  konumAliniyor.value = true
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      konumAliniyor.value = false
      const lat = pos.coords.latitude.toFixed(5)
      const lng = pos.coords.longitude.toFixed(5)
      ziyaretKonum.value = `Enlem: ${lat}, Boylam: ${lng}`
      if (ziyaretForm.value.notlar) {
        ziyaretForm.value.notlar += `\n[📍 Konum: ${lat}, ${lng}]`
      } else {
        ziyaretForm.value.notlar = `[📍 Konum: ${lat}, ${lng}] `
      }
      toast.add({ severity: 'success', summary: 'Konum Alındı', detail: 'Coğrafi konum başarıyla eklendi.', life: 2500 })
    },
    (err) => {
      konumAliniyor.value = false
      toast.add({ severity: 'error', summary: 'Konum Hatası', detail: 'Konum bilgisi alınamadı: ' + err.message, life: 3000 })
    },
    { enableHighAccuracy: true, timeout: 10000 }
  )
}

const barkodlaUrunBul = () => {
  if (!barkodArama.value) return
  const kod = barkodArama.value.trim().toLowerCase()
  const bulunan = stoklar.value.find(s =>
    (s.barkod && s.barkod.toLowerCase() === kod) ||
    (s.stokKodu && s.stokKodu.toLowerCase() === kod)
  )
  if (bulunan) {
    yeniSiparisForm.value.stokId = bulunan.id
    hizliSiparisStokSecildi()
    toast.add({ severity: 'success', summary: 'Ürün Bulundu', detail: bulunan.ad, life: 2000 })
  } else {
    toast.add({ severity: 'warn', summary: 'Bulunamadı', detail: 'Bu barkoda ait ürün bulunamadı.', life: 2500 })
  }
}

const whatsappSiparisPaylas = (s) => {
  const musteri = s.cariHesapAdi || s.musteriAdi || 'Müşterimiz'
  const kod = s.siparisNo || s.id
  const tutar = formatPara(s.toplamTutar || s.genelToplam || 0)
  const mesaj = `Sayın ${musteri}, #${kod} numaralı ${tutar} tutarındaki siparişiniz hazırlanmaktadır. RasPel ERP Saha Ekibi.`
  const url = `https://api.whatsapp.com/send?text=${encodeURIComponent(mesaj)}`
  window.open(url, '_blank')
}

const ziyaretKaydet = async () => {
  if (!ziyaretForm.value.cariHesapId || !ziyaretForm.value.notlar) {
    toast.add({ severity: 'warn', summary: 'Eksik Bilgi', detail: 'Müşteri ve notlar zorunludur.', life: 3000 })
    return
  }
  ziyaretKaydediliyor.value = true
  try {
    const cari = cariHesaplar.value.find(c => c?.id === ziyaretForm.value.cariHesapId)
    await notAPI.create({
      baslik: `Saha Ziyareti: ${cari?.ad || 'Müşteri'} (${ziyaretForm.value.amac})`,
      icerik: ziyaretForm.value.notlar,
      kategori: 'SAHA_ZIYARET'
    })
    toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Ziyaret notu kaydedildi.', life: 3000 })
    ziyaretForm.value.notlar = ''
    ziyaretForm.value.cariHesapId = null
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err.message, life: 3000 })
  } finally {
    ziyaretKaydediliyor.value = false
  }
}

const hizliSiparisStokSecildi = () => {
  const stokId = yeniSiparisForm.value?.stokId
  if (!stokId) return
  const s = stoklar.value.find(item => item?.id === stokId)
  if (s) {
    yeniSiparisForm.value.birimFiyat = s.fiyat || s.satisFiyati || 0
  }
}

const hizliSiparisKaydet = async () => {
  if (!yeniSiparisForm.value?.cariHesapId || !yeniSiparisForm.value?.stokId) {
    toast.add({ severity: 'warn', summary: 'Eksik Bilgi', detail: 'Müşteri ve ürün seçimi zorunludur.', life: 3000 })
    return
  }
  siparisKaydediliyor.value = true
  try {
    const miktar = yeniSiparisForm.value.miktar || 1
    const fiyat = yeniSiparisForm.value.birimFiyat || 0
    await siparisAPI.create({
      cariHesapId: yeniSiparisForm.value.cariHesapId,
      tarih: new Date().toISOString().substring(0, 10),
      durum: 'BEKLIYOR',
      aciklama: 'Saha Siparişi',
      teslimatAdresi: yeniSiparisForm.value.adres || '',
      kalemler: [
        {
          stokId: yeniSiparisForm.value.stokId,
          miktar: miktar,
          birimFiyat: fiyat,
          tutar: miktar * fiyat
        }
      ]
    })
    toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Sipariş merkeze gönderildi.', life: 3000 })
    yeniSiparisModal.value = false
    await tumunuYukle()
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err.message, life: 3000 })
  } finally {
    siparisKaydediliyor.value = false
  }
}
</script>

<style scoped>
.saha-portali-sayfasi {
  padding-bottom: 40px;
}

.saha-header-card {
  background: linear-gradient(135deg, #1e40af 0%, #3b82f6 100%);
  color: white;
  padding: 1.25rem 1.5rem;
  border-radius: 1rem;
  margin-bottom: 1.25rem;
  box-shadow: 0 8px 20px -4px rgba(30, 64, 175, 0.3);
}

.saha-header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.875rem;
}

.user-avatar {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.35rem;
  backdrop-filter: blur(4px);
}

.user-name {
  font-size: 1.15rem;
  font-weight: 700;
  margin: 0 0 0.15rem 0;
}

.user-badge {
  font-size: 0.75rem;
  color: #dbeafe;
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.tab-badge {
  background: #ef4444;
  color: white;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 9999px;
}

.section-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.section-title-row h3 {
  font-size: 1.1rem;
  font-weight: 700;
  margin: 0;
  color: var(--text-primary);
}

.count-pill {
  font-size: 0.75rem;
  color: var(--text-secondary);
  background: var(--bg-muted, rgba(0, 0, 0, 0.05));
  padding: 3px 8px;
  border-radius: 6px;
  font-weight: 600;
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1rem;
}

.saha-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 1rem;
  padding: 1.15rem;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.65rem;
}

.order-code {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--primary-color, #3b82f6);
}

.customer-title {
  font-size: 1.05rem;
  font-weight: 700;
  margin: 0 0 0.5rem 0;
  color: var(--text-primary);
}

.address-box {
  display: flex;
  align-items: flex-start;
  gap: 0.4rem;
  font-size: 0.8rem;
  color: var(--text-secondary);
  margin-bottom: 0.85rem;
}

.address-box i {
  color: #ef4444;
  margin-top: 2px;
}

.amount-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--bg-muted, rgba(0,0,0,0.03));
  border-radius: 0.625rem;
  padding: 0.65rem 0.85rem;
  margin-bottom: 0.85rem;
}

.amount-box small {
  display: block;
  font-size: 0.7rem;
  color: var(--text-secondary);
}

.price-col {
  text-align: right;
}

.price-val {
  font-size: 1.05rem;
  font-weight: 800;
  color: #10b981;
}

.card-bottom-actions {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.65rem;
}

.call-btn, .map-btn, .whatsapp-btn {
  flex: 1;
  text-align: center;
  padding: 0.5rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 600;
  border-radius: 0.5rem;
  text-decoration: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  cursor: pointer;
}

.call-btn {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
  border: 1px solid rgba(16, 185, 129, 0.3);
}

.whatsapp-btn {
  background: rgba(37, 211, 102, 0.1);
  color: #25d366;
  border: 1px solid rgba(37, 211, 102, 0.3);
}

.map-btn {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.delivery-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: auto;
  padding-top: 0.5rem;
  border-top: 1px dashed var(--border);
}

.form-container-card {
  max-width: 600px;
  margin: 0 auto;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 1rem;
  padding: 1.5rem;
}

.form-header h3 {
  font-size: 1.15rem;
  font-weight: 700;
  margin: 0 0 0.25rem 0;
}

.form-header p {
  font-size: 0.8rem;
  color: var(--text-secondary);
  margin-bottom: 1.25rem;
}

.form-body {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-field label {
  display: block;
  font-size: 0.8rem;
  font-weight: 600;
  margin-bottom: 0.35rem;
  color: var(--text-secondary);
}

.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.empty-box {
  text-align: center;
  padding: 3.5rem 1rem;
  background: var(--bg-card);
  border: 1px dashed var(--border);
  border-radius: 1rem;
  color: var(--text-secondary);
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 0.75rem;
  opacity: 0.4;
}

.expense-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.expense-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 0.875rem;
  padding: 1rem 1.25rem;
}

.expense-type-badge {
  font-size: 0.75rem;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 6px;
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.expense-desc {
  font-size: 0.85rem;
  margin: 0.25rem 0;
  color: var(--text-primary);
}

.expense-date {
  font-size: 0.75rem;
  color: var(--text-secondary);
}

.expense-amount {
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--primary-color, #3b82f6);
}

.tab-pill-group {
  display: flex;
  border-radius: 0.625rem;
  border: 1px solid var(--border);
  overflow: hidden;
  margin-bottom: 0.75rem;
}

.pill-btn {
  flex: 1;
  padding: 0.5rem;
  font-size: 0.8rem;
  font-weight: 700;
  border: none;
  background: var(--bg-card);
  color: var(--text-secondary);
  cursor: pointer;
}

.pill-btn.active {
  background: var(--primary-color, #3b82f6);
  color: white;
}

.modal-form-content {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
  padding-top: 0.5rem;
}

.fade-in-section {
  animation: fadeIn 0.25s ease-in-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
