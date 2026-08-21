<template>
  <div class="saha-portali-sayfasi">
    <!-- Mobil Header & Profil -->
    <div class="saha-header mb-4 p-4 rounded-xl">
      <div class="flex justify-between items-center flex-wrap gap-3">
        <div class="flex items-center gap-3">
          <div class="saha-avatar">
            <i class="pi pi-user text-2xl text-white" />
          </div>
          <div>
            <h2 class="text-lg font-bold text-white mb-0.5">
              {{ authStore.kullanici?.displayName || authStore.kullanici?.username || 'Saha Personeli' }}
            </h2>
            <span class="text-xs text-blue-100 font-medium">
              <i class="pi pi-compass mr-1" /> Saha & Personel Mobil Portalı
            </span>
          </div>
        </div>

        <div class="flex items-center gap-2">
          <Button
            label="Hızlı Sipariş Al"
            icon="pi pi-plus"
            class="p-button-warning p-button-sm"
            @click="yeniSiparisModal = true"
          />
          <Button
            icon="pi pi-refresh"
            class="p-button-text text-white p-button-sm"
            :loading="yukleniyor"
            @click="tumunuYukle"
          />
        </div>
      </div>
    </div>

    <!-- Mobil Navigasyon Sekmeleri -->
    <div class="saha-sekmeler flex gap-2 mb-4 overflow-x-auto pb-1">
      <button
        type="button"
        :class="['saha-sekme-btn', { aktif: aktifSekme === 'siparisler' }]"
        @click="aktifSekme = 'siparisler'"
      >
        <i class="pi pi-shopping-bag mr-1.5" />
        Sipariş & Teslimat
        <span
          v-if="bekleyenSiparisSayisi > 0"
          class="badge-sayi"
        >{{ bekleyenSiparisSayisi }}</span>
      </button>
      <button
        type="button"
        :class="['saha-sekme-btn', { aktif: aktifSekme === 'izinler' }]"
        @click="aktifSekme = 'izinler'"
      >
        <i class="pi pi-calendar mr-1.5" />
        İzin Taleplerim
      </button>
      <button
        type="button"
        :class="['saha-sekme-btn', { aktif: aktifSekme === 'masraflar' }]"
        @click="aktifSekme = 'masraflar'"
      >
        <i class="pi pi-wallet mr-1.5" />
        Masraf & Avans
      </button>
      <button
        type="button"
        :class="['saha-sekme-btn', { aktif: aktifSekme === 'ziyaret' }]"
        @click="aktifSekme = 'ziyaret'"
      >
        <i class="pi pi-map-marker mr-1.5" />
        Ziyaret & Check-in
      </button>
    </div>

    <!-- SEKME 1: SİPARİŞ & TESLİMAT YÖNETİMİ -->
    <div
      v-if="aktifSekme === 'siparisler'"
      class="sekme-icerik"
    >
      <div class="flex justify-between items-center mb-3">
        <h3 class="text-base font-bold text-gray-800 dark:text-gray-100">
          <i class="pi pi-truck text-primary mr-1" /> Aktif Saha Siparişleri
        </h3>
        <span class="text-xs text-muted">{{ siparisler.length }} Sipariş</span>
      </div>

      <div
        v-if="siparisler.length > 0"
        class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4"
      >
        <div
          v-for="s in siparisler"
          :key="s.id"
          class="siparis-kart p-4 rounded-xl border bg-white dark:bg-gray-800 shadow-sm flex flex-col justify-between"
        >
          <div>
            <div class="flex justify-between items-start mb-2">
              <span class="font-bold text-primary text-sm">#{{ s.siparisNo || s.id }}</span>
              <Tag
                :value="s.durum || 'BEKLIYOR'"
                :severity="siparisDurumSeverity(s.durum)"
              />
            </div>
            <h4 class="font-bold text-base text-gray-900 dark:text-gray-100 mb-1">
              {{ s.cariHesapAdi || s.musteriAdi || 'Müşteri' }}
            </h4>
            <p
              v-if="s.teslimatAdresi"
              class="text-xs text-muted mb-2 flex items-center gap-1"
            >
              <i class="pi pi-map-marker text-red-500" /> {{ s.teslimatAdresi }}
            </p>
            <div class="text-xs text-gray-600 dark:text-gray-300 mb-3 bg-gray-50 dark:bg-gray-700/50 p-2 rounded">
              <div class="flex justify-between">
                <span>Tarih:</span>
                <strong>{{ formatDate(s.tarih) }}</strong>
              </div>
              <div class="flex justify-between mt-1 text-sm font-bold text-emerald-600 dark:text-emerald-400">
                <span>Tutar:</span>
                <span>{{ formatCurrency(s.toplamTutar || s.genelToplam || 0) }} ₺</span>
              </div>
            </div>
          </div>

          <!-- Kart Aksiyonları -->
          <div class="pt-3 border-t flex flex-col gap-2">
            <div class="flex gap-2">
              <a
                v-if="s.telefon"
                :href="'tel:' + s.telefon"
                class="flex-1 text-center py-1.5 px-3 bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 rounded-lg text-xs font-bold border border-emerald-200"
              >
                <i class="pi pi-phone mr-1" /> Müşteriyi Ara
              </a>
              <a
                v-if="s.teslimatAdresi"
                :href="'https://maps.google.com/?q=' + encodeURIComponent(s.teslimatAdresi)"
                target="_blank"
                class="flex-1 text-center py-1.5 px-3 bg-blue-50 dark:bg-blue-950/40 text-blue-600 rounded-lg text-xs font-bold border border-blue-200"
              >
                <i class="pi pi-map mr-1" /> Haritada Aç
              </a>
            </div>

            <div class="flex gap-2 mt-1">
              <Button
                v-if="s.durum !== 'TESLIM_EDILDI'"
                label="Durum Güncelle"
                icon="pi pi-sync"
                class="p-button-outlined p-button-sm flex-1"
                @click="durumSecModalAc(s)"
              />
              <Button
                v-if="s.durum !== 'TESLIM_EDILDI'"
                label="İmza & Teslim Et"
                icon="pi pi-pencil"
                class="p-button-success p-button-sm flex-1"
                @click="imzaModalAc(s)"
              />
            </div>
          </div>
        </div>
      </div>
      <div
        v-else
        class="text-center py-12 text-muted"
      >
        <i class="pi pi-inbox text-4xl mb-2 block text-gray-400" />
        Şu anda atanmış aktif bir saha siparişi bulunmuyor.
      </div>
    </div>

    <!-- SEKME 2: İZİN TALEPLERİM -->
    <div
      v-if="aktifSekme === 'izinler'"
      class="sekme-icerik"
    >
      <div class="flex justify-between items-center mb-4">
        <div>
          <h3 class="text-base font-bold text-gray-800 dark:text-gray-100">
            <i class="pi pi-calendar-plus text-primary mr-1" /> İzin Taleplerim
          </h3>
          <p class="text-xs text-muted">
            İzin talebinizi açın; İK ve yöneticiniz onayladığında anlık bilgilendirilirsiniz.
          </p>
        </div>
        <Button
          label="Yeni İzin Talebi"
          icon="pi pi-plus"
          class="p-button-primary p-button-sm"
          @click="yeniIzinModal = true"
        />
      </div>

      <div
        v-if="izinler.length > 0"
        class="space-y-3"
      >
        <div
          v-for="i in izinler"
          :key="i.id"
          class="p-4 rounded-xl border bg-white dark:bg-gray-800 flex justify-between items-center"
        >
          <div>
            <div class="flex items-center gap-2 mb-1">
              <span class="font-bold text-sm text-gray-800 dark:text-gray-100">{{ i.izinTuru }}</span>
              <Tag
                :value="i.durum"
                :severity="talepDurumSeverity(i.durum)"
              />
            </div>
            <div class="text-xs text-muted">
              <i class="pi pi-clock mr-1" /> {{ formatDate(i.baslangic) }} → {{ formatDate(i.bitis) }}
              <strong class="text-primary ml-1">({{ i.gunSayisi }} Gün)</strong>
            </div>
            <p
              v-if="i.aciklama"
              class="text-xs text-gray-600 dark:text-gray-300 mt-1"
            >
              {{ i.aciklama }}
            </p>
          </div>
          <div class="text-right text-xs">
            <span
              v-if="i.onaylayan"
              class="text-muted block"
            >Onaylayan: {{ i.onaylayan }}</span>
          </div>
        </div>
      </div>
      <div
        v-else
        class="text-center py-12 text-muted"
      >
        <i class="pi pi-calendar text-4xl mb-2 block text-gray-400" />
        Henüz bir izin talebiniz bulunmuyor.
      </div>
    </div>

    <!-- SEKME 3: MASRAF & AVANS TALEPLERİM -->
    <div
      v-if="aktifSekme === 'masraflar'"
      class="sekme-icerik"
    >
      <div class="flex justify-between items-center mb-4">
        <div>
          <h3 class="text-base font-bold text-gray-800 dark:text-gray-100">
            <i class="pi pi-receipt text-primary mr-1" /> Saha Masraf & Avans Taleplerim
          </h3>
          <p class="text-xs text-muted">
            Benzin, yemek, konaklama fişlerinizi yükleyin veya avans talebi gönderin.
          </p>
        </div>
        <Button
          label="Yeni Masraf / Avans"
          icon="pi pi-plus"
          class="p-button-primary p-button-sm"
          @click="yeniMasrafModal = true"
        />
      </div>

      <div
        v-if="masraflar.length > 0"
        class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4"
      >
        <div
          v-for="m in masraflar"
          :key="m.id"
          class="p-4 rounded-xl border bg-white dark:bg-gray-800 flex flex-col justify-between"
        >
          <div>
            <div class="flex justify-between items-start mb-2">
              <span class="text-xs font-bold px-2 py-0.5 rounded bg-blue-50 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300">
                {{ m.tur === 'AVANS' ? 'Avans Talebi' : m.kategori }}
              </span>
              <Tag
                :value="m.durum"
                :severity="talepDurumSeverity(m.durum)"
              />
            </div>
            <div class="text-xl font-extrabold text-gray-900 dark:text-gray-100 mb-1">
              {{ formatCurrency(m.tutar) }} {{ m.paraBirimi || 'TRY' }}
            </div>
            <p class="text-xs text-muted mb-2">
              {{ formatDate(m.tarih) }} · {{ m.aciklama }}
            </p>
          </div>
          <div
            v-if="m.onayNotu"
            class="text-xs text-amber-700 bg-amber-50 p-2 rounded mt-2"
          >
            <strong>Not:</strong> {{ m.onayNotu }}
          </div>
        </div>
      </div>
      <div
        v-else
        class="text-center py-12 text-muted"
      >
        <i class="pi pi-wallet text-4xl mb-2 block text-gray-400" />
        Henüz bir masraf veya avans kaydınız bulunmuyor.
      </div>
    </div>

    <!-- SEKME 4: MÜŞTERİ ZİYARET & CHECK-IN -->
    <div
      v-if="aktifSekme === 'ziyaret'"
      class="sekme-icerik"
    >
      <div class="kart-kutu max-w-xl mx-auto p-5 rounded-xl border bg-white dark:bg-gray-800">
        <h3 class="text-base font-bold text-gray-800 dark:text-gray-100 mb-2">
          <i class="pi pi-map-marker text-red-500 mr-1" /> Müşteri Ziyaret Kaydı & Görüşme Notu
        </h3>
        <p class="text-xs text-muted mb-4">
          Ziyaret ettiğiniz müşteriyi seçip görüşme sonucunu kaydedin. Kayıt anında CRM ve yöneticiye iletilir.
        </p>

        <div class="space-y-3">
          <div>
            <label class="form-label text-xs font-semibold">Ziyaret Edilen Müşteri *</label>
            <Dropdown
              v-model="ziyaretForm.cariHesapId"
              :options="cariHesaplar"
              option-label="ad"
              option-value="id"
              placeholder="Müşteri Seçin"
              filter
              class="w-full"
            />
          </div>
          <div>
            <label class="form-label text-xs font-semibold">Ziyaret Amacı</label>
            <Dropdown
              v-model="ziyaretForm.amac"
              :options="['Satış & Tanıtım Görüşmesi', 'Sipariş & Teklif Takibi', 'Tahsilat', 'Teknik Destek / Bakım', 'Rutin Ziyaret']"
              class="w-full"
            />
          </div>
          <div>
            <label class="form-label text-xs font-semibold">Görüşme Notları & Sonuç *</label>
            <Textarea
              v-model="ziyaretForm.notlar"
              rows="3"
              placeholder="Görüşülen yetkili, talep edilen ürünler, sonraki aksiyon..."
              class="w-full"
            />
          </div>
          <Button
            label="Ziyareti Merkeze Bildir"
            icon="pi pi-send"
            class="p-button-primary w-full mt-2"
            :loading="ziyaretKaydediliyor"
            @click="ziyaretKaydet"
          />
        </div>
      </div>
    </div>

    <!-- DİJİTAL DOKUNMATİK İMZA & TESLİMAT MODALI -->
    <Dialog
      v-model:visible="imzaModal"
      :modal="true"
      header="✍️ Müşteri Teslimat İmzası Al"
      :style="{ width: '450px', maxWidth: '95vw' }"
    >
      <div class="space-y-3">
        <p class="text-xs text-muted">
          Lütfen teslim alan müşterinizin adını girin ve parmağıyla aşağıdaki kutuya imza atmasını sağlayın.
        </p>
        <div>
          <label class="form-label text-xs font-semibold">Teslim Alan Kişi Adı Soyadı *</label>
          <InputText
            v-model="imzaForm.teslimAlan"
            placeholder="Örn: Ahmet Yılmaz"
            class="w-full"
          />
        </div>
        <div>
          <label class="form-label text-xs font-semibold">Teslimat Notu</label>
          <InputText
            v-model="imzaForm.notlar"
            placeholder="Eksiksiz ve hasarsız teslim edildi"
            class="w-full"
          />
        </div>
        <div>
          <div class="flex justify-between items-center mb-1">
            <label class="form-label text-xs font-semibold">Dijital İmza Alanı</label>
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
            class="imza-canvas w-full h-40 border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-lg bg-gray-50"
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
          icon="pi pi-times"
          class="p-button-text"
          @click="imzaModal = false"
        />
        <Button
          label="Teslimatı Onayla"
          icon="pi pi-check"
          class="p-button-success"
          :loading="teslimEdiliyor"
          @click="teslimatOnayla"
        />
      </template>
    </Dialog>

    <!-- YENİ İZİN TALEBİ MODALI -->
    <Dialog
      v-model:visible="yeniIzinModal"
      :modal="true"
      header="Yeni İzin Talebi Aç"
      :style="{ width: '420px', maxWidth: '95vw' }"
    >
      <div class="space-y-3">
        <div>
          <label class="form-label text-xs font-semibold">İzin Türü *</label>
          <Dropdown
            v-model="izinForm.izinTuru"
            :options="['Yıllık İzin', 'Mazeret İzni', 'Rapor / Sağlık', 'Evlilik İzni', 'Doğum İzni', 'Ücretsiz İzin']"
            class="w-full"
          />
        </div>
        <div class="grid grid-cols-2 gap-2">
          <div>
            <label class="form-label text-xs font-semibold">Başlangıç *</label>
            <InputText
              v-model="izinForm.baslangic"
              type="date"
              class="w-full"
            />
          </div>
          <div>
            <label class="form-label text-xs font-semibold">Bitiş *</label>
            <InputText
              v-model="izinForm.bitis"
              type="date"
              class="w-full"
            />
          </div>
        </div>
        <div>
          <label class="form-label text-xs font-semibold">İzin Nedeni / Açıklama</label>
          <Textarea
            v-model="izinForm.aciklama"
            rows="2"
            placeholder="İzin detayları..."
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
          class="p-button-primary"
          :loading="izinGonderiliyor"
          @click="izinTalepGonder"
        />
      </template>
    </Dialog>

    <!-- YENİ MASRAF / AVANS TALEBİ MODALI -->
    <Dialog
      v-model:visible="yeniMasrafModal"
      :modal="true"
      header="Yeni Masraf / Avans Talebi"
      :style="{ width: '420px', maxWidth: '95vw' }"
    >
      <div class="space-y-3">
        <div>
          <label class="form-label text-xs font-semibold">Talep Türü</label>
          <div class="flex gap-2">
            <button
              type="button"
              :class="['flex-1 py-1.5 text-xs font-bold rounded border', masrafForm.tur === 'MASRAF' ? 'bg-primary text-white border-primary' : 'bg-gray-50']"
              @click="masrafForm.tur = 'MASRAF'"
            >
              Harcama / Masraf
            </button>
            <button
              type="button"
              :class="['flex-1 py-1.5 text-xs font-bold rounded border', masrafForm.tur === 'AVANS' ? 'bg-primary text-white border-primary' : 'bg-gray-50']"
              @click="masrafForm.tur = 'AVANS'"
            >
              Avans Talebi
            </button>
          </div>
        </div>
        <div v-if="masrafForm.tur === 'MASRAF'">
          <label class="form-label text-xs font-semibold">Harcama Kategorisi</label>
          <Dropdown
            v-model="masrafForm.kategori"
            :options="['YAKIT', 'YEMEK', 'KONAKLAMA', 'ULASIM', 'MALZEME', 'DIGER']"
            class="w-full"
          />
        </div>
        <div>
          <label class="form-label text-xs font-semibold">Tutar (₺) *</label>
          <input
            v-model.number="masrafForm.tutar"
            type="number"
            min="1"
            step="0.01"
            class="p-inputtext w-full"
            placeholder="0.00"
          >
        </div>
        <div>
          <label class="form-label text-xs font-semibold">Açıklama *</label>
          <InputText
            v-model="masrafForm.aciklama"
            placeholder="Örn: Ankara seyahati yakıt fişi"
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
          class="p-button-primary"
          :loading="masrafGonderiliyor"
          @click="masrafTalepGonder"
        />
      </template>
    </Dialog>

    <!-- SAHADA YENİ HIZLI SİPARİŞ MODALI -->
    <Dialog
      v-model:visible="yeniSiparisModal"
      :modal="true"
      header="Sahada Yeni Sipariş Al"
      :style="{ width: '500px', maxWidth: '95vw' }"
    >
      <div class="space-y-3">
        <div>
          <label class="form-label text-xs font-semibold">Müşteri *</label>
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
        <div>
          <label class="form-label text-xs font-semibold">Ürün / Stok *</label>
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
        <div class="grid grid-cols-2 gap-2">
          <div>
            <label class="form-label text-xs font-semibold">Miktar</label>
            <input
              v-model.number="yeniSiparisForm.miktar"
              type="number"
              min="1"
              class="p-inputtext w-full"
            >
          </div>
          <div>
            <label class="form-label text-xs font-semibold">Birim Fiyat (₺)</label>
            <input
              v-model.number="yeniSiparisForm.birimFiyat"
              type="number"
              min="0"
              class="p-inputtext w-full"
            >
          </div>
        </div>
        <div>
          <label class="form-label text-xs font-semibold">Teslimat Adresi / Notu</label>
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
          class="p-button-success"
          :loading="siparisKaydediliyor"
          @click="hizliSiparisKaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useAuthStore } from '../stores/authStore.js'
