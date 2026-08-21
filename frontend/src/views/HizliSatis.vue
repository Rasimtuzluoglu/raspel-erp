<template>
  <div class="pos-container">
    <div class="pos-header">
      <div class="breadcrumb">
        <i class="pi pi-home" /> Anasayfa / POS / Yeni Satış
      </div>
      <div
        v-if="authStore?.kullanici"
        class="user-info"
      >
        <i class="pi pi-user" /> {{ authStore?.kullanici?.displayName || authStore?.kullanici?.username }}
      </div>
    </div>

    <div class="pos-body grid">
      <div class="col-8 pos-left">
        <Card class="filter-card">
          <template #title>
            <div class="filter-title">
              <span>Hızlı Filtreler</span>
              <Button
                label="Temizle"
                severity="secondary"
                size="small"
                @click="filtreleriTemizle"
              />
            </div>
          </template>
          <template #content>
            <div class="filter-row">
              <Dropdown
                v-model="filtreKategori"
                :options="kategoriler"
                option-label="ad"
                placeholder="Kategori"
                class="filter-select"
                show-clear
              />
              <Dropdown
                v-model="filtreArac"
                :options="aracListesi"
                placeholder="Araç"
                class="filter-select"
                show-clear
              />
            </div>
          </template>
        </Card>

        <div class="serial-search">
          <span class="p-input-icon-left">
            <i class="pi pi-search" />
            <InputText
              v-model="seriNoArama"
              placeholder="Seri No / Barkod ile ara..."
              class="w-full"
            />
          </span>
          <span class="p-input-icon-left">
            <i class="pi pi-search" />
            <InputText
              v-model="aramaMetni"
              placeholder="Ürün adı ile ara..."
              class="w-full"
            />
          </span>
          <Button
            icon="pi pi-camera"
            label="Barkod Oku"
            severity="secondary"
            outlined
            @click="scannerAcik = true"
          />
        </div>

        <div class="product-section">
          <div class="product-header">
            <h3>Mevcut Ürünler ({{ filtrelenmisUrunler.length }})</h3>
          </div>
          <div class="product-grid">
            <div
              v-for="u in filtrelenmisUrunler"
              :key="u.id"
              class="product-card"
              @click="sepeteEkle(u)"
            >
              <div class="product-img">
                <i class="pi pi-box" />
              </div>
              <Tag
                :value="
                  kritikStokMu(u)
                    ? 'Son ' + Math.floor(u.miktar) + ' adet'
                    : (u.miktar || 0) + ' ' + (u.birim || 'adet')
                "
                :severity="kritikStokMu(u) ? 'danger' : 'info'"
                class="stock-badge"
              />
              <div class="product-details">
                <span class="product-name">{{ u.ad }}</span>
                <span class="product-serial">{{ u.barkod || '-' }}</span>
                <span class="product-meta">{{ u.marka || '-' }} / {{ u.olcu || '-' }} / {{ u.birimHacim || '-' }} ft³</span>
                <span class="product-price">{{ formatCurrency(u.fiyat || u.satisFiyati || 0) }}</span>
              </div>
            </div>
            <div
              v-if="filtrelenmisUrunler.length === 0"
              class="empty-products"
            >
              <i class="pi pi-inbox" />
              <p>Ürün bulunamadı</p>
            </div>
          </div>
        </div>
      </div>

      <div class="col-4 pos-right">
        <Card class="customer-card">
          <template #title>
            Müşteri
          </template>
          <template #content>
            <div class="customer-field">
              <SelectButton
                v-model="musteriModu"
                :options="musteriModlari"
                option-label="label"
                option-value="value"
                class="w-full musteri-modu"
              />
              <template v-if="musteriModu === 'musteri'">
                <AutoComplete
                  v-model="musteriGiris"
                  :suggestions="musteriOnerileri"
                  option-label="ad"
                  placeholder="Müşteri ara (isim, vergi no, telefon)..."
                  class="w-full"
                  @complete="musteriAra($event)"
                  @option-select="musteriSec"
                >
                  <template #option="slotProps">
                    <div class="musteri-option">
                      {{ slotProps.option.ad }}
                      <span class="musteri-option-detay">{{
                        slotProps.option.vergiNo || slotProps.option.telefon
                      }}</span>
                    </div>
                  </template>
                </AutoComplete>
                <div
                  v-if="seciliMusteri"
                  class="secili-musteri-chip"
                >
                  <i class="pi pi-user" />
                  <span class="secili-musteri-ad">{{ seciliMusteri.ad }}</span>
                  <button
                    type="button"
                    class="secili-musteri-sil"
                    title="Müşteriyi Kaldır"
                    @click="musteriTemizle"
                  >
                    <i class="pi pi-times" />
                  </button>
                </div>
                <Button
                  label="+ Yeni"
                  severity="secondary"
                  size="small"
                  @click="yeniMusteriDialog = true"
                />
              </template>
            </div>
          </template>
        </Card>

        <div class="teslim-eden-alan">
          <label for="hizli-teslim-eden">Teslim Eden</label>
          <Dropdown
            id="hizli-teslim-eden"
            v-model="teslimEden"
            :options="personelSecenekleri"
            option-label="label"
            option-value="value"
            filter
            editable
            placeholder="Personel seçin veya yazın"
            class="w-full"
            :show-clear="true"
          >
            <template #option="s">
              <div class="personel-opsiyon">
                <i class="pi pi-user" />
                <span>{{ s.option.label }}</span>
              </div>
            </template>
          </Dropdown>
        </div>

        <div class="teslim-eden-alan">
          <label for="hizli-teslim-durum">Teslim Durumu</label>
          <Dropdown
            id="hizli-teslim-durum"
            v-model="teslimDurumu"
            :options="teslimDurumSecenekleri"
            option-label="label"
            option-value="value"
            class="w-full"
          />
        </div>

        <div class="teslim-eden-alan">
          <label for="hizli-teslim-not">Teslim Notu</label>
          <Textarea
            id="hizli-teslim-not"
            v-model="teslimNotu"
            rows="1"
            placeholder="Teslimat notu (isteğe bağlı)"
            class="w-full"
          />
        </div>

        <Card class="sepet-card">
          <template #title>
            <div class="sepet-header">
              <span>Sipariş Özeti ({{ sepet.length }})</span>
              <Button
                v-if="sepet.length"
                label="Temizle"
                icon="pi pi-trash"
                severity="danger"
                size="small"
                @click="sepet = []"
              />
            </div>
          </template>
          <template #content>
            <div
              v-if="sepet.length === 0"
              class="sepet-bos"
            >
              Sepete ürün ekleyin
            </div>
            <div
              v-for="(item, idx) in sepet"
              :key="idx"
              class="sepet-item"
            >
              <div class="sepet-ad">
                {{ item.ad }}
              </div>
              <div class="sepet-satir">
                <Button
                  icon="pi pi-minus"
                  rounded
                  text
                  severity="secondary"
                  size="small"
                  @click="miktarAzalt(idx)"
                />
                <span class="sepet-adet">{{ item.miktar }}</span>
                <Button
                  icon="pi pi-plus"
                  rounded
                  text
                  severity="secondary"
                  size="small"
                  @click="item.miktar++"
                />
                <select
                  v-model="item.fiyatTipi"
                  class="fiyat-tip-select"
                  @change="fiyatTipiDegisti(item)"
                >
                  <option value="perakende">
                    Perakende
                  </option>
                  <option value="toptan">
                    Toptan (-10%)
                  </option>
                  <option value="ozel">
                    Özel (-20%)
                  </option>
                </select>
                <input
                  v-model.number="item.fiyat"
                  type="number"
                  step="0.01"
                  class="fiyat-giris-input"
                  title="Birim Fiyatı Düzenle"
                >
                <span class="sepet-tutar">{{ formatCurrency(item.miktar * item.fiyat) }}</span>
                <Button
                  icon="pi pi-times"
                  rounded
                  text
                  severity="danger"
                  size="small"
                  @click="sepetSil(idx)"
                />
              </div>
            </div>
            <hr class="ozet-ayrac">
            <div class="ozet-satir">
              <span>Toplam Ft³</span>
              <span>{{ toplamFt3.toFixed(2) }} ft³</span>
            </div>
            <div class="ozet-satir">
              <span>İndirim</span>
              <div class="ozet-indirim">
                <SelectButton
                  v-model="indirimTipi"
                  :options="indirimTipleri"
                  option-label="label"
                  option-value="value"
                />
                <InputNumber
                  v-model="indirimDegeri"
                  :min="0"
                  :max="indirimTipi === 'yuzde' ? 100 : toplam"
                  :suffix="indirimTipi === 'yuzde' ? '%' : ' ₺'"
                  class="indirim-input"
                />
              </div>
            </div>
            <div class="ozet-satir ozet-genel">
              <span>Genel Toplam</span>
              <span class="genel-toplam-deger">{{ formatCurrency(genelToplam) }}</span>
            </div>
          </template>
        </Card>

        <Card class="odeme-card">
          <template #title>
            Ödeme
          </template>
          <template #content>
            <SelectButton
              v-model="odemeDurumu"
              :options="odemeTipleri"
              option-label="label"
              option-value="value"
              class="w-full"
            />
            <div
              v-if="odemeDurumu !== 'yok'"
              class="odenen-satir"
            >
              <label>Ödenen Tutar</label>
              <InputNumber
                v-model="odenenTutar"
                :min="0"
                :max="genelToplam"
                mode="currency"
                currency="TRY"
                locale="tr-TR"
                class="w-full"
              />
            </div>
            <div class="odeme-durum">
              <Tag
                :value="odemeDurumText"
                :severity="odemeDurumSeverity"
                class="w-full"
              />
            </div>
            <div
              v-if="kalanTutar > 0"
              class="odeme-kalan"
            >
              <span>Kalan:</span>
              <span class="kalan-deger">{{ formatCurrency(kalanTutar) }}</span>
            </div>
          </template>
        </Card>

        <Card class="fis-card">
          <template #title>
            <div class="fis-card-header">
              <span><i class="pi pi-print" /> Termal Fiş Ayarları</span>
              <div class="fis-ayarlar">
                <InputText
                  v-model="fisAltNotu"
                  placeholder="Fiş alt notu..."
                  size="small"
                  style="width: 160px"
                  title="Fiş altı özel mesajı"
                />
                <SelectButton
                  v-model="fisFiyatli"
                  :options="fisSecenekleri"
                  option-label="label"
                  option-value="value"
                  size="small"
                />
                <Button
                  label="Yazdır (F9)"
                  icon="pi pi-print"
                  size="small"
                  :disabled="sepet.length === 0"
                  @click="fisiYazdir"
                />
              </div>
            </div>
          </template>
          <template #content>
            <div class="fis-onizleme-kapsam">
              <div
                id="fisOnizleme"
                class="fis-onizleme"
              >
                <div class="fis-header">
                  <div class="fis-baslik">
                    {{ sirketAdi || 'RASPEL ERP' }}
                  </div>
                  <div class="fis-tarih">
                    {{ simdikiTarih }}
                  </div>
                  <div class="fis-fisno">
                    Fiş No: {{ fisNo || '-------' }}
                  </div>
                </div>
                <div
                  v-if="musteriAdi"
                  class="fis-musteri"
                >
                  <span>Müşteri: {{ musteriAdi }}</span>
                </div>
                <div class="fis-ayrac">
                  ---
                </div>
                <div class="fis-kalemler">
                  <div
                    v-for="i in sepet"
                    :key="i.id"
                    class="fis-kalem"
                  >
                    <div class="fis-kalem-ad">
                      {{ i.ad }} x{{ i.miktar }}
                    </div>
                    <div
                      v-if="fisFiyatli"
                      class="fis-kalem-tutar"
                    >
                      {{ formatCurrency(i.miktar * i.fiyat) }}
                    </div>
                  </div>
                </div>
                <div class="fis-ayrac">
                  ---
                </div>
                <template v-if="fisFiyatli">
                  <div class="fis-toplam">
                    <span>Ara Toplam</span>
                    <span>{{ formatCurrency(toplam) }}</span>
                  </div>
                  <div
                    v-if="indirimDegeri > 0"
                    class="fis-indirim"
                  >
                    <span>İndirim ({{ indirimTipi === 'yuzde' ? indirimDegeri + '%' : '' }})</span>
                    <span>-{{ formatCurrency(indirimTutari) }}</span>
                  </div>
                  <div class="fis-genel-toplam">
                    <span>GENEL TOPLAM</span>
                    <span class="fis-toplam-deger">{{ formatCurrency(genelToplam) }}</span>
                  </div>
                  <div class="fis-ayrac">
                    ---
                  </div>
                </template>
                <div class="fis-odeme">
                  <div
                    v-if="fisFiyatli"
                    class="fis-odeme-satir"
                  >
                    <span>Ödenen</span>
                    <span>{{ formatCurrency(odenenTutar) }}</span>
                  </div>
                  <div
                    v-if="fisFiyatli && kalanTutar > 0"
                    class="fis-odeme-satir"
                  >
                    <span>Kalan</span>
                    <span>{{ formatCurrency(kalanTutar) }}</span>
                  </div>
                  <div class="fis-odeme-satir fis-odeme-durum">
                    <span>Toplam Ürün</span>
                    <span>{{ sepet.length }}</span>
                  </div>
                  <div class="fis-odeme-satir fis-odeme-durum">
                    <span>Durum</span>
                    <span>{{ odemeDurumText }}</span>
                  </div>
                </div>
                <div class="fis-footer">
                  <div class="fis-ayrac">
                    ---
                  </div>
                  <div class="fis-tesekkur">
                    {{ fisAltNotu || 'Bizi tercih ettiginiz icin tesekkur ederiz!' }}
                  </div>
                  <div
                    v-if="authStore?.kullanici?.displayName"
                    class="fis-satici"
                  >
                    Islem Yapan: {{ authStore?.kullanici?.displayName }}
                  </div>
                </div>
              </div>
            </div>
          </template>
        </Card>

        <Button
          label="Satışı Tamamla"
          icon="pi pi-check"
          class="p-button-success w-full satis-buton"
          :loading="kaydediliyor"
          :disabled="sepet.length === 0 || (!anlikMusteri && !seciliMusteri)"
          @click="satisiTamamla"
        />
      </div>
    </div>

    <Dialog
      v-model:visible="yeniMusteriDialog"
      header="Yeni Cari Hesap Ekle"
      :modal="true"
      :style="{ width: '520px' }"
      class="yeni-musteri-dialog"
    >
      <div class="ym-form-grid">
        <div class="field full-width">
          <label for="ym-ad">Ad / Firma Adı <span class="required">*</span></label>
          <InputText
            id="ym-ad"
            v-model="yeniMusteri.ad"
            placeholder="Örn: Ahmet Yılmaz veya Yılmaz A.Ş."
            class="w-full"
          />
        </div>
        <div class="field">
          <label for="ym-telefon">Telefon</label>
          <InputText
            id="ym-telefon"
            v-model="yeniMusteri.telefon"
            placeholder="05XX XXX XX XX"
            class="w-full"
          />
        </div>
        <div class="field">
          <label for="ym-email">E-posta</label>
          <InputText
            id="ym-email"
            v-model="yeniMusteri.email"
            placeholder="ornek@domain.com"
            class="w-full"
          />
        </div>
        <div class="field">
          <label for="ym-vergi">Vergi / TC No</label>
          <InputText
            id="ym-vergi"
            v-model="yeniMusteri.vergiNo"
            placeholder="10 veya 11 haneli numara"
            class="w-full"
          />
        </div>
        <div class="field">
          <label for="ym-tur">Cari Türü</label>
          <Dropdown
            id="ym-tur"
            v-model="yeniMusteri.tur"
            :options="['Musteri', 'Tedarikci', 'Her Ikisi']"
            placeholder="MÜŞTERİ"
            class="w-full"
          />
        </div>
        <div class="field full-width">
          <label for="ym-adres">Adres</label>
          <Textarea
            id="ym-adres"
            v-model="yeniMusteri.adres"
            rows="2"
            placeholder="Fatura adresi..."
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer-btns">
          <Button
            label="İptal"
            icon="pi pi-times"
            class="p-button-text"
            @click="yeniMusteriDialog = false"
          />
          <Button
            label="Kaydet & Seç"
            icon="pi pi-check"
            class="p-button-primary"
            :loading="musteriKaydediliyor"
            @click="musteriKaydet"
          />
        </div>
      </template>
    </Dialog>

    <BarcodeScannerModal
      v-model:visible="scannerAcik"
      @scan="barkodTarandi"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useAuthStore } from '../stores/authStore.js'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { useStokStore } from '../stores/stokStore.js'
