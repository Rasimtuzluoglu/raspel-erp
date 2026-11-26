<template>
  <div class="cari-hesaplar-container">
    <PageHeader
      :title="t('cariHesaplar.title')"
      icon="pi pi-users"
    >
      <template #actions>
        <Button
          :label="t('cariHesaplar.yeniCariHesap')"
          icon="pi pi-plus"
          class="p-button-success"
          @click="openDialog"
        />
        <Button
          icon="pi pi-ellipsis-v"
          class="p-button-outlined cari-islem-tetik"
          :aria-label="t('common.actions')"
          :title="t('common.actions')"
          @click="islemMenu?.toggle($event)"
        />
        <Menu
          ref="islemMenu"
          :model="islemMenusu"
          popup
        />
      </template>
    </PageHeader>

    <IlkZiyaretIpuclari
      anahtar="cari-hesaplar"
      :baslik="t('cariHesaplar.ipucuBaslik')"
      :metin="t('cariHesaplar.ipucuMetin')"
    />

    <CariFiltreCubugu
      ref="filtreCubuguRef"
      :arama="aramaMetni"
      :tur="filtreTur"
      :bakiye="filtreBakiye"
      :etiket="filtreEtiket"
      :etiket-onerileri="etiketOnerileri"
      :cari-tur-secenekleri="cariTurSecenekleri"
      :bakiye-filtreleri="bakiyeFiltreleri"
      :aktif-filtreler="aktifFiltreler"
      :risk-gorebilir="riskGorebilir"
      :kolonlar="kolonlar"
      @update:arama="aramaMetni = $event"
      @update:tur="filtreTur = $event"
      @update:bakiye="filtreBakiye = $event"
      @update:etiket="filtreEtiket = $event"
      @update:kolonlar="kolonGuncelle"
      @update:yogunluk="tabloYogunluk = $event"
      @ara="ara"
      @filtre-degisti="filtreDegisti"
      @etiket-oner="etiketOner"
      @etiket-ara="filtreEtiketAra"
      @filtreleri-temizle="filtreleriTemizle"
      @cip-kaldir="filtreCipKaldir"
      @riskli="riskliCarilerAc"
    />

    <div
      v-if="loading && ilkYukleme"
      class="loading-iskelet"
      :aria-label="t('common.loading')"
    >
      <SkeletonLoader
        :count="6"
        height="44px"
      />
    </div>

    <CariKpiBandi
      :cari-ozet="cariOzet"
      :toplam-kayit="cariHesapStore.toplamKayit"
      :filtre-bakiye="filtreBakiye"
      :yukleniyor="ilkYukleme"
      :aktif-filtreler="aktifFiltreler"
      @temizle="filtreleriTemizle"
      @bakiye-filtre="kpiBakiyeFiltre"
    />

    <CariListePaneli
      :cari-hesaplar="cariHesapStore?.cariHesaplar || []"
      :toplam-kayit="cariHesapStore.toplamKayit"
      :secili="selectedCariHesaplar"
      :yukleniyor="loading"
      :ilk-yukleme="ilkYukleme"
      :yogunluk="tabloYogunluk"
      :sayfa="cariSayfa"
      :sayfa-boyutu="cariSayfaBoyutu"
      :siralama="siralama"
      :kolonlar="kolonlar"
      :is-admin="isAdmin"
      :eylemler="cariEylemleri"
      :kopyala="kopyala"
      @update:secili="selectedCariHesaplar = $event"
      @sayfa-degisti="cariSayfaDegisti"
      @sirala="cariSirala"
      @tahsilat="tahsilatAc"
      @toplu-email="yardimciDiyaloglarRef.value?.emailAc()"
      @toplu-sil="batchSil"
      @csv="batchCsvExport"
      @yeni="openDialog"
    />

    <!-- Cari Hesap Dialog -->
    <CariHesapFormDialog
      ref="formDialogRef"
      v-model:visible="showDialog"
      :cari-id="formCariId"
      :baslangic-verisi="formBaslangicVerisi"
      @kaydedildi="formKaydedildi"
    />

    <!-- Hareketler Dialog -->
    <CariKartDialog
      v-model:visible="showHareketlerDialog"
      :cari="selectedCariHesap"
      :ozet="hareketOzeti"
      :hareketler="cariHareketler"
      :bakiye="cariHareketlerBakiye"
      :yukleniyor="cariHareketlerYukleniyor"
      :faturalar="cariFaturalar"
      :ozel-fiyatlar="cariOzelFiyatlar"
      :son-urunler="sonUrunler"
      :secili-urun="seciliUrunFiyatGecmisi"
      :notlar="cariNotlar"
      @ekstre="ekstreGonder"
      @fatura-detay="faturaDetayAc"
      @ozel-fiyat-ekle="ozelFiyatEkleGonder"
      @ozel-fiyat-sil="cariOzelFiyatSil"
      @urun-goster="urunFiyatGecmisiGoster"
      @urun-kapat="urunFiyatKapat"
      @not-ekle="notEkleGonder"
      @not-sil="cariNotSil"
    />

    <TahsilatGirDialog
      v-model:visible="tahsilatDialog"
      :cari="tahsilatHedefCari"
      :cariler="[]"
      @kaydedildi="tahsilatSonrasiYenile"
    />

    <BorclandirmaGirDialog
      v-model:visible="borclandirmaDialog"
      :cari="borclandirmaHedefCari"
      @kaydedildi="borclandirmaSonrasiYenile"
    />

    <CariYardimciDiyaloglar
      ref="yardimciDiyaloglarRef"
      :secili-cari-hesaplar="selectedCariHesaplar"
      :cari-tur-secenekleri="cariTurSecenekleri"
      :aktif-secenekleri="aktifSecenekleri"
      @yenilendi="formKaydedildi"
      @secili-temizle="selectedCariHesaplar = []"
    />



    <CariFaturaDetayDialog
      v-model:visible="faturaDetayDialog"
      :fatura="seciliFatura"
    />

    <Message
      v-if="cariHesapStore.error"
      severity="error"
      :text="cariHesapStore.error"
    />

    <CariKart360Dialog
      v-model:visible="kartDialogAcik"
      :cari-id="kartCariId"
      :cari="kartCari"
      @ekstre="ekstreAc"
      @adresler="adresAc360"
      @tahsilat="tahsilatAc"
      @fatura="yeniFatura"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { useAuthStore } from '../stores/authStore.js'
