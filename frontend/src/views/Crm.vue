<template>
  <div class="crm-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        CRM — Fırsat Takibi
      </h1>
      <div class="baslik-aksiyonlar">
        <SelectButton
          v-model="gorunumTipi"
          :options="gorunumSecenekleri"
          option-label="label"
          option-value="value"
          class="mr-2"
        />
        <Button
          label="Yeni Fırsat"
          icon="pi pi-plus"
          @click="dialogAc()"
        />
      </div>
    </div>

    <IlkZiyaretIpuclari
      anahtar="crm"
      baslik="CRM Fırsat Takibi"
      metin="Potansiyel müşterileri fırsat olarak kaydedin, aşamalarını (Yeni → Temas → Teklif → Kazanıldı) takip edin. İster tablo görünümünde ister Kanban panosunda sürükle-bırak ile yönetin."
    />

    <div
      v-if="gorunumTipi === 'tablo'"
      class="crm-filtreler"
    >
      <Button
        v-for="d in durumlar"
        :key="d.value"
        :label="d.label"
        :class="{ 'filtre-aktif': aktifDurum === d.value }"
        size="small"
        :severity="aktifDurum === d.value ? 'primary' : 'secondary'"
        :outlined="aktifDurum !== d.value"
        @click="filtreDegistir(d.value)"
      />
    </div>

    <div class="crm-istatistik">
      <div class="istatistik-kutu">
        <span>Toplam Fırsat</span>
        <strong>{{ firsatlar ? firsatlar.length : 0 }}</strong>
      </div>
      <div class="istatistik-kutu">
        <span>Toplam Değer</span>
        <strong>{{ formatCurrency(toplamDeger) }}</strong>
      </div>
      <div class="istatistik-kutu">
        <span>Kazanılan</span>
        <strong>{{ kazananSayisi }}</strong>
      </div>
    </div>

    <!-- Kanban Görünümü -->
    <div
      v-if="gorunumTipi === 'kanban'"
      class="crm-kanban-board"
    >
      <div
        v-for="kolon in durumlar"
        :key="kolon.value"
        class="kanban-kolon"
        @dragover.prevent
        @drop="firsatSurukleBirak($event, kolon.value)"
      >
        <div class="kolon-baslik">
          <Tag
            :value="kolon.label"
            :severity="durumSeverity(kolon.value)"
          />
          <span class="kolon-sayi">{{ firsatlarByDurum(kolon.value).length }}</span>
        </div>
        <div class="kolon-icerik">
          <div
            v-for="item in firsatlarByDurum(kolon.value)"
            :key="item.id"
            class="kanban-kart"
            draggable="true"
            @dragstart="suruklemeBaslat($event, item)"
            @click="dialogAc(item)"
          >
            <div class="kart-baslik">
              <strong>{{ item.ad }}</strong>
              <span class="kart-tutar">{{ formatCurrency(item.deger) }}</span>
            </div>
            <div class="kart-cari">
              <i class="pi pi-building" /> {{ item.cariHesapAd || 'Cari Belirtilmemiş' }}
            </div>
            <div
              v-if="item.tahminiKapanis"
              class="kart-tarih"
            >
              <i class="pi pi-calendar" /> {{ formatDate(item.tahminiKapanis) }}
            </div>
          </div>
          <div
            v-if="!firsatlarByDurum(kolon.value).length"
            class="kolon-bos"
          >
            Fırsat yok
          </div>
        </div>
      </div>
    </div>

    <!-- Tablo Görünümü -->
    <AppDataTable
      v-else
      :value="firsatlar"
      :loading="yukleniyor"
      arama-aktif
      arama-placeholder="Fırsatlarda ara..."
      gorunum-anahtari="crm_firsatlar"
    >
      <Column
        field="ad"
        header="Fırsat"
        sortable
      />
      <Column
        field="cariHesapAd"
        header="Cari Hesap"
      >
        <template #body="{ data }">
          {{ data.cariHesapAd || '-' }}
        </template>
      </Column>
      <Column
        field="deger"
        header="Değer"
        sortable
      >
        <template #body="{ data }">
          {{ formatCurrency(data.deger) }}
        </template>
      </Column>
      <Column
        field="durum"
        header="Durum"
      >
        <template #body="{ data }">
          <Tag
            :value="durumEtiketi(data.durum)"
            :severity="durumSeverity(data.durum)"
          />
        </template>
      </Column>
      <Column
        field="kaynak"
        header="Kaynak"
      />
      <Column
        field="tahminiKapanis"
        header="Tahmini Kapanış"
        sortable
      >
        <template #body="{ data }">
          {{ formatDate(data.tahminiKapanis) }}
        </template>
      </Column>
      <Column
        header="İşlem"
        style="width: 60px"
      >
        <template #body="{ data }">
          <SatirEylemleri
            :gorunur="{ duzenle: true, cogalt: true, sil: true }"
            @duzenle="dialogAc(data)"
            @cogalt="cogalt(data)"
            @sil="sil(data)"
          />
        </template>
      </Column>
    </AppDataTable>

    <Dialog
      v-model:visible="dialog"
      :header="dialogHeader"
      modal
      :style="{ width: '540px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label class="zorunlu">Fırsat Adı</label>
          <InputText
            v-model="form.ad"
            class="w-full"
            :class="{ 'p-invalid': formHatali.ad }"
          />
          <small
            v-if="formHatali.ad"
            class="hata-mesaj"
          >Fırsat adı zorunludur</small>
        </div>
        <div class="field">
          <label>Durum</label>
          <Select
            v-model="form.durum"
            :options="durumlar"
            option-label="label"
            option-value="value"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Cari Hesap</label>
          <Select
            v-model="form.cariHesapId"
            :options="cariler"
            option-label="ad"
            option-value="id"
            class="w-full"
            show-clear
            filter
          />
        </div>
        <div class="field">
          <label>Değer (₺)</label><InputNumber
            v-model="form.deger"
            mode="currency"
            currency="TRY"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Kaynak</label>
          <Select
            v-model="form.kaynak"
            :options="['Web', 'Telefon', 'Referans', 'Fuarlar', 'Sosyal Medya', 'E-Posta']"
            class="w-full"
            show-clear
          />
        </div>
        <div class="field">
          <label>Tahmini Kapanış</label><DatePicker
            v-model="form.tahminiKapanis"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>Açıklama</label><Textarea
            v-model="form.aciklama"
            rows="3"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          label="İptal"
          icon="pi pi-times"
          class="p-button-text"
          @click="dialog = false"
        />
        <Button
          label="Kaydet"
          icon="pi pi-check"
          :loading="kaydediliyor"
          @click="kaydet"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useToastBildirim } from '../composables/useToastBildirim.js'