import { siparisAPI, personelIzinAPI, personelMasrafTalepAPI, cariHesapAPI, stokAPI, notAPI } from '../api/index.js'
import { formatCurrency, formatDate } from '../utils/format.js'
import { useToast } from 'primevue/usetoast'

const authStore = useAuthStore()
const toast = useToast()

const aktifSekme = ref('siparisler')
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
  tutar: 0,
  aciklama: ''
})
const ziyaretForm = ref({
  cariHesapId: null,
  amac: 'Satış & Tanıtım Görüşmesi',
  notlar: ''
})
const yeniSiparisForm = ref({
  cariHesapId: null,
  stokId: null,
  miktar: 1,
  birimFiyat: 0,
  adres: ''
})

// İmza Canvas
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
  siparisler.value.filter(s => s.durum !== 'TESLIM_EDILDI' && s.durum !== 'IPTAL').length
)

const siparisDurumSeverity = (durum) => {
  const map = {
    BEKLIYOR: 'warning',
    HAZIRLANIYOR: 'info',
    YOLDA: 'help',
    TESLIM_EDILDI: 'success',
    IPTAL: 'danger'
  }
  return map[durum] || 'info'
}

const talepDurumSeverity = (durum) => {
  const map = {
    BEKLEMEDE: 'warning',
    ONAYLANDI: 'success',
    REDDEDILDI: 'danger'
  }
  return map[durum] || 'info'
}