import { useRouter, useRoute } from 'vue-router'
import { excelAPI, hareketAPI, notAPI, faturaAPI, cariHesapAPI, raporAPI } from '../api/index.js'
import { useKisayollar } from '../composables/useKisayollar.js'
import { usePanoyaKopyala } from '../composables/usePanoyaKopyala.js'
import IlkZiyaretIpuclari from '../components/IlkZiyaretIpuclari.vue'
import TahsilatGirDialog from '../components/TahsilatGirDialog.vue'
import BorclandirmaGirDialog from '../components/BorclandirmaGirDialog.vue'
import CariKart360Dialog from '../components/CariKart360Dialog.vue'
import CariFaturaDetayDialog from '../components/CariFaturaDetayDialog.vue'
import PageHeader from '../components/PageHeader.vue'
import Menu from 'primevue/menu'
import { formatCurrency } from '../utils/format.js'
import { escapeHtml } from '../utils/escapeHtml.js'
import { fisPenceresiAcVeYazdir } from '../utils/fisYazdir.js'
import { useI18n } from 'vue-i18n'
import CariHesapFormDialog from '../components/cari/CariHesapFormDialog.vue'
import CariFiltreCubugu from '../components/cari/CariFiltreCubugu.vue'
import CariKpiBandi from '../components/cari/CariKpiBandi.vue'
import CariListePaneli from '../components/cari/CariListePaneli.vue'
import CariYardimciDiyaloglar from '../components/cari/CariYardimciDiyaloglar.vue'
import CariKartDialog from '../components/cari/CariKartDialog.vue'

const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const cariHesapStore = useCariHesapStore()
const authStore = useAuthStore()
const isAdmin = computed(() => authStore?.kullanici?.role === 'ADMIN')
const riskGorebilir = computed(() => authStore?.kullanici?.role !== 'DRIVER')
const router = useRouter()
const route = useRoute()
const { kopyala } = usePanoyaKopyala()
const { t } = useI18n()

const tabloYogunluk = ref('comfortable')
// VarsayÄ±lan gÃ¶rÃ¼nÃ¼mde yalnÄ±zca gÃ¼nlÃ¼k kullanÄ±m iÃ§in gereken kolonlar aÃ§Ä±k.
// ID, yetkili, kredi limiti, temsilci ve etiketler kolon ayarlarÄ±ndan aÃ§Ä±labilir.
const varsayilanKolonGorunurluk = {
  id: false,
  yetkiliKisi: false,
  krediLimiti: false,
  temsilciAd: false,
  etiketler: false
}
const kolonGorunurluk = ref({ ...varsayilanKolonGorunurluk })
const varsayilanKolonlar = computed(() => [
  { field: 'id', header: 'ID' },
  { field: 'ad', header: t('cariHesaplar.ad') },
  { field: 'tur', header: t('cariHesaplar.tur') },
  { field: 'yetkiliKisi', header: t('cariHesaplar.yetkili') },
  { field: 'telefon', header: t('cariHesaplar.telefon') },
  { field: 'krediLimiti', header: t('cariHesaplar.krediLimiti') },
  { field: 'odemeVadesi', header: t('cariHesaplar.vadeGun') },
  { field: 'temsilciAd', header: t('cariHesaplar.satisTemsilcisi') },
  { field: 'bakiye', header: t('cariHesaplar.bakiye') },
  { field: 'etiketler', header: t('cariHesaplar.etiketler') }
])
const kolonlar = computed(() =>
  varsayilanKolonlar.value.map((k) => ({ ...k, visible: kolonGorunurluk.value[k.field] !== false }))
)
const kolonGuncelle = (yeni) => {
  const map = {}
  ;(yeni || []).forEach((k) => {
    map[k.field] = k.visible !== false
  })
  kolonGorunurluk.value = map
}

useKisayollar({
  yeni: () => openDialog(),
  iptal: () => {
    showDialog.value = false
  },
  kaydet: () => formDialogRef.value?.kaydet(),
  // Ctrl+K: arama kutusuna odaklan. Ã–nceden placeholder'da "Ctrl+F" yazÄ±yordu
  // ama hiÃ§bir kÄ±sayol baÄŸlÄ± deÄŸildi (tarayÄ±cÄ±nÄ±n kendi bul iÅŸlevi aÃ§Ä±lÄ±yordu).
  ara: () => {
    filtreCubuguRef.value?.odakla()
  }
})

const showDialog = ref(false)
const formDialogRef = ref(null)
const formCariId = ref(null)
const formBaslangicVerisi = ref(null)
const filtreCubuguRef = ref(null)
const yardimciDiyaloglarRef = ref(null)
const showHareketlerDialog = ref(false)
const kartDialogAcik = ref(false)
const kartCariId = ref(null)
const kartCari = ref(null)

const loading = ref(false)
const ilkYukleme = ref(true)
const cariHareketler = ref([])
const cariHareketlerYukleniyor = ref(false)
// Faz 2.2: tarih aralÄ±klÄ± ekstre (boÅŸsa tÃ¼m hareketler yazdÄ±rÄ±lÄ±r).
const ekstreBaslangic = ref(null)
const ekstreBitis = ref(null)
const selectedCariHesaplar = ref([])
const selectedCariHesap = ref(null)
const aramaMetni = ref('')
const islemMenu = ref(null)
let aramaZamanlayici = null
onUnmounted(() => {
  if (aramaZamanlayici) clearTimeout(aramaZamanlayici)
})