import { useConfirm } from 'primevue/useconfirm'
import { crmAPI, cariHesapAPI } from '../api/index.js'
import SatirEylemleri from '../components/SatirEylemleri.vue'
import IlkZiyaretIpuclari from '../components/IlkZiyaretIpuclari.vue'
import { useGeriAl } from '../composables/useGeriAl.js'
import { formatCurrency } from '../utils/format.js'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { silVeGeriAl } = useGeriAl()

const durumlar = [
  { label: 'Yeni', value: 'YENI' },
  { label: 'Temas', value: 'TEMAS' },
  { label: 'Teklif', value: 'TEKLIF' },
  { label: 'Kazanıldı', value: 'KAZANILDI' },
  { label: 'Kaybedildi', value: 'KAYBEDILDI' }
]

const firsatlar = ref([])
const cariler = ref([])
const yukleniyor = ref(false)
const kaydediliyor = ref(false)
const aktifDurum = ref('')
const dialog = ref(false)
const duzenleme = ref(false)
const form = ref({
  ad: '',
  durum: 'YENI',
  cariHesapId: null,
  deger: 0,
  kaynak: null,
  tahminiKapanis: null,
  aciklama: ''
})
const formHatali = ref({ ad: false })

const dialogHeader = computed(() => (duzenleme.value ? 'Fırsat Düzenle' : 'Yeni Fırsat'))
const toplamDeger = computed(() => firsatlar.value.reduce((t, f) => t + (Number(f.deger) || 0), 0))
const kazananSayisi = computed(() => firsatlar.value.filter((f) => f.durum === 'KAZANILDI').length)

const formatDate = (d) =>
  d ? new Intl.DateTimeFormat('tr-TR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(d)) : '-'
const durumEtiketi = (d) => durumlar.find((x) => x.value === d)?.label || d
const durumSeverity = (d) =>
  ({ YENI: 'info', TEMAS: 'primary', TEKLIF: 'warning', KAZANILDI: 'success', KAYBEDILDI: 'danger' })[d] || 'secondary'

const filtreDegistir = (d) => {
  aktifDurum.value = aktifDurum.value === d ? '' : d
  firsatlariYukle()
}

onMounted(async () => {
  firsatlariYukle()
  try {
    const r = await cariHesapAPI.getAll()
    cariler.value = r.data?.content || r.data || []
  } catch {
    /* empty */
  }
})

const firsatlariYukle = async () => {
  yukleniyor.value = true
  try {
    const params = aktifDurum.value ? { durum: aktifDurum.value } : {}
    const r = await crmAPI.getFirsatlar(params)
    firsatlar.value = r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'Fırsatlar yüklenemedi')
  }
  yukleniyor.value = false
}

const dialogAc = (data) => {
  duzenleme.value = !!data
  formHatali.value = { ad: false }
  form.value = data
    ? { ...data, tahminiKapanis: data.tahminiKapanis ? new Date(data.tahminiKapanis) : null }
    : { ad: '', durum: 'YENI', cariHesapId: null, deger: 0, kaynak: null, tahminiKapanis: null, aciklama: '' }
  dialog.value = true
}

const cogalt = (data) => {
  const kopya = { ...data, id: null, ad: data.ad + ' (Kopya)' }
  dialogAc(kopya)
  duzenleme.value = false
}