// İmza İşlemleri
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

const imzaBaslat = (e) => {
  cizimYapiliyor = true
  ctx.beginPath()
  ctx.moveTo(e.offsetX, e.offsetY)
}

const imzaCiz = (e) => {
  if (!cizimYapiliyor) return
  ctx.lineTo(e.offsetX, e.offsetY)
  ctx.stroke()
}

const imzaBaslatTouch = (e) => {
  e.preventDefault()
  cizimYapiliyor = true
  const rect = imzaCanvas.value.getBoundingClientRect()
  const touch = e.touches[0]
  ctx.beginPath()
  ctx.moveTo(touch.clientX - rect.left, touch.clientY - rect.top)
}

const imzaCizTouch = (e) => {
  e.preventDefault()
  if (!cizimYapiliyor) return
  const rect = imzaCanvas.value.getBoundingClientRect()
  const touch = e.touches[0]
  ctx.lineTo(touch.clientX - rect.left, touch.clientY - rect.top)
  ctx.stroke()
}

const imzaBitir = () => {
  cizimYapiliyor = false
}

const imzayiTemizle = () => {
  if (ctx && imzaCanvas.value) {
    ctx.clearRect(0, 0, imzaCanvas.value.width, imzaCanvas.value.height)
  }
}

const teslimatOnayla = async () => {
  if (!imzaForm.value.teslimAlan) {
    toast.add({ severity: 'warn', summary: 'Eksik Bilgi', detail: 'Teslim alan kişi adını giriniz.', life: 3000 })
    return
  }
  teslimEdiliyor.value = true
  try {
    if (seciliSiparis.value) {
      seciliSiparis.value.durum = 'TESLIM_EDILDI'
    }
    toast.add({ severity: 'success', summary: 'Teslim Edildi', detail: 'Sipariş teslimatı ve dijital imza kaydedildi.', life: 3000 })
    imzaModal.value = false
    await tumunuYukle()
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err.message, life: 3000 })
  } finally {
    teslimEdiliyor.value = false
  }
}

