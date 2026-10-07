<template>
  <div class="kalem-bilesen">
    <!-- Tek ekleme yolu: hizli kalem ekleme satiri.
         LISTENIN USTUNDE: once tablonun altindaydi ve kalem sayisi arttikca
         ekran disina kasiyordu; her kalem eklemeden sonra asagi kaydirmak
         gerekiyordu. -->
    <div class="hizli-kalem">
      <div class="hizli-kalem-baslik">
        <i class="pi pi-plus-circle" /> {{ $t('faturaKalemleri.hizliEkle') }}
      </div>
      <div class="hizli-kalem-grid">
        <!-- Her alan etiketli bir sarmalayicida: etiket satiri sayesinde
             kolonlar tek sirada hizali kalir (once yalniz placeholder vardi). -->
        <div class="hizli-alan">
          <label for="hizli-barkod-input">{{ $t('faturaKalemleri.etiketBarkod') }}</label>
          <IconField class="hizli-barkod">
            <InputIcon class="pi pi-barcode" />
            <InputText
              id="hizli-barkod-input"
              ref="barkodInput"
              v-model="yeni.barkod"
              :placeholder="$t('faturaKalemleri.barkodOkut')"
              autofocus
              @keyup.enter="barkodlaEkle"
            />
          </IconField>
        </div>
        <div class="hizli-alan">
          <label>{{ $t('faturaKalemleri.etiketStok') }}</label>
          <AutoComplete
            ref="hizliStokAuto"
            v-model="yeni.stok"
            :suggestions="oneriler"
            option-label="etiket"
            :placeholder="$t('faturaKalemleri.stokAra')"
            class="hizli-stok"
            :force-selection="false"
            :panel-style="PANEL_STILI"
            :scroll-height="PANEL_YUKSEKLIGI"
            @complete="onAra($event)"
            @option-select="stokSecildi"
          >
            <template #option="slotProps">
              <div class="stok-opsiyon">
                <span class="stok-opsiyon-kod">{{ stokKoduGoster(slotProps.option.stok) }}</span>
                <span class="stok-opsiyon-ad">{{ slotProps.option.stok.ad }}</span>
                <span class="stok-opsiyon-fiyat">{{ formatCurrency(stokSatisFiyati(slotProps.option.stok)) }}</span>
                <!-- Stokta KAC ADET oldugu oneride gorunur: once yalniz kod/ad/
                     fiyat vardi; hangi urunun stogu oldugu belirsizdi. -->
                <span
                  class="stok-opsiyon-stok"
                  :class="{ yok: Number(slotProps.option.stok.miktar || 0) <= 0 }"
                >{{ $t('faturaKalemleri.stokAdet', { n: slotProps.option.stok.miktar ?? 0 }) }}</span>
              </div>
            </template>
          </AutoComplete>
        </div>
        <div class="hizli-alan">
          <label>{{ $t('faturaKalemleri.etiketAdet') }}</label>
          <InputNumber
            v-model="yeni.adet"
            :min="1"
            :placeholder="$t('faturaKalemleri.adetZorunlu')"
            class="hizli-adet sayi-girdi"
            @focus="sec($event)"
            @keyup.enter="kalemEkle"
          />
        </div>
        <div class="hizli-alan">
          <label>{{ $t('faturaKalemleri.etiketFiyat') }}</label>
          <InputNumber
            v-model="yeni.fiyat"
            :min="0"
            :min-fraction-digits="2"
            :max-fraction-digits="2"
            :placeholder="$t('faturaKalemleri.birimFiyatZorunlu')"
            class="hizli-fiyat sayi-girdi"
            @focus="sec($event)"
            @keyup.enter="kalemEkle"
          />
        </div>
        <div class="hizli-alan">
          <label>{{ $t('faturaKalemleri.etiketKdv') }}</label>
          <Dropdown
            v-model="yeni.kdv"
            :options="kdvSecenekleri"
            class="hizli-kdv"
          />
        </div>
        <div class="hizli-alan hizli-alan-btn">
          <!-- Butonun etiketi yok; hizanin bozulmamasi icin etiket
               yuksekliginde bos bir yer tutucu birakilir. -->
          <span
            class="hizli-alan-bos"
            aria-hidden="true"
          >&nbsp;</span>
          <Button
            :label="$t('faturaKalemleri.ekle')"
            icon="pi pi-plus"
            class="hizli-ekle-btn"
            @click="kalemEkle"
          />
        </div>
      </div>
      <small class="hizli-kalem-ipucu">{{ $t('faturaKalemleri.hizliEkleIpucu') }}</small>
      <!-- Barkod sonucu: bulunamadiysa SESSIZ kalmamali. -->
      <small
        v-if="barkodMesaj"
        class="hizli-barkod-mesaj"
        role="alert"
      >
        <i class="pi pi-exclamation-triangle" /> {{ barkodMesaj }}
      </small>
    </div>

    <!-- Kalem listesi: genis ekranda tablo, dar ekranda kart -->
    <div
      v-if="!darEkran"
      class="kalem-tablo-kapsam"
    >
      <DataTable
        :value="kalemler"
        :row-class="satirSinifi"
        striped-rows
        @row-click="satirSecildiRow"
      >
        <template #empty>
          <!-- KOMPAKT bos durum: global `EmptyState` (88px daire + 48px dolgu)
               bu diyalogda gereksiz yer kapliyordu. -->
          <div class="kalem-yok-tablo">
            <i class="pi pi-inbox" />
            <span>{{ $t('faturaKalemleri.bosKalem') }}</span>
          </div>
        </template>
        <Column
          header="#"
          style="width: 40px"
        >
          <template #body="s">
            {{ s.index + 1 }}
          </template>
        </Column>
        <Column
          :header="$t('faturaKalemleri.aciklamaZorunlu')"
          :style="{ minWidth: ACIKLAMA_MIN_GENISLIK }"
        >
          <template #body="s">
            <!-- REDTEAM/Faz9: satir ici stok aramasi KALDIRILDI. Ayni isi iki
                 yerden yapmak (hizli ekleme + satir) kafa karistiriyordu ve
                 aciklama alani hem serbest metin hem stok seciciydi. Kalem
                 ekleme tek yoldan yapilir: hizli ekleme satiri + barkod.
                 Aciklama artik serbest metindir. -->
            <InputText
              v-model="s.data.aciklama"
              :placeholder="$t('faturaKalemleri.aciklamaPlaceholder')"
              class="w-full"
            />
          </template>
        </Column>
        <Column
          :header="$t('faturaKalemleri.adetZorunlu')"
          style="width: 110px"
        >
          <template #body="s">
            <!-- Adet alani stokta kac adet oldugunu gosterir. Adet stoktan
                 fazlaysa hucre kirmiziya doner; satir kayittan once
                 `Satis.vue`de onay istiyor. -->
            <div
              class="adet-sarici"
              :class="{ 'stok-yetersiz': stokYetersizMu(s.data) }"
            >
              <InputNumber
                v-model="s.data.adet"
                :min="1"
                class="w-full sayi-girdi"
                @focus="sec($event)"
              />
              <small
                v-if="stokBilgisiGosterilebilir(s.data)"
                class="stok-notu"
                :class="{ kritik: stokKritikMu(s.data) }"
              >{{ $t('faturaKalemleri.stokMiktari', { n: s.data.stokMiktar }) }}</small>
            </div>
          </template>
        </Column>
        <Column
          :header="$t('faturaKalemleri.birimFiyatZorunlu')"
          style="width: 150px"
        >
          <template #body="s">
            <InputNumber
              v-model="s.data.birimFiyat"
              :min="0"
              :min-fraction-digits="2"
              :max-fraction-digits="2"
              class="w-full sayi-girdi"
              @focus="sec($event)"
            />
          </template>
        </Column>
        <Column
          :header="$t('faturaKalemleri.iskonto')"
          style="width: 100px"
        >
          <template #body="s">
            <!-- Bos birakilirsa: sunucudaki kademeli indirim kurallari uygulanir.
                 Deger yazilirsa elle indirim yapilmis olur ve kurallar devreye girmez.
                 Yuzde degil, tutar yazilmadi; bosluk anlamlidir. -->
            <InputNumber
              v-model="s.data.iskontoOrani"
              :min="0"
              :max="100"
              :min-fraction-digits="0"
              :placeholder="$t('faturaKalemleri.iskontoKural')"
              class="w-full sayi-girdi"
              @focus="sec($event)"
            />
          </template>
        </Column>
        <Column
          :header="$t('faturaKalemleri.kdv')"
          style="width: 90px"
        >
          <template #body="s">
            <Dropdown
              v-model="s.data.kdvOrani"
              :options="kdvSecenekleri"
              class="w-full"
            />
          </template>
        </Column>
        <Column
          :header="$t('faturaKalemleri.tutar')"
          style="width: 140px"
          body-class="tutar-hucre"
        >
          <template #body="s">
            {{ formatCurrency(kalemTutar(s.data)) }}
          </template>
        </Column>
        <Column
          header=""
          style="width: 96px"
        >
          <template #body="s">
            <div class="satir-aksiyon">
              <Button
                icon="pi pi-copy"
                class="p-button-rounded p-button-text p-button-sm"
                :aria-label="$t('faturaKalemleri.cogalt')"
                :title="$t('faturaKalemleri.cogalt')"
                @click="cogalt(s.data)"
              />
              <Button
                icon="pi pi-trash"
                class="p-button-rounded p-button-danger p-button-sm"
                :aria-label="$t('common.delete')"
                :title="$t('common.delete')"
                @click="silOnayla(s.index)"
              />
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Dar ekran: kart duzeni -->
    <div
      v-else
      class="kalem-kartlar"
    >
      <div
        v-if="!kalemler.length"
        class="kalem-yok"
      >
        <i class="pi pi-inbox" />
        <span>{{ $t('faturaKalemleri.bosKalem') }}</span>
      </div>
      <div
        v-for="(k, idx) in kalemler"
        :key="idx"
        class="kalem-kart"
        :class="{ 'aktif-kalem': aktifSatir === idx }"
        @click="satirSecildiRow(idx)"
      >
        <div class="kalem-kart-ust">
          <!-- Bkz. masaustu tablo: satir ici stok aramasi kaldirildi. -->
          <InputText
            v-model="k.aciklama"
            :placeholder="$t('faturaKalemleri.aciklamaPlaceholder')"
            class="w-full"
          />
          <div class="kalem-kart-aksiyon">
            <Button
              icon="pi pi-copy"
              class="p-button-rounded p-button-text p-button-sm"
              :title="$t('faturaKalemleri.cogalt')"
              @click="cogalt(k)"
            />
            <Button
              icon="pi pi-trash"
              class="p-button-rounded p-button-danger p-button-sm"
              :title="$t('common.delete')"
              @click="silOnayla(idx)"
            />
          </div>
        </div>
        <div class="kalem-kart-grid">
          <label>
            <small>{{ $t('faturaKalemleri.adetZorunlu') }}</small>
            <InputNumber
              v-model="k.adet"
              :min="1"
              class="w-full sayi-girdi"
              @focus="sec($event)"
            />
          </label>
          <label>
            <small>{{ $t('faturaKalemleri.birimFiyatZorunlu') }}</small>
            <InputNumber
              v-model="k.birimFiyat"
              :min="0"
              :min-fraction-digits="2"
              :max-fraction-digits="2"
              class="w-full sayi-girdi"
              @focus="sec($event)"
            />
          </label>
          <label>
            <small>{{ $t('faturaKalemleri.iskonto') }}</small>
            <!-- Bkz. masaustu tablo sutunu: bos = sunucu kurallari uygulanir. -->
            <InputNumber
              v-model="k.iskontoOrani"
              :min="0"
              :max="100"
              :min-fraction-digits="0"
              :placeholder="$t('faturaKalemleri.iskontoKural')"
              class="w-full sayi-girdi"
              @focus="sec($event)"
            />
          </label>
          <label>
            <small>{{ $t('faturaKalemleri.kdv') }}</small>
            <Dropdown
              v-model="k.kdvOrani"
              :options="kdvSecenekleri"
              class="w-full"
            />
          </label>
        </div>
        <div class="kalem-kart-tutar">
          <span>{{ $t('faturaKalemleri.tutar') }}</span>
          <strong>{{ formatCurrency(kalemTutar(k)) }}</strong>
        </div>
      </div>
    </div>

    <!-- Silme geri alma: kalem KAYBOLMAZ, kisa sure geri alinabilir. -->
    <div
      v-if="sonSilinen"
      class="geri-al-bar"
    >
      <span><i class="pi pi-trash" /> {{ $t('faturaKalemleri.kalemSilindi', { ad: sonSilinen.kalem.aciklama || $t('faturaKalemleri.urun') }) }}</span>
      <button
        type="button"
        @click="silmeyiGeriAl"
      >
        <i class="pi pi-undo" /> {{ $t('hizliSatis.geriAl') }}
      </button>
    </div>

    <!-- Stok secilmeden yazilan aciklamalar: kayit sirasinda stokId=null kalir
         ve stok hareketi olusmaz. Kullaniciya acikca bildirilir. -->
    <div
      v-if="stokSecsizKalemler.length"
      class="stok-secsi-uyari"
    >
      <i class="pi pi-exclamation-triangle" />
      <span>{{ $t('faturaKalemleri.stokSecsizKalemUyarisi', { adet: stokSecsizKalemler.length }) }}</span>
    </div>

    <div class="summary-box">
      <div class="summary-row">
        <span>{{ $t('faturaKalemleri.araToplam') }}</span><span>{{ formatCurrency(araToplam) }}</span>
      </div>
      <div class="summary-row">
        <span>{{ $t('faturaKalemleri.kdvLabel') }}</span><span>{{ formatCurrency(kdvToplam) }}</span>
      </div>
      <div class="summary-row total">
        <span>{{ $t('faturaKalemleri.genelToplam') }}</span><span>{{ formatCurrency(genelToplam) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useConfirm } from 'primevue/useconfirm'
import { formatCurrency } from '../utils/format.js'
import { kalemTutar } from '../utils/faturaHesapla.js'
import { stokAPI } from '../api/index.js'

const { t } = useI18n()
const confirm = useConfirm()

const props = defineProps({
  kalemler: { type: Array, required: true },
  araToplam: { type: Number, default: 0 },
  kdvToplam: { type: Number, default: 0 },
  genelToplam: { type: Number, default: 0 },
  /** KDV orani secenekleri (varsayilan [0,1,8,10,18,20]). */
  kdvSecenekleri: { type: Array, default: () => [0, 1, 8, 10, 18, 20] },
  /**
   * Yeni kalemde KDV yoksa kullanilacak varsayilan oran. Varsayilan 0'dir;
   * stok secilirse stogun KDV'si (tanimliysa) uygulanir.
   */
  kdvVarsayilan: { type: Number, default: 0 }
})

// REDTEAM/Faz9: `stok-sec` KALDIRILDI. Satir ici stok aramasi yerine kalem
// ekleme tek yoldan yapilir (hizli ekleme + barkod), bu yuzden "satirda stok
// secildi" olayi da yok.
const emit = defineEmits(['add', 'remove', 'geri-al'])

// AutoComplete paneli PrimeVue'da input ile AYNI genislikte olur
// (overlay.style.minWidth = getOuterWidth(target)). Tablodaki aciklama kolonu
// daraldiginda panel de daralir ve urun adi kesisir. Panel icin taban bir
// genislik veriyoruz; ekranda yer varsa ozellikle genisler.
const ACIKLAMA_MIN_GENISLIK = '260px'
const PANEL_STILI = { minWidth: '340px' }
const PANEL_YUKSEKLIGI = '320px'

const oneriler = ref([])
let aramaZamanlayici = null
const hizliStokAuto = ref(null)

const stokKoduGoster = (stok) => stok?.stokKodu || stok?.barkod || ''
const stokSatisFiyati = (stok) => stok?.satisFiyati || stok?.fiyat || 0

// Aktif kalem listesinin (props) ilk gecerli KDV varsayilaniyla baslamasi icin.
const yeni = reactive({ stok: null, barkod: '', adet: 1, fiyat: 0, kdv: props.kdvVarsayilan })
const barkodInput = ref(null)
/** Barkod sonucu geri bildirimi (bulunamadi vb.) — panel icinde gosterilir. */
const barkodMesaj = ref('')
const barkodAraniyor = ref(false)

// Sayisal girdide odaklaninca mevcut degeri sec (hizli ustune yazma).
const sec = (event) => {
  try {
    event?.target?.select?.()
  } catch {
    /* yoksay */
  }
}

// Dar ekran tespiti (<=720px) icin kart duzeni.
const darEkran = ref(false)
let mq = null
const mqGuncelle = (e) => {
  darEkran.value = e.matches
}
onMounted(() => {
  try {
    mq = window.matchMedia('(max-width: 720px)')
    darEkran.value = mq.matches
    mq.addEventListener('change', mqGuncelle)
  } catch {
    darEkran.value = false
  }
  // Diyalog acildiginda imlec BARKOD alanina gelsin (en hizli giris yolu).
  // Dialog `v-focustrap` ile ilk odagi konteynere verebilir; bir sonraki
  // tick'te odagi barkoda cekerek deterministik hale getiriyoruz.
  nextTick(() => barkodInput.value?.focus?.())
})
onUnmounted(() => {
  try {
    mq?.removeEventListener('change', mqGuncelle)
  } catch {
    /* yoksay */
  }
})

// Sunucu tarafi stok arama (debounce). Oneri etiketi stok kodu + ad.
const onAra = (event) => {
  const q = (event?.query || '').trim()
  if (aramaZamanlayici) clearTimeout(aramaZamanlayici)
  if (q.length < 2) {
    oneriler.value = []
    return
  }
  aramaZamanlayici = setTimeout(async () => {
    try {
      const r = await stokAPI.ara(q)
      const liste = Array.isArray(r.data) ? r.data : (r.data?.content || [])
      oneriler.value = liste.map((s) => ({
        etiket: `${s.stokKodu ? '[' + s.stokKodu + '] ' : ''}${s.ad}`,
        stok: s
      }))
    } catch {
      oneriler.value = []
    }
  }, 250)
}

// Hizli ekleme satirinda stok secilince adet/fiyat/KDV otomatik dolar.
const stokSecildi = (e) => {
  const s = e?.value?.stok
  if (!s) return
  yeni.stok = s
  yeni.fiyat = Number(s.satisFiyati || s.fiyat || 0)
  yeni.kdv = s.kdvOrani != null ? Number(s.kdvOrani) : props.kdvVarsayilan
}

/**
 * Bir stoktan kalem satiri uretir (hem hizli ekleme hem barkod kullanir).
 * Iskonto NULL birakilir: backend `null` gorunce kademeli indirim kurallarini
 * uygular, `0` gorunce atlar.
 */
const kalemPayload = (s, adet = 1) => ({
  stokId: s?.id ?? null,
  aciklama: s?.ad ?? '',
  adet: adet || 1,
  birimFiyat: Number(s?.satisFiyati || s?.fiyat || 0),
  iskontoOrani: null,
  kdvOrani: s?.kdvOrani != null ? Number(s.kdvOrani) : props.kdvVarsayilan,
  // Stokta KAC ADET oldugu kalemle birlikte tasinir. Satis ekrani kayittan
  // once adet > stok durumunu kontrol edip kullaniciyi uyarir.
  stokMiktar: s?.miktar != null ? Number(s.miktar) : null
})

const kalemEkle = () => {
  emit('add', kalemPayload(yeni.stok, yeni.adet))
  // Satir ekleme hizli olsun: stok temizlenir, adet 1'e doner, KDV varsayilan kalir.
  yeni.stok = null
  yeni.adet = 1
  yeni.fiyat = 0
  // Odak urun alanina doner: ardisik kalem eklemede her seferinde fare ile
  // alana tiklamak zorunda kalmasin diye.
  nextTick(() => hizliStokAuto.value?.focus?.())
}

/**
 * Barkodu okutup Enter'a basmak kalemi ANINDA ekler. Once barkod yolu yoktu;
 * etiketi olan urun icin bile elle arama gerekiyordu.
 *
 * Bulunamazsa kalem EKLENMEZ ve panel icinde kisa bir mesaj gosterilir
 * (sessiz yutma yok).
 */
const barkodlaEkle = async () => {
  const kod = (yeni.barkod || '').trim()
  if (!kod) return
  barkodAraniyor.value = true
  barkodMesaj.value = ''
  try {
    const r = await stokAPI.barkodIleBul(kod)
    const s = r?.data
    if (!s?.id) {
      barkodMesaj.value = t('faturaKalemleri.barkodBulunamadi', { kod })
      return
    }
    emit('add', kalemPayload(s, 1))
    yeni.barkod = ''
    nextTick(() => barkodInput.value?.focus?.())
  } catch {
    barkodMesaj.value = t('faturaKalemleri.barkodBulunamadi', { kod })
  } finally {
    barkodAraniyor.value = false
  }
}

// Kac kalemde stok secilmeden serbest metin birakildi? Satis kaydinda
// stokId=null satirlar olusurdu ve nedeni kullaniciya hic belli olmuyordu.
const stokSecsizKalemler = computed(() =>
  (props.kalemler || []).filter((k) => !k.stokId && k.aciklama && k.aciklama.trim())
)

// Stok miktari bilinen satirlarda gosterilir; bilinmiyorsa (serbest metin, eski
// kayit) alan bos kalir — yanlis bir "stokta 0" bilgisi gostermekten iyidir.
const stokBilgisiGosterilebilir = (k) => !!k?.stokId && k.stokMiktar != null
const stokYetersizMu = (k) =>
  stokBilgisiGosterilebilir(k) && Number(k.adet || 0) > Number(k.stokMiktar)
const stokKritikMu = (k) => stokBilgisiGosterilebilir(k) && Number(k.stokMiktar) <= 0

// Satiri kopyala: mevcut degerler korunur, kayit kimligi tasinmaz (yeni satir).
const cogalt = (kalem) => {
  const kopya = { ...kalem }
  delete kopya.id
  emit('add', kopya)
}

// ---------------------------------------------------------------------------
// SATIR SECME + SILME (onay + geri al) — REDTEAM/Faz9
// ---------------------------------------------------------------------------
// Once silme tek tiklamayla ve SESSIZCE yapiliyordu (yanlis tiklamada kalem
// kayboluyordu). Artik: onay sorulur, silindikten sonra kisa sure "geri al"
// bandi cikar. Ayrica satir secilip `Del` ile de silinebilir.
const aktifSatir = ref(-1)
const sonSilinen = ref(null)
let silZamanlayici = null
const GERI_AL_MS = 8000

const satirSecildiRow = (e) => {
  const idx = typeof e === 'number' ? e : e?.index
  if (typeof idx === 'number') aktifSatir.value = idx
}
const satirSinifi = (data) => {
  const idx = (props.kalemler || []).indexOf(data)
  return idx >= 0 && idx === aktifSatir.value ? 'aktif-kalem' : ''
}

/** Kalemi siler ve geri alinabilir birakir. */
const kalemSil = (idx) => {
  const kalem = props.kalemler[idx]
  if (!kalem) return
  sonSilinen.value = { index: idx, kalem: { ...kalem } }
  emit('remove', idx)
  if (aktifSatir.value === idx) aktifSatir.value = -1
  else if (aktifSatir.value > idx) aktifSatir.value -= 1
  clearTimeout(silZamanlayici)
  silZamanlayici = setTimeout(() => {
    sonSilinen.value = null
  }, GERI_AL_MS)
}

/** Silmeden once onay ister (yanlis tiklama veri kaybi yaratmasin). */
const silOnayla = (idx) => {
  const kalem = props.kalemler[idx]
  if (!kalem) return
  confirm.require({
    message: t('faturaKalemleri.silOnay', { ad: kalem.aciklama || t('faturaKalemleri.urun') }),
    header: t('faturaKalemleri.silBaslik'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('common.delete'),
    rejectLabel: t('common.vazgec'),
    accept: () => kalemSil(idx)
  })
}

/** Geri al: silinen kalem ESKI KONUMUNA eklenir (parent'a bildirilir). */
const silmeyiGeriAl = () => {
  if (!sonSilinen.value) return
  emit('geri-al', sonSilinen.value)
  sonSilinen.value = null
  clearTimeout(silZamanlayici)
}

// `Del` ile aktif satiri sil (yalnizca odak bir metin alaninda degilken).
const klavyeHandler = (e) => {
  if (e.key !== 'Delete') return
  const el = e.target
  const girdide = el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.tagName === 'SELECT' || el.isContentEditable)
  if (girdide) return
  if (aktifSatir.value < 0) return
  e.preventDefault()
  silOnayla(aktifSatir.value)
}
onMounted(() => document.addEventListener('keydown', klavyeHandler))
onUnmounted(() => {
  document.removeEventListener('keydown', klavyeHandler)
  clearTimeout(silZamanlayici)
})
</script>

<style scoped>
.kalem-tablo-kapsam {
  overflow-x: auto;
}
.satir-aksiyon {
  display: flex;
  gap: 2px;
  justify-content: flex-end;
}
:deep(.sayi-girdi .p-inputnumber-input) {
  text-align: right;
}
:deep(.tutar-hucre) {
  text-align: right;
  font-variant-numeric: tabular-nums;
}
:deep(.p-autocomplete),
:deep(.p-dropdown) {
  width: 100%;
}

/* Hizli kalem ekleme */
.hizli-kalem {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-primary);
}
.hizli-kalem-baslik {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-secondary);
  margin-bottom: 10px;
}
.hizli-kalem-baslik i {
  color: var(--accent);
}
.hizli-kalem-grid {
  display: grid;
  /* TEK SIRA, ETIKETLI. Kolonlar tablo sirasiyla hizali:
     stok(aciklama) -> adet -> birim fiyat -> kdv; barkod ayri yardimci alan,
     Ekle en sagda. `align-items:end` etiket+alan altlarini hizalar. */
  grid-template-columns: minmax(150px, 190px) minmax(220px, 1fr) 96px 130px 96px auto;
  gap: 6px 10px;
  align-items: end;
}
.hizli-alan {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.hizli-alan > label,
.hizli-alan-bos {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  line-height: 1.2;
  white-space: nowrap;
}
/* Butonun etiketi yok; hiza icin etiket yuksekliginde bos yer tutucu. */
.hizli-alan-bos {
  visibility: hidden;
}
.hizli-alan-btn {
  justify-content: flex-end;
}
.hizli-barkod {
  min-width: 0;
}
.hizli-ekle-btn {
  white-space: nowrap;
}
.hizli-kalem-ipucu {
  display: block;
  margin-top: 8px;
  color: var(--text-muted);
  font-size: 11.5px;
}
/* NOT: `.stok-opsiyon*` kurallari BURADA degil, `assets/app.css` icinde
   tanimlidir. AutoComplete paneli body'ye teleport edilir; scoped CSS
   bilesenin data-v ozelligi eklenmedigi icin panel ici bu kurallar
   uygulanmazdi (urun adi yine kesisik gorunuyordu). */
.stok-secsi-uyari {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  padding: 9px 12px;
  border-radius: 10px;
  font-size: 12.5px;
  font-weight: 600;
  color: #b45309;
  background: rgba(245, 158, 11, 0.12);
  border: 1px solid rgba(245, 158, 11, 0.4);
}
.stok-secsi-uyari i {
  color: #f59e0b;
}

/* Dar ekran kart duzeni */
.kalem-kartlar {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.kalem-yok {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 18px;
  color: var(--text-muted);
  font-size: 13px;
  border: 1px dashed var(--border);
  border-radius: 12px;
}
/* Tablo ici KOMPAKT bos durum (bkz. DataTable #empty). */
.kalem-yok-tablo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 22px 16px;
  color: var(--text-muted);
  font-size: 13px;
}
.kalem-yok-tablo i {
  font-size: 16px;
}
.kalem-kart {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 10px;
  background: var(--bg-primary);
}
.kalem-kart-ust {
  display: flex;
  align-items: center;
  gap: 8px;
}
.kalem-kart-aksiyon {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
}
.kalem-kart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 8px;
}
.kalem-kart-grid label {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.kalem-kart-grid small {
  color: var(--text-muted);
  font-size: 11px;
}
.kalem-kart-tutar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--border);
  font-size: 13px;
  color: var(--text-secondary);
}
.kalem-kart-tutar strong {
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}

