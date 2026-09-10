<template>
  <div class="crm-container">
    <div class="sayfa-baslik">
      <h1 class="page-title">
        {{ t('crm.title') }}
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
          :label="t('crm.yeniFirsat')"
          icon="pi pi-plus"
          @click="dialogAc()"
        />
      </div>
    </div>

    <IlkZiyaretIpuclari
      anahtar="crm"
      :baslik="t('crm.ipucuBaslik')"
      :metin="t('crm.ipucuMetin')"
    />

    <div
      v-if="gorunumTipi === 'tablo'"
      class="crm-filtreler"
    >
      <Button
        v-for="d in durumlar"
        :key="d.value"
        :label="d.label"
        size="small"
        :severity="aktifDurum === d.value ? 'contrast' : 'secondary'"
        :outlined="aktifDurum !== d.value"
        @click="filtreDegistir(d.value)"
      />
    </div>

    <div class="crm-istatistik">
      <div class="istatistik-kutu">
        <span>{{ t('crm.toplamFirsat') }}</span>
        <strong>{{ firsatlar ? firsatlar.length : 0 }}</strong>
      </div>
      <div class="istatistik-kutu">
        <span>{{ t('crm.toplamDeger') }}</span>
        <strong>{{ formatCurrency(toplamDeger) }}</strong>
      </div>
      <div class="istatistik-kutu">
        <span>{{ t('crm.kazanilan') }}</span>
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
              <i class="pi pi-building" /> {{ item.cariHesapAd || t('crm.cariBelirtilmemis') }}
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
            {{ t('crm.firsatYok') }}
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
      :arama-placeholder="t('crm.aramaPlaceholder')"
      gorunum-anahtari="crm_firsatlar"
    >
      <Column
        field="ad"
        :header="t('crm.firsat')"
        sortable
      />
      <Column
        field="cariHesapAd"
        :header="t('crm.cariHesap')"
      >
        <template #body="{ data }">
          {{ data.cariHesapAd || '-' }}
        </template>
      </Column>
      <Column
        field="deger"
        :header="t('crm.deger')"
        sortable
      >
        <template #body="{ data }">
          {{ formatCurrency(data.deger) }}
        </template>
      </Column>
      <Column
        field="durum"
        :header="t('common.status')"
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
        :header="t('crm.kaynak')"
      />
      <Column
        field="tahminiKapanis"
        :header="t('crm.tahminiKapanis')"
        sortable
      >
        <template #body="{ data }">
          {{ formatDate(data.tahminiKapanis) }}
        </template>
      </Column>
      <Column
        :header="t('crm.islem')"
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

    <Card class="churn-kart">
      <template #title>
        <div class="churn-baslik">
          <span><i class="pi pi-chart-line" /> {{ t('crm.churnBaslik') }}</span>
          <Button
            icon="pi pi-refresh"
            class="p-button-sm p-button-text"
            :loading="churnYukleniyor"
            @click="churnYukle"
          />
        </div>
      </template>
      <template #content>
        <p class="churn-aciklama">
          {{ t('crm.churnAciklama') }}
        </p>
        <DataTable
          :value="churnList"
          size="small"
          striped-rows
          :loading="churnYukleniyor"
        >
          <Column
            field="cariAd"
            :header="t('crm.musteri')"
          />
          <Column :header="t('crm.risk')">
            <template #body="{ data }">
              <Tag
                :value="data.seviye"
                :severity="churnSeverity(data.seviye)"
              />
            </template>
          </Column>
          <Column
            field="skor"
            :header="t('crm.skor')"
          >
            <template #body="{ data }">
              {{ data.skor }}
            </template>
          </Column>
          <Column
            field="sonIslemGunOnce"
            :header="t('crm.sonIslem')"
          />
          <Column
            field="toplamCiro"
            :header="t('crm.toplamCiro')"
          >
            <template #body="{ data }">
              {{ formatCurrency(data.toplamCiro) }}
            </template>
          </Column>
          <Column
            field="oneri"
            :header="t('crm.oneri')"
          />
        </DataTable>
        <div
          v-if="churnList.length === 0 && !churnYukleniyor"
          class="churn-bos"
        >
          {{ t('crm.churnBos') }}
        </div>
      </template>
    </Card>

    <Dialog
      v-model:visible="dialog"
      :header="dialogHeader"
      modal
      :style="{ width: '540px' }"
    >
      <div class="form-grid">
        <div class="field">
          <label class="zorunlu">{{ t('crm.firsatAdi') }}</label>
          <InputText
            v-model="form.ad"
            class="w-full"
            :class="{ 'p-invalid': formHatali.ad }"
          />
          <small
            v-if="formHatali.ad"
            class="hata-mesaj"
          >{{ t('crm.firsatAdiZorunlu') }}</small>
        </div>
        <div class="field">
          <label>{{ t('common.status') }}</label>
          <Select
            v-model="form.durum"
            :options="durumlar"
            option-label="label"
            option-value="value"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('crm.cariHesap') }}</label>
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
          <label>{{ t('crm.degerTL') }}</label><InputNumber
            v-model="form.deger"
            mode="currency"
            currency="TRY"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('crm.kaynak') }}</label>
          <Select
            v-model="form.kaynak"
            :options="kaynakSecenekleri"
            class="w-full"
            show-clear
          />
        </div>
        <div class="field">
          <label>{{ t('crm.tahminiKapanis') }}</label><DatePicker
            v-model="form.tahminiKapanis"
            date-format="dd/mm/yy"
            class="w-full"
          />
        </div>
        <div class="field">
          <label>{{ t('common.description') }}</label><Textarea
            v-model="form.aciklama"
            rows="3"
            class="w-full"
          />
        </div>
      </div>
      <template #footer>
        <Button
          :label="t('common.cancel')"
          icon="pi pi-times"
          class="p-button-text"
          @click="dialog = false"
        />
        <Button
          :label="t('common.save')"
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
import { crmAPI, cariHesapAPI, churnAPI } from '../api/index.js'
import SatirEylemleri from '../components/SatirEylemleri.vue'
import IlkZiyaretIpuclari from '../components/IlkZiyaretIpuclari.vue'
import { useGeriAl } from '../composables/useGeriAl.js'
import { formatCurrency } from '../utils/format.js'
import { useI18n } from 'vue-i18n'