import BarcodeScannerModal from '../components/BarcodeScannerModal.vue'
import { useKategoriStore } from '../stores/kategoriStore.js'
import { faturaAPI, cariHesapAPI, personelAPI } from '../api/index.js'
import AutoComplete from 'primevue/autocomplete'
import SelectButton from 'primevue/selectbutton'
import { useKisayollar } from '../composables/useKisayollar.js'

const toast = useToast()
const toastBildirim = useToastBildirim()
const authStore = useAuthStore()
const cariHesapStore = useCariHesapStore()
const stokStore = useStokStore()
const kategoriStore = useKategoriStore()

useKisayollar({
  kaydet: () => satisiTamamla(),
  iptal: () => {
    if (yeniMusteriDialog.value) yeniMusteriDialog.value = false
  },
  yeni: () => {
    aramaMetni.value = ''
    sepet.value = []
  },
  yazdir: () => fisiYazdir()
})

const handlePosKeys = (e) => {
  if (e.key === 'F2') {
    e.preventDefault()
    sepet.value = []
    toast.add({ severity: 'info', summary: 'Kısayol F2', detail: 'Sepet temizlendi', life: 2000 })
  } else if (e.key === 'F4') {
    e.preventDefault()
    musteriModu.value = 'musteri'
    toast.add({ severity: 'info', summary: 'Kısayol F4', detail: 'Müşteri seçimi aktif', life: 2000 })
  } else if (e.key === 'F9') {
    e.preventDefault()
    odemeDurumu.value = 'tam'
    if (sepet.value.length && (anlikMusteri.value || seciliMusteri.value)) satisiTamamla()
  } else if (e.key === 'F10') {
    e.preventDefault()
    odemeDurumu.value = 'kismi'
    if (sepet.value.length && (anlikMusteri.value || seciliMusteri.value)) satisiTamamla()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handlePosKeys)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handlePosKeys)
})