const filtreTur = ref(null)
const filtreBakiye = ref(null)
const filtreEtiket = ref('')
// DeÄŸerler backend'in beklediÄŸi ASCII sabitler; etiketler yerelleÅŸtirilir.
const cariTurSecenekleri = computed(() => [
  { label: t('cariTur.musteri'), value: 'Musteri' },
  { label: t('cariTur.tedarikci'), value: 'Tedarikci' },
  { label: t('cariTur.herIkisi'), value: 'Her Ikisi' }
])
const bakiyeFiltreleri = computed(() => [
  { label: t('cariHesaplar.bizeAlacakli'), value: 'alacak' },
  { label: t('cariHesaplar.bizeBorclu'), value: 'borc' }
])
const aktifFiltreler = computed(() => {
  const liste = []
  if (filtreTur.value) {
    liste.push({
      tip: 'tur',
      etiket: cariTurSecenekleri.value.find((o) => o.value === filtreTur.value)?.label || filtreTur.value
    })
  }
  if (filtreBakiye.value) {
    liste.push({
      tip: 'bakiye',
      etiket: bakiyeFiltreleri.value.find((o) => o.value === filtreBakiye.value)?.label || filtreBakiye.value
    })
  }
  if (filtreEtiket.value && filtreEtiket.value.trim()) {
    liste.push({ tip: 'etiket', etiket: `#${filtreEtiket.value.trim()}` })
  }
  return liste
})
const filtreCipKaldir = (tip) => {
  if (tip === 'tur') filtreTur.value = null
  if (tip === 'bakiye') filtreBakiye.value = null
  if (tip === 'etiket') filtreEtiket.value = ''
  cariSayfa.value = 0
  loadCariHesaplar(0, cariSayfaBoyutu.value)
}
const aktifSecenekleri = computed(() => [
  { label: t('common.active'), value: true },
  { label: t('common.passive'), value: false }
])
// Faz 2.9: cari Ã§alÄ±ÅŸma para birimi.


const cariOzet = ref({})

const cariOzetYukle = async () => {
  try {
    const r = await cariHesapAPI.ozet()
    cariOzet.value = r.data || {}
  } catch {
    cariOzet.value = {}
  }
}

// Yardimci diyaloglar tek bir bilesende; parent yalnizca acma metodlarini cagirir.
const riskliCarilerAc = () => yardimciDiyaloglarRef.value?.riskliAc()
const topluGuncelleAc = () => yardimciDiyaloglarRef.value?.topluAc()
const acilisAc = (cari) => yardimciDiyaloglarRef.value?.acilisAc(cari)

const filtreleriTemizle = () => {
  aramaMetni.value = ''
  filtreTur.value = null
  filtreBakiye.value = null
  filtreEtiket.value = ''
  cariSayfa.value = 0
  loadCariHesaplar(0, cariSayfaBoyutu.value)
}

const filtreDegisti = () => {
  cariSayfa.value = 0
  loadCariHesaplar(0, cariSayfaBoyutu.value)
}

/** KPI kartÄ±na tÄ±klama: aynÄ± filtreye tekrar basÄ±nca filtre kaldÄ±rÄ±lÄ±r. */
const kpiBakiyeFiltre = (yon) => {
  filtreBakiye.value = filtreBakiye.value === yon ? null : yon
  cariSayfa.value = 0
  loadCariHesaplar(0, cariSayfaBoyutu.value)
}

// Faz 2.4: etiket filtresi (Enter/odak kaybÄ±nda uygula).
const filtreEtiketAra = () => {
  cariSayfa.value = 0
  loadCariHesaplar(0, cariSayfaBoyutu.value)
}

// Etiket autocomplete: ÅŸirkette kullanÄ±lan etiketler bir kez yÃ¼klenir,
// yazarken istemci tarafÄ±nda filtrelenir (ek istek yok).
const tumEtiketler = ref([])
const etiketOnerileri = ref([])
const etiketleriYukle = async () => {
  try {
    const r = await cariHesapAPI.etiketler()
    tumEtiketler.value = unwrapList(r) || []
  } catch {
    tumEtiketler.value = []
  }
}
const etiketOner = (event) => {
  const q = String(event?.query || '').toLocaleLowerCase('tr')
  etiketOnerileri.value = tumEtiketler.value
    .filter((e) => !q || e.toLocaleLowerCase('tr').includes(q))
    .slice(0, 20)
}

const toplamTahsilat = computed(() =>
  cariHareketler.value.filter((h) => h.tur === 'TAHSILAT').reduce((s, h) => s + (h.tutar || 0), 0)
)
const toplamOdeme = computed(() =>
  cariHareketler.value.filter((h) => h.tur === 'ODEME').reduce((s, h) => s + (h.tutar || 0), 0)
)
const toplamBorclandirma = computed(() =>
  cariHareketler.value.filter((h) => h.tur === 'BORC').reduce((s, h) => s + (h.tutar || 0), 0)
)
// Hareket tablosu `cari.hareket` kayitlarini gosterir; SATIS/ALIS faturalari
// burada satir acmaz (FaturaService cari bakiyeyi dogrudan gunceller). Bu yuzden
// hareketlerden hesaplanan bakiye, kaydin gercek bakiyesiyle UYUSMAZ: 50.000 TL
// acik faturali bir cari listede -50.000 gosterirken burada 0 cikardi.
// KAYIT BAKIYESI (negatif = cari bize borcu) esas alinir; hareketlerin saf
// etkisi ekstrede gorunur, kaydi yalnizca baslangic bakiyesi olarak telafi eder.
const netHareketEtkisi = computed(() =>
  toplamTahsilat.value - toplamOdeme.value - toplamBorclandirma.value
)

const guncelBakiye = computed(() => {
  const kayit = selectedCariHesap.value?.bakiye
  return kayit != null ? kayit : netHareketEtkisi.value
})

// Hareketlerden onceki (acilis) bakiye: en son hareketin yuruyen bakiyesi
// kaydin gercek bakiyesine tam otursun diye fark kadar eklenir.
const hareketBaslangicBakiyesi = computed(() => {
  const kayit = selectedCariHesap.value?.bakiye
  if (kayit == null) return 0
  return kayit - netHareketEtkisi.value
})

const hareketDelta = (h) => (h.tur === 'TAHSILAT' ? h.tutar || 0 : -(h.tutar || 0))

// Ekstre: tarihe gore artan sirali, yuruyen bakiye kolonlu hareket listesi.
const cariHareketlerBakiye = computed(() => {
  const sirali = [...cariHareketler.value].sort(
    (a, b) => new Date(a.hareketTarihi || 0) - new Date(b.hareketTarihi || 0)
  )
  let bakiye = hareketBaslangicBakiyesi.value
  return sirali.map((h) => {
    bakiye += hareketDelta(h)
    return { ...h, bakiye }
  })
})

// Faz 2.2: Date nesnesini YYYY-MM-DD'ye Ã§evirir.
const tarihIso = (d) => {
  const x = new Date(d)
  return `${x.getFullYear()}-${String(x.getMonth() + 1).padStart(2, '0')}-${String(x.getDate()).padStart(2, '0')}`
}

// Cari 360 kartÄ± baÅŸlÄ±ÄŸÄ± (CariHareketBasligi) baÄŸlantÄ±sÄ±
const hareketOzeti = computed(() => ({
  tahsilat: toplamTahsilat.value,
  odeme: toplamOdeme.value,
  borclandirma: toplamBorclandirma.value,
  bakiye: guncelBakiye.value
}))