const toast = useToast()
const toastBildirim = useToastBildirim()
const confirm = useConfirm()
const { silVeGeriAl } = useGeriAl()
const { t } = useI18n()

const durumlar = computed(() => [
  { label: t('crm.durumYeni'), value: 'YENI' },
  { label: t('crm.durumTemas'), value: 'TEMAS' },
  { label: t('crm.durumTeklif'), value: 'TEKLIF' },
  { label: t('crm.durumKazanildi'), value: 'KAZANILDI' },
  { label: t('crm.durumKaybedildi'), value: 'KAYBEDILDI' }
])

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

const dialogHeader = computed(() => (duzenleme.value ? t('crm.firsatDuzenle') : t('crm.yeniFirsat')))
const toplamDeger = computed(() => firsatlar.value.reduce((t, f) => t + (Number(f.deger) || 0), 0))
const kazananSayisi = computed(() => firsatlar.value.filter((f) => f.durum === 'KAZANILDI').length)

import { formatTarih as formatDate } from '../utils/format.js'
const durumEtiketi = (d) => durumlar.value.find((x) => x.value === d)?.label || d
const durumSeverity = (d) =>
  ({ YENI: 'info', TEMAS: 'primary', TEKLIF: 'warning', KAZANILDI: 'success', KAYBEDILDI: 'danger' })[d] || 'secondary'

const filtreDegistir = (d) => {
  aktifDurum.value = aktifDurum.value === d ? '' : d
  firsatlariYukle()
}

onMounted(async () => {
  firsatlariYukle()
  churnYukle()
  try {
    const r = await cariHesapAPI.getAll()
    cariler.value = r.data?.content || r.data || []
  } catch {
    /* empty */
  }
})