/* Adet alani + stok miktari rozeti. Adet stoktan fazlaysa hucre kirmizi. */
.adet-sarici {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.stok-notu {
  font-size: 11px;
  line-height: 1.2;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}
.stok-notu.kritik {
  color: var(--danger, #dc2626);
}
.adet-sarici.stok-yetersiz :deep(.p-inputnumber-input) {
  border-color: var(--danger, #dc2626);
  color: var(--danger, #dc2626);
  font-weight: 600;
}

/* Orta genislik (daraltilmis diyalog/pencere): alanlar iki kolona iner,
   hicbir alan sikismaz. */
@media (max-width: 1150px) {
  .hizli-kalem-grid {
    grid-template-columns: 1fr 1fr;
  }
  .hizli-alan-btn {
    grid-column: 1 / -1;
  }
}

@media (max-width: 720px) {
  .hizli-kalem-grid {
    grid-template-columns: 1fr;
  }
}

.summary-box {
  background: var(--border);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 12px 16px;
  margin-top: 12px;
  /* OZET SAGDA ve KOMPAKT: once tam genislikteydi ve ilgi dagitiyordu. */
  margin-left: auto;
  width: fit-content;
  min-width: 300px;
  max-width: 100%;
}
.summary-row {
  display: flex;
  justify-content: space-between;
  gap: 32px;
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

/* Barkod alani: tam genislik + IKON PAYI. PrimeVue `IconField` ikonu mutlak
   konumlar; tema bu projede input'a sol dolgu vermedigi icin ikon
   placeholder'in uzerine biniyordu. */
.hizli-barkod :deep(.p-inputtext) {
  width: 100%;
  padding-left: 2.25rem;
}
.hizli-barkod-mesaj {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 8px;
  color: var(--danger, #dc2626);
}
/* NOT: `.stok-opsiyon-stok` (onerideki stok adedi rozeti) BURADA DEGIL,
   `assets/app.css` icinde. Oneri listesi body'ye teleport edilir; scoped
   kural teleport edilen icerige uygulanmaz (bkz. `.stok-opsiyon` notu). */

/* Aktif satir vurgusu (Del ile silme hedefi) */
:deep(.aktif-kalem) > td {
  background: color-mix(in srgb, var(--accent) 10%, transparent) !important;
}
.kalem-kart.aktif-kalem {
  border-color: var(--accent);
}
/* Silme geri alma bandi */
.geri-al-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 10px;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: color-mix(in srgb, var(--danger, #dc2626) 8%, var(--bg-primary));
  font-size: 12.5px;
}
.geri-al-bar button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: none;
  background: transparent;
  color: var(--accent);
  font-weight: 600;
  cursor: pointer;
}
</style>
