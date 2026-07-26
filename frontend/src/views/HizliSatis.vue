<template>
  <div class="pos-container">
    <div class="pos-header">
      <div class="breadcrumb"><i class="pi pi-home"></i> Anasayfa / POS / Yeni Satış</div>
      <div class="user-info" v-if="authStore.kullanici">
        <i class="pi pi-user"></i> {{ authStore.kullanici.ad || authStore.kullanici.kullaniciAdi }}
      </div>
    </div>

    <div class="pos-body grid">
      <div class="col-8 pos-left">
        <Card class="filter-card">
          <template #title>
            <div class="filter-title">
              <span>Hızlı Filtreler</span>
              <Button label="Temizle" severity="secondary" size="small" @click="filtreleriTemizle" />
            </div>
          </template>
          <template #content>
            <div class="filter-row">
              <Dropdown v-model="filtreKategori" :options="kategoriler" optionLabel="ad" placeholder="Kategori" class="filter-select" showClear />
              <Dropdown v-model="filtreArac" :options="aracListesi" placeholder="Araç" class="filter-select" showClear />
            </div>
          </template>
        </Card>

        <div class="serial-search">
          <span class="p-input-icon-left">
            <i class="pi pi-search" />
            <InputText v-model="seriNoArama" placeholder="Seri No ile ara..." class="w-full" />
          </span>
        </div>

        <div class="product-section">
          <div class="product-header">
            <h3>Mevcut Ahşap ({{ filtrelenmisUrunler.length }})</h3>
          </div>
          <div class="product-grid">
            <div v-for="u in filtrelenmisUrunler" :key="u.id" class="product-card" @click="sepeteEkle(u)">
              <div class="product-img">
                <i class="pi pi-box"></i>
              </div>
              <Tag :value="(u.miktar || 0) + ' ' + (u.birim || 'adet')" severity="info" class="stock-badge" />
              <div class="product-details">
                <span class="product-name">{{ u.ad }}</span>
                <span class="product-serial">{{ u.barkod || '-' }}</span>
                <span class="product-meta">{{ u.marka || '-' }} / {{ u.olcu || '-' }} / {{ u.birimHacim || '-' }} ft³</span>
                <span class="product-price">{{ formatCurrency(u.fiyat || u.satisFiyati || 0) }}</span>
              </div>
            </div>
            <div v-if="filtrelenmisUrunler.length === 0" class="empty-products">
              <i class="pi pi-inbox"></i>
              <p>Ürün bulunamadı</p>
            </div>
          </div>
        </div>
      </div>

      <div class="col-4 pos-right">
        <Card class="customer-card">
          <template #title>Müşteri</template>
          <template #content>
            <div class="customer-field">
              <div class="anlik-musteri">
                <ToggleButton v-model="anlikMusteri" onLabel="Anlık Müşteri" offLabel="Müşteri Seç" :onIcon="anlikMusteri ? 'pi pi-check' : 'pi pi-users'" class="w-full" />
              </div>
              <template v-if="!anlikMusteri">
                <AutoComplete v-model="seciliMusteri" :suggestions="musteriOnerileri" @complete="musteriAra($event)"
                  optionLabel="ad" placeholder="Müşteri ara ve seç..." class="w-full" :forceSelection="true">
                  <template #option="slotProps">
                    <div class="musteri-option">{{ slotProps.option.ad }} <span class="musteri-option-detay">{{ slotProps.option.vergiNo || slotProps.option.telefon }}</span></div>
                  </template>
                </AutoComplete>
                <Button label="+ Yeni" severity="secondary" size="small" @click="yeniMusteriDialog = true" />
              </template>
            </div>
          </template>
        </Card>

        <Card class="sepet-card">
          <template #title>
            <div class="sepet-header">
              <span>Sipariş Özeti ({{ sepet.length }})</span>
              <Button v-if="sepet.length" label="Temizle" icon="pi pi-trash" severity="danger" size="small" @click="sepet = []" />
            </div>
          </template>
          <template #content>
            <div v-if="sepet.length === 0" class="sepet-bos">Sepete ürün ekleyin</div>
            <div v-for="(item, idx) in sepet" :key="idx" class="sepet-item">
              <div class="sepet-ad">{{ item.ad }}</div>
              <div class="sepet-satir">
                <Button icon="pi pi-minus" rounded text severity="secondary" size="small" @click="miktarAzalt(idx)" />
                <span class="sepet-adet">{{ item.miktar }}</span>
                <Button icon="pi pi-plus" rounded text severity="secondary" size="small" @click="item.miktar++" />
                <span class="sepet-birimfiyat">{{ formatCurrency(item.fiyat) }}</span>
                <span class="sepet-tutar">{{ formatCurrency(item.miktar * item.fiyat) }}</span>
                <Button icon="pi pi-times" rounded text severity="danger" size="small" @click="sepetSil(idx)" />
              </div>
            </div>
            <hr class="ozet-ayrac" />
            <div class="ozet-satir">
              <span>Toplam Ft³</span>
              <span>{{ toplamFt3.toFixed(2) }} ft³</span>
            </div>
            <div class="ozet-satir">
              <span>İndirim</span>
              <div class="ozet-indirim">
                <SelectButton v-model="indirimTipi" :options="indirimTipleri" optionLabel="label" optionValue="value" />
                <InputNumber v-model="indirimDegeri" :min="0" :max="indirimTipi === 'yuzde' ? 100 : toplam" :suffix="indirimTipi === 'yuzde' ? '%' : ' ₺'" class="indirim-input" />
              </div>
            </div>
            <div class="ozet-satir ozet-genel">
              <span>Genel Toplam</span>
              <span class="genel-toplam-deger">{{ formatCurrency(genelToplam) }}</span>
            </div>
          </template>
        </Card>

        <Card class="odeme-card">
          <template #title>Ödeme</template>
          <template #content>
            <SelectButton v-model="odemeDurumu" :options="odemeTipleri" optionLabel="label" optionValue="value" class="w-full" />
            <div class="odenen-satir" v-if="odemeDurumu !== 'yok'">
              <label>Ödenen Tutar</label>
              <InputNumber v-model="odenenTutar" :min="0" :max="genelToplam" mode="currency" currency="TRY" locale="tr-TR" class="w-full" />
            </div>
            <div class="odeme-durum">
              <Tag :value="odemeDurumText" :severity="odemeDurumSeverity" class="w-full" />
            </div>
            <div class="odeme-kalan" v-if="kalanTutar > 0">
              <span>Kalan:</span>
              <span class="kalan-deger">{{ formatCurrency(kalanTutar) }}</span>
            </div>
          </template>
        </Card>

        <Card class="fis-card">
          <template #title>
            <div class="fis-card-header">
              <span><i class="pi pi-print"></i> Termal Yazıcı Fiş Önizlemesi</span>
              <Button label="Yazdır" icon="pi pi-print" size="small" @click="fisiYazdir" :disabled="sepet.length === 0" />
            </div>
          </template>
          <template #content>
            <div class="fis-onizleme-kapsam">
              <div class="fis-onizleme" id="fisOnizleme">
                <div class="fis-header">
                  <div class="fis-baslik">{{ sirketAdi || 'RASPEL ERP' }}</div>
                  <div class="fis-tarih">{{ simdikiTarih }}</div>
                  <div class="fis-fisno">Fiş No: {{ fisNo || '-------' }}</div>
                </div>
                <div class="fis-musteri" v-if="musteriAdi">
                  <span>Müşteri: {{ musteriAdi }}</span>
                </div>
                <div class="fis-ayrac">---</div>
                <div class="fis-kalemler">
                  <div v-for="i in sepet" :key="i.id" class="fis-kalem">
                    <div class="fis-kalem-ad">{{ i.ad }} x{{ i.miktar }}</div>
                    <div class="fis-kalem-tutar">{{ formatCurrency(i.miktar * i.fiyat) }}</div>
                  </div>
                </div>
                <div class="fis-ayrac">---</div>
                <div class="fis-toplam">
                  <span>Ara Toplam</span>
                  <span>{{ formatCurrency(toplam) }}</span>
                </div>
                <div class="fis-indirim" v-if="indirimDegeri > 0">
                  <span>İndirim ({{ indirimTipi === 'yuzde' ? indirimDegeri + '%' : '' }})</span>
                  <span>-{{ formatCurrency(indirimTutari) }}</span>
                </div>
                <div class="fis-genel-toplam">
                  <span>GENEL TOPLAM</span>
                  <span class="fis-toplam-deger">{{ formatCurrency(genelToplam) }}</span>
                </div>
                <div class="fis-ayrac">---</div>
                <div class="fis-odeme">
                  <div class="fis-odeme-satir">
                    <span>Ödenen</span>
                    <span>{{ formatCurrency(odenenTutar) }}</span>
                  </div>
                  <div class="fis-odeme-satir" v-if="kalanTutar > 0">
                    <span>Kalan</span>
                    <span>{{ formatCurrency(kalanTutar) }}</span>
                  </div>
                  <div class="fis-odeme-satir fis-odeme-durum">
                    <span>Durum</span>
                    <span>{{ odemeDurumText }}</span>
                  </div>
                </div>
                <div class="fis-footer">
                  <div class="fis-ayrac">---</div>
                  <div class="fis-tesekkur">İyi günler dileriz</div>
                </div>
              </div>
            </div>
          </template>
        </Card>

        <Button label="Satışı Tamamla" icon="pi pi-check" class="p-button-success w-full satis-buton" @click="satisiTamamla"
          :loading="kaydediliyor" :disabled="sepet.length === 0 || (!anlikMusteri && !seciliMusteri)" />
      </div>
    </div>

    <Dialog v-model:visible="yeniMusteriDialog" header="Yeni Cari Hesap" :modal="true" :style="{ width: '450px' }" class="yeni-musteri-dialog">
      <div class="p-fluid">
        <div class="field">
          <label for="ym-ad">Ad / Firma Adı <span class="required">*</span></label>
          <InputText id="ym-ad" v-model="yeniMusteri.ad" />
        </div>
        <div class="field">
          <label for="ym-telefon">Telefon</label>
          <InputText id="ym-telefon" v-model="yeniMusteri.telefon" />
        </div>
        <div class="field">
          <label for="ym-email">E-posta</label>
          <InputText id="ym-email" v-model="yeniMusteri.email" />
        </div>
        <div class="field">
          <label for="ym-adres">Adres</label>
          <Textarea id="ym-adres" v-model="yeniMusteri.adres" rows="3" />
        </div>
        <div class="field">
          <label for="ym-vergi">Vergi No</label>
          <InputText id="ym-vergi" v-model="yeniMusteri.vergiNo" />
        </div>
      </div>
      <template #footer>
        <Button label="İptal" icon="pi pi-times" class="p-button-text" @click="yeniMusteriDialog = false" />
        <Button label="Kaydet" icon="pi pi-check" @click="musteriKaydet" :loading="musteriKaydediliyor" />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useAuthStore } from '../stores/authStore.js'