const sirketAdi = computed(() => authStore.sirketAdi || '')

const aramaMetni = ref('')
const seriNoArama = ref('')
const scannerAcik = ref(false)

const barkodTarandi = (barkod) => {
  scannerAcik.value = false
  if (!barkod) return
  const urun = stokStore.stoklar.find((s) => s.barkod === barkod)
  if (urun) {
    sepeteEkle(urun)
    toast.add({ severity: 'success', summary: 'Ürün Eklendi', detail: urun.ad, life: 2000 })
  } else {
    seriNoArama.value = barkod
    toast.add({ severity: 'warn', summary: 'Bulunamadı', detail: `"${barkod}" barkodlu ürün bulunamadı`, life: 3000 })
  }
}

const filtreKategori = ref(null)
const filtreArac = ref(null)

const seciliMusteri = ref(null)
const musteriGiris = ref('')
const teslimEden = ref('')
const teslimDurumu = ref('BEKLIYOR')
const teslimNotu = ref('')
const personelListesi = ref([])
const musteriModu = ref('musteri')
const musteriModlari = ref([
  { label: 'Perakende', value: 'perakende', icon: 'pi pi-shopping-cart' },
  { label: 'Müşteri', value: 'musteri', icon: 'pi pi-users' }
])
const anlikMusteri = computed(() => musteriModu.value === 'perakende')