const durumSecModalAc = (siparis) => {
  const durum = prompt('Yeni sipariş durumunu yazın (HAZIRLANIYOR, YOLDA, TESLIM_EDILDI):', siparis.durum || 'YOLDA')
  if (durum) {
    siparis.durum = durum.toUpperCase()
    toast.add({ severity: 'success', summary: 'Güncellendi', detail: `Sipariş durumu: ${durum}`, life: 2500 })
  }
}

// İzin Talep Gönderme
const izinTalepGonder = async () => {
  izinGonderiliyor.value = true
  try {
    const bas = new Date(izinForm.value.baslangic)
    const bit = new Date(izinForm.value.bitis)
    const gunSayisi = Math.max(1, Math.round((bit - bas) / (1000 * 60 * 60 * 24)) + 1)

    await personelIzinAPI.create({
      personelId: authStore.kullanici?.personelId || 1,
      izinTuru: izinForm.value.izinTuru,
      baslangic: izinForm.value.baslangic,
      bitis: izinForm.value.bitis,
      gunSayisi,
      aciklama: izinForm.value.aciklama
    })

    toast.add({ severity: 'success', summary: 'İzin Talebi Gönderildi', detail: 'Yöneticinizin onayına iletildi.', life: 3000 })
    yeniIzinModal.value = false
    await tumunuYukle()
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err.message, life: 3000 })
  } finally {
    izinGonderiliyor.value = false
  }
}