import { useCariHesapStore } from '../stores/cariHesapStore.js'
import { useStokStore } from '../stores/stokStore.js'
import { useKategoriStore } from '../stores/kategoriStore.js'
import { faturaAPI, cariHesapAPI } from '../api/index.js'
import AutoComplete from 'primevue/autocomplete'
import SelectButton from 'primevue/selectbutton'
import ToggleButton from 'primevue/togglebutton'

const toast = useToast()
const authStore = useAuthStore()
const cariHesapStore = useCariHesapStore()
const stokStore = useStokStore()
const kategoriStore = useKategoriStore()

const sirketAdi = computed(() => authStore.sirketAdi || '')

const aramaMetni = ref('')
const seriNoArama = ref('')

const filtreKategori = ref(null)
const filtreArac = ref(null)

const seciliMusteri = ref(null)
const anlikMusteri = ref(false)
const musteriOnerileri = ref([])
const yeniMusteriDialog = ref(false)
const yeniMusteri = ref({ ad: '', telefon: '', email: '', adres: '', vergiNo: '' })
const musteriKaydediliyor = ref(false)

const sepet = ref([])
const kaydediliyor = ref(false)
const fisNo = ref('')

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
  stokStore.stoklar.forEach(s => {
    if (s.marka) araclar.add(s.marka)
  })
  return [...araclar].sort()
})