watch(musteriModu, (mod) => {
  if (mod === 'perakende') {
    seciliMusteri.value = null
    musteriGiris.value = ''
  }
})
const musteriOnerileri = ref([])
const yeniMusteriDialog = ref(false)
const yeniMusteri = ref({ ad: '', telefon: '', email: '', adres: '', vergiNo: '', tur: 'Musteri' })
const musteriKaydediliyor = ref(false)

const sepet = ref([])
const kaydediliyor = ref(false)
const fisNo = ref('')

const fisFiyatli = ref(true)
const fisSecenekleri = ref([
  { label: 'Fiyatlı', value: true },
  { label: 'Fiyatsız', value: false }
])
const fisAltNotu = ref(localStorage.getItem('raspel_fis_notu') || 'Bizi tercih ettiğiniz için teşekkür ederiz!')
watch(fisAltNotu, (v) => localStorage.setItem('raspel_fis_notu', v || ''))

const indirimTipi = ref('tutar')
const indirimTipleri = ref([
  { label: '₺', value: 'tutar' },
  { label: '%', value: 'yuzde' }
])
const indirimDegeri = ref(0)

const odemeDurumu = ref('tam')
const odemeTipleri = ref([
  { label: 'Tam Ödeme', value: 'tam' },
  { label: 'Yarım Ödeme', value: 'yarim' },
  { label: 'Ödeme Yok', value: 'yok' }
])
const odenenTutar = ref(0)

const kategoriler = computed(() => kategoriStore.kategoriler || [])

const aracListesi = computed(() => {
  const araclar = new Set()
  stokStore.stoklar.forEach((s) => {
    if (s.marka) araclar.add(s.marka)
  })
  return [...araclar].sort()
})

const toplam = computed(() => sepet.value.reduce((t, i) => t + i.miktar * i.fiyat, 0))

const toplamFt3 = computed(() =>
  sepet.value.reduce((t, i) => {
    const hacim = i.birimHacim || 1
    return t + i.miktar * hacim
  }, 0)
)

const indirimTutari = computed(() => {
  if (indirimDegeri.value <= 0) return 0
  if (indirimTipi.value === 'yuzde') return toplam.value * (Math.min(indirimDegeri.value, 100) / 100)
  return Math.min(indirimDegeri.value, toplam.value)
})

const genelToplam = computed(() => Math.max(0, toplam.value - indirimTutari.value))

watch(odemeDurumu, (v) => {
  if (v === 'tam') odenenTutar.value = genelToplam.value
  else if (v === 'yarim') odenenTutar.value = genelToplam.value / 2
  else odenenTutar.value = 0
})

watch(genelToplam, () => {
  if (odemeDurumu.value === 'tam') odenenTutar.value = genelToplam.value
  else if (odemeDurumu.value === 'yarim') odenenTutar.value = genelToplam.value / 2
})

const kalanTutar = computed(() => Math.max(0, genelToplam.value - odenenTutar.value))