// Masraf Talep Gönderme
const masrafTalepGonder = async () => {
  if (!masrafForm.value.tutar || masrafForm.value.tutar <= 0) {
    toast.add({ severity: 'warn', summary: 'Eksik Bilgi', detail: 'Geçerli bir tutar girin.', life: 3000 })
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
    toast.add({ severity: 'success', summary: 'Talep İletildi', detail: 'Muhasebe onayına gönderildi.', life: 3000 })
    yeniMasrafModal.value = false
    await tumunuYukle()
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err.message, life: 3000 })
  } finally {
    masrafGonderiliyor.value = false
  }
}

// Ziyaret Kaydet
const ziyaretKaydet = async () => {
  if (!ziyaretForm.value.cariHesapId || !ziyaretForm.value.notlar) {
    toast.add({ severity: 'warn', summary: 'Eksik Bilgi', detail: 'Müşteri ve görüşme notunu girin.', life: 3000 })
    return
  }
  ziyaretKaydediliyor.value = true
  try {
    const cari = cariHesaplar.value.find(c => c.id === ziyaretForm.value.cariHesapId)
    await notAPI.create({
      baslik: `Saha Ziyareti: ${cari?.ad || 'Müşteri'} (${ziyaretForm.value.amac})`,
      icerik: ziyaretForm.value.notlar,
      kategori: 'SAHA_ZIYARET'
    })
    toast.add({ severity: 'success', summary: 'Ziyaret Kaydedildi', detail: 'Görüşme notu CRM ve merkeze işlendi.', life: 3000 })
    ziyaretForm.value.notlar = ''
  } catch (err) {
    toast.add({ severity: 'error', summary: 'Hata', detail: err.message, life: 3000 })
  } finally {
    ziyaretKaydediliyor.value = false
  }
}