const toplam = computed(() => sepet.value.reduce((t, i) => t + (i.miktar * i.fiyat), 0))

const toplamFt3 = computed(() => sepet.value.reduce((t, i) => {
  const hacim = i.birimHacim || 1
  return t + (i.miktar * hacim)
}, 0))

const indirimTutari = computed(() => {
  if (indirimDegeri.value <= 0) return 0
  if (indirimTipi.value === 'yuzde') return toplam.value * (Math.min(indirimDegeri.value, 100) / 100)
  return Math.min(indirimDegeri.value, toplam.value)
})

const genelToplam = computed(() => Math.max(0, toplam.value - indirimTutari.value))

const odenenTutarComputed = computed(() => {
  if (odemeDurumu.value === 'tam') return genelToplam.value
  if (odemeDurumu.value === 'yarim') return genelToplam.value / 2
  return 0
})

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
    list = list.filter(u => u.kategori === filtreKategori.value.ad)
  }

  if (filtreArac.value) {
    list = list.filter(u => u.marka === filtreArac.value)
  }

  if (seriNoArama.value) {
    const q = seriNoArama.value.toLowerCase()
    list = list.filter(u => u.barkod?.toLowerCase().includes(q) || u.seriNo?.toLowerCase().includes(q))
  }

  if (aramaMetni.value) {
    const q = aramaMetni.value.toLowerCase()
    list = list.filter(u =>
      u.ad?.toLowerCase().includes(q) ||
      u.stokKodu?.toLowerCase().includes(q) ||
      u.barkod?.toLowerCase().includes(q)
    )
  }

  return list.slice(0, 100)
})