const odemeDurumText = computed(() => {
  if (odemeDurumu.value === 'yok' || odenenTutar.value === 0) return 'Ödenmedi'
  if (odenenTutar.value >= genelToplam.value) return 'Tamamen Ödendi'
  return 'Kısmi Ödendi'
})

const odemeDurumEnum = computed(() => {
  if (odemeDurumu.value === 'yok' || odenenTutar.value === 0) return 'ODENMEDI'
  if (odenenTutar.value >= genelToplam.value) return 'ODENDI'
  return 'KISMI_ODENDI'
})

const odemeDurumSeverity = computed(() => {
  if (odemeDurumu.value === 'yok' || odenenTutar.value === 0) return 'danger'
  if (odenenTutar.value >= genelToplam.value) return 'success'
  return 'warning'
})

const musteriAdi = computed(() => {
  if (anlikMusteri.value) return 'Anlık Müşteri'
  return seciliMusteri.value?.ad || ''
})

const filtrelenmisUrunler = computed(() => {
  let list = stokStore.stoklar || []

  if (filtreKategori.value) {
    list = list.filter((u) => u.kategori === filtreKategori.value.ad)
  }

  if (filtreArac.value) {
    list = list.filter((u) => u.marka === filtreArac.value)
  }

  if (seriNoArama.value) {
    const q = seriNoArama.value.toLowerCase()
    list = list.filter((u) => u.barkod?.toLowerCase().includes(q) || u.seriNo?.toLowerCase().includes(q))
  }

  if (aramaMetni.value) {
    const q = aramaMetni.value.toLowerCase()
    list = list.filter(
      (u) =>
        u.ad?.toLowerCase().includes(q) || u.stokKodu?.toLowerCase().includes(q) || u.barkod?.toLowerCase().includes(q)
    )
  }

  return list.slice(0, 100)
})

const kritikStokMu = (u) => {
  if (!u?.miktar) return false
  if (u.minMiktar != null && u.miktar <= u.minMiktar) return true
  return u.miktar <= 10
}

const simdikiTarih = computed(() => {
  const d = new Date()
  return d.toLocaleDateString('tr-TR') + ' ' + d.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })
})

const formatCurrency = (v) =>
  v != null ? new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v) : '0,00 ₺'

onMounted(async () => {
  try {
    await Promise.all([
      cariHesapStore.getAllCariHesaplar(),
      stokStore.getAll(),
      kategoriStore.getAllKategoriler(),
      personelListesiniYukle()
    ])
  } catch (e) {
    console.error('Yukleme hatasi', e)
  }
})

const personelSecenekleri = computed(() =>
  personelListesi.value
    .filter((p) => p.aktif !== false)
    .map((p) => ({ label: `${p.ad || ''} ${p.soyad || ''}`.trim(), value: `${p.ad || ''} ${p.soyad || ''}`.trim() }))
)

const teslimDurumSecenekleri = [
  { label: 'Bekliyor', value: 'BEKLIYOR' },
  { label: 'Yolda', value: 'YOLDA' },
  { label: 'Teslim Edildi', value: 'TESLIM_EDILDI' }
]

const personelListesiniYukle = async () => {
  try {
    const r = await personelAPI.getAll({ size: 500 })
    personelListesi.value = r.data?.content || r.data || []
  } catch {
    personelListesi.value = []
  }
}

const filtreleriTemizle = () => {
  filtreKategori.value = null
  filtreArac.value = null
  seriNoArama.value = ''
  aramaMetni.value = ''
}

const musteriAra = (event) => {
  const query = event.query
  if (!query || query.length < 1) {
    musteriOnerileri.value = cariHesapStore.cariHesaplar.slice(0, 20)
    return
  }
  const q = query.toLowerCase()
  musteriOnerileri.value = cariHesapStore.cariHesaplar
    .filter(
      (c) => c.ad?.toLowerCase().includes(q) || c.vergiNo?.toLowerCase().includes(q) || c.telefon?.includes(query)
    )
    .slice(0, 20)
}

const musteriSec = (event) => {
  seciliMusteri.value = event.value
  musteriGiris.value = ''
}

const musteriTemizle = () => {
  seciliMusteri.value = null
  musteriGiris.value = ''
}

const musteriKaydet = async () => {
  if (!yeniMusteri.value.ad) {
    toastBildirim.uyari('Ad / Firma adı zorunludur')
    return
  }
  musteriKaydediliyor.value = true
  try {
    const r = await cariHesapAPI.create(yeniMusteri.value)
    seciliMusteri.value = r.data
    musteriGiris.value = ''
    yeniMusteriDialog.value = false
    yeniMusteri.value = { ad: '', telefon: '', email: '', adres: '', vergiNo: '', tur: 'Musteri' }
    toastBildirim.basarili('Cari hesap oluşturuldu')
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || 'Kayıt başarısız')
  }
  musteriKaydediliyor.value = false
}

const sepeteEkle = (u) => {
  const varOlan = sepet.value.find((i) => i.id === u.id)
  if (varOlan) {
    varOlan.miktar++
  } else {
    const stdFiyat = u.fiyat || u.satisFiyati || 0
    const toptan = u.toptanFiyati || Math.round(stdFiyat * 0.9 * 100) / 100
    const ozel = u.ozelFiyati || Math.round(stdFiyat * 0.8 * 100) / 100
    sepet.value.push({
      id: u.id,
      ad: u.ad,
      stokKodu: u.stokKodu,
      barkod: u.barkod,
      miktar: 1,
      fiyat: stdFiyat,
      perakendeFiyati: stdFiyat,
      toptanFiyati: toptan,
      ozelFiyati: ozel,
      fiyatTipi: 'perakende',
      birim: u.birim || 'adet',
      birimHacim: u.birimHacim || 1
    })
  }
}