// Hızlı Sipariş
const hizliSiparisStokSecildi = () => {
  const s = stoklar.value.find(item => item.id === yeniSiparisForm.value.stokId)
  if (s) {
    yeniSiparisForm.value.birimFiyat = s.fiyat || s.satisFiyati || 0
  }
}

const hizliSiparisKaydet = async () => {
  if (!yeniSiparisForm.value.cariHesapId || !yeniSiparisForm.value.stokId) {
    toast.add({ severity: 'warn', summary: 'Eksik Bilgi', detail: 'Lütfen müşteri ve ürün seçin.', life: 3000 })
    return
  }
  siparisKaydediliyor.value = true
  try {
    await siparisAPI.create({
      cariHesapId: yeniSiparisForm.value.cariHesapId,
      tarih: new Date().toISOString().substring(0, 10),
      durum: 'BEKLIYOR',
      aciklama: 'Saha Personeli Hızlı Siparişi',
      teslimatAdresi: yeniSiparisForm.value.adres,
      kalemler: [
        {
          stokId: yeniSiparisForm.value.stokId,
          miktar: yeniSiparisForm.value.miktar,
          birimFiyat: yeniSiparisForm.value.birimFiyat,
          tutar: (yeniSiparisForm.value.miktar || 1) * (yeniSiparisForm.value.birimFiyat || 0)
        }
      ]
    })
    toast.add({ severity: 'success', summary: 'Sipariş Oluşturuldu', detail: 'Sipariş merkeze iletildi.', life: 3000 })
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
.saha-header {
  background: linear-gradient(135deg, #1e40af 0%, #3b82f6 100%);
  box-shadow: 0 4px 14px rgba(30, 64, 175, 0.25);
}
.saha-avatar {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: rgba(255,255,255,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.saha-sekme-btn {
  padding: 10px 16px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
  background: var(--bg-card);
  border: 1px solid var(--border);
  color: var(--text-secondary);
  cursor: pointer;
  white-space: nowrap;
  display: flex;
  align-items: center;
  transition: all 0.2s;
}
.saha-sekme-btn.aktif {
  background: var(--primary-color, #3b82f6);
  color: #ffffff;
  border-color: var(--primary-color, #3b82f6);
}

.badge-sayi {
  background: #ef4444;
  color: #fff;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  margin-left: 6px;
}

.siparis-kart {
  min-height: 240px;
}

.imza-canvas {
  touch-action: none;
  cursor: crosshair;
}
</style>