const simdikiTarih = computed(() => {
  const d = new Date()
  return d.toLocaleDateString('tr-TR') + ' ' + d.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })
})

const formatCurrency = (v) => v != null ? new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(v) : '0,00 ₺'

onMounted(async () => {
  try {
    await Promise.all([
      cariHesapStore.getAllCariHesaplar(),
      stokStore.getAll(),
      kategoriStore.getAllKategoriler()
    ])
  } catch (e) {
    console.error('Yukleme hatasi', e)
  }
})

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
  musteriOnerileri.value = cariHesapStore.cariHesaplar.filter(c =>
    c.ad?.toLowerCase().includes(q) ||
    c.vergiNo?.toLowerCase().includes(q) ||
    c.telefon?.includes(query)
  ).slice(0, 20)
}

const musteriKaydet = async () => {
  if (!yeniMusteri.value.ad) {
    toast.add({ severity: 'warn', summary: 'Uyarı', detail: 'Ad / Firma adı zorunludur', life: 3000 })
    return
  }
  musteriKaydediliyor.value = true
  try {
    const r = await cariHesapAPI.create(yeniMusteri.value)
    seciliMusteri.value = r.data
    yeniMusteriDialog.value = false
    yeniMusteri.value = { ad: '', telefon: '', email: '', adres: '', vergiNo: '' }
    toast.add({ severity: 'success', summary: 'Başarılı', detail: 'Cari hesap oluşturuldu', life: 3000 })
  } catch (e) {
    toast.add({ severity: 'error', summary: 'Hata', detail: e?.response?.data?.message || 'Kayıt başarısız', life: 5000 })
  }
  musteriKaydediliyor.value = false
}

const sepeteEkle = (u) => {
  const varOlan = sepet.value.find(i => i.id === u.id)
  if (varOlan) {
    varOlan.miktar++
  } else {
    sepet.value.push({
      id: u.id,
      ad: u.ad,
      stokKodu: u.stokKodu,
      barkod: u.barkod,
      miktar: 1,
      fiyat: u.fiyat || u.satisFiyati || 0,
      birim: u.birim || 'adet',
      birimHacim: u.birimHacim || 1
    })
  }
}

const miktarAzalt = (idx) => {
  if (sepet.value[idx].miktar > 1) sepet.value[idx].miktar--
  else sepetSil(idx)
}

const sepetSil = (idx) => {
  sepet.value.splice(idx, 1)
}