const fiyatTipiDegisti = (item) => {
  if (item.fiyatTipi === 'toptan') item.fiyat = item.toptanFiyati
  else if (item.fiyatTipi === 'ozel') item.fiyat = item.ozelFiyati
  else item.fiyat = item.perakendeFiyati
}

const miktarAzalt = (idx) => {
  if (sepet.value[idx].miktar > 1) sepet.value[idx].miktar--
  else sepetSil(idx)
}

const sepetSil = (idx) => {
  sepet.value.splice(idx, 1)
}

const fisiYazdir = () => {
  if (!sepet.value.length) return
  fisNo.value = 'F-' + Date.now().toString(36).toUpperCase()

  const fiyatli = fisFiyatli.value
  const kalemHtml = sepet.value
    .map((i) => {
      const ad = escapeHtml(i.ad || '')
      const satir = `<div class="satir"><span class="ad">${ad} x${i.miktar}</span>${fiyatli ? `<span class="tutar">${formatCurrency(i.miktar * i.fiyat)}</span>` : ''}</div>`
      return satir
    })
    .join('')

  const ozetHtml = fiyatli
    ? `
    <div class="ayrac">- - - - - - - - - - - - - -</div>
    <div class="satir"><span class="ad">Ara Toplam</span><span class="tutar">${formatCurrency(toplam.value)}</span></div>
    ${indirimDegeri.value > 0 ? `<div class="satir"><span class="ad">İndirim${indirimTipi.value === 'yuzde' ? ' (' + indirimDegeri.value + '%)' : ''}</span><span class="tutar">-${formatCurrency(indirimTutari.value)}</span></div>` : ''}
    <div class="satir genel"><span class="ad">GENEL TOPLAM</span><span class="tutar">${formatCurrency(genelToplam.value)}</span></div>
    <div class="ayrac">- - - - - - - - - - - - - -</div>
    <div class="satir"><span class="ad">Ödenen</span><span class="tutar">${formatCurrency(odenenTutar.value)}</span></div>
    ${kalanTutar.value > 0 ? `<div class="satir"><span class="ad">Kalan</span><span class="tutar">${formatCurrency(kalanTutar.value)}</span></div>` : ''}
  `
    : ''

  const musteriHtml = musteriAdi.value ? `<div class="musteri">Müşteri: ${escapeHtml(musteriAdi.value)}</div>` : ''

  const html = `<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Fiş Önizleme</title>
<style>
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { font-family: 'Courier New', monospace; width: 80mm; margin: 0 auto; color: #000; font-size: 12px; }
  .aracubuk {
    position: fixed; top: 0; left: 0; right: 0; z-index: 10;
    width: 100%; padding: 10px; text-align: center;
    background: #1e293b; box-shadow: 0 2px 8px rgba(0,0,0,0.2);
  }
  .aracubuk button {
    font-family: Arial, sans-serif; font-size: 14px; font-weight: 600;
    padding: 10px 24px; border: none; border-radius: 6px; cursor: pointer;
    background: #3b82f6; color: #fff; margin: 0 4px;
  }
  .aracubuk button.iptal { background: #475569; }
  .fis { padding: 6px 4px; margin-top: 52px; }
  .baslik { text-align: center; font-size: 14px; font-weight: bold; margin-bottom: 4px; }
  .tarih, .fisno { text-align: center; font-size: 10px; margin-top: 2px; }
  .musteri { margin-top: 6px; font-size: 11px; }
  .ayrac { text-align: center; color: #555; margin: 4px 0; letter-spacing: 1px; }
  .satir { display: flex; justify-content: space-between; padding: 2px 0; }
  .satir .ad { flex: 1; white-space: pre-wrap; word-break: break-word; padding-right: 6px; }
  .satir .tutar { white-space: nowrap; }
  .satir.genel { border-top: 2px solid #000; font-weight: bold; padding-top: 4px; margin-top: 4px; }
  .tesekkur { text-align: center; margin-top: 8px; font-size: 10px; }
  @media print {
    .aracubuk { display: none !important; }
    .fis { margin-top: 0; }
  }
</style>
</head>
<body>
  <div class="aracubuk">
    <button onclick="window.print()">Yazdır</button>
    <button class="iptal" onclick="window.close()">Kapat</button>
  </div>
  <div class="fis">
    <div class="baslik">${escapeHtml(sirketAdi.value || 'RASPEL ERP')}</div>
    <div class="tarih">${simdikiTarih.value}</div>
    <div class="fisno">Fiş No: ${fisNo.value}</div>
    ${musteriHtml}
    ${teslimEden.value ? `<div class="musteri">Teslim Eden: ${escapeHtml(teslimEden.value)}</div>` : ''}
    ${teslimDurumu.value && teslimDurumu.value !== 'BEKLIYOR' ? `<div class="musteri">Teslim: ${teslimDurumEtiketi(teslimDurumu.value)}</div>` : ''}
    ${teslimNotu.value ? `<div class="musteri">Not: ${escapeHtml(teslimNotu.value)}</div>` : ''}
    <div class="ayrac">- - - - - - - - - - - - - -</div>
    ${kalemHtml}
    ${ozetHtml}
    <div class="satir"><span class="ad">Toplam Ürün</span><span class="tutar">${sepet.value.length}</span></div>
    <div class="satir"><span class="ad">Durum</span><span class="tutar">${odemeDurumText.value}</span></div>
    <div class="ayrac">- - - - - - - - - - - - - -</div>
    <div class="tesekkur">Islem Yapan: ${escapeHtml(authStore?.kullanici?.displayName || '-')}</div>
    <div class="tesekkur">Iyi gunler dileriz</div>
  </div>
</body>
</html>`

  const win = window.open('', '_blank', 'width=400,height=600')
  if (!win) {
    toastBildirim.hata('Pencere engellendi. Pop-up engelleyiciyi kapatın.')
    return
  }
  win.document.open()
  win.document.write(html)
  win.document.close()
  setTimeout(() => {
    try {
      win.focus()
      win.print()
    } catch (e) {
      console.error('Termal yazıcı hatası:', e)
    }
  }, 300)
}