/** BaÅŸlÄ±k bileÅŸeninden gelen aralÄ±kla ekstre yazdÄ±rÄ±r. */
const ekstreGonder = ({ baslangic, bitis }) => {
  ekstreBaslangic.value = baslangic
  ekstreBitis.value = bitis
  cariEkstreYazdir()
}

const cariEkstreYazdir = async () => {
  const cari = selectedCariHesap.value
  if (!cari) return
  const aralikli = !!(ekstreBaslangic.value && ekstreBitis.value)
  let satirlar = ''
  let basBakiye = null
  let sonBakiye = guncelBakiye.value
  try {
    if (aralikli) {
      // Faz 2.2: sunucudan tarih aralÄ±klÄ±, devir/yÃ¼rÃ¼yen bakiyeli ekstre.
      const r = await raporAPI.cariEkstre({
        cariHesapId: cari.id,
        baslangic: tarihIso(ekstreBaslangic.value),
        bitis: tarihIso(ekstreBitis.value)
      })
      const e = r.data || {}
      basBakiye = e.donemBasBakiye
      sonBakiye = e.donemSonBakiye
      satirlar = (e.hareketler || [])
        .map(
          (h) => `<tr>
            <td>${formatDate(h.tarih)}</td>
            <td>${escapeHtml(h.tur || '')}</td>
            <td class="sag">${h.borc ? formatCurrency(h.borc) : ''}</td>
            <td class="sag">${h.alacak ? formatCurrency(h.alacak) : ''}</td>
            <td class="sag ${(h.yuruyenBakiye || 0) >= 0 ? 'poz' : 'neg'}">${h.yuruyenBakiye != null ? formatCurrency(h.yuruyenBakiye) : ''}</td>
            <td>${escapeHtml(h.aciklama || '')}</td>
          </tr>`
        )
        .join('')
    } else {
      satirlar = cariHareketlerBakiye.value
        .map(
          (h) => `<tr>
            <td>${formatDate(h.hareketTarihi)}</td>
            <td>${escapeHtml(hareketTuruEtiketi(h.tur))}</td>
            <td class="sag">${h.tur === 'TAHSILAT' ? '' : formatCurrency(h.tutar)}</td>
            <td class="sag">${h.tur === 'TAHSILAT' ? formatCurrency(h.tutar) : ''}</td>
            <td class="sag ${h.bakiye >= 0 ? 'poz' : 'neg'}">${formatCurrency(h.bakiye)}</td>
            <td>${escapeHtml(h.aciklama || '')}</td>
          </tr>`
        )
        .join('')
    }
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('common.error'))
    return
  }
  const aralikMetni = aralikli
    ? `${formatDate(tarihIso(ekstreBaslangic.value))} - ${formatDate(tarihIso(ekstreBitis.value))}`
    : ''
  const basSatiri =
    basBakiye != null
      ? `<div class="ozet">${escapeHtml(t('cariHesaplar.ekstreDevir'))}: ${formatCurrency(basBakiye)}</div>`
      : ''
  const html = `<!DOCTYPE html><html><head><meta charset="UTF-8">
    <title>${escapeHtml(t('cariHesaplar.ekstreBaslik'))}</title>
    <style>
      body{font-family:'Segoe UI',Arial,sans-serif;color:#111;margin:24px;font-size:12px;}
      h1{font-size:16px;margin:0 0 4px;} h2{font-size:14px;margin:0 0 12px;color:#444;}
      table{width:100%;border-collapse:collapse;margin-top:12px;}
      th,td{border-bottom:1px solid #ddd;padding:6px 8px;text-align:left;}
      th{background:#f3f4f6;} .sag{text-align:right;} .poz{color:#15803d;} .neg{color:#b91c1c;}
      .ozet{margin-top:12px;text-align:right;font-size:13px;}
      @media print{.no-print{display:none;}}
    </style></head><body>
      <div class="no-print" style="text-align:right;margin-bottom:8px;">
        <button onclick="window.print()">${escapeHtml(t('common.print'))}</button>
      </div>
      <h1>${escapeHtml(t('cariHesaplar.ekstreBaslik'))}</h1>
      <h2>${escapeHtml(cari.ad || '')}${aralikMetni ? ' â€” ' + escapeHtml(aralikMetni) : ''}</h2>
      <table><thead><tr>
        <th>${escapeHtml(t('common.date'))}</th>
        <th>${escapeHtml(t('cariHesaplar.tur'))}</th>
        <th class="sag">${escapeHtml(t('cariHesaplar.borc'))}</th>
        <th class="sag">${escapeHtml(t('common.amount'))}</th>
        <th class="sag">${escapeHtml(t('cariHesaplar.bakiye'))}</th>
        <th>${escapeHtml(t('common.description'))}</th>
      </tr></thead><tbody>${satirlar}</tbody></table>
      ${basSatiri}
      <div class="ozet"><strong>${escapeHtml(t('cariHesaplar.guncelBakiye'))}: ${formatCurrency(sonBakiye)}</strong></div>
    </body></html>`
  const pencere = fisPenceresiAcVeYazdir(html)
  if (!pencere) toastBildirim.hata(t('common.popupEngellendi'))
}

const hareketTuruEtiketi = (tur) =>
  ({ TAHSILAT: t('cariHesaplar.tahsilat'), ODEME: t('cariHesaplar.odeme'), BORC: t('cariHesaplar.borclandirma') })[tur] || tur


onMounted(async () => {
  // 360Â° gÃ¶rÃ¼nÃ¼mlerden Ã§apraz gezinme: arama terimini uygula.
  const q = route?.query?.q
  if (typeof q === 'string' && q.trim()) aramaMetni.value = q.trim()
  await loadCariHesaplar()
  cariOzetYukle()
  etiketleriYukle()
})

const cariSayfa = ref(0)
const cariSayfaBoyutu = ref(10)
const siralama = ref({ field: 'ad', order: 1 })