const churnList = ref([])
const churnYukleniyor = ref(false)

const churnYukle = async () => {
  churnYukleniyor.value = true
  try {
    const r = await churnAPI.analiz()
    churnList.value = r.data || []
  } catch {
    churnList.value = []
  } finally {
    churnYukleniyor.value = false
  }
}

const churnSeverity = (s) => (s === 'YUKSEK' ? 'danger' : s === 'ORTA' ? 'warning' : 'success')

const firsatlariYukle = async () => {
  yukleniyor.value = true
  try {
    const params = aktifDurum.value ? { durum: aktifDurum.value } : {}
    const r = await crmAPI.getFirsatlar(params)
    firsatlar.value = r.data || []
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('crm.hataYukleme'))
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
  const kopya = { ...data, id: null, ad: t('crm.kopya', { ad: data.ad }) }
  dialogAc(kopya)
  duzenleme.value = false
}

const kaydet = async () => {
  if (!form.value.ad.trim()) {
    formHatali.value.ad = true
    toastBildirim.uyari(t('crm.firsatAdiZorunlu'))
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
    toastBildirim.basarili(t('crm.firsatKaydedildi'))
    dialog.value = false
    firsatlariYukle()
  } catch (err) {
    toastBildirim.hata(err?.response?.data?.message || t('crm.islemBasarisiz'))
  }
  kaydediliyor.value = false
}

const gorunumTipi = ref('kanban')
const gorunumSecenekleri = computed(() => [
  { label: t('crm.kanban'), value: 'kanban' },
  { label: t('crm.tablo'), value: 'tablo' }
])

const kaynakSecenekleri = computed(() => [
  t('crm.kaynakWeb'),
  t('crm.kaynakTelefon'),
  t('crm.kaynakReferans'),
  t('crm.kaynakFuarlar'),
  t('crm.kaynakSosyalMedya'),
  t('crm.kaynakEposta')
])

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
    toastBildirim.basarili(t('crm.asamaTasindi', { durum: durumEtiketi(yeniDurum) }))
  } catch {
    item.durum = eskiDurum
    toastBildirim.hata(t('crm.asamaGuncellenemedi'))
  } finally {
    suruklenenFirsat.value = null
  }
}

const sil = (data) => {
  confirm.require({
    message: t('crm.silOnayMesaj', { ad: data.ad }),
    header: t('masraflar.silmeOnayi'),
    icon: 'pi pi-exclamation-triangle',
    acceptLabel: t('masraflar.evetSil'),
    rejectLabel: t('common.cancel'),
    accept: async () => {
      try {
        await crmAPI.firsatSil(data.id)
        firsatlar.value = firsatlar.value.filter((f) => f.id !== data.id)
        silVeGeriAl({
          veri: data,
          metin: t('crm.firsatSilindi', { ad: data.ad }),
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
        toast.add({ severity: 'success', summary: t('crm.silindi'), detail: t('crm.firsatSilindiDetay'), life: 3000 })
      } catch (err) {
        toastBildirim.hata(err?.response?.data?.message || t('crm.silmeBasarisiz'))
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
  gap: 12px;
  flex-wrap: wrap;
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
  grid-template-columns: repeat(auto-fit, minmax(min(220px, 100%), 1fr));
  gap: 16px;
  margin-top: 10px;
  align-items: start;
}
.kanban-kolon {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px;
  min-height: 280px;
  min-width: 0;
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
.churn-kart {
  margin-top: 24px;
}
.churn-baslik {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  width: 100%;
}
.churn-baslik span {
  display: inline-flex;
  align-items: center;
  min-width: 0;
}
.churn-aciklama {
  font-size: 0.85rem;
  color: var(--text-secondary);
  margin-bottom: 12px;
}
.churn-bos {
  padding: 16px;
  text-align: center;
  color: var(--text-muted);
  font-size: 0.85rem;
}
</style>