const escapeHtml = (metin) => {
  return String(metin ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const teslimDurumEtiketi = (d) => ({ BEKLIYOR: 'Bekliyor', YOLDA: 'Yolda', TESLIM_EDILDI: 'Teslim Edildi' })[d] || d

const satisiTamamla = async () => {
  if (!anlikMusteri.value && !seciliMusteri.value) return
  if (sepet.value.length === 0) return
  kaydediliyor.value = true
  try {
    await faturaAPI.create({
      cariHesapId: anlikMusteri.value ? null : seciliMusteri.value.id,
      cariHesapAdi: anlikMusteri.value ? 'Anlik Musteri' : seciliMusteri.value.ad,
      tur: 'SATIS',
      durum: 'KESILDI',
      tarih: new Date().toISOString().split('T')[0],
      teslimEden: teslimEden.value || null,
      teslimDurumu: teslimDurumu.value || 'BEKLIYOR',
      teslimNotu: teslimNotu.value || null,
      aciklama: 'Hizli Satis',
      araToplam: toplam.value,
      indirim: indirimTutari.value,
      genelToplam: genelToplam.value,
      odenenTutar: odenenTutar.value,
      odemeDurumu: odemeDurumEnum.value,
      kalemler: sepet.value.map((i) => ({
        stokId: i.id,
        aciklama: i.ad,
        adet: i.miktar,
        birimFiyat: i.fiyat,
        kdvOrani: 20,
        tutar: Math.round(i.miktar * i.fiyat * 100) / 100
      }))
    })
    toastBildirim.basarili(`Satış tamamlandı - ${formatCurrency(genelToplam.value)}`)
    try {
      fisiYazdir()
    } catch {
      /* empty */
    }
    sepet.value = []
    seciliMusteri.value = null
    musteriGiris.value = ''
    teslimEden.value = ''
    teslimDurumu.value = 'BEKLIYOR'
    teslimNotu.value = ''
    musteriModu.value = 'musteri'
    indirimDegeri.value = 0
    odemeDurumu.value = 'tam'
    odenenTutar.value = 0
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || 'Satış başarısız')
  }
  kaydediliyor.value = false
}
</script>

<style scoped>
.pos-container {
  padding: 20px;
  height: calc(100vh - 80px);
  overflow-y: auto;
}
.pos-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.breadcrumb {
  font-size: 13px;
  color: var(--text-muted);
}
.breadcrumb i {
  margin-right: 4px;
}
.user-info {
  font-size: 13px;
  color: var(--text-secondary);
}
.user-info i {
  margin-right: 4px;
}

.pos-body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.pos-left {
  flex: 0 0 66.666%;
  max-width: 66.666%;
}
.pos-right {
  flex: 0 0 33.333%;
  max-width: 33.333%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-card :deep(.p-card-content) {
  padding-top: 0;
}
.filter-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.filter-row {
  display: flex;
  gap: 8px;
}
.filter-select {
  flex: 1;
}

.serial-search {
  margin: 8px 0;
}
.serial-search :deep(.p-inputtext) {
  width: 100%;
  padding-left: 42px;
}

.product-section {
  margin-top: 4px;
}
.product-header h3 {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: var(--text-primary);
}
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 10px;
  max-height: calc(100vh - 280px);
  overflow-y: auto;
  padding-bottom: 8px;
}

.product-card {
  position: relative;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.15s;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}
.product-card:hover {
  border-color: var(--accent);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}
.product-img {
  width: 64px;
  height: 64px;
  background: var(--surface-ground);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}
.product-img i {
  font-size: 28px;
  color: var(--text-muted);
}
.stock-badge {
  position: absolute;
  top: 8px;
  right: 8px;
}
.product-details {
  width: 100%;
}
.product-name {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 2px;
}
.product-serial {
  display: block;
  font-size: 11px;
  color: var(--text-muted);
}
.product-meta {
  display: block;
  font-size: 10px;
  color: var(--text-muted);
  margin: 2px 0;
}
.product-price {
  display: inline-block;
  font-size: 15px;
  font-weight: 700;
  color: var(--accent);
  margin-top: 4px;
  padding: 2px 10px;
  background: rgba(59, 130, 246, 0.1);
  border-radius: 12px;
}
.empty-products {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
}
.empty-products i {
  font-size: 36px;
  display: block;
  margin-bottom: 8px;
}

.customer-card :deep(.p-card-content) {
  padding-top: 0;
}
.customer-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.teslim-eden-alan {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.teslim-eden-alan label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.personel-opsiyon {
  display: flex;
  align-items: center;
  gap: 8px;
}
.personel-opsiyon i {
  font-size: 12px;
  color: var(--text-muted);
}
.anlik-musteri {
  margin-bottom: 4px;
}
.musteri-modu {
  display: flex;
}
.musteri-modu .p-selectbutton .p-button {
  flex: 1;
  justify-content: center;
}
.musteri-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.musteri-option-detay {
  font-size: 11px;
  color: var(--text-muted);
}
.secili-musteri-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(59, 130, 246, 0.12);
  border: 1px solid rgba(59, 130, 246, 0.25);
  border-radius: 8px;
  padding: 6px 10px;
  font-size: 13px;
}
.secili-musteri-chip i {
  color: #60a5fa;
  font-size: 14px;
}
.secili-musteri-ad {
  flex: 1;
  color: var(--text-primary);
  font-weight: 500;
}
.secili-musteri-sil {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 13px;
  padding: 2px;
}
.secili-musteri-sil:hover {
  color: #f87171;
}

.sepet-card {
  max-height: 350px;
  overflow-y: auto;
}
.sepet-card :deep(.p-card-content) {
  padding-top: 0;
}
.sepet-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.sepet-bos {
  text-align: center;
  padding: 20px;
  color: var(--text-muted);
  font-size: 13px;
}
.sepet-item {
  padding: 6px 0;
  border-bottom: 1px solid var(--border);
}
.sepet-item:last-child {
  border-bottom: none;
}
.sepet-ad {
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 2px;
}
.sepet-satir {
  display: flex;
  align-items: center;
  gap: 4px;
}
.sepet-adet {
  width: 22px;
  text-align: center;
  font-weight: 700;
  font-size: 13px;
}
.sepet-birimfiyat {
  font-size: 11px;
  color: var(--text-muted);
  margin-left: auto;
}
.sepet-tutar {
  font-size: 13px;
  font-weight: 700;
  min-width: 60px;
  text-align: right;
}

.ozet-satir {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 13px;
}
.ozet-indirim {
  display: flex;
  align-items: center;
  gap: 6px;
}
.indirim-input {
  width: 100px;
}
.ozet-ayrac {
  border: none;
  border-top: 1px solid var(--border);
  margin: 6px 0;
}
.ozet-genel {
  border-top: 2px solid var(--border);
  margin-top: 4px;
  padding-top: 8px;
}
.genel-toplam-deger {
  font-size: 18px;
  font-weight: 800;
  color: var(--accent);
}

.odeme-card :deep(.p-card-content) {
  padding-top: 0;
}
.odeme-card :deep(.p-selectbutton) {
  display: flex;
}
.odeme-card :deep(.p-selectbutton .p-button) {
  flex: 1;
  font-size: 12px;
}
.odenen-satir {
  margin-top: 8px;
}
.odenen-satir label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 4px;
}
.odeme-durum {
  margin-top: 8px;
}
.odeme-durum :deep(.p-tag) {
  justify-content: center;
}
.odeme-kalan {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 13px;
}
.kalan-deger {
  font-weight: 700;
  color: var(--accent);
}