const loadCariHesaplar = async (sayfa = cariSayfa.value, boyut = cariSayfaBoyutu.value) => {
  loading.value = true
  try {
    const params = {
      page: sayfa,
      size: boyut,
      sort: `${siralama.value.field},${siralama.value.order === -1 ? 'desc' : 'asc'}`
    }
    if (aramaMetni.value.trim()) params.q = aramaMetni.value.trim()
    if (filtreTur.value) params.tur = filtreTur.value
    if (filtreBakiye.value) params.bakiyeYonu = filtreBakiye.value
    if (filtreEtiket.value && filtreEtiket.value.trim().length >= 2) params.etiket = filtreEtiket.value.trim()
    await cariHesapStore.filtreliCari(params)
  } catch (error) {
    toastBildirim.hata(t('cariHesaplar.hataYukleme'))
  } finally {
    loading.value = false
    ilkYukleme.value = false
  }
}

const cariSayfaDegisti = (e) => {
  cariSayfa.value = e.page
  cariSayfaBoyutu.value = e.rows
  loadCariHesaplar(e.page, e.rows)
}

const cariSirala = (e) => {
  const alan = e?.sortField || 'ad'
  const yon = e?.sortOrder === -1 ? -1 : 1
  siralama.value = { field: alan, order: yon }
  cariSayfa.value = 0
  loadCariHesaplar(0, cariSayfaBoyutu.value)
}

const ara = () => {
  if (aramaZamanlayici) clearTimeout(aramaZamanlayici)
  aramaZamanlayici = setTimeout(async () => {
    cariSayfa.value = 0
    await loadCariHesaplar(0, cariSayfaBoyutu.value)
  }, 300)
}

const openDialog = () => {
  formCariId.value = null
  formBaslangicVerisi.value = null
  showDialog.value = true
}

const formKaydedildi = async () => {
  await Promise.allSettled([loadCariHesaplar(), cariOzetYukle()])
}

const cariEylemleri = (c) => {
  const eylemler = [
    { grup: t('cariHesaplar.grupFinans') },
    {
      etiket: t('cariHesaplar.tahsilatAl'),
      aciklama: t('cariHesaplar.tahsilatAlAciklama'),
      ikon: 'pi pi-money-bill',
      sinif: 'eylem-pozitif',
      islem: () => tahsilatAc(c)
    },
    {
      etiket: t('cariHesaplar.borcEkle'),
      aciklama: t('cariHesaplar.borcEkleAciklama'),
      ikon: 'pi pi-plus-circle',
      sinif: 'eylem-uyari',
      islem: () => borclandirmaAc(c)
    },
    {
      etiket: t('cariHesaplar.acilisFisi'),
      aciklama: t('cariHesaplar.acilisFisiAciklama'),
      ikon: 'pi pi-flag',
      islem: () => acilisAc(c)
    },
    { grup: t('cariHesaplar.grupKayit') },
    { etiket: t('cariHesaplar.detay360'), ikon: 'pi pi-id-card', islem: () => kartAc(c) },
    { etiket: t('common.edit'), ikon: 'pi pi-pencil', islem: () => editCariHesap(c) },
    { etiket: t('cariHesaplar.yeniFatura'), ikon: 'pi pi-file-plus', islem: () => yeniFatura(c) },
    { grup: t('cariHesaplar.grupIletisim') },
    {
      etiket: t('cariHesaplar.telefonKopyala'),
      ikon: 'pi pi-copy',
      islem: () => kopyala(c.telefon, t('cariHesaplar.telefonKopyalandi'))
    },
    { etiket: t('cariHesaplar.ara'), ikon: 'pi pi-phone', islem: () => cariAraTelefon(c) },
    { etiket: t('cariHesaplar.epostaGonder'), ikon: 'pi pi-envelope', islem: () => cariEpostaGonder(c) },
    { etiket: t('cariHesaplar.whatsapp'), ikon: 'pi pi-whatsapp', islem: () => cariWhatsapp(c) },
    { grup: t('cariHesaplar.grupDiger') },
    {
      etiket: c.aktif === false ? t('cariHesaplar.aktifYap') : t('cariHesaplar.pasifYap'),
      ikon: 'pi pi-power-off',
      islem: () => aktifPasifToggle(c)
    },
    { etiket: t('cariHesaplar.cogalt'), ikon: 'pi pi-copy', islem: () => cariCogalt(c) }
  ]
  // Cari silme yalnizca ADMIN; islem kaydi olan cari backend'de engellenir.
  if (isAdmin.value) {
    eylemler.push({ ayrac: true })
    eylemler.push({ etiket: t('common.delete'), ikon: 'pi pi-trash', sinif: 'eylem-sil', islem: () => confirmDelete(c.id) })
  }
  return eylemler
}

const cariAraTelefon = (c) => {
  const tel = String(c.telefon || '').replace(/\s/g, '')
  if (!tel) {
    toastBildirim.uyari(t('cariHesaplar.telefonYok'))
    return
  }
  window.location.href = `tel:${tel}`
}

const cariEpostaGonder = (c) => {
  if (!c.email) {
    toastBildirim.uyari(t('cariHesaplar.epostaYok'))
    return
  }
  window.location.href = `mailto:${c.email}`
}

const cariWhatsapp = (c) => {
  const tel = String(c.telefon || '').replace(/\D/g, '')
  if (!tel) {
    toastBildirim.uyari(t('cariHesaplar.telefonYok'))
    return
  }
  window.open(`https://wa.me/${tel}`, '_blank')
}

const aktifPasifToggle = async (c) => {
  try {
    await cariHesapStore.updateCariHesap(c.id, { ...c, aktif: c.aktif === false })
    toastBildirim.basarili(t('cariHesaplar.guncellendi'))
    await Promise.allSettled([loadCariHesaplar(), cariOzetYukle()])
  } catch (e) {
    toastBildirim.hata(e?.response?.data?.message || t('cariHesaplar.islemBasarisiz'))
  }
}

// Cari kopyasi: form, benzersiz alanlar (vergi no/iban) temizlenmis sekilde
// on-dolu ACILIR; kaydetmeden mukerrer kayit olusturulmaz.
const cariCogalt = (c) => {
  formCariId.value = null
  formBaslangicVerisi.value = {
    ad: `${c.ad} (Kopya)`,
    tur: c.tur || '',
    vergiNumarasi: '',
    vergiDairesi: c.vergiDairesi || '',
    telefon: c.telefon || '',
    email: c.email || '',
    iban: '',
    il: c.il || '',
    ilce: c.ilce || '',
    adres: c.adres || '',
    yetkiliKisi: c.yetkiliKisi || '',
    yetkiliTelefon: c.yetkiliTelefon || '',
    krediLimiti: c.krediLimiti ?? null,
    odemeVadesi: c.odemeVadesi ?? 0,
    temsilciId: c.temsilciId ?? null,
    temsilciAd: c.temsilciAd || null,
    notlar: c.notlar || '',
    etiketler: c.etiketler || '',
    paraBirimi: c.paraBirimi || 'TRY',
    fotoUrl: c.fotoUrl || '',
    fotoThumbUrl: c.fotoThumbUrl || '',
    aktif: c.aktif !== false
  }
  showDialog.value = true
}