const fisiYazdir = () => {
  fisNo.value = 'F-' + Date.now().toString(36).toUpperCase()
  setTimeout(() => { window.print() }, 200)
}

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
      aciklama: 'Hizli Satis',
      araToplam: toplam.value,
      indirim: indirimTutari.value,
      genelToplam: genelToplam.value,
      odenenTutar: odenenTutar.value,
      odemeDurumu: odemeDurumText.value,
      kalemler: sepet.value.map(i => ({
        stokId: i.id,
        aciklama: i.ad,
        adet: i.miktar,
        birimFiyat: i.fiyat,
        kdvOrani: 20,
        tutar: Math.round(i.miktar * i.fiyat * 100) / 100
      }))
    })
    toast.add({ severity: 'success', summary: 'Başarılı', detail: `Satış tamamlandı - ${formatCurrency(genelToplam.value)}`, life: 5000 })
    sepet.value = []
    seciliMusteri.value = null
    anlikMusteri.value = false
    indirimDegeri.value = 0
    odemeDurumu.value = 'tam'
    odenenTutar.value = 0
  } catch (e) {
    toast.add({ severity: 'error', summary: 'Hata', detail: e?.response?.data?.message || 'Satış başarısız', life: 5000 })
  }
  kaydediliyor.value = false
}
</script>

<style scoped>
.pos-container { padding: 20px; height: calc(100vh - 80px); overflow-y: auto; }
.pos-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.breadcrumb { font-size: 13px; color: var(--text-muted); }
.breadcrumb i { margin-right: 4px; }
.user-info { font-size: 13px; color: var(--text-secondary); }
.user-info i { margin-right: 4px; }

.pos-body { display: flex; gap: 16px; align-items: flex-start; }
.pos-left { flex: 0 0 66.666%; max-width: 66.666%; }
.pos-right { flex: 0 0 33.333%; max-width: 33.333%; display: flex; flex-direction: column; gap: 12px; }

.filter-card :deep(.p-card-content) { padding-top: 0; }
.filter-title { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.filter-row { display: flex; gap: 8px; }
.filter-select { flex: 1; }

.serial-search { margin: 8px 0; }
.serial-search :deep(.p-inputtext) { width: 100%; padding-left: 42px; }

.product-section { margin-top: 4px; }
.product-header h3 { font-size: 14px; font-weight: 600; margin: 0 0 8px 0; color: var(--text-primary); }
.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 10px; max-height: calc(100vh - 280px); overflow-y: auto; padding-bottom: 8px; }

.product-card {
  position: relative; background: var(--bg-card); border: 1px solid var(--border);
  border-radius: 10px; padding: 12px; cursor: pointer; transition: all 0.15s;
  display: flex; flex-direction: column; align-items: center; text-align: center;
}
.product-card:hover { border-color: var(--accent); box-shadow: 0 2px 8px rgba(0,0,0,0.08); transform: translateY(-1px); }
.product-img { width: 64px; height: 64px; background: var(--surface-ground); border-radius: 8px; display: flex; align-items: center; justify-content: center; margin-bottom: 8px; }
.product-img i { font-size: 28px; color: var(--text-muted); }
.stock-badge { position: absolute; top: 8px; right: 8px; }
.product-details { width: 100%; }
.product-name { display: block; font-size: 13px; font-weight: 600; color: var(--text-primary); margin-bottom: 2px; }
.product-serial { display: block; font-size: 11px; color: var(--text-muted); }
.product-meta { display: block; font-size: 10px; color: var(--text-muted); margin: 2px 0; }
.product-price { display: inline-block; font-size: 15px; font-weight: 700; color: var(--accent); margin-top: 4px; padding: 2px 10px; background: rgba(59,130,246,0.1); border-radius: 12px; }
.empty-products { grid-column: 1 / -1; text-align: center; padding: 40px; color: var(--text-muted); }
.empty-products i { font-size: 36px; display: block; margin-bottom: 8px; }

.customer-card :deep(.p-card-content) { padding-top: 0; }
.customer-field { display: flex; flex-direction: column; gap: 8px; }
.anlik-musteri { margin-bottom: 4px; }
.musteri-option { display: flex; justify-content: space-between; align-items: center; }
.musteri-option-detay { font-size: 11px; color: var(--text-muted); }