const kaydet = async () => {
  if (!form.value.ad.trim()) {
    formHatali.value.ad = true
    toastBildirim.uyari('Fırsat adı zorunludur')
    return
  }
  formHatali.value.ad = false
  kaydediliyor.value = true
  try {
    const payload = {
      ...form.value,
      tahminiKapanis: form.value.tahminiKapanis
        ? (form.value.tahminiKapanis.toISOString?.().split('T')[0] ?? form.value.tahminiKapanis)
        : null
    }
    if (duzenleme.value) await crmAPI.firsatGuncelle(form.value.id, payload)
    else await crmAPI.firsatOlustur(payload)
    toastBildirim.basarili('Fırsat kaydedildi')
    dialog.value = false
    firsatlariYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || 'İşlem başarısız')
  }
  kaydediliyor.value = false
}

const gorunumTipi = ref('kanban')
const gorunumSecenekleri = [
  { label: 'Kanban', value: 'kanban' },
  { label: 'Tablo', value: 'tablo' }
]

const firsatlarByDurum = (durum) => {
  return firsatlar.value.filter((f) => f.durum === durum)
}

const suruklenenFirsat = ref(null)

const suruklemeBaslat = (e, item) => {
  suruklenenFirsat.value = item
  if (e.dataTransfer) {
    e.dataTransfer.setData('text/plain', item.id)
  }
}

const firsatSurukleBirak = async (e, yeniDurum) => {
  if (!suruklenenFirsat.value) return
  const item = suruklenenFirsat.value
  if (item.durum === yeniDurum) return

  const eskiDurum = item.durum
  item.durum = yeniDurum
  try {
    await crmAPI.firsatGuncelle(item.id, {
      ...item,
      durum: yeniDurum
    })
    toastBildirim.basarili(`Fırsat "${durumEtiketi(yeniDurum)}" aşamasına taşındı`)
  } catch {
    item.durum = eskiDurum
    toastBildirim.hata('Aşama güncellenemedi')
  } finally {
    suruklenenFirsat.value = null
  }
}

const sil = (data) => {
  confirm.require({
    message: `"${data.ad}" fırsatını silmek istediğinize emin misiniz?`,
    header: 'Silme Onayı',
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: 'Evet, Sil',
    rejectLabel: 'İptal',
    accept: async () => {
      try {
        await crmAPI.firsatSil(data.id)
        firsatlar.value = firsatlar.value.filter((f) => f.id !== data.id)
        silVeGeriAl({
          veri: data,
          metin: `"${data.ad}" fırsatı silindi`,
          geriYukle: async (kayit) => {
            await crmAPI.firsatOlustur({
              ad: kayit.ad,
              cariHesapId: kayit.cariHesapId,
              durum: kayit.durum,
              deger: kayit.deger,
              kaynak: kayit.kaynak,
              tahminiKapanis: kayit.tahminiKapanis,
              aciklama: kayit.aciklama
            })
            firsatlariYukle()
          }
        })
        toast.add({ severity: 'success', summary: 'Silindi', detail: 'Fırsat silindi', life: 3000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || 'Silme başarısız')
      }
    }
  })
}
</script>

<style scoped>
.crm-container {
  padding: 0;
}
.sayfa-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.baslik-aksiyonlar {
  display: flex;
  align-items: center;
  gap: 10px;
}
.crm-filtreler {
  display: flex;
  gap: 8px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}
.crm-istatistik {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.istatistik-kutu {
  flex: 1;
  min-width: 160px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.istatistik-kutu span {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 600;
  text-transform: uppercase;
}
.istatistik-kutu strong {
  font-size: 20px;
  color: var(--text-primary);
}
.crm-kanban-board {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-top: 10px;
  align-items: start;
}
.kanban-kolon {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px;
  min-height: 450px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.kolon-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}
.kolon-sayi {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-muted);
  background: rgba(148, 163, 184, 0.1);
  padding: 2px 8px;
  border-radius: 12px;
}
.kolon-icerik {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
}
.kanban-kart {
  background: var(--bg-primary, #0f172a);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 12px;
  cursor: grab;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.kanban-kart:hover {
  transform: translateY(-2px);
  border-color: var(--accent, #3b82f6);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
.kanban-kart:active {
  cursor: grabbing;
}
.kart-baslik {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}
.kart-baslik strong {
  font-size: 13px;
  color: var(--text-primary);
}
.kart-tutar {
  font-size: 12px;
  font-weight: 700;
  color: #10b981;
  white-space: nowrap;
}
.kart-cari,
.kart-tarih {
  font-size: 11px;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: 5px;
}
.kolon-bos {
  text-align: center;
  font-size: 12px;
  color: var(--text-muted);
  padding: 20px 0;
  border: 1px dashed var(--border);
  border-radius: 8px;
}
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}
.w-full {
  width: 100%;
}
.zorunlu::after {
  content: ' *';
  color: #ef4444;
}
.hata-mesaj {
  color: #ef4444;
  font-size: 12px;
}
:deep(.p-invalid) {
  border-color: #ef4444 !important;
}
</style>