const editCariHesap = (cariHesap) => {
  formCariId.value = cariHesap.id
  formBaslangicVerisi.value = cariHesap
  showDialog.value = true
}

const confirmDelete = (id) => {
  confirm.require({
    message: t('cariHesaplar.silOnayMesaj'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('common.evetSil'), severity: 'danger', size: 'small' },
    accept: () => deleteCariHesap(id),
    reject: () => {}
  })
}

const deleteCariHesap = async (id) => {
  try {
    await cariHesapStore.deleteCariHesap(id)
    toastBildirim.basarili(t('cariHesaplar.silindi'))
    // Silinen kayÄ±t liste ve KPI'dan da dÃ¼ÅŸmeli.
    await Promise.allSettled([loadCariHesaplar(), cariOzetYukle()])
  } catch (error) {
    toastBildirim.hata(t('cariHesaplar.silmeHata'))
  }
}

const batchSil = () => {
  if (selectedCariHesaplar.value.length === 0) return
  confirm.require({
    message: t('cariHesaplar.topluSilOnayMesaj', { n: selectedCariHesaplar.value.length }),
    header: t('common.topluSilmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      try {
        const ids = selectedCariHesaplar.value.map((c) => c.id).filter((v) => v != null)
        const r = await cariHesapAPI.topluSil(ids)
        const silinen = r.data?.silinen?.length || 0
        const atlanan = r.data?.atlanan || []
        if (silinen > 0) toastBildirim.basarili(t('cariHesaplar.topluSilindiSayi', { n: silinen }))
        atlanan.forEach((a) => toastBildirim.hata(a.neden))
      } catch (error) {
        toastBildirim.hata(error?.response?.data?.message || t('cariHesaplar.silmeHata'))
      } finally {
        selectedCariHesaplar.value = []
        await loadCariHesaplar()
      }
    }
  })
}

const batchCsvExport = () => {
  const ids = selectedCariHesaplar.value.map((c) => c.id).join(',')
  window.open(`/api/cari-hesaplar/export/csv?ids=${ids}`, '_blank')
}

const yeniFatura = (cariHesap) => {
  router.push({ name: 'Faturalar', query: { cariId: cariHesap.id } })
}

const tahsilatDialog = ref(false)
const tahsilatHedefCari = ref(null)

const tahsilatAc = async (cariHesap) => {
  tahsilatHedefCari.value = cariHesap
  tahsilatDialog.value = true
  try {
    const r = await faturaAPI.cariFaturalari(cariHesap.id, { size: 200 })
    const faturalar = unwrapList(r)
    tahsilatHedefCari.value = {
      ...cariHesap,
      acikFaturalar: faturalar
        .filter((f) => Number(f.kalanTutar) > 0)
        .map((f) => ({
          faturaId: f.id,
          faturaNumarasi: f.faturaNumarasi,
          vadeTarihi: f.vadeTarihi,
          kalanTutar: f.kalanTutar
        }))
    }
  } catch {
    tahsilatHedefCari.value = cariHesap
  }
}

// Tahsilat/borÃ§landÄ±rma sonrasÄ± yenileme.
// Ã–NCE getAllCariHesaplar() Ã§aÄŸrÄ±lÄ±yordu; bu parametresiz (size=50) tÃ¼m ÅŸirket
// sayfasÄ±nÄ± getirip listeyi eziyor, dolayÄ±sÄ±yla kullanÄ±cÄ±nÄ±n arama/tÃ¼r/bakiye
// filtresi ve bulunduÄŸu sayfa sessizce kayboluyordu. KPI kartlarÄ± (ozet) de
// filtreyi yok sayar; o da yenilenmeli.
const islemSonrasiYenile = async () => {
  try {
    await loadCariHesaplar()
    await cariOzetYukle()
  } catch {
    /* yenileme hatasi global olarak bildirilir */
  }
}

const tahsilatSonrasiYenile = async () => {
  tahsilatHedefCari.value = null
  await islemSonrasiYenile()
}

const borclandirmaDialog = ref(false)
const borclandirmaHedefCari = ref(null)

const borclandirmaAc = (cariHesap) => {
  borclandirmaHedefCari.value = cariHesap
  borclandirmaDialog.value = true
}

const borclandirmaSonrasiYenile = async () => {
  borclandirmaHedefCari.value = null
  await islemSonrasiYenile()
  if (showHareketlerDialog.value && selectedCariHesap.value) {
    await viewHareketler(selectedCariHesap.value)
  }
}

const kartAc = (cariHesap) => {
  kartCari.value = cariHesap
  kartCariId.value = cariHesap.id
  kartDialogAcik.value = true
}

const viewHareketler = async (cariHesap) => {  selectedCariHesap.value = cariHesap
  showHareketlerDialog.value = true
  cariHareketlerYukleniyor.value = true
  try {
    const res = await hareketAPI.getByCariHesap(cariHesap.id)
    cariHareketler.value = res.data._embedded
      ? res.data._embedded.hareketler || res.data._embedded.hareketList || []
      : Array.isArray(res.data)
        ? res.data
        : res.data.content || []
  } catch (error) {
    toastBildirim.hata(t('cariHesaplar.hareketlerHata'))
    cariHareketler.value = []
  } finally {
    cariHareketlerYukleniyor.value = false
  }
  cariNotlariYukle(cariHesap.id)
  sonUrunleriYukle(cariHesap.id)
  cariFaturalariYukle(cariHesap.id)
  cariOzelFiyatlariYukle(cariHesap.id)
}

// Cari 360 penceresinden tetiklenen alt dialoglar (ekstre / adres).
const ekstreAc = () => {
  if (kartCari.value) viewHareketler(kartCari.value)
}
const adresAc360 = () => {
  if (kartCari.value) yardimciDiyaloglarRef.value?.adresAc(kartCari.value)
}