.sepet-card { max-height: 350px; overflow-y: auto; }
.sepet-card :deep(.p-card-content) { padding-top: 0; }
.sepet-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.sepet-bos { text-align: center; padding: 20px; color: var(--text-muted); font-size: 13px; }
.sepet-item { padding: 6px 0; border-bottom: 1px solid var(--border); }
.sepet-item:last-child { border-bottom: none; }
.sepet-ad { font-size: 12px; font-weight: 600; margin-bottom: 2px; }
.sepet-satir { display: flex; align-items: center; gap: 4px; }
.sepet-adet { width: 22px; text-align: center; font-weight: 700; font-size: 13px; }
.sepet-birimfiyat { font-size: 11px; color: var(--text-muted); margin-left: auto; }
.sepet-tutar { font-size: 13px; font-weight: 700; min-width: 60px; text-align: right; }

.ozet-satir { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; font-size: 13px; }
.ozet-indirim { display: flex; align-items: center; gap: 6px; }
.indirim-input { width: 100px; }
.ozet-ayrac { border: none; border-top: 1px solid var(--border); margin: 6px 0; }
.ozet-genel { border-top: 2px solid var(--border); margin-top: 4px; padding-top: 8px; }
.genel-toplam-deger { font-size: 18px; font-weight: 800; color: var(--accent); }

.odeme-card :deep(.p-card-content) { padding-top: 0; }
.odeme-card :deep(.p-selectbutton) { display: flex; }
.odeme-card :deep(.p-selectbutton .p-button) { flex: 1; font-size: 12px; }
.odenen-satir { margin-top: 8px; }
.odenen-satir label { display: block; font-size: 12px; font-weight: 600; color: var(--text-secondary); margin-bottom: 4px; }
.odeme-durum { margin-top: 8px; }
.odeme-durum :deep(.p-tag) { justify-content: center; }
.odeme-kalan { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; font-size: 13px; }
.kalan-deger { font-weight: 700; color: var(--accent); }

.fis-card :deep(.p-card-content) { padding: 0; }
.fis-card-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.fis-onizleme-kapsam { overflow-x: auto; padding: 12px; background: var(--bg-secondary); border-radius: 0 0 8px 8px; }
.fis-onizleme {
  width: 80mm; margin: 0 auto; padding: 12px 8px;
  background: white; color: black;
  font-size: 11px; font-family: 'Courier New', monospace;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);
}
.fis-header { text-align: center; margin-bottom: 6px; }
.fis-baslik { font-size: 13px; font-weight: 700; }
.fis-tarih { font-size: 10px; margin-top: 2px; }
.fis-fisno { font-size: 10px; margin-top: 1px; color: #555; }
.fis-musteri { margin-bottom: 4px; font-size: 10px; }
.fis-ayrac { text-align: center; color: #999; margin: 3px 0; letter-spacing: 2px; }
.fis-kalemler { }
.fis-kalem { display: flex; justify-content: space-between; padding: 2px 0; }
.fis-kalem-ad { }
.fis-kalem-tutar { white-space: nowrap; }
.fis-toplam { display: flex; justify-content: space-between; padding: 3px 0; font-size: 12px; }
.fis-indirim { display: flex; justify-content: space-between; padding: 2px 0; color: #c00; font-size: 11px; }
.fis-genel-toplam { display: flex; justify-content: space-between; padding: 4px 0; border-top: 2px solid #000; font-weight: 700; font-size: 13px; }
.fis-toplam-deger { }
.fis-odeme { margin-top: 4px; }
.fis-odeme-satir { display: flex; justify-content: space-between; padding: 2px 0; font-size: 10px; }
.fis-odeme-durum { font-weight: 600; }
.fis-footer { text-align: center; margin-top: 4px; }
.fis-tesekkur { font-size: 10px; color: #555; }

.satis-buton { margin-top: 4px; }

.field { margin-bottom: 12px; }
.required { color: #f87171; }

@media (max-width: 1100px) {
  .pos-body { flex-direction: column; }
  .pos-left, .pos-right { flex: 0 0 100%; max-width: 100%; }
  .product-grid { grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); }
}
</style>

<style>
@media print {
  body * { visibility: hidden; }
  #fisOnizleme, #fisOnizleme * { visibility: visible; }
  #fisOnizleme {
    position: fixed; top: 0; left: 0;
    width: 80mm; padding: 10mm;
    background: white; color: black;
    font-size: 12px; font-family: 'Courier New', monospace;
  }
}
</style>