.fis-card :deep(.p-card-content) {
  padding: 0;
}
.fis-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  gap: 8px;
  flex-wrap: wrap;
}
.fis-ayarlar {
  display: flex;
  align-items: center;
  gap: 8px;
}
.fis-ayarlar .p-selectbutton .p-button {
  padding: 4px 10px;
  font-size: 11px;
}
.fis-onizleme-kapsam {
  overflow-x: auto;
  padding: 12px;
  background: var(--bg-secondary);
  border-radius: 0 0 8px 8px;
}
.fis-onizleme {
  width: 80mm;
  margin: 0 auto;
  padding: 12px 8px;
  background: white;
  color: black;
  font-size: 11px;
  font-family: 'Courier New', monospace;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}
.fis-header {
  text-align: center;
  margin-bottom: 6px;
}
.fis-baslik {
  font-size: 13px;
  font-weight: 700;
}
.fis-tarih {
  font-size: 10px;
  margin-top: 2px;
}
.fis-fisno {
  font-size: 10px;
  margin-top: 1px;
  color: #555;
}
.fis-musteri {
  margin-bottom: 4px;
  font-size: 10px;
}
.fis-ayrac {
  text-align: center;
  color: #999;
  margin: 3px 0;
  letter-spacing: 2px;
}
.fis-kalemler {
}
.fis-kalem {
  display: flex;
  justify-content: space-between;
  padding: 2px 0;
}
.fis-kalem-ad {
}
.fis-kalem-tutar {
  white-space: nowrap;
}
.fis-toplam {
  display: flex;
  justify-content: space-between;
  padding: 3px 0;
  font-size: 12px;
}
.fis-indirim {
  display: flex;
  justify-content: space-between;
  padding: 2px 0;
  color: #c00;
  font-size: 11px;
}
.fis-genel-toplam {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  border-top: 2px solid #000;
  font-weight: 700;
  font-size: 13px;
}
.fis-toplam-deger {
}
.fis-odeme {
  margin-top: 4px;
}
.fis-odeme-satir {
  display: flex;
  justify-content: space-between;
  padding: 2px 0;
  font-size: 10px;
}
.fis-odeme-durum {
  font-weight: 600;
}
.fis-footer {
  text-align: center;
  margin-top: 4px;
}
.fis-tesekkur {
  font-size: 10px;
  color: #555;
}
.fis-satici {
  font-size: 9px;
  color: #888;
  margin-top: 2px;
  text-align: center;
}

.satis-buton {
  margin-top: 4px;
}

.ym-form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.ym-form-grid .full-width {
  grid-column: span 2;
}
.dialog-footer-btns {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  width: 100%;
}

.field {
  margin-bottom: 12px;
}
.field label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
}
.required {
  color: #f87171;
}

.fiyat-tip-select {
  background: var(--bg-primary);
  color: var(--text-primary);
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 11px;
  padding: 2px 4px;
  outline: none;
}
.fiyat-giris-input {
  width: 70px;
  background: var(--bg-primary);
  color: var(--text-primary);
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 12px;
  padding: 2px 4px;
  text-align: right;
  outline: none;
}

@media (max-width: 1100px) {
  .pos-body {
    flex-direction: column;
  }
  .pos-left,
  .pos-right {
    flex: 0 0 100%;
    max-width: 100%;
  }
  .product-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  }
}
</style>

<style>
@media print {
  body * {
    visibility: hidden;
  }
  #fisOnizleme,
  #fisOnizleme * {
    visibility: visible;
  }
  #fisOnizleme {
    position: fixed;
    top: 0;
    left: 0;
    width: 80mm;
    padding: 10mm;
    background: white;
    color: black;
    font-size: 12px;
    font-family: 'Courier New', monospace;
  }
}
</style>