const cariOzelFiyatlar = ref([])
const ozelFiyatStok = ref(null)
const ozelFiyatTutar = ref(null)
// Ã–zel fiyat ekleme sekmesindeki stok seÃ§ici sunucu aramalÄ±. Sayfa aÃ§Ä±lÄ±ÅŸÄ±nda
// 1000 kayÄ±t Ã§ekiliyordu ve dropdown yalnÄ±zca bu sekme aÃ§Ä±ldÄ±ÄŸÄ±nda gÃ¶rÃ¼ndÃ¼ÄŸÃ¼
// iÃ§in hem gereksiz aÄŸ trafiÄŸi hem de 1000. stoktan sonrasÄ±nÄ±n seÃ§ilememesi
const cariOzelFiyatlariYukle = async (cariId) => {
  try {
    const r = await cariHesapAPI.getFiyatlar(cariId)
    cariOzelFiyatlar.value = r.data || []
  } catch {
    cariOzelFiyatlar.value = []
  }
}

/** Özel fiyat sekmesinden gelen değerlerle kaydeder. */
const ozelFiyatEkleGonder = ({ stokId, tutar }) => {
  ozelFiyatStok.value = stokId
  ozelFiyatTutar.value = tutar
  cariOzelFiyatEkle()
}

const cariOzelFiyatEkle = async () => {
  if (!ozelFiyatStok.value || !ozelFiyatTutar.value) {
    toastBildirim.uyari(t('cariHesaplar.urunFiyatSecin'))
    return
  }
  try {
    await cariHesapAPI.fiyatKaydet(selectedCariHesap.value.id, {
      stokId: ozelFiyatStok.value,
      fiyat: ozelFiyatTutar.value
    })
    ozelFiyatStok.value = null
    ozelFiyatTutar.value = null
    cariOzelFiyatlariYukle(selectedCariHesap.value.id)
    toastBildirim.basarili(t('cariHesaplar.ozelFiyatEklendi'))
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('cariHesaplar.fiyatEklenemedi'))
  }
}

const cariOzelFiyatSil = (f) => {
  confirm.require({
    message: t('common.silmeOnayMesaji'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('common.evetSil'), severity: 'danger', size: 'small' },
    accept: async () => {
      try {
        await cariHesapAPI.fiyatSil(f.id)
        cariOzelFiyatlar.value = cariOzelFiyatlar.value.filter((x) => x.id !== f.id)
        toastBildirim.basarili(t('cariHesaplar.ozelFiyatSilindi'))
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('cariHesaplar.silinemedi'))
      }
    }
  })
}

const cariFaturalar = ref([])

const cariFaturalariYukle = async (cariId) => {
  try {
    const r = await faturaAPI.cariFaturalari(cariId, { size: 100 })
    cariFaturalar.value = unwrapList(r)
  } catch {
    cariFaturalar.value = []
  }
}

const faturaDetayDialog = ref(false)
const seciliFatura = ref(null)

const faturaDetayAc = async (fatura) => {
  try {
    const r = await faturaAPI.getById(fatura.id)
    seciliFatura.value = r.data
    faturaDetayDialog.value = true
  } catch {
    seciliFatura.value = fatura
    faturaDetayDialog.value = true
  }
}

const sonUrunler = ref([])

const sonUrunleriYukle = async (cariId) => {
  try {
    const r = await faturaAPI.cariSonUrunler(cariId, 20)
    sonUrunler.value = r.data || []
  } catch {
    sonUrunler.value = []
  }
}

const seciliUrunFiyatGecmisi = ref(null)

/** Ürün fiyat geçmişi panelini kapatır. */
const urunFiyatKapat = () => {
  seciliUrunFiyatGecmisi.value = null
}

const urunFiyatGecmisiGoster = async (urun) => {
  if (!selectedCariHesap.value || !urun.stokId) return
  try {
    const r = await faturaAPI.cariUrunFiyatGecmisi(selectedCariHesap.value.id, urun.stokId)
    seciliUrunFiyatGecmisi.value = { ...r.data, urunAd: urun.stokAd }
  } catch {
    seciliUrunFiyatGecmisi.value = null
  }
}

const cariNotlar = ref([])
const yeniCariNot = ref('')
const yeniCariNotOnem = ref('NORMAL')
const cariNotlariYukle = async (cariId) => {
  try {
    const r = await notAPI.cariNotlari(cariId)
    cariNotlar.value = r.data || []
  } catch {
    cariNotlar.value = []
  }
}

/** Not sekmesinden gelen iÃ§erikle not ekler. */
const notEkleGonder = ({ baslik, onem }) => {
  yeniCariNot.value = baslik
  yeniCariNotOnem.value = onem
  cariNotEkle()
}

const cariNotEkle = async () => {
  const metin = yeniCariNot.value.trim()
  if (!metin || !selectedCariHesap.value) return
  try {
    await notAPI.create({
      baslik: yeniCariNotOnem.value === 'YUKSEK' ? t('cariHesaplar.onemliNot') : t('cariHesaplar.gorusme'),
      icerik: metin,
      cariHesapId: selectedCariHesap.value.id,
      onemDerecesi: yeniCariNotOnem.value
    })
    yeniCariNot.value = ''
    yeniCariNotOnem.value = 'NORMAL'
    await cariNotlariYukle(selectedCariHesap.value.id)
  } catch (err) {
    toastBildirim.hata(t('cariHesaplar.notEklenemedi'))
  }
}

const cariNotSil = (n) => {
  confirm.require({
    message: t('common.silmeOnayMesaji'),
    header: t('common.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    rejectProps: { label: t('common.vazgec'), severity: 'secondary', outlined: true, size: 'small' },
    acceptProps: { label: t('common.evetSil'), severity: 'danger', size: 'small' },
    accept: async () => {
      try {
        await notAPI.delete(n.id)
        await cariNotlariYukle(selectedCariHesap.value.id)
      } catch (err) {
        toastBildirim.hata(t('cariHesaplar.notSilinemedi'))
      }
    }
  })
}

const csvExport = () => {
  window.open('/api/cari-hesaplar/export/csv', '_blank')
}

const excelIndir = async () => {
  try {
    const res = await excelAPI.cariHesaplar()
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'CariHesaplar.xlsx')
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  } catch {
    /* silent */
  }
}

// Ust arac cubugundaki "Islemler" menusu: Excel/CSV/riskli/toplu islemleri
// tek tetikte toplar (eskiden 4 ayri buton vardi ve hiyerarsi zayifti).
const islemMenusu = computed(() => [
  { label: t('cariHesaplar.riskliCariler'), icon: 'pi pi-exclamation-triangle', command: () => riskliCarilerAc() },
  {
    label: t('cariHesaplar.topluGuncelle'),
    icon: 'pi pi-pencil',
    disabled: selectedCariHesaplar.value.length === 0,
    command: () => topluGuncelleAc()
  },
  { separator: true },
  { label: t('cariHesaplar.excel'), icon: 'pi pi-file-excel', command: () => excelIndir() },
  { label: t('cariHesaplar.csv'), icon: 'pi pi-download', command: () => csvExport() }
])


import { formatTarih as formatDate } from '../utils/format.js'
</script>

<style scoped>






.fatura-link {
  color: var(--accent);
  cursor: pointer;
  font-weight: 600;
  text-decoration: none;
}
.fatura-link:hover {
  text-decoration: underline;
}

































.cari-ozel-fiyatlar {
  margin-top: 20px;
  border-top: 1px solid var(--border);
  padding-top: 14px;
}
.ozel-fiyat-satir {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
  border-bottom: 1px solid var(--border);
}
.ozel-fiyat-urun {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
}
.ozel-fiyat-tutar {
  font-size: 13px;
  font-weight: 700;
  color: var(--accent);
}
.ozel-fiyat-ekle {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  align-items: center;
}
.ozel-fiyat-stok-select {
  flex: 1;
}
.ozel-fiyat-tutar-input {
  width: 140px;
}
.cari-faturalar {
  margin-top: 20px;
  border-top: 1px solid var(--border);
  padding-top: 14px;
}
.cari-urunler {
  margin-top: 20px;
  border-top: 1px solid var(--border);
  padding-top: 14px;
}
.cari-urunler-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.cari-urun-chip {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 8px 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  text-align: left;
}
.cari-urun-chip:hover {
  border-color: var(--accent);
  transform: translateY(-1px);
}
.cari-urun-ad {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}
.cari-urun-bilgi {
  font-size: 11px;
  color: var(--text-muted);
}
.cari-urun-fiyat {
  font-size: 12px;
  font-weight: 700;
  color: var(--accent);
}
.cari-urun-fiyat-gecmisi {
  margin-top: 12px;
  padding: 12px;
  background: rgba(139, 92, 246, 0.06);
  border: 1px solid rgba(139, 92, 246, 0.25);
  border-radius: 8px;
}
.fg-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  font-weight: 600;
  color: #a78bfa;
  margin-bottom: 8px;
}
.fg-kapat {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
}
.fg-satir {
  display: flex;
  gap: 12px;
  align-items: center;
  font-size: 12px;
  color: var(--text-secondary);
  padding: 3px 0;
}
.fg-tarih {
  width: 80px;
  flex-shrink: 0;
}
.fg-fatura {
  flex: 1;
}
.fg-adet {
  width: 60px;
  text-align: right;
}
.fg-fiyat {
  font-weight: 600;
  color: var(--text-primary);
  min-width: 80px;
  text-align: right;
}
.cari-notlar {
  margin-top: 20px;
  border-top: 1px solid var(--border);
  padding-top: 14px;
}
.cari-not-baslik h4 {
  margin: 0 0 10px;
  font-size: 14px;
}
.cari-not-ekle {
  display: flex;
  gap: 8px;
  align-items: center;
}
.not-onem-select {
  width: 110px;
  flex-shrink: 0;
}
.cari-not-baslik-satir {
  display: flex;
  align-items: center;
  gap: 8px;
}
.not-onem-rozet {
  padding: 1px 8px;
  border-radius: 20px;
  font-size: 10px;
  font-weight: 700;
}
.not-onem-rozet.yuksek {
  background: var(--warning-soft);
  color: var(--warning);
}
.not-onem-rozet.kritik {
  background: var(--danger-soft);
  color: var(--danger);
}
.cari-not-ekle .p-inputtext {
  flex: 1;
}
.cari-not-bos {
  color: var(--text-muted);
  font-size: 13px;
  padding: 8px 0;
}
.cari-not-satir {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
}
.cari-not-icerik strong {
  font-size: 13px;
}
.cari-not-icerik p {
  margin: 2px 0;
  font-size: 13px;
}
.cari-not-icerik small {
  color: var(--text-muted);
  font-size: 11px;
}

.cari-hesaplar-container {
  padding: 0;
  max-width: 100%;
}

h1 {
  color: var(--text-primary);
  margin-bottom: 20px;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

h3 {
  color: var(--text-primary);
  margin: 0 0 10px 0;
}

.toolbar {
  margin-bottom: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px 18px;
}


/* Yatay kaydÄ±rmada kimlik ve iÅŸlem kolonlarÄ± gÃ¶rÃ¼nÃ¼r kalsÄ±n. */
.table-container :deep(.cari-secim-kolon),
.table-container :deep(.cari-ad-kolon),




.table-container :deep(thead .cari-secim-kolon),
.table-container :deep(thead .cari-ad-kolon),

/* SeÃ§im kutusu: gÃ¶rsel kutu 20 px, dokunma alanÄ± 40Ã—40 px (telefon hedefi). */





.loading {
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
  font-size: 16px;
}




.ekstre-arac {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}
.ekstre-hizli {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-secondary);
}
.ekstre-hizli :deep(.p-button) {
  padding: 4px 10px;
  font-size: 12px;
}
.ekstre-tarih {
  width: 150px;
}








.error {
  display: block;
  color: var(--danger);
  font-size: 11px;
  margin-top: 4px;
}


.positive {
  color: var(--success);
  font-weight: bold;
}



.badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: bold;
}

.badge.tahsilat {
  background-color: var(--success-soft);
  color: var(--success);
}

.badge.odeme {
  background-color: var(--danger-soft);
  color: var(--danger);
}

.badge.borclandirma {
  background-color: var(--warning-soft, rgba(245, 158, 11, 0.15));
  color: var(--warning, #f59e0b);
}

.hareket-info {
  background: var(--bg-muted);
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 15px;
}

.hareket-info p {
  margin: 5px 0 0 0;
}

.bakiye-ozet {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--border);
}

.ozet-satir {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ozet-etiket {
  font-size: 12px;
  color: var(--text-secondary);
}

.ozet-bakiye {
  margin-left: auto;
}

.w-full {
  width: 100% !important;
}




.kopyalanabilir:hover 



</style>